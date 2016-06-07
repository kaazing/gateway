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
package org.kaazing.gateway.transport.wsn;

import static org.kaazing.mina.core.buffer.SimpleBufferAllocator.BUFFER_ALLOCATOR;

import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;
import org.kaazing.gateway.transport.ws.WsBinaryMessage;
import org.kaazing.gateway.transport.ws.WsCloseMessage;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.util.ws.WebSocketWireProtocol;
import org.slf4j.Logger;

/**
* NOTE: this class is a "classic" unit test for the WsCloseFilter. Overall testing of websocket close
* handling for the wsn transport layer is in test class WsCloseTransportTest.
*/
public class WsCloseFilterTest {

    @Test
    public void filterWriteShouldWriteIfCloseNotSent() throws Exception {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        context.setThreadingPolicy(new Synchroniser());
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = context.mock(IoSession.class);
        final WriteRequest writeRequest = context.mock(WriteRequest.class);
        final Logger logger = context.mock(Logger.class);
        final ScheduledExecutorService scheduler = context.mock(ScheduledExecutorService.class);
        final WsMessage message = new WsBinaryMessage(BUFFER_ALLOCATOR.wrap(ByteBuffer.wrap(new byte[]{0x41})));

        context.checking(new Expectations() {
            {
                oneOf(logger).isTraceEnabled(); will(returnValue(false));
                oneOf(writeRequest).getMessage(); will(returnValue(message));
                oneOf(nextFilter).filterWrite(session, writeRequest);
            }
        });

        Properties configuration = new Properties();
        WsCloseFilter filter = new WsCloseFilter(WebSocketWireProtocol.RFC_6455, configuration, logger, scheduler);
        filter.filterWrite(nextFilter, session, writeRequest);
        context.assertIsSatisfied();
    }

    @Test
    public void filterWriteShouldSwallowMessageIfCloseSent() throws Exception {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        context.setThreadingPolicy(new Synchroniser());
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = context.mock(IoSession.class);
        final WriteRequest closeRequest = context.mock(WriteRequest.class, "closeRequest");
        final WriteRequest writeRequest = context.mock(WriteRequest.class);
        final Logger logger = context.mock(Logger.class);
        final ScheduledExecutorService scheduler = context.mock(ScheduledExecutorService.class);
        final WriteFuture closeFuture = context.mock(WriteFuture.class, "closeFuture");
        final WsMessage close = new WsCloseMessage();

        context.checking(new Expectations() {
            {
                allowing(logger).isTraceEnabled(); will(returnValue(false));
                oneOf(session).isConnected(); will(returnValue(true));
                oneOf(closeRequest).getFuture(); will(returnValue(closeFuture));
                oneOf(closeFuture).addListener(with(any(IoFutureListener.class)));
                exactly(2).of(closeRequest).getMessage(); will(returnValue(close));
                oneOf(nextFilter).filterWrite(session, closeRequest);
                oneOf(scheduler).schedule(with(any(WsCloseFilter.class)), with(any(Long.class)), with(TimeUnit.MILLISECONDS));
            }
        });

        Properties configuration = new Properties();
        WsCloseFilter filter = new WsCloseFilter(WebSocketWireProtocol.RFC_6455, configuration, logger, scheduler);
        filter.filterWrite(nextFilter, session, closeRequest);
        filter.filterWrite(nextFilter, session, writeRequest);
        context.assertIsSatisfied();
    }

    @Test
    public void messageReceivedShouldReceiveIfCloseNotReceived() throws Exception {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        context.setThreadingPolicy(new Synchroniser());
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = context.mock(IoSession.class);
        final Logger logger = context.mock(Logger.class);
        final ScheduledExecutorService scheduler = context.mock(ScheduledExecutorService.class);
        final WsMessage message = new WsBinaryMessage(BUFFER_ALLOCATOR.wrap(ByteBuffer.wrap(new byte[]{0x41})));

        context.checking(new Expectations() {
            {
                oneOf(logger).isTraceEnabled(); will(returnValue(false));
                oneOf(nextFilter).messageReceived(session, message);
            }
        });

        Properties configuration = new Properties();
        WsCloseFilter filter = new WsCloseFilter(WebSocketWireProtocol.RFC_6455, configuration, logger, scheduler);
        filter.messageReceived(nextFilter, session, message);
        context.assertIsSatisfied();
    }

    @Test
    public void messageReceivedShouldSwallowMessageIfCloseReceived() throws Exception {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        context.setThreadingPolicy(new Synchroniser());
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = context.mock(IoSession.class);
        final Logger logger = context.mock(Logger.class);
        final ScheduledExecutorService scheduler = context.mock(ScheduledExecutorService.class);
        final WsMessage close = new WsCloseMessage();
        final WsMessage message = new WsBinaryMessage(BUFFER_ALLOCATOR.wrap(ByteBuffer.wrap(new byte[]{0x41})));

        context.checking(new Expectations() {
            {
                allowing(logger).isTraceEnabled(); will(returnValue(false));
                oneOf(session).isConnected(); will(returnValue(true));
                oneOf(nextFilter).filterWrite(with(session), with(writeRequestWithMessage(close)));
            }

            public Matcher<WriteRequest> writeRequestWithMessage(final Object message) {
                return new BaseMatcher<WriteRequest>() {
                    @Override
                    public boolean matches(Object arg0) {
                        WriteRequest request = (WriteRequest)arg0;
                        return message.equals(request.getMessage());
                    }
                    @Override
                    public void describeTo(Description arg0) {
                        arg0.appendText("write request containing a message equal to " + message);
                    }
                };
            }
        });

        Properties configuration = new Properties();
        WsCloseFilter filter = new WsCloseFilter(WebSocketWireProtocol.RFC_6455, configuration, logger, scheduler);
        filter.messageReceived(nextFilter, session, close);
        filter.messageReceived(nextFilter, session, message);
        context.assertIsSatisfied();
    }

}
