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
package org.kaazing.gateway.security.auth;

import static javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.security.TypedCallbackHandlerMap;
import org.kaazing.gateway.security.auth.context.DefaultLoginContextFactory;
import org.kaazing.gateway.security.auth.context.ResultAwareLoginContext;
import org.kaazing.gateway.security.auth.token.DefaultAuthenticationToken;
import org.kaazing.gateway.server.spi.security.AuthenticationToken;
import org.kaazing.gateway.server.spi.security.AuthenticationTokenCallback;

public class TimeoutLoginModuleTest {
    DefaultLoginContextFactory factory;
    Mockery context;
    Configuration configuration;
    public static final String REALM_NAME = "demo";


    
    @Before
    public void setUp() throws Exception {
        initMockContext();
        configuration = context.mock(Configuration.class);
        factory = new DefaultLoginContextFactory(REALM_NAME, configuration);
    }

    private void initMockContext() {
        context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
    }

    @Test
    public void testTimeoutLoginModuleMissingLifetimeSpecification() throws Exception {
        AuthenticationToken s = new DefaultAuthenticationToken("joe:welcome");
        context.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);
                final String loginModuleName = "org.kaazing.gateway.security.auth.TimeoutLoginModule";
                final HashMap<String, Object> options = new HashMap<>();
                final AppConfigurationEntry entry = new AppConfigurationEntry(loginModuleName,
                        REQUIRED, options);
                will(returnValue(new AppConfigurationEntry[]{entry}));
            }
        });
        LoginContext loginContext = factory.createLoginContext(makeTokenCallbackMapWithToken(s));
        context.assertIsSatisfied();
        assertNotNull(loginContext);
        try {
            loginContext.login();
            fail("Expected configuration exception at initialization: no timeouts were specified.");
        } catch (LoginException e) {
            final String msg = "You must specify session-timeout option.";
            System.out.println(e.getMessage());
            assertTrue(e.getMessage().contains(msg));
        }
    }


    @Test
    public void testTimeoutLoginModuleBadMaximumLifetimeOptionSyntax() throws Exception {
        AuthenticationToken s = new DefaultAuthenticationToken("joe:welcome");
        context.checking(new Expectations() {
            {
                final String loginModuleName = "org.kaazing.gateway.security.auth.TimeoutLoginModule";
                final HashMap<String, Object> options = new HashMap<>();
                options.put("session-timeout", "-1");
                final AppConfigurationEntry entry = new AppConfigurationEntry(loginModuleName,
                        REQUIRED, options);

                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);
                will(returnValue(new AppConfigurationEntry[]{entry}));
            }
        });
        LoginContext loginContext = factory.createLoginContext(makeTokenCallbackMapWithToken(s));
        context.assertIsSatisfied();
        assertNotNull(loginContext);
        try {
            loginContext.login();
            fail("Expected configuration exception at initialization: bad syntax for session-timeout.");
        } catch (LoginException e) {
            final String msg = "java.lang.NumberFormatException";
            assertTrue(e.getMessage().contains(msg));
        }
    }

    @Test
    public void testTimeoutLoginModuleLoginFailureCleansState() throws Exception {
        AuthenticationToken s = new DefaultAuthenticationToken("joe:welcome");
        context.checking(new Expectations() {
            {
                final String loginModuleName = "org.kaazing.gateway.security.auth.TimeoutLoginModule";
                final HashMap<String, Object> options = new HashMap<>();
                options.put("session-timeout", "30 minutes");
                options.put("force-failure", "true");
                final AppConfigurationEntry entry = new AppConfigurationEntry(loginModuleName,
                        REQUIRED, options);

                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);
                will(returnValue(new AppConfigurationEntry[]{entry}));
            }
        });
        ResultAwareLoginContext loginContext = (ResultAwareLoginContext) factory.createLoginContext(makeTokenCallbackMapWithToken(s));
        context.assertIsSatisfied();
        assertNotNull(loginContext);
        try {
            loginContext.login();
            fail("Expected a forced failure to clean up state - did not occur.");
        } catch (LoginException e) {
            // Make sure we reset everything to 0 and make the login result a failure
            DefaultLoginResult loginResult = loginContext.getLoginResult();
            assertEquals(null, loginResult.getSessionTimeout());
        }
    }

    @Test
    public void testTimeoutLoginModuleLoginSuccessAndEffectWithOnlyMaximumLifetimeSpecified() throws Exception {
        AuthenticationToken s = new DefaultAuthenticationToken("joe:welcome");
        context.checking(new Expectations() {
            {
                final String loginModuleName = "org.kaazing.gateway.security.auth.TimeoutLoginModule";
                final HashMap<String, Object> options = new HashMap<>();
                options.put("session-timeout", "10 minutes");
                final AppConfigurationEntry entry = new AppConfigurationEntry(loginModuleName,
                        REQUIRED, options);

                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);
                will(returnValue(new AppConfigurationEntry[]{entry}));
            }
        });
        ResultAwareLoginContext loginContext = (ResultAwareLoginContext) factory.createLoginContext(makeTokenCallbackMapWithToken(s));
        context.assertIsSatisfied();
        assertNotNull(loginContext);
        loginContext.login();
        DefaultLoginResult loginResult = loginContext.getLoginResult();
        assertEquals(Long.valueOf(600L), loginResult.getSessionTimeout());
    }


    private TypedCallbackHandlerMap makeTokenCallbackMapWithToken(AuthenticationToken s) {
        TypedCallbackHandlerMap map = new TypedCallbackHandlerMap();
        map.put(AuthenticationTokenCallback.class, new AuthenticationTokenCallbackHandler(s));
        return map;
    }
}

