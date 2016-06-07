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
package org.kaazing.gateway.transport.sse.bridge.filter;

import java.nio.ByteBuffer;

import org.kaazing.gateway.transport.bridge.MessageBuffer;
import org.kaazing.gateway.transport.sse.bridge.SseMessage;
import org.kaazing.mina.core.buffer.IoBufferEx;

public abstract class SseBuffer extends MessageBuffer<SseMessage> {

    SseBuffer(MessageBuffer<SseMessage> parent, ByteBuffer buf) {
        super(parent, buf);
    }

    SseBuffer(ByteBuffer buf) {
        super(buf);
    }

    static final class SseSharedBuffer extends SseBuffer {

        SseSharedBuffer(ByteBuffer buf) {
            super(buf);
        }

        SseSharedBuffer(MessageBuffer<SseMessage> parent, ByteBuffer buf) {
            super(parent, buf);
        }

        @Override
        public int flags() {
            return IoBufferEx.FLAG_SHARED;
        }

        @Override
        protected SseSharedBuffer asSharedBuffer0() {
            return this;
        }

        @Override
        protected SseUnsharedBuffer asUnsharedBuffer0() {
            return new SseUnsharedBuffer(buf());
        }

        @Override
        protected SseBuffer create0(MessageBuffer<SseMessage> parent, ByteBuffer buf) {
            return new SseSharedBuffer(parent, buf);
        }

    }

    static final class SseUnsharedBuffer extends SseBuffer {

        SseUnsharedBuffer(ByteBuffer buf) {
            super(buf);
        }

        SseUnsharedBuffer(MessageBuffer<SseMessage> parent, ByteBuffer buf) {
            super(parent, buf);
        }

        @Override
        public int flags() {
            return IoBufferEx.FLAG_NONE;
        }

        @Override
        protected SseSharedBuffer asSharedBuffer0() {
            return new SseSharedBuffer(buf());
        }

        @Override
        protected SseUnsharedBuffer asUnsharedBuffer0() {
            return this;
        }

        @Override
        protected SseBuffer create0(MessageBuffer<SseMessage> parent, ByteBuffer buf) {
            return new SseUnsharedBuffer(parent, buf);
        }

    }

}
