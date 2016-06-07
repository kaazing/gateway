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

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IoHandlerAdapter<T extends IoSession> implements IoHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

	@SuppressWarnings("unchecked")
	@Override
	public final void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		doExceptionCaught((T)session, cause);
	}
	
	protected void doExceptionCaught(T session, Throwable cause) throws Exception {
        if (logger.isWarnEnabled()) {
            logger.warn("EXCEPTION, please implement "
                    + getClass().getName()
                    + ".doExceptionCaught() for proper handling:", cause);
        }
	}

	@SuppressWarnings("unchecked")
	@Override
	public final void messageReceived(IoSession session, Object message) throws Exception {
		doMessageReceived((T)session, message);
	}
	
	protected void doMessageReceived(T session, Object message) throws Exception {
	}

	@SuppressWarnings("unchecked")
	@Override
	public final void messageSent(IoSession session, Object message) throws Exception {
		doMessageSent((T)session, message);
	}
	
	protected void doMessageSent(T session, Object message) throws Exception {
	}

	@SuppressWarnings("unchecked")
	@Override
	public final void sessionClosed(IoSession session) throws Exception {
		doSessionClosed((T)session);
	}
	
	protected void doSessionClosed(T session) throws Exception {
	}

	@SuppressWarnings("unchecked")
	@Override
	public final void sessionCreated(IoSession session) throws Exception {
		doSessionCreated((T)session);
	}
	
	protected void doSessionCreated(T session) throws Exception {
	}

	@SuppressWarnings("unchecked")
	@Override
	public final void sessionIdle(IoSession session, IdleStatus status) throws Exception {
		doSessionIdle((T)session, status);
	}
	
	protected void doSessionIdle(T session, IdleStatus status) throws Exception {
	}

	@SuppressWarnings("unchecked")
	@Override
	public final void sessionOpened(IoSession session) throws Exception {
		doSessionOpened((T)session);
	}
	
	protected void doSessionOpened(T session) throws Exception {
	}
}
