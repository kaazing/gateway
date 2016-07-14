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
/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.jboss.netty.channel.socket.nio;

import static org.jboss.netty.channel.Channels.*;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;

import org.apache.mina.core.future.WriteFuture;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.kaazing.mina.netty.bootstrap.ConnectionlessServerBootstrap;

/**
 * Receives downstream events from a {@link ChannelPipeline}.  It contains
 * an array of I/O workers.
 */
class NioChildDatagramPipelineSink extends AbstractNioChannelSink {

    private final WorkerPool<NioDatagramWorker> workerPool;

    /**
     * Creates a new {@link NioDatagramPipelineSink} with a the number of {@link NioDatagramWorker}s
     * specified in workerCount.  The {@link NioDatagramWorker}s take care of reading and writing
     * for the {@link NioDatagramChannel}.
     *
     * @param workerExecutor
     *        the {@link Executor} that will run the {@link NioDatagramWorker}s
     *        for this sink
     * @param workerCount
     *        the number of {@link NioDatagramWorker}s for this sink
     */
    NioChildDatagramPipelineSink(final WorkerPool<NioDatagramWorker> workerPool) {
        this.workerPool = workerPool;
    }

    /**
     * Handle downstream event.
     *
     * @param pipeline the {@link ChannelPipeline} that passes down the
     *                 downstream event.
     * @param e The downstream event.
     */
    public void eventSunk(final ChannelPipeline pipeline, final ChannelEvent e)
            throws Exception {

        final NioDatagramChannel channel = (NioDatagramChannel) e.getChannel();
        final ChannelFuture future = e.getFuture();
        if (e instanceof ChannelStateEvent) {
            final ChannelStateEvent stateEvent = (ChannelStateEvent) e;
            final ChannelState state = stateEvent.getState();
            final Object value = stateEvent.getValue();
            switch (state) {
                case OPEN:
                    if (Boolean.FALSE.equals(value)) {
                        closeRequested(pipeline, e);
                        channel.worker.close(channel, future);
                    }
                    break;
                case BOUND:
                    if (value != null) {
                        bind(channel, future, (InetSocketAddress) value);
                    } else {
                        channel.worker.close(channel, future);
                    }
                    break;
                case CONNECTED:
                    if (value != null) {
                        connect(channel, future, (InetSocketAddress) value);
                    } else {
                        NioDatagramWorker.disconnect(channel, future);
                    }
                    break;
                case INTEREST_OPS:
                    channel.worker.setInterestOps(channel, future, ((Integer) value).intValue());
                    break;
            }
        } else if (e instanceof MessageEvent) {
            // final MessageEvent event = (MessageEvent) e;
            // final boolean offered = parent.writeBufferQueue.offer(event);
            // assert offered;
            // parent.worker.writeFromUserCode(parent);
            //
            // In netty AbstractNioChannel#writeBufferQueue is thread-safe but not in mina.netty's copy.
            // Scheduling the write on parent channel thread since child channels and parent channel are
            // on different i/o threads
            NioDatagramChannel parent = (NioDatagramChannel) channel.getParent();

            class WriteTask implements Runnable {
                @Override
                public void run() {
                    Object message = ((MessageEvent) e).getMessage();
                    ChannelFuture parentFuture = parent.write(message, channel.getRemoteAddress());
                    parentFuture.addListener(f -> {
                        if (f.isSuccess()) {
                            future.setSuccess();
                        } else {
                            future.setFailure(f.getCause());
                        }
                    });
                }
            }

            parent.getWorker().executeInIoThread(new WriteTask());
        }
    }

    private static void close(NioDatagramChannel channel, ChannelFuture future) {
        System.out.println("childsink close");

        try {
            channel.getDatagramChannel().socket().close();
            if (channel.setClosed()) {
                future.setSuccess();
                if (channel.isBound()) {
                    fireChannelUnbound(channel);
                }
                fireChannelClosed(channel);
            } else {
                future.setSuccess();
            }
        } catch (final Throwable t) {
            future.setFailure(t);
            fireExceptionCaught(channel, t);
        }
    }

    /**
     * Will bind the DatagramSocket to the passed-in address.
     * Every call bind will spawn a new thread using the that basically in turn
     */
    private static void bind(final NioDatagramChannel channel,
                             final ChannelFuture future, final InetSocketAddress address) {
        System.out.println("childsink bind");

        boolean bound = false;
        boolean started = false;
        try {
            // First bind the DatagramSocket the specified port.
            channel.getDatagramChannel().socket().bind(address);
            bound = true;

            future.setSuccess();
            fireChannelBound(channel, address);

            channel.worker.register(channel, null);
            started = true;
        } catch (final Throwable t) {
            future.setFailure(t);
            fireExceptionCaught(channel, t);
        } finally {
            if (!started && bound) {
                close(channel, future);
            }
        }
    }

    private static void connect(
            NioDatagramChannel channel, ChannelFuture future,
            InetSocketAddress remoteAddress) {
System.out.println("childsink connect");
        boolean bound = channel.isBound();
        boolean connected = false;
        boolean workerStarted = false;

        future.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);

        // Clear the cached address so that the next getRemoteAddress() call
        // updates the cache.
        channel.remoteAddress = null;

        try {
            channel.getDatagramChannel().connect(remoteAddress);
            connected = true;

            // Fire events.
            future.setSuccess();
            if (!bound) {
                fireChannelBound(channel, channel.getLocalAddress());
            }
            fireChannelConnected(channel, channel.getRemoteAddress());

            if (!bound) {
                channel.worker.register(channel, future);
            }

            workerStarted = true;
        } catch (Throwable t) {
            future.setFailure(t);
            fireExceptionCaught(channel, t);
        } finally {
            if (connected && !workerStarted) {
                channel.worker.close(channel, future);
            }
        }
    }

    void closeRequested(ChannelPipeline pipeline, ChannelEvent evt) throws Exception {
        if (!evt.getFuture().isSuccess()) {
            NioDatagramChannel parent = (NioDatagramChannel) evt.getChannel().getParent();
            ConnectionlessServerBootstrap.ConnectionlessParentChannelHandler handler = (ConnectionlessServerBootstrap.ConnectionlessParentChannelHandler) parent.getPipeline().getLast();
            handler.closeChildChannel((NioDatagramChannel) evt.getChannel());
        }
    }

    NioDatagramWorker nextWorker() {
        return workerPool.nextWorker();
    }

}