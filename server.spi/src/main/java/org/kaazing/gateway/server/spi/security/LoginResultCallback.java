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
package org.kaazing.gateway.server.spi.security;

import java.util.concurrent.atomic.AtomicReference;

import javax.security.auth.callback.Callback;

/**
 * {@code LoginResultCallback} provides access to a {@link LoginResult} object from with a
 * {@link javax.security.auth.spi.LoginModule}, permitting communication of additional login result data
 * to the {@link javax.security.auth.login.LoginContext} provider, i.e. the gateway server.
 * <p>
 *
 * @see <a href="http://download.oracle.com/javase/6/docs/technotes/guides/security/jaas/JAASRefGuide.html">JAAS Reference Guide </a>
 */
public class LoginResultCallback implements Callback {

    private AtomicReference<LoginResult> loginResult = new AtomicReference<>();

        public LoginResultCallback() {
            super();
        }

        public LoginResult getLoginResult() {
            return loginResult.get();
        }

        /**
         * <strong>For internal use only.</strong>
         * @param loginResult the login result object to present to the login module
         * @throws UnsupportedOperationException when called from outside
         *         the {@link javax.security.auth.login.LoginContext} object's
         *         {@link javax.security.auth.callback.CallbackHandler}.
         */
        public final void setLoginResult(LoginResult loginResult) {
            if ( !this.loginResult.compareAndSet(null, loginResult)) {
                throw new UnsupportedOperationException("A Login Result already exists in this callback and cannot be set again.");
            }
        }


}
