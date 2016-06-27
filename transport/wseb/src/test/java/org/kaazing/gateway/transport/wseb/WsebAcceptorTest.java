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
package org.kaazing.gateway.transport.wseb;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Collections;
import java.util.Map;

import junit.framework.Assert;

import org.apache.mina.core.service.IoHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.Bindings;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.http.HttpAcceptor;
import org.kaazing.gateway.transport.http.HttpConnector;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.transport.nio.internal.NioSocketConnector;
import org.kaazing.gateway.transport.ws.WsAcceptor;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;

public class WsebAcceptorTest {

    private ResourceAddressFactory addressFactory;

    private NioSocketConnector tcpConnector;
    private HttpConnector httpConnector;
    private WsebConnector wsebConnector;
    
    private NioSocketAcceptor tcpAcceptor;
    private HttpAcceptor httpAcceptor;
    private WsebAcceptor wsebAcceptor;

    @Rule
    public TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    @Before
    public void init() {
        SchedulerProvider schedulerProvider = new SchedulerProvider();
         
        addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        TransportFactory transportFactory = TransportFactory.newTransportFactory(Collections.EMPTY_MAP);
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

        wsebAcceptor = (WsebAcceptor)transportFactory.getTransport("wseb").getAcceptor();
        wsebAcceptor.setBridgeServiceFactory(serviceFactory);
        wsebAcceptor.setResourceAddressFactory(addressFactory);
        wsebAcceptor.setSchedulerProvider(schedulerProvider);

        WsAcceptor wsAcceptor = (WsAcceptor) transportFactory.getTransport("ws").getAcceptor();
        wsAcceptor.setWsebAcceptor(wsebAcceptor);

        wsebConnector = (WsebConnector)transportFactory.getTransport("wseb").getConnector();
        wsebConnector.setBridgeServiceFactory(serviceFactory);
    }
    
    @After
    public void disposeConnector() {
        if (tcpAcceptor != null) {
            tcpAcceptor.dispose();
        }
        if (httpAcceptor != null) {
            httpAcceptor.dispose();
        }
        if (wsebAcceptor != null) {
            wsebAcceptor.dispose();
        }
        if (tcpConnector != null) {
            tcpConnector.dispose();
        }
        if (httpConnector != null) {
            httpConnector.dispose();
        }
        if (wsebConnector != null) {
            wsebConnector.dispose();
        }
    }


    @Test
    @Ignore ("https://github.com/kaazing/gateway/issues/287")
    public void shouldBindAWsAddress() throws Exception {
        String location = "wse://localhost:8000/echo";
        Map<String, Object> addressOptions = Collections.emptyMap(); //Collections.<String, Object>singletonMap("http.transport", URI.create("pipe://internal"));
        ResourceAddress wseAddress = addressFactory.newResourceAddress(location, addressOptions);
        IoHandler acceptHandler = new IoHandlerAdapter() {};

        wsebAcceptor.bind(wseAddress, acceptHandler, null);

        Bindings<Bindings.Binding> bindings = wsebAcceptor.bindings();

        Bindings.Binding binding = bindings.getBinding(wseAddress);

        Assert.assertNotNull(binding);
        Assert.assertSame(wseAddress, binding.bindAddress());
        Assert.assertEquals(1, binding.referenceCount());
        Assert.assertSame(acceptHandler, binding.handler());

        System.out.println(wsebAcceptor.bindings());

    }

}
