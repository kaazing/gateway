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
package org.kaazing.gateway.security.auth.context;

import static javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.InetAddress;
import java.util.HashMap;

import javax.security.auth.Subject;
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
import org.kaazing.gateway.security.auth.AuthenticationTokenCallbackHandler;
import org.kaazing.gateway.security.auth.InetAddressCallbackHandler;
import org.kaazing.gateway.security.auth.SimpleInetAddressTestLoginModule;
import org.kaazing.gateway.security.auth.token.DefaultAuthenticationToken;
import org.kaazing.gateway.server.spi.security.AuthenticationToken;
import org.kaazing.gateway.server.spi.security.AuthenticationTokenCallback;
import org.kaazing.gateway.server.spi.security.InetAddressCallback;

public class DefaultLoginContextFactoryTest {
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
    public void testCreateLoginContext() throws Exception {
        context.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);
            }
        });
        LoginContext loginContext = factory.createLoginContext(new TypedCallbackHandlerMap());
        context.assertIsSatisfied();
        assertNotNull(loginContext);
        Subject subject = loginContext.getSubject();
        assertNull(subject);
    }

    @Test
    public void testCreateLoginContextNullAuthToken() throws Exception {
        context.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);
            }
        });
        LoginContext loginContext = factory.createLoginContext(null, null);
        context.assertIsSatisfied();
        assertNotNull(loginContext);
        Subject subject = loginContext.getSubject();
        assertNull(subject);
    }

    @Test(expected = LoginException.class)
    public void testCreateAndLoginToLoginContextWithNoLoginModulesDefined() throws Exception {
        context.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);
            }
        });
        LoginContext loginContext = factory.createLoginContext(null);
        context.assertIsSatisfied();
        assertNotNull(loginContext);
        loginContext.login();
    }

    @Test
    public void testBasicAuthCustomLoginModuleLoginSuccess() throws Exception {
        AuthenticationToken s = new DefaultAuthenticationToken("joe:welcome");
        context.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);
                final String loginModuleName = "org.kaazing.gateway.security.auth.SimpleTestLoginModule";
                final HashMap<String, Object> options = new HashMap<>();
                final AppConfigurationEntry entry = new AppConfigurationEntry(loginModuleName,
                        REQUIRED, options);
                will(returnValue(new AppConfigurationEntry[]{entry}));
            }
        });
        final TypedCallbackHandlerMap additionalCallbacks = new TypedCallbackHandlerMap();
        additionalCallbacks.put(AuthenticationTokenCallback.class, new AuthenticationTokenCallbackHandler(s));
        LoginContext loginContext = factory.createLoginContext(additionalCallbacks);
        context.assertIsSatisfied();
        assertNotNull(loginContext);
        loginContext.login();
    }

    @Test
    public void testInetAddressFilterCustomLoginModuleLoginSuccess() throws Exception {
        InetAddress address = InetAddress.getByName("localhost");
        context.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);
                final String loginModuleName = SimpleInetAddressTestLoginModule.class.getName();
                final HashMap<String, Object> options = new HashMap<>();
                final AppConfigurationEntry entry = new AppConfigurationEntry(loginModuleName,
                        REQUIRED, options);
                will(returnValue(new AppConfigurationEntry[]{entry}));
            }
        });
        final TypedCallbackHandlerMap additionalCallbacks = new TypedCallbackHandlerMap();
        additionalCallbacks.put(InetAddressCallback.class, new InetAddressCallbackHandler(address));
        LoginContext loginContext = factory.createLoginContext(additionalCallbacks);
        context.assertIsSatisfied();
        assertNotNull(loginContext);
        loginContext.login();
    }
}

