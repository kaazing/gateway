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
package org.kaazing.gateway.management.jmx;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;

public class JmxManagementServiceHandlerTest {

    private final String FRONTEND_URI_1 = "tcp://localhost:8123";
    private final String FRONTEND_URI_2 = "tcp://localhost:8124";
    private final String BACKEND_URI_1 = "tcp://localhost:9123";
    private final String BACKEND_URI_2 = "tcp://localhost:9124";
    private final String PROXY = "proxy";

    @Rule
    public TestRule timeoutRule = createRuleChain(20, SECONDS);

    @Test
    public void testNoJMXBindingNameConflictsOnMultiServicesUsingSameConnect() throws Exception {
        /**
         * Point of test is to make sure that the JMX bean created for the services do not throw an exception about
         * registering two services to the same name
         */
        GatewayConfigurationBuilder configBuilder = new GatewayConfigurationBuilder();
        GatewayConfiguration gatewayConfiguration = configBuilder.service().name("sameName").accept(FRONTEND_URI_1)
                .connect(BACKEND_URI_1).type(PROXY).done().service().accept(FRONTEND_URI_2).connect(BACKEND_URI_1)
                .type(PROXY).done().done();

        Gateway gateway = new Gateway();
        try {
            gateway.start(gatewayConfiguration);
        } finally {
            gateway.stop();
        }
    }

    @Test
    public void testNoJMXBindingNameConflictsOnMultiServicesUsingSameAccept() throws Exception {
        /**
         * Point of test is to make sure that the JMX bean created for the services do not throw an exception about
         * registering two services to the same name
         */
        boolean correctPath = false;

        GatewayConfigurationBuilder configBuilder = new GatewayConfigurationBuilder();
        GatewayConfiguration gatewayConfiguration = configBuilder
                .service()
                    .accept(FRONTEND_URI_1)
                    .connect(BACKEND_URI_1)
                    .type(PROXY)
                .done()
                .service()
                    .name("sameName")
                    .accept(FRONTEND_URI_1)
                    .connect(BACKEND_URI_2)
                    .type(PROXY)
                .done()
            .done();

        Gateway gateway = new Gateway();
        try {
            gateway.start(gatewayConfiguration);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            String message = e.getMessage();
            Assert.assertTrue("Got an exception that wasn't a binding error: " + message,
                    (message != null && message.contains("Error binding")));
            correctPath = true;
        } finally {
            gateway.stop();
        }

        Assert.assertTrue("The test did no go down the expected / correctPath", correctPath);
    }
}
