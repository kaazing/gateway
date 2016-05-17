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
package org.kaazing.gateway.transport.ws;

import java.nio.ByteBuffer;

import org.kaazing.gateway.util.Utils;

/**
 * This represents a close message with optional status (2 byte unsigned integer with a value defined in RFC-6455) and
 * reason message (array of UTF-8 bytes stored as this message's payload using the setBytes method).
 */
public class WsCloseMessage extends WsMessage {
    public static final WsCloseMessage NORMAL_CLOSE = new WsCloseMessage(1000, null);
    public static final WsCloseMessage PROTOCOL_ERROR = new WsCloseMessage(1002, null);
    public static final WsCloseMessage MESSAGE_TOO_LONG_ERROR = new WsCloseMessage(1009, null);
    public static final WsCloseMessage UNEXPECTED_CONDITION = new WsCloseMessage(1011, null);

    private final int status;
    private final ByteBuffer reason;
    
    public WsCloseMessage() {
        this(1005, null);   // 1005 = no status code was actually present on the wire
    }

    public WsCloseMessage(int status, ByteBuffer reason) {
        validateCloseCode(status);
        this.status = status;
        if (reason != null) {
            int size = reason.remaining();
            if (size > 125) {
                throw new IllegalArgumentException("WebSocket close reason size should be <= 125, but it is = "+size);
            }
        }
        this.reason = reason;
    }

    @Override
    public Kind getKind() {
        return WsMessage.Kind.CLOSE;
    }
    
    public ByteBuffer getReason() {
        return reason;
    }
    
    public int getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return String.valueOf(getKind()) + ':' + ' ' +
                "status=" + status + ", reason=" + Utils.asString(reason);
    }

    public static void validateCloseCode(int statusCode) throws IllegalArgumentException {
        if (statusCode < 1000 || (statusCode > 1011 && statusCode <= 2999) || statusCode == 1004) {
            throw new IllegalArgumentException("Invalid close code: " + statusCode);
        }
    }

}
