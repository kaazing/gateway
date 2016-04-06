/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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
package org.kaazing.gateway.transport.wsn;

import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.LIGHTWEIGHT;

import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.transport.ws.AbstractWsAcceptProcessor;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer;
import org.kaazing.mina.core.buffer.IoBufferEx;

public class WsnAcceptProcessor extends AbstractWsAcceptProcessor<WsnSession> {


    @Override
    protected Object getMessageFromWriteRequest(WsnSession session, WriteRequest request) {
        Object message = super.getMessageFromWriteRequest(session, request);
        boolean unwrapWsMessages = session.getLocalAddress().getOption(LIGHTWEIGHT);
        if (unwrapWsMessages && message instanceof WsMessage) {
            WsMessage wsMessage = (WsMessage)message;
            final IoBufferEx ioBufferEx = ((WsMessage) message).getBytes();
            if (wsMessage.getKind() == WsMessage.Kind.TEXT && ioBufferEx instanceof WsBuffer) {
                ((WsBuffer) ioBufferEx).setKind(WsBuffer.Kind.TEXT);
            }
            ioBufferEx.mark(); // mark this buffer so it can get reset() when messageSent is called.
            return ioBufferEx;
        }
        return message;
    }

    @Override
    protected boolean shouldAccountForWrittenBytes(WsnSession session) {
        return !session.getLocalAddress().getOption(LIGHTWEIGHT);
    }
}
