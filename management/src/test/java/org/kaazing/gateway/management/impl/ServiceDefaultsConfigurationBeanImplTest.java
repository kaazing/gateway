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
package org.kaazing.gateway.management.impl;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;

public class ServiceDefaultsConfigurationBeanImplTest {

    private final String SNMP_SERVICE_ACCEPT_URL = "ws://localhost:8000/snmp";
    private final String SNMP_SERVICE_NAME = "SNMP Management Service";
    private final String SNMP_SERVICE_TYPE = "management.snmp";

    @Rule
    public TestRule chain = createRuleChain(20, SECONDS);

    @Test
    public void testServiceDefaultsWithoutSslCiphers() throws Exception {
        /**
         * Point of test is create a service with service defaults that do not contain any specification
         * of sslCiphers and see what we get from the service defaults management bean. It should correctly
         * give us a JSON string without any mention of "ssl.ciphers".
         */
        GatewayConfigurationBuilder configBuilder = new GatewayConfigurationBuilder();

        GatewayConfiguration gatewayConfiguration =
                configBuilder
                        .service()
                        .name(SNMP_SERVICE_NAME)
                        .accept(SNMP_SERVICE_ACCEPT_URL)
                        .type(SNMP_SERVICE_TYPE)
                        .done()
                        .serviceDefaults()
                        .acceptOption("ssl.encryption", "enabled")
                        .acceptOption("ssl.verify.client", "none")
                        .done()
                        .done();

        Gateway gateway = new Gateway();

        try {
            gateway.start(gatewayConfiguration);

            // If we hit the bug (KG-10371) this is for, we'd get an NPE because of missing ssl.ciphers
        } finally {
            gateway.stop();
        }
    }

    @Test
    public void testServiceDefaultsWithSslCiphers() throws Exception {
        /**
         * Point of test is create a service with service defaults that do not contain any specification
         * of sslCiphers and see what we get from the service defaults management bean. It should correctly
         * give us a JSON string without any mention of "ssl.ciphers".
         */
        GatewayConfigurationBuilder configBuilder = new GatewayConfigurationBuilder();

        GatewayConfiguration gatewayConfiguration =
                configBuilder
                        .service()
                        .name(SNMP_SERVICE_NAME)
                        .accept(SNMP_SERVICE_ACCEPT_URL)
                        .type(SNMP_SERVICE_TYPE)
                        .done()
                        .serviceDefaults()
                        .acceptOption("ssl.encryption", "enabled")
                        .acceptOption("ssl.ciphers", "HIGH")
                        .done()
                        .done();

        Gateway gateway = new Gateway();

        try {
            gateway.start(gatewayConfiguration);

            // How do we check the management stuff here?
        } finally {
            gateway.stop();
        }
    }

}
