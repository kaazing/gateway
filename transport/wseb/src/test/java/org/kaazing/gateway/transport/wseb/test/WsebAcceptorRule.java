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
package org.kaazing.gateway.transport.wseb.test;

import static org.kaazing.gateway.util.InternalSystemProperty.WSE_SPECIFICATION;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.apache.mina.core.service.IoHandler;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.http.HttpAcceptor;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.transport.nio.internal.NioSocketConnector;
import org.kaazing.gateway.transport.ws.WsAcceptor;
import org.kaazing.gateway.transport.wseb.WsebAcceptor;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;

/**
 * Declaring an instance of this class as a @Rule causes the gateway to be started in process before each test method and stopped
 * after it. The rule can be chained with a K3poRule for use with robot (this causes Robot to be started before the gateway and
 * stopped after it).
 */
public class WsebAcceptorRule implements TestRule {

    private final Properties configuration;
    private ResourceAddressFactory resourceAddressFactory;
    private WsebAcceptor wsebAcceptor;


    @Override
    public Statement apply(Statement base, Description description) {
        return new AcceptorStatement(base);
    }
    public WsebAcceptorRule() {
        this(new Properties());
    }

    public WsebAcceptorRule(Properties configuration) {
        this.configuration = configuration;
    }

    public void bind(final String accept,
                     IoHandler acceptHandler) throws InterruptedException {
        ResourceAddress acceptAddress = resourceAddressFactory.newResourceAddress(accept);
        wsebAcceptor.bind(acceptAddress, acceptHandler, null);
    }

    public void bind(ResourceAddress acceptAddress,
                     IoHandler acceptHandler) throws InterruptedException {
        wsebAcceptor.bind(acceptAddress, acceptHandler, null);
    }

    private final class AcceptorStatement extends Statement {

        private final Statement base;
        private Map<String, ?> config = Collections.emptyMap();
        private TransportFactory transportFactory = TransportFactory.newTransportFactory(config);
        private BridgeServiceFactory bridgeServiceFactory = new BridgeServiceFactory(transportFactory);

        private NioSocketConnector tcpConnector;
        private NioSocketAcceptor tcpAcceptor;
        private HttpAcceptor httpAcceptor;
        private WsAcceptor wsAcceptor;


        private SchedulerProvider schedulerProvider;

        public AcceptorStatement(Statement base) {
            this.base = base;
        }

        @Override
        public void evaluate() throws Throwable {
            try {
                // Connector setup
                resourceAddressFactory = ResourceAddressFactory.newResourceAddressFactory();
                tcpConnector = (NioSocketConnector)transportFactory.getTransport("tcp").getConnector();
                tcpAcceptor = (NioSocketAcceptor)transportFactory.getTransport("tcp").getAcceptor();
                httpAcceptor = (HttpAcceptor)transportFactory.getTransport("http").getAcceptor();
                wsebAcceptor = (WsebAcceptor)transportFactory.getTransport("wseb").getAcceptor();
                wsAcceptor = (WsAcceptor)transportFactory.getTransport("ws").getAcceptor();

                schedulerProvider = new SchedulerProvider();

                tcpConnector.setTcpAcceptor(tcpAcceptor);
                tcpAcceptor.setBridgeServiceFactory(bridgeServiceFactory);
                tcpAcceptor.setResourceAddressFactory(resourceAddressFactory);
                tcpAcceptor.setSchedulerProvider(schedulerProvider);

                httpAcceptor.setBridgeServiceFactory(bridgeServiceFactory);
                httpAcceptor.setResourceAddressFactory(resourceAddressFactory);
                httpAcceptor.setSchedulerProvider(schedulerProvider);

                wsebAcceptor.setBridgeServiceFactory(bridgeServiceFactory);
                wsebAcceptor.setResourceAddressFactory(resourceAddressFactory);
                wsebAcceptor.setSchedulerProvider(schedulerProvider);
                wsebAcceptor.setWsAcceptor(wsAcceptor);
                configuration.setProperty(WSE_SPECIFICATION.getPropertyName(), "true");
                wsebAcceptor.setConfiguration(configuration);

                base.evaluate();
            } finally {
                tcpConnector.dispose();
                tcpAcceptor.dispose();
                httpAcceptor.dispose();
                wsebAcceptor.dispose();
                schedulerProvider.shutdownNow();
            }
        }

    }
}

