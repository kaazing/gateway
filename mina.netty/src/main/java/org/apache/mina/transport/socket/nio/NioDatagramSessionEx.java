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
import java.nio.channels.SelectionKey;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.DefaultIoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.DatagramSessionConfigEx;

import org.kaazing.mina.core.service.IoServiceEx;

/**
 * An {@link IoSession} for datagram transport (UDP/IP).
 */
class NioDatagramSessionEx extends NioSessionEx {

    static final TransportMetadata METADATA =
            new DefaultTransportMetadata(
                    "nio", "datagram", true, false,
                    InetSocketAddress.class,
                    DatagramSessionConfigEx.class, IoBuffer.class);

    private final IoServiceEx service;
    private final DatagramSessionConfigEx config;
    private final IoFilterChain filterChain = new DefaultIoFilterChain(this);
    private final DatagramChannel ch;
    private final IoHandler handler;
    private final InetSocketAddress localAddress;
    private final InetSocketAddress remoteAddress;
    private final IoProcessor<NioSession> processor;

    private SelectionKey key;

    /**
     * Creates a new acceptor-side session instance.
     */
    NioDatagramSessionEx(IoServiceEx service,
                        DatagramChannel ch, IoProcessor<NioSession> processor,
                        SocketAddress remoteAddress) {
        this.service = service;
        this.ch = ch;
        this.config = new NioDatagramSessionConfigEx(ch);
        this.handler = service.getHandler();
        this.processor = processor;
        this.remoteAddress = (InetSocketAddress) remoteAddress;
        this.localAddress = (InetSocketAddress) ch.socket().getLocalSocketAddress();
    }

    /**
     * Creates a new connector-side session instance.
     */
    NioDatagramSessionEx(IoServiceEx service,
                        DatagramChannel ch, IoProcessor<NioSession> processor) {
        this(service, ch, processor, ch.socket().getRemoteSocketAddress());
    }

    @Override
    public int getIoLayer() {
        return 0;
    }

    @Override
    public IoServiceEx getService() {
        return service;
    }

    @Override
    public IoProcessor<NioSession> getProcessor() {
        return processor;
    }

    @Override
    public DatagramSessionConfigEx getConfig() {
        return config;
    }

    @Override
    public IoFilterChain getFilterChain() {
        return filterChain;
    }

    @Override
    DatagramChannel getChannel() {
        return ch;
    }

    @Override
    SelectionKey getSelectionKey() {
        return key;
    }

    @Override
    void setSelectionKey(SelectionKey key) {
        this.key = key;
    }

    @Override
    public IoHandler getHandler() {
        return handler;
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return METADATA;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    @Override
    public InetSocketAddress getServiceAddress() {
        return (InetSocketAddress) super.getServiceAddress();
    }
}
