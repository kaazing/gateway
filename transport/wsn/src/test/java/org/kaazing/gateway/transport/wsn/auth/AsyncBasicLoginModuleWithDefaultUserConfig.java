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
package org.kaazing.gateway.transport.wsn.auth;

import javax.security.auth.login.LoginException;

import org.kaazing.gateway.security.auth.config.parse.DefaultUserConfig;

public class AsyncBasicLoginModuleWithDefaultUserConfig extends AsyncBasicLoginModule {
    private static final String TEST_PRINCIPAL_PASS = "testPrincipalPass";
    private static final String TEST_PRINCIPAL_NAME = "testPrincipalName";
    private DefaultUserConfig defaultPrincipal = new DefaultUserConfig();

    @Override
    public boolean commit() throws LoginException {
        if (super.commit()) {
            defaultPrincipal.setName(TEST_PRINCIPAL_NAME);
            defaultPrincipal.setPassword(TEST_PRINCIPAL_PASS);
            subject.getPrincipals().add(defaultPrincipal);
            return true;
        }
        return false;
    }

    @Override
    public boolean logout() throws LoginException {
        super.logout();
        subject.getPrincipals().remove(defaultPrincipal);
        return true;
    }
}