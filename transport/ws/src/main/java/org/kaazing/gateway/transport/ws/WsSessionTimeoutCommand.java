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
package org.kaazing.gateway.transport.ws;

import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A runnable that when scheduled supports closing a WebSocket session
 * in the future, unconditionally.  The ability to schedule and cancel this runnable
 * are also included here.
 */
public class WsSessionTimeoutCommand extends WsScheduledCommand implements Runnable {

    private static final AttributeKey FUTURE_KEY = new AttributeKey(WsSessionTimeoutCommand.class, "sessionTimeoutFuture");
    private static final Logger log = LoggerFactory.getLogger("session.timeout");

    private volatile IoSession session;
    private final long sessionId;

    public WsSessionTimeoutCommand(IoSession session) {
        this.session = session;
        this.sessionId = session != null ? session.getId() : 0;
    }

    @Override
    public void run() {
        final IoSession session = this.session;
        if (session != null) {
            if (!session.isClosing()) {
                log("Session timing out.");
                CloseFuture closeFuture = session.close(true);
                closeFuture.addListener(new IoFutureListener<IoFuture>() {
                    @Override
                    public void operationComplete(IoFuture future) {
                        log("Session timed out.");
                    }
                });
            }
        }
    }

    @Override
    public AttributeKey getScheduledFutureKey() {
        return FUTURE_KEY;
    }

    @Override
    public void clear() {
        session = null;
    }

    public void log(String value) {
        if (log.isTraceEnabled()) {
            if (session == null) {
                log.trace(String.format("[ws #%s] " + value, sessionId + "/closing"));
            } else {
                log.trace(String.format("[ws #%s] " + value, sessionId));
            }
        }
    }

}
