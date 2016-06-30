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
package org.kaazing.gateway.transport.ws.bridge.extensions.pingpong;

import org.apache.mina.core.filterchain.IoFilter;
import org.kaazing.gateway.transport.ws.extension.ExtensionHeader;
import org.kaazing.gateway.transport.ws.extension.ExtensionHeaderBuilder;
import org.kaazing.gateway.transport.ws.extension.ExtensionHelper;
import org.kaazing.gateway.transport.ws.extension.ExtensionParameterBuilder;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtension;
import org.kaazing.gateway.util.Utils;

public final class PingPongExtension extends WebSocketExtension {
    static final String EXTENSION_TOKEN = "x-kaazing-ping-pong";

    // We want to use valid but infrequently used UTF-8 characteers. ASCII control characters fit the bill!
    static final byte[] CONTROL_BYTES = { (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x02 };

    private final ExtensionHeader extension;

    public PingPongExtension(ExtensionHeader extension, ExtensionHelper extensionHelper) {
        super(extensionHelper);
        this.extension = new ExtensionHeaderBuilder(extension).append(
                new ExtensionParameterBuilder(Utils.toHex(CONTROL_BYTES))).done();
    }

    @Override
    public ExtensionHeader getExtensionHeader() {
        return extension;
    }

    @Override
    public IoFilter getFilter() {
        return new PingPongFilter();
    }

}
