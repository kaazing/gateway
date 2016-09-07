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

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;

/**
 * Receives downstream events from a {@link ChannelPipeline}.  It contains
 * an array of I/O workers.
 */
class NioDatagramPipelineSink extends AbstractNioChannelSink {

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
                        NioServerDatagramBoss.disconnect(channel, future);
                    }
                    break;
                case INTEREST_OPS:
                    channel.worker.setInterestOps(channel, future, ((Integer) value).intValue());
                    break;
            }
        } else if (e instanceof MessageEvent) {
            final MessageEvent event = (MessageEvent) e;
            final boolean offered = channel.writeBufferQueue.offer(event);
            assert offered;
            channel.worker.writeFromUserCode(channel);
        }
    }

    private static void close(NioDatagramChannel channel, ChannelFuture future) {
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
            // mina.netty change - moved to NioDatagramWorker so that it is
            // fired in the correct thread
            // fireChannelConnected(channel, channel.getRemoteAddress());

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

}
