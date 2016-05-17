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

import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IoHandlerAdapter<S extends IoSession, M> implements IoHandler<S, M> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void exceptionCaught(S session, Throwable cause) throws Exception {
        if (logger.isWarnEnabled()) {
            logger.warn("EXCEPTION, please implement "
                    + getClass().getName()
                    + ".exceptionCaught() for proper handling:", cause);
        }
    }

    @Override
    public void messageReceived(S session, M message) throws Exception {
    }

    @Override
    public void messageSent(S session, M message) throws Exception {
    }

    @Override
    public void sessionClosed(S session) throws Exception {
    }

    @Override
    public void sessionCreated(S session) throws Exception {
    }

    @Override
    public void sessionIdle(S session) throws Exception {
    }

    @Override
    public void sessionOpened(S session) throws Exception {
    }

}
