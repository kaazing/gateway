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

import static org.jboss.netty.channel.Channels.fireWriteComplete;
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
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.MessageEvent;
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
        ReadDispatcher dispatcher = (ReadDispatcher) k.attachment();
        return dispatcher.dispatch(this, k);
    }

    private boolean readTcp(SelectionKey k, NioSocketChannel channel) {
        final SocketChannel ch = (SocketChannel) k.channel();
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
            return SelectorUtil.select(selector, 10L);
        }
    }

    @Override
    protected Runnable createRegisterTask(Channel channel, ChannelFuture future) {
        if (channel instanceof NioSocketChannel) {
            boolean server = !(channel instanceof NioClientSocketChannel);
            return new TcpChannelRegisterTask((NioSocketChannel) channel, future, server);
        } else {
            return new UdpChannelRegistionTask((NioDatagramChannel) channel, future);
        }
    }

    private final class TcpChannelRegisterTask implements Runnable {
        private final NioSocketChannel channel;
        private final ChannelFuture future;
        private final boolean server;

        TcpChannelRegisterTask(
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
                        selector, channel.getInternalInterestOps(), new TcpReadDispatcher(channel));

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

    @Override
    protected void write0(final AbstractNioChannel<?> channel) {
        if (channel instanceof NioSocketChannel || channel instanceof NioChildDatagramChannel) {
            super.write0(channel);
        } else {
            write0Udp(channel);
        }
    }

    @Override
    void writeFromUserCode(final AbstractNioChannel<?> channel) {
        if (channel instanceof NioDatagramChannel) {
            writeFromUserCodeUdp(channel);
        } else {
            super.writeFromUserCode(channel);
        }
    }

    @Override
    protected void close(SelectionKey k) {
        ReadDispatcher dispatcher = (ReadDispatcher) k.attachment();
        AbstractNioChannel<?> ch = dispatcher.channel();
        close(ch, succeededFuture(ch));
    }

    @Override
    void writeFromSelectorLoop(final SelectionKey k) {
        ReadDispatcher dispatcher = (ReadDispatcher) k.attachment();
        AbstractNioChannel<?> ch = dispatcher.channel();
        ch.writeSuspended = false;
        write0(ch);
    }

    //
    // ---------------- UDP worker -----------------------
    //
    private boolean readUdp(final SelectionKey key, NioDatagramChannel channel) {
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

    /**
     * RegisterTask is a task responsible for registering a channel with a
     * selector.
     */
    private final class UdpChannelRegistionTask implements Runnable {
        private final NioDatagramChannel channel;

        private final ChannelFuture future;

        UdpChannelRegistionTask(final NioDatagramChannel channel,
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
                        selector, channel.getInternalInterestOps(), new UdpReadDispatcher(channel));

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

    void writeFromUserCodeUdp(final AbstractNioChannel<?> channel) {
        assert channel instanceof NioDatagramChannel;

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

    private void write0Udp(final AbstractNioChannel<?> channel) {
        assert channel instanceof NioDatagramChannel;

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

    interface ReadDispatcher {
        AbstractNioChannel channel();
        boolean dispatch(NioWorker worker, SelectionKey key);
    }

    static final class TcpReadDispatcher implements ReadDispatcher {

        private final NioSocketChannel channel;

        TcpReadDispatcher(NioSocketChannel channel) {
            this.channel = channel;
        }

        @Override
        public AbstractNioChannel channel() {
            return channel;
        }

        @Override
        public boolean dispatch(NioWorker worker, SelectionKey key) {
            return worker.readTcp(key, channel);
        }
    }

    static final class UdpReadDispatcher implements ReadDispatcher {

        private final NioDatagramChannel channel;

        UdpReadDispatcher(NioDatagramChannel channel) {
            this.channel = channel;
        }

        @Override
        public AbstractNioChannel channel() {
            return channel;
        }

        @Override
        public boolean dispatch(NioWorker worker, SelectionKey key) {
            return worker.readUdp(key, channel);
        }
    }

}
