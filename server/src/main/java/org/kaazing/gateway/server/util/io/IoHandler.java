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

import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;


public interface IoHandler<S extends IoSession, M> {

    void sessionCreated(S session) throws Exception;

    void sessionOpened(S session) throws Exception;

    void sessionIdle(S session) throws Exception;

    void sessionClosed(S session) throws Exception;

    void exceptionCaught(S session, Throwable cause) throws Exception;

    void messageReceived(S session, M message) throws Exception;

    void messageSent(S session, M message) throws Exception;

    @SuppressWarnings("unchecked")
    class Adapter<S extends IoSession, M, H extends IoHandler<S, M>>
            implements org.apache.mina.core.service.IoHandler {

        protected final H handler;

        public Adapter(H handler) {
            this.handler = handler;
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause)
                throws Exception {
            handler.exceptionCaught((S) session, cause);
        }

        @Override
        public void messageReceived(IoSession session, Object message)
                throws Exception {
            handler.messageReceived((S) session, (M) message);
        }

        @Override
        public void messageSent(IoSession session, Object message)
                throws Exception {
            handler.messageSent((S) session, (M) message);
        }

        @Override
        public void sessionClosed(IoSession session) throws Exception {
            handler.sessionClosed((S) session);
        }

        @Override
        public void sessionCreated(IoSession session) throws Exception {
            handler.sessionCreated((S) session);
        }

        @Override
        public void sessionIdle(IoSession session, IdleStatus status)
                throws Exception {
            handler.sessionIdle((S) session);
        }

        @Override
        public void sessionOpened(IoSession session) throws Exception {
            handler.sessionOpened((S) session);
        }

    }

}
