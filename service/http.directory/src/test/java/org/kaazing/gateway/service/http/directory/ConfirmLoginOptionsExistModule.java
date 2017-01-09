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
package org.kaazing.gateway.service.http.directory;

import static org.kaazing.gateway.server.spi.security.LoginModuleOptions.CONFIG_DIRECTORY;
import static org.kaazing.gateway.server.spi.security.LoginModuleOptions.CONFIG_DIRECTORY_NAME;
import static org.kaazing.gateway.server.spi.security.LoginModuleOptions.EXPIRING_STATE;
import static org.kaazing.gateway.server.spi.security.LoginModuleOptions.EXPIRING_STATE_NAME;
import static org.kaazing.gateway.server.spi.security.LoginModuleOptions.KEYSTORE;
import static org.kaazing.gateway.server.spi.security.LoginModuleOptions.KEYSTORE_NAME;
import static org.kaazing.gateway.server.spi.security.LoginModuleOptions.TRUSTSTORE;
import static org.kaazing.gateway.server.spi.security.LoginModuleOptions.TRUSTSTORE_NAME;

import java.security.KeyStore;
import java.security.Principal;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.kaazing.gateway.server.spi.security.ExpiringState;

/**
 * Login module that always logs in
 *
 */
public class ConfirmLoginOptionsExistModule implements LoginModule {
    private static final Principal ROLE_PRINCIPAL = new RolePrincipal();

    private Subject subject;
    private Map<String, ?> options;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
        Map<String, ?> options) {
        this.subject = subject;
        this.options = options;
    }

    @Override
    public boolean login() throws LoginException {
        final String configDir = CONFIG_DIRECTORY.get(options);
        assertNotNull(configDir, CONFIG_DIRECTORY_NAME);
        final ExpiringState es = EXPIRING_STATE.get(options);
        assertNotNull(es, EXPIRING_STATE_NAME);
        final KeyStore keyStore = KEYSTORE.get(options);
        assertNotNull(keyStore, KEYSTORE_NAME);
        final KeyStore trustStore = TRUSTSTORE.get(options);
        assertNotNull(trustStore, TRUSTSTORE_NAME);
        return true;
    }

    private void assertNotNull(Object type, String propertyName) throws LoginException {
        if (type == null) {
            throw new LoginException("Got a null options for type " + propertyName);
        }
    }

    @Override
    public boolean commit() throws LoginException {
        subject.getPrincipals().add(ROLE_PRINCIPAL);
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        return true;
    }

    static class RolePrincipal implements Principal {
        @Override
        public String getName() {
            return "AUTHORIZED";
        }
    }

}
