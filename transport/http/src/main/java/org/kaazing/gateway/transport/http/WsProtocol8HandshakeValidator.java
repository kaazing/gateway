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
package org.kaazing.gateway.transport.http;

import org.apache.mina.util.Base64;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;

public class WsProtocol8HandshakeValidator extends WsHandshakeValidator {


    @Override
    protected boolean doValidate(HttpRequestMessage request, final boolean isPostMethodAllowed) {
        boolean ok = super.doValidate(request, isPostMethodAllowed);
        if ( !ok ) return false;

        ok = requireHeader(request, SEC_WEB_SOCKET_VERSION, getExpectedWebSocketVersion());
        if ( !ok ) return false;

        ok = requireHeader(request, SEC_WEB_SOCKET_KEY);
        return ok && validateWebSocketKey(request.getHeader(SEC_WEB_SOCKET_KEY));

        // all other handshake elements are optional and not verifiable.
    }

    protected String getExpectedWebSocketVersion() {
        return "8";
    }

    public boolean validateWebSocketKey(String key) {
        if ( key == null ) {
            return false;
        }
        byte[] bytes = key.getBytes(UTF_8);
        byte[] decodedKey = Base64.decodeBase64(bytes);
        return decodedKey.length == 16;

    }


}

