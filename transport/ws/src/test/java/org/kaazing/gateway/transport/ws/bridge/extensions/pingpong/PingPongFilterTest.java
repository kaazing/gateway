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
package org.kaazing.gateway.transport.ws.bridge.extensions.pingpong;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.kaazing.gateway.transport.ws.bridge.extensions.pingpong.PingPongExtension.CONTROL_BYTES;
import static org.kaazing.mina.core.buffer.SimpleBufferAllocator.BUFFER_ALLOCATOR;

import java.nio.ByteBuffer;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.gateway.transport.ws.WsBinaryMessage;
import org.kaazing.gateway.transport.ws.WsCloseMessage;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.WsPingMessage;
import org.kaazing.gateway.transport.ws.WsPongMessage;
import org.kaazing.gateway.transport.ws.WsTextMessage;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.core.write.WriteRequestEx;

public class PingPongFilterTest {
    private static final ByteBuffer BYTES = ByteBuffer.wrap("ABC".getBytes(UTF_8));
    private static final ByteBuffer PAYLOAD_STARTING_WITH_CONTROL_BYTES;
    private static final ByteBuffer INCOMPLETE_CONTROL_BYTES = ByteBuffer.wrap(new byte[]{CONTROL_BYTES[0],
            CONTROL_BYTES[1], CONTROL_BYTES[2]});
    private static final WsTextMessage ESCAPE_MESSAGE = new WsTextMessage(BUFFER_ALLOCATOR.wrap(ByteBuffer
            .wrap(CONTROL_BYTES)));
    private static final byte[] EMPTY_PING_BYTES = { (byte)0x09, (byte)0x00 };
    private static final byte[] EMPTY_PONG_BYTES = { (byte)0x0a, (byte)0x00 };
    private static final ByteBuffer EMULATED_PING_BYTES;
    private static final ByteBuffer EMULATED_PONG_BYTES;
    private static final WsTextMessage EMULATED_PING;
    private static final WsTextMessage EMULATED_PONG;
    private PingPongFilter filter;

    static {
        PAYLOAD_STARTING_WITH_CONTROL_BYTES = ByteBuffer.allocate(CONTROL_BYTES.length + BYTES.remaining());
        PAYLOAD_STARTING_WITH_CONTROL_BYTES.put(CONTROL_BYTES);
        PAYLOAD_STARTING_WITH_CONTROL_BYTES.put(BYTES);
        PAYLOAD_STARTING_WITH_CONTROL_BYTES.flip();

        EMULATED_PING_BYTES = ByteBuffer.allocate(CONTROL_BYTES.length + EMPTY_PING_BYTES.length);
        EMULATED_PING_BYTES.put(CONTROL_BYTES);
        EMULATED_PING_BYTES.put(EMPTY_PING_BYTES);
        EMULATED_PING_BYTES.flip();
        EMULATED_PING = new WsTextMessage(BUFFER_ALLOCATOR.wrap(EMULATED_PING_BYTES));

        EMULATED_PONG_BYTES = ByteBuffer.allocate(CONTROL_BYTES.length + EMPTY_PONG_BYTES.length);
        EMULATED_PONG_BYTES.put(CONTROL_BYTES);
        EMULATED_PONG_BYTES.put(EMPTY_PONG_BYTES);
        EMULATED_PONG_BYTES.flip();
        EMULATED_PONG = new WsTextMessage(BUFFER_ALLOCATOR.wrap(EMULATED_PONG_BYTES));
    }

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    private final IoFilterChain filterChain = context.mock(IoFilterChain.class, "filterChain");
    final IoSessionEx session = context.mock(IoSessionEx.class);
    final NextFilter nextFilter = context.mock(NextFilter.class);

    @Before
    public void before() throws Exception {
        filter = new PingPongFilter();
        context.checking(new Expectations() {
            {
                oneOf(filterChain).getSession(); will(returnValue(session));
                allowing(session).getBufferAllocator(); will(returnValue(BUFFER_ALLOCATOR));
            }
        });
        filter.onPreAdd(filterChain, "x-kaazing-ping-ping", nextFilter);
    }

    @Test
    public void shouldWriteEscapeTextMessageConsistingOfControlBytes() throws Exception {
        final WriteRequestEx writeRequest = context.mock(WriteRequestEx.class);
        final WsTextMessage message = new WsTextMessage(BUFFER_ALLOCATOR.wrap(ByteBuffer.wrap(CONTROL_BYTES)));

        context.checking(new Expectations() {
            {
                allowing(writeRequest).getMessage(); will(returnValue(message));
                oneOf(nextFilter).filterWrite(with(session), with(hasMessage(ESCAPE_MESSAGE)));
                oneOf(nextFilter).filterWrite(session, writeRequest);
            }
        });

        filter.filterWrite(nextFilter, session, writeRequest);
        context.assertIsSatisfied();
    }

    @Test
    public void shouldWriteAndEscapeTextMessageStartingWithControlBytes() throws Exception {
        final WriteRequestEx writeRequest = context.mock(WriteRequestEx.class);
        final WsTextMessage message = new WsTextMessage(BUFFER_ALLOCATOR.wrap(PAYLOAD_STARTING_WITH_CONTROL_BYTES));

        context.checking(new Expectations() {
            {
                allowing(writeRequest).getMessage(); will(returnValue(message));
                oneOf(nextFilter).filterWrite(with(session), with(hasMessage(ESCAPE_MESSAGE)));
                oneOf(nextFilter).filterWrite(session, writeRequest);
            }
        });

        filter.filterWrite(nextFilter, session, writeRequest);
        context.assertIsSatisfied();
    }

    @Test
    public void shouldWriteNotEscapeTextMessageContainingIncompleteControlBytes() throws Exception {
        final WriteRequestEx writeRequest = context.mock(WriteRequestEx.class);
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
    public void shouldWriteNotEscapeBinaryMessage() throws Exception {
        final WriteRequestEx writeRequest = context.mock(WriteRequestEx.class);
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
    public void shouldWriteWriteBinaryMessage() throws Exception {
        final WriteRequestEx writeRequest = context.mock(WriteRequestEx.class);
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
    public void shouldWriteWriteTextMessage() throws Exception {
        final WriteRequestEx writeRequest = context.mock(WriteRequestEx.class);
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
    public void shouldWritePingAsEmulatedPing() throws Exception {
        final WriteRequestEx writeRequest = context.mock(WriteRequestEx.class);
        final WsMessage message = new WsPingMessage();
        final WsTextMessage emulatedPing = new WsTextMessage(BUFFER_ALLOCATOR.wrap(EMULATED_PING_BYTES));

        context.checking(new Expectations() {
            {
                allowing(writeRequest).getMessage(); will(returnValue(message));
                oneOf(writeRequest).setMessage(with(emulatedPing));
                oneOf(nextFilter).filterWrite(session, writeRequest);
            }
        });

        filter.filterWrite(nextFilter, session, writeRequest);
        context.assertIsSatisfied();
    }

    @Test
    public void shouldWritePongAsEmulatedPong() throws Exception {
        final WriteRequestEx writeRequest = context.mock(WriteRequestEx.class);
        final WsMessage message = new WsPongMessage();
        final WsTextMessage emulatedPong = new WsTextMessage(BUFFER_ALLOCATOR.wrap(EMULATED_PONG_BYTES));

        context.checking(new Expectations() {
            {
                allowing(writeRequest).getMessage(); will(returnValue(message));
                oneOf(writeRequest).setMessage(with(emulatedPong));
                oneOf(nextFilter).filterWrite(session, writeRequest);
            }
        });

        filter.filterWrite(nextFilter, session, writeRequest);
        context.assertIsSatisfied();
    }

    @Test
    public void shouldReceiveEmulatedPingAsPing() throws Exception {
        final WsPingMessage ping = new WsPingMessage();
        context.checking(new Expectations() {
            {
                oneOf(nextFilter).messageReceived(with(session), with(ping));
            }
        });

        filter.messageReceived(nextFilter, session, EMULATED_PING);
        context.assertIsSatisfied();
    }

    @Test
    public void shouldReceiveEmulatedPongAsPong() throws Exception {
        final WsPongMessage pong = new WsPongMessage();
        context.checking(new Expectations() {
            {
                oneOf(nextFilter).messageReceived(with(session), with(pong));
            }
        });

        filter.messageReceived(nextFilter, session, EMULATED_PONG);
        context.assertIsSatisfied();
    }

    @Test
    public void shouldReceiveEscapedTextMessage() throws Exception {
        final WsTextMessage escape = new WsTextMessage(BUFFER_ALLOCATOR.wrap(ByteBuffer.wrap(CONTROL_BYTES)));
        final WsTextMessage message = new WsTextMessage(BUFFER_ALLOCATOR.wrap(PAYLOAD_STARTING_WITH_CONTROL_BYTES));
        final WsTextMessage normal = new WsTextMessage(BUFFER_ALLOCATOR.wrap(BYTES));

        context.checking(new Expectations() {
            {
                oneOf(nextFilter).messageReceived(session, message);
                oneOf(nextFilter).messageReceived(session, normal);
            }
        });

        filter.messageReceived(nextFilter, session, escape);
        filter.messageReceived(nextFilter, session, message);
        filter.messageReceived(nextFilter, session, normal);
        context.assertIsSatisfied();
    }

    @Test
    public void shouldRejectTextMessageNotStartingWithControlBytesPrecededByEscapeFrame() throws Exception {
        final WsTextMessage escape = new WsTextMessage(BUFFER_ALLOCATOR.wrap(ByteBuffer.wrap(CONTROL_BYTES)));
        final WsTextMessage normal = new WsTextMessage(BUFFER_ALLOCATOR.wrap(BYTES));
        final WsCloseMessage close = WsCloseMessage.PROTOCOL_ERROR;

        context.checking(new Expectations() {
            {
                oneOf(nextFilter).filterWrite(with(session), with(hasMessage(close)));
                oneOf(session).close(true);
            }
        });

        filter.messageReceived(nextFilter, session, escape);
        filter.messageReceived(nextFilter, session, normal);
        context.assertIsSatisfied();
    }

    @Test
    public void shouldRejectUnescapedTextMessageStartingWithControlBytes() throws Exception {
        final WsTextMessage message = new WsTextMessage(BUFFER_ALLOCATOR.wrap(PAYLOAD_STARTING_WITH_CONTROL_BYTES));
        final WsCloseMessage close = WsCloseMessage.PROTOCOL_ERROR;

        context.checking(new Expectations() {
            {
                oneOf(nextFilter).filterWrite(with(session), with(hasMessage(close)));
                oneOf(session).close(true);
            }
        });

        filter.messageReceived(nextFilter, session, message);
        context.assertIsSatisfied();
    }

    @Test
    public void shouldReceiveBinaryMessageConsistingOfControlBytes() throws Exception {
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
    public void shouldReceiveBinaryMessage() throws Exception {
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
    public void shouldReceiveTextMessage() throws Exception {
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
    public void shouldReplyNativePongToNativePing() throws Exception {
        final WsMessage message = new WsPingMessage(BUFFER_ALLOCATOR.wrap(ByteBuffer.wrap(new byte[]{0x41})));
        final WsMessage reply = new WsPongMessage(BUFFER_ALLOCATOR.wrap(ByteBuffer.wrap(new byte[]{0x41})));

        context.checking(new Expectations() {
            {
                oneOf(nextFilter).filterWrite(with(session), with(hasMessage(reply)));
            }
        });

        filter.messageReceived(nextFilter, session, message);
        context.assertIsSatisfied();
    }

    @Test
    public void shouldReceiveAndSwallowNativePong() throws Exception {
        final WsMessage message = new WsPongMessage(BUFFER_ALLOCATOR.wrap(ByteBuffer.wrap(new byte[]{0x41})));

        context.checking(new Expectations() {
            {
            }
        });

        filter.messageReceived(nextFilter, session, message);
        context.assertIsSatisfied();
    }

}
