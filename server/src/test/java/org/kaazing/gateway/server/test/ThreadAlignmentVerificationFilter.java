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
package org.kaazing.gateway.server.test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

/**
 * Filter to validate thread alignment on any filter pipeline
 */
public class ThreadAlignmentVerificationFilter extends IoFilterAdapter {

    private AtomicInteger alignmentSuccesses = new AtomicInteger(0);
    private AtomicInteger alignmentViolations = new AtomicInteger(0);
    private AtomicReference<Thread> expectedThread = new AtomicReference<>(null);

    /**
     * Verify all operations occur on the specified thread
     */
    public ThreadAlignmentVerificationFilter(Thread expectedExecutionThread) {
        expectedThread.set(expectedExecutionThread);
    }

    /**
     * Verify all operations occur on the same thread
     */
    public ThreadAlignmentVerificationFilter() {
    }

    public int getAlignmentSuccesses() {
        return alignmentSuccesses.get();
    }

    public int getAlignmentViolations() {
        return alignmentViolations.get();
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        checkThread(session);
        super.filterWrite(nextFilter, session, writeRequest);
    }

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        checkThread(session);
        super.messageReceived(nextFilter, session, message);
    }

    @Override
    public void messageSent(NextFilter nextFilter, IoSession session,
                            WriteRequest writeRequest) throws Exception {
        checkThread(session);
        super.messageSent(nextFilter, session, writeRequest);
    }

    @Override
    public void sessionClosed(NextFilter nextFilter, IoSession session)
            throws Exception {
        // This is called by NioSocketAcceptor during gateway shutdown so thread is not expected to match
        super.sessionClosed(nextFilter, session);
    }

    @Override
    public void sessionCreated(NextFilter nextFilter, IoSession session)
            throws Exception {
        checkThread(session);
        super.sessionCreated(nextFilter, session);
    }

    @Override
    public void sessionIdle(NextFilter nextFilter, IoSession session, IdleStatus status) throws Exception {
        checkThread(session);
        super.sessionIdle(nextFilter, session, status);
    }

    @Override
    public void sessionOpened(NextFilter nextFilter, IoSession session)
            throws Exception {
        checkThread(session);
        super.sessionOpened(nextFilter, session);
    }

    private void checkThread(IoSession session) {
        Thread current = Thread.currentThread();
        Thread expected = expectedThread.get();
        if (expected == null) {
            expectedThread.compareAndSet(null, current);
            expected = expectedThread.get();
//            RuntimeException e = new RuntimeException();
//            String caller = e.getStackTrace()[1].toString().replace(this.getClass().getName(), this.getClass().getSimpleName());
//            System.out.println(String.format(
//                    "%s: setting expected thread to current thread %s (session %s)", caller, current, session));
        }
        if (current != expected) {
            alignmentViolations.incrementAndGet();
            String error =
                    String.format("expected current thread %s to match %s in session %s", current, expectedThread, session);
            RuntimeException e = new RuntimeException(error);
            String caller = e.getStackTrace()[1].toString().replace(this.getClass().getName(), this.getClass().getSimpleName());
            error = error + " in " + caller;
            System.out.println(error);
            e.printStackTrace();
        } else {
            alignmentSuccesses.incrementAndGet();
        }
    }
}

