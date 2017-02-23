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
package org.kaazing.gateway.transport.nio.internal;

import static org.apache.mina.core.session.IdleStatus.BOTH_IDLE;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.transport.IoFilterAdapter;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;

/**
 * Closes the connection if no data is read or written in x amount of time
 *
 */
public class UdpAlignFilter extends IoFilterAdapter<IoSessionEx> {

    private final Logger logger;
    private final Integer align;

    public UdpAlignFilter(Logger logger, Integer align, IoSession session) {
        this.logger = logger;
        this.align = align;
    }

    @Override
    public void onPostAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
        IoSession session = parent.getSession();
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Setting align %d on session %s ", align, session));
        }
        // TODO should do anything ?
    }

    @Override
    protected void doSessionIdle(NextFilter nextFilter, IoSessionEx session, IdleStatus status) throws Exception {

        if (status == BOTH_IDLE) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Closing tcp session %s because idle timeout of %d secs is exceeded", session,
                    align));
            }
            session.close(false);
        }
        super.doSessionIdle(nextFilter, session, status);
    }

    @Override
    protected void doMessageReceived(NextFilter nextFilter, IoSessionEx session, Object message) throws Exception {
        if (message instanceof IoBuffer) {
            System.out.println(session + ": got a buffer : " + message);
        }
        super.doMessageReceived(nextFilter, session, message);
    }
}
