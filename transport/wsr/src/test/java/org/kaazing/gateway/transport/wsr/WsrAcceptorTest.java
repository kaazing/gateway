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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.apache.mina.core.service.IoHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.Bindings;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.http.HttpAcceptor;
import org.kaazing.gateway.transport.http.HttpConnector;
import org.kaazing.gateway.transport.nio.NioSocketAcceptor;
import org.kaazing.gateway.transport.nio.NioSocketConnector;
import org.kaazing.gateway.transport.pipe.NamedPipeAcceptor;
import org.kaazing.gateway.transport.pipe.NamedPipeConnector;
import org.kaazing.gateway.transport.ws.WsAcceptor;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.future.UnbindFuture;
import org.kaazing.mina.core.session.IoSessionEx;

public class WsrAcceptorTest {

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
        wsAcceptor.setConfiguration(new Properties());

        wsrConnector = (WsrConnector)transportFactory.getTransport("wsr").getConnector();
        wsrConnector.setBridgeServiceFactory(serviceFactory);
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

    @Test
    public void shouldBindAWsrAddress() throws Exception {
        URI location = URI.create("wsr://localhost:8000/echo");
        Map<String, Object> addressOptions = Collections.emptyMap(); //Collections.<String, Object>singletonMap("http.transport", URI.create("pipe://internal"));
        ResourceAddress wsrAddress = addressFactory.newResourceAddress(location, addressOptions);
        IoHandler acceptHandler = new IoHandlerAdapter<IoSessionEx>() {};

        wsrAcceptor.bind(wsrAddress, acceptHandler, null);

        WsrBindings bindings = wsrAcceptor.bindings();

        Bindings.Binding binding = bindings.getBinding(wsrAddress);

        Assert.assertNotNull(binding);
        Assert.assertSame(wsrAddress, binding.bindAddress());
        Assert.assertEquals(1, binding.referenceCount());
        Assert.assertSame(acceptHandler, binding.handler());

        System.out.println(wsrAcceptor.bindings());

    }

    @Test
    public void shouldBindAndUnbindOnWsrAddress()
            throws Exception {
        URI location = URI.create("wsr://localhost:8000/echo");
        Map<String, Object> addressOptions = Collections.emptyMap(); //Collections.<String, Object>singletonMap("http.transport", URI.create("pipe://internal"));
        ResourceAddress wsrAddress = addressFactory.newResourceAddress(location, addressOptions);
        IoHandler acceptHandler = new IoHandlerAdapter<IoSessionEx>() {};

        wsAcceptor.bind(wsrAddress, acceptHandler, null);

        assertEquals(2, wsrAcceptor.bindings().entrySet().size());

        UnbindFuture unbindFuture = wsAcceptor.unbind(wsrAddress);
        unbindFuture.await(3, SECONDS);
        assertTrue(unbindFuture.isDone());
        assertTrue(unbindFuture.isUnbound());

        assertEquals(0, wsrAcceptor.bindings().entrySet().size());
    }

}
