/*
 * Copyright 2014, Kaazing Corporation. All rights reserved.
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

package org.kaazing.gateway.transport.ws.extension;

import java.io.IOException;
import java.util.Comparator;

import org.kaazing.gateway.resource.address.ws.WsResourceAddress;


/**
 * {@link WebSocketExtensionFactorySpi} is part of <i>Service Provider Interface</i> <em>(SPI)</em> for extension developers.
 * <p>
 * Developing an extension involves implementing:
 * <UL>
 *   <LI> a sub-class of {@link WebSocketExtensionFactorySpi}
 *   <LI> a sub-class of {@link WebSocketExtensionSpi}
 *   <LI> (optional) a filter ({@link org.apache.mina.core.filterchain.IoFilter IoFilter})
 * </UL>
 * <p>
 */
public abstract class WebSocketExtensionFactorySpi {

    /**
     * Returns the name of the extension that this factory will create.
     *
     * @return String   name of the extension
     */
    public abstract String getExtensionName();
    
    public abstract Comparator<Extension> getComparator();
    
    /**
     * This method is called when the extension is requested by the client during the WebSocket handshake. 
     * @param requestedExtension  Extension token and parameters from the WebSocket handshake request
     * @param address    WebSocket resource address on which the handshake is taking place
     * @return           WebSocketExtensionSpi instance representing the active, negotiated extension
     * @throws IOException  If the extension cannot be negotiated. Throwing this exception will result
     *                      in failing the WebSocket connection.
     */
    public abstract WebSocketExtensionSpi negotiate(Extension requestedExtension, WsResourceAddress address)
            throws IOException;

}
