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
package org.kaazing.gateway.transport.ws.bridge.filter;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.transport.ws.WsBinaryMessage;
import org.kaazing.gateway.transport.ws.WsFilterAdapter;
import org.kaazing.gateway.transport.ws.WsTextMessage;
import org.kaazing.mina.core.buffer.IoBufferEx;

public class WsFrameTextFilter extends WsFilterAdapter {

	@Override
    protected Object doFilterWriteWsBinary(NextFilter nextFilter, IoSession session, WriteRequest writeRequest, WsBinaryMessage wsBinary) throws Exception {
	    IoBufferEx binary = wsBinary.getBytes();
        return new WsTextMessage(binary);
	}

	@Override
	protected void wsTextReceived(NextFilter nextFilter, IoSession session, WsTextMessage wsText) throws Exception {
        IoBufferEx text = wsText.getBytes();
        super.wsBinaryReceived(nextFilter, session, new WsBinaryMessage(text));
	}
}
