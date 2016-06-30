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

import static java.lang.String.format;
import static java.util.Collections.newSetFromMap;
import static org.jboss.netty.channel.Channels.fireChannelBound;
import static org.jboss.netty.channel.Channels.fireChannelClosed;
import static org.jboss.netty.channel.Channels.fireChannelUnbound;
import static org.jboss.netty.channel.Channels.fireExceptionCaught;
import static org.jboss.netty.channel.Channels.future;
import static org.jboss.netty.channel.Channels.succeededFuture;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelSink;
import org.jboss.netty.util.ThreadNameDeterminer;
import org.jboss.netty.util.ThreadRenamingRunnable;

/**
 * Boss implementation which handles accepting of new connections
 */
public final class NioServerBoss extends AbstractNioSelector
        implements Boss {

    private final Set<ChannelFuture> channelUnregisteredFutures = newSetFromMap(new ConcurrentHashMap<>());

    NioServerBoss(Executor bossExecutor) {
        super(bossExecutor);
    }

    NioServerBoss(Executor bossExecutor, ThreadNameDeterminer determiner) {
        super(bossExecutor, determiner);
    }

    void bind(final NioServerSocketChannel channel, final ChannelFuture future,
              final SocketAddress localAddress) {
        registerTask(new RegisterTask(channel, future, localAddress));
    }

    @Override
    protected void close(SelectionKey k) {
        NioServerSocketChannel ch = (NioServerSocketChannel) k.attachment();
        close(ch, succeededFuture(ch));
    }

    void close(final NioServerSocketChannel channel, final ChannelFuture closeFuture) {
        final boolean bound = channel.isBound();

        try {

            // Create the unregistered future prior to close for the case where the selector is woken immediately after
            // the physical close.
            ChannelFuture channelUnregisteredFuture = future(channel);
            channelUnregisteredFutures.add(channelUnregisteredFuture);

            channel.socket.close();

            increaseCancelledKeys();

            // Wake up selector if not already woken so that it can complete the close process. Note this is needed for
            // Window's where the actual close does not occur until after the selection key is unregistered
            if (wakenUp.compareAndSet(false, true)) {
                selector.wakeup();
            }

            // Set a listener to mark the closeFuture done once the close is complete.
            channelUnregisteredFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        try {
                            if (channel.setClosed()) {
                                closeFuture.setSuccess();
                                if (bound) {
                                    fireChannelUnbound(channel);
                                }
                                fireChannelClosed(channel);
                            } else {
                                closeFuture.setSuccess();
                            }
                        } catch (Throwable t) {
                            closeFuture.setFailure(t);
                            fireExceptionCaught(channel, t);
                        }
                    } else {
                        closeFuture.setFailure(future.getCause());
                    }
                }

            });
        } catch (Throwable t) {
            closeFuture.setFailure(t);
            fireExceptionCaught(channel, t);
        }
    }

    @Override
    protected void process(Selector selector) {
        Iterator<ChannelFuture> iter = channelUnregisteredFutures.iterator();
        if (iter.hasNext()) {
            boolean isDebugEnabled = logger.isDebugEnabled();
            // On Window's the socket's FD does not actually close until the selection key is removed
            loop: do {
                ChannelFuture future = iter.next();
                Channel c = future.getChannel();
                for (SelectionKey key : selector.keys()) {
                    // Edge case where the selector wakes up early. Note this can occur, after the future is created in
                    // the close method above and the boss thread wake's up before ServerSocketChannel.close() cancels
                    // the
                    // keys and has returned. In this case the selector will return immediately on the next call to
                    // select and the key won't be here on the next iteration.
                    if (key.attachment() == c) {
                        if (isDebugEnabled) {
                            logger.debug(format(
                                    "Found channel selector key for channel %s still in selector key set. Waiting for next"
                                            + " iteration when it is removed to mark the unregister future complete.", c));
                        }
                        continue loop;
                    }
                }
                future.setSuccess();
                iter.remove();
            } while (iter.hasNext());
        }

        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        if (selectedKeys.isEmpty()) {
            return;
        }
        for (Iterator<SelectionKey> i = selectedKeys.iterator(); i.hasNext();) {
            SelectionKey k = i.next();
            i.remove();
            NioServerSocketChannel channel = (NioServerSocketChannel) k.attachment();

            try {
                // accept connections in a for loop until no new connection is ready
                for (;;) {
                    SocketChannel acceptedSocket = channel.socket.accept();
                    if (acceptedSocket == null) {
                        break;
                    }
                    registerAcceptedChannel(channel, acceptedSocket, thread);
                }
            } catch (CancelledKeyException e) {
                // Raised by accept() when the server socket was closed.
                k.cancel();
                channel.close();
            } catch (SocketTimeoutException e) {
                // Thrown every second to get ClosedChannelException
                // raised.
            } catch (ClosedChannelException e) {
                // Closed as requested.
            } catch (Throwable t) {
                if (logger.isWarnEnabled()) {
                    logger.warn(
                            "Failed to accept a connection.", t);
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    // Ignore
                }
            }
        }
    }

    private static void registerAcceptedChannel(NioServerSocketChannel parent, SocketChannel acceptedSocket,
                                         Thread currentThread) {
        try {
            ChannelSink sink = parent.getPipeline().getSink();
            ChannelPipeline pipeline =
                    parent.getConfig().getPipelineFactory().getPipeline();
            NioWorker worker = parent.workerPool.nextWorker();
            worker.register(new NioAcceptedSocketChannel(
                    parent.getFactory(), pipeline, parent, sink
                    , acceptedSocket,
                    worker, currentThread), null);
        } catch (Exception e) {
            if (logger.isWarnEnabled()) {
                logger.warn(
                        "Failed to initialize an accepted socket.", e);
            }

            try {
                acceptedSocket.close();
            } catch (IOException e2) {
                if (logger.isWarnEnabled()) {
                    logger.warn(
                            "Failed to close a partially accepted socket.",
                            e2);
                }
            }
        }
    }

    @Override
    protected int select(Selector selector) throws IOException {
        // Just do a blocking select without any timeout
        // as this thread does not execute anything else.
        return selector.select();
    }

    @Override
    protected ThreadRenamingRunnable newThreadRenamingRunnable(int id, ThreadNameDeterminer determiner) {
        return new ThreadRenamingRunnable(this,
                "New I/O server boss #" + id, determiner);
    }

    @Override
    protected Runnable createRegisterTask(Channel channel, ChannelFuture future) {
        return new RegisterTask((NioServerSocketChannel) channel, future, null);
    }

    private final class RegisterTask implements Runnable {
        private final NioServerSocketChannel channel;
        private final ChannelFuture future;
        private final SocketAddress localAddress;

        public RegisterTask(final NioServerSocketChannel channel, final ChannelFuture future,
                            final SocketAddress localAddress) {
            this.channel = channel;
            this.future = future;
            this.localAddress = localAddress;
        }

        @Override
        public void run() {
            boolean bound = false;
            boolean registered = false;
            try {
                channel.socket.socket().bind(localAddress, channel.getConfig().getBacklog());
                bound = true;

                future.setSuccess();
                fireChannelBound(channel, channel.getLocalAddress());
                channel.socket.register(selector, SelectionKey.OP_ACCEPT, channel);

                registered = true;
            } catch (Throwable t) {
                future.setFailure(t);
                fireExceptionCaught(channel, t);
            } finally {
                if (!registered && bound) {
                    close(channel, future);
                }
            }
        }
    }
}
