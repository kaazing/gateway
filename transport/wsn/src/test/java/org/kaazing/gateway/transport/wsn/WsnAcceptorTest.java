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
package org.kaazing.gateway.transport.wsn;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.kaazing.test.util.ITUtil.timeoutRule;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.http.HttpAcceptor;
import org.kaazing.gateway.transport.http.HttpConnector;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.transport.nio.internal.NioSocketConnector;
import org.kaazing.gateway.transport.ws.WsAcceptor;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.future.UnbindFuture;

public class WsnAcceptorTest {

    @Rule
    public final TestRule timeoutRule = timeoutRule(10, SECONDS);

    private ResourceAddressFactory addressFactory;

    private NioSocketConnector tcpConnector;
    private HttpConnector httpConnector;
    private WsnConnector wsnConnector;
    
    private NioSocketAcceptor tcpAcceptor;
    private HttpAcceptor httpAcceptor;
    private WsnAcceptor wsnAcceptor;

    @Before
    public void init() {
        SchedulerProvider schedulerProvider = new SchedulerProvider();
         
        addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        Map<String, Object> config = Collections.emptyMap();
        TransportFactory transportFactory = TransportFactory.newTransportFactory(config);
        BridgeServiceFactory serviceFactory = new BridgeServiceFactory(transportFactory);

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

        wsnAcceptor = (WsnAcceptor)transportFactory.getTransport("wsn").getAcceptor();
        wsnAcceptor.setBridgeServiceFactory(serviceFactory);
        wsnAcceptor.setResourceAddressFactory(addressFactory);
        wsnAcceptor.setSchedulerProvider(schedulerProvider);

        WsAcceptor wsAcceptor = (WsAcceptor) transportFactory.getTransport("ws").getAcceptor();
        wsAcceptor.setWsnAcceptor(wsnAcceptor);
        wsAcceptor.setConfiguration(new Properties());

        wsnConnector = (WsnConnector)transportFactory.getTransport("wsn").getConnector();
        wsnConnector.setBridgeServiceFactory(serviceFactory);
        wsnConnector.setSchedulerProvider(schedulerProvider);
    }
    
    @After
    public void disposeConnector() {
        if (tcpAcceptor != null) {
            tcpAcceptor.dispose();
        }
        if (httpAcceptor != null) {
            httpAcceptor.dispose();
        }
        if (wsnAcceptor != null) {
            wsnAcceptor.dispose();
        }
        if (tcpConnector != null) {
            tcpConnector.dispose();
        }
        if (httpConnector != null) {
            httpConnector.dispose();
        }
        if (wsnConnector != null) {
            wsnConnector.dispose();
        }
    }

    @Test
    public void shouldBindAndUnbindLeavingEmptyBindingsMaps() throws Exception {

        Map<String, Object> acceptOptions = new HashMap<>();

        final String connectURIString = "ws://localhost:8001/echo";
        final ResourceAddress bindAddress =
                addressFactory.newResourceAddress(
                        connectURIString,
                        acceptOptions);

        final IoHandler ioHandler = new IoHandlerAdapter();

        for ( int i = 0; i < 10; i++) {
            wsnAcceptor.bind(bindAddress, ioHandler, null);
        }

        for (int j = 0; j < 10; j++) {
            UnbindFuture future = wsnAcceptor.unbind(bindAddress);
            org.junit.Assert.assertTrue("Unbind failed", future.await(1, TimeUnit.SECONDS));
        }
        org.junit.Assert.assertTrue(wsnAcceptor.emptyBindings());
        org.junit.Assert.assertTrue(httpAcceptor.emptyBindings());
        org.junit.Assert.assertTrue(tcpAcceptor.emptyBindings());
    }

    @Test
    public void shouldBeAbleToBindAndUnbindWsxAndWsnAddresses() throws Exception {

        String location = "wsn://localhost:8002/echo";
        Map<String, Object> addressOptions = Collections.emptyMap(); //Collections.<String, Object>singletonMap("http.transport", URI.create("pipe://internal"));
        ResourceAddress wsnAddress = addressFactory.newResourceAddress(location, addressOptions);

        location = "wsx://localhost:8002/echo";
        addressOptions = Collections.emptyMap(); //Collections.<String, Object>singletonMap("http.transport", URI.create("pipe://internal"));
        ResourceAddress wsxAddress = addressFactory.newResourceAddress(location, addressOptions);

        IoHandler acceptHandler = new IoHandlerAdapter() {};
        wsnAcceptor.bind(wsnAddress, acceptHandler, null);
        wsnAcceptor.bind(wsxAddress, acceptHandler, null);

        wsnAcceptor.unbind(wsxAddress);
        wsnAcceptor.unbind(wsnAddress);

        System.out.println(wsnAcceptor.bindings());
    }

    @Test
    public void shouldBindWsxAddressesWithTcpBind() throws Exception {
        String uri1 = "wsx://localhost:8003/";
        HashMap<String, Object> options1 = new HashMap<>();
        options1.put("tcp.bind", "7777");

        String uri2 = "wsx://localhost:8003/";
        HashMap<String, Object> options2 = new HashMap<>();

        ResourceAddress address1 = addressFactory.newResourceAddress(uri1, options1);
        ResourceAddress address2 = addressFactory.newResourceAddress(uri2, options2);

        final IoHandler ioHandler = new IoHandlerAdapter();
        wsnAcceptor.bind(address1, ioHandler, null);
        wsnAcceptor.bind(address2, ioHandler, null);

        IoFuture future1 = wsnAcceptor.unbind(address1);
        IoFuture future2 = wsnAcceptor.unbind(address2);
        future1.await(5, TimeUnit.SECONDS);
        future2.await(5, TimeUnit.SECONDS);

        assertTrue(wsnAcceptor.emptyBindings());
        assertTrue(httpAcceptor.emptyBindings());
        assertTrue(tcpAcceptor.emptyBindings());
    }
}
