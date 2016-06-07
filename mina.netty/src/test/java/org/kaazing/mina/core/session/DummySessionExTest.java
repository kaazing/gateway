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
package org.kaazing.mina.core.session;

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
