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
package org.kaazing.gateway.transport;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

/**
 * The contents of this file are subject to the Common Public Attribution
 * License Version 1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License
 * at http://www.kaazing.org/CPAL.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the Original
 * Developer.
 *
 * In addition, Exhibit A has been modified to be consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is Kaazing Gateway. The Initial Developer of the
 * Original Code is Kaazing Corporation.
 *
 * All portions of the code written by Kaazing Corporation are
 * Copyright (c) 2007-2010 Kaazing Corporation. All Rights Reserved.
 */

public class IoFilterAdapter<T extends IoSession> implements IoFilter {

    // -- not generic --

    @Override
    public void init() throws Exception {
    }

    @Override
    public void destroy() throws Exception {
    }

    @Override
    public void onPostAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
    }

    @Override
    public void onPostRemove(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
    }

    @Override
    public void onPreAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
    }

    @Override
    public void onPreRemove(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
    }

    // -- handler api --

    @SuppressWarnings("unchecked")
    @Override
    public final void exceptionCaught(NextFilter nextFilter, IoSession session, Throwable cause) throws Exception {
        doExceptionCaught(nextFilter, (T)session, cause);
    }

    protected void doExceptionCaught(NextFilter nextFilter, T session, Throwable cause) throws Exception {
        nextFilter.exceptionCaught(session, cause);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        doMessageReceived(nextFilter, (T)session, message);
    }

    protected void doMessageReceived(NextFilter nextFilter, T session, Object message) throws Exception {
        nextFilter.messageReceived(session, message);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void messageSent(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        doMessageSent(nextFilter, (T)session, writeRequest);
    }

    protected void doMessageSent(NextFilter nextFilter, T session, WriteRequest writeRequest) throws Exception {
        nextFilter.messageSent(session, writeRequest);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void sessionClosed(NextFilter nextFilter, IoSession session) throws Exception {
        doSessionClosed(nextFilter, (T)session);
    }

    protected void doSessionClosed(NextFilter nextFilter, T session) throws Exception {
        nextFilter.sessionClosed(session);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void sessionCreated(NextFilter nextFilter, IoSession session) throws Exception {
        doSessionCreated(nextFilter, (T)session);
    }

    protected void doSessionCreated(NextFilter nextFilter, T session) throws Exception {
        nextFilter.sessionCreated(session);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void sessionIdle(NextFilter nextFilter, IoSession session, IdleStatus status) throws Exception {
        doSessionIdle(nextFilter, (T)session, status);
    }

    protected void doSessionIdle(NextFilter nextFilter, T session, IdleStatus status) throws Exception {
        nextFilter.sessionIdle(session, status);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void sessionOpened(NextFilter nextFilter, IoSession session) throws Exception {
        doSessionOpened(nextFilter, (T)session);
    }

    protected void doSessionOpened(NextFilter nextFilter, T session) throws Exception {
        nextFilter.sessionOpened(session);
    }

    // -- filter api --

    @SuppressWarnings("unchecked")
    @Override
    public final void filterClose(NextFilter nextFilter, IoSession session) throws Exception {
        doFilterClose(nextFilter, (T)session);
    }

    protected void doFilterClose(NextFilter nextFilter, T session) throws Exception {
        nextFilter.filterClose(session);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        doFilterWrite(nextFilter, (T)session, writeRequest);
    }

    protected void doFilterWrite(NextFilter nextFilter, T session, WriteRequest writeRequest) throws Exception {
        nextFilter.filterWrite(session, writeRequest);
    }
}
