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
package org.kaazing.mina.core.filterchain;

import java.util.concurrent.Executor;

import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

import org.kaazing.mina.core.session.AbstractIoSessionEx;

/**
 * Extended version of DefaultIoFilterChain to add support for thread alignment. Every method is
 * executed explicitly on the IoSession's I/O worker thread that is not the current thread.
 */
public class DefaultIoFilterChainEx extends DefaultIoFilterChain {

    private final Thread ioThread;
    private final Executor ioExecutor;

    public DefaultIoFilterChainEx(DefaultIoFilterChainEx filterChain, Thread ioThread, Executor ioExecutor) {
        super(filterChain);
        this.ioThread = ioThread;
        this.ioExecutor = ioExecutor;
    }

    public DefaultIoFilterChainEx(AbstractIoSessionEx session) {
        super(session);
        ioThread = session.getIoThread();
        ioExecutor = session.getIoExecutor();

        // conditionally add alignment checking filter if assert is enabled
        if (AssertAlignedFilter.isAssertEnabled()) {
            addFirst("assert thread aligned", new AssertAlignedFilter(session));
        }
    }

    @Override
    protected final void callNextSessionCreated(final Entry entry, final IoSession session) {
        if (aligned()) {
            super.callNextSessionCreated(entry, session);
        }
        else {
            execute(new Runnable() {
                @Override
                public void run() {
                    // handle re-alignment race using this instead of super
                    DefaultIoFilterChainEx.this.callNextSessionCreated(entry, session);
                }
            });
        }
    }

    @Override
    protected final void callNextSessionOpened(final Entry entry, final IoSession session) {
        if (aligned()) {
            super.callNextSessionOpened(entry, session);
        }
        else {
            execute(new Runnable() {
                @Override
                public void run() {
                    // handle re-alignment race using this instead of super
                    DefaultIoFilterChainEx.this.callNextSessionOpened(entry, session);
                }
            });
        }
    }

    @Override
    protected final void callNextSessionClosed(final Entry entry, final IoSession session) {
        if (aligned()) {
            super.callNextSessionClosed(entry, session);
        }
        else {
            execute(new Runnable() {
                @Override
                public void run() {
                    // handle re-alignment race using this instead of super
                    DefaultIoFilterChainEx.this.callNextSessionClosed(entry, session);
                }
            });
        }
    }

    @Override
    protected final void callNextSessionIdle(final Entry entry, final IoSession session, final IdleStatus status) {
        if (aligned()) {
            super.callNextSessionIdle(entry, session, status);
        }
        else {
            execute(new CallNextSessionIdleCommand(status, entry, session));
        }
    }

    @Override
    protected final void callNextMessageReceived(final Entry entry, final IoSession session, final Object message) {
        if (aligned()) {
            // Note: no suspendRead / resumeRead coordination necessary when thread-aligned
            super.callNextMessageReceived(entry, session, message);
        }
        else {
            // Note: reads will be resumed after completion of scheduled callNextMessageReceived
            session.suspendRead();

            execute(new Runnable() {
                @Override
                public void run() {
                    // handle re-alignment race using this instead of super
                    DefaultIoFilterChainEx.this.callNextMessageReceived(entry, session, message);

                    // Note: reads were suspended before scheduling callNextMessageReceived
                    //       if suspendRead was called during callNextNessageReceived
                    //       then calling resumeRead below will not actually resume reads
                    //       due to internal read suspend counter
                    session.resumeRead();
                }
            });
        }
    }

    @Override
    protected final void callNextMessageSent(
            final Entry entry, final IoSession session, final WriteRequest writeRequest) {
        if (aligned()) {
            super.callNextMessageSent(entry, session, writeRequest);
        }
        else {
            execute(new Runnable() {
                @Override
                public void run() {
                    // handle re-alignment race using this instead of super
                    DefaultIoFilterChainEx.this.callNextMessageSent(entry, session, writeRequest);
                }
            });
        }
    }

    @Override
    protected final void callNextExceptionCaught(final Entry entry, final IoSession session, final Throwable cause) {
        if (aligned()) {
            super.callNextExceptionCaught(entry, session, cause);
        }
        else {
            execute(new Runnable() {
                @Override
                public void run() {
                    // handle re-alignment race using this instead of super
                    DefaultIoFilterChainEx.this.callNextExceptionCaught(entry, session, cause);
                }
            });
        }
    }

    @Override
    protected final void callPreviousFilterWrite(
            final Entry entry, final IoSession session, final WriteRequest writeRequest) {
        if (aligned()) {
            super.callPreviousFilterWrite(entry, session, writeRequest);
        }
        else {
            final Entry entry0 = entry;
            execute(new Runnable() {
                @Override
                public void run() {
                    // handle re-alignment race using this instead of super
                    DefaultIoFilterChainEx.this.callPreviousFilterWrite(entry0, session, writeRequest);
                }
            });
        }
    }

    @Override
    protected final void callPreviousFilterClose(final Entry entry, final IoSession session) {
        if (aligned()) {
            super.callPreviousFilterClose(entry, session);
        }
        else {
            execute(new Runnable() {
                @Override
                public void run() {
                    // handle re-alignment race using this instead of super
                    DefaultIoFilterChainEx.this.callPreviousFilterClose(entry, session);
                }
            });
        }
    }

    private boolean aligned() {
        return Thread.currentThread() == ioThread;
    }

    private void execute(Runnable command) {
        ioExecutor.execute(command);
    }

    // allow detection of legitimate non-IoThread commands
    public final class CallNextSessionIdleCommand implements Runnable {
        private final IdleStatus status;
        private final Entry entry;
        private final IoSession session;

        public CallNextSessionIdleCommand(IdleStatus status, Entry entry,
                IoSession session) {
            this.status = status;
            this.entry = entry;
            this.session = session;
        }

        @Override
        public void run() {
            // handle re-alignment race using this instead of super
            DefaultIoFilterChainEx.this.callNextSessionIdle(entry, session, status);
        }
    }

}
