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

package org.kaazing.gateway.transport.ws.extension.spi;

import org.apache.mina.core.filterchain.IoFilter;
import org.kaazing.gateway.resource.address.ws.WsResourceAddress;
import org.kaazing.gateway.transport.ws.extension.WsExtension;

/**
 * {@link WebSocketExtensionSpi} is part of <i>Service Provider Interface</i> <em>(SPI)</em> for extension developers.
 * When an enabled extension is successfully negotiated, an instance of this class is created using the corresponding
 * {@link WebSocketExtensionFactorySpi} that is registered through META-INF/services. This class can supply
 * a filter that can view and manipulate WebSocket traffic.
 */
public abstract class WebSocketExtensionSpi {

    /**
     * This method is called when the extension is successfully negotiated.
     * @param extension Details of the negotiated extension and parameters (from the response header)
     * @param address The WebSocket address of the WebSocket connection
     */
    public void negotiated(WsExtension extension, WsResourceAddress address) {

    };

    /**
     * This method is called after the negotiated method. If allows extensions to provide a filter that will be added to
     * the filter chain after the WebSocket codec filter and so can be used to see and modify WebSocket frames going to
     * and from the client.
     * @param extension Details of the negotiated extension and parameters
     * @return A filter which is to be added to the filter chain, or null if none is to be added
     */
    public IoFilter getFilter(WsExtension extension) {
        return null;
    };

}
