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
package org.kaazing.gateway.server.util.io;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

public class IoFilterAdapter<S extends IoSession, M> implements IoFilter<S, M> {

    @Override
    public void init() throws Exception {
    }

    @Override
    public void onPostAdd(IoFilterChain parent, String name,
                          NextFilter nextFilter) throws Exception {
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
    public void destroy() throws Exception {
    }

    @Override
    public void exceptionCaught(NextFilter nextFilter, S session,
                                Throwable cause) throws Exception {
        nextFilter.exceptionCaught(session, cause);
    }

    @Override
    public void filterClose(NextFilter nextFilter, S session) throws Exception {
        nextFilter.filterClose(session);
    }

    @Override
    public void filterWrite(NextFilter nextFilter, S session, WriteRequest writeRequest, M message) throws Exception {
        nextFilter.filterWrite(session, writeRequest);
    }

    @Override
    public void messageReceived(NextFilter nextFilter, S session, M message) throws Exception {
        nextFilter.messageReceived(session, message);
    }

    @Override
    public void messageSent(NextFilter nextFilter, S session,
                            WriteRequest writeRequest, Object message) throws Exception {
        nextFilter.messageSent(session, writeRequest);
    }

    @Override
    public void sessionClosed(NextFilter nextFilter, S session)
            throws Exception {
        nextFilter.sessionClosed(session);
    }

    @Override
    public void sessionCreated(NextFilter nextFilter, S session)
            throws Exception {
        nextFilter.sessionCreated(session);
    }

    @Override
    public void sessionIdle(NextFilter nextFilter, S session, IdleStatus status) throws Exception {
        nextFilter.sessionIdle(session, status);
    }

    @Override
    public void sessionOpened(NextFilter nextFilter, S session)
            throws Exception {
        nextFilter.sessionOpened(session);
    }

}
