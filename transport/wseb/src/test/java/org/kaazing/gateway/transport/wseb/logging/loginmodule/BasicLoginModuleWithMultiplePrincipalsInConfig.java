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
package org.kaazing.gateway.transport.wseb.logging.loginmodule;

import com.sun.security.auth.UnixPrincipal;
import javax.security.auth.login.LoginException;

public class BasicLoginModuleWithMultiplePrincipalsInConfig extends BasicLoginModuleWithDefaultUserConfig{

    private UnixPrincipal unixPrincipal = new UnixPrincipal("unixPrincipalName");

    @Override
    public boolean commit() throws LoginException {
        if (super.commit()) {
            subject.getPrincipals().add(unixPrincipal);
            return true;
        }
        return false;
    }

    @Override
    public boolean logout() throws LoginException {
        super.logout();
        subject.getPrincipals().remove(unixPrincipal);
        return true;
    }
}
