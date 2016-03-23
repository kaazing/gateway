/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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
import javax.security.auth.login.LoginException;
import org.kaazing.gateway.security.auth.config.parse.DefaultUserConfig;


public class BasicLoginModuleWithMultiplePrincipalsOnTheSubject extends BasicLoginModuleWithDefaultUserConfig {

    private DefaultUserConfig defaultPrincipalNotForLogging = new DefaultUserConfig();
    private static final String SECOND_PRINCIPAL_NAME = "secondPrincipalName";
    private static final String SECOND_PRINCIPAL_PASSWORD = "secondPrincipalPassword";

    @Override
    public boolean commit() throws LoginException {
        super.commit();
        defaultPrincipalNotForLogging.setName(SECOND_PRINCIPAL_NAME);
        defaultPrincipalNotForLogging.setPassword(SECOND_PRINCIPAL_PASSWORD);
        subject.getPrincipals().add(defaultPrincipalNotForLogging);
        return succeeded;
    }

}
