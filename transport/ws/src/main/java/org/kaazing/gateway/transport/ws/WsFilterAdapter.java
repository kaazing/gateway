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

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.mina.filter.util.WriteRequestFilterEx;

public class WsFilterAdapter extends WriteRequestFilterEx {

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {

        WsMessage wsMessage = (WsMessage) message;
        switch (wsMessage.getKind()) {
        case CONTINUATION:
            WsContinuationMessage wsCont = (WsContinuationMessage) wsMessage;
            wsContinuationReceived(nextFilter, session, wsCont);
            break;
        case TEXT:
            WsTextMessage wsText = (WsTextMessage) wsMessage;
            wsTextReceived(nextFilter, session, wsText);
            break;
        case BINARY:
            WsBinaryMessage wsBinary = (WsBinaryMessage) wsMessage;
            wsBinaryReceived(nextFilter, session, wsBinary);
            break;
        case CLOSE:
        	WsCloseMessage wsClose = (WsCloseMessage) wsMessage;
            wsCloseReceived(nextFilter, session, wsClose);
        	break;
        case PING:
            WsPingMessage wsPing = (WsPingMessage) wsMessage;
            wsPingReceived(nextFilter, session, wsPing);
            break;
        case PONG:
            WsPongMessage wsPong = (WsPongMessage) wsMessage;
            wsPongReceived(nextFilter, session, wsPong);
            break;
        default:
            throw new IllegalStateException("Unrecognized WS message kind: " + wsMessage.getKind());
        }
    }

    @Override
    protected Object doFilterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest, Object message) throws Exception {

        // The balancer control frame in written text from the WsAcceptor, independent of the encoding in use.
        // Since this frame is not a WsMessage, the filters on the WsAcceptor (e.g. the base-64 codec filter) should not process
        // this message.
        if (message instanceof IoBuffer) {
            return message;
        }
        WsMessage wsMessage = (WsMessage) message;
        switch (wsMessage.getKind()) {
        case CONTINUATION:
            WsContinuationMessage wsCont = (WsContinuationMessage) wsMessage;
            return doFilterWriteWsCont(nextFilter, session, writeRequest, wsCont);
        case TEXT:
            WsTextMessage wsText = (WsTextMessage) wsMessage;
            return doFilterWriteWsText(nextFilter, session, writeRequest, wsText);
        case BINARY:
            WsBinaryMessage wsBinary = (WsBinaryMessage) wsMessage;
            return doFilterWriteWsBinary(nextFilter, session, writeRequest, wsBinary);
        case CLOSE:
        	WsCloseMessage wsClose = (WsCloseMessage) wsMessage;
        	return doFilterWriteWsClose(nextFilter, session, writeRequest, wsClose);
        case PING:
            WsPingMessage wsPing = (WsPingMessage) wsMessage;
            return doFilterWriteWsPing(nextFilter, session, writeRequest, wsPing);
        case PONG:
            WsPongMessage wsPong = (WsPongMessage) wsMessage;
            return doFilterWriteWsPong(nextFilter, session, writeRequest, wsPong);
        default:
            throw new IllegalStateException("Unrecognized WS message kind: " + wsMessage.getKind());
        }
    }

    protected void wsContinuationReceived(NextFilter nextFilter, IoSession session, WsContinuationMessage wsCont) throws Exception {
        super.messageReceived(nextFilter, session, wsCont);
    }

    protected void wsTextReceived(NextFilter nextFilter, IoSession session, WsTextMessage wsText) throws Exception {
        super.messageReceived(nextFilter, session, wsText);
    }

    protected void wsBinaryReceived(NextFilter nextFilter, IoSession session, WsBinaryMessage wsBinary) throws Exception {
        super.messageReceived(nextFilter, session, wsBinary);
    }

    protected void wsCloseReceived(NextFilter nextFilter, IoSession session, WsCloseMessage wsClose) throws Exception {
        super.messageReceived(nextFilter, session, wsClose);
    }

    protected void wsPingReceived(NextFilter nextFilter, IoSession session, WsPingMessage wsPing) throws Exception {
        super.messageReceived(nextFilter, session, wsPing);
    }

    protected void wsPongReceived(NextFilter nextFilter, IoSession session, WsPongMessage wsPong) throws Exception {
        super.messageReceived(nextFilter, session, wsPong);
    }

    protected Object doFilterWriteWsCont(NextFilter nextFilter, IoSession session, WriteRequest writeRequest, WsContinuationMessage wsCont)
            throws Exception {
        return null;
    }

    protected Object doFilterWriteWsText(NextFilter nextFilter, IoSession session, WriteRequest writeRequest, WsTextMessage wsText)
            throws Exception {
        return null;
    }

    protected Object doFilterWriteWsBinary(NextFilter nextFilter, IoSession session, WriteRequest writeRequest,
            WsBinaryMessage wsBinary) throws Exception {
        return null;
    }

    protected Object doFilterWriteWsClose(NextFilter nextFilter, IoSession session, WriteRequest writeRequest,
            WsCloseMessage wsClose) throws Exception {
        return null;
    }

    protected Object doFilterWriteWsPing(NextFilter nextFilter, IoSession session, WriteRequest writeRequest,
            WsPingMessage wsPing) throws Exception {
        return null;
    }

    protected Object doFilterWriteWsPong(NextFilter nextFilter, IoSession session, WriteRequest writeRequest,
            WsPongMessage wsPong) throws Exception {
        return null;
    }
}
