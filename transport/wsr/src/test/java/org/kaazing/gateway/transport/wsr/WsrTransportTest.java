/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.transport.wsr;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.http.HttpAcceptor;
import org.kaazing.gateway.transport.http.HttpConnector;
import org.kaazing.gateway.transport.nio.NioSocketAcceptor;
import org.kaazing.gateway.transport.nio.NioSocketConnector;
import org.kaazing.gateway.transport.pipe.NamedPipeAcceptor;
import org.kaazing.gateway.transport.pipe.NamedPipeConnector;
import org.kaazing.gateway.transport.ws.WsAcceptor;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;

public class WsrTransportTest {

    private SchedulerProvider schedulerProvider;

    private ResourceAddressFactory addressFactory;
    private BridgeServiceFactory serviceFactory;

    private NamedPipeConnector pipeConnector;
    private NioSocketConnector tcpConnector;
    private HttpConnector httpConnector;
    private WsrConnector wsrConnector;

    private NamedPipeAcceptor pipeAcceptor;
    private NioSocketAcceptor tcpAcceptor;
    private HttpAcceptor httpAcceptor;
    private WsrAcceptor wsrAcceptor;
    private WsAcceptor wsAcceptor;

    @Before
    public void init() {
        schedulerProvider = new SchedulerProvider();

        addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        TransportFactory transportFactory = TransportFactory.newTransportFactory(Collections.EMPTY_MAP);
        serviceFactory = new BridgeServiceFactory(transportFactory);

        pipeAcceptor = (NamedPipeAcceptor)transportFactory.getTransport("pipe").getAcceptor();
        pipeAcceptor.setBridgeServiceFactory(serviceFactory);
        pipeAcceptor.setResourceAddressFactory(addressFactory);

        pipeConnector = (NamedPipeConnector)transportFactory.getTransport("pipe").getConnector();
        pipeConnector.setResourceAddressFactory(addressFactory);
        pipeConnector.setBridgeServiceFactory(serviceFactory);
        pipeConnector.setNamedPipeAcceptor(pipeAcceptor);

        tcpAcceptor = (NioSocketAcceptor)transportFactory.getTransport("tcp").getAcceptor();
        tcpAcceptor.setResourceAddressFactory(addressFactory);
        tcpAcceptor.setBridgeServiceFactory(serviceFactory);
        tcpAcceptor.setSchedulerProvider(schedulerProvider);

        tcpConnector = (NioSocketConnector)transportFactory.getTransport("tcp").getConnector();
        tcpConnector.setResourceAddressFactory(addressFactory);
        tcpConnector.setBridgeServiceFactory(serviceFactory);

        httpAcceptor = (HttpAcceptor)transportFactory.getTransport("http").getAcceptor();
        httpAcceptor.setBridgeServiceFactory(serviceFactory);
        httpAcceptor.setResourceAddressFactory(addressFactory);
        httpAcceptor.setSchedulerProvider(schedulerProvider);

        httpConnector = (HttpConnector)transportFactory.getTransport("http").getConnector();
        httpConnector.setBridgeServiceFactory(serviceFactory);
        httpConnector.setResourceAddressFactory(addressFactory);

        wsrAcceptor = (WsrAcceptor)transportFactory.getTransport("wsr").getAcceptor();
        wsrAcceptor.setBridgeServiceFactory(serviceFactory);
        wsrAcceptor.setResourceAddressFactory(addressFactory);
        wsrAcceptor.setSchedulerProvider(schedulerProvider);

        wsAcceptor = (WsAcceptor)transportFactory.getTransport("ws").getAcceptor();
        wsAcceptor.setWsrAcceptor(wsrAcceptor);

        wsrConnector = (WsrConnector)transportFactory.getTransport("wsr").getConnector();
        wsrConnector.setBridgeServiceFactory(serviceFactory);
        wsrConnector.setResourceAddressFactory(addressFactory);

    }

    @After
    public void disposeConnector() {
        if (pipeAcceptor != null) {
            pipeAcceptor.dispose();
        }
        if (tcpAcceptor != null) {
            tcpAcceptor.dispose();
        }
        if (httpAcceptor != null) {
            httpAcceptor.dispose();
        }
        if (wsrAcceptor != null) {
            wsrAcceptor.dispose();
        }
        if (pipeConnector != null) {
            pipeConnector.dispose();
        }
        if (tcpConnector != null) {
            tcpConnector.dispose();
        }
        if (httpConnector != null) {
            httpConnector.dispose();
        }
        if (wsrConnector != null) {
            wsrConnector.dispose();
        }
    }

    @Test @Ignore("KG-7706: Need this test to help author robot tests and close out KG-7706")
    public void shouldCorrectlyConstructLocalAndRemoteAddressesForConnectedWsrSessions() throws Exception {

        final URI location = URI.create("wsr://localhost:8000/echo");
        Map<String, Object> addressOptions = Collections.emptyMap();
        ResourceAddress address = addressFactory.newResourceAddress(location, addressOptions);
        TransportTestIoHandlerAdapter acceptHandler = new TransportTestIoHandlerAdapter(1) {

            @Override
            public String getCheckpointFailureMessage() {
                return "Failed to construct accept session local/remote addresses correctly.";
            }

            @Override
            public void doMessageReceived(final IoSessionEx session, Object message)
                    throws Exception {
                // echo message
                IoBufferEx buf = (IoBufferEx)message;
                WriteFuture future = session.write(buf.duplicate());

                // close session abruptly, without triggering WebSocket close handshake
                future.addListener(new IoFutureListener<WriteFuture>() {
                    @Override
                    public void operationComplete(WriteFuture future) {
                        BridgeSession bridgeSession = (BridgeSession) session;
                        assertEquals("remote address of accept session was not "+location, location, BridgeSession.REMOTE_ADDRESS.get(bridgeSession).getResource());
                        assertEquals("local  address of accept session was not "+location, location, BridgeSession.LOCAL_ADDRESS.get(bridgeSession).getResource());
                        checkpoint();
                    }
                });
            }

        };
        wsrAcceptor.bind(address, acceptHandler, null);

        TransportTestIoHandlerAdapter connectHandler = new TransportTestIoHandlerAdapter(1) {

            @Override
            public String getCheckpointFailureMessage() {
                return "Failed to construct connect session local/remote addresses correctly.";
            }

            @Override
            public void doSessionCreated(IoSessionEx session) throws Exception {
                IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
                session.write(allocator.wrap(ByteBuffer.wrap("Hello, world".getBytes())));
                BridgeSession bridgeSession = (BridgeSession) session;
                assertEquals("remote address of connect session was not " + location, location, BridgeSession.REMOTE_ADDRESS.get(bridgeSession).getResource());
                assertEquals("local  address of connect session was not " + location, location, BridgeSession.LOCAL_ADDRESS.get(bridgeSession).getResource());
                checkpoint();
            }

        };

        ConnectFuture connectFuture = wsrConnector.connect(address, connectHandler, null);
        connectFuture.await(3, TimeUnit.SECONDS);
        acceptHandler.await(3, TimeUnit.SECONDS);
        connectHandler.await(3, TimeUnit.SECONDS);
    }

}
