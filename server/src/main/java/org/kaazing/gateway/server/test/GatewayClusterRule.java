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
package org.kaazing.gateway.server.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.PropertyConfigurator;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;

/**
 * Declaring an instance of this class as a @Rule causes a Gateway cluster to be started in process before each test method and
 * stopped after it. An array of Gateway objects is passed to the inner rule which is assumed to be the test rule. This array can
 * be used to kill bounce Gateways while the test is running. The rule can be chained with a K3poRule for use with robot (this
 * causes Robot to be started before the gateway and stopped after it).
 */
public class GatewayClusterRule implements TestRule {

    private List<GatewayConfiguration> configurations;
    private List<Gateway> gateways = new ArrayList<>();
    private Map<GatewayConfiguration, Gateway> gatewaysByConfiguration = new HashMap<>();

    private String log4jPropertiesResourceName;

    @Override
    public Statement apply(Statement base, Description description) {
        return new GatewayStatement(base);
    }

    public void init(GatewayConfiguration... configurations) {
        this.configurations = Arrays.asList(configurations);
     }

    public void init(List<GatewayConfiguration> configurations) {
        this.configurations = configurations;
    }

    public void init(List<GatewayConfiguration> configurations, String log4jPropertiesResourceName) {
        init(configurations);
        this.log4jPropertiesResourceName = log4jPropertiesResourceName;
    }

    public Gateway getGateway(GatewayConfiguration configuration) {
        return gatewaysByConfiguration.get(configuration);
    }

    private final class GatewayStatement extends Statement {

        private final Statement base;

        public GatewayStatement(Statement base) {
            this.base = base;
        }

        @Override
        public void evaluate() throws Throwable {
            if (log4jPropertiesResourceName != null) {
                // Initialize log4j using a properties file available on the class path
                Properties log4j = new Properties();
                InputStream in = Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream(log4jPropertiesResourceName);
                if (in == null) {
                    throw new IOException(String.format("Could not load resource %s", log4jPropertiesResourceName));
                }
                log4j.load(in);
                PropertyConfigurator.configure(log4j);
            }

            try {
                for (GatewayConfiguration configuration : configurations) {
                    Gateway gateway = new Gateway();
                    gateway.start(configuration);
                    gateways.add(gateway);
                    gatewaysByConfiguration.put(configuration, gateway);
                }
                base.evaluate();
            } finally {
                for (Gateway gateway : gateways) {
                    // Calling stop on a non-running Gateway is OK.
                    gateway.stop();
                }
            }
        }
    }
}
