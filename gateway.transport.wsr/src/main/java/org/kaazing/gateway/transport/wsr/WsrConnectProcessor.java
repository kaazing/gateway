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

package org.kaazing.gateway.transport.wsr;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.transport.BridgeConnectProcessor;
import org.kaazing.gateway.transport.wsr.bridge.filter.WsrBuffer;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;

public class WsrConnectProcessor extends BridgeConnectProcessor<WsrSession> {

    @Override
	protected WriteFuture flushNow(WsrSession session, IoSessionEx parent,
			IoBufferEx buf, IoFilterChain filterChain, WriteRequest request) {

        if (buf instanceof WsrBuffer) {
            WsrBuffer wsrBuffer = (WsrBuffer)buf;
        	RtmpBinaryDataMessage wsrMessage = wsrBuffer.getMessage();
            if (wsrMessage == null) {
                // cache newly constructed message (atomic update)
            	RtmpBinaryDataMessage newWsrMessage = new RtmpBinaryDataMessage(buf);
            	newWsrMessage.setChunkStreamId(5);
            	newWsrMessage.setMessageStreamId(session.getDownstreamId());
                if (wsrBuffer.isAutoCache()) {
                    // buffer is cached on parent, continue with derived caching
                    newWsrMessage.initCache();
                }
                boolean wasUpdated = wsrBuffer.setMessage(newWsrMessage);
                wsrMessage = wasUpdated ? newWsrMessage : wsrBuffer.getMessage();
            }
            return flushNowInternal(parent, wsrMessage, wsrBuffer, filterChain, request);
        }
        else {
            // flush the buffer out to the session
        	RtmpBinaryDataMessage newWsrMessage = new RtmpBinaryDataMessage(buf);
        	newWsrMessage.setChunkStreamId(5);
        	newWsrMessage.setMessageStreamId(session.getDownstreamId());
            return flushNowInternal(parent, newWsrMessage, buf, filterChain, request);
        }
    }
}
