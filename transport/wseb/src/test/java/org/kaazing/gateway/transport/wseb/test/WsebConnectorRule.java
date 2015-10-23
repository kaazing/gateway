/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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
package org.kaazing.gateway.transport.wseb.test;

import static org.junit.Assert.fail;

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
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.http.HttpConnector;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.transport.nio.internal.NioSocketConnector;
import org.kaazing.gateway.transport.wseb.WsebConnector;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;

/**
 * Declaring an instance of this class as a @Rule causes the gateway to be started in process before each test method and stopped
 * after it. The rule can be chained with a K3poRule for use with robot (this causes Robot to be started before the gateway and
 * stopped after it).
 */
public class WsebConnectorRule implements TestRule {

    private final String log4jPropertiesResourceName;
    private ResourceAddressFactory resourceAddressFactory;
    private WsebConnector wseConnector;


    @Override
    public Statement apply(Statement base, Description description) {
        return new ConnectorStatement(base);
    }
    public WsebConnectorRule() {
        this(null);
    }

    public WsebConnectorRule(String log4jPropertiesResourceName) {
        this.log4jPropertiesResourceName = log4jPropertiesResourceName;
    }
    
    public ConnectFuture connect(final String connect,
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

    private final class ConnectorStatement extends Statement {

        private final Statement base;
        private Map<String, ?> config = Collections.emptyMap();
        private TransportFactory transportFactory = TransportFactory.newTransportFactory(config);
        private BridgeServiceFactory bridgeServiceFactory = new BridgeServiceFactory(transportFactory);

        private NioSocketConnector tcpConnector;
        private NioSocketAcceptor tcpAcceptor;
        private HttpConnector httpConnector;
        private SchedulerProvider schedulerProvider;

        public ConnectorStatement(Statement base) {
            this.base = base;
        }

        @Override
        public void evaluate() throws Throwable {
            if (log4jPropertiesResourceName != null) {
                // Initialize log4j using a properties file available on the class path
                Properties log4j = new Properties();
                InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(log4jPropertiesResourceName);
                if (in == null) {
                    throw new IOException(String.format("Could not load resource %s", log4jPropertiesResourceName));
                }
                log4j.load(in);
                PropertyConfigurator.configure(log4j);
            }
            try {
                // Connector setup
                resourceAddressFactory = ResourceAddressFactory.newResourceAddressFactory();
                tcpConnector = (NioSocketConnector)transportFactory.getTransport("tcp").getConnector();
                tcpAcceptor = (NioSocketAcceptor)transportFactory.getTransport("tcp").getAcceptor();
                httpConnector = (HttpConnector)transportFactory.getTransport("http").getConnector();
                wseConnector = (WsebConnector)transportFactory.getTransport("wseb").getConnector();
                schedulerProvider = new SchedulerProvider();
        
                tcpConnector.setResourceAddressFactory(resourceAddressFactory);
                wseConnector.setResourceAddressFactory(resourceAddressFactory);
                wseConnector.setBridgeServiceFactory(bridgeServiceFactory);
                tcpConnector.setBridgeServiceFactory(bridgeServiceFactory);
                tcpConnector.setTcpAcceptor(tcpAcceptor);
                httpConnector.setBridgeServiceFactory(bridgeServiceFactory);
                httpConnector.setResourceAddressFactory(resourceAddressFactory);
                
                base.evaluate();
            } finally {
                tcpConnector.dispose();
                tcpAcceptor.dispose();
                httpConnector.dispose();
                wseConnector.dispose();
                schedulerProvider.shutdownNow();
            }
        }

    }
}

