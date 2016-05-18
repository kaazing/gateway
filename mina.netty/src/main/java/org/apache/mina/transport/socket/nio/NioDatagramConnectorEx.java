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
 * 1. Use our XxxEx classes instead of Xxx in order to create sessions which implement IoSessionEx.
 */
package org.apache.mina.transport.socket.nio;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;

import org.apache.mina.core.polling.AbstractPollingIoConnector;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.transport.socket.DatagramConnectorEx;
import org.apache.mina.transport.socket.DatagramSessionConfigEx;
import org.apache.mina.transport.socket.DefaultDatagramSessionConfigEx;

import org.kaazing.mina.core.write.WriteRequestEx;
import org.kaazing.mina.core.write.DefaultWriteRequestEx.ShareableWriteRequest;

/**
 * {@link IoConnector} for datagram transport (UDP/IP).
 */
public final class NioDatagramConnectorEx
        extends AbstractPollingIoConnector<NioSession, DatagramChannel>
        implements DatagramConnectorEx {

    private final List<ThreadLocal<WriteRequestEx>> sharedWriteRequests = ShareableWriteRequest.initWithLayers(16);

    /**
     * Creates a new instance.
     */
    public NioDatagramConnectorEx() {
        super(new DefaultDatagramSessionConfigEx(), NioProcessor.class);
    }

    /**
     * Creates a new instance.
     */
    public NioDatagramConnectorEx(int processorCount) {
        super(new DefaultDatagramSessionConfigEx(), NioProcessor.class, processorCount);
    }

    /**
     * Creates a new instance.
     */
    public NioDatagramConnectorEx(IoProcessor<NioSession> processor) {
        super(new DefaultDatagramSessionConfigEx(), processor);
    }

    /**
     * Constructor for {@link NioDatagramConnectorEx} with default configuration which will use a built-in
     * thread pool executor to manage the given number of processor instances. The processor class must have
     * a constructor that accepts ExecutorService or Executor as its single argument, or, failing that, a
     * no-arg constructor.
     *
     * @param processorClass the processor class.
     * @param processorCount the number of processors to instantiate.
     * @see org.apache.mina.core.service.SimpleIoProcessorPool#SimpleIoProcessorPool(Class, Executor, int)
     * @since 2.0.0-M4
     */
    public NioDatagramConnectorEx(Class<? extends IoProcessor<NioSession>> processorClass,
            int processorCount) {
        super(new DefaultDatagramSessionConfigEx(), processorClass, processorCount);
    }

    /**
     * Constructor for {@link NioDatagramConnectorEx} with default configuration with default configuration which
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
    public NioDatagramConnectorEx(Class<? extends IoProcessor<NioSession>> processorClass) {
        super(new DefaultDatagramSessionConfigEx(), processorClass);
    }

    @Override
    public ThreadLocal<WriteRequestEx> getThreadLocalWriteRequest(int ioLayer) {
        return sharedWriteRequests.get(ioLayer);
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return NioDatagramSession.METADATA;
    }

    @Override
    public DatagramSessionConfigEx getSessionConfig() {
        return (DatagramSessionConfigEx) super.getSessionConfig();
    }

    @Override
    public InetSocketAddress getDefaultRemoteAddress() {
        return (InetSocketAddress) super.getDefaultRemoteAddress();
    }

    @Override
    public void setDefaultRemoteAddress(InetSocketAddress defaultRemoteAddress) {
        super.setDefaultRemoteAddress(defaultRemoteAddress);
    }

    @Override
    protected void init() throws Exception {
        // Do nothing
    }

    @Override
    protected DatagramChannel newHandle(SocketAddress localAddress)
            throws Exception {
        DatagramChannel ch = DatagramChannel.open();

        try {
            if (localAddress != null) {
                ch.socket().bind(localAddress);
            }

            return ch;
        } catch (Exception e) {
            // If we got an exception while binding the datagram,
            // we have to close it otherwise we will loose an handle
            ch.close();
            throw e;
        }
    }

    @Override
    protected boolean connect(DatagramChannel handle,
            SocketAddress remoteAddress) throws Exception {
        handle.connect(remoteAddress);
        return true;
    }

    @Override
    protected NioSession newSession(IoProcessor<NioSession> processor,
            DatagramChannel handle) {
        NioSessionEx session = new NioDatagramSessionEx(this, handle, processor);
        session.getConfig().setAll(getSessionConfig());
        return session;
    }

    @Override
    protected void close(DatagramChannel handle) throws Exception {
        handle.disconnect();
        handle.close();
    }

    // Unused extension points.
    @Override
    @SuppressWarnings("unchecked")
    protected Iterator<DatagramChannel> allHandles() {
        return Collections.EMPTY_LIST.iterator();
    }

    @Override
    protected ConnectionRequest getConnectionRequest(DatagramChannel handle) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void destroy() throws Exception {
        // Do nothing
    }

    @Override
    protected boolean finishConnect(DatagramChannel handle) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void register(DatagramChannel handle, ConnectionRequest request)
            throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected int select(int timeout) throws Exception {
        return 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Iterator<DatagramChannel> selectedHandles() {
        return Collections.EMPTY_LIST.iterator();
    }

    @Override
    protected void wakeup() {
        // Do nothing
    }
}
