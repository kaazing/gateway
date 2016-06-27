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

import java.io.IOException;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.kaazing.gateway.security.LoginContextFactory;
import org.kaazing.gateway.security.TypedCallbackHandlerMap;
import org.kaazing.gateway.security.auth.AuthenticationTokenCallbackHandler;
import org.kaazing.gateway.security.auth.DefaultLoginResult;
import org.kaazing.gateway.security.auth.DispatchCallbackHandler;
import org.kaazing.gateway.security.auth.LoginResultCallbackHandler;
import org.kaazing.gateway.security.auth.token.DefaultAuthenticationToken;
import org.kaazing.gateway.server.spi.security.AuthenticationTokenCallback;
import org.kaazing.gateway.server.spi.security.LoginResult;
import org.kaazing.gateway.server.spi.security.LoginResultCallback;
import org.slf4j.Logger;

public class DefaultLoginContextFactory implements LoginContextFactory {

    protected static final Logger LOG = LoginContextFactories.getLogger();
    private static final String ERROR_MSG = "Failed to create a login context.";

    protected boolean logEnabled() {
        return  LOG.isTraceEnabled();
    }

    protected void log(String s, Object... objs) {
        LOG.trace(String.format(s, objs));
    }

    protected final String name;
    protected final Configuration configuration;


    public DefaultLoginContextFactory(String name,
                                      Configuration configuration) {
        this.name = name;
        this.configuration = configuration;
    }

    public String getName() {
        return name;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    private DispatchCallbackHandler createDefaultCallbackHandler(final LoginResult loginResult,
                                                                 TypedCallbackHandlerMap additionalCallbacks) {
        DispatchCallbackHandler handler = new DispatchCallbackHandler();
        handler.registerAll(additionalCallbacks);
        handler.register(LoginResultCallback.class, new LoginResultCallbackHandler(loginResult));
        return handler;
    }



    @Override
    public LoginContext createLoginContext(TypedCallbackHandlerMap additionalCallbacks) throws LoginException {
        try {
            final DefaultLoginResult loginResult = new DefaultLoginResult();

            // Expect to have a Login Module configured, such that if it
            // "fronts" the Login Module chain, arrange for it to be able to
            // read the authorization token as provided.

            DispatchCallbackHandler handler = createDefaultCallbackHandler(loginResult,
                                                                           additionalCallbacks);

            defineAuthenticationTokenScheme(additionalCallbacks);

            return createLoginContext(handler, loginResult);

        } catch (Exception e) {
            final String msg = String.format("%s", ERROR_MSG);
            if (logEnabled()) {
                LOG.trace(msg, e);
            }
            throw (LoginException) new LoginException(msg).initCause(e);
        }
    }

    private void defineAuthenticationTokenScheme(TypedCallbackHandlerMap additionalCallbacks) {

        final AuthenticationTokenCallbackHandler callbackHandler =
                additionalCallbacks.get(AuthenticationTokenCallback.class,
                AuthenticationTokenCallbackHandler.class);

        DefaultAuthenticationToken authToken = null;

        if (callbackHandler != null) {
            authToken = (DefaultAuthenticationToken)
                    callbackHandler.getAuthToken();
        }

        if (authToken != null && !authToken.isEmpty()) {
            if (authToken.getScheme() == null) {
                String authorization = authToken.get();
                int spaceIdx = authorization.indexOf(" ");
                if (spaceIdx > 0) {
                    // The first "word" in the string is the HTTP authentication
                    // scheme.  We need to split that out so that we can look up
                    // the correct LoginContext factory class, based on that scheme.
                    //
                    // After splitting out the scheme, we create a new
                    // AuthenticationToken.  This time, we explicitly set the
                    // scheme and the challenge response data separately, for
                    // the benefit of the login modules which will consume this
                    // new AuthenticationToken (KG-2309).
                    String[] authorizationParts = new String[2];
                    authorizationParts[0] = authorization.substring(0, spaceIdx);
                    authorizationParts[1] = authorization.substring(spaceIdx + 1);
                    final String authType = authorizationParts[0];
                    authToken.setScheme(authType);
                }
            }
        }
    }

    /**
     * For login context providers that can abstract their tokens into a username and password,
     * this is a utility method that can create the login context based on the provided username and password.
     *
     * @param subject  the subject that has been created for the user, or <code>null</code> if none has been created.
     * @param username the presented user name
     * @param password the presented password
     * @return a login context based on the parameters
     * @throws LoginException when a login context cannot be created
     */
    @Override
    public LoginContext createLoginContext(Subject subject, final String username, final char[] password) throws LoginException {
        final DefaultLoginResult loginResult = new DefaultLoginResult();

        CallbackHandler handler = new CallbackHandler() {
            @Override
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {

                for (Callback callback : callbacks) {
                    if (callback instanceof NameCallback) {
                        ((NameCallback) callback).setName(username);
                    } else if (callback instanceof PasswordCallback) {
                        ((PasswordCallback) callback).setPassword(password);
                    } else if (callback instanceof LoginResultCallback) {
                        ((LoginResultCallback) callback).setLoginResult(loginResult);
                    } else {
                        throw new UnsupportedCallbackException(callback);
                    }
                }
            }
        };

        // use JAAS to login and establish subject principals
        return createLoginContext(subject, handler, loginResult);
    }

    /**
     * For login context providers that can abstract their tokens into a subject and a CallbackHandler
     * that understands their token, this is a utility method that can be called to construct a create login.
     *
     *
     * @param subject the subject that has been created for the user, or <code>null</code> if none has been created.
     * @param handler the callback handler that can understand
     * @param loginResult the login result object capturing the result of the login call.
     * @return a login context
     * @throws LoginException when a login context cannot be created
     */
    protected LoginContext createLoginContext(Subject subject, CallbackHandler handler, DefaultLoginResult loginResult)
            throws LoginException {
        return new ResultAwareLoginContext(name, subject, handler, configuration, loginResult);
    }

     /**
     * For login context providers that can abstract their tokens into a CallbackHandler
     * that understands their token, this is a utility method that can be called to construct a create login.
     *
     * @param handler the callback handler that can understand
     * @return a login context
     * @throws LoginException when a login context cannot be created
     */
    protected LoginContext createLoginContext(CallbackHandler handler, final DefaultLoginResult loginResult)
            throws LoginException {
        return createLoginContext(null, handler, loginResult);
    }



}

