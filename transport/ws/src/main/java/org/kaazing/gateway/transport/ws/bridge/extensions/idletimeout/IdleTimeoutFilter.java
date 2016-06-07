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
package org.kaazing.gateway.transport.ws.bridge.extensions.idletimeout;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IdleStatus;
import org.kaazing.gateway.transport.IoFilterAdapter;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.WsPongMessage;
import org.kaazing.mina.core.future.DefaultWriteFutureEx;
import org.kaazing.mina.core.future.WriteFutureEx;
import org.kaazing.mina.core.session.IoSessionConfigEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.core.write.DefaultWriteRequestEx;
import org.kaazing.mina.core.write.WriteRequestEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This filter is used when the x-kaazing-idle-timeout extension is active to keep the client alive by sending regular
 * messages (PONG) if there is no other data flowing to the client.
 */
class IdleTimeoutFilter extends IoFilterAdapter<IoSessionEx>  {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdleTimeoutFilter.class);

    private final long pongDelayMillis; // how long to wait before sending pong

    private final IoFutureListener<WriteFuture> setPongTimeOnWrite = new IoFutureListener<WriteFuture>() {
        @Override
        public void operationComplete(WriteFuture future) {
            pongWritten(System.currentTimeMillis());
        }
    };

    IdleTimeoutFilter(long inactivityTimeout) {
        assert inactivityTimeout > 0;
        // KG-7057: Assume maximum possible round-trip time is half the configured inactivity timeout, but don't let it be 0
        long maxExpectedServerToClientLatency = Math.max(inactivityTimeout / 4, 1);
        this.pongDelayMillis = inactivityTimeout - maxExpectedServerToClientLatency;
    }

    @Override
    public void onPostAdd(IoFilterChain filterChain, String name, NextFilter nextFilter) throws Exception {
        schedulePong((IoSessionEx)filterChain.getSession());
    }

    @Override
    protected void doSessionIdle(NextFilter nextFilter, IoSessionEx session, IdleStatus status) throws Exception {
        if (status == IdleStatus.WRITER_IDLE) {
            writePong(nextFilter, session);
        }
        super.doSessionIdle(nextFilter, session, status);
    }

    protected WsMessage emptyPongMessage(IoSessionEx session) throws Exception {
        WsPongMessage pong = new WsPongMessage();
        return pong;
    }

    private void schedulePong(IoSessionEx session) {
        setWriteIdleTimeInMillis(session, pongDelayMillis);
    }

    private void setWriteIdleTimeInMillis(IoSessionEx session, long delay) {
        IoSessionConfigEx config = session.getConfig();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("IdleTimeoutFilter.setReadIdleTimeInMillis(" + delay + ")");
        }
        config.setIdleTimeInMillis(IdleStatus.WRITER_IDLE, delay);
    }

    void pongWritten(long currentTimeMillis) {
        long pongSentTime = currentTimeMillis;
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("IdleTimeoutFilter.pongWritten at time " + pongSentTime);
        }
    }

    private void writePong(NextFilter nextFilter, IoSessionEx session) throws Exception {
        WsMessage emptyPong = emptyPongMessage(session);
        WriteFutureEx writeFuture = new DefaultWriteFutureEx(session);
        WriteRequestEx pongRequest = new DefaultWriteRequestEx(emptyPong, writeFuture);
        pongRequest.getFuture().addListener(setPongTimeOnWrite);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("Writing %s at time %d", emptyPong, System.currentTimeMillis()));
        }
        nextFilter.filterWrite(session, pongRequest);
    }


}
