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
import static org.junit.Assert.assertEquals;
import static org.kaazing.gateway.management.test.util.TlsTestUtil.getKeystoreFileLocation;
import static org.kaazing.gateway.management.test.util.TlsTestUtil.keyStore;
import static org.kaazing.gateway.management.test.util.TlsTestUtil.password;
import static org.kaazing.gateway.management.test.util.TlsTestUtil.trustStore;

import java.security.KeyStore;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.management.test.util.JmxRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class JmxSessionPrincipalIT {

    private static final String JMX_URI = "service:jmx:rmi:///jndi/rmi://localhost:2020/jmxrmi";
    private static final String WS_URI = "ws://localhost:8001/echo";

    protected static final String ADMIN = "AUTHORIZED";
    protected static final String ECHO_WSN_SERVICE = "echoWSN";

    private final KeyStore keyStore = keyStore();
    private final char[] password = password();
    private final KeyStore trustStore = trustStore();

    private K3poRule k3po = new K3poRule();

    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .accept(WS_URI)
                            .type("echo")
                            .name(ECHO_WSN_SERVICE)
                            .crossOrigin()
                                .allowOrigin("*")
                            .done()
                            .realmName("demo")
                            .authorization()
                                .requireRole("TEST")
                            .done()
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
                                .name("demo")
                                .description("Kaazing WebSocket Gateway Demo")
                                .httpChallengeScheme("Application Token")
                                .httpQueryParameter("token")
                                .userPrincipalClass("org.kaazing.gateway.management.test.util.TokenCustomLoginModule$RolePrincipal")
                                .userPrincipalClass("org.kaazing.gateway.management.test.util.TokenCustomLoginModule$UserPrincipal")
                                .loginModule()
                                    .type("class:org.kaazing.gateway.management.test.util.TokenCustomLoginModule")
                                    .success("required")
                                .done()
                            .done()
                            .realm()
                                .name("jmxrealm")
                                .description("realm for jmx")
                                .httpChallengeScheme("Application Basic")
                                .loginModule()
                                    .type("class:org.kaazing.gateway.management.test.util.TestLoginModule")
                                    .success("required")
                                .done()
                            .done()
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    private JmxRule jmxConnection = new JmxRule(JMX_URI);
    public Timeout timeout = Timeout.seconds(10); 

    @Rule
    public TestRule chain = RuleChain.outerRule(gateway).around(k3po).around(jmxConnection).around(timeout);

    // Test should only kill sessions that have the "joe" user Principal
    @Specification({
        "wsn.session.with.user.principal.joe",
        "wse.session.with.user.principal.joe",
        "wsn.session.with.user.principal.ann" })
    @Test
    public void shouldKillSessionsByUserPrincipal() throws Exception {
        ObjectName echoServiceMbeanName = null;

        k3po.start();

        k3po.awaitBarrier("JOE_WSN_SESSION_ESTABLISHED");
        k3po.awaitBarrier("JOE_WSE_SESSION_ESTABLISHED");
        k3po.awaitBarrier("ANN_WSN_SESSION_ESTABLISHED");

        MBeanServerConnection mbeanServerConn = jmxConnection.getConnection();
        Set<ObjectName> mbeanNames = mbeanServerConn.queryNames(null, null);
        String MBeanPrefix = "subtype=services,serviceType=echo,serviceId=\"" + ECHO_WSN_SERVICE + "\",name=summary";
        for (ObjectName name : mbeanNames) {
            if (name.toString().indexOf(MBeanPrefix) > 0) {
                echoServiceMbeanName = name;

                ObjectName targetService = new ObjectName(name.toString());
                Object[] params = {"joe", "org.kaazing.gateway.management.test.util.TokenCustomLoginModule$UserPrincipal"};
                String[] signature = {String.class.getName(), String.class.getName()};

                mbeanServerConn.invoke(targetService, "closeSessions", params, signature);
            }
        }

        long startTime = currentTimeMillis();
        Long numberOfCurrentSessions = (Long) mbeanServerConn.getAttribute(echoServiceMbeanName, "NumberOfCurrentSessions");
        while (numberOfCurrentSessions > 1 && (currentTimeMillis() - startTime) < 5000) {
            Thread.sleep(500);
            numberOfCurrentSessions = (Long) mbeanServerConn.getAttribute(echoServiceMbeanName, "NumberOfCurrentSessions");
        }

        assertEquals("Ann Wsn session should still be alive", (Long) 1L, numberOfCurrentSessions);

        k3po.finish();
    }

    // Test should kill all sessions that have "TEST" as a role Principal
    // please see "Jmx should KillSessions By Role Principal can fail if invoked early in session initialization #448"
    @Specification({
        "wsn.session.with.user.principal.joe",
        "wse.session.with.user.principal.joe",
        "wsn.session.with.user.principal.ann" })
    @Test
    @Ignore("https://github.com/kaazing/tickets/issues/448")
    public void shouldKillSessionsByRolePrincipal() throws Exception {
        ObjectName echoServiceMbeanName = null;

        k3po.start();

        k3po.awaitBarrier("JOE_WSN_SESSION_ESTABLISHED");
        k3po.awaitBarrier("JOE_WSE_SESSION_ESTABLISHED");
        k3po.awaitBarrier("ANN_WSN_SESSION_ESTABLISHED");
        Thread.sleep(2000);
        MBeanServerConnection mbeanServerConn = jmxConnection.getConnection();
        Set<ObjectName> mbeanNames = mbeanServerConn.queryNames(null, null);
        String MBeanPrefix = "subtype=services,serviceType=echo,serviceId=\"" + ECHO_WSN_SERVICE + "\",name=summary";
        for (ObjectName name : mbeanNames) {
            if (name.toString().indexOf(MBeanPrefix) > 0) {
                echoServiceMbeanName = name;

                ObjectName targetService = new ObjectName(name.toString());
                Object[] params = {"TEST", "org.kaazing.gateway.management.test.util.TokenCustomLoginModule$RolePrincipal"};
                String[] signature = {String.class.getName(), String.class.getName()};

                mbeanServerConn.invoke(targetService, "closeSessions", params, signature);
            }
        }

        long startTime = currentTimeMillis();
        Long numberOfCurrentSessions = (Long) mbeanServerConn.getAttribute(echoServiceMbeanName, "NumberOfCurrentSessions");
        while (numberOfCurrentSessions > 0 && (currentTimeMillis() - startTime) < 5000) {
            Thread.sleep(500);
            numberOfCurrentSessions = (Long) mbeanServerConn.getAttribute(echoServiceMbeanName, "NumberOfCurrentSessions");
        }

        assertEquals("Not all sessions have been closed", (Long) 0L, numberOfCurrentSessions);

        k3po.finish();
    }

}