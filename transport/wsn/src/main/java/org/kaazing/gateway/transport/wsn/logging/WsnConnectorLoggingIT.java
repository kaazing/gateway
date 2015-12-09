/*
 * Copyright 2014, Kaazing Corporation. All rights reserved.
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
package com.kaazing.gateway.server.transport.wsn.logging;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.spi.LoggingEvent;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

import com.kaazing.gateway.server.test.MemoryAppender;
import com.kaazing.gateway.server.test.MethodExecutionTrace;
import com.kaazing.gateway.server.test.WsnConnectorRule;
import com.kaazing.gateway.server.transport.ws.bridge.filter.WsBuffer;
import com.kaazing.gateway.server.transport.wsn.WsnProtocol;
import com.kaazing.gateway.server.transport.wsn.WsnSession;
import com.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import com.kaazing.mina.core.buffer.IoBufferEx;
import com.kaazing.mina.core.session.IoSessionEx;

// This is a subset of BaseFramingIT (connector version) used to verify wsn transport level logging
public class WsnConnectorLoggingIT {
    private final WsnConnectorRule connector = new WsnConnectorRule();
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/framing");
    private final TestRule timeoutRule = new DisableOnDebug(Timeout.builder().withTimeout(10, SECONDS)
                .withLookingForStuckThread(true).build());
    private final TestRule trace = new MethodExecutionTrace();

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(connector).around(k3po).around(timeoutRule);

    private static String TEXT_FILTER_NAME = WsnProtocol.NAME + "#text";

    private Mockery context;

    @Before
    public void initialize() {
        context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());
    }



    @Test
    @Specification({
        "echo.binary.payload.length.125/handshake.response.and.frame"
        })
    public void shouldLogOpenWriteReceivedAndClose() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                allowing(handler).messageReceived(with(any(IoSessionEx.class)), with(any(Object.class)));
                allowing(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        WsnSession wsnConnectSession = (WsnSession) connectFuture.getSession();
        // ### Issue# 316: Temporary hack till the issue related to Connector writing out TEXT frame
        //                  instead of BINARY is resolved.
        if (wsnConnectSession != null) {
            IoFilterChain parentFilterChain = wsnConnectSession.getParent().getFilterChain();
            if (parentFilterChain.contains(TEXT_FILTER_NAME)) {
                parentFilterChain.remove(TEXT_FILTER_NAME);
            }
        }

        Random random = new Random();
        byte[] bytes = new byte[125];
        random.nextBytes(bytes);

        IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
        WsBuffer wsBuffer = allocator.wrap(ByteBuffer.wrap(bytes), IoBufferEx.FLAG_SHARED);
        wsBuffer.setKind(WsBuffer.Kind.BINARY);
        wsnConnectSession.write(wsBuffer);

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();

        for (LoggingEvent event : MemoryAppender.getEvents()) {
            String message = event.getMessage().toString();
            if (message.contains("#")) {
                System.out.println(message);
            }
        }
    }

    private static class RandomString {

        private static final char[] SYMBOLS;

        static {
            StringBuilder tmp = new StringBuilder();
            for (char ch = 32; ch <= 126; ++ch) {
                tmp.append(ch);
            }
            SYMBOLS = tmp.toString().toCharArray();
        }

        private final Random random = new Random();

        private final char[] buf;

        public RandomString(int length) {
            if (length < 1) {
                throw new IllegalArgumentException("length < 1: " + length);
            }
            buf = new char[length];
        }

        public String nextString() {
            for (int idx = 0; idx < buf.length; ++idx) {
                buf[idx] = SYMBOLS[random.nextInt(SYMBOLS.length)];
            }

            return new String(buf);
        }
    }
}
