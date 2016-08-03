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
package org.kaazing.gateway.service.turn.rest;

import java.security.Principal;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

public class TestLoginModule implements LoginModule {

    // initial state
    private Subject subject;
    private Map<String, ?> sharedState;

    private RolePrincipal userPrincipal;
    
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
        Map<String, ?> options) {
        this.subject = subject;
        this.sharedState = sharedState;
    }

    @Override
    public boolean login() throws LoginException {
        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        String username = (String)sharedState.get("javax.security.auth.login.name");
        userPrincipal = new RolePrincipal(username);
        subject.getPrincipals().add(userPrincipal);
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
    
    private static class RolePrincipal implements Principal {

        private final String name;

        public RolePrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return("Role:  " + name);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null)
                return false;

            if (this == o)
                return true;

            if (!(o instanceof RolePrincipal))
                return false;
            RolePrincipal that = (RolePrincipal)o;

            return this.getName().equals(that.getName());
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }
}

