/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.filterchain;

import java.util.concurrent.Executor;

import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

import com.kaazing.mina.core.session.AbstractIoSessionEx;


/**
 * Extended version of DefaultIoFilterChain to add support for thread alignment. Every method is 
 * executed explicity on the IoSession's I/O worker thread that is not the current thread.
*/   
public class DefaultIoFilterChainEx extends DefaultIoFilterChain  {
    private final Thread ioThread;
    private final Executor ioExecutor;

    public DefaultIoFilterChainEx(AbstractIoSessionEx session) {
        super(session);
        ioThread = session.getIoThread();
        ioExecutor = session.getIoExecutor();
        addFirst("assert thread aligned", new AssertAlignedFilter(session));
    }

    @Override
    protected void callNextSessionCreated(final Entry entry, final IoSession session) {
        if (aligned()) {
            super.callNextSessionCreated(entry, session);
        }
        else {
            execute(new Runnable() {
                @Override
                public void run() {
                    DefaultIoFilterChainEx.super.callNextSessionCreated(entry, session);
                }
            });
        }
    }

    @Override
    protected void callNextSessionOpened(final Entry entry, final IoSession session) {
        if (aligned()) {
            super.callNextSessionOpened(entry, session);
        }
        else {
            execute(new Runnable() {
                @Override
                public void run() {
                    DefaultIoFilterChainEx.super.callNextSessionOpened(entry, session);
                }
            });
        }
    }

    @Override
    protected void callNextSessionClosed(final Entry entry, final IoSession session) {
        if (aligned()) {
            super.callNextSessionClosed(entry, session);
        }
        else {
            execute(new Runnable() {
                @Override
                public void run() {
                    DefaultIoFilterChainEx.super.callNextSessionClosed(entry, session);
                }
            });
        }
    }

    @Override
    protected void callNextSessionIdle(final Entry entry, final IoSession session, final IdleStatus status) {
        if (aligned()) {
            super.callNextSessionIdle(entry, session, status);
        }
        else {
            execute(new Runnable() {
                @Override
                public void run() {
                    DefaultIoFilterChainEx.super.callNextSessionIdle(entry, session, status);
                }
            });
        }
    }

    @Override
    protected void callNextMessageReceived(final Entry entry, final IoSession session, final Object message) {
        if (aligned()) {
            super.callNextMessageReceived(entry, session, message);
        }
        else {
            execute(new Runnable() {
                @Override
                public void run() {
                    DefaultIoFilterChainEx.super.callNextMessageReceived(entry, session, message);
                }
            });
        }
    }

    @Override
    protected void callNextMessageSent(final Entry entry, final IoSession session, final WriteRequest writeRequest) {
        if (aligned()) {
            super.callNextMessageSent(entry, session, writeRequest);
        }
        else {
            execute(new Runnable() {
                @Override
                public void run() {
                    DefaultIoFilterChainEx.super.callNextMessageSent(entry, session, writeRequest);
                }
            });
        }
    }

    @Override
    protected void callNextExceptionCaught(final Entry entry, final IoSession session, final Throwable cause) {
        if (aligned()) {
            super.callNextExceptionCaught(entry, session, cause);
        }
        else {
            execute(new Runnable() {
                @Override
                public void run() {
                    DefaultIoFilterChainEx.super.callNextExceptionCaught(entry, session, cause);
                }
            });
        }
    }

    @Override
    protected void callPreviousFilterWrite(final Entry entry, final IoSession session, final WriteRequest writeRequest) {
        if (aligned()) {
            super.callPreviousFilterWrite(entry, session, writeRequest);
        }
        else {
            execute(new Runnable() {
                @Override
                public void run() {
                    DefaultIoFilterChainEx.super.callPreviousFilterWrite(entry, session, writeRequest);
                }
            });
        }
    }

    @Override
    protected void callPreviousFilterClose(final Entry entry, final IoSession session) {
        if (aligned()) {
            super.callPreviousFilterClose(entry, session);
        }
        else {
            execute(new Runnable() {
                @Override
                public void run() {
                    DefaultIoFilterChainEx.super.callPreviousFilterClose(entry, session);
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
    
}
