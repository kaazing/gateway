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

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.transport.AbstractBridgeSession;
import org.kaazing.gateway.transport.BridgeAcceptProcessor;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;

public abstract class AbstractWsAcceptProcessor<T extends AbstractBridgeSession<?, ?>> extends BridgeAcceptProcessor<T> {

    protected AbstractWsAcceptProcessor() {
        super();
    }

    @Override
	protected WriteFuture flushNow(T session, IoSessionEx parent,
			IoBufferEx buf, IoFilterChain filterChain, WriteRequest request) {

        if (buf instanceof WsBuffer) {
            WsBuffer wsBuffer = (WsBuffer)buf;
            // TODO: key of is-shared here, instead of auto-cache below
            WsMessage wsMessage = wsBuffer.getMessage();
            if (wsMessage == null) {
                // cache newly constructed message (atomic update)
                WsMessage newWsMessage;
                switch (wsBuffer.getKind()) {
                case TEXT:
                    newWsMessage = new WsTextMessage(buf, wsBuffer.isFin());
                    break;
                case CONTINUATION:
                    newWsMessage = new WsContinuationMessage(buf, wsBuffer.isFin());
                    break;
                case PING:
                    newWsMessage = new WsPingMessage(buf);
                    break;
                case PONG:
                    newWsMessage = new WsPongMessage(buf);
                    break;
                default:
                    newWsMessage = new WsBinaryMessage(buf, wsBuffer.isFin());
                    break;
                }
                if (wsBuffer.isAutoCache()) {
                    // buffer is cached on parent, continue with derived caching
                    newWsMessage.initCache();
                }
                boolean wasUpdated = wsBuffer.setMessage(newWsMessage);
                wsMessage = wasUpdated ? newWsMessage : wsBuffer.getMessage();
            }
            return flushNowInternal(parent, wsMessage, wsBuffer, filterChain, request);
        }
        else {
            // flush the buffer out to the session
            return flushNowInternal(parent, new WsBinaryMessage(buf), buf, filterChain, request);
        }
    }

}
