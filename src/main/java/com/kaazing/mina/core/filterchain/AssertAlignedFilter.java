/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.filterchain;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kaazing.mina.core.session.AbstractIoSessionEx;

class AssertAlignedFilter extends IoFilterAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AssertAlignedFilter.class); 
    
    private static final boolean assertsAreEnabled;
    static {
        boolean enabled;
        try {
            assert false;
            enabled = false;
        }
        catch (AssertionError e) {
            enabled = true;
        }
        assertsAreEnabled = enabled;
    }
    
    private final Thread expected;
    
    public AssertAlignedFilter(AbstractIoSessionEx session) {
        expected = session.getIoThread();
    }

    @Override
    public void onPostAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
        if (!assertsAreEnabled) {
            parent.remove(this);
        }
        else {
            checkThread(parent.getSession());
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
        checkThread(session);
        super.filterWrite(nextFilter, session, writeRequest);
    }
    
    public void exceptionCaught(NextFilter nextFilter, IoSession session, Throwable cause) throws Exception {
        //checkThread(session);
        super.exceptionCaught(nextFilter, session, cause);
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
        checkThread(session);
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
        if ( current != expected ) {
            String error = String.format("expected current thread %s to match %s in session %s", current, expected, session);
            RuntimeException e = new RuntimeException(error);
            String caller = e.getStackTrace()[1].toString().replace(this.getClass().getName(), this.getClass().getSimpleName());
            error = String.format("%s: %s", caller , error);
            //System.out.println(error);
            //e.printStackTrace();
            LOGGER.error(error, e);
            throw e;
        }
    }
    
}