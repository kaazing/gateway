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
 * Declaring an instance of this class as a @Rule causes the gateway to be
 * started in process before each test method and stopped after it. The rule
 * can be chained with a K3poRule for use with robot (this causes Robot to be
 * started before the gateway and stopped after it).
 */
public class WsnAcceptorRule implements TestRule {

    private ResourceAddressFactory addressFactory;
    private WsnAcceptor wsnAcceptor;

    @Override
    public Statement apply(Statement base, Description description) {
        return new AcceptorStatement(base);
    }

    public WsnAcceptorRule() {
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

        public AcceptorStatement(Statement base) {
            this.base = base;
        }

        @Override
        public void evaluate() throws Throwable {
            SchedulerProvider schedulerProvider = new SchedulerProvider();
            addressFactory = ResourceAddressFactory.newResourceAddressFactory();
            TransportFactory transportFactory = TransportFactory.newTransportFactory(Collections.emptyMap());

            Map<String, Object> resources = new HashMap<>();
            resources.put("schedulerProvider", schedulerProvider);
            resources.put("configuration", new Properties());
            resources.put("bridgeServiceFactory", new BridgeServiceFactory(transportFactory));
            resources.put("resourceAddressFactory", addressFactory);
            transportFactory.injectResources(resources);

            wsnAcceptor = (WsnAcceptor)transportFactory.getTransport("wsn").getAcceptor();

            try {
                base.evaluate();
            } finally {
                wsnAcceptor.dispose();
                transportFactory.getTransport("http").getAcceptor().dispose();
                transportFactory.getTransport("tcp").getAcceptor().dispose();
                schedulerProvider.shutdownNow();
            }
        }

    }

}
