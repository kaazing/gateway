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
import static org.hamcrest.core.AllOf.allOf;
import static org.kaazing.gateway.resource.address.ResourceAddressFactory.newResourceAddressFactory;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.INJECTABLE_HEADERS;
import static org.kaazing.gateway.transport.http.HttpMatchers.hasMethod;
import static org.kaazing.gateway.transport.http.HttpMatchers.hasReadHeader;
import static org.kaazing.gateway.transport.http.HttpMethod.GET;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.spi.LoginModule;

import org.apache.mina.core.service.IoHandler;
import org.jmock.Expectations;
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
import org.kaazing.gateway.resource.address.http.HttpOriginSecurity;
import org.kaazing.gateway.resource.address.http.HttpRealmInfo;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.security.LoginContextFactory;
import org.kaazing.gateway.security.TypedCallbackHandlerMap;
import org.kaazing.gateway.security.auth.context.DefaultLoginContextFactory;
import org.kaazing.gateway.security.auth.context.ResultAwareLoginContext;
import org.kaazing.gateway.server.context.resolve.DefaultCrossSiteConstraintContext;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.http.HttpAcceptor;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class AcceptorMultiFactorAuthIT {
    private HttpAcceptor httpAcceptor;
    private ResourceAddress httpAddress;
    String firstFactorRealmName = "firstFactorRealm";
    String secondFactorRealmName = "secondFactorRealm";

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
    private LoginContextFactory loginContextFactory;

    @Rule
    public RuleChain chainRule = RuleChain.outerRule(k3po).around(timeoutRule);
    private Configuration configuration;

    @Before
    public void setupAcceptor() {
        ResourceAddressFactory addressFactory = newResourceAddressFactory();
        Map<String, ?> config = Collections.emptyMap();
        TransportFactory transportFactory = TransportFactory.newTransportFactory(config);
        BridgeServiceFactory serviceFactory = new BridgeServiceFactory(transportFactory);

        HttpAcceptor httpAcceptor = (HttpAcceptor) transportFactory.getTransport("http").getAcceptor();
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

//         Configuration configuration = new AppConfigurationEntry(loginModuleName, controlFlag, options)
        // LoginContextFactory loginContext1 = LoginContextFactories.create(firstFactorRealmName, configuration);

        configuration = mockery.mock(Configuration.class);
        realms[0] = new HttpRealmInfo(firstFactorRealmName, "Basic", "firstFactor", new String[0], new String[0], new String[0],
                new DefaultLoginContextFactory(firstFactorRealmName, configuration), Collections.emptySet());
        realms[1] = new HttpRealmInfo(secondFactorRealmName, "Basic", "secondFactor", new String[0], new String[0],
                new String[0], new DefaultLoginContextFactory(firstFactorRealmName, configuration), Collections.emptySet());
        options.setOption(HttpResourceAddress.REALMS, realms);

        options.setOption(HttpResourceAddress.REQUIRED_ROLES, new String[]{"AUTHORIZED"});
        ResourceAddress httpAddress = addressFactory.newResourceAddress(location, options);

        this.tcpAcceptor = tcpAcceptor;
        this.httpAcceptor = httpAcceptor;

        this.httpAddress = httpAddress;
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
    @Specification({"request.with.secure.challenge.identity/client",})
    public void serverMayGiveSecChallengeIdentityHeaderWith401() throws Exception {
        final IoHandler handler = mockery.mock(IoHandler.class);
        final ResultAwareLoginContext resultAwareLoginContext = mockery.mock(ResultAwareLoginContext.class);
        mockery.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(firstFactorRealmName); will(returnValue(loginModule));
                final String loginModuleName = "org.kaazing.gateway.security.auth.SimpleTestLoginModule";
                final HashMap<String, Object> options = new HashMap<>();
                final AppConfigurationEntry entry = new AppConfigurationEntry(loginModuleName,
                        REQUIRED, options);
                will(returnValue(new AppConfigurationEntry[]{entry}));
            }
        });
        httpAcceptor.bind(httpAddress, handler, null);
        k3po.finish();
    }

    @Test
    @Specification({"request.missing.secure.challenge.identity/client",})
    public void serverShouldChallengeFirstFactorWhenSecChallengeIdentityHeaderMissing() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({"response.with.secure.challenge.identity/client",})
    public void clientShouldAttachSecChallengeIdentityToFollowingRequests() throws Exception {
        k3po.finish();
    }

    private static ResourceAddress httpAddress() {
        Map<String, DefaultCrossSiteConstraintContext> constraints = new HashMap<>();
        DefaultCrossSiteConstraintContext constraintContext =
                new DefaultCrossSiteConstraintContext("http://source.example.com:80", "GET,POST", null, null);
        constraints.put(constraintContext.getAllowOrigin(), constraintContext);
        HttpOriginSecurity httpOriginSecuirty = new HttpOriginSecurity(constraints);

        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        ResourceOptions options = ResourceOptions.FACTORY.newResourceOptions();
        options.setOption(HttpResourceAddress.ORIGIN_SECURITY, httpOriginSecuirty);
        return addressFactory.newResourceAddress("http://localhost:8000/path", options);
    }
}
