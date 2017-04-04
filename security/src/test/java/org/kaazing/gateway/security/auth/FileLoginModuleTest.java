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

import java.nio.file.Paths;
import java.util.HashMap;

import javax.security.auth.Subject;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.mina.util.Base64;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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

    @Rule
    public ExpectedException thrown = ExpectedException.none();

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
    public void shouldLoginUsingCallbacks() throws Exception {

        AuthenticationToken s = new DefaultAuthenticationToken("Basic",
                "Basic " + new String(Base64.encodeBase64(String.format("%s:%s", GOOD_USERNAME, GOOD_PASSWORD).getBytes())));

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

                will(returnValue(new AppConfigurationEntry[]{basicEntry, fileEntry}));
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
            fail("Login failed to succeed as expected: " + e.getMessage());
        }
    }

    @Test
    public void shouldLoginUsingSharedState() throws Exception {

        AuthenticationToken s = new DefaultAuthenticationToken("Basic",
                "Basic " + new String(Base64.encodeBase64(String.format("%s:%s", GOOD_USERNAME, GOOD_PASSWORD).getBytes())));

        context.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);

                final String basicLoginModuleName = "org.kaazing.gateway.security.auth.BasicLoginModule";
                final HashMap<String, Object> basicOptions = new HashMap<>();
                final AppConfigurationEntry basicEntry = new AppConfigurationEntry(basicLoginModuleName, OPTIONAL, basicOptions);

                final String fileLoginModuleName = "org.kaazing.gateway.security.auth.FileLoginModule";
                final HashMap<String, Object> fileOptions = new HashMap<>();
                fileOptions.put("file", "src/test/resources/jaas-config.xml");
                fileOptions.put("tryFirstPass", "true");

                final AppConfigurationEntry fileEntry = new AppConfigurationEntry(fileLoginModuleName, REQUIRED, fileOptions);

                will(returnValue(new AppConfigurationEntry[]{basicEntry, fileEntry}));
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
            fail("Login failed to succeed as expected: " + e.getMessage());
        }
    }

    @Test
    public void shouldFailNoFileOptionProvided() throws Exception {

        context.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);

                final String fileLoginModuleName = "org.kaazing.gateway.security.auth.FileLoginModule";
                final HashMap<String, Object> fileOptions = new HashMap<>();

                final AppConfigurationEntry fileEntry = new AppConfigurationEntry(fileLoginModuleName, REQUIRED, fileOptions);

                will(returnValue(new AppConfigurationEntry[]{fileEntry}));
            }
        });

        Subject subject = new Subject();
        DispatchCallbackHandler handler = new DispatchCallbackHandler();
        DefaultLoginResult loginResult = new DefaultLoginResult();
        handler.register(LoginResultCallback.class, new LoginResultCallbackHandler(loginResult));
        LoginContext loginContext = new ResultAwareLoginContext("demo", subject, handler, configuration, loginResult);

        context.assertIsSatisfied();
        assertNotNull(loginContext);

        thrown.expect(LoginException.class);
        loginContext.login();
    }

    @Test
    public void shouldFailFileLoginModuleMissingFile() throws Exception {

        AuthenticationToken s = new DefaultAuthenticationToken("Basic",
                "Basic " + new String(Base64.encodeBase64(String.format("%s:%s", GOOD_USERNAME, GOOD_PASSWORD).getBytes())));

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

                will(returnValue(new AppConfigurationEntry[]{basicEntry, fileEntry}));
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

        thrown.expect(LoginException.class);
        loginContext.login();
    }

    @Test
    public void shouldFailWhenConfiguredFileIsFolder() throws Exception {

        AuthenticationToken s = new DefaultAuthenticationToken("Basic",
                "Basic " + new String(Base64.encodeBase64(String.format("%s:%s", GOOD_USERNAME, GOOD_PASSWORD).getBytes())));

        context.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);

                final String basicLoginModuleName = "org.kaazing.gateway.security.auth.BasicLoginModule";
                final HashMap<String, Object> basicOptions = new HashMap<>();
                final AppConfigurationEntry basicEntry = new AppConfigurationEntry(basicLoginModuleName, OPTIONAL, basicOptions);

                final String fileLoginModuleName = "org.kaazing.gateway.security.auth.FileLoginModule";
                final HashMap<String, Object> fileOptions = new HashMap<>();
                fileOptions.put("file", "src/test/");

                final AppConfigurationEntry fileEntry = new AppConfigurationEntry(fileLoginModuleName, REQUIRED, fileOptions);

                will(returnValue(new AppConfigurationEntry[]{basicEntry, fileEntry}));
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

        thrown.expect(LoginException.class);
        loginContext.login();
    }

    @Test
    public void shouldLoginUsingAbsoluteFilePath() throws Exception {

        AuthenticationToken s = new DefaultAuthenticationToken("Basic",
                "Basic " + new String(Base64.encodeBase64(String.format("%s:%s", GOOD_USERNAME, GOOD_PASSWORD).getBytes())));

        context.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);

                final String basicLoginModuleName = "org.kaazing.gateway.security.auth.BasicLoginModule";
                final HashMap<String, Object> basicOptions = new HashMap<>();
                final AppConfigurationEntry basicEntry = new AppConfigurationEntry(basicLoginModuleName, OPTIONAL, basicOptions);

                final String fileLoginModuleName = "org.kaazing.gateway.security.auth.FileLoginModule";
                final HashMap<String, Object> fileOptions = new HashMap<>();
                fileOptions.put("file", Paths.get("src/test/resources/jaas-config.xml").toAbsolutePath().toString());

                final AppConfigurationEntry fileEntry = new AppConfigurationEntry(fileLoginModuleName, REQUIRED, fileOptions);

                will(returnValue(new AppConfigurationEntry[]{basicEntry, fileEntry}));
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
            fail("Login failed to succeed as expected: " + e.getMessage());
        }
    }

    @Test
    public void shouldLoginUsingFileRelativeToGatewayConfigDirectory() throws Exception {

        AuthenticationToken s = new DefaultAuthenticationToken("Basic",
                "Basic " + new String(Base64.encodeBase64(String.format("%s:%s", GOOD_USERNAME, GOOD_PASSWORD).getBytes())));

        context.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);

                final String basicLoginModuleName = "org.kaazing.gateway.security.auth.BasicLoginModule";
                final HashMap<String, Object> basicOptions = new HashMap<>();
                final AppConfigurationEntry basicEntry = new AppConfigurationEntry(basicLoginModuleName, OPTIONAL, basicOptions);

                final String fileLoginModuleName = "org.kaazing.gateway.security.auth.FileLoginModule";
                final HashMap<String, Object> fileOptions = new HashMap<>();
                fileOptions.put("GATEWAY_CONFIG_DIRECTORY", Paths.get("src/test/resources").toAbsolutePath().toString());
                fileOptions.put("file", "jaas-config.xml");

                final AppConfigurationEntry fileEntry = new AppConfigurationEntry(fileLoginModuleName, REQUIRED, fileOptions);

                will(returnValue(new AppConfigurationEntry[]{basicEntry, fileEntry}));
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
            fail("Login failed to succeed as expected: " + e.getMessage());
        }
    }

    @Test
    public void shouldFailFileLoginModuleBadUsernameFromCallback() throws Exception {

        AuthenticationToken s = new DefaultAuthenticationToken("Basic",
                "Basic " + new String(Base64.encodeBase64(String.format("%s:%s", BAD_USERNAME, GOOD_PASSWORD).getBytes())));

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

                will(returnValue(new AppConfigurationEntry[]{basicEntry, fileEntry}));
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

        thrown.expect(LoginException.class);
        loginContext.login();
    }

    @Test
    public void shouldFailFileLoginModuleBadUsernameFromSharedState() throws Exception {

        AuthenticationToken s = new DefaultAuthenticationToken("Basic",
                "Basic " + new String(Base64.encodeBase64(String.format("%s:%s", BAD_USERNAME, GOOD_PASSWORD).getBytes())));

        context.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);

                final String basicLoginModuleName = "org.kaazing.gateway.security.auth.BasicLoginModule";
                final HashMap<String, Object> basicOptions = new HashMap<>();
                final AppConfigurationEntry basicEntry = new AppConfigurationEntry(basicLoginModuleName, OPTIONAL, basicOptions);

                final String fileLoginModuleName = "org.kaazing.gateway.security.auth.FileLoginModule";
                final HashMap<String, Object> fileOptions = new HashMap<>();
                fileOptions.put("file", "src/test/resources/jaas-config.xml");
                fileOptions.put("tryFirstPass", "true");

                final AppConfigurationEntry fileEntry = new AppConfigurationEntry(fileLoginModuleName, REQUIRED, fileOptions);

                will(returnValue(new AppConfigurationEntry[]{basicEntry, fileEntry}));
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

        thrown.expect(LoginException.class);
        loginContext.login();
    }

    @Test
    public void shouldFailFileLoginModuleBadPasswordFromCallback() throws Exception {

        AuthenticationToken s = new DefaultAuthenticationToken("Basic",
                "Basic " + new String(Base64.encodeBase64(String.format("%s:%s", GOOD_USERNAME, BAD_PASSWORD).getBytes())));

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

                will(returnValue(new AppConfigurationEntry[]{basicEntry, fileEntry}));
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

        thrown.expect(LoginException.class);
        loginContext.login();
    }

    @Test
    public void shouldFailFileLoginModuleBadPasswordFromSharedState() throws Exception {

        AuthenticationToken s = new DefaultAuthenticationToken("Basic",
                "Basic " + new String(Base64.encodeBase64(String.format("%s:%s", GOOD_USERNAME, BAD_PASSWORD).getBytes())));

        context.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);

                final String basicLoginModuleName = "org.kaazing.gateway.security.auth.BasicLoginModule";
                final HashMap<String, Object> basicOptions = new HashMap<>();
                final AppConfigurationEntry basicEntry = new AppConfigurationEntry(basicLoginModuleName, OPTIONAL, basicOptions);

                final String fileLoginModuleName = "org.kaazing.gateway.security.auth.FileLoginModule";
                final HashMap<String, Object> fileOptions = new HashMap<>();
                fileOptions.put("file", "src/test/resources/jaas-config.xml");
                fileOptions.put("tryFirstPass", "true");

                final AppConfigurationEntry fileEntry = new AppConfigurationEntry(fileLoginModuleName, REQUIRED, fileOptions);

                will(returnValue(new AppConfigurationEntry[]{basicEntry, fileEntry}));
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

        thrown.expect(LoginException.class);
        loginContext.login();
    }

    @Test
    public void shouldFailFileLoginModuleNoUsernameCallback() throws Exception {

        context.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);

                final String fileLoginModuleName = "org.kaazing.gateway.security.auth.FileLoginModule";
                final HashMap<String, Object> fileOptions = new HashMap<>();
                fileOptions.put("file", "src/test/resources/jaas-config.xml");

                final AppConfigurationEntry fileEntry = new AppConfigurationEntry(fileLoginModuleName, REQUIRED, fileOptions);

                will(returnValue(new AppConfigurationEntry[]{fileEntry}));
            }
        });

        Subject subject = new Subject();
        DispatchCallbackHandler handler = new DispatchCallbackHandler();
        DefaultLoginResult loginResult = new DefaultLoginResult();
        handler.register(LoginResultCallback.class, new LoginResultCallbackHandler(loginResult));
        handler.register(PasswordCallback.class, new PasswordCallbackHandler(GOOD_PASSWORD.toCharArray()));
        LoginContext loginContext = new ResultAwareLoginContext("demo", subject, handler, configuration, loginResult);

        context.assertIsSatisfied();
        assertNotNull(loginContext);

        thrown.expect(LoginException.class);
        loginContext.login();
    }

    @Test
    public void shouldFailFileLoginModuleNoPasswordCallback() throws Exception {

        context.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);

                final String fileLoginModuleName = "org.kaazing.gateway.security.auth.FileLoginModule";
                final HashMap<String, Object> fileOptions = new HashMap<>();
                fileOptions.put("file", "src/test/resources/jaas-config.xml");

                final AppConfigurationEntry fileEntry = new AppConfigurationEntry(fileLoginModuleName, REQUIRED, fileOptions);

                will(returnValue(new AppConfigurationEntry[]{fileEntry}));
            }
        });

        Subject subject = new Subject();
        DispatchCallbackHandler handler = new DispatchCallbackHandler();
        DefaultLoginResult loginResult = new DefaultLoginResult();
        handler.register(LoginResultCallback.class, new LoginResultCallbackHandler(loginResult));
        handler.register(NameCallback.class, new NameCallbackHandler(GOOD_USERNAME));
        LoginContext loginContext = new ResultAwareLoginContext("demo", subject, handler, configuration, loginResult);

        context.assertIsSatisfied();
        assertNotNull(loginContext);

        thrown.expect(LoginException.class);
        loginContext.login();
    }

    @Test
    public void shouldFailFileLoginModuleEmptyUsernameFromCallback() throws Exception {

        context.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);

                final String fileLoginModuleName = "org.kaazing.gateway.security.auth.FileLoginModule";
                final HashMap<String, Object> fileOptions = new HashMap<>();
                fileOptions.put("file", "src/test/resources/jaas-config.xml");

                final AppConfigurationEntry fileEntry = new AppConfigurationEntry(fileLoginModuleName, REQUIRED, fileOptions);

                will(returnValue(new AppConfigurationEntry[]{fileEntry}));
            }
        });

        Subject subject = new Subject();
        DispatchCallbackHandler handler = new DispatchCallbackHandler();
        DefaultLoginResult loginResult = new DefaultLoginResult();
        handler.register(LoginResultCallback.class, new LoginResultCallbackHandler(loginResult));
        handler.register(NameCallback.class, new NameCallbackHandler(null));
        handler.register(PasswordCallback.class, new PasswordCallbackHandler(GOOD_PASSWORD.toCharArray()));
        LoginContext loginContext = new ResultAwareLoginContext("demo", subject, handler, configuration, loginResult);

        context.assertIsSatisfied();
        assertNotNull(loginContext);

        thrown.expect(LoginException.class);
        loginContext.login();
    }

    @Test
    public void shouldFailFileLoginModuleEmptyPasswordFromCallback() throws Exception {

        context.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);

                final String fileLoginModuleName = "org.kaazing.gateway.security.auth.FileLoginModule";
                final HashMap<String, Object> fileOptions = new HashMap<>();
                fileOptions.put("file", "src/test/resources/jaas-config.xml");

                final AppConfigurationEntry fileEntry = new AppConfigurationEntry(fileLoginModuleName, REQUIRED, fileOptions);

                will(returnValue(new AppConfigurationEntry[]{fileEntry}));
            }
        });

        Subject subject = new Subject();
        DispatchCallbackHandler handler = new DispatchCallbackHandler();
        DefaultLoginResult loginResult = new DefaultLoginResult();
        handler.register(LoginResultCallback.class, new LoginResultCallbackHandler(loginResult));
        handler.register(NameCallback.class, new NameCallbackHandler(GOOD_USERNAME));
        handler.register(PasswordCallback.class, new PasswordCallbackHandler(null));
        LoginContext loginContext = new ResultAwareLoginContext("demo", subject, handler, configuration, loginResult);

        context.assertIsSatisfied();
        assertNotNull(loginContext);

        thrown.expect(LoginException.class);
        loginContext.login();
    }

}
