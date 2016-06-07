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
package org.kaazing.gateway.service.proxy;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.service.ServiceProperties;
import org.kaazing.mina.core.session.IoSessionEx;

public interface ProxyServiceExtensionSpi {
    /**
     * The proxy service establishes a connection based on the configured "connect"
     * whenever a new client connection is received (on the configured "accept").
     * When this connect completes extensions are notified via
     * proxiedConnectionEstablished() and given both the IoSessionEx representing
     * the accept side of the end-to-end connection and the IoSessionEx representing
     * the connect side of the end-to-end connection.
     *
     * @param acceptSession - the accept side of the end-to-end proxy connection
     * @param connectSession - the connect side of the end-to-end proxy connection
     */
    default void proxiedConnectionEstablished(IoSessionEx acceptSession, IoSessionEx connectSession) {
        // no behavior by default so an extension can decide where it attaches its behavior
    }

    /**
     * Whenever a new client connection is received (on the configured "accept")
     * the extension is giving an opportunity to initialize the associated session.
     *
     * @param acceptSession - the accept side of the end-to-end proxy connection
     * @param properties - the service properties that contain the values used to
     *                     configure the proxy service
     */
    default void initAcceptSession(IoSession acceptSession, ServiceProperties properties) {
        // no behavior by default so an extension can decide where it attaches its behavior
    }

    /**
     * Whenever a new backend connection is created (on the configured "connect") in
     * response to a client connection the extension is giving an opportunity to
     * initialize the associated session.
     *
     * @param connectSession - the connect side of the end-to-end proxy connection
     * @param properties - the service properties that contain the values used to
     *                     configure the proxy service
     */
    default void initConnectSession(IoSession connectSession, ServiceProperties properties) {
        // no behavior by default so an extension can decide where it attaches its behavior
    }
}
