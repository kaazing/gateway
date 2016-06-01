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
package org.kaazing.gateway.transport.http.bridge.filter;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.transport.IoFilterAdapter;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;

/**
 * Closes the connection if the http handshake is not successful in the given amount of time
 *
 */
public class HttpStartHandshakeTimerFilter extends IoFilterAdapter<IoSessionEx> {

    private final Logger logger;
    private final Long handshakeTimeout;
    private ScheduledExecutorService taskExecutor;

    public HttpStartHandshakeTimerFilter(Logger logger, Long handshakeTimeout, ScheduledExecutorService taskExecutor) {
        this.logger = logger;
        this.handshakeTimeout = handshakeTimeout;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void onPostAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
        IoSession session = parent.getSession();
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("Setting handshake timeout of %d milliseconds on http session %s ", handshakeTimeout,
                    session));
        }

        taskExecutor.schedule(() -> {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format(
                        "Closing http session %s because handshake timeout of %d milliseconds is exceeded", session,
                        handshakeTimeout));
            }
            session.close(true);
         }, handshakeTimeout, TimeUnit.MILLISECONDS);
    }
}
