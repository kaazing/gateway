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

import static org.kaazing.mina.core.util.Util.verifyInIoThread;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

import org.kaazing.mina.core.session.AbstractIoSessionEx;
import org.kaazing.mina.core.session.IoSessionEx;

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

    static boolean isAssertEnabled() {
        return assertEnabled;
    }

    private final IoSessionEx sessionEx;

    public AssertAlignedFilter(AbstractIoSessionEx session) {
        this.sessionEx = session;
    }

    @Override
    public void onPostAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
        if (!assertEnabled) {
            parent.remove(this);
        }
        else {
            verifyInIoThread(parent.getSession(), sessionEx.getIoThread());
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
        verifyInIoThread(session, sessionEx.getIoThread());
        super.filterWrite(nextFilter, session, writeRequest);
    }

    @Override
    public void exceptionCaught(NextFilter nextFilter, IoSession session, Throwable cause) throws Exception {
        //verifyInIoThread(session, sessionEx.getIoThread());
        super.exceptionCaught(nextFilter, session, cause);
    }

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        verifyInIoThread(session, sessionEx.getIoThread());
        super.messageReceived(nextFilter, session, message);
    }

    @Override
    public void messageSent(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {
        verifyInIoThread(session, sessionEx.getIoThread());
        super.messageSent(nextFilter, session, writeRequest);
    }

    @Override
    public void sessionClosed(NextFilter nextFilter, IoSession session)
            throws Exception {
        // This is called by NioSocketAcceptor during gateway shutdown so thread is not sessionEx.getIoThread() to match
        verifyInIoThread(session, sessionEx.getIoThread());
        super.sessionClosed(nextFilter, session);
    }

    @Override
    public void sessionCreated(NextFilter nextFilter, IoSession session)
            throws Exception {
        verifyInIoThread(session, sessionEx.getIoThread());
        super.sessionCreated(nextFilter, session);
    }

    @Override
    public void sessionIdle(NextFilter nextFilter, IoSession session, IdleStatus status) throws Exception {
        verifyInIoThread(session, sessionEx.getIoThread());
        super.sessionIdle(nextFilter, session, status);
    }

    @Override
    public void sessionOpened(NextFilter nextFilter, IoSession session)
            throws Exception {
        verifyInIoThread(session, sessionEx.getIoThread());
        super.sessionOpened(nextFilter, session);
    }

}
