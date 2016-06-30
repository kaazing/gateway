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
package org.apache.mina.transport.socket.nio;


import static org.kaazing.junit.matchers.JUnitMatchers.instanceOf;
import static org.kaazing.mina.core.buffer.SimpleBufferAllocator.BUFFER_ALLOCATOR;
import static org.kaazing.mina.netty.PortUtil.nextPort;
import static java.lang.String.format;
import static java.lang.System.out;
import static java.nio.ByteBuffer.wrap;
import static org.apache.mina.core.session.IdleStatus.BOTH_IDLE;
import static org.apache.mina.core.session.IdleStatus.READER_IDLE;
import static org.apache.mina.core.session.IdleStatus.WRITER_IDLE;
import static org.jboss.netty.util.CharsetUtil.UTF_8;
import static org.jmock.lib.script.ScriptedAction.perform;
import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.kaazing.mina.core.session.IoSessionEx;

public class NioSocketAcceptorExIT {
    // max expected milliseconds between the call to AbstractIoSession#increaseIdleCount in
    // DefaultIoFilterChain.fireSessionIdle and its call to our test filter's sessionIdle method
    static final long IDLE_TOLERANCE_MILLIS = 30;

    private NioSocketAcceptorEx acceptor;

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    @Before
    public void before() {
        acceptor = new NioSocketAcceptorEx(1);
        // For some reason, NioSocketAcceptorEx does not apply the defaults it has inside its own member for
        // reuseaddress and that is actually the method used at bind.
        acceptor.setReuseAddress(true);
    }

    @After
    public void after() throws Exception {
        acceptor.dispose();
    }

    @Test
    public void shouldConnect() throws Exception {

        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(instanceOf(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(instanceOf(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(instanceOf(IoSessionEx.class)),
                        with(equal(BUFFER_ALLOCATOR.wrap(wrap("text".getBytes(UTF_8))))));
                will(perform("$0.close(false); return;"));
                oneOf(handler).sessionClosed(with(instanceOf(IoSessionEx.class)));
            }
        });

        InetSocketAddress bindAddress = new InetSocketAddress("127.0.0.1", nextPort(2100, 100));

        out.println(format("BindAddress %s", bindAddress));

        acceptor.setHandler(handler);
        acceptor.bind(bindAddress);

        Socket socket = new Socket();
        socket.connect(bindAddress);
        socket.getOutputStream().write("text".getBytes(UTF_8));

        int eos = socket.getInputStream().read();
        assertEquals(-1, eos);

        acceptor.unbind();
    }

    @Test (timeout = 5000)
    public void shouldRepeatedlyIdleTimeoutWhenBothIdle() throws Exception {
        shouldRepeatedlyIdleTimeoutWhenIdle(BOTH_IDLE);
    }

    @Test (timeout = 5000)
    public void shouldRepeatedlyIdleTimeoutWhenReaderIdle() throws Exception {
        shouldRepeatedlyIdleTimeoutWhenIdle(READER_IDLE);
    }

    @Test //(timeout = 5000)
    public void shouldRepeatedlyIdleTimeoutWhenWriterIdle() throws Exception {
        shouldRepeatedlyIdleTimeoutWhenIdle(WRITER_IDLE);
    }

    private void shouldRepeatedlyIdleTimeoutWhenIdle(final IdleStatus statusUnderTest) throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(instanceOf(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(instanceOf(IoSessionEx.class)));
                will(perform("$0.getConfig().setIdleTimeInMillis(status, 50L); return;").where("status", statusUnderTest));
                oneOf(handler).sessionIdle(with(instanceOf(IoSessionEx.class)), with(statusUnderTest));
                oneOf(handler).sessionIdle(with(instanceOf(IoSessionEx.class)), with(statusUnderTest));
                will(perform("$0.close(false); return;"));
                oneOf(handler).sessionClosed(with(instanceOf(IoSessionEx.class)));
            }
        });

        InetSocketAddress bindAddress = new InetSocketAddress("127.0.0.1", nextPort(2100, 100));

        out.println(format("BindAddress %s", bindAddress));

        acceptor.setHandler(handler);
        acceptor.bind(bindAddress);

        Socket socket = new Socket();
        socket.connect(bindAddress);

        int eos = socket.getInputStream().read();
        assertEquals(-1, eos);

        acceptor.unbind();
    }
}
