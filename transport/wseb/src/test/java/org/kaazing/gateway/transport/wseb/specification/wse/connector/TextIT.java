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
package org.kaazing.gateway.transport.wseb.specification.wse.connector;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.kaazing.specification.ws.internal.Functions.randomBytesUTF8;
import static org.kaazing.test.util.ITUtil.timeoutRule;

import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer.Kind;
import org.kaazing.gateway.transport.wseb.test.WsebConnectorRule;
import org.kaazing.gateway.util.InternalSystemProperty;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MethodExecutionTrace;

public class TextIT {

    private final WsebConnectorRule connector;

    {
        Properties configuration = new Properties();
        configuration.setProperty(InternalSystemProperty.WS_CLOSE_TIMEOUT.getPropertyName(), "1s");
        connector = new WsebConnectorRule(configuration);
    }

    private JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private TestRule contextRule = ITUtil.toTestRule(context);
    private final TestRule trace = new MethodExecutionTrace();
    private final TestRule timeoutRule = timeoutRule(15, SECONDS);

    private final K3poRule k3po = new K3poRule()
            .setScriptRoot("org/kaazing/specification/wse/data");

    // This latch is needed to ensure messageReceived has fired before each test method exits
    private CountDownLatch received = new CountDownLatch(1);

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(connector).around(k3po).around(contextRule)
            .around(timeoutRule);

    @Ignore("Bug Gateway #254: echo service does not properly handle 0 length data frames")
    @Test
    @Specification("echo.text.payload.length.0/response")
    public void shouldEchoFrameWithPayloadLength0() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        final byte[] bytes = new byte[0];

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(ioBufferMatching(bytes)));
                will(countDown(received));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);
        assertTrue("Connect failed", connectFuture.await(5,  SECONDS));

        IoSessionEx connectSession = (IoSessionEx) connectFuture.getSession();

        IoBufferAllocatorEx<?> allocator = connectSession.getBufferAllocator();
        IoBufferEx buffer = allocator.wrap(ByteBuffer.wrap(bytes));
        ((WsBuffer)buffer).setKind(Kind.TEXT);
        connectSession.write(buffer);

        assertTrue("Echoed data not received", received.await(10, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification("echo.text.payload.length.127/response")
    public void shouldEchoFrameWithPayloadLength127() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        final byte[] bytes = randomBytesUTF8(127);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(ioBufferMatching(bytes)));
                will(countDown(received));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);
        assertTrue("Connect failed", connectFuture.await(5,  SECONDS));

        IoSessionEx connectSession = (IoSessionEx) connectFuture.getSession();

        IoBufferAllocatorEx<?> allocator = connectSession.getBufferAllocator();
        IoBufferEx buffer = allocator.wrap(ByteBuffer.wrap(bytes));
        ((WsBuffer)buffer).setKind(Kind.TEXT);
        connectSession.write(buffer);

        assertTrue("Echoed data not received", received.await(10, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification("echo.text.payload.length.128/response")
    public void shouldEchoFrameWithPayloadLength128() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        final byte[] bytes = randomBytesUTF8(128);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(ioBufferMatching(bytes)));
                will(countDown(received));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);
        assertTrue("Connect failed", connectFuture.await(5,  SECONDS));

        IoSessionEx connectSession = (IoSessionEx) connectFuture.getSession();

        IoBufferAllocatorEx<?> allocator = connectSession.getBufferAllocator();
        IoBufferEx buffer = allocator.wrap(ByteBuffer.wrap(bytes));
        ((WsBuffer)buffer).setKind(Kind.TEXT);
        connectSession.write(buffer);

        assertTrue("Echoed data not received", received.await(10, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification("echo.text.payload.length.65535/response")
    public void shouldEchoFrameWithPayloadLength65535() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        final byte[] bytes = randomBytesUTF8(65535);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(ioBufferMatching(bytes)));
                will(countDown(received));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);
        assertTrue("Connect failed", connectFuture.await(5,  SECONDS));

        IoSessionEx connectSession = (IoSessionEx) connectFuture.getSession();

        IoBufferAllocatorEx<?> allocator = connectSession.getBufferAllocator();
        IoBufferEx buffer = allocator.wrap(ByteBuffer.wrap(bytes));
        ((WsBuffer)buffer).setKind(Kind.TEXT);
        connectSession.write(buffer);

        assertTrue("Echoed data not received", received.await(10, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification("echo.text.payload.length.65536/response")
    public void shouldEchoFrameWithPayloadLength65536() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        final byte[] bytes = randomBytesUTF8(65536);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(ioBufferMatching(bytes)));
                will(countDown(received));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);
        assertTrue("Connect failed", connectFuture.await(5,  SECONDS));

        IoSessionEx connectSession = (IoSessionEx) connectFuture.getSession();

        IoBufferAllocatorEx<?> allocator = connectSession.getBufferAllocator();
        IoBufferEx buffer = allocator.wrap(ByteBuffer.wrap(bytes));
        ((WsBuffer)buffer).setKind(Kind.TEXT);
        connectSession.write(buffer);

        assertTrue("Echoed data not received", received.await(10, SECONDS));

        k3po.finish();
    }

    // Server test only
    @Specification("client.send.text.invalid.utf8/response")
    void clientSendTextInvalidUTF8() throws Exception {
        k3po.finish();
    }

    // Server test only
    @Specification("echo.text.with.fragmented.character/request")
    void shouldEchoTextWithFragmentedCharacter() throws Exception {
        k3po.start();
        Thread.sleep(1000);
        k3po.notifyBarrier("WRITE_SECOND_FRAGMENT");
        k3po.finish();
    }

}
