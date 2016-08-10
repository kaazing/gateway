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

import static org.kaazing.mina.netty.config.InternalSystemProperty.MAXIMUM_PROCESS_TASKS_TIME;
import static java.lang.String.format;
import static org.jboss.netty.channel.Channels.fireChannelBound;
import static org.jboss.netty.channel.Channels.fireChannelConnected;
import static org.jboss.netty.channel.Channels.fireExceptionCaught;
import static org.jboss.netty.channel.Channels.fireMessageReceived;
import static org.jboss.netty.channel.Channels.succeededFuture;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ReceiveBufferSizePredictor;
import org.jboss.netty.util.ThreadNameDeterminer;

import org.kaazing.mina.netty.config.InternalSystemProperty;

public class NioWorker extends AbstractNioWorker {

    // Avoid static variables to facilitate unit tests
    private final long MAXIMUM_PROCESS_TASKS_TIME_MILLIS
    = MAXIMUM_PROCESS_TASKS_TIME.getLongProperty(System.getProperties());

    private final long MAXIMUM_PROCESS_TASKS_TIME_NANOS =
            TimeUnit.MILLISECONDS.toNanos(MAXIMUM_PROCESS_TASKS_TIME_MILLIS);

    private final long QUICK_SELECT_TIMEOUT =
            InternalSystemProperty.QUICK_SELECT_TIMEOUT.getLongProperty(System.getProperties());

    {
        // Always report when any of the tuning features are active, irrespective of log4j configuration
        if (MAXIMUM_PROCESS_TASKS_TIME_MILLIS > 0) {
            String message = format(
               "NioWorker: maximum task queue processing time = %d ms. Quick select timeout = %s.",
               MAXIMUM_PROCESS_TASKS_TIME_MILLIS, QUICK_SELECT_TIMEOUT == 0 ? "selectNow used" : QUICK_SELECT_TIMEOUT);
            if (PERF_LOGGER.isInfoEnabled()) {
                PERF_LOGGER.info(message);
            }
             else {
                System.out.println(message);
            }
        }
    }

    public NioWorker(Executor executor) {
        super(executor);
    }

    public NioWorker(Executor executor, ThreadNameDeterminer determiner) {
        super(executor, determiner);
    }

    @Override
    protected final long getMaximumProcessTaskQueueTimeNanos() {
        return MAXIMUM_PROCESS_TASKS_TIME_NANOS;
    }

    @Override
    protected boolean read(SelectionKey k) {
        final SocketChannel ch = (SocketChannel) k.channel();
        final NioSocketChannel channel = (NioSocketChannel) k.attachment();

        final ReceiveBufferSizePredictor predictor =
            channel.getConfig().getReceiveBufferSizePredictor();
        final int predictedRecvBufSize = predictor.nextReceiveBufferSize();
        final ChannelBufferFactory bufferFactory = channel.getConfig().getBufferFactory();

        int ret = 0;
        int readBytes = 0;
        boolean failure = true;

        ByteBuffer bb = recvBufferPool.get(predictedRecvBufSize).order(bufferFactory.getDefaultOrder());
        try {
            while ((ret = ch.read(bb)) > 0) {
                readBytes += ret;
                if (!bb.hasRemaining()) {
                    break;
                }
            }
            failure = false;
        } catch (ClosedChannelException e) {
            // Can happen, and does not need a user attention.
        } catch (Throwable t) {
            fireExceptionCaught(channel, t);
        }

        if (readBytes > 0) {
            bb.flip();

            final ChannelBuffer buffer = bufferFactory.getBuffer(readBytes);
            buffer.setBytes(0, bb);
            buffer.writerIndex(readBytes);

            // Update the predictor.
            predictor.previousReceiveBufferSize(readBytes);

            // Fire the event.
            fireMessageReceived(channel, buffer);
        }

        if (ret < 0 || failure) {
            k.cancel(); // Some JDK implementations run into an infinite loop without this.
            close(channel, succeededFuture(channel));
            return false;
        }

        return true;
    }

    @Override
    protected boolean scheduleWriteIfNecessary(final AbstractNioChannel<?> channel) {
        final Thread currentThread = Thread.currentThread();
        final Thread workerThread = thread;
        if (currentThread != workerThread) {
            if (channel.writeTaskInTaskQueue.compareAndSet(false, true)) {
                registerTask(channel.writeTask);
            }

            return true;
        }

        return false;
    }

    @Override
    protected int select(Selector selector, boolean quickSelect) throws IOException {
        if (quickSelect) {
            return SelectorUtil.select(selector, QUICK_SELECT_TIMEOUT);
        } else {
            return SelectorUtil.select(selector);
        }
    }

    @Override
    protected Runnable createRegisterTask(Channel channel, ChannelFuture future) {
        boolean server = !(channel instanceof NioClientSocketChannel);
        return new RegisterTask((NioSocketChannel) channel, future, server);
    }

    private final class RegisterTask implements Runnable {
        private final NioSocketChannel channel;
        private final ChannelFuture future;
        private final boolean server;

        RegisterTask(
                NioSocketChannel channel, ChannelFuture future, boolean server) {

            this.channel = channel;
            this.future = future;
            this.server = server;
        }

        @Override
        public void run() {
            SocketAddress localAddress = channel.getLocalAddress();
            SocketAddress remoteAddress = channel.getRemoteAddress();

            if (localAddress == null || remoteAddress == null) {
                if (future != null) {
                    future.setFailure(new ClosedChannelException());
                }
                close(channel, succeededFuture(channel));
                return;
            }

            try {
                if (server) {
                    channel.channel.configureBlocking(false);
                }

                channel.channel.register(
                        selector, channel.getRawInterestOps(), channel);

                if (future != null) {
                    channel.setConnected();
                    future.setSuccess();
                }

                if (server || !((NioClientSocketChannel) channel).boundManually) {
                    fireChannelBound(channel, localAddress);
                }
                fireChannelConnected(channel, remoteAddress);
            } catch (IOException e) {
                if (future != null) {
                    future.setFailure(e);
                }
                close(channel, succeededFuture(channel));
                if (!(e instanceof ClosedChannelException)) {
                    throw new ChannelException(
                            "Failed to register a socket to the selector.", e);
                }
            }
        }
    }

    @Override
    public void run() {
        super.run();
        recvBufferPool.releaseExternalResources();
    }
}
