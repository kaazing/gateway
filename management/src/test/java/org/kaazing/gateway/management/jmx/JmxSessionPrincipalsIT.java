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

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.management.test.util.TlsTestUtil.getKeystoreFileLocation;
import static org.kaazing.gateway.management.test.util.TlsTestUtil.keyStore;
import static org.kaazing.gateway.management.test.util.TlsTestUtil.password;
import static org.kaazing.gateway.management.test.util.TlsTestUtil.trustStore;

import java.security.KeyStore;
import java.util.Arrays;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.kaazing.gateway.management.test.util.JmxRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MemoryAppender;
import org.kaazing.test.util.MethodExecutionTrace;

/**
 * This test verifies the correct functioning of the configured user principal classes. Each test opens three
 * connections by running three scripts each of which opens a connection and does not close it.
 *
 * IMPLEMENTATION NOTE:
 * Calling k3po.finish() verifies the scripts have completed but does not cause the connections to be closed
 * (K3poRule only does that after the test method completes).
 */
public class JmxSessionPrincipalsIT {

    protected static final String JMX_URI = "service:jmx:rmi:///jndi/rmi://localhost:2020/jmxrmi";
    protected static final String WS_URI = "ws://localhost:8001/echo";

    protected static final String ADMIN = "AUTHORIZED";
    protected static final String ECHO_WS_SERVICE = "echoWS";

    protected final KeyStore keyStore = keyStore();
    protected final char[] password = password();
    protected final KeyStore trustStore = trustStore();

    private K3poRule k3po = new K3poRule();

    private GatewayRule gateway = new GatewayRule() {
        {

            init(getGatewayConfiguration());
        }
    };

    protected GatewayConfiguration getGatewayConfiguration() {
        // @formatter:off
        @SuppressWarnings("deprecation")
        GatewayConfiguration configuration =
                new GatewayConfigurationBuilder()
                    .property("org.kaazing.gateway.transport.ws.CLOSE_TIMEOUT",  "1s") // speed up the test
                    .service()
                        .accept(WS_URI)
                        .type("echo")
                        .name(ECHO_WS_SERVICE)
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
        return configuration;
    }

    private JmxRule jmxConnection = new JmxRule(JMX_URI);
    private TestRule timeout = ITUtil.timeoutRule(20, SECONDS);
    private TestRule trace = new MethodExecutionTrace();

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(gateway).around(k3po).around(jmxConnection).around(timeout);

    @Specification({
        "wsn.session.with.user.principal.joe",
        "wse.session.with.user.principal.joe",
        "wsn.session.with.user.principal.ann" })
    @Test
    public void sessionAttributePrincipalsShouldListUserPrincipals() throws Exception {
        k3po.finish();

        MBeanServerConnection mbeanServerConn = jmxConnection.getConnection();
        Set<ObjectName> mbeanNames = mbeanServerConn.queryNames(
                ObjectName.getInstance("*:serviceType=echo,name=sessions,*"), null);
        for (ObjectName name : mbeanNames) {
            String principals = (String) mbeanServerConn.getAttribute(name, "Principals");
            System.out.println(format("Session %s, principals=\"%s\"", name, principals));
            assertTrue(format("principles should contain UserPrincipal: Session %s, principals=\"%s\"", name, principals),
                    principals.contains("UserPrincipal"));
            assertTrue(format("principles should contain RolePrincipal: Session %s, principals=\"%s\"", name, principals),
                    principals.contains("RolePrincipal"));
        }

    }

    // Test should only kill sessions that have the "joe" user Principal
    @Specification({
        "wsn.session.with.user.principal.joe",
        "wse.session.with.user.principal.joe",
        "wsn.session.with.user.principal.ann" })
    @Test
    public void shouldCloseSessionsByUserPrincipal() throws Exception {
        shouldCloseSessionsByUserPrincipal("org.kaazing.gateway.management.test.util.TokenCustomLoginModule$UserPrincipal");
    }

    // Test should kill all sessions that have "TEST" as a role Principal
    @Specification({
        "wsn.session.with.user.principal.joe",
        "wse.session.with.user.principal.joe",
        "wsn.session.with.user.principal.ann" })
    @Test
    public void shouldCloseSessionsByRolePrincipal() throws Exception {
        shouldCloseSessionsByRolePrincipal("org.kaazing.gateway.management.test.util.TokenCustomLoginModule$RolePrincipal");
    }

    // Test should only kill sessions that have the "joe" user Principal
    protected final void shouldCloseSessionsByUserPrincipal(String userPrincipalClassName) throws Exception {
        k3po.finish();
        ObjectName echoServiceMbeanName = null;

        MBeanServerConnection mbeanServerConn = jmxConnection.getConnection();
        Set<ObjectName> mbeanNames = mbeanServerConn.queryNames(null, null);
        String MBeanPrefix = "subtype=services,serviceType=echo,serviceId=\"" + ECHO_WS_SERVICE + "\",name=summary";
        for (ObjectName name : mbeanNames) {
            if (name.toString().indexOf(MBeanPrefix) > 0) {
                echoServiceMbeanName = name;
                break;
            }
        }

        // Wait for all three sessions to be open
        long startTime = currentTimeMillis();
        Long numberOfCurrentSessions = (Long) mbeanServerConn.getAttribute(echoServiceMbeanName, "NumberOfCurrentSessions");
        while (numberOfCurrentSessions < 3 && (currentTimeMillis() - startTime) < 10000) {
            Thread.sleep(500);
            numberOfCurrentSessions = (Long) mbeanServerConn.getAttribute(echoServiceMbeanName, "NumberOfCurrentSessions");
        }
        assertEquals("All three sessions should be alive", (Long) 3L, numberOfCurrentSessions);

        ObjectName targetService = new ObjectName(echoServiceMbeanName.toString());
        Object[] params = {"joe", userPrincipalClassName};
        String[] signature = {String.class.getName(), String.class.getName()};
        mbeanServerConn.invoke(targetService, "closeSessions", params, signature);

        startTime = currentTimeMillis();
        numberOfCurrentSessions = (Long) mbeanServerConn.getAttribute(echoServiceMbeanName, "NumberOfCurrentSessions");
        while (numberOfCurrentSessions > 1 && (currentTimeMillis() - startTime) < 10000) {
            Thread.sleep(500);
            numberOfCurrentSessions = (Long) mbeanServerConn.getAttribute(echoServiceMbeanName, "NumberOfCurrentSessions");
        }

        assertEquals("Just Ann's Wsn session should still be alive", (Long) 1L, numberOfCurrentSessions);
    }

    // Test should kill all sessions that have "TEST" as a role Principal
    // please see "Jmx should KillSessions By Role Principal can fail if invoked early in session initialization #448"
    protected final void shouldCloseSessionsByRolePrincipal(String rolePrincipalClassName) throws Exception {
        k3po.finish();
        ObjectName echoServiceMbeanName = null;
        MBeanServerConnection mbeanServerConn = jmxConnection.getConnection();
        Set<ObjectName> mbeanNames = mbeanServerConn.queryNames(null, null);
        String MBeanPrefix = "subtype=services,serviceType=echo,serviceId=\"" + ECHO_WS_SERVICE + "\",name=summary";
        for (ObjectName name : mbeanNames) {
            if (name.toString().indexOf(MBeanPrefix) > 0) {
                echoServiceMbeanName = name;
                break;
            }
        }

        // Wait for all three sessions to be open
        long startTime = currentTimeMillis();
        Long numberOfCurrentSessions = (Long) mbeanServerConn.getAttribute(echoServiceMbeanName, "NumberOfCurrentSessions");
        while (numberOfCurrentSessions < 3 && (currentTimeMillis() - startTime) < 10000) {
            Thread.sleep(500);
            numberOfCurrentSessions = (Long) mbeanServerConn.getAttribute(echoServiceMbeanName, "NumberOfCurrentSessions");
        }
        assertEquals("All three sessions should be alive", (Long) 3L, numberOfCurrentSessions);

        ObjectName targetService = new ObjectName(echoServiceMbeanName.toString());
        Object[] params = {"TEST", rolePrincipalClassName};
        String[] signature = {String.class.getName(), String.class.getName()};

        mbeanServerConn.invoke(targetService, "closeSessions", params, signature);

        startTime = currentTimeMillis();
        numberOfCurrentSessions = (Long) mbeanServerConn.getAttribute(echoServiceMbeanName, "NumberOfCurrentSessions");
        while (numberOfCurrentSessions > 0 && (currentTimeMillis() - startTime) < 10000) {
            Thread.sleep(500);
            numberOfCurrentSessions = (Long) mbeanServerConn.getAttribute(echoServiceMbeanName, "NumberOfCurrentSessions");
        }

        assertEquals("Not all sessions have been closed", (Long) 0L, numberOfCurrentSessions);
    }



    // Test case for issue #783: closeSessions (by principal) service bean method causes NPEs
    @Specification({
        "wsn.session.with.user.principal.joe"})
    @Test
    public void shouldNotThrowNullPointerExceptionWhenCloseSessionsIsExecuted() throws Exception {
        k3po.finish();
        ObjectName echoServiceMbeanName = null;

        MBeanServerConnection mbeanServerConn = jmxConnection.getConnection();
        Set<ObjectName> mbeanNames = mbeanServerConn.queryNames(null, null);
        String MBeanPrefix = "subtype=services,serviceType=echo,serviceId=\"" + ECHO_WS_SERVICE + "\",name=summary";
        for (ObjectName name : mbeanNames) {
            if (name.toString().indexOf(MBeanPrefix) > 0) {
                echoServiceMbeanName = name;
                break;
            }
        }

        // Wait for the session to be open
        long startTime = currentTimeMillis();
        Long numberOfCurrentSessions = (Long) mbeanServerConn.getAttribute(echoServiceMbeanName, "NumberOfCurrentSessions");
        while (numberOfCurrentSessions < 1 && (currentTimeMillis() - startTime) < 10000) {
            Thread.sleep(500);
            numberOfCurrentSessions = (Long) mbeanServerConn.getAttribute(echoServiceMbeanName, "NumberOfCurrentSessions");
        }
        assertEquals("One session should be alive", (Long) 1L, numberOfCurrentSessions);

        ObjectName targetService = new ObjectName(echoServiceMbeanName.toString());
        Object[] params = {"joe", "org.kaazing.gateway.management.test.util.TokenCustomLoginModule$UserPrincipal"};
        String[] signature = {String.class.getName(), String.class.getName()};
        mbeanServerConn.invoke(targetService, "closeSessions", params, signature);

        startTime = currentTimeMillis();
        numberOfCurrentSessions = (Long) mbeanServerConn.getAttribute(echoServiceMbeanName, "NumberOfCurrentSessions");
        while (numberOfCurrentSessions > 0 && (currentTimeMillis() - startTime) < 10000) {
            Thread.sleep(500);
            numberOfCurrentSessions = (Long) mbeanServerConn.getAttribute(echoServiceMbeanName, "NumberOfCurrentSessions");
        }

        assertEquals("Wsn session should have been killed", (Long) 0L, numberOfCurrentSessions);
        MemoryAppender.assertLogMessages(
                null,
                Arrays.asList("Error during doSessionClosed session listener notifications"), // forbidden patterns
                null,
                Arrays.asList(NullPointerException.class), // forbidden exceptions
                null,
                false);
    }

}
