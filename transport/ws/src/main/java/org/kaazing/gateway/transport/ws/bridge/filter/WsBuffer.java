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
package org.kaazing.gateway.transport.ws.bridge.filter;

import java.nio.ByteBuffer;

import org.kaazing.gateway.transport.bridge.MessageBuffer;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.mina.core.buffer.IoBufferEx;

public abstract class WsBuffer extends MessageBuffer<WsMessage> {

    private boolean fin = true;

    public enum Kind {
        BINARY, TEXT, CONTINUATION, PING, PONG
    }

    private Kind kind = Kind.BINARY;

    WsBuffer(MessageBuffer<WsMessage> parent, ByteBuffer buf) {
        super(parent, buf);
    }

    WsBuffer(ByteBuffer buf) {
        super(buf);
    }

    @Override
    protected final MessageBuffer<WsMessage> create0(MessageBuffer<WsMessage> parent, ByteBuffer buf) {
        WsBuffer wsParent = (WsBuffer)parent;
        WsBuffer wsBuffer = create1(parent, buf);
        wsBuffer.setKind(wsParent.getKind());
        return wsBuffer;
    }

    protected abstract WsBuffer create1(MessageBuffer<WsMessage> parent, ByteBuffer buf);

    public final void setKind(Kind kind) {
        if (kind == null) {
            throw new NullPointerException("kind");
        }

        this.kind = kind;
    }

    public void setFin(boolean fin) {
        this.fin = fin;
    }

    public boolean isFin() {
        return fin;
    }

    public final Kind getKind() {
        return kind;
    }

    public static final class WsSharedBuffer extends WsBuffer {

        public WsSharedBuffer(ByteBuffer buf) {
            super(buf);
        }

        WsSharedBuffer(MessageBuffer<WsMessage> parent, ByteBuffer buf) {
            super(parent, buf);
        }

        @Override
        public int flags() {
            return IoBufferEx.FLAG_SHARED;
        }

        @Override
        protected WsSharedBuffer asSharedBuffer0() {
            return this;
        }

        @Override
        protected WsUnsharedBuffer asUnsharedBuffer0() {
            return new WsUnsharedBuffer(buf());
        }

        @Override
        protected WsBuffer create1(MessageBuffer<WsMessage> parent, ByteBuffer buf) {
            return new WsSharedBuffer(parent, buf);
        }

    }

    public static final class WsUnsharedBuffer extends WsBuffer {

        public WsUnsharedBuffer(ByteBuffer buf) {
            super(buf);
        }

        WsUnsharedBuffer(MessageBuffer<WsMessage> parent, ByteBuffer buf) {
            super(parent, buf);
        }

        @Override
        public int flags() {
            return IoBufferEx.FLAG_NONE;
        }

        @Override
        protected WsSharedBuffer asSharedBuffer0() {
            return new WsSharedBuffer(buf());
        }

        @Override
        protected WsUnsharedBuffer asUnsharedBuffer0() {
            return this;
        }

        @Override
        protected WsBuffer create1(MessageBuffer<WsMessage> parent, ByteBuffer buf) {
            return new WsUnsharedBuffer(parent, buf);
        }

    }
}
