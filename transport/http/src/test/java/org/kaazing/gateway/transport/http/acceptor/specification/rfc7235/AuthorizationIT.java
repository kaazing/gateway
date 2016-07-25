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
package org.kaazing.gateway.transport.http.acceptor.specification.rfc7235;

import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;

import org.apache.mina.core.service.IoHandler;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.security.LoginContextFactory;
import org.kaazing.gateway.security.TypedCallbackHandlerMap;
import org.kaazing.gateway.security.auth.AuthenticationTokenCallbackHandler;
import org.kaazing.gateway.security.auth.context.DefaultLoginContextFactory;
import org.kaazing.gateway.server.spi.security.AuthenticationTokenCallback;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpAcceptorRule;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MethodExecutionTrace;

public class AuthorizationIT {

    static final String REALM_NAME = "Kaazing Gateway Demo";

    private final HttpAcceptorRule acceptor = new HttpAcceptorRule();
    Mockery context;

    private JUnitRuleMockery context1 = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };
    
    private void initMockContext() {
        context = new Mockery() {
            {
                setThreadingPolicy(new Synchroniser());
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
    }

    private TestRule contextRule = ITUtil.toTestRule(context1);
    private final TestRule trace = new MethodExecutionTrace();
    private final K3poRule robot = new K3poRule().setScriptRoot("org/kaazing/specification/http/rfc7235");
    private final TestRule timeoutRule = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(acceptor).around(contextRule).around(robot).around(timeoutRule);

    @Ignore
    @Test
    @Specification("framework/invalid.then.valid.credentials/request")
    public void authorizedInvalidThenValidCredentials() throws Exception {
        authorizationStart();
    }

    @Ignore
    @Test
    @Specification("framework/missing.then.valid.credentials/request")
    public void authorizedMissingThenValidCredentials() throws Exception {
        authorizationStart();
    }

    @Ignore
    @Test
    @Specification("status/valid.credentials/request")
    public void authorizedValidCredentials() throws Exception {
        authorizationStart();
    }

    /*
     * Not Applicable for gateway authorization
     * 
     * @Specification("framework/forbidden/request")
     * 
     * @Test public void forbiddenTest() throws Exception { robot.finish(); }
     */

    @Ignore
    @Test
    @Specification("framework/partial.then.valid.credentials/request")
    public void unauthorizedInvalidUsernameValidPassword() throws Exception {
        authorizationStart();
    }

    @Ignore
    @Test
    @Specification("status/multiple.requests.with.invalid.credentials/request")
    public void unauthorizedMultipleInvalidRequests() throws Exception {
        authorizationStart();
    }

    @Ignore
    @Test
    @Specification("headers/invalid.user/request")
    public void unauthorizedUnknownUser() throws Exception {
        authorizationStart();
    }
    
    private void authorizationStart() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        acceptor.getAcceptOptions().put("http.realmName", REALM_NAME);
        acceptor.getAcceptOptions().put("http.requiredRoles", new String[] { "AUTHORIZED" });
        acceptor.getAcceptOptions().put("http.realmAuthorizationMode", "challenge");
        acceptor.getAcceptOptions().put("http.realmChallengeScheme", "Basic");
        acceptor.getAcceptOptions().put("http.realmDescription", REALM_NAME);
        initMockContext();
        Configuration configuration = context.mock(Configuration.class);
        LoginContextFactory factory = new DefaultLoginContextFactory(REALM_NAME, configuration);

        context.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);
                final String loginModuleName = "org.kaazing.gateway.transport.http.acceptor.specification.rfc7235.SimpleTestLoginModule";
                final HashMap<String, Object> options = new HashMap<>();
                final AppConfigurationEntry entry = new AppConfigurationEntry(loginModuleName,
                        REQUIRED, options);
                will(returnValue(new AppConfigurationEntry[]{entry}));
            }
        });

        acceptor.getAcceptOptions().put("http.loginContextFactory", factory);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {
            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
            }
        };
        acceptor.bind("http://localhost:8000/resource", acceptHandler);
        
        robot.finish();
        assertTrue(latch.await(4, SECONDS));
    }

}
