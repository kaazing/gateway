/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import static java.lang.String.format;
import static org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.file.FileRegion;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChainBuilder;
import org.apache.mina.core.service.IoServiceListenerSupport;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;
import org.apache.mina.core.write.WriteToClosedSessionException;
import org.apache.mina.util.ExceptionMonitor;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelFuture;

import com.kaazing.mina.core.buffer.IoBufferEx;
import com.kaazing.mina.core.service.AbstractIoProcessor;
import com.kaazing.mina.core.service.AbstractIoService;
import com.kaazing.mina.netty.ChannelIoBufferAllocator.ChannelIoBuffer;
import com.kaazing.mina.netty.buffer.ByteBufferWrappingChannelBuffer;

/**
 * Since this class is stateless it is a singleton within each consuming service (to avoid static state)
 */
final class ChannelIoProcessor extends AbstractIoProcessor<ChannelIoSession<? extends ChannelConfig>> {

    private static class ResetableThreadLocal<T> extends ThreadLocal<T> {

        public void reset() {
            set(initialValue());
        }

    }

    // note: ChannelIoProcessor instance is shared across worker threads (!)
    private final ResetableThreadLocal<ByteBufferWrappingChannelBuffer> wrappingBuf;

    ChannelIoProcessor() {
        this.wrappingBuf = new ResetableThreadLocal<ByteBufferWrappingChannelBuffer>() {

            @Override
            protected ByteBufferWrappingChannelBuffer initialValue() {
                return new ByteBufferWrappingChannelBuffer();
            }
        };
    };

    @Override
    protected void add0(ChannelIoSession<? extends ChannelConfig> session) {
        addNow(session);
    }

    @Override
    protected void remove0(ChannelIoSession<? extends ChannelConfig> session) {
        removeNow(session);
    }

    @Override
    protected void flush0(ChannelIoSession<? extends ChannelConfig> session) {
        flushNow(session);
    }

    @Override
    public void dispose() {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean isDisposed() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isDisposing() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected void updateTrafficControl0(ChannelIoSession<? extends ChannelConfig> session) {
        // suspend/resumeRead is implemented directly in ChannelIoSession so this should never be called
        throw new UnsupportedOperationException();
    }

    protected void init(ChannelIoSession<? extends ChannelConfig> session) {
    }

    protected void destroy(ChannelIoSession<? extends ChannelConfig> session) {
        session.getChannel().close();
    }

    private void addNow(ChannelIoSession<? extends ChannelConfig> session) {
        try {
            init(session);

            // Build the filter chain of this session.
            IoFilterChainBuilder chainBuilder = session.getService().getFilterChainBuilder();
            chainBuilder.buildFilterChain(session.getFilterChain());

            // DefaultIoFilterChain.CONNECT_FUTURE is cleared inside here
            // in AbstractIoFilterChain.fireSessionOpened().
            // Propagate the SESSION_CREATED event up to the chain
            IoServiceListenerSupport listeners = ((AbstractIoService) session.getService()).getListeners();
            listeners.fireSessionCreated(session);
        } catch (Throwable e) {
            ExceptionMonitor.getInstance().exceptionCaught(e);

            try {
                destroy(session);
            } catch (Exception e1) {
                ExceptionMonitor.getInstance().exceptionCaught(e1);
            }
        }
    }

    private boolean removeNow(ChannelIoSession<? extends ChannelConfig> session) {
        clearWriteRequestQueue(session);

        try {
            destroy(session);
            return true;
        } catch (Exception e) {
            IoFilterChain filterChain = session.getFilterChain();
            filterChain.fireExceptionCaught(e);
        } finally {
            clearWriteRequestQueue(session);
            ((AbstractIoService) session.getService()).getListeners()
                    .fireSessionDestroyed(session);
        }
        return false;
    }

    private void clearWriteRequestQueue(ChannelIoSession<? extends ChannelConfig> session) {
        WriteRequestQueue writeRequestQueue = session.getWriteRequestQueue();
        WriteRequest req;

        List<WriteRequest> failedRequests = new ArrayList<WriteRequest>();

        if ((req = writeRequestQueue.poll(session)) != null) {
            Object message = req.getMessage();

            if (message instanceof IoBuffer) {
                IoBuffer buf = (IoBuffer) message;

                // The first unwritten empty buffer must be
                // forwarded to the filter chain.
                if (buf.hasRemaining()) {
                    buf.reset();
                    failedRequests.add(req);
                } else {
                    IoFilterChain filterChain = session.getFilterChain();
                    filterChain.fireMessageSent(req);
                }
            } else {
                failedRequests.add(req);
            }

            // Discard others.
            while ((req = writeRequestQueue.poll(session)) != null) {
                failedRequests.add(req);
            }
        }

        // Create an exception and notify.
        if (!failedRequests.isEmpty()) {
            WriteToClosedSessionException cause = new WriteToClosedSessionException(
                    failedRequests);

            for (WriteRequest r : failedRequests) {
                session.decreaseScheduledBytesAndMessages(r);
                r.getFuture().setException(cause);
            }

            IoFilterChain filterChain = session.getFilterChain();
            filterChain.fireExceptionCaught(cause);
        }
    }

    private boolean flushNow(ChannelIoSession<? extends ChannelConfig> session) {
        if (!session.isConnected()) {
            removeNow(session);
            return false;
        }

        final WriteRequestQueue writeRequestQueue = session.getWriteRequestQueue();

        final Channel channel = session.getChannel();
        final IoFilterChain filterChain = session.getFilterChain();
        WriteRequest req = null;

        try {
            for (;;) {
                // Check for pending writes.
                req = session.getCurrentWriteRequest();

                if (req == null) {
                    req = writeRequestQueue.poll(session);

                    if (req == null) {
                        break;
                    }

                    session.setCurrentWriteRequest(req);
                }

                Object message = req.getMessage();

                if (message instanceof ChannelIoBuffer) {
                    ChannelIoBuffer channelIoBuf = (ChannelIoBuffer) message;
                    if (channelIoBuf.remaining() == 0) {
                        filterChain.fireMessageSent(req);
                    }
                    else {
                        // 1. detect shared buffer
                        if (channelIoBuf.isShared()) {
                            // 1a. buffer is shared
                            ByteBuffer sharedBuf = channelIoBuf.buf();
                            int position = sharedBuf.position();

                            // note: NETTY duplicates original ByteBuffer when converting ChannelBuffer to ByteBuffer
                            //       instead, wrap sharedBuf with a non-duplicating ChannelBuffer
                            ByteBufferWrappingChannelBuffer wrappingBuf = this.wrappingBuf.get();
                            ChannelBuffer channelBuf = wrappingBuf.wrap(sharedBuf);

                            // write shared buffer to channel
                            ChannelFuture future = channel.write(channelBuf);
                            if (future.isDone()) {
                                // unwrap wrapping buffer to release ByteBuffer reference
                                wrappingBuf.unwrap();

                                // reset shared buffer before messageSent
                                sharedBuf.position(position);

                                // shared buffer write complete
                                ChannelWriteFutureListener.operationComplete(future, filterChain, req);
                            }
                            else {
                                // shared buffer write incomplete
                                // update MINA IoBuffer to reference new shared buffer instead
                                // (leaving old shared buffer on this NETTY channel writeQueue)
                                ByteBuffer newSharedBuf = sharedBuf.duplicate();

                                // "reset" shared before messageSent
                                newSharedBuf.position(position);
                                channelIoBuf.buf(newSharedBuf);

                                // leave wrapping buffer wrapped, but create new wrapping buffer for next flush
                                this.wrappingBuf.reset();

                                // register listener to detect when write completed
                                future.addListener(new ChannelWriteFutureListener(filterChain, req));
                            }
                        }
                        else {
                            // 1b. buffer is unshared
                            // write unshared buffer to channel
                            ByteBuffer unsharedBuf = channelIoBuf.buf();
                            // note: NETTY duplicates original ByteBuffer when converting ChannelBuffer to ByteBuffer
                            //       instead, wrap sharedBuf with a non-duplicating ChannelBuffer
                            ByteBufferWrappingChannelBuffer wrappingBuf = this.wrappingBuf.get();
                            ChannelBuffer channelBuf = wrappingBuf.wrap(unsharedBuf);
                            ChannelFuture future = channel.write(channelBuf);
                            if (future.isDone()) {
                                // unwrap wrapping buffer to release ByteBuffer reference
                                wrappingBuf.unwrap();

                                // unshared buffer write complete
                                ChannelWriteFutureListener.operationComplete(future, filterChain, req);
                            }
                            else {
                                // leave wrapping buffer wrapped, but create new wrapping buffer for next flush
                                this.wrappingBuf.reset();

                                // unshared buffer write incomplete
                                future.addListener(new ChannelWriteFutureListener(filterChain, req));
                            }
                        }
                    }
                }
                else if (message instanceof FileRegion) {
                    FileRegion region = (FileRegion) message;
                    ChannelFuture future = channel.write(region);  // TODO: FileRegion
                    future.addListener(new ChannelWriteFutureListener(filterChain, req));
                }
                else if (message instanceof IoBufferEx && ((IoBufferEx) message).isShared()) {
                    String messageClassName = message.getClass().getName();
                    throw new IllegalStateException(format("Shared buffer MUST be ChannelIoBuffer, not %s", messageClassName));
                }
                else if (message instanceof IoBuffer) {
                    IoBuffer buf = (IoBuffer) message;
                    if (buf.remaining() == 0) {
                        filterChain.fireMessageSent(req);
                    }
                    else {
                        ChannelFuture future = channel.write(wrappedBuffer(buf.buf()));
                        future.addListener(new ChannelWriteFutureListener(filterChain, req));
                    }
                }
                else {
                    throw new IllegalStateException(
                            "Don't know how to handle message of type '"
                                    + message.getClass().getName()
                                    + "'.  Are you missing a protocol encoder?");
                }

                session.setCurrentWriteRequest(null);
            }
        } catch (Exception e) {
            if (req != null) {
                req.getFuture().setException(e);
            }

            filterChain.fireExceptionCaught(e);
            return false;
        }

        return true;
    }

}
