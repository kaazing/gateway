/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.transport.wsr.bridge.filter;

import java.nio.ByteBuffer;

import org.kaazing.gateway.transport.bridge.MessageBuffer;
import org.kaazing.gateway.transport.wsr.RtmpBinaryDataMessage;
import org.kaazing.mina.core.buffer.IoBufferEx;

public abstract class WsrBuffer extends MessageBuffer<RtmpBinaryDataMessage> {

    WsrBuffer(MessageBuffer<RtmpBinaryDataMessage> parent, ByteBuffer buf) {
        super(parent, buf);
    }

    WsrBuffer(ByteBuffer buf) {
        super(buf);
    }

    static final class WsrSharedBuffer extends WsrBuffer {

        WsrSharedBuffer(ByteBuffer buf) {
            super(buf);
        }

        WsrSharedBuffer(MessageBuffer<RtmpBinaryDataMessage> parent, ByteBuffer buf) {
            super(parent, buf);
        }

        @Override
        public int flags() {
            return IoBufferEx.FLAG_SHARED;
        }

        @Override
        protected WsrSharedBuffer asSharedBuffer0() {
            return this;
        }

        @Override
        protected WsrUnsharedBuffer asUnsharedBuffer0() {
            return new WsrUnsharedBuffer(buf());
        }

        @Override
        protected WsrBuffer create0(MessageBuffer<RtmpBinaryDataMessage> parent, ByteBuffer buf) {
            return new WsrSharedBuffer(parent, buf);
        }

    }

    static final class WsrUnsharedBuffer extends WsrBuffer {

        WsrUnsharedBuffer(ByteBuffer buf) {
            super(buf);
        }

        WsrUnsharedBuffer(MessageBuffer<RtmpBinaryDataMessage> parent, ByteBuffer buf) {
            super(parent, buf);
        }

        @Override
        public int flags() {
            return IoBufferEx.FLAG_NONE;
        }

        @Override
        protected WsrSharedBuffer asSharedBuffer0() {
            return new WsrSharedBuffer(buf());
        }

        @Override
        protected WsrUnsharedBuffer asUnsharedBuffer0() {
            return this;
        }

        @Override
        protected WsrBuffer create0(MessageBuffer<RtmpBinaryDataMessage> parent, ByteBuffer buf) {
            return new WsrUnsharedBuffer(parent, buf);
        }

    }

}
