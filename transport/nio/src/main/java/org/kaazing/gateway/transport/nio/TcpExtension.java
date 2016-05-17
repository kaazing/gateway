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
package org.kaazing.gateway.transport.nio;

import org.apache.mina.core.session.IoSession;

/**
 * {@link TcpExtension} is part of <i>Service Provider Interface</i> <em>(SPI)</em> for TCP transport extension developers.
 * When TCP URI is bound, an instance of this class may be created using the corresponding
 * {@link TcpExtensionFactorySpi} that is registered through META-INF/services. This class can perform actions during
 * IoSession initialization when a client connects to the bound TCP URI, for example, if could add a filter to the
 * filter chain.
 */
public interface TcpExtension {

    /**
     * Called to give the extension an opportunity to act on each new client connection
     * @param session  The IoSession representing a new client connection
     */
    void initializeSession(IoSession session);

}
