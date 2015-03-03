/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.transport.wseb.filter;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.transport.ws.WsCommandMessage;
import org.kaazing.gateway.transport.wseb.WsebSession;
import org.kaazing.mina.core.future.DefaultWriteFutureEx;
import org.kaazing.mina.core.future.WriteFutureEx;
import org.kaazing.mina.core.write.DefaultWriteRequestEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WsebReconnectFilter extends IoFilterAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(WsebReconnectFilter.class);
    
    private final WsebSession wsebSession;

    public WsebReconnectFilter(WsebSession wsebSession) {
        this.wsebSession = wsebSession;
    }
    
    @Override
    public void filterClose(final NextFilter nextFilter, final IoSession session)
    throws Exception {
        if (!wsebSession.isClosing()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("WsebReconnectFilter.filterClose: writing RECONNECT before closing downstream %s",
                        session.getId()));
            }
            // Delay filterClose till write request is written because otherwise DefaultIoFilterChan.TailFilter.filterClose
            // will remove the session from the processor, preventing flush from going through 
            WriteFutureEx writeFuture = new DefaultWriteFutureEx(session);
            writeFuture.addListener(new IoFutureListener<WriteFuture>() {
                @Override
                public void operationComplete(WriteFuture future) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("WsebReconnectFilter.filterClose: writeFuture fired, calling filterClose");
                    }
                    nextFilter.filterClose(session);
                }
            });
            nextFilter.filterWrite(session, new DefaultWriteRequestEx(WsCommandMessage.RECONNECT, writeFuture));
        }
        else {
            super.filterClose(nextFilter, session);
        }
    }
    
}
