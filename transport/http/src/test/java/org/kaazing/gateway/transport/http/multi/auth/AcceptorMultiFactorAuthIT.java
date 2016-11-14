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
package org.kaazing.gateway.transport.http.multi.auth;

import static javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;
import static org.kaazing.gateway.resource.address.ResourceAddressFactory.newResourceAddressFactory;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.INJECTABLE_HEADERS;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.resource.address.http.DefaultHttpRealmInfo;
import org.kaazing.gateway.resource.address.http.HttpRealmInfo;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.security.auth.context.DefaultLoginContextFactory;
import org.kaazing.gateway.server.ExpiringState;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpAcceptor;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class AcceptorMultiFactorAuthIT {
    private static final String FIRST_REALM = "firstFactorRealm";
    private static final String SECOND_REALM = "secondFactorRealm";

    private HttpAcceptor httpAcceptor;
    private ResourceAddress httpAddress;
    private Configuration configuration;
    private IoHandlerAdapter<HttpAcceptSession> acceptHandler;

    private K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/http/multi/auth");

    private TestRule timeoutRule = new DisableOnDebug(Timeout.builder().withTimeout(10, TimeUnit.SECONDS).build());

    @Rule
    public JUnitRuleMockery mockery = new JUnitRuleMockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
            setThreadingPolicy(new Synchroniser());
        }
    };
    private NioSocketAcceptor tcpAcceptor;

    @Rule
    public RuleChain chainRule = RuleChain.outerRule(k3po).around(timeoutRule);

    @Before
    public void setupAcceptor() {
        ResourceAddressFactory addressFactory = newResourceAddressFactory();
        Map<String, ?> config = Collections.emptyMap();
        TransportFactory transportFactory = TransportFactory.newTransportFactory(config);
        BridgeServiceFactory serviceFactory = new BridgeServiceFactory(transportFactory);

        HttpAcceptor httpAcceptor = (HttpAcceptor) transportFactory.getTransport("http").getAcceptor();
        httpAcceptor.setExpiringState(new ExpiringState() {
            private ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();

            @Override
            public Object remove(String key, Object value) {
                return map.remove(key, value);
            }

            @Override
            public Object putIfAbsent(String key, Object value, long ttl, TimeUnit timeunit) {
                return map.putIfAbsent(key, value);
            }

            @Override
            public Object get(String key) {
                return map.get(key);
            }
        });

        httpAcceptor.setBridgeServiceFactory(serviceFactory);
        httpAcceptor.setResourceAddressFactory(addressFactory);

        SchedulerProvider provider = new SchedulerProvider();
        httpAcceptor.setSchedulerProvider(provider);

        NioSocketAcceptor tcpAcceptor = (NioSocketAcceptor) transportFactory.getTransport("tcp").getAcceptor();
        tcpAcceptor.setSchedulerProvider(provider);
        tcpAcceptor.setResourceAddressFactory(addressFactory);
        tcpAcceptor.setBridgeServiceFactory(serviceFactory);

        String location = "http://localhost:8000/resource";
        ResourceOptions options = ResourceOptions.FACTORY.newResourceOptions();
        options.setOption(INJECTABLE_HEADERS, Collections.emptySet());

        HttpRealmInfo[] realms = new HttpRealmInfo[2];

        configuration = mockery.mock(Configuration.class);
        realms[0] = new DefaultHttpRealmInfo(FIRST_REALM, "Basic", "firstFactor", new String[0], new String[0], new String[0],
                new DefaultLoginContextFactory(FIRST_REALM, configuration), Collections.emptySet());
        realms[1] = new DefaultHttpRealmInfo(SECOND_REALM, "Basic", "secondFactor", new String[0], new String[0], new String[0],
                new DefaultLoginContextFactory(FIRST_REALM, configuration), Collections.emptySet());
        options.setOption(HttpResourceAddress.REALMS, realms);

        options.setOption(HttpResourceAddress.REQUIRED_ROLES, new String[]{"AUTHORIZED"});
        ResourceAddress httpAddress = addressFactory.newResourceAddress(location, options);

        this.tcpAcceptor = tcpAcceptor;
        this.httpAcceptor = httpAcceptor;

        this.httpAddress = httpAddress;

        acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                session.setStatus(HttpStatus.SUCCESS_OK);
                session.close(false);
            }
        };
    }

    @After
    public void disposeAcceptor() {
        if (httpAcceptor != null) {
            httpAcceptor.dispose();
        }

        if (tcpAcceptor != null) {
            tcpAcceptor.dispose();
        }
    }

    @Test
    @Specification({"request.with.secure.challenge.identity/client"})
    public void serverMayGiveSecChallengeIdentityHeaderWith401() throws Exception {

        mockery.checking(new Expectations() {
            {
                allowing(configuration).getAppConfigurationEntry(FIRST_REALM);
                final String loginModuleName = "org.kaazing.gateway.transport.http.multi.auth.FirstFactorLoginModule";
                final HashMap<String, Object> options = new HashMap<>();
                final AppConfigurationEntry entry = new AppConfigurationEntry(loginModuleName, REQUIRED, options);
                final String loginModuleName2 = "org.kaazing.gateway.transport.http.multi.auth.SecondFactorLoginModule";
                final AppConfigurationEntry entry2 = new AppConfigurationEntry(loginModuleName2, REQUIRED, options);
                will(onConsecutiveCalls(
                        // challenge first
                        returnValue(new AppConfigurationEntry[]{entry}),
                        // challenge first passed
                        returnValue(new AppConfigurationEntry[]{entry}),
                        // challenge second
                        returnValue(new AppConfigurationEntry[]{entry2}),
                        // challenge second passed
                        returnValue(new AppConfigurationEntry[]{entry2})));
            }
        });
        httpAcceptor.bind(httpAddress, acceptHandler, null);
        k3po.finish();
    }

    @Test
    @Specification({"request.missing.secure.challenge.identity/client"})
    public void serverShouldChallengeFirstFactorWhenSecChallengeIdentityHeaderMissing() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(configuration).getAppConfigurationEntry(FIRST_REALM);
                final String loginModuleName = "org.kaazing.gateway.transport.http.multi.auth.FirstFactorLoginModule";
                final HashMap<String, Object> options = new HashMap<>();
                final AppConfigurationEntry entry = new AppConfigurationEntry(loginModuleName, REQUIRED, options);
                final String loginModuleName2 =
                        "org.kaazing.gateway.transport.http.multi.auth.SecondFactorAlwaysFailLoginModule";
                final AppConfigurationEntry entry2 = new AppConfigurationEntry(loginModuleName2, REQUIRED, options);
                will(onConsecutiveCalls(
                        // challenge first
                        returnValue(new AppConfigurationEntry[]{entry}),
                        // challenge first passed
                        returnValue(new AppConfigurationEntry[]{entry}),
                        // challenge second
                        returnValue(new AppConfigurationEntry[]{entry2}),
                        // challenge second failed
                        returnValue(new AppConfigurationEntry[]{entry2})));
            }
        });
        httpAcceptor.bind(httpAddress, acceptHandler, null);
        k3po.finish();
    }

    @Test
    @Specification({"response.with.secure.challenge.identity/client"})
    public void clientShouldAttachSecChallengeIdentityToFollowingRequests() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(configuration).getAppConfigurationEntry(FIRST_REALM);
                final String loginModuleName = "org.kaazing.gateway.transport.http.multi.auth.FirstFactorLoginModule";
                final HashMap<String, Object> options = new HashMap<>();
                final AppConfigurationEntry entry = new AppConfigurationEntry(loginModuleName, REQUIRED, options);
                final String loginModuleName2 = "org.kaazing.gateway.transport.http.multi.auth.SecondFactorLoginModule";
                final AppConfigurationEntry entry2 = new AppConfigurationEntry(loginModuleName2, REQUIRED, options);
                will(onConsecutiveCalls(
                        // challenge first
                        returnValue(new AppConfigurationEntry[]{entry}),
                        // challenge first passed
                        returnValue(new AppConfigurationEntry[]{entry}),
                        // challenge second
                        returnValue(new AppConfigurationEntry[]{entry2})));
            }
        });
        httpAcceptor.bind(httpAddress, acceptHandler, null);
        k3po.finish();
    }

}
