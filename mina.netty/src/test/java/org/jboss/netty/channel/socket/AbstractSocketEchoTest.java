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
/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.jboss.netty.channel.socket;

import static org.jboss.netty.channel.Channels.future;
import static org.jboss.netty.util.TestUtil.getLocalHost;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.util.internal.ExecutorUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public abstract class AbstractSocketEchoTest {

    private static final Random random = new Random();
    static final byte[] data = new byte[1048576];

    private static ExecutorService executor;


    static {
        random.nextBytes(data);
    }

    @BeforeClass
    public static void init() {
        executor = Executors.newCachedThreadPool();
    }

    @AfterClass
    public static void destroy() {
        ExecutorUtil.terminate(executor);
    }

    protected abstract ChannelFactory newServerSocketChannelFactory(Executor executor);
    protected abstract ChannelFactory newClientSocketChannelFactory(Executor executor);

    @Test(timeout = 5000)
    public void testSimpleEcho() throws Throwable {
        ServerBootstrap sb = new ServerBootstrap(newServerSocketChannelFactory(executor));
        ClientBootstrap cb = new ClientBootstrap(newClientSocketChannelFactory(executor));

        EchoHandler sh = new EchoHandler();
        EchoHandler ch = new EchoHandler();

        try {
        sb.getPipeline().addFirst("handler", sh);
        cb.getPipeline().addFirst("handler", ch);

        Channel sc = sb.bind(new InetSocketAddress(0));
        int port = ((InetSocketAddress) sc.getLocalAddress()).getPort();

        ChannelFuture ccf = cb.connect(new InetSocketAddress(getLocalHost(), port));
        assertTrue(ccf.awaitUninterruptibly().isSuccess());

        Channel cc = ccf.getChannel();
        for (int i = 0; i < data.length;) {
            int length = Math.min(random.nextInt(1024 * 64), data.length - i);
            cc.write(ChannelBuffers.wrappedBuffer(data, i, length));
            i += length;
        }

        while (ch.counter.get() < data.length) {
            if (sh.exception.get() != null) {
                break;
            }
            if (ch.exception.get() != null) {
                break;
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // Ignore.
            }
        }

        while (sh.counter.get() < data.length) {
            if (sh.exception.get() != null) {
                break;
            }
            if (ch.exception.get() != null) {
                break;
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // Ignore.
            }
        }

        sh.channel.close().awaitUninterruptibly();
        ch.channel.close().awaitUninterruptibly();
        sc.close().awaitUninterruptibly();

        ch.expectException(true);
        ChannelFuture connectAfterCloseFuture = cb.connect(new InetSocketAddress(getLocalHost(), port));
        assertTrue(!connectAfterCloseFuture.awaitUninterruptibly().isSuccess());
        assertTrue(ch.caughtExceptionFuture.awaitUninterruptibly().isSuccess());
        ch.expectException(false);
        } finally {
            cb.shutdown();
            sb.shutdown();

            cb.releaseExternalResources();
            sb.releaseExternalResources();
        }

        if (sh.exception.get() != null && !(sh.exception.get() instanceof IOException)) {
            throw sh.exception.get();
        }
        if (ch.exception.get() != null && !(ch.exception.get() instanceof IOException)) {
            throw ch.exception.get();
        }
        if (sh.exception.get() != null) {
            throw sh.exception.get();
        }
        if (ch.exception.get() != null) {
            throw ch.exception.get();
        }
    }

    private static class EchoHandler extends SimpleChannelUpstreamHandler {
        volatile Channel channel;
        final AtomicReference<Throwable> exception = new AtomicReference<>();
        final AtomicInteger counter = new AtomicInteger();
        volatile boolean expectException;
        ChannelFuture caughtExceptionFuture;

        EchoHandler() {
        }

        @Override
        public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
                throws Exception {
            channel = e.getChannel();
        }

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
                throws Exception {
            ChannelBuffer m = (ChannelBuffer) e.getMessage();
            byte[] actual = new byte[m.readableBytes()];
            m.getBytes(0, actual);

            int lastIdx = counter.get();
            for (int i = 0; i < actual.length; i ++) {
                assertEquals(data[i + lastIdx], actual[i]);
            }

            if (channel.getParent() != null) {
                channel.write(m);
            }
            counter.addAndGet(actual.length);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
                throws Exception {
            if (expectException) {
                caughtExceptionFuture.setSuccess();
            } else if (exception.compareAndSet(null, e.getCause())) {
                e.getChannel().close();
            }
        }

        public void expectException(boolean on) {
            expectException = on;
            if (on) {
                caughtExceptionFuture = future(channel);
            }
        }
    }
}
