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

import org.kaazing.gateway.transport.bridge.Message;
import org.kaazing.gateway.util.Utils;
import org.kaazing.mina.core.buffer.IoBufferEx;

public abstract class WsMessage extends Message {

    public enum Kind {
        BINARY, TEXT, CONTINUATION, CLOSE, COMMAND, PING, PONG
    }

    private final boolean fin;

    public abstract Kind getKind();

    private IoBufferEx buf;

    public WsMessage() {
        this(true);
    }

    public WsMessage(boolean fin) {
        this.fin = fin;
    }

    public IoBufferEx getBytes() {
        return buf;
    }

    public void setBytes(IoBufferEx buf) {
        this.buf = buf;
    }

    public boolean isFin() {
        return fin;
    }

    @Override
    public int hashCode() {
        return buf.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof WsMessage)) {
            return false;
        }

        WsMessage that = (WsMessage) obj;
        return (that.getKind() == this.getKind() && that.fin == this.fin &&
                Utils.sameOrEquals(this.buf == null ? null : this.buf.buf(),
                        that.buf == null ? null : that.buf.buf())); // IoBufferEx has no equals method
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getKind());
        if (buf != null) {
            builder.append(':');
            builder.append(' ');
            // KG-2648: getHexDump is not safe for simultaneous use in multiple threads (mutates the ByteBuffer)
            builder.append(buf.duplicate().getHexDump());
        }
        return builder.toString();
    }
}
