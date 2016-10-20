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
package org.kaazing.gateway.transport.http.multi.auth;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.kaazing.gateway.security.auth.BaseStateDrivenLoginModule;
import org.kaazing.gateway.server.spi.security.AuthenticationTokenCallback;

public class SecondFactorAlwaysFailLoginModule extends BaseStateDrivenLoginModule {

    @Override
    protected boolean doLogin() {
        AuthenticationTokenCallback atc = new AuthenticationTokenCallback();

        try {
            handler.handle(new Callback[]{atc});
        } catch (IOException e) {
            // TODO: log exception
            return false;
        } catch (UnsupportedCallbackException e) {
            // TODO: log exception
            return false;
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
