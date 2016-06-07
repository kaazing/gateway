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

public class NegotiateLoginModuleCallbackRegistrar {

    /**
     * Registers the Callbacks that will be used by the LoginModule.
     * Extensions such as Enterprise Gateway can use this hook to add extension-specific Callbacks.
     *
     * @param handler       Callbackhandler
     * @param authToken     used for authentication purposes
     * @param gss           GSS data used for Krb5
     */
    public void register(DispatchCallbackHandler handler, String authToken, byte[] gss) {
    }

    /**
     * Unregisters the Callbacks that will be used by the LoginModule.
     *
     * @param handler       Callbackhandler
     */
    public void unregister(DispatchCallbackHandler handler) {
    }
}
