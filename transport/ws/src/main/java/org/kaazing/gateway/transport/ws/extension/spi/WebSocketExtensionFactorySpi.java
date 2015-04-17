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

import java.io.IOException;


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

    /**
     * Creates a {@link WebSocketExtensionSpi} instance. The specified HTTP request header formatted string is validated as well.
     * An IOException is thrown if the specified string is deemed invalid. If an IOException is thrown, the extension is not
     * negotiated during the opening handshake and does not have it's hooks exercised when messages are being received or sent.
     * The format for extensionWithParams is as shown below:
     *
     * {@code}
     *      extension-name[;param1=value1;param2;param3=value3]
     * {@code}
     *
     * @param extensionWithParams  String representation of the extension in request header format
     * @return WebSocketExtensionSpi  instance
     * @throw IOException if the extension considers the specified string is invalid
     */
    public abstract WebSocketExtensionSpi createExtension(String extensionWithParams) throws IOException;
}
