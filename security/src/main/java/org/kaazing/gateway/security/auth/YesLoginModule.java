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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

/**
 * A login module that says yes to everyone and can load customizable roles for authorisation.
 * <p/>
 * Login Module Options:<br/>
 *   roles:  A comma-separated list of roles that will be established in the subject.
 * <p/>
 * Typical usage:
 * <pre>
 * GatewayLauncher.LoginModuleConfig yesLoginModule = new GatewayLauncher.LoginModuleConfig("demo",
 *               "class:org.kaazing.gateway.security.auth.YesLoginModule", "requisite")
 *               .options("roles", "AUTHORIZED, ADMINISTRATOR");
 * </pre>
 */
public class YesLoginModule extends BaseStateDrivenLoginModule {

    private List<String> roles = new ArrayList<>();

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, sharedState, options);

        resolveRoles(options);
    }

    private void resolveRoles(Map<String, ?> options) {
        String roleString = (String) options.get("roles");
        if (roleString != null && null != roles)  {
           String[] roleNames = roleString.split(",");
           for (int i = 0; i < roleNames.length; i++) {
               if (roleNames[i] != null) {
                   roleNames[i] = roleNames[i].trim();
               }
           }
           roles = Arrays.asList(roleNames);
        }
    }

    @Override
    protected boolean doLogin() throws LoginException {
        return true;
    }

    @Override
    protected boolean doCommit() throws LoginException {
        if (roles != null) {
            for (String role : roles) {
                final String role0 = role;
                subject.getPrincipals().add(new Principal() {
                    @Override
                    public String getName() {
                        return role0;
                    }
                });
            }
        }
        return true;
    }

    @Override
    protected boolean doLogout() throws LoginException {
        return true;
    }
}
