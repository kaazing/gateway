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
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
/* This class has the following differences from NioSocketAcceptor in Mina 2.0.0-RC1:
 * 1. Use NioSocketSessionEx, DefaultSocketSessionConfigEx, SocketSessionConfigEx and SocketAcceptorEx
 *    instead of DefaultSocketSessionConfig, etc, in order to create sessions which implement IoSessionEx.
 * 2. Make DatagramChannelIterator final for efficiency reasons.
 */
package org.apache.mina.transport.socket.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.polling.AbstractPollingConnectionlessIoAcceptor;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.transport.socket.DatagramAcceptorEx;
import org.apache.mina.transport.socket.DatagramSessionConfigEx;
import org.apache.mina.transport.socket.DefaultDatagramSessionConfigEx;

import org.kaazing.mina.core.buffer.SimpleBufferAllocator;
import org.kaazing.mina.core.future.BindFuture;
import org.kaazing.mina.core.future.DefaultBindFuture;
import org.kaazing.mina.core.future.DefaultUnbindFuture;
import org.kaazing.mina.core.future.UnbindFuture;
import org.kaazing.mina.core.write.WriteRequestEx;
import org.kaazing.mina.core.write.DefaultWriteRequestEx.ShareableWriteRequest;

/**
 * {@link IoAcceptor} for datagram transport (UDP/IP).
 *
 * @org.apache.xbean.XBean
 */
public final class NioDatagramAcceptorEx
        extends AbstractPollingConnectionlessIoAcceptor<NioSession, DatagramChannel>
        implements DatagramAcceptorEx {

    private final List<ThreadLocal<WriteRequestEx>> sharedWriteRequests = ShareableWriteRequest.initWithLayers(16);

    private volatile Selector selector;

    /**
     * Creates a new instance.
     */
    public NioDatagramAcceptorEx() {
        super(new DefaultDatagramSessionConfigEx());
    }

    /**
     * Creates a new instance.
     */
    public NioDatagramAcceptorEx(Executor executor) {
        super(new DefaultDatagramSessionConfigEx(), executor);
    }

    @Override
    public ThreadLocal<WriteRequestEx> getThreadLocalWriteRequest(int ioLayer) {
        return sharedWriteRequests.get(ioLayer);
    }

    @Override
    protected void init() throws Exception {
        this.selector = Selector.open();
    }

    @Override
    protected void destroy() throws Exception {
        if (selector != null) {
            selector.close();
        }
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return NioDatagramSessionEx.METADATA;
    }

    @Override
    public DatagramSessionConfigEx getSessionConfig() {
        return (DatagramSessionConfigEx) super.getSessionConfig();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) super.getLocalAddress();
    }

    @Override
    public InetSocketAddress getDefaultLocalAddress() {
        return (InetSocketAddress) super.getDefaultLocalAddress();
    }

    @Override
    public void setDefaultLocalAddress(InetSocketAddress localAddress) {
        setDefaultLocalAddress((SocketAddress) localAddress);
    }

    @Override
    public BindFuture bindAsync(SocketAddress localAddress) {
        try {
            // TODO: bind asynchronously
            bind(localAddress);
            return DefaultBindFuture.succeededFuture();
        }
        catch (IOException e) {
            DefaultBindFuture future = new DefaultBindFuture();
            future.setException(e);
            return future;
        }
    }

    @Override
    public UnbindFuture unbindAsync(SocketAddress localAddress) {
        try {
            // TODO: unbind asynchronously
            unbind(localAddress);
            return DefaultUnbindFuture.succeededFuture();
        }
        catch (Exception e) {
            DefaultUnbindFuture future = new DefaultUnbindFuture();
            future.setException(e);
            return future;
        }
    }

    @Override
    protected DatagramChannel open(SocketAddress localAddress) throws Exception {
        final DatagramChannel c = DatagramChannel.open();
        boolean success = false;
        try {
            new NioDatagramSessionConfigEx(c).setAll(getSessionConfig());
            c.configureBlocking(false);
            c.socket().bind(localAddress);
            c.register(selector, SelectionKey.OP_READ);
            success = true;
        } finally {
            if (!success) {
                close(c);
            }
        }

        return c;
    }

    @Override
    protected boolean isReadable(DatagramChannel handle) {
        SelectionKey key = handle.keyFor(selector);

        if ((key == null) || (!key.isValid())) {
            return false;
        }

        return key.isReadable();
    }

    @Override
    protected boolean isWritable(DatagramChannel handle) {
        SelectionKey key = handle.keyFor(selector);

        if ((key == null) || (!key.isValid())) {
            return false;
        }

        return key.isWritable();
    }

    @Override
    protected SocketAddress localAddress(DatagramChannel handle)
            throws Exception {
        return handle.socket().getLocalSocketAddress();
    }

    @Override
    protected NioSession newSession(
            IoProcessor<NioSession> processor, DatagramChannel handle,
            SocketAddress remoteAddress) {
        SelectionKey key = handle.keyFor(selector);

        if ((key == null) || (!key.isValid())) {
            return null;
        }

        NioDatagramSessionEx newSession = new NioDatagramSessionEx(
                this, handle, processor, remoteAddress);
        newSession.setSelectionKey(key);

        return newSession;
    }

    @Override
    protected SocketAddress receive(DatagramChannel handle, IoBuffer buffer)
            throws Exception {
        return handle.receive(buffer.buf());
    }

    @Override
    protected int select() throws Exception {
        return selector.select();
    }

    @Override
    protected int select(int timeout) throws Exception {
        return selector.select(timeout);
    }

    @Override
    protected Iterator<DatagramChannel> selectedHandles() {
        return new DatagramChannelIterator(selector.selectedKeys());
    }

    @Override
    protected int send(NioSession session, IoBuffer buffer,
            SocketAddress remoteAddress) throws Exception {
        return ((DatagramChannel) session.getChannel()).send(
                buffer.buf(), remoteAddress);
    }

    @Override
    protected void setInterestedInWrite(NioSession session, boolean interested)
            throws Exception {
        SelectionKey key = session.getSelectionKey();
        if (key == null) {
            return;
        }

        if (interested) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        } else {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }

    @Override
    protected void close(DatagramChannel handle) throws Exception {
        SelectionKey key = handle.keyFor(selector);

        if (key != null) {
            key.cancel();
        }

        handle.disconnect();
        handle.close();
    }

    @Override
    protected void wakeup() {
        selector.wakeup();
    }

    @Override
    protected IoBuffer newReadBuffer(int readBufferSize) {
        // note: this assumes NioSessionEx.getBufferAllocator() returns SimpleBufferAllocator.BUFFER_ALLOCATOR
        return SimpleBufferAllocator.BUFFER_ALLOCATOR.wrap(ByteBuffer.allocate(readBufferSize));
    }

    private static final class DatagramChannelIterator implements Iterator<DatagramChannel> {

        private final Iterator<SelectionKey> i;

        private DatagramChannelIterator(Collection<SelectionKey> keys) {
            this.i = keys.iterator();
        }

        @Override
        public boolean hasNext() {
            return i.hasNext();
        }

        @Override
        public DatagramChannel next() {
            return (DatagramChannel) i.next().channel();
        }

        @Override
        public void remove() {
            i.remove();
        }

    }
}
