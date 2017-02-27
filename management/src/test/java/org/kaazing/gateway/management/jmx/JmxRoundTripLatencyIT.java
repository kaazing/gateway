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

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.management.test.util.TlsTestUtil.getKeystoreFileLocation;
import static org.kaazing.gateway.management.test.util.TlsTestUtil.keyStore;
import static org.kaazing.gateway.management.test.util.TlsTestUtil.password;
import static org.kaazing.gateway.management.test.util.TlsTestUtil.trustStore;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.security.KeyStore;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.management.test.util.JmxRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class JmxRoundTripLatencyIT {

    private static final String ECHO_WSE_SERVICE = "echoWse";
    private static final String ECHO_WSN_SERVICE = "echoWsn";
    private static final String JMX_URI = "service:jmx:rmi:///jndi/rmi://localhost:2020/jmxrmi";
    private static final String WS_URI = "ws://localhost:8001/echo";
    private static final String WSE_URI = "wse://localhost:8123/echo";

    protected static final String ADMIN = "AUTHORIZED";

    private final KeyStore keyStore = keyStore();
    private final char[] password = password();
    private final KeyStore trustStore = trustStore();

    private K3poRule k3po = new K3poRule();

    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            @SuppressWarnings("deprecation")
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .name(ECHO_WSE_SERVICE)
                            .accept(WSE_URI)
                            .acceptOption("ws.inactivity.timeout", "2sec")
                            .type("echo")
                        .done()
                        .service()
                            .name(ECHO_WSN_SERVICE)
                            .accept(WS_URI)
                            .type("echo")
                            .crossOrigin()
                                .allowOrigin("*")
                            .done()
                            .acceptOption("ws.inactivity.timeout", "2sec")
                        .done()
                        .service()
                            .property("connector.server.address", "jmx://localhost:2020/")
                            .type("management.jmx")
                            .authorization()
                                .requireRole(ADMIN)
                            .done()
                            .realmName("jmxrealm")
                        .done()
                        .security()
                            .trustStore(trustStore)
                            .keyStore(keyStore)
                            .keyStorePassword(password)
                            .keyStoreFile(getKeystoreFileLocation())
                            .realm()
                                .name("jmxrealm")
                                .description("realm for jmx")
                                .httpChallengeScheme("Application Basic")
                                .loginModule()
                                    .type("class:org.kaazing.gateway.management.test.util.TestLoginModule")
                                    .success("requisite")
                                .done()
                            .done()
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    private JmxRule jmxConnection = new JmxRule(JMX_URI);

    @Rule
    public TestRule chain = createRuleChain(gateway, k3po).around(jmxConnection);


    @Specification("echoServiceToGetRoundTripLatencyAttributesFromJMX")
    @Test
    public void getRoundTripLatencyAttributesFromJmx() throws Exception {
        Long latency = null;
        Long latencyTimestamp = null;
        Long currentTimestamp = currentTimeMillis();

        k3po.start();

        k3po.awaitBarrier("SESSION_ESTABLISHED");

        MBeanServerConnection mbeanServerConn = jmxConnection.getConnection();
        Set<ObjectName> mbeanNames = mbeanServerConn.queryNames(null, null);
        String MBeanPrefix = "subtype=services,serviceType=echo,serviceId=\"" + ECHO_WSN_SERVICE + "\",name=sessions";
        for (ObjectName name : mbeanNames) {
            if (name.toString().indexOf(MBeanPrefix) > 0) {
                mbeanServerConn.getObjectInstance(name);

                latency = (Long) mbeanServerConn.getAttribute(name, "LastRoundTripLatency");
                latencyTimestamp = (Long) mbeanServerConn.getAttribute(name, "LastRoundTripLatencyTimestamp");
            }
        }

        assertTrue("Could not retrieve Round Trip Latency from Jmx", latency > -1);
        assertTrue("Could not retrieve Round Trip Latency Timestamp from Jmx", latencyTimestamp > currentTimestamp);
        k3po.finish();
    }

}
