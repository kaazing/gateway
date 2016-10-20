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

import static org.jboss.netty.channel.Channels.fireChannelInterestChanged;

import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.AbstractChannel;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelSink;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.socket.nio.SocketSendBufferPool.SendBuffer;
import org.jboss.netty.util.internal.ThreadLocalBoolean;

abstract class AbstractNioChannel<C extends SelectableChannel & WritableByteChannel> extends AbstractChannel {

    /**
     * The {@link AbstractNioWorker}.
     */
    volatile AbstractNioWorker worker;

    /**
     * Monitor object for synchronizing access to the {@link WriteRequestQueue}.
     */
    final Object writeLock = new Object();

    /**
     * WriteTask that performs write operations.
     */
    final Runnable writeTask = new WriteTask();

    /**
     * Indicates if there is a {@link WriteTask} in the task queue.
     */
    final AtomicBoolean writeTaskInTaskQueue = new AtomicBoolean();

    /**
     * Queue of write {@link MessageEvent}s.
     */
    final Queue<MessageEvent> writeBufferQueue;

    /**
     * Keeps track of the number of bytes that the {@link WriteRequestQueue} currently
     * contains.
     */
    final AtomicInteger writeBufferSize = new AtomicInteger();

    /**
     * Keeps track of the highWaterMark.
     */
    final AtomicInteger highWaterMarkCounter = new AtomicInteger();

    /**
     * The current write {@link MessageEvent}
     */
    MessageEvent currentWriteEvent;
    SendBuffer currentWriteBuffer;

    SendBuffer sharedUnpooled;
    SendBuffer sharedPooled;

    /**
     * Boolean that indicates that write operation is in progress.
     */
    boolean inWriteNowLoop;
    boolean writeSuspended;

    volatile InetSocketAddress localAddress;
    volatile InetSocketAddress remoteAddress;

    final C channel;

    protected AbstractNioChannel(
            Integer id, Channel parent, ChannelFactory factory, ChannelPipeline pipeline,
            ChannelSink sink, AbstractNioWorker worker, C ch) {
        super(id, parent, factory, pipeline, sink);
        this.worker = worker;
        channel = ch;
        writeBufferQueue = new WriteRequestQueue(new ArrayDeque<>(16));
    }

    protected AbstractNioChannel(
            Channel parent, ChannelFactory factory,
            ChannelPipeline pipeline, ChannelSink sink, AbstractNioWorker worker, C ch)  {
        this(parent, factory, pipeline, sink, worker, ch, false);
    }

    protected AbstractNioChannel(
            Channel parent, ChannelFactory factory,
            ChannelPipeline pipeline, ChannelSink sink, AbstractNioWorker worker, C ch, boolean concurrent)  {
        super(parent, factory, pipeline, sink);
        this.worker = worker;
        channel = ch;
        // note: ArrayDeque for cases where we always write from the same I/O worker thread
        writeBufferQueue = new WriteRequestQueue(concurrent ? new ConcurrentLinkedQueue<>() : new ArrayDeque<>(16));
    }

    /**
     * Return the {@link AbstractNioWorker} that handle the IO of the
     * {@link AbstractNioChannel}
     *
     * @return worker
     */
    public AbstractNioWorker getWorker() {
        return worker;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        InetSocketAddress localAddress = this.localAddress;
        if (localAddress == null) {
            try {
                localAddress = getLocalSocketAddress();
                if (localAddress.getAddress().isAnyLocalAddress()) {
                    // Don't cache on a wildcard address so the correct one
                    // will be cached once the channel is connected/bound
                    return localAddress;
                }
                this.localAddress = localAddress;
            } catch (Throwable t) {
                // Sometimes fails on a closed socket in Windows.
                return null;
            }
        }
        return localAddress;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        InetSocketAddress remoteAddress = this.remoteAddress;
        if (remoteAddress == null) {
            try {
                this.remoteAddress = remoteAddress =
                    getRemoteSocketAddress();
            } catch (Throwable t) {
                // Sometimes fails on a closed socket in Windows.
                return null;
            }
        }
        return remoteAddress;
    }

    @Override
    public abstract NioChannelConfig getConfig();

    @Override
    protected int getInternalInterestOps() {
        return super.getInternalInterestOps();
    }

    @Override
    protected void setInternalInterestOps(int interestOps) {
        super.setInternalInterestOps(interestOps);
    }

    public void setWorker(AbstractNioWorker newWorker) {
        if (newWorker == null) {
            if (worker == null) {
                throw new IllegalStateException("Cannot deregister more than once without re-register");
            }
            worker.deregister(this);
        }
        else {
            worker = newWorker;
            worker.register(this);
        }
    }

    @Override
    protected boolean setClosed() {
        return super.setClosed();
    }

    abstract InetSocketAddress getLocalSocketAddress() throws Exception;

    abstract InetSocketAddress getRemoteSocketAddress() throws Exception;

    private final class WriteRequestQueue implements Queue<MessageEvent> {
        private final ThreadLocalBoolean notifying = new ThreadLocalBoolean();

        private final Queue<MessageEvent> queue;

        public WriteRequestQueue(Queue<MessageEvent> queue) {
            this.queue = queue;
        }

        @Override
        public MessageEvent remove() {
            return queue.remove();
        }

        @Override
        public MessageEvent element() {
            return queue.element();
        }

        @Override
        public MessageEvent peek() {
            return queue.peek();
        }

        @Override
        public int size() {
            return queue.size();
        }

        @Override
        public boolean isEmpty() {
            return queue.isEmpty();
        }

        @Override
        public Iterator<MessageEvent> iterator() {
            return queue.iterator();
        }

        @Override
        public Object[] toArray() {
            return queue.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return queue.toArray(a);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return queue.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends MessageEvent> c) {
            return queue.addAll(c);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return queue.removeAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return queue.retainAll(c);
        }

        @Override
        public void clear() {
            queue.clear();
        }

        @Override
        public boolean add(MessageEvent e) {
            return queue.add(e);
        }

        @Override
        public boolean remove(Object o) {
            return queue.remove(o);
        }

        @Override
        public boolean contains(Object o) {
            return queue.contains(o);
        }

        @Override
        public boolean offer(MessageEvent e) {
            boolean success = queue.offer(e);
            assert success;

            int messageSize = getMessageSize(e);
            int newWriteBufferSize = writeBufferSize.addAndGet(messageSize);
            int highWaterMark =  getConfig().getWriteBufferHighWaterMark();

            if (newWriteBufferSize >= highWaterMark) {
                if (newWriteBufferSize - messageSize < highWaterMark) {
                    highWaterMarkCounter.incrementAndGet();
                    // For Kaazing the poll and offer methods on the WriteRequestQueue will always be called on the IO thread
                    // so we do not need to incur the overhead of the following check (which was added in Netty 3.5.10
                    // commit: 452df045 Always fire interestChanged from IO thread):
                    /*
                    if (setUnwritable()) {
                        if (!isIoThread(AbstractNioChannel.this)) {
                            fireChannelInterestChangedLater(AbstractNioChannel.this);
                        } else
                    */
                    if (!notifying.get()) {
                        notifying.set(Boolean.TRUE);
                        fireChannelInterestChanged(AbstractNioChannel.this);
                        notifying.set(Boolean.FALSE);
                    }
                }
            }
            return true;
        }

        @Override
        public MessageEvent poll() {
            MessageEvent e = queue.poll();
            if (e != null) {
                int messageSize = getMessageSize(e);
                int newWriteBufferSize = writeBufferSize.addAndGet(-messageSize);
                int lowWaterMark = getConfig().getWriteBufferLowWaterMark();

                if (newWriteBufferSize == 0 || newWriteBufferSize < lowWaterMark) {
                    if (newWriteBufferSize + messageSize >= lowWaterMark) {
                        highWaterMarkCounter.decrementAndGet();
                        // For Kaazing the poll and offer methods on the WriteRequestQueue will always be called on the IO thread
                        // so we do not need to incur the overhead of the following check (which was added in Netty 3.5.10
                        // commit: 452df045 Always fire interestChanged from IO thread):
                        /*
                        if (isConnected() && setWritable()) {
                            if (!isIoThread(AbstractNioChannel.this)) {
                               fireChannelInterestChangedLater(AbstractNioChannel.this);
                            } else if (!notifying.get()) {
                        */
                        if (isConnected() && !notifying.get()) {
                            notifying.set(Boolean.TRUE);
                            fireChannelInterestChanged(AbstractNioChannel.this);
                            notifying.set(Boolean.FALSE);
                        }
                    }
                }
            }
            return e;
        }

        private int getMessageSize(MessageEvent e) {
            Object m = e.getMessage();
            if (m instanceof ChannelBuffer) {
                return ((ChannelBuffer) m).readableBytes();
            }
            return 0;
        }
    }

    private final class WriteTask implements Runnable {

        WriteTask() {
        }

        @Override
        public void run() {
            writeTaskInTaskQueue.set(false);
            worker.writeFromTaskLoop(AbstractNioChannel.this);
        }
    }

}
