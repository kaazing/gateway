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

import static java.lang.String.format;

import java.nio.channels.ClosedChannelException;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBridgeHandler implements IoHandler {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractBridgeHandler.class);

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
    	if (LOG.isTraceEnabled()) {
            // Do not print endless stack traces if a session is closed (e.g. during gateway destroy) with pending writes
            if (session.isClosing() && cause instanceof ClosedChannelException) {
                LOG.trace(format("Exception caught in bridge handler, probably because session was closed with pending writes: %s",
                        cause));
            }
            else {
        		LOG.trace("Exception caught in bridge handler", cause);
        	}
    	}

        IoHandler handler = getHandler(session, false);
        if (handler != null) {
            handler.exceptionCaught(session, cause);
        }
        else {
            if (LOG.isDebugEnabled()) {
                String message = format("Unable to get handler in bridge handler exceptionCaught while processing exception %s",
                        cause);
                if (LOG.isTraceEnabled()) {
                    // note: still debug level, but with extra detail about the exception
                    LOG.debug(message, cause);
                }
                else {
                    LOG.debug(message);
                }
            }
            session.close(true);
        }
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
    	//LOG.trace("MESSAGE RECEIVED: " + session + " " + message.hashCode() + " " + message);
        getHandler(session).messageReceived(session, message);
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        getHandler(session).messageSent(session, message);
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
    	//LOG.trace("SESSION CLOSED: " + session);
    	getHandler(session).sessionClosed(session);
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
    	//LOG.trace("SESSION CREATED: " + session);
        getHandler(session).sessionCreated(session);
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        getHandler(session).sessionIdle(session, status);
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
    	//LOG.trace("SESSION OPENED: " + session);
        IoHandler handler = getHandler(session);
        handler.sessionOpened(session);
    }

    protected final IoHandler getHandler(IoSession session) throws Exception {
        return getHandler(session, true);
    }

    protected abstract IoHandler getHandler(IoSession session, boolean throwIfNull) throws Exception;

}
