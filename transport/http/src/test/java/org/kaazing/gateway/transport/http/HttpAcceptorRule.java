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

package org.kaazing.gateway.transport.http;

import org.apache.mina.core.service.IoHandler;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.transport.nio.internal.NioSocketConnector;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;

import java.net.URI;
import java.util.Collections;


/**
 * Declaring an instance of this class as a @Rule causes the gateway to be
 * started in process before each test method and stopped after it. The rule
 * can be chained with a K3poRule for use with robot (this causes Robot to be
 * started before the gateway and stopped after it).
 */
public class HttpAcceptorRule implements TestRule {

    private ResourceAddressFactory addressFactory;
    private HttpAcceptor httpAcceptor;

    @Override
    public Statement apply(Statement base, Description description) {
        return new AcceptorStatement(base);
    }

    public void bind(String address, IoHandler acceptHandler) {
        ResourceAddress resourceAddress = addressFactory.newResourceAddress(URI.create(address));
        bind(resourceAddress, acceptHandler);
    }

    public void bind(ResourceAddress acceptAddress, IoHandler acceptHandler) {
        httpAcceptor.bind(acceptAddress, acceptHandler, null);
    }

    private final class AcceptorStatement extends Statement {

        private final Statement base;

        private NioSocketConnector tcpConnector;
        private NioSocketAcceptor tcpAcceptor;
        private SchedulerProvider schedulerProvider;

        public AcceptorStatement(Statement base) {
            this.base = base;
        }

        @Override
        public void evaluate() throws Throwable {
            try {
                schedulerProvider = new SchedulerProvider();

                addressFactory = ResourceAddressFactory.newResourceAddressFactory();
                TransportFactory transportFactory = TransportFactory.newTransportFactory(Collections.<String, Object> emptyMap());
                BridgeServiceFactory serviceFactory = new BridgeServiceFactory(transportFactory);

                tcpAcceptor = (NioSocketAcceptor)transportFactory.getTransport("tcp").getAcceptor();
                tcpAcceptor.setResourceAddressFactory(addressFactory);
                tcpAcceptor.setBridgeServiceFactory(serviceFactory);
                tcpAcceptor.setSchedulerProvider(schedulerProvider);

                tcpConnector = (NioSocketConnector)transportFactory.getTransport("tcp").getConnector();
                tcpConnector.setResourceAddressFactory(addressFactory);
                tcpConnector.setBridgeServiceFactory(serviceFactory);
                tcpConnector.setTcpAcceptor(tcpAcceptor);

                httpAcceptor = (HttpAcceptor)transportFactory.getTransport("http").getAcceptor();
                httpAcceptor.setBridgeServiceFactory(serviceFactory);
                httpAcceptor.setResourceAddressFactory(addressFactory);
                httpAcceptor.setSchedulerProvider(schedulerProvider);

                base.evaluate();
            } finally {
                tcpConnector.dispose();
                tcpAcceptor.dispose();
                httpAcceptor.dispose();
                schedulerProvider.shutdownNow();
            }
        }

    }

}
