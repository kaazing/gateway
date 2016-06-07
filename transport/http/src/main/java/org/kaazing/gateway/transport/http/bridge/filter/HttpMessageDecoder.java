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

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.statemachine.DecodingState;

public abstract class HttpMessageDecoder extends ProtocolDecoderAdapter {
	
	private static final AttributeKey DECODING_STATE = new AttributeKey(HttpMessageDecoder.class, "decodingState");
	
	@Override
	public void decode(IoSession session, IoBuffer in,
					   ProtocolDecoderOutput out) throws Exception {

		DecodingState decodingState = getDecodingState(session);
		if (decodingState.decode(in, out) == null) {
			session.removeAttribute(DECODING_STATE);
		}
	}

	@Override
	public void finishDecode(IoSession session, ProtocolDecoderOutput out)
			throws Exception {

		DecodingState decodingState = (DecodingState)session.removeAttribute(DECODING_STATE);
		if (decodingState != null) {
			decodingState.finishDecode(out);
		}
	}
	
	protected abstract DecodingState initDecodingState(IoSession session);

	private DecodingState getDecodingState(IoSession session) {
		DecodingState decodingState = (DecodingState)session.getAttribute(DECODING_STATE);

		if (decodingState == null) {
			decodingState = initDecodingState(session);
			session.setAttribute(DECODING_STATE, decodingState);
		}
		
		return decodingState;
	}


}
