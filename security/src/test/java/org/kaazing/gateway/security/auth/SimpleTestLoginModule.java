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

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.kaazing.gateway.server.spi.security.AuthenticationTokenCallback;

public class SimpleTestLoginModule extends BaseStateDrivenLoginModule {

    String[] validUserPasswords = {"joe", "welcome", "jane", "welcome"};

    @Override
    protected boolean doLogin() {
        AuthenticationTokenCallback atc = new AuthenticationTokenCallback();

        try {
            handler.handle(new Callback[] {atc});
        } catch (IOException e) {
            // TODO: log exception
            return false;
        } catch (UnsupportedCallbackException e) {
            // TODO: log exception
            return false;
        }

        String up = atc.getAuthenticationToken().get();
        String name = up.substring(0, up.indexOf(':'));
        String passwordCB = up.substring(up.indexOf(':')+1);
        for ( int i = 0; i < validUserPasswords.length; i+=2) {
            String user = validUserPasswords[i];
            String password = validUserPasswords[i+1];
            if ( name.equals(user) &&
                 passwordCB.equals(password)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean doCommit() {
        return true;
    }

    @Override
    protected boolean doLogout() {
        return true;
    }
}

