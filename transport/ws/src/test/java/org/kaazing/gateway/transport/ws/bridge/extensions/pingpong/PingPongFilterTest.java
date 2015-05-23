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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.kaazing.gateway.transport.ws.bridge.extensions.pingpong.PingPongExtension.CONTROL_BYTES;
import static org.kaazing.mina.core.buffer.SimpleBufferAllocator.BUFFER_ALLOCATOR;

import java.nio.ByteBuffer;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.transport.ws.WsBinaryMessage;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.WsPingMessage;
import org.kaazing.gateway.transport.ws.WsPongMessage;
import org.kaazing.gateway.transport.ws.WsTextMessage;

public class PingPongFilterTest {
    private static final ByteBuffer BYTES = ByteBuffer.wrap("ABC".getBytes(UTF_8));
    private static final ByteBuffer PAYLOAD_STARTING_WITH_CONTROL_BYTES;
    private static final ByteBuffer INCOMPLETE_CONTROL_BYTES = ByteBuffer.wrap(new byte[]{CONTROL_BYTES[0],
            CONTROL_BYTES[1], CONTROL_BYTES[2]});
    private static final WsTextMessage ESCAPE_MESSAGE = new WsTextMessage(BUFFER_ALLOCATOR.wrap(ByteBuffer
            .wrap(CONTROL_BYTES)));
    private PingPongFilter filter;

    static {
        PAYLOAD_STARTING_WITH_CONTROL_BYTES = ByteBuffer.allocate(CONTROL_BYTES.length + BYTES.remaining());
        PAYLOAD_STARTING_WITH_CONTROL_BYTES.put(CONTROL_BYTES);
        PAYLOAD_STARTING_WITH_CONTROL_BYTES.put(BYTES);
    }

    @Before
    public void before() {
        filter = new PingPongFilter();
    }

    @Test
    public void filterWriteShouldEscapeTextMessageConsistingOfControlBytes() throws Exception {
        Mockery context = new Mockery();
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = context.mock(IoSession.class);
        final WriteRequest writeRequest = context.mock(WriteRequest.class);
        final WsTextMessage message = new WsTextMessage(BUFFER_ALLOCATOR.wrap(ByteBuffer.wrap(CONTROL_BYTES)));

        context.checking(new Expectations() {
            {
                oneOf(writeRequest).getMessage(); will(returnValue(message));
                //oneOf(nextFilter).filterWrite(with(session), with(hasMessageMatching(ESCAPE_MESSAGE)));
                oneOf(nextFilter).filterWrite(session, writeRequest);
            }
        });

        filter.filterWrite(nextFilter, session, writeRequest);
        context.assertIsSatisfied();
    }

    @Test
    public void filterWriteShouldEscapeTextMessageStartingWithControlBytes() throws Exception {
        Mockery context = new Mockery();
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = context.mock(IoSession.class);
        final WriteRequest writeRequest = context.mock(WriteRequest.class);
        final WsTextMessage message = new WsTextMessage(BUFFER_ALLOCATOR.wrap(PAYLOAD_STARTING_WITH_CONTROL_BYTES));

        context.checking(new Expectations() {
            {
                oneOf(writeRequest).getMessage(); will(returnValue(message));
                //oneOf(nextFilter).filterWrite(with(session), with(hasMessageMatching(ESCAPE_MESSAGE)));
                oneOf(nextFilter).filterWrite(session, writeRequest);
            }
        });

        filter.filterWrite(nextFilter, session, writeRequest);
        context.assertIsSatisfied();
    }

    @Test
    public void filterWriteShouldNotEscapeTextMessageContainingIncompleteControlBytes() throws Exception {
        Mockery context = new Mockery();
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = context.mock(IoSession.class);
        final WriteRequest writeRequest = context.mock(WriteRequest.class);
        final WsTextMessage message = new WsTextMessage(BUFFER_ALLOCATOR.wrap(INCOMPLETE_CONTROL_BYTES));

        context.checking(new Expectations() {
            {
                allowing(writeRequest).getMessage(); will(returnValue(message));
                oneOf(nextFilter).filterWrite(session, writeRequest);
            }
        });

        filter.filterWrite(nextFilter, session, writeRequest);
        context.assertIsSatisfied();
    }

    @Test
    public void filterWriteShouldNotEscapeBinaryMessage() throws Exception {
        Mockery context = new Mockery();
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = context.mock(IoSession.class);
        final WriteRequest writeRequest = context.mock(WriteRequest.class);
        final WsBinaryMessage message = new WsBinaryMessage(BUFFER_ALLOCATOR.wrap(PAYLOAD_STARTING_WITH_CONTROL_BYTES));

        context.checking(new Expectations() {
            {
                allowing(writeRequest).getMessage(); will(returnValue(message));
                oneOf(nextFilter).filterWrite(session, writeRequest);
            }
        });

        filter.filterWrite(nextFilter, session, writeRequest);
        context.assertIsSatisfied();
    }

    @Test
    public void filterWriteShouldWriteBinaryMessage() throws Exception {
        Mockery context = new Mockery();
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = context.mock(IoSession.class);
        final WriteRequest writeRequest = context.mock(WriteRequest.class);
        final WsBinaryMessage message = new WsBinaryMessage(BUFFER_ALLOCATOR.wrap(BYTES));

        context.checking(new Expectations() {
            {
                allowing(writeRequest).getMessage(); will(returnValue(message));
                oneOf(nextFilter).filterWrite(session, writeRequest);
            }
        });

        filter.filterWrite(nextFilter, session, writeRequest);
        context.assertIsSatisfied();
    }

    @Test
    public void filterWriteShouldWriteTextMessage() throws Exception {
        Mockery context = new Mockery();
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = context.mock(IoSession.class);
        final WriteRequest writeRequest = context.mock(WriteRequest.class);
        final WsTextMessage message = new WsTextMessage(BUFFER_ALLOCATOR.wrap(BYTES));

        context.checking(new Expectations() {
            {
                allowing(writeRequest).getMessage(); will(returnValue(message));
                oneOf(nextFilter).filterWrite(session, writeRequest);
            }
        });

        filter.filterWrite(nextFilter, session, writeRequest);
        context.assertIsSatisfied();
    }


    @Test
    public void filterWriteShouldWritePing() throws Exception {
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

        filter.filterWrite(nextFilter, session, writeRequest);
        context.assertIsSatisfied();
    }

    @Test
    public void filterWriteShouldWritePong() throws Exception {
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

        filter.filterWrite(nextFilter, session, writeRequest);
        context.assertIsSatisfied();
    }

    @Test
    public void messageReceivedShouldSkipTextMessageConsistingOfControlBytes() throws Exception {
        Mockery context = new Mockery();
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = context.mock(IoSession.class);
        final WsTextMessage message = new WsTextMessage(BUFFER_ALLOCATOR.wrap(ByteBuffer.wrap(CONTROL_BYTES)));

        filter.messageReceived(nextFilter, session, message);
        context.assertIsSatisfied();
    }

    @Test
    public void messageReceivedShouldReceiveBinaryMessageConsistingOfControlBytes() throws Exception {
        Mockery context = new Mockery();
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = context.mock(IoSession.class);
        final WsBinaryMessage message = new WsBinaryMessage(BUFFER_ALLOCATOR.wrap(ByteBuffer.wrap(CONTROL_BYTES)));

        context.checking(new Expectations() {
            {
                oneOf(nextFilter).messageReceived(session, message);
            }
        });

        filter.messageReceived(nextFilter, session, message);
        context.assertIsSatisfied();
    }

    @Test
    public void messageReceivedShouldReceiveBinaryMessage() throws Exception {
        Mockery context = new Mockery();
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = context.mock(IoSession.class);
        final WsBinaryMessage message = new WsBinaryMessage(BUFFER_ALLOCATOR.wrap(BYTES));

        context.checking(new Expectations() {
            {
                oneOf(nextFilter).messageReceived(session, message);
            }
        });

        filter.messageReceived(nextFilter, session, message);
        context.assertIsSatisfied();
    }

    @Test
    public void messageReceivedShouldReceiveTextMessage() throws Exception {
        Mockery context = new Mockery();
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = context.mock(IoSession.class);
        final WsTextMessage message = new WsTextMessage(BUFFER_ALLOCATOR.wrap(BYTES));

        context.checking(new Expectations() {
            {
                oneOf(nextFilter).messageReceived(session, message);
            }
        });

        filter.messageReceived(nextFilter, session, message);
        context.assertIsSatisfied();
    }

    @Test
    public void messageReceivedShouldReceivePing() throws Exception {
        Mockery context = new Mockery();
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = context.mock(IoSession.class);
        final WsMessage message = new WsPingMessage(BUFFER_ALLOCATOR.wrap(ByteBuffer.wrap(new byte[]{0x41})));

        context.checking(new Expectations() {
            {
                oneOf(nextFilter).messageReceived(session, message);
            }
        });

        filter.messageReceived(nextFilter, session, message);
        context.assertIsSatisfied();
    }

    @Test
    public void messageReceivedShouldReceivePong() throws Exception {
        Mockery context = new Mockery();
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = context.mock(IoSession.class);
        final WsMessage message = new WsPongMessage(BUFFER_ALLOCATOR.wrap(ByteBuffer.wrap(new byte[]{0x41})));

        context.checking(new Expectations() {
            {
                oneOf(nextFilter).messageReceived(session, message);
            }
        });

        filter.messageReceived(nextFilter, session, message);
        context.assertIsSatisfied();
    }

}
