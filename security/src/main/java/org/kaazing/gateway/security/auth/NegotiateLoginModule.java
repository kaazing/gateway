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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.kaazing.gateway.server.spi.security.AuthenticationTokenCallback;
import org.kaazing.gateway.util.Encoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NegotiateLoginModule extends BaseStateDrivenLoginModule {

    public static final Charset UTF8 = Charset.forName("UTF-8");

    public static final String CLASS_NAME = NegotiateLoginModule.class.getName();
    public static final Logger LOG = LoggerFactory.getLogger(CLASS_NAME);

    private static final String KAAZING_TOKEN_KEY = "org.kaazing.gateway.server.auth.token";
    private static final String KAAZING_GSS_TOKEN_KEY = "org.kaazing.gateway.server.auth.gss.token";

    private boolean debug;
    private boolean tryFirstToken;

    private final NegotiateLoginModuleCallbackRegistrar callbackRegistrar;

    private static NegotiateLoginModuleCallbackRegistrar newCallbackRegistrar() {
        ServiceLoader<NegotiateLoginModuleCallbackRegistrar> loader = ServiceLoader.load(
            NegotiateLoginModuleCallbackRegistrar.class);
        Iterator<NegotiateLoginModuleCallbackRegistrar> iterator = loader.iterator();
        if (iterator.hasNext()) {
            // If this is in the context of Enterprise Gateway, then load the Enterprise-specific
            // registrar that can register additional Callbacks.
            return iterator.next();
        }
        // Otherwise, return null.
        return null;
    }

    public NegotiateLoginModule() {
        this.callbackRegistrar = newCallbackRegistrar();
    }

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, sharedState, options);
        this.debug = "true".equalsIgnoreCase((String) options.get("debug"));
        this.tryFirstToken = "true".equalsIgnoreCase((String) options.get("tryFirstToken"));
    }

    @Override
    protected boolean doLogin() throws LoginException {

        if (! authenticationSchemeIsNegotiate()) {
            return true;
        }

        if (tryFirstToken) {
            try {
                attemptAuthenticate(true);
                return true;
            } catch (Exception le) {
                if (debug) {
                    LOG.debug("[NegotiateLoginModule] " + "reading from shared state failed: " + le.getMessage());
                }
            }
        }

        try {
            attemptAuthenticate(false);
            return true;
        } catch (Exception loginException) {
            if (debug) {
                LOG.debug("[NegotiateLoginModule] " + "regular authentication failed: " + loginException.getMessage());
            }
        }

        return false;
    }

    private void attemptAuthenticate(boolean useSharedState) throws LoginException {

        String credentials;
        try {
            String negotiateAuthToken = getNegotiateAuthToken(useSharedState);
            if (negotiateAuthToken == null) {
                throw new LoginException("No HTTP Negotiate Authentication Token found.");
            }
            if (negotiateAuthToken.startsWith("Negotiate ")) {
                negotiateAuthToken = negotiateAuthToken.substring("Negotiate ".length());
            }
            if (this.callbackRegistrar != null) {
                ByteBuffer gssBuf = Encoding.BASE64.decode(ByteBuffer.wrap(negotiateAuthToken.getBytes(UTF8)));
                byte[] gss = new byte[gssBuf.remaining()];
                gssBuf.get(gss);
                ((Map) sharedState).put(KAAZING_GSS_TOKEN_KEY, gss);

                this.callbackRegistrar.register((DispatchCallbackHandler) handler, negotiateAuthToken, gss);
            }
        } catch (Throwable e) {
            if (debug) {
                LOG.debug("Exception decoding HTTP Basic Authentication token", e);
            }
            throw (LoginException) (new LoginException()).initCause(e);
        }
    }

    private boolean authenticationSchemeIsNegotiate() throws LoginException {
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
                authenticationTokenCallback.getAuthenticationToken().getScheme().equalsIgnoreCase("Negotiate");

    }

    private String getNegotiateAuthToken(boolean useSharedState) {
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
        return authenticationTokenCallback.getAuthenticationToken().get();
    }

    private void cleanState() {
        if (this.callbackRegistrar != null) {
            sharedState.remove(KAAZING_GSS_TOKEN_KEY);
            this.callbackRegistrar.unregister((DispatchCallbackHandler) handler);
        }
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
