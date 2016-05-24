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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
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
import org.kaazing.gateway.util.scheduler.SchedulerProvider;


/**
 * Declaring an instance of this class as a @Rule causes the gateway to be
 * started in process before each test method and stopped after it. The rule
 * can be chained with a K3poRule for use with robot (this causes Robot to be
 * started before the gateway and stopped after it).
 */
public class WsnAcceptorRule implements TestRule {

    private final String log4jPropertiesResourceName;
    private ResourceAddressFactory addressFactory;
    private WsnAcceptor wsnAcceptor;

    @Override
    public Statement apply(Statement base, Description description) {
        return new AcceptorStatement(base);
    }

    public WsnAcceptorRule() {
        this(null);
    }

    public WsnAcceptorRule(String log4jPropertiesResourceName) {
        this.log4jPropertiesResourceName = log4jPropertiesResourceName;
    }

    public void bind(String accept, IoHandler acceptHandler) {

        final ResourceAddress acceptAddress =
                addressFactory.newResourceAddress(accept);

        wsnAcceptor.bind(acceptAddress, acceptHandler, null);
    }
   
   public void bind(ResourceAddress acceptAddress, IoHandler acceptHandler) {
       wsnAcceptor.bind(acceptAddress, acceptHandler, null);
   }

   private final class AcceptorStatement extends Statement {

        private final Statement base;

        private NioSocketConnector tcpConnector;
        private NioSocketAcceptor tcpAcceptor;
        private WsAcceptor wsAcceptor;
        private HttpAcceptor httpAcceptor;
        private SchedulerProvider schedulerProvider;

        public AcceptorStatement(Statement base) {
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
                in.close();
                PropertyConfigurator.configure(log4j);
            }
            try {
                // Connector setup
                schedulerProvider = new SchedulerProvider();

                addressFactory = ResourceAddressFactory.newResourceAddressFactory();
                TransportFactory transportFactory = TransportFactory.newTransportFactory(Collections.emptyMap());
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

                wsAcceptor = (WsAcceptor)transportFactory.getTransport("ws").getAcceptor();

                wsnAcceptor = (WsnAcceptor)transportFactory.getTransport("wsn").getAcceptor();
                wsnAcceptor.setConfiguration(new Properties());
                wsnAcceptor.setBridgeServiceFactory(serviceFactory);
                wsnAcceptor.setSchedulerProvider(schedulerProvider);
                wsnAcceptor.setResourceAddressFactory(addressFactory);
                wsnAcceptor.setWsAcceptor(wsAcceptor);

                base.evaluate();
            } finally {
                tcpConnector.dispose();
                tcpAcceptor.dispose();
                httpAcceptor.dispose();
                wsnAcceptor.dispose();
                schedulerProvider.shutdownNow();
            }
        }

    }


}
