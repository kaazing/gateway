/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import static org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer;

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
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelFuture;

import com.kaazing.mina.core.service.AbstractIoProcessor;
import com.kaazing.mina.core.service.AbstractIoService;

/**
 * Since this class is stateless it is a singleton within each consuming service (to avoid static state)
 */
final class ChannelIoProcessor extends AbstractIoProcessor<ChannelIoSession<? extends ChannelConfig>> {

    ChannelIoProcessor() {
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
        flushNow(session, System.currentTimeMillis());
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

    private boolean flushNow(ChannelIoSession<? extends ChannelConfig> session, long currentTime) {
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

                if (message instanceof IoBuffer) {
                    IoBuffer buf = (IoBuffer) message;
                    int written = buf.remaining();
                    ChannelFuture future = channel.write(wrappedBuffer(buf.buf()));
                    future.addListener(new ChannelWriteFutureListener(filterChain, req));
                    session.increaseWrittenBytes(written, System.currentTimeMillis());
                } else if (message instanceof FileRegion) {
                    FileRegion region = (FileRegion) message;
                    long writtenBefore = region.getWrittenBytes();
                    ChannelFuture future = channel.write(region);  // TODO: FileRegion
                    future.addListener(new ChannelWriteFutureListener(filterChain, req));
                    session.increaseWrittenBytes((int) (region.getWrittenBytes() - writtenBefore),
                            System.currentTimeMillis());
                } else {
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
