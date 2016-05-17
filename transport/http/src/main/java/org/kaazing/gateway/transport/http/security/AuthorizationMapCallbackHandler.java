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
package org.kaazing.gateway.transport.http.security;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.kaazing.gateway.transport.http.bridge.filter.HttpSubjectSecurityFilter;

public class AuthorizationMapCallbackHandler implements CallbackHandler {
    private final HttpSubjectSecurityFilter.AuthorizationMap map;

    public AuthorizationMapCallbackHandler(HttpSubjectSecurityFilter.AuthorizationMap map) {
        this.map = map;
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof AuthorizationMapCallback) {
                ((AuthorizationMapCallback) callback).setMap(map);
            } else {
                throw new UnsupportedCallbackException(callback);
            }
        }
    }
}
