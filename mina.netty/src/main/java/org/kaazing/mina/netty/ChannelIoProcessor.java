/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/** The copyright above pertains to portions created by Kaazing */

package org.kaazing.mina.netty;

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
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;

import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.service.AbstractIoProcessor;
import org.kaazing.mina.netty.ChannelIoBufferAllocator.ChannelIoBuffer;
import org.kaazing.mina.netty.channel.DownstreamMessageEventEx;
import org.kaazing.mina.netty.util.threadlocal.VicariousThreadLocal;
import org.kaazing.mina.util.ExceptionMonitor;

/**
 * Since this class is stateless it is a singleton within each consuming service (to avoid static state)
 */
final class ChannelIoProcessor extends AbstractIoProcessor<ChannelIoSession<? extends ChannelConfig>> {

    private static class ResetableThreadLocal<T> extends VicariousThreadLocal<T> {

        public T reset() {
            T newValue = initialValue();
            set(newValue);
            return newValue;
        }

    }

    // note: ChannelIoProcessor instance is shared across worker threads (!)
    private final ResetableThreadLocal<DownstreamMessageEventEx> writeRequestEx;

    ChannelIoProcessor() {
        this.writeRequestEx = new ResetableThreadLocal<DownstreamMessageEventEx>() {

            @Override
            protected DownstreamMessageEventEx initialValue() {
                return new DownstreamMessageEventEx();
            }
        };
    }

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
            IoServiceListenerSupport listeners = session.getService().getListeners();
            listeners.fireSessionCreated(session);
        } catch (Throwable e) {
            ExceptionMonitor.getInstance().exceptionCaught(e, session);

            try {
                destroy(session);
            } catch (Exception e1) {
                ExceptionMonitor.getInstance().exceptionCaught(e1, session);
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
            session.getService().getListeners()
                    .fireSessionDestroyed(session);
        }
        return false;
    }

    private void clearWriteRequestQueue(ChannelIoSession<? extends ChannelConfig> session) {
        WriteRequestQueue writeRequestQueue = session.getWriteRequestQueue();
        WriteRequest req;

        List<WriteRequest> failedRequests = new ArrayList<>();

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
                req = writeRequestQueue.poll(session);

                if (req == null) {
                    break;
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

                            // write shared buffer to channel
                            DownstreamMessageEventEx writeRequest = writeRequestEx.get();
                            if (!writeRequest.isResetable()) {
                                writeRequest = writeRequestEx.reset();
                            }
                            assert writeRequest.isResetable();
                            writeRequest.reset(channel, sharedBuf, null, false);

                            ChannelPipeline pipeline = channel.getPipeline();
                            pipeline.sendDownstream(writeRequest);

                            ChannelFuture future = writeRequest.getFuture();
                            if (future.isDone()) {
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

                                // register listener to detect when write completed
                                future.addListener(new ChannelWriteFutureListener(filterChain, req));
                            }
                        }
                        else {
                            // 1b. buffer is unshared
                            // write unshared buffer to channel
                            ByteBuffer unsharedBuf = channelIoBuf.buf();

                            DownstreamMessageEventEx writeRequest = writeRequestEx.get();
                            if (!writeRequest.isResetable()) {
                                writeRequest = writeRequestEx.reset();
                            }
                            assert writeRequest.isResetable();
                            writeRequest.reset(channel, unsharedBuf, null, false);

                            ChannelPipeline pipeline = channel.getPipeline();
                            pipeline.sendDownstream(writeRequest);

                            ChannelFuture future = writeRequest.getFuture();
                            if (future.isDone()) {
                                // unshared buffer write complete
                                ChannelWriteFutureListener.operationComplete(future, filterChain, req);
                            }
                            else {
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
