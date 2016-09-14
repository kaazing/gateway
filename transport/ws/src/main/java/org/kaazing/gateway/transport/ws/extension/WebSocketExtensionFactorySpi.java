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
package org.kaazing.gateway.transport.ws.extension;

import static java.lang.String.format;

import java.net.ProtocolException;

import org.kaazing.gateway.resource.address.ws.WsResourceAddress;

/**
 * {@link WebSocketExtensionFactorySpi} is part of <i>Service Provider Interface</i> <em>(SPI)</em> for extension developers.
 * <p>
 * Developing an extension involves implementing:
 * <UL>
 *   <LI> a sub-class of {@link WebSocketExtensionFactorySpi}
 *   <LI> a sub-class of {@link WebSocketExtension}
 *   <LI> (optional) a filter ({@link org.apache.mina.core.filterchain.IoFilter IoFilter})
 * </UL>
 * <p>
 */
public abstract class WebSocketExtensionFactorySpi implements Comparable<WebSocketExtensionFactorySpi> {

    /**
     * Returns the name of the extension that this factory will create.
     *
     * @return String   name of the extension
     */
    public abstract String getExtensionName();

    /**
     * This method is called when a connector wants to include the extension in websocket handsahke request.
     *
     * @param extensionHelper extension helper
     * @param address    WebSocket resource address on which the handshake is taking place
     * @return         - WebSocketExtensionSpi instance that needs to be included in handshake request
     *                   or null if the extension doesn't want to be included in handshake request
     */
    public WebSocketExtension offer(ExtensionHelper extensionHelper, WsResourceAddress address) {
        return null;
    }

    /**
     * This method is called when the extension is requested by the client during the WebSocket handshake.
     * @param requestedExtension  Extension token and parameters from the WebSocket handshake request
     * @param extensionHelper TODO
     * @param address    WebSocket resource address on which the handshake is taking place
     * @return         - WebSocketExtensionSpi instance representing the active, negotiated extension,
     *                   or null if the extension request from the client is rejected but the websocket
     *                   connection need not be failed.
     * @throws ProtocolException
     *                   If the extension header is invalid for example invalid extension parameters
     *                   (protocol violation). Throwing this exception will result in failing the WebSocket
     *                   connection.
     */
    public abstract WebSocketExtension negotiate(ExtensionHeader requestedExtension, ExtensionHelper extensionHelper,
        WsResourceAddress address) throws ProtocolException;


    /**
     * This method allows extensions to specify the order that they would like to be placed on the Filter Chain.
     *
     * @return A {@link WebSocketExtensionFactorySpi.ExtensionOrderCategory} that specifies the extension type and where it
     * should be ordered
     */
    public ExtensionOrderCategory getOrderCategory() {
        return ExtensionOrderCategory.OTHER;
    }

    /**
     * An ordered enum that describes the category that an extension requests to be ordered in.  The enum is ordered by the order
     * they appear in the WebSocket handshake response extensions header.  That is the nearest to the network being last, see
     * https://tools.ietf.org/html/draft-ietf-hybi-permessage-compression-21#section-3
     */
    public enum ExtensionOrderCategory{
        /**
         * Other, meaning not any of the other categories
         */
        OTHER,
        /**
         * Extensions which transmit websocket binary or text frames which are not application data (typically these
         * are extension-specific control frames starting with four extension-specific control bytes). The extension must consume
         * these frames when they are being received, or transform them into standard websocket control frames, so that prior
         * extensions (in category OTHER) and the application do not see them.
         */
        EMULATION,
        /**
         * Network, meaning something that transforms the network bytes
         */
        NETWORK,
    }

    @Override
    public int compareTo(WebSocketExtensionFactorySpi o) {
        return getOrderCategory().ordinal() - o.getOrderCategory().ordinal();
    }

    @Override
    public String toString() {
        return format("%s, order category: %s", getExtensionName (), getOrderCategory());
    }

}
