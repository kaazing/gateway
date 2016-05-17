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

import static javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL;
import static javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.HashMap;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.mina.util.Base64;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.security.auth.context.DefaultLoginContextFactory;
import org.kaazing.gateway.security.auth.context.ResultAwareLoginContext;
import org.kaazing.gateway.security.auth.token.DefaultAuthenticationToken;
import org.kaazing.gateway.server.spi.security.AuthenticationToken;
import org.kaazing.gateway.server.spi.security.AuthenticationTokenCallback;
import org.kaazing.gateway.server.spi.security.LoginResultCallback;

public class FileLoginModuleTest {
    private static final String GOOD_USERNAME = "joe";
    private static final String GOOD_PASSWORD = "welcome";

    private static final String BAD_USERNAME = "fred";
    private static final String BAD_PASSWORD = "smith";

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
    public void shouldLoginFileLoginModule()
        throws Exception {

        AuthenticationToken s = new DefaultAuthenticationToken("Basic", "Basic "+new String(Base64.encodeBase64(String.format("%s:%s", GOOD_USERNAME, GOOD_PASSWORD).getBytes())));

        context.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);

                final String basicLoginModuleName = "org.kaazing.gateway.security.auth.BasicLoginModule";
                final HashMap<String, Object> basicOptions = new HashMap<>();
                final AppConfigurationEntry basicEntry = new AppConfigurationEntry(basicLoginModuleName, OPTIONAL, basicOptions);

                final String fileLoginModuleName = "org.kaazing.gateway.security.auth.FileLoginModule";
                final HashMap<String, Object> fileOptions = new HashMap<>();
                fileOptions.put("file", "src/test/resources/jaas-config.xml");

                final AppConfigurationEntry fileEntry = new AppConfigurationEntry(fileLoginModuleName, REQUIRED, fileOptions);

                will(returnValue(new AppConfigurationEntry[]{ basicEntry, fileEntry}));
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
            final CallbackHandler nameCallbackHandler = handler.getDispatchMap().get(NameCallback.class);
            final CallbackHandler passwordCallbackHandler = handler.getDispatchMap().get(PasswordCallback.class);
            Assert.assertNotNull(nameCallbackHandler);
            Assert.assertNotNull(passwordCallbackHandler);

            NameCallback nameCallback = new NameCallback(">|<");
            PasswordCallback passwordCallback = new PasswordCallback(">|<", false);

            nameCallbackHandler.handle(new Callback[]{nameCallback});
            passwordCallbackHandler.handle(new Callback[]{passwordCallback});

            Assert.assertEquals("Expected 'joe' as the name", "joe", nameCallback.getName());
            Assert.assertEquals("Expected 'welcome' as the password", "welcome", new String(passwordCallback.getPassword()));

        } catch (LoginException e) {
            fail("Login failed to succeed as expected: "+e.getMessage());
        }
    }

    @Test(expected = LoginException.class)
    public void shouldFailFileLoginModuleMissingFile()
        throws Exception {

        AuthenticationToken s = new DefaultAuthenticationToken("Basic", "Basic "+new String(Base64.encodeBase64(String.format("%s:%s", GOOD_USERNAME, GOOD_PASSWORD).getBytes())));

        context.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);

                final String basicLoginModuleName = "org.kaazing.gateway.security.auth.BasicLoginModule";
                final HashMap<String, Object> basicOptions = new HashMap<>();
                final AppConfigurationEntry basicEntry = new AppConfigurationEntry(basicLoginModuleName, OPTIONAL, basicOptions);

                final String fileLoginModuleName = "org.kaazing.gateway.security.auth.FileLoginModule";
                final HashMap<String, Object> fileOptions = new HashMap<>();
                fileOptions.put("file", "src/test/jaas-config.xml");

                final AppConfigurationEntry fileEntry = new AppConfigurationEntry(fileLoginModuleName, REQUIRED, fileOptions);

                will(returnValue(new AppConfigurationEntry[]{ basicEntry, fileEntry}));
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

        loginContext.login();
    }

    @Test(expected = FailedLoginException.class)
    public void shouldFailFileLoginModuleBadUsername()
        throws Exception {

        AuthenticationToken s = new DefaultAuthenticationToken("Basic", "Basic "+new String(Base64.encodeBase64(String.format("%s:%s", BAD_USERNAME, GOOD_PASSWORD).getBytes())));

        context.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);

                final String basicLoginModuleName = "org.kaazing.gateway.security.auth.BasicLoginModule";
                final HashMap<String, Object> basicOptions = new HashMap<>();
                final AppConfigurationEntry basicEntry = new AppConfigurationEntry(basicLoginModuleName, OPTIONAL, basicOptions);

                final String fileLoginModuleName = "org.kaazing.gateway.security.auth.FileLoginModule";
                final HashMap<String, Object> fileOptions = new HashMap<>();
                fileOptions.put("file", "src/test/resources/jaas-config.xml");

                final AppConfigurationEntry fileEntry = new AppConfigurationEntry(fileLoginModuleName, REQUIRED, fileOptions);

                will(returnValue(new AppConfigurationEntry[]{ basicEntry, fileEntry}));
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

        loginContext.login();
    }

    @Test(expected = FailedLoginException.class)
    public void shouldFailFileLoginModuleBadPassword()
        throws Exception {

        AuthenticationToken s = new DefaultAuthenticationToken("Basic", "Basic "+new String(Base64.encodeBase64(String.format("%s:%s", GOOD_USERNAME, BAD_PASSWORD).getBytes())));

        context.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);

                final String basicLoginModuleName = "org.kaazing.gateway.security.auth.BasicLoginModule";
                final HashMap<String, Object> basicOptions = new HashMap<>();
                final AppConfigurationEntry basicEntry = new AppConfigurationEntry(basicLoginModuleName, OPTIONAL, basicOptions);

                final String fileLoginModuleName = "org.kaazing.gateway.security.auth.FileLoginModule";
                final HashMap<String, Object> fileOptions = new HashMap<>();
                fileOptions.put("file", "src/test/resources/jaas-config.xml");

                final AppConfigurationEntry fileEntry = new AppConfigurationEntry(fileLoginModuleName, REQUIRED, fileOptions);

                will(returnValue(new AppConfigurationEntry[]{ basicEntry, fileEntry}));
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

        loginContext.login();
    }
}
