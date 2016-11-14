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
package org.kaazing.gateway.transport.wsn.specification.ws.connector;

import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.SUPPORTED_PROTOCOLS;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.http.HttpConnector;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.transport.nio.internal.NioSocketConnector;
import org.kaazing.gateway.transport.ws.WsConnector;
import org.kaazing.gateway.transport.wsn.WsnConnector;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;


/**
 * Declaring an instance of this class as a @Rule causes the gateway to be
 * started in process before each test method and stopped after it. The rule
 * can be chained with a K3poRule for use with robot (this causes Robot to be
 * started before the gateway and stopped after it).
 */
public class WsnConnectorRule implements TestRule {

    private ResourceAddressFactory addressFactory;
    private WsnConnector wsnConnector;
    private Map<String, Object> connectOptions = new HashMap<>();

    @Override
    public Statement apply(Statement base, Description description) {
        return new ConnectorStatement(base);
    }

    public WsnConnectorRule() {

    }

    public ConnectFuture connect(String connect, Long wsInactivityTimeout, IoHandler connectHandler)
            throws InterruptedException {
        if (wsInactivityTimeout != null) {
            connectOptions.put("inactivityTimeout", wsInactivityTimeout);
        }

        ResourceAddress connectAddress =
                addressFactory.newResourceAddress(connect, connectOptions);

        return wsnConnector.connect(connectAddress, connectHandler, null);
    }

    public ConnectFuture connect(String connect, String[] protocols, String[] extensions, IoHandler connectHandler)
            throws InterruptedException {
        if (protocols != null) {
            connectOptions.put(SUPPORTED_PROTOCOLS.name(), protocols);
        }

        ResourceAddress connectAddress =
                addressFactory.newResourceAddress(connect, connectOptions);
        return wsnConnector.connect(connectAddress, connectHandler, null);
    }

    private final class ConnectorStatement extends Statement {

        private final Statement base;

        private NioSocketConnector tcpConnector;
        private NioSocketAcceptor tcpAcceptor;
        private HttpConnector httpConnector;
        private SchedulerProvider schedulerProvider;
        private WsConnector wsConnector;

        public ConnectorStatement(Statement base) {
            this.base = base;
        }

        @Override
        public void evaluate() throws Throwable {
            try {
                // Connector setup
                schedulerProvider = new SchedulerProvider();

                addressFactory = ResourceAddressFactory.newResourceAddressFactory();
                TransportFactory transportFactory = TransportFactory.newTransportFactory(Collections.emptyMap());
                BridgeServiceFactory serviceFactory = new BridgeServiceFactory(transportFactory);

                tcpAcceptor = (NioSocketAcceptor) transportFactory.getTransport("tcp").getAcceptor();
                tcpAcceptor.setResourceAddressFactory(addressFactory);
                tcpAcceptor.setBridgeServiceFactory(serviceFactory);
                tcpAcceptor.setSchedulerProvider(schedulerProvider);

                tcpConnector = (NioSocketConnector) transportFactory.getTransport("tcp").getConnector();
                tcpConnector.setResourceAddressFactory(addressFactory);
                tcpConnector.setBridgeServiceFactory(serviceFactory);
                tcpConnector.setTcpAcceptor(tcpAcceptor);

                httpConnector = (HttpConnector) transportFactory.getTransport("http").getConnector();
                httpConnector.setBridgeServiceFactory(serviceFactory);
                httpConnector.setResourceAddressFactory(addressFactory);

                wsConnector = (WsConnector) transportFactory.getTransport("ws").getConnector();

                wsnConnector = (WsnConnector) transportFactory.getTransport("wsn").getConnector();
                wsnConnector.setConfiguration(new Properties());
                wsnConnector.setBridgeServiceFactory(serviceFactory);
                wsnConnector.setSchedulerProvider(schedulerProvider);
                wsnConnector.setResourceAddressFactory(addressFactory);
                wsnConnector.setWsConnector(wsConnector);

                base.evaluate();
            } finally {
                tcpConnector.dispose();
                tcpAcceptor.dispose();
                httpConnector.dispose();
                wsnConnector.dispose();
                wsConnector.dispose();
                schedulerProvider.shutdownNow();
            }
        }

    }

    public Map<String, Object> getConnectOptions() {
        return connectOptions;
    }

}
