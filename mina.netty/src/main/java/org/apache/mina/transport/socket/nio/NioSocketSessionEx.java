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
/* This class has the following differences from NioSocketSession in Mina 2.0.0-RC1:
 * 1. Use our XxxEx classes instead of Xxx and extends NioSessionEx instead of NioSession
 *    in order to create sessions which implement IoSessionEx.
 * 2. Add method initSessionConfig.
 * 3. Remove inner class SessionConfigImpl, use DefaultSocketSessionConfigEx instead (for config member variable).
 */
package org.apache.mina.transport.socket.nio;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.file.FileRegion;
import org.apache.mina.core.filterchain.DefaultIoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.DefaultSocketSessionConfigEx;
import org.apache.mina.transport.socket.SocketSessionConfigEx;

import org.kaazing.mina.core.service.IoServiceEx;

/**
 * An {@link IoSession} for socket transport (TCP/IP).
 */
public class NioSocketSessionEx extends NioSessionEx {

    static final TransportMetadata METADATA =
            new DefaultTransportMetadata(
                    "nio", "socket", false, true,
                    InetSocketAddress.class,
                    SocketSessionConfigEx.class,
                    IoBuffer.class, FileRegion.class);

    private final IoServiceEx service;

    private final SocketSessionConfigEx config = new DefaultSocketSessionConfigEx();

    private final IoProcessor<NioSessionEx> processor;

    private final IoFilterChain filterChain = new DefaultIoFilterChain(this);

    private final SocketChannel ch;

    private final IoHandler handler;

    private SelectionKey key;


    /**
     *
     * Creates a new instance of NioSocketSession.
     *
     * @param service the associated IoService
     * @param processor the associated IoProcessor
     * @param ch the used channel
     */
    public NioSocketSessionEx(IoServiceEx service, IoProcessor<NioSessionEx> processor, SocketChannel ch) {
        this.service = service;
        this.processor = processor;
        this.ch = ch;
        this.handler = service.getHandler();
    }

    public void initSessionConfig() throws RuntimeIoException {
        this.config.setAll(service.getSessionConfig());
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
    public SocketSessionConfigEx getConfig() {
        return config;
    }

    @Override
    public IoProcessor<NioSessionEx> getProcessor() {
        return processor;
    }

    @Override
    public IoFilterChain getFilterChain() {
        return filterChain;
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return METADATA;
    }

    @Override
    SocketChannel getChannel() {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public InetSocketAddress getRemoteAddress() {
        if (ch == null) {
            return null;
        }

        Socket socket = ch.socket();

        if (socket == null) {
            return null;
        }

        return (InetSocketAddress) socket.getRemoteSocketAddress();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InetSocketAddress getLocalAddress() {
        if (ch == null) {
            return null;
        }

        Socket socket = ch.socket();

        if (socket == null) {
            return null;
        }

        return (InetSocketAddress) socket.getLocalSocketAddress();
    }

    @Override
    public InetSocketAddress getServiceAddress() {
        return (InetSocketAddress) super.getServiceAddress();
    }
}
