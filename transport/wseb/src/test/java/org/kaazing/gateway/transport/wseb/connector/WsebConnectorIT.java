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

package org.kaazing.gateway.transport.wseb.connector;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.fail;
import static org.junit.rules.RuleChain.outerRule;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.PropertyConfigurator;
import org.apache.mina.core.future.ConnectFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.http.HttpConnector;
import org.kaazing.gateway.transport.nio.NioSocketAcceptor;
import org.kaazing.gateway.transport.nio.NioSocketConnector;
import org.kaazing.gateway.transport.wseb.WsebConnector;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.session.IoSessionEx;

public class WsebConnectorIT {

    private final K3poRule robot = new K3poRule();

    private TestRule timeout = new DisableOnDebug(new Timeout(4, SECONDS));

    @Rule
    public TestRule chain = outerRule(robot).around(timeout);
    
    ResourceAddressFactory resourceAddressFactory;
    BridgeServiceFactory bridgeServiceFactory;

    NioSocketConnector tcpConnector;
    NioSocketAcceptor tcpAcceptor;
    HttpConnector httpConnector;
    WsebConnector wseConnector;
    SchedulerProvider schedulerProvider;

    @Before
    public void setUp()
        throws Exception {

        Map<String, ?> config = Collections.emptyMap();
        TransportFactory transportFactory = TransportFactory.newTransportFactory(config);
        bridgeServiceFactory = new BridgeServiceFactory(transportFactory);

        // Connector setup
        resourceAddressFactory = ResourceAddressFactory.newResourceAddressFactory();
        tcpConnector = (NioSocketConnector)transportFactory.getTransport("tcp").getConnector();
        tcpAcceptor = (NioSocketAcceptor)transportFactory.getTransport("tcp").getAcceptor();
        httpConnector = (HttpConnector)transportFactory.getTransport("http").getConnector();;
        wseConnector = (WsebConnector)transportFactory.getTransport("wseb").getConnector();;
        schedulerProvider = new SchedulerProvider();

        tcpConnector.setResourceAddressFactory(resourceAddressFactory);
        wseConnector.setResourceAddressFactory(resourceAddressFactory);
        wseConnector.setBridgeServiceFactory(bridgeServiceFactory);
        tcpConnector.setBridgeServiceFactory(bridgeServiceFactory);
        tcpConnector.setTcpAcceptor(tcpAcceptor);
        httpConnector.setBridgeServiceFactory(bridgeServiceFactory);
        httpConnector.setResourceAddressFactory(resourceAddressFactory);
        
        // Initialize log4j using a properties file available on the class path
        String log4jPropertiesResourceName = "log4j-diagnostic.properties";
        Properties log4j = new Properties();
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(log4jPropertiesResourceName);
        if (in == null) {
            throw new IOException(String.format("Could not load resource %s", log4jPropertiesResourceName));
        }
        log4j.load(in);
        PropertyConfigurator.configure(log4j);
    }

    @After
    public void after() throws Exception {
        tcpConnector.dispose();
        tcpAcceptor.dispose();
        httpConnector.dispose();
        wseConnector.dispose();
        schedulerProvider.shutdownNow();
    }

    @Specification("shouldReplyPongToPing")
    // TODO: remove this once we enable spec test ControlIT
    @Test
    public void shouldReplyPongToPing() throws Exception {
        ConnectFuture connected = connect("wse://localhost:8011/path", null, new IoHandlerAdapter<IoSessionEx>() {
            
        });
        //future.getSession().write(new WsebBufferAllocator(SimpleBufferAllocator.BUFFER_ALLOCATOR).wrap(Utils.asByteBuffer("Message from connector")));
        robot.finish();
    }
    
    private ConnectFuture connect(final String connect,
                                  final Long wsInactivityTimeout,
                                  IoHandlerAdapter<?> connectHandler) throws InterruptedException {
        Map<String, Object> connectOptions = new HashMap<String, Object>();
        if (wsInactivityTimeout != null) {
            connectOptions.put("inactivityTimeout", wsInactivityTimeout);
        }
        final ResourceAddress connectAddress =
                resourceAddressFactory.newResourceAddress(
                        URI.create(connect),
                        connectOptions);

        ConnectFuture future = wseConnector.connect(connectAddress, connectHandler, null);

        future.await(TimeUnit.MILLISECONDS.toMillis(3000));

        if (!future.isConnected()) {
            fail("Failed to connect: " + future.getException());
        }
        return future;
    }
    
}
