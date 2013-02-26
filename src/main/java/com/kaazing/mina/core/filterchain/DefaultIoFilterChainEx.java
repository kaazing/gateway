/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.filterchain;

import java.util.concurrent.Executor;

import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

import com.kaazing.mina.core.session.AbstractIoSession;
import com.kaazing.mina.core.session.AbstractIoSessionEx;


/**
 * Extended version of DefaultIoFilterChain to add support for thread alignment. Every method is 
 * executed explicity on the IoSession's I/O worker thread that is not the current thread.
*/   
public class DefaultIoFilterChainEx extends DefaultIoFilterChain  {
    private final Thread affinity;
    private final Executor executor;

    public DefaultIoFilterChainEx(AbstractIoSessionEx session) {
        super(session);
        affinity = session.getIoThread();
        executor = session.getIoExecutor();
        addFirst("assert thread aligned", new AssertAlignedFilter(session));
    }

    @Override
    protected void callNextSessionCreated(Entry entry, IoSession session) {
        // TODO Auto-generated method stub
        super.callNextSessionCreated(entry, session);
    }

    @Override
    protected void callNextSessionOpened(Entry entry, IoSession session) {
        // TODO Auto-generated method stub
        super.callNextSessionOpened(entry, session);
    }

    @Override
    protected void callNextSessionClosed(Entry entry, IoSession session) {
        // TODO Auto-generated method stub
        super.callNextSessionClosed(entry, session);
    }

    @Override
    protected void callNextSessionIdle(Entry entry, IoSession session, IdleStatus status) {
        // TODO Auto-generated method stub
        super.callNextSessionIdle(entry, session, status);
    }

    @Override
    protected void callNextMessageReceived(Entry entry, IoSession session, Object message) {
        // TODO Auto-generated method stub
        super.callNextMessageReceived(entry, session, message);
    }

    @Override
    protected void callNextMessageSent(Entry entry, IoSession session, WriteRequest writeRequest) {
        // TODO Auto-generated method stub
        super.callNextMessageSent(entry, session, writeRequest);
    }

    @Override
    protected void callNextExceptionCaught(Entry entry, IoSession session, Throwable cause) {
        // TODO Auto-generated method stub
        super.callNextExceptionCaught(entry, session, cause);
    }

    @Override
    protected void callPreviousFilterWrite(Entry entry, IoSession session, WriteRequest writeRequest) {
        // TODO Auto-generated method stub
        super.callPreviousFilterWrite(entry, session, writeRequest);
    }

    @Override
    protected void callPreviousFilterClose(Entry entry, IoSession session) {
        // TODO Auto-generated method stub
        super.callPreviousFilterClose(entry, session);
    }
    
    
}
