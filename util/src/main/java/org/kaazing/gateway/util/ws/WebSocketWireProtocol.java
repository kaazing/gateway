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
package org.kaazing.gateway.util.ws;

/**
 * Models the wire protocol versions of the IETF Web Socket Specification,
 * and associate specification version(s) with each known wire protocol.
 * <p/>
 * For example,
 * see <a href="http://tools.ietf.org/html/draft-ietf-hybi-thewebsocketprotocol-17">the latest websocket specification</a>
 * as of writing with specification version 17 and wire protocol version 13.
 * <p/>
 * Note: arbitrary specification version numbers are assigned to Hixie-75 and Hixie-76 versions of the protocol.
 */
public enum WebSocketWireProtocol {
    RFC_6455(13, 14, 15, 16, 17),
    HIXIE_75(false, -5),
    HIXIE_76(false, -4),
    HYBI_0(false, 0),
    HYBI_1(false, 1),
    HYBI_2(false, 2),
    HYBI_3(false, 3),
    HYBI_4(false, 4),
    HYBI_5(5),
    HYBI_6(6),
    HYBI_7(7),
    HYBI_8(8, 9, 10, 11, 12),
    HYBI_13(13, 14, 15, 16, 17);

    private int[] specificationVersions;
    private boolean nativeExtensionsSupported;

    WebSocketWireProtocol(int... specificationVersions) {
        this(true, specificationVersions);
    }

    WebSocketWireProtocol(boolean nativeExtensionsSupported, int... specificationVersions) {
        this.nativeExtensionsSupported = nativeExtensionsSupported;
        this.specificationVersions = specificationVersions;
    }

    public int[] getSpecificationVersions() {
        return specificationVersions;
    }

    public boolean areNativeExtensionsSupported() {
        return nativeExtensionsSupported;
    }

    public static WebSocketWireProtocol valueOf(int specVersion) {
        if (specVersion >= 13 && specVersion <= 17) {
            return RFC_6455;
        }
        for (WebSocketWireProtocol p : WebSocketWireProtocol.values()) {
            for (int specificationVersion : p.getSpecificationVersions()) {
                if (specVersion == specificationVersion) {
                    return p;
                }
            }
        }
        return null;
    }
}

