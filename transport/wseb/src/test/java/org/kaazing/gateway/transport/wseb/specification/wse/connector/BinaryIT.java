/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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

import static org.kaazing.test.util.ITUtil.timeoutRule;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.nio.ByteBuffer;
import java.util.Random;
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
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.gateway.transport.wseb.test.WsebConnectorRule;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MethodExecutionTrace;

public class BinaryIT {
    private final WsebConnectorRule connector = new WsebConnectorRule();

    private JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private TestRule contextRule = ITUtil.toTestRule(context);
    private final TestRule trace = new MethodExecutionTrace();
    private final TestRule timeoutRule = timeoutRule(10, SECONDS);

    private final K3poRule k3po = new K3poRule()
            .setScriptRoot("org/kaazing/specification/wse/data/binary");

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(connector).around(contextRule).around(k3po)
            .around(timeoutRule)            ;

    // This latch is only needed to work around gateway#345
    // TODO: remove this latch and all of its usage in the methods below once that issue is resolved
    private CountDownLatch received = new CountDownLatch(1);

    @Test
    @Ignore("Issue gateway#306: IllegalArgumentException: message is empty. Forgot to call flip")
    @Specification({
        "echo.payload.length.0/response" })
    public void shouldEchoFrameWithPayloadLength0() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        Random random = new Random();
        final byte[] bytes = new byte[0];
        random.nextBytes(bytes);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(ioBufferMatching(bytes)));
                will(countDown(received));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);

        IoSessionEx connectSession = (IoSessionEx) connectFuture.getSession();

        IoBufferAllocatorEx<?> allocator = connectSession.getBufferAllocator();
        IoBufferEx buffer = allocator.wrap(ByteBuffer.wrap(bytes));
        connectSession.write(buffer);

        received.await(10, SECONDS);

        connectSession.close(false).await();


        k3po.finish();
    }

    @Test
    @Specification({
        "echo.payload.length.127/response" })
    public void shouldEchoFrameWithPayloadLength127() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        Random random = new Random();
        final byte[] bytes = new byte[127];
        random.nextBytes(bytes);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(ioBufferMatching(bytes)));
                will(countDown(received));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);

        IoSessionEx connectSession = (IoSessionEx) connectFuture.getSession();

        IoBufferAllocatorEx<?> allocator = connectSession.getBufferAllocator();
        IoBufferEx buffer = allocator.wrap(ByteBuffer.wrap(bytes));
        connectSession.write(buffer);

        received.await(10, SECONDS);

        connectSession.close(false).await();

        k3po.finish();
    }

    @Test
    @Specification({
        "echo.payload.length.128/response" })
    public void shouldEchoFrameWithPayloadLength128() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        Random random = new Random();
        final byte[] bytes = new byte[128];
        random.nextBytes(bytes);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(ioBufferMatching(bytes)));
                will(countDown(received));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);

        IoSessionEx connectSession = (IoSessionEx) connectFuture.getSession();

        IoBufferAllocatorEx<?> allocator = connectSession.getBufferAllocator();
        IoBufferEx buffer = allocator.wrap(ByteBuffer.wrap(bytes));
        connectSession.write(buffer);

        received.await(10, SECONDS);

        connectSession.close(false).await();


        k3po.finish();
    }

    @Test
    @Specification({
        "echo.payload.length.65535/response" })
    public void shouldEchoFrameWithPayloadLength65535() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        Random random = new Random();
        final byte[] bytes = new byte[65535];
        random.nextBytes(bytes);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(ioBufferMatching(bytes)));
                will(countDown(received));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);

        IoSessionEx connectSession = (IoSessionEx) connectFuture.getSession();

        IoBufferAllocatorEx<?> allocator = connectSession.getBufferAllocator();
        IoBufferEx buffer = allocator.wrap(ByteBuffer.wrap(bytes));
        connectSession.write(buffer);

        received.await(10, SECONDS);

        connectSession.close(false).await();


        k3po.finish();
    }

    @Test
    @Specification({
        "echo.payload.length.65536/response" })
    public void shouldEchoFrameWithPayloadLength65536() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        Random random = new Random();
        final byte[] bytes = new byte[65536];
        random.nextBytes(bytes);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(ioBufferMatching(bytes)));
                will(countDown(received));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);

        IoSessionEx connectSession = (IoSessionEx) connectFuture.getSession();

        IoBufferAllocatorEx<?> allocator = connectSession.getBufferAllocator();
        IoBufferEx buffer = allocator.wrap(ByteBuffer.wrap(bytes));
        connectSession.write(buffer);

        received.await(10, SECONDS);

        connectSession.close(false).await();


        k3po.finish();
    }
}