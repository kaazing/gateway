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

import static java.util.stream.Collectors.joining;
import static javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.mina.util.Base64;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.kaazing.gateway.security.auth.context.DefaultLoginContextFactory;
import org.kaazing.gateway.security.auth.context.ResultAwareLoginContext;
import org.kaazing.gateway.security.auth.token.DefaultAuthenticationToken;
import org.kaazing.gateway.server.spi.security.AuthenticationToken;
import org.kaazing.gateway.server.spi.security.AuthenticationTokenCallback;
import org.kaazing.gateway.server.spi.security.LoginResultCallback;
import org.kaazing.netx.URLConnectionHelper;

public class NegotiateLoginModuleWithDataCallbackRegisterTest {
    private static final String REALM_NAME = "demo";
    private static final String NEGOTIATE_AUTH_SCHEME = "Negotiate";
    private static final byte[] TOKEN_DATA = "test".getBytes();

    private static ClassLoader classLoader;

    DefaultLoginContextFactory factory;
    Mockery context;
    Configuration configuration;

    @BeforeClass
    public static void before() throws Exception {
        classLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new TestClassLoader(NegotiateLoginModuleWithDataCallbackRegisterTest.TestNegotiateDataCallbackRegistrar.class.getName()));
    }

    @AfterClass
    public static void after() {
        Thread.currentThread().setContextClassLoader(classLoader);
    }
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
    public void testNegotiateModuleFailsOnInvalidTokenData() throws Exception {
        AuthenticationToken s = new DefaultAuthenticationToken(NEGOTIATE_AUTH_SCHEME,
                NEGOTIATE_AUTH_SCHEME + " " + new String(Base64.encodeBase64(TOKEN_DATA)).substring(1));
        context.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);
                final String loginModuleName = "org.kaazing.gateway.security.auth.NegotiateLoginModule";
                final HashMap<String, Object> options = new HashMap<>();
                final AppConfigurationEntry entry = new AppConfigurationEntry(loginModuleName, REQUIRED, options);
                will(returnValue(new AppConfigurationEntry[]{entry}));
            }
        });

        Subject subject = new Subject();
        DispatchCallbackHandler handler = new DispatchCallbackHandler();
        DefaultLoginResult loginResult = new DefaultLoginResult();
        handler.register(AuthenticationTokenCallback.class, new AuthenticationTokenCallbackHandler(s));
        handler.register(LoginResultCallback.class, new LoginResultCallbackHandler(loginResult));
        LoginContext loginContext = new ResultAwareLoginContext(REALM_NAME, subject, handler, configuration, loginResult);

        context.assertIsSatisfied();
        assertNotNull(loginContext);

        thrown.expect(LoginException.class);
        loginContext.login();
    }

    @Test
    public void testNegotiateLoginModuleRegistersGssCallback() throws Exception {
        AuthenticationToken s = new DefaultAuthenticationToken(NEGOTIATE_AUTH_SCHEME,
                NEGOTIATE_AUTH_SCHEME + " " + new String(Base64.encodeBase64(TOKEN_DATA)));
        context.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);
                final String loginModuleName = "org.kaazing.gateway.security.auth.NegotiateLoginModule";
                final HashMap<String, Object> options = new HashMap<>();
                final AppConfigurationEntry entry = new AppConfigurationEntry(loginModuleName, REQUIRED, options);
                will(returnValue(new AppConfigurationEntry[]{entry}));
            }
        });

        Subject subject = new Subject();
        DispatchCallbackHandler handler = new DispatchCallbackHandler();
        DefaultLoginResult loginResult = new DefaultLoginResult();
        handler.register(AuthenticationTokenCallback.class, new AuthenticationTokenCallbackHandler(s));
        handler.register(LoginResultCallback.class, new LoginResultCallbackHandler(loginResult));
        LoginContext loginContext = new ResultAwareLoginContext(REALM_NAME, subject, handler, configuration, loginResult);

        context.assertIsSatisfied();
        assertNotNull(loginContext);

        loginContext.login();

        final CallbackHandler negotiateDataCallbackHandler = handler.getDispatchMap().get(TestNegotiateDataCallback.class);
        assertNotNull(negotiateDataCallbackHandler);

        TestNegotiateDataCallback gssCallback = new TestNegotiateDataCallback();
        negotiateDataCallbackHandler.handle(new Callback[]{gssCallback});
        assertArrayEquals(TOKEN_DATA, gssCallback.getData());
    }

    @Test
    public void testNegotiateLoginModuleRegistersGssCallbackWhenNotInSharedState() throws Exception {
        AuthenticationToken s = new DefaultAuthenticationToken(NEGOTIATE_AUTH_SCHEME,
                NEGOTIATE_AUTH_SCHEME + " " + new String(Base64.encodeBase64(TOKEN_DATA)));
        context.checking(new Expectations() {
            {
                oneOf(configuration).getAppConfigurationEntry(REALM_NAME);
                final String loginModuleName = "org.kaazing.gateway.security.auth.NegotiateLoginModule";
                final HashMap<String, Object> options = new HashMap<>();
                options.put("tryFirstToken", "true");
                final AppConfigurationEntry entry = new AppConfigurationEntry(loginModuleName, REQUIRED, options);
                will(returnValue(new AppConfigurationEntry[]{entry}));
            }
        });

        Subject subject = new Subject();
        DispatchCallbackHandler handler = new DispatchCallbackHandler();
        DefaultLoginResult loginResult = new DefaultLoginResult();
        handler.register(AuthenticationTokenCallback.class, new AuthenticationTokenCallbackHandler(s));
        handler.register(LoginResultCallback.class, new LoginResultCallbackHandler(loginResult));
        LoginContext loginContext = new ResultAwareLoginContext(REALM_NAME, subject, handler, configuration, loginResult);

        context.assertIsSatisfied();
        assertNotNull(loginContext);

        loginContext.login();

        final CallbackHandler gssCallbackHandler = handler.getDispatchMap().get(TestNegotiateDataCallback.class);
        assertNotNull(gssCallbackHandler);

        TestNegotiateDataCallback gssCallback = new TestNegotiateDataCallback();
        gssCallbackHandler.handle(new Callback[]{gssCallback});
        assertArrayEquals(TOKEN_DATA, gssCallback.getData());
    }

    /**
     * A classloader whose getResources("META-INF/services/org.kaazing.gateway.security.auth.NegotiateLoginModuleCallbackRegistrar")
     * method will return a URL whose contents will be the list of class names supplied in the constructor.
     * This avoids the need for test meta-info resources files to be available on the test class path.
     */
    private static class TestClassLoader extends ClassLoader {
        private final List<URL> urls;

        TestClassLoader(String... factorySpiClassNames) throws IOException {
            URLConnectionHelper helper = URLConnectionHelper.newInstance();
            String contents = Arrays.stream(factorySpiClassNames).collect(joining("\n"));
            URI uri = URI.create("data:," + contents);
            URL url = helper.toURL(uri);
            urls = Collections.singletonList(url);
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            if (name.equals("META-INF/services/" + NegotiateLoginModuleCallbackRegistrar.class.getName())) {
                return Collections.enumeration(urls);
            }
            return super.getResources(name);
        }

    }

    public static class TestNegotiateDataCallback implements Callback {
        private byte[] data;

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }
    }

    public static class TestNegotiateDataCallbackHandler implements CallbackHandler {

        private final byte[] gssData;

        public TestNegotiateDataCallbackHandler(byte[] gssData) {
            this.gssData = gssData;
        }

        @Override
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (Callback callback : callbacks) {
                if (callback instanceof TestNegotiateDataCallback) {
                    ((TestNegotiateDataCallback) callback).setData(gssData);
                } else {
                    throw new UnsupportedCallbackException(callback);
                }
            }
        }

    }

    public static class TestNegotiateDataCallbackRegistrar extends NegotiateLoginModuleCallbackRegistrar {
        public void register(DispatchCallbackHandler handler, String authToken, byte[] gss)
        {
             handler.register(TestNegotiateDataCallback.class, new TestNegotiateDataCallbackHandler(gss));
        }

        public void unregister(DispatchCallbackHandler handler) {
            handler.unregister(TestNegotiateDataCallback.class);
        }
    }
}
