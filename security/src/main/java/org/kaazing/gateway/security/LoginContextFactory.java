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
package org.kaazing.gateway.security;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

public interface LoginContextFactory {

    /**
     * Create a login context for the provided authentication parameters.
     *
     * @param additionalCallbacks additional callbacks to be made available to the login module chain
     * @return a login context based upon the provided authentication parameters
     * @throws javax.security.auth.login.LoginException
     *            when a login context cannot be created based on the authentication parameters
     */
    LoginContext createLoginContext(TypedCallbackHandlerMap additionalCallbacks) throws LoginException;

    /**
     * For login context providers that can abstract their tokens into a username and password,
     * this is a utility method that can create the login context based on the provided username and password.
     *
     * @param subject  the subject that has been created for the user, or <code>null</code> if none has been created.
     * @param username the presented user name
     * @param password the presented password
     * @return a login context based on the parameters
     * @throws javax.security.auth.login.LoginException when a login context cannot be created
     */
    LoginContext createLoginContext(Subject subject, String username, char[] password) throws LoginException;
}

