/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.management.jmx;

import static java.net.URI.create;
import static org.junit.Assert.assertEquals;
import static org.kaazing.gateway.server.Gateway.GATEWAY_CONFIG_DIRECTORY_PROPERTY;

import java.security.KeyStore;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.AttributeList;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class JmxIT {

    private static final String JMX_URI = "service:jmx:rmi:///jndi/rmi://localhost:2010/jmxrmi";

    protected static final String ADMIN = "ADMINISTRATOR";

    private final KeyStore keyStore = TlsTestUtil.keyStore();
    private final char[] password = TlsTestUtil.password();
    private final KeyStore trustStore = TlsTestUtil.trustStore();

    private K3poRule k3po = new K3poRule();

    private DisableOnDebug timeoutRule =
            new DisableOnDebug(Timeout.builder().withTimeout(5, TimeUnit.SECONDS).withLookingForStuckThread(true).build());

    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .accept(create("ws://localhost:8555/echo"))
                            .type("echo")
                            .crossOrigin()
                                .allowOrigin("http://localhost:8001")
                            .done()
                        .done()
                        .service()
                            .property("connector.server.address", "jmx://localhost:2010/")
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
                            .keyStoreFile(TlsTestUtil.getKeystoreFileLocation())
                            .realm()
                                .name("jmxrealm")
                                .description("realm for jmx")
                                .httpChallengeScheme("Application Basic")
                                .loginModule()
                                    .type("file")
                                    .success("required")
                                    .option("file", "jaas-config.xml")
                                .done()
                            .done()
                        .done()
                        .property(GATEWAY_CONFIG_DIRECTORY_PROPERTY, "/Users/David/Desktop/kaazing-websocket-gateway-4.0.9/conf")
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    private JmxRule jmxConnection = new JmxRule(JMX_URI);

    @Rule
    public TestRule chain = RuleChain.outerRule(k3po).around(timeoutRule).around(gateway).around(jmxConnection);

    @Specification("simple.echo.for.session.cnt")
    @Test
    @Ignore("https://github.com/kaazing/gateway/issues/331")
    public void echoServiceCrossOriginAllow() throws Exception {
        k3po.start();
        k3po.awaitBarrier("SESSION_ESTABLISHED");
        String attributeNames[] = {"NumberOfCurrentSessions", "NumberOfCurrentNativeSessions", "NumberOfCurrentEmulatedSessions",
                "TotalMessageSentCount"};
        // Could do this cleaner
        MBeanServerConnection mbeanServerConn = jmxConnection.getConnection();
        Set<ObjectName> mbeanNames = mbeanServerConn.queryNames(null, null);
        String MBeanPrefix = "subtype=services,serviceType=echo";
        for (ObjectName name : mbeanNames) {
            if (name.toString().indexOf(MBeanPrefix) > 0) {
                AttributeList attrValues = mbeanServerConn.getAttributes(name, attributeNames);
                for (int i = 0; i < attrValues.size(); i++) {
                    if (attrValues.get(i).toString().contains("NumberOfCurrentSessions")) {
                        assertEquals("NumberOfCurrentSessions = 1", attrValues.get(i));
                    }
                }
            }
        }

        k3po.notifyBarrier("READ_NUM_OF_SESSIONS");

        k3po.finish();
    }

}
