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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.ReceiveBufferSizePredictor;
import org.jboss.netty.util.ThreadNameDeterminer;
import org.jboss.netty.util.ThreadRenamingRunnable;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Queue;
import java.util.concurrent.Executor;

import static org.jboss.netty.channel.Channels.*;

/**
 * A class responsible for registering channels with {@link Selector}.
 * It also implements the {@link Selector} loop.
 */
public class NioServerDatagramBoss extends AbstractNioWorker /* AbstractNioSelector */ implements Boss {

    /**
     * Sole constructor.
     *
     * @param executor the {@link Executor} used to execute {@link Runnable}s
     *                 such as {@link ChannelRegistionTask}
     */
    NioServerDatagramBoss(final Executor executor) {
        super(executor);
    }

    @Override
    protected boolean read(final SelectionKey key) {
        final NioDatagramChannel channel = (NioDatagramChannel) key.attachment();
        ReceiveBufferSizePredictor predictor =
                channel.getConfig().getReceiveBufferSizePredictor();
        final ChannelBufferFactory bufferFactory = channel.getConfig().getBufferFactory();
        final DatagramChannel nioChannel = (DatagramChannel) key.channel();
        final int predictedRecvBufSize = predictor.nextReceiveBufferSize();

        final ByteBuffer byteBuffer = recvBufferPool.get(predictedRecvBufSize).order(bufferFactory.getDefaultOrder());

        boolean failure = true;
        SocketAddress remoteAddress = null;
        try {
            // Receive from the channel in a non blocking mode. We have already been notified that
            // the channel is ready to receive.
            remoteAddress = nioChannel.receive(byteBuffer);
            failure = false;
        } catch (ClosedChannelException e) {
            // Can happen, and does not need a user attention.
        } catch (Throwable t) {
            fireExceptionCaught(channel, t);
        }

        if (remoteAddress != null) {
            // Flip the buffer so that we can wrap it.
            byteBuffer.flip();

            int readBytes = byteBuffer.remaining();
            if (readBytes > 0) {
                // Update the predictor.
                predictor.previousReceiveBufferSize(readBytes);

                final ChannelBuffer buffer = bufferFactory.getBuffer(readBytes);
                buffer.setBytes(0, byteBuffer);
                buffer.writerIndex(readBytes);

                // Update the predictor.
                predictor.previousReceiveBufferSize(readBytes);

                // Notify the interested parties about the newly arrived message.
                fireMessageReceived(
                        channel, buffer, remoteAddress);
            }
        }

        if (failure) {
            key.cancel(); // Some JDK implementations run into an infinite loop without this.
            close(channel, succeededFuture(channel));
            return false;
        }

        return true;
    }

    @Override
    protected boolean scheduleWriteIfNecessary(final AbstractNioChannel<?> channel) {
        final Thread workerThread = thread;
        if (workerThread == null || Thread.currentThread() != workerThread) {
            if (channel.writeTaskInTaskQueue.compareAndSet(false, true)) {
                // "add" the channels writeTask to the writeTaskQueue.
                registerTask(channel.writeTask);
            }
            return true;
        }

        return false;
    }

    static void disconnect(NioDatagramChannel channel, ChannelFuture future) {
        boolean connected = channel.isConnected();
        boolean iothread = isIoThread(channel);
        try {
            channel.getDatagramChannel().disconnect();
            future.setSuccess();
            if (connected) {
                if (iothread) {
                    fireChannelDisconnected(channel);
                } else {
                    fireChannelDisconnectedLater(channel);
                }
            }
        } catch (Throwable t) {
            future.setFailure(t);
            if (iothread) {
                fireExceptionCaught(channel, t);
            } else {
                fireExceptionCaughtLater(channel, t);
            }
        }
    }

    @Override
    protected Runnable createRegisterTask(Channel channel, ChannelFuture future) {
        return new ChannelRegistionTask((NioDatagramChannel) channel, future);
    }

    /**
     * RegisterTask is a task responsible for registering a channel with a
     * selector.
     */
    private final class ChannelRegistionTask implements Runnable {
        private final NioDatagramChannel channel;

        private final ChannelFuture future;

        ChannelRegistionTask(final NioDatagramChannel channel,
                             final ChannelFuture future) {
            this.channel = channel;
            this.future = future;
        }

        /**
         * This runnable's task. Does the actual registering by calling the
         * underlying DatagramChannels peer DatagramSocket register method.
         */
        public void run() {
            final SocketAddress localAddress = channel.getLocalAddress();
            final SocketAddress remoteAddress = channel.getRemoteAddress();
            if (localAddress == null) {
                if (future != null) {
                    future.setFailure(new ClosedChannelException());
                }
                close(channel, succeededFuture(channel));
                return;
            }

            try {
                channel.getDatagramChannel().register(
                        selector, channel.getInternalInterestOps(), channel);

                if (future != null) {
                    future.setSuccess();
                }
                // mina.netty change - similar to tcp, connected event is fired here instead
                // in NioDatagramPipelineSink. This means NioDatagramChannelIoSession is
                // created in the correct i/o thread
                fireChannelConnected(channel, remoteAddress);
            } catch (final IOException e) {
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
    public void writeFromUserCode(final AbstractNioChannel<?> channel) {
        /*
         * Note that we are not checking if the channel is connected. Connected
         * has a different meaning in UDP and means that the channels socket is
         * configured to only send and receive from a given remote peer.
         */
        if (!channel.isBound()) {
            cleanUpWriteBuffer(channel);
            return;
        }

        if (scheduleWriteIfNecessary(channel)) {
            return;
        }

        // From here, we are sure Thread.currentThread() == workerThread.

        if (channel.writeSuspended) {
            return;
        }

        if (channel.inWriteNowLoop) {
            return;
        }

        write0(channel);
    }

    @Override
    protected void write0(final AbstractNioChannel<?> channel) {

        boolean addOpWrite = false;
        boolean removeOpWrite = false;

        long writtenBytes = 0;

        final SocketSendBufferPool sendBufferPool = this.sendBufferPool;
        final DatagramChannel ch = ((NioDatagramChannel) channel).getDatagramChannel();
        final Queue<MessageEvent> writeBuffer = channel.writeBufferQueue;
        final int writeSpinCount = channel.getConfig().getWriteSpinCount();
        synchronized (channel.writeLock) {
            // inform the channel that write is in-progress
            channel.inWriteNowLoop = true;

            // loop forever...
            for (;;) {
                MessageEvent evt = channel.currentWriteEvent;
                SocketSendBufferPool.SendBuffer buf;
                if (evt == null) {
                    if ((channel.currentWriteEvent = evt = writeBuffer.poll()) == null) {
                        removeOpWrite = true;
                        channel.writeSuspended = false;
                        break;
                    }
                    // mina.netty change - similar to mina.netty's AbstractNioWorker, passing channel as parameter
                    channel.currentWriteBuffer = buf = sendBufferPool.acquire(channel, evt.getMessage());
                } else {
                    buf = channel.currentWriteBuffer;
                }

                try {
                    long localWrittenBytes = 0;
                    SocketAddress raddr = evt.getRemoteAddress();
                    if (raddr == null) {
                        for (int i = writeSpinCount; i > 0; i --) {
                            localWrittenBytes = buf.transferTo(ch);
                            if (localWrittenBytes != 0) {
                                writtenBytes += localWrittenBytes;
                                break;
                            }
                            if (buf.finished()) {
                                break;
                            }
                        }
                    } else {
                        for (int i = writeSpinCount; i > 0; i --) {
                            localWrittenBytes = buf.transferTo(ch, raddr);
                            if (localWrittenBytes != 0) {
                                writtenBytes += localWrittenBytes;
                                break;
                            }
                            if (buf.finished()) {
                                break;
                            }
                        }
                    }

                    if (localWrittenBytes > 0 || buf.finished()) {
                        // Successful write - proceed to the next message.
                        buf.release();
                        ChannelFuture future = evt.getFuture();
                        channel.currentWriteEvent = null;
                        channel.currentWriteBuffer = null;
                        evt = null;
                        buf = null;
                        future.setSuccess();
                    } else {
                        // Not written at all - perhaps the kernel buffer is full.
                        addOpWrite = true;
                        channel.writeSuspended = true;
                        break;
                    }
                } catch (final AsynchronousCloseException e) {
                    // Doesn't need a user attention - ignore.
                } catch (final Throwable t) {
                    buf.release();
                    ChannelFuture future = evt.getFuture();
                    channel.currentWriteEvent = null;
                    channel.currentWriteBuffer = null;
                    // Mark the event object for garbage collection.
                    //noinspection UnusedAssignment
                    buf = null;
                    //noinspection UnusedAssignment
                    evt = null;
                    future.setFailure(t);
                    fireExceptionCaught(channel, t);
                }
            }
            channel.inWriteNowLoop = false;

            // Initially, the following block was executed after releasing
            // the writeLock, but there was a race condition, and it has to be
            // executed before releasing the writeLock:
            //
            // https://issues.jboss.org/browse/NETTY-410
            //
            if (addOpWrite) {
                setOpWrite(channel);
            } else if (removeOpWrite) {
                clearOpWrite(channel);
            }
        }

        fireWriteComplete(channel, writtenBytes);
    }

    @Override
    protected ThreadRenamingRunnable newThreadRenamingRunnable(int id, ThreadNameDeterminer determiner) {
        return new ThreadRenamingRunnable(this, "New I/O udp boss #" + id, determiner);
    }

}
