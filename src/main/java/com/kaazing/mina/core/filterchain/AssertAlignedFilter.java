/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.filterchain;

import static com.kaazing.mina.core.util.Util.verifyInIoThread;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

import com.kaazing.mina.core.session.AbstractIoSessionEx;

class AssertAlignedFilter extends IoFilterAdapter {
    
    private static final boolean assertEnabled;
    static {
        boolean enabled;
        try {
            assert false;
            enabled = false;
        }
        catch (AssertionError e) {
            enabled = true;
        }
        assertEnabled = enabled;
    }
    
    static final boolean isAssertEnabled() {
    	return assertEnabled;
    }
    
    private final Thread expected;
    
    public AssertAlignedFilter(AbstractIoSessionEx session) {
        expected = session.getIoThread();
    }

    @Override
    public void onPostAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
        if (!assertEnabled) {
            parent.remove(this);
        }
        else {
        	verifyInIoThread(parent.getSession(), expected);
        }
    }

    @Override
    public void onPostRemove(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
    }
    
    @Override
    public void onPreAdd(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
    }

    @Override
    public void onPreRemove(IoFilterChain parent, String name,
            NextFilter nextFilter) throws Exception {
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        verifyInIoThread(session, expected);
        super.filterWrite(nextFilter, session, writeRequest);
    }
    
    public void exceptionCaught(NextFilter nextFilter, IoSession session, Throwable cause) throws Exception {
        //verifyInIoThread(session, expected);
        super.exceptionCaught(nextFilter, session, cause);
    }

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        verifyInIoThread(session, expected);
        super.messageReceived(nextFilter, session, message);
    }

    @Override
    public void messageSent(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {
        verifyInIoThread(session, expected);
        super.messageSent(nextFilter, session, writeRequest);
    }

    @Override
    public void sessionClosed(NextFilter nextFilter, IoSession session)
            throws Exception {
        // This is called by NioSocketAcceptor during gateway shutdown so thread is not expected to match
        verifyInIoThread(session, expected);
        super.sessionClosed(nextFilter, session);
    }

    @Override
    public void sessionCreated(NextFilter nextFilter, IoSession session)
            throws Exception {
        verifyInIoThread(session, expected);
        super.sessionCreated(nextFilter, session);
    }

    @Override
    public void sessionIdle(NextFilter nextFilter, IoSession session, IdleStatus status) throws Exception {
        verifyInIoThread(session, expected);
        super.sessionIdle(nextFilter, session, status);
    }

    @Override
    public void sessionOpened(NextFilter nextFilter, IoSession session)
            throws Exception {
        verifyInIoThread(session, expected);
        super.sessionOpened(nextFilter, session);
    }
    
}