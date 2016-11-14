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
package org.kaazing.gateway.service.proxy;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Rule;
import org.junit.Test;

public class DeferredConnectStrategyFilterTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery() { {
        setThreadingPolicy(new Synchroniser());
    } };

    @Test
    public void shouldNotDeferSessionCreated() throws Exception {

        DummySession session = new DummySession();
        final IoFilterChain filterChain = session.getFilterChain();
        filterChain.addLast("first", new DeferredConnectStrategyFilter());

        IoHandler handler = context.mock(IoHandler.class);
        session.setHandler(handler);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(session);
            }
        });

        filterChain.fireSessionCreated();
    }

    @Test
    public void shouldDeferSessionOpened() throws Exception {

        DummySession session = new DummySession();
        final IoFilterChain filterChain = session.getFilterChain();
        filterChain.addLast("first", new DeferredConnectStrategyFilter());

        IoHandler handler = context.mock(IoHandler.class);
        session.setHandler(handler);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(session);
            }
        });

        filterChain.fireSessionCreated();
        filterChain.fireSessionOpened();
    }

    @Test
    public void shouldFlushSessionOpenedBeforeMessageReceived() throws Exception {

        DummySession session = new DummySession();
        final IoFilterChain filterChain = session.getFilterChain();
        filterChain.addLast("first", new DeferredConnectStrategyFilter());

        IoHandler handler = context.mock(IoHandler.class);
        session.setHandler(handler);

        Object message = new Object();

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(session);
                oneOf(handler).sessionOpened(session);
                oneOf(handler).messageReceived(session, message);
            }
        });

        filterChain.fireSessionCreated();
        filterChain.fireSessionOpened();
        filterChain.fireMessageReceived(message);
    }

    @Test
    public void shouldFlushSessionOpenedBeforeMessageSent() throws Exception {

        DummySession session = new DummySession();
        final IoFilterChain filterChain = session.getFilterChain();
        filterChain.addLast("first", new DeferredConnectStrategyFilter());

        IoHandler handler = context.mock(IoHandler.class);

        Object message = new Object();
        WriteRequest request = new DefaultWriteRequest(message);

        session.setHandler(handler);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(session);
                oneOf(handler).sessionOpened(session);
                oneOf(handler).messageSent(session, message);
            }
        });

        filterChain.fireSessionCreated();
        filterChain.fireSessionOpened();
        filterChain.fireMessageSent(request);
    }

    @Test
    public void shouldFlushSessionOpenedBeforeSessionIdle() throws Exception {

        DummySession session = new DummySession();
        final IoFilterChain filterChain = session.getFilterChain();
        filterChain.addLast("first", new DeferredConnectStrategyFilter());

        IoHandler handler = context.mock(IoHandler.class);
        session.setHandler(handler);

        IdleStatus status = IdleStatus.BOTH_IDLE;

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(session);
                oneOf(handler).sessionOpened(session);
                oneOf(handler).sessionIdle(session, status);
            }
        });

        filterChain.fireSessionCreated();
        filterChain.fireSessionOpened();
        filterChain.fireSessionIdle(status);
    }

    @Test
    public void shouldFlushSessionOpenedBeforeSessionClosed() throws Exception {

        DummySession session = new DummySession();
        final IoFilterChain filterChain = session.getFilterChain();
        filterChain.addLast("first", new DeferredConnectStrategyFilter());

        IoHandler handler = context.mock(IoHandler.class);
        session.setHandler(handler);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(session);
                oneOf(handler).sessionOpened(session);
                oneOf(handler).sessionClosed(session);
            }
        });

        filterChain.fireSessionCreated();
        filterChain.fireSessionOpened();
        filterChain.fireSessionClosed();
    }

    @Test
    public void shouldFlushSessionOpenedBeforeExceptionCaught() throws Exception {

        DummySession session = new DummySession();
        final IoFilterChain filterChain = session.getFilterChain();
        filterChain.addLast("first", new DeferredConnectStrategyFilter());

        IoHandler handler = context.mock(IoHandler.class);
        session.setHandler(handler);

        Exception cause = new Exception();

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(session);
                oneOf(handler).sessionOpened(session);
                oneOf(handler).exceptionCaught(session, cause);
            }
        });

        filterChain.fireSessionCreated();
        filterChain.fireSessionOpened();
        filterChain.fireExceptionCaught(cause);
    }
}
