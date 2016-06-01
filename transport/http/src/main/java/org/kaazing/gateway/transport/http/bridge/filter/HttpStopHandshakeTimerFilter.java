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

import org.kaazing.gateway.transport.IoFilterAdapter;
import org.kaazing.mina.core.session.IoSessionEx;

/**
 * Closes the connection if the http handshake is not successful in the given amount of time
 *
 */
public class HttpStopHandshakeTimerFilter extends IoFilterAdapter<IoSessionEx> {

    private ScheduledExecutorService taskExecutor;

    public HttpStopHandshakeTimerFilter(ScheduledExecutorService taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @Override
    protected void doSessionOpened(NextFilter nextFilter, IoSessionEx session) throws Exception {
        taskExecutor.shutdownNow();
        nextFilter.sessionOpened(session);
    }
}
