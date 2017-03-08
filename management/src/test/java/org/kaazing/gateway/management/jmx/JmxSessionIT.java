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
import static org.junit.Assert.assertEquals;
import static org.kaazing.gateway.management.test.util.TlsTestUtil.getKeystoreFileLocation;
import static org.kaazing.gateway.management.test.util.TlsTestUtil.keyStore;
import static org.kaazing.gateway.management.test.util.TlsTestUtil.password;

import java.security.KeyStore;
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
import org.kaazing.k3po.junit.annotation.ScriptProperty;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MethodExecutionTrace;

public class JmxSessionIT {

    private static final String ECHO_WSN_SERVICE = "echoWsn";
    private static final String JMX_URI = "service:jmx:rmi:///jndi/rmi://localhost:2020/jmxrmi";
    private static final String WS_URI = "ws://localhost:8001/echo";

    protected static final String ADMIN = "AUTHORIZED";
    private final KeyStore keyStore = keyStore();
    private final char[] password = password();

    private K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/closing");

    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            @SuppressWarnings("deprecation")
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .name(ECHO_WSN_SERVICE)
                            .accept(WS_URI)
                            .type("echo")
                            .crossOrigin()
                                .allowOrigin("*")
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
    private TestRule timeout = ITUtil.timeoutRule(10, SECONDS);
    private TestRule trace = new MethodExecutionTrace();

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(gateway).around(k3po).around(jmxConnection).around(timeout);

    @Specification("client.send.empty.close.frame/handshake.request.and.frame")
    @ScriptProperty("location 'http://localhost:8001/echo'")
    @Test
    public void sessionWhichHasClosedShouldBeRemovedFromCurrentSessionCountAndActiveSessions() throws Exception {
        k3po.finish();

        // to make sure jmx is updated
        Thread.sleep(1000);

        MBeanServerConnection mbeanServerConn = jmxConnection.getConnection();
        ObjectName summaryBeansObjectNamePattern = new ObjectName(
                "org.kaazing.gateway.server.management:root=gateways,subtype=services,serviceType=echo,serviceId=\""
                        + ECHO_WSN_SERVICE + "\",name=summary,*");
        Set<ObjectName> mbeanNames = mbeanServerConn.queryNames(summaryBeansObjectNamePattern, null);
        assertEquals(1, mbeanNames.size());
        ObjectName summaryBean = mbeanNames.iterator().next();
        assertEquals(Long.valueOf(1), (Long) mbeanServerConn.getAttribute(summaryBean, "NumberOfCumulativeSessions"));
        assertEquals(Long.valueOf(0), (Long) mbeanServerConn.getAttribute(summaryBean, "NumberOfCurrentSessions"));

        mbeanNames = mbeanServerConn.queryNames(
                ObjectName.getInstance("*:serviceType=echo,name=sessions,*"), null);
        assertEquals("The set of sessions should be empty", 0, mbeanNames.size());
    }
}
