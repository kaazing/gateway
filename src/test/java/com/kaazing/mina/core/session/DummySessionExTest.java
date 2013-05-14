/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.session;

import static java.lang.Thread.currentThread;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.junit.Test;

public class DummySessionExTest {

    private ExecutorService executor = Executors.newFixedThreadPool(1);

    @Test
    public void shouldNotBeReadSuspendedAfterThreadRealignment() throws Exception {

        DummySessionEx session = executor.submit(new DummySessionExFactory()).get();
        IoFilterChain filterChain = session.getFilterChain();
        filterChain.fireMessageReceived(new Object());
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
        assertFalse(session.isReadSuspended());
    }

    @Test
    public void shouldNotBeReadSuspendedWhenThreadAligned() {

        DummySessionEx session = new DummySessionEx();
        IoFilterChain filterChain = session.getFilterChain();
        filterChain.fireMessageReceived(new Object());
        assertFalse(session.isReadSuspended());
    }

    @Test
    public void shouldBeReadSuspendedAfterThreadRealignment() throws Exception {

        DummySessionEx session = executor.submit(new DummySessionExFactory()).get();
        session.setHandler(new IoHandlerAdapter() {

            @Override
            public void messageReceived(IoSession session, Object message)
                    throws Exception {
                session.suspendRead();
            }

        });
        IoFilterChain filterChain = session.getFilterChain();
        filterChain.fireMessageReceived(new Object());
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
        assertTrue(session.isReadSuspended());
    }

    @Test
    public void shouldBeReadSuspendedWhenThreadAligned() {

        DummySessionEx session = new DummySessionEx();
        session.setHandler(new IoHandlerAdapter() {

            @Override
            public void messageReceived(IoSession session, Object message)
                    throws Exception {
                session.suspendRead();
            }

        });
        IoFilterChain filterChain = session.getFilterChain();
        filterChain.fireMessageReceived(new Object());
        assertTrue(session.isReadSuspended());
    }

    private final class DummySessionExFactory implements Callable<DummySessionEx> {
        @Override
        public DummySessionEx call() {
            return new DummySessionEx(currentThread(), executor);
        }
    }

}
