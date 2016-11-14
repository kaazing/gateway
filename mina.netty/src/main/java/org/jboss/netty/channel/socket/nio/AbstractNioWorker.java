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

import static org.jboss.netty.channel.Channels.fireChannelClosed;
import static org.jboss.netty.channel.Channels.fireChannelClosedLater;
import static org.jboss.netty.channel.Channels.fireChannelDisconnected;
import static org.jboss.netty.channel.Channels.fireChannelDisconnectedLater;
import static org.jboss.netty.channel.Channels.fireChannelInterestChanged;
import static org.jboss.netty.channel.Channels.fireChannelInterestChangedLater;
import static org.jboss.netty.channel.Channels.fireChannelUnbound;
import static org.jboss.netty.channel.Channels.fireChannelUnboundLater;
import static org.jboss.netty.channel.Channels.fireExceptionCaught;
import static org.jboss.netty.channel.Channels.fireExceptionCaughtLater;
import static org.jboss.netty.channel.Channels.fireWriteCompleteLater;
import static org.jboss.netty.channel.Channels.succeededFuture;
import static org.kaazing.mina.netty.config.InternalSystemProperty.UDP_CHANNEL_BUFFER_SIZE;
import static uk.co.real_logic.agrona.concurrent.ringbuffer.RingBufferDescriptor.TRAILER_LENGTH;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.socket.Worker;
import org.jboss.netty.channel.socket.nio.NioWorker.ReadDispatcher;
import org.jboss.netty.channel.socket.nio.NioWorker.TcpReadDispatcher;
import org.jboss.netty.channel.socket.nio.NioWorker.UdpReadDispatcher;
import org.jboss.netty.channel.socket.nio.SocketSendBufferPool.SendBuffer;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.util.ThreadNameDeterminer;
import org.jboss.netty.util.ThreadRenamingRunnable;

import org.kaazing.mina.netty.channel.DefaultWriteCompletionEventEx;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.collections.Int2ObjectHashMap;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.agrona.concurrent.ringbuffer.OneToOneRingBuffer;

public abstract class AbstractNioWorker extends AbstractNioSelector implements Worker {
    private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(AbstractNioWorker.class);

    private final int UDP_CHANNEL_BUFFER_SIZE_PER_WORKER
            = UDP_CHANNEL_BUFFER_SIZE.getIntProperty(System.getProperties());

    protected final SocketReceiveBufferAllocator recvBufferPool = new SocketReceiveBufferAllocator();
    protected final SocketSendBufferPool sendBufferPool = new SocketSendBufferPool();
    private final DefaultWriteCompletionEventEx writeCompletionEvent = new DefaultWriteCompletionEventEx();

    private final OneToOneRingBuffer ringBuffer;
    private final Int2ObjectHashMap<Channel> channels;
    private final AtomicBuffer atomicBuffer = new UnsafeBuffer(new byte[0]);

    AbstractNioWorker(Executor executor) {
        this(executor, null);
    }

    AbstractNioWorker(Executor executor, ThreadNameDeterminer determiner) {
        super(executor, determiner);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(UDP_CHANNEL_BUFFER_SIZE_PER_WORKER + TRAILER_LENGTH);
        ringBuffer = new OneToOneRingBuffer(new UnsafeBuffer(byteBuffer));
        channels = new Int2ObjectHashMap<>();
    }

    @Override
    public void executeInIoThread(Runnable task) {
        executeInIoThread(task, false);
    }

    /**
     * Execute the {@link Runnable} in a IO-Thread
     *
     * @param task
     *            the {@link Runnable} to execute
     * @param alwaysAsync
     *            {@code true} if the {@link Runnable} should be executed
     *            in an async fashion even if the current Thread == IO Thread
     */
    public void executeInIoThread(Runnable task, boolean alwaysAsync) {
        if (!alwaysAsync && isIoThread()) {
            task.run();
        } else {
            registerTask(task);
        }
    }

    @Override
    protected void close(SelectionKey k) {
        AbstractNioChannel<?> ch = (AbstractNioChannel<?>) k.attachment();
        close(ch, succeededFuture(ch));
    }

    @Override
    protected ThreadRenamingRunnable newThreadRenamingRunnable(int id, ThreadNameDeterminer determiner) {
        return new ThreadRenamingRunnable(this, "New I/O worker #" + id, determiner);
    }

    @Override
    public void run() {
        super.run();
        sendBufferPool.releaseExternalResources();
        recvBufferPool.releaseExternalResources();
    }

    @Override
    protected void process(Selector selector) throws IOException {
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        // check if the set is empty and if so just return to not create garbage by
        // creating a new Iterator every time even if there is nothing to process.
        // See https://github.com/netty/netty/issues/597
        if (selectedKeys.isEmpty()) {
            return;
        }
        boolean perfLogEnabled = PERF_LOGGER.isDebugEnabled();
        long startProcess = perfLogEnabled ? System.nanoTime() : 0;
        long numReads = 0;
        long numWrites = 0;
        for (Iterator<SelectionKey> i = selectedKeys.iterator(); i.hasNext();) {
            SelectionKey k = i.next();
            i.remove();
            try {
                int readyOps = k.readyOps();
                if ((readyOps & SelectionKey.OP_READ) != 0 || readyOps == 0) {
                    numReads++;
                    if (!read(k)) {
                        // Connection already closed - no need to handle write.
                        continue;
                    }
                }
                if ((readyOps & SelectionKey.OP_WRITE) != 0) {
                    numWrites++;
                    writeFromSelectorLoop(k);
                }
            } catch (CancelledKeyException e) {
                close(k);
            }

            if (cleanUpCancelledKeys()) {
                break; // break the loop to avoid ConcurrentModificationException
            }
        }
        if (perfLogEnabled) {
            long totalTime = System.nanoTime() - startProcess;
            if (totalTime >= LATENCY_BEFORE_LOG_PROCESS_SELECT) {
                PERF_LOGGER.debug(String.format("AbstractNioWorker.process(Selector) took %d ms: %d reads, %d writes",
                        TimeUnit.NANOSECONDS.toMillis(totalTime), numReads, numWrites));
            }
        }
    }

    void writeFromUserCode(final AbstractNioChannel<?> channel) {
        if (!channel.isConnected()) {
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

    void writeFromTaskLoop(AbstractNioChannel<?> ch) {
        if (!ch.writeSuspended) {
            write0(ch);
        }
    }

    void writeFromSelectorLoop(final SelectionKey k) {
        AbstractNioChannel<?> ch = (AbstractNioChannel<?>) k.attachment();
        ch.writeSuspended = false;
        write0(ch);
    }

    protected abstract boolean scheduleWriteIfNecessary(AbstractNioChannel<?> channel);

    protected void write0(AbstractNioChannel<?> channel) {
        boolean open = true;
        boolean addOpWrite = false;
        boolean removeOpWrite = false;
        boolean iothread = isIoThread(channel);

        long writtenBytes = 0;

        final SocketSendBufferPool sendBufferPool = this.sendBufferPool;
        final WritableByteChannel ch = channel.channel;
        final Queue<MessageEvent> writeBuffer = channel.writeBufferQueue;
        final int writeSpinCount = channel.getConfig().getWriteSpinCount();
        List<Throwable> causes = null;

        synchronized (channel.writeLock) {
            channel.inWriteNowLoop = true;
            for (;;) {

                MessageEvent evt = channel.currentWriteEvent;
                SendBuffer buf = null;
                ChannelFuture future = null;
                try {
                    if (evt == null) {
                        if ((channel.currentWriteEvent = evt = writeBuffer.poll()) == null) {
                            removeOpWrite = true;
                            channel.writeSuspended = false;
                            break;
                        }
                        future = evt.getFuture();

                        channel.currentWriteBuffer = buf = sendBufferPool.acquire(channel, evt.getMessage());
                    } else {
                        future = evt.getFuture();
                        buf = channel.currentWriteBuffer;
                    }

                    long localWrittenBytes = 0;
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

                    if (buf.finished()) {
                        // Successful write - proceed to the next message.
                        buf.release();
                        channel.currentWriteEvent = null;
                        channel.currentWriteBuffer = null;
                        // Mark the event object for garbage collection.
                        //noinspection UnusedAssignment
                        evt = null;
                        buf = null;
                        future.setSuccess();
                    } else {
                        // Not written fully - perhaps the kernel buffer is full.
                        addOpWrite = true;
                        channel.writeSuspended = true;

                        if (writtenBytes > 0) {
                            // Notify progress listeners if necessary.
                            future.setProgress(
                                    localWrittenBytes,
                                    buf.writtenBytes(), buf.totalBytes());
                        }
                        break;
                    }
                } catch (AsynchronousCloseException e) {
                    // Doesn't need a user attention - ignore.
                } catch (Throwable t) {
                    if (buf != null) {
                        buf.release();
                    }
                    channel.currentWriteEvent = null;
                    channel.currentWriteBuffer = null;
                    // Mark the event object for garbage collection.
                    //noinspection UnusedAssignment
                    buf = null;
                    //noinspection UnusedAssignment
                    evt = null;
                    if (future != null) {
                        future.setFailure(t);
                    }
                    if (iothread) {
                        // An exception was thrown from within a write in the iothread. We store a reference to it
                        // in a list for now and notify the handlers in the chain after the writeLock was released
                        // to prevent possible deadlock.
                        // See #1310
                        if (causes == null) {
                            causes = new ArrayList<>(1);
                        }
                        causes.add(t);
                    } else {
                        fireExceptionCaughtLater(channel, t);
                    }
                    if (t instanceof IOException) {
                        // close must be handled from outside the write lock to fix a possible deadlock
                        // which can happen when MemoryAwareThreadPoolExecutor is used and the limit is exceed
                        // and a close is triggered while the lock is hold. This is because the close(..)
                        // may try to submit a task to handle it via the ExecutorHandler which then deadlocks.
                        // See #1310
                        open = false;
                    }
                }
            }
            channel.inWriteNowLoop = false;

            // Initially, the following block was executed after releasing
            // the writeLock, but there was a race condition, and it has to be
            // executed before releasing the writeLock:
            //
            //     https://issues.jboss.org/browse/NETTY-410
            //
            if (open) {
                if (addOpWrite) {
                    setOpWrite(channel);
                } else if (removeOpWrite) {
                    clearOpWrite(channel);
                }
            }
        }
        if (causes != null) {
            for (Throwable cause: causes) {
                // notify about cause now as it was triggered in the write loop
                fireExceptionCaught(channel, cause);
            }
        }
        if (!open) {
            // close the channel now
            close(channel, succeededFuture(channel));
        }
        if (iothread) {
            if (writtenBytes > 0) {
                // note: avoid re-allocation of write completion events
                writeCompletionEvent.init(channel, writtenBytes);
                channel.getPipeline().sendUpstream(writeCompletionEvent);
            }
        } else {
            fireWriteCompleteLater(channel, writtenBytes);
        }
    }

    static boolean isIoThread(AbstractNioChannel<?> channel) {
        AbstractNioSelector worker = channel.worker;
        return worker != null && Thread.currentThread() == worker.thread;
    }

    protected void setOpWrite(AbstractNioChannel<?> channel) {
        Selector selector = this.selector;
        SelectionKey key = channel.channel.keyFor(selector);
        if (key == null) {
            return;
        }
        if (!key.isValid()) {
            close(key);
            return;
        }

        int interestOps = channel.getInternalInterestOps();
        if ((interestOps & SelectionKey.OP_WRITE) == 0) {
            interestOps |= SelectionKey.OP_WRITE;
            key.interestOps(interestOps);
            channel.setInternalInterestOps(interestOps);
        }
    }

    protected void clearOpWrite(AbstractNioChannel<?> channel) {
        Selector selector = this.selector;
        SelectionKey key = channel.channel.keyFor(selector);
        if (key == null) {
            return;
        }
        if (!key.isValid()) {
            close(key);
            return;
        }

        int interestOps = channel.getInternalInterestOps();
        if ((interestOps & SelectionKey.OP_WRITE) != 0) {
            interestOps &= ~SelectionKey.OP_WRITE;
            key.interestOps(interestOps);
            channel.setInternalInterestOps(interestOps);
        }
    }

    protected void close(AbstractNioChannel<?> channel, ChannelFuture future) {
        boolean connected = channel.isConnected();
        boolean bound = channel.isBound();
        boolean iothread = isIoThread(channel);

        try {
            channel.channel.close();
            increaseCancelledKeys();

            if (channel.setClosed()) {
                future.setSuccess();
                if (connected) {
                    if (iothread) {
                        fireChannelDisconnected(channel);
                    } else {
                        fireChannelDisconnectedLater(channel);
                    }
                }
                if (bound) {
                    if (iothread) {
                        fireChannelUnbound(channel);
                    } else {
                        fireChannelUnboundLater(channel);
                    }
                }

                cleanUpWriteBuffer(channel);
                if (iothread) {
                    fireChannelClosed(channel);
                } else {
                    fireChannelClosedLater(channel);
                }
            } else {
                future.setSuccess();
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

    protected static void cleanUpWriteBuffer(AbstractNioChannel<?> channel) {
        Exception cause = null;
        boolean fireExceptionCaught = false;

        // Clean up the stale messages in the write buffer.
        synchronized (channel.writeLock) {
            MessageEvent evt = channel.currentWriteEvent;
            if (evt != null) {
                // Create the exception only once to avoid the excessive overhead
                // caused by fillStackTrace.
                if (channel.isOpen()) {
                    cause = new NotYetConnectedException();
                } else {
                    cause = new ClosedChannelException();
                }

                ChannelFuture future = evt.getFuture();
                if (channel.currentWriteBuffer != null) {
                    channel.currentWriteBuffer.release();
                    channel.currentWriteBuffer = null;
                }
                channel.currentWriteEvent = null;
                // Mark the event object for garbage collection.
                //noinspection UnusedAssignment
                evt = null;
                future.setFailure(cause);
                fireExceptionCaught = true;
            }

            Queue<MessageEvent> writeBuffer = channel.writeBufferQueue;
            for (;;) {
                evt = writeBuffer.poll();
                if (evt == null) {
                    break;
                }
                // Create the exception only once to avoid the excessive overhead
                // caused by fillStackTrace.
                if (cause == null) {
                    if (channel.isOpen()) {
                        cause = new NotYetConnectedException();
                    } else {
                        cause = new ClosedChannelException();
                    }
                    fireExceptionCaught = true;
                }
                evt.getFuture().setFailure(cause);
            }
        }

        if (fireExceptionCaught) {
            if (isIoThread(channel)) {
                fireExceptionCaught(channel, cause);
            } else {
                fireExceptionCaughtLater(channel, cause);
            }
        }
    }

    void setInterestOps(final AbstractNioChannel<?> channel, final ChannelFuture future, final int interestOps) {
        boolean iothread = isIoThread(channel);
        if (!iothread) {
            channel.getPipeline().execute(new Runnable() {
                @Override
                public void run() {
                    setInterestOps(channel, future, interestOps);
                }
            });
            return;
        }

        boolean changed = false;
        try {
            Selector selector = this.selector;
            SelectionKey key = channel.channel.keyFor(selector);

            // Override OP_WRITE flag - a user cannot change this flag.
            int newInterestOps = interestOps & ~Channel.OP_WRITE | channel.getInternalInterestOps() & Channel.OP_WRITE;

            if (key == null || selector == null) {
                if (channel.getInternalInterestOps() != newInterestOps) {
                    changed = true;
                }

                // Not registered to the worker yet.
                // Set the rawInterestOps immediately; RegisterTask will pick it up.
                channel.setInternalInterestOps(newInterestOps);

                future.setSuccess();
                if (changed) {
                    if (iothread) {
                        fireChannelInterestChanged(channel);
                    } else {
                        fireChannelInterestChangedLater(channel);
                    }
                }

                return;
            }

            if (channel.getInternalInterestOps() != newInterestOps) {
                changed = true;
                key.interestOps(newInterestOps);
                if (Thread.currentThread() != thread &&
                    wakenUp.compareAndSet(false, true)) {
                    selector.wakeup();
                }
                channel.setInternalInterestOps(newInterestOps);
            }

            future.setSuccess();
            if (changed) {
                fireChannelInterestChanged(channel);
            }
        } catch (CancelledKeyException e) {
            // setInterestOps() was called on a closed channel.
            ClosedChannelException cce = new ClosedChannelException();
            future.setFailure(cce);
            fireExceptionCaught(channel, cce);
        } catch (Throwable t) {
            future.setFailure(t);
            fireExceptionCaught(channel, t);
        }
    }

    /**
     * Read is called when a Selector has been notified that the underlying channel
     * was something to be read. The channel would previously have registered its interest
     * in read operations.
     *
     * @param k The selection key which contains the Selector registration information.
     */
    protected abstract boolean read(SelectionKey k);

    public void deregister(final AbstractNioChannel<?> channel) {
        // avoid modifying selected keys from process(select)
        // use processTasks instead
        registerTask(new Runnable() {
            @Override
            public void run() {
                channels.remove(channel.getId().intValue());
                if (channel instanceof NioChildDatagramChannel) {
                    return;
                }

                SelectionKey key = channel.channel.keyFor(selector);
                if (key != null) {
                    key.cancel();
                    increaseCancelledKeys();
                    try {
                        selector.selectNow();
                    } catch (IOException e) {
                        // Ignore
                    }

                    // wake up selector if necessary, to avoid selector timeout stall
                    if (wakenUp.compareAndSet(false, true)) {
                        selector.wakeup();
                    }
                }
            }
        });
    }

    public void register(final AbstractNioChannel<?> channel) {
        // avoid modifying selected keys from process(select)
        // use processTasks instead
        registerTask(new Runnable() {

            @Override
            public void run() {
                try {
                    channels.put(channel.getId().intValue(), channel);

                    if (channel instanceof NioChildDatagramChannel) {
                        return;
                    }

                    // ensure channel.writeSuspended cannot remain true due to race
                    // note: setOpWrite is a no-op before selectionKey is registered w/ selector
                    int interestOps = channel.getInternalInterestOps();
                    interestOps |= SelectionKey.OP_WRITE;
                    channel.setInternalInterestOps(interestOps);
                    ReadDispatcher readDispatcher = channel instanceof NioSocketChannel
                            ? new TcpReadDispatcher((NioSocketChannel) channel)
                            : new UdpReadDispatcher((NioDatagramChannel) channel);
                    channel.channel.register(selector, interestOps, readDispatcher);
                }
                catch (ClosedChannelException e) {
                    close(channel, succeededFuture(channel));
                }
            }
        });
    }

    // This method is called from the boss thread
    public void messageReceived(AbstractNioChannel<?> channel, Object message) {
        assert channel.getId() >= 0;

        ChannelBuffer buf = (ChannelBuffer) message;
        ByteBuffer byteBuffer = buf.toByteBuffer();
        atomicBuffer.wrap(byteBuffer);
        // If there is no space in the ring buffer,  the message would be dropped (ok for UDP)
        boolean written = ringBuffer.write(channel.getId(), atomicBuffer, 0, atomicBuffer.capacity());
        if (LOGGER.isDebugEnabled() && !written) {
            LOGGER.debug(String.format("Message %s for channel %s is not written to ring buffer", message, channel));
        }

        // Wake up the selector so the event gets processed immediately by the worker thread
        if (selector != null) {
            if (wakenUp.compareAndSet(false, true)) {
                selector.wakeup();
            }
        }
    }

    protected void processRead() throws IOException {
        if (ringBuffer == null) {
            return;
        }
        ringBuffer.read(this::handleRead);
    }

    private void handleRead(int msgTypeId, DirectBuffer buffer, int index, int length) {
        Channel childChannel = channels.get(msgTypeId);
        if (childChannel == null) {
            // register task may still be in the task queue, so this gives the register task a chance
            // to complete rather than effectively dropping the UDP packet.
            processTaskQueue();
            childChannel = channels.get(msgTypeId);
        }

        if (childChannel != null) {
            final ChannelBufferFactory bufferFactory = childChannel.getConfig().getBufferFactory();
            ByteBuffer byteBuffer = recvBufferPool.get(length).order(bufferFactory.getDefaultOrder());
            buffer.getBytes(index, byteBuffer, length);
            byteBuffer.flip();

            ChannelBuffer channelBuffer = bufferFactory.getBuffer(byteBuffer);

            Channels.fireMessageReceived(childChannel, channelBuffer);
        } else if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("There is no channel %s found, so dropping message %s", msgTypeId, buffer));
        }
    }
}
