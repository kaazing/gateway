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
package org.kaazing.gateway.transport.http;

import static org.kaazing.gateway.util.InternalSystemProperty.HTTPXE_SPECIFICATION;

import java.util.Collections;
import java.util.HashMap;
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
import org.kaazing.gateway.util.scheduler.SchedulerProvider;

/**
 * Declaring an instance of this class as a @Rule causes the acceptor to be
 * started in process before each test method and stopped after it. The rule
 * can be chained with a K3poRule for use with robot (this causes Robot to be
 * started before the acceptor and stopped after it).
 */
public class HttpAcceptorRule implements TestRule {

    private ResourceAddressFactory addressFactory;
    private HttpAcceptor httpAcceptor;
    private Map<String, Object> acceptOptions = new HashMap<>();

    @Override
    public Statement apply(Statement base, Description description) {
        return new AcceptorStatement(base);
    }

    public void bind(String address, IoHandler acceptHandler) {
        ResourceAddress resourceAddress = 
                addressFactory.newResourceAddress(address, getAcceptOptions());

        bind(resourceAddress, acceptHandler);
    }

    public Map<String, Object> getAcceptOptions() {
        return acceptOptions;
    }

    public void bind(ResourceAddress acceptAddress, IoHandler acceptHandler) {
        httpAcceptor.bind(acceptAddress, acceptHandler, null);
    }

    private final class AcceptorStatement extends Statement {

        private final Statement base;

        public AcceptorStatement(Statement base) {
            this.base = base;
        }

        @Override
        public void evaluate() throws Throwable {
            SchedulerProvider schedulerProvider = new SchedulerProvider();
            TransportFactory transportFactory = TransportFactory.newTransportFactory(Collections.emptyMap());
            addressFactory = ResourceAddressFactory.newResourceAddressFactory();
            Properties config = new Properties();
            config.setProperty(HTTPXE_SPECIFICATION.getPropertyName(), "true");

            Map<String, Object> resources = new HashMap<>();
            resources.put("schedulerProvider", schedulerProvider);
            resources.put("configuration", new Properties());
            resources.put("bridgeServiceFactory", new BridgeServiceFactory(transportFactory));
            resources.put("resourceAddressFactory", addressFactory);
            resources.put("configuration", config);
            transportFactory.injectResources(resources);

            httpAcceptor = (HttpAcceptor) transportFactory.getTransport("http").getAcceptor();

            try {
                base.evaluate();
            } finally {
                httpAcceptor.dispose();
                transportFactory.getTransport("tcp").getAcceptor().dispose();
                schedulerProvider.shutdownNow();
            }
        }
    }

}
