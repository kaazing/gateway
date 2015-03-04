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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.kaazing.gateway.transport.ws.AbstractWsControlMessage.Style.CLIENT;
import static org.kaazing.mina.core.buffer.SimpleBufferAllocator.BUFFER_ALLOCATOR;

import java.nio.ByteBuffer;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;
import org.kaazing.gateway.transport.ws.WsBinaryMessage;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.WsPingMessage;
import org.kaazing.gateway.transport.ws.WsPongMessage;

/**
* NOTE: this class is a "classic" unit test for the WsCloseFilter. Overall testing of websocket close
* handling for the wsn transport layer is in test class WsCloseTransportTest.
*/
public class PingPongFilterTest {

    @Test
    public void filterWriteShouldSetExtensionOnClientPing() throws Exception {
        Mockery context = new Mockery();
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = context.mock(IoSession.class);
        final WriteRequest writeRequest = context.mock(WriteRequest.class);
        final WsPingMessage message = new WsPingMessage(BUFFER_ALLOCATOR.wrap(ByteBuffer.wrap(new byte[]{0x41})));
        message.setStyle(CLIENT);

        context.checking(new Expectations() {
            {
                allowing(writeRequest).getMessage(); will(returnValue(message));
                oneOf(nextFilter).filterWrite(session, writeRequest);
            }
        });

        PingPongFilter filter = PingPongFilter.getInstance();
        filter.filterWrite(nextFilter, session, writeRequest);
        context.assertIsSatisfied();
        assertNotNull(message.getExtension());
    }

    @Test
    public void filterWriteShouldSetExtensionOnClientPong() throws Exception {
        Mockery context = new Mockery();
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = context.mock(IoSession.class);
        final WriteRequest writeRequest = context.mock(WriteRequest.class);
        final WsPongMessage message = new WsPongMessage(BUFFER_ALLOCATOR.wrap(ByteBuffer.wrap(new byte[]{0x41})));
        message.setStyle(CLIENT);

        context.checking(new Expectations() {
            {
                allowing(writeRequest).getMessage(); will(returnValue(message));
                oneOf(nextFilter).filterWrite(session, writeRequest);
            }
        });

        PingPongFilter filter = PingPongFilter.getInstance();
        filter.filterWrite(nextFilter, session, writeRequest);
        context.assertIsSatisfied();
        assertNotNull(message.getExtension());
    }

    @Test
    public void filterWriteShouldWriteNonPingOrPong() throws Exception {
        Mockery context = new Mockery();
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = context.mock(IoSession.class);
        final WriteRequest writeRequest = context.mock(WriteRequest.class);
        final WsMessage message = new WsBinaryMessage(BUFFER_ALLOCATOR.wrap(ByteBuffer.wrap(new byte[]{0x41})));

        context.checking(new Expectations() {
            {
                oneOf(writeRequest).getMessage(); will(returnValue(message));
                oneOf(nextFilter).filterWrite(session, writeRequest);
            }
        });

        PingPongFilter filter = PingPongFilter.getInstance();
        filter.filterWrite(nextFilter, session, writeRequest);
        context.assertIsSatisfied();
    }

    @Test
    public void filterWriteShouldNotChangeServerPing() throws Exception {
        Mockery context = new Mockery();
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = context.mock(IoSession.class);
        final WriteRequest writeRequest = context.mock(WriteRequest.class);
        final WsMessage message = new WsPingMessage(BUFFER_ALLOCATOR.wrap(ByteBuffer.wrap(new byte[]{0x41})));

        context.checking(new Expectations() {
            {
                allowing(writeRequest).getMessage(); will(returnValue(message));
                oneOf(nextFilter).filterWrite(session, writeRequest);
            }
        });

        PingPongFilter filter = PingPongFilter.getInstance();
        filter.filterWrite(nextFilter, session, writeRequest);
        context.assertIsSatisfied();
        assertNull(message.getExtension());
    }

    @Test
    public void filterWriteShouldNotChangeServerPong() throws Exception {
        Mockery context = new Mockery();
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = context.mock(IoSession.class);
        final WriteRequest writeRequest = context.mock(WriteRequest.class);
        final WsMessage message = new WsPongMessage(BUFFER_ALLOCATOR.wrap(ByteBuffer.wrap(new byte[]{0x41})));

        context.checking(new Expectations() {
            {
                allowing(writeRequest).getMessage(); will(returnValue(message));
                oneOf(nextFilter).filterWrite(session, writeRequest);
            }
        });

        PingPongFilter filter = PingPongFilter.getInstance();
        filter.filterWrite(nextFilter, session, writeRequest);
        context.assertIsSatisfied();
        assertNull(message.getExtension());
    }
    
}
