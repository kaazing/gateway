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

package org.kaazing.gateway.transport.ws.bridge.extensions.pingpong;

import static org.kaazing.gateway.transport.ws.bridge.extensions.pingpong.PingPongExtension.CONTROL_BYTES;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.transport.ws.WsFilterAdapter;
import org.kaazing.gateway.transport.ws.WsPingMessage;
import org.kaazing.gateway.transport.ws.WsPongMessage;
import org.kaazing.gateway.transport.ws.WsTextMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.core.write.DefaultWriteRequestEx;

/**
 * This filter is used when the x-kaazing-ping-pong extension is active, to set the extension instance on outgoing PING or
 * PONG messages of style client, to cause them to be encoded as extension messages visible to the Kaazing client libraries.
 */
class PingPongFilter extends WsFilterAdapter {
    private static final byte[] EMPTY_PING_BYTES = { (byte)0x09, (byte)0x00 };
    private static final byte[] EMPTY_PONG_BYTES = { (byte)0x0a, (byte)0x00 };

    private static final byte[] EMULATED_PING_FRAME_PAYLOAD = ByteBuffer.allocate(CONTROL_BYTES.length + EMPTY_PING_BYTES.length)
            .put(CONTROL_BYTES).put(EMPTY_PING_BYTES).array();
    private static final byte[] EMULATED_PONG_FRAME_PAYLOAD = ByteBuffer.allocate(CONTROL_BYTES.length + EMPTY_PONG_BYTES.length)
            .put(CONTROL_BYTES).put(EMPTY_PONG_BYTES).array();

    private WsTextMessage emulatedPing;
    private WsTextMessage emulatedPong;
    private WsTextMessage escapeMessage;

    @Override
    public void onPreAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
        IoSessionEx sessionEx = (IoSessionEx) parent.getSession();
        IoBufferAllocatorEx<?> allocator = sessionEx.getBufferAllocator();
        emulatedPing = createTextMessage(allocator, EMULATED_PING_FRAME_PAYLOAD);
        emulatedPong = createTextMessage(allocator, EMULATED_PONG_FRAME_PAYLOAD);
        escapeMessage = createTextMessage(allocator, CONTROL_BYTES);
    }

    @Override
    protected Object doFilterWriteWsPing(NextFilter nextFilter, IoSession session, WriteRequest writeRequest, WsPingMessage wsPing)
            throws Exception {
        return emulatedPing;
    }

    @Override
    protected Object doFilterWriteWsPong(NextFilter nextFilter, IoSession session, WriteRequest writeRequest, WsPongMessage wsPong)
            throws Exception {
        return emulatedPong;
    }

    @Override
    protected Object doFilterWriteWsText(NextFilter nextFilter, IoSession session, WriteRequest writeRequest, WsTextMessage wsText)
            throws Exception {
        IoBufferEx buf = wsText.getBytes();
        if (buf.remaining() < CONTROL_BYTES.length) {
            return wsText;
        }
        int pos = buf.position();
        for (int i=0; i<CONTROL_BYTES.length; i++) {
            if (buf.get(pos+i) != CONTROL_BYTES[i]) {
                return wsText;
            }
        }
        nextFilter.filterWrite(session, new DefaultWriteRequestEx(escapeMessage));
        return wsText;
    }

    @Override
    protected void wsTextReceived(NextFilter nextFilter, IoSession session, WsTextMessage wsText) throws Exception {
        IoBufferEx buf = wsText.getBytes();
        boolean skip = false;
        if (buf.remaining() == CONTROL_BYTES.length) {
            skip = true;
            int pos = buf.position();
            for (int i=0; i<CONTROL_BYTES.length; i++) {
                if (buf.get(pos+i) != CONTROL_BYTES[i]) {
                    skip = false;
                    break;
                }
            }
        }
        if (!skip) {
            super.wsTextReceived(nextFilter, session, wsText);
        }
    }

    private WsTextMessage createTextMessage(IoBufferAllocatorEx<?> allocator, byte[] content) {
        // Use FLAG_SHARED to the same message instance can be written safely multiple times
        ByteBuffer payload = allocator.allocate(content.length, IoBufferEx.FLAG_SHARED);
        int offset = payload.position();
        payload.put(content);
        payload.flip();
        payload.position(offset);
        WsTextMessage result = new WsTextMessage(allocator.wrap(payload, IoBufferEx.FLAG_SHARED));
        return result;
    }

}
