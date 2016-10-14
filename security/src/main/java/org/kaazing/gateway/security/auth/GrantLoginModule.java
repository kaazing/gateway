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

import java.security.Principal;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

public class GrantLoginModule implements LoginModule {
    private Principal principal;
    private Subject subject;
    private String name;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.name = (String) options.get("name");
    }

    @Override
    public boolean login() throws LoginException {
        this.principal = new GrantPrincipal(this.name);

        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        this.subject.getPrincipals().add(this.principal);
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        this.subject.getPrincipals().remove(this.principal);
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        this.subject.getPrincipals().remove(this.principal);
        return true;
    }

}
