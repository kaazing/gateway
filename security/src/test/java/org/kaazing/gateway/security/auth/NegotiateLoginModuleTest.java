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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.HashMap;

import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.mina.util.Base64;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.security.auth.context.DefaultLoginContextFactory;
import org.kaazing.gateway.security.auth.context.ResultAwareLoginContext;
import org.kaazing.gateway.security.auth.token.DefaultAuthenticationToken;
import org.kaazing.gateway.server.spi.security.AuthenticationToken;
import org.kaazing.gateway.server.spi.security.AuthenticationTokenCallback;
import org.kaazing.gateway.server.spi.security.LoginResultCallback;

public class NegotiateLoginModuleTest {
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
    public void testNegotiateModulePassThroughOnUnrecognizedScheme() throws Exception {
        AuthenticationToken s = new DefaultAuthenticationToken("Basic", "Basic "+new String(Base64.encodeBase64("joe:welcome".getBytes())));
        context.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);
                final String loginModuleName = "org.kaazing.gateway.security.auth.NegotiateLoginModule";
                final HashMap<String, Object> options = new HashMap<>();
                final AppConfigurationEntry entry = new AppConfigurationEntry(loginModuleName,
                        REQUIRED, options);
                will(returnValue(new AppConfigurationEntry[]{entry}));
            }
        });

        Subject subject = new Subject();
        DispatchCallbackHandler handler = new DispatchCallbackHandler();
        DefaultLoginResult loginResult = new DefaultLoginResult();
        handler.register(AuthenticationTokenCallback.class, new AuthenticationTokenCallbackHandler(s));
        handler.register(LoginResultCallback.class, new LoginResultCallbackHandler(loginResult));
        LoginContext loginContext = new ResultAwareLoginContext("demo", subject, handler, configuration, loginResult);

        context.assertIsSatisfied();
        assertNotNull(loginContext);
        try {
            loginContext.login();
        } catch (LoginException e) {
            fail("Login failed to succeed as expected: "+e.getMessage());
        }
    }


}

