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

public interface IoFilter<S extends IoSession, M> {

    void init() throws Exception;

    void onPreAdd(IoFilterChain parent, String name,
                         NextFilter nextFilter) throws Exception;

    void onPostAdd(IoFilterChain parent, String name,
                          NextFilter nextFilter) throws Exception;

    void onPreRemove(IoFilterChain parent, String name,
                            NextFilter nextFilter) throws Exception;

    void onPostRemove(IoFilterChain parent, String name,
                             NextFilter nextFilter) throws Exception;

    void destroy() throws Exception;

    void sessionCreated(NextFilter nextFilter, S session)
            throws Exception;

    void sessionOpened(NextFilter nextFilter, S session)
            throws Exception;

    void sessionIdle(NextFilter nextFilter, S session, IdleStatus status)
            throws Exception;

    void sessionClosed(NextFilter nextFilter, S session)
            throws Exception;

    void messageReceived(NextFilter nextFilter, S session, M message) throws Exception;

    void messageSent(NextFilter nextFilter, S session,
                            WriteRequest writeRequest, Object message) throws Exception;

    void filterWrite(NextFilter nextFilter, S session,
                            WriteRequest writeRequest, M message) throws Exception;

    void filterClose(NextFilter nextFilter, S session) throws Exception;

    void exceptionCaught(NextFilter nextFilter, S session,
                                Throwable cause) throws Exception;

    @SuppressWarnings("unchecked")
    class Adapter<S extends IoSession, M, F extends IoFilter<S, M>> implements
            org.apache.mina.core.filterchain.IoFilter {

        protected final F filter;

        public Adapter(F typedFilter) {
            this.filter = typedFilter;
        }

        @Override
        public void init() throws Exception {
            filter.init();
        }

        @Override
        public void destroy() throws Exception {
            filter.destroy();
        }

        @Override
        public void exceptionCaught(NextFilter nextFilter, IoSession session,
                                    Throwable cause) throws Exception {
            filter.exceptionCaught(nextFilter, (S) session, cause);
        }

        @Override
        public void filterClose(NextFilter nextFilter, IoSession session)
                throws Exception {
            filter.filterClose(nextFilter, (S) session);
        }

        @Override
        public void filterWrite(NextFilter nextFilter, IoSession session,
                                WriteRequest writeRequest) throws Exception {
            filter.filterWrite(nextFilter, (S) session, writeRequest,
                    (M) writeRequest.getMessage());
        }

        @Override
        public void messageReceived(NextFilter nextFilter, IoSession session,
                                    Object message) throws Exception {
            filter.messageReceived(nextFilter, (S) session, (M) message);
        }

        @Override
        public void messageSent(NextFilter nextFilter, IoSession session,
                                WriteRequest writeRequest) throws Exception {
            filter.messageSent(nextFilter, (S) session, writeRequest,
                    writeRequest.getMessage());
        }

        @Override
        public void onPostAdd(IoFilterChain parent, String name,
                              NextFilter nextFilter) throws Exception {
            filter.onPostAdd(parent, name, nextFilter);
        }

        @Override
        public void onPostRemove(IoFilterChain parent, String name,
                                 NextFilter nextFilter) throws Exception {
            filter.onPostRemove(parent, name, nextFilter);
        }

        @Override
        public void onPreAdd(IoFilterChain parent, String name,
                             NextFilter nextFilter) throws Exception {
            filter.onPreAdd(parent, name, nextFilter);
        }

        @Override
        public void onPreRemove(IoFilterChain parent, String name,
                                NextFilter nextFilter) throws Exception {
            filter.onPreRemove(parent, name, nextFilter);
        }

        @Override
        public void sessionClosed(NextFilter nextFilter, IoSession session)
                throws Exception {
            filter.sessionClosed(nextFilter, (S) session);
        }

        @Override
        public void sessionCreated(NextFilter nextFilter, IoSession session)
                throws Exception {
            filter.sessionCreated(nextFilter, (S) session);
        }

        @Override
        public void sessionIdle(NextFilter nextFilter, IoSession session,
                                IdleStatus status) throws Exception {
            filter.sessionIdle(nextFilter, (S) session, status);
        }

        @Override
        public void sessionOpened(NextFilter nextFilter, IoSession session)
                throws Exception {
            filter.sessionOpened(nextFilter, (S) session);
        }

    }
}
