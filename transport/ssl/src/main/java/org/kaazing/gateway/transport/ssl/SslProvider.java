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
package org.kaazing.gateway.transport.ssl;

import java.security.AccessController;
import java.security.Provider;

public final class SslProvider
    extends Provider {

    private static final long serialVersionUID = -7759284199245054463L;

    public SslProvider() {
        super("SslTransport", 1.0, "Provider for SSL Transport Certificate Management");

        AccessController.doPrivileged(new java.security.PrivilegedAction<Object>() {
            @Override
            public Object run() {
                put("KeyManagerFactory.SslTransport", "org.kaazing.gateway.transport.ssl.SslKeyManagerFactorySpi");
                return null;
            }
        });
    }
}
