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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.kaazing.gateway.security.auth.BaseStateDrivenLoginModule;
import org.kaazing.gateway.security.auth.DispatchCallbackHandler;
import org.kaazing.gateway.security.auth.NameCallbackHandler;
import org.kaazing.gateway.security.auth.PasswordCallbackHandler;
import org.kaazing.gateway.server.spi.security.AuthenticationToken;
import org.kaazing.gateway.server.spi.security.AuthenticationTokenCallback;
import org.kaazing.gateway.util.Encoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicLoginModule extends BaseStateDrivenLoginModule {

    public static final Charset UTF8 = Charset.forName("UTF-8");

    public static final String CLASS_NAME = BasicLoginModule.class.getName();
    public static final Logger LOG = LoggerFactory.getLogger(CLASS_NAME);

    private static final String KAAZING_TOKEN_KEY = "org.kaazing.gateway.server.auth.token";

    private static final String NAME = "javax.security.auth.login.name";
    private static final String PWD = "javax.security.auth.login.password";

    private boolean debug;
    private boolean tryFirstToken;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, sharedState, options);
        this.debug = "true".equalsIgnoreCase((String) options.get("debug"));
        this.tryFirstToken = "true".equalsIgnoreCase((String) options.get("tryFirstToken"));
    }

    @Override
    protected boolean doLogin() throws LoginException {

        if (!authenticationSchemeIsBasic()) {
            return true;
        }

        if (tryFirstToken) {
            try {
                attemptAuthenticate(true);
                return true;
            } catch (LoginException le) {
                if (debug) {
                    LOG.debug("[BasicLoginModule] " + "reading from shared state failed: " + le.getMessage());
                }
            }
        }

        try {
            attemptAuthenticate(false);
            return true;
        } catch (LoginException loginException) {
            cleanState();
            if (debug) {
                LOG.debug("[BasicLoginModule] " + "regular authentication failed: " + loginException.getMessage());
            }
        }

        return false;
    }

    private boolean authenticationSchemeIsBasic() throws LoginException {
         final AuthenticationTokenCallback authenticationTokenCallback = new AuthenticationTokenCallback();

        try {
            handler.handle(new Callback[]{authenticationTokenCallback});
        } catch (IOException e) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Encountered exception handling authenticationTokenCallback.", e);
            }
            return false;
        } catch (UnsupportedCallbackException e) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("UnsupportedCallbackException handling authenticationTokenCallback.");
            }
            return false;
        }

        return authenticationTokenCallback.getAuthenticationToken() != null &&
               authenticationTokenCallback.getAuthenticationToken().getScheme() != null &&
                authenticationTokenCallback.getAuthenticationToken().getScheme().equalsIgnoreCase("Basic");

    }


    private void attemptAuthenticate(boolean useSharedState) throws LoginException {

        String credentials;
        try {
            String basicAuthToken = getBasicAuthToken(useSharedState);
            if (basicAuthToken == null) {
                throw new LoginException("No HTTP Basic Authentication Token found.");
            }
            if (basicAuthToken.startsWith("Basic ")) {
                basicAuthToken = basicAuthToken.substring("Basic ".length());
            }
            ByteBuffer credentialsBuf = Encoding.BASE64.decode(ByteBuffer.wrap(basicAuthToken.getBytes(UTF8)));
            byte[] credentialsBytes = new byte[credentialsBuf.remaining()];
            credentialsBuf.get(credentialsBytes);
            credentials = new String(credentialsBytes, UTF8);

            // username:password
            int colonAt = credentials.indexOf(':');
            if (colonAt != -1) {

                String username = credentials.substring(0, colonAt);
                char[] password = credentials.substring(colonAt + 1).toCharArray();

                ((Map) sharedState).put(NAME, username);
                ((Map) sharedState).put(PWD, password);
                ((DispatchCallbackHandler) handler).register(NameCallback.class, new NameCallbackHandler(username));
                ((DispatchCallbackHandler) handler).register(PasswordCallback.class, new PasswordCallbackHandler(password));
            } else {
                throw new LoginException("Syntax error while decoding HTTP Basic Authentication token.");
            }
        } catch (Throwable e) {
            if (debug) {
                LOG.debug("Exception decoding HTTP Basic Authentication token", e);
            }
            cleanState();
            throw (LoginException) (new LoginException(e.getMessage())).initCause(e);
        }
    }

    private String getBasicAuthToken(boolean useSharedState) {
        if (useSharedState) {
            return (String) ((Map) sharedState).get(KAAZING_TOKEN_KEY);
        }

        final AuthenticationTokenCallback authenticationTokenCallback = new AuthenticationTokenCallback();
        try {
            handler.handle(new Callback[]{authenticationTokenCallback});
        } catch (IOException e) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Encountered exception handling authenticationTokenCallback.", e);
            }
            return null;
        } catch (UnsupportedCallbackException e) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("UnsupportedCallbackException handling authenticationTokenCallback.");
            }
            return null;
        }

        AuthenticationToken authToken = authenticationTokenCallback.getAuthenticationToken();
        if (authToken != null) {
            return authToken.get();
        }

        return null;
    }

    private void cleanState() {
        sharedState.remove(NAME);
        sharedState.remove(PWD);
        ((DispatchCallbackHandler) handler).unregister(NameCallback.class);
        ((DispatchCallbackHandler) handler).unregister(PasswordCallback.class);
    }

    @Override
    protected boolean doCommit() throws LoginException {
        return true;
    }

    @Override
    protected boolean doLogout() throws LoginException {
        cleanState();
        return true;
    }



}
