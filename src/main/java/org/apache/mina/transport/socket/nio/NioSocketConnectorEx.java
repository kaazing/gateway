/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
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
package org.apache.mina.transport.socket.nio;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Executor;

import org.apache.mina.core.polling.AbstractPollingIoConnector;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.SimpleIoProcessorPool;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.AbstractIoSessionConfig;
import org.apache.mina.transport.socket.DefaultSocketSessionConfigEx;
import org.apache.mina.transport.socket.SocketConnectorEx;
import org.apache.mina.transport.socket.SocketSessionConfigEx;

/**
 * {@link IoConnector} for socket transport (TCP/IP).
 */
public final class NioSocketConnectorEx
        extends AbstractPollingIoConnector<NioSession, SocketChannel>
        implements SocketConnectorEx {

    private volatile Selector selector;

    /**
     * Constructor for {@link NioSocketConnectorEx} with default configuration (multiple thread model).
     */
    public NioSocketConnectorEx() {
        super(new DefaultSocketSessionConfigEx(), NioProcessor.class);
        ((DefaultSocketSessionConfigEx) getSessionConfig()).init(this);
    }

    /**
     * Constructor for {@link NioSocketConnectorEx} with default configuration, and
     * given number of {@link NioProcessor} for multithreading I/O operations
     * @param processorCount the number of processor to create and place in a
     * {@link SimpleIoProcessorPool}
     */
    public NioSocketConnectorEx(int processorCount) {
        super(new DefaultSocketSessionConfigEx(), NioProcessor.class, processorCount);
        ((DefaultSocketSessionConfigEx) getSessionConfig()).init(this);
    }

    /**
     *  Constructor for {@link NioSocketConnectorEx} with default configuration but a
     *  specific {@link IoProcessor}, useful for sharing the same processor over multiple
     *  {@link IoService} of the same type.
     * @param processor the processor to use for managing I/O events
     */
    public NioSocketConnectorEx(IoProcessor<NioSession> processor) {
        super(new DefaultSocketSessionConfigEx(), processor);
        ((DefaultSocketSessionConfigEx) getSessionConfig()).init(this);
    }

    /**
     *  Constructor for {@link NioSocketConnectorEx} with a given {@link Executor} for handling
     *  connection events and a given {@link IoProcessor} for handling I/O events, useful for sharing
     *  the same processor and executor over multiple {@link IoService} of the same type.
     * @param executor the executor for connection
     * @param processor the processor for I/O operations
     */
    public NioSocketConnectorEx(Executor executor, IoProcessor<NioSession> processor) {
        super(new DefaultSocketSessionConfigEx(), executor, processor);
        ((DefaultSocketSessionConfigEx) getSessionConfig()).init(this);
    }

    /**
     * Constructor for {@link NioSocketConnectorEx} with default configuration which will use a built-in
     * thread pool executor to manage the given number of processor instances. The processor class must have
     * a constructor that accepts ExecutorService or Executor as its single argument, or, failing that, a
     * no-arg constructor.
     *
     * @param processorClass the processor class.
     * @param processorCount the number of processors to instantiate.
     * @see org.apache.mina.core.service.SimpleIoProcessorPool#SimpleIoProcessorPool(Class, Executor, int)
     * @since 2.0.0-M4
     */
    public NioSocketConnectorEx(Class<? extends IoProcessor<NioSession>> processorClass,
            int processorCount) {
        super(new DefaultSocketSessionConfigEx(), processorClass, processorCount);
    }

    /**
     * Constructor for {@link NioSocketConnectorEx} with default configuration with default configuration which
     * will use a built-in
     * thread pool executor to manage the default number of processor instances. The processor class must have
     * a constructor that accepts ExecutorService or Executor as its single argument, or, failing that, a
     * no-arg constructor. The default number of instances is equal to the number of processor cores
     * in the system, plus one.
     *
     * @param processorClass the processor class.
     * @see org.apache.mina.core.service.SimpleIoProcessorPool#SimpleIoProcessorPool(Class, Executor, int)
     * @see org.apache.mina.core.service.SimpleIoProcessorPool#DEFAULT_SIZE
     * @since 2.0.0-M4
     */
    public NioSocketConnectorEx(Class<? extends IoProcessor<NioSession>> processorClass) {
        super(new DefaultSocketSessionConfigEx(), processorClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void init() throws Exception {
        this.selector = Selector.open();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void destroy() throws Exception {
        if (selector != null) {
            selector.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransportMetadata getTransportMetadata() {
        return NioSocketSession.METADATA;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketSessionConfigEx getSessionConfig() {
        return (SocketSessionConfigEx) super.getSessionConfig();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InetSocketAddress getDefaultRemoteAddress() {
        return (InetSocketAddress) super.getDefaultRemoteAddress();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefaultRemoteAddress(InetSocketAddress defaultRemoteAddress) {
        super.setDefaultRemoteAddress(defaultRemoteAddress);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Iterator<SocketChannel> allHandles() {
        return new SocketChannelIterator(selector.keys());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean connect(SocketChannel handle, SocketAddress remoteAddress)
            throws Exception {
        return handle.connect(remoteAddress);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected ConnectionRequest getConnectionRequest(SocketChannel handle) {
        SelectionKey key = handle.keyFor(selector);

        if ((key == null) || (!key.isValid())) {
            return null;
        }

        return (ConnectionRequest) key.attachment();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void close(SocketChannel handle) throws Exception {
        SelectionKey key = handle.keyFor(selector);

        if (key != null) {
            key.cancel();
        }

        handle.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean finishConnect(SocketChannel handle) throws Exception {
        if (handle.finishConnect()) {
            SelectionKey key = handle.keyFor(selector);

            if (key != null) {
                key.cancel();
            }

            return true;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SocketChannel newHandle(SocketAddress localAddress)
            throws Exception {
        SocketChannel ch = SocketChannel.open();

        int receiveBufferSize =
            (getSessionConfig()).getReceiveBufferSize();
        if (receiveBufferSize > 65535) {
            if (AbstractIoSessionConfig.ENABLE_BUFFER_SIZE) {
                System.out.println("NioSocketConnector.newHandle(" + receiveBufferSize + ")");
                ch.socket().setReceiveBufferSize(receiveBufferSize);
            }
        }

        if (localAddress != null) {
            ch.socket().bind(localAddress);
        }
        ch.configureBlocking(false);
        return ch;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NioSession newSession(IoProcessor<NioSession> processor, SocketChannel handle) {

        final NioSocketSessionEx nioSocketSession = new NioSocketSessionEx(this, processor, handle);
        // NB: We do not catch the RuntimeIoException for this
        // call because catching one and returning null leads to NPE.
        nioSocketSession.initSessionConfig();
        return nioSocketSession;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void register(SocketChannel handle, ConnectionRequest request)
            throws Exception {
        handle.register(selector, SelectionKey.OP_CONNECT, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int select(int timeout) throws Exception {
        return selector.select(timeout);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Iterator<SocketChannel> selectedHandles() {
        return new SocketChannelIterator(selector.selectedKeys());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void wakeup() {
        selector.wakeup();
    }

    private static final class SocketChannelIterator implements Iterator<SocketChannel> {

        private final Iterator<SelectionKey> i;

        private SocketChannelIterator(Collection<SelectionKey> selectedKeys) {
            this.i = selectedKeys.iterator();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return i.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SocketChannel next() {
            SelectionKey key = i.next();
            return (SocketChannel) key.channel();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove() {
            i.remove();
        }
    }
}
