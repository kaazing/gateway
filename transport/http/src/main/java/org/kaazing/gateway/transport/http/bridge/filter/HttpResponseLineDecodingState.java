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

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.statemachine.DecodingState;
import org.apache.mina.filter.codec.statemachine.LinearWhitespaceSkippingState;
import org.kaazing.gateway.transport.DecodingStateMachine;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpVersion;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.filter.codec.statemachine.ConsumeToCrLfDecodingState;
import org.kaazing.mina.filter.codec.statemachine.ConsumeToLinearWhitespaceDecodingState;


public abstract class HttpResponseLineDecodingState extends DecodingStateMachine {

	private static final Charset US_ASCII = Charset.forName("US-ASCII");
	private final CharsetDecoder US_ASCII_DECODER = US_ASCII.newDecoder();
	
	private static final Charset UTF_8 = Charset.forName("UTF-8");
	private final CharsetDecoder UTF_8_DECODER = UTF_8.newDecoder();

	private final DecodingState READ_VERSION = new ConsumeToLinearWhitespaceDecodingState(allocator) {
        boolean verifiedVersion;

        @Override
		public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out) throws Exception {
            if (!verifiedVersion && in.hasRemaining()) {
                verifiedVersion = true;
                int ch = in.get(in.position());
                if (ch != 'H') {
                    throw new ProtocolDecoderException("Invalid HTTP version, starting with char = " + ch);
                }
            }

            return super.decode(in, out);
        }

		@Override
		protected DecodingState finishDecode(IoBuffer buffer, ProtocolDecoderOutput out) throws Exception {
			if (!buffer.hasRemaining()) {
				return this;
			}

			String httpVersion = buffer.getString(US_ASCII_DECODER);
			out.write(HttpVersion.valueOf(httpVersion
					.replaceAll("\\/|\\.", "_")));
			return AFTER_READ_VERSION;
		}
		
	};
	
	private final DecodingState AFTER_READ_VERSION = new LinearWhitespaceSkippingState() {
		@Override
		protected DecodingState finishDecode(int skippedBytes) throws Exception {
			return READ_STATUS;
		}
	};

	private final DecodingState READ_STATUS = new ConsumeToLinearWhitespaceDecodingState(allocator) {
		@Override
		protected DecodingState finishDecode(IoBuffer buffer, ProtocolDecoderOutput out) throws Exception {
			String statusCode = buffer.getString(UTF_8_DECODER);
			out.write(HttpStatus.getHttpStatus(statusCode));
			return AFTER_READ_STATUS;
		}
		
	};

	private final DecodingState AFTER_READ_STATUS = new LinearWhitespaceSkippingState() {
		@Override
		protected DecodingState finishDecode(int skippedBytes) throws Exception {
			return READ_REASON;
		}
	};

	private final DecodingState READ_REASON = new ConsumeToCrLfDecodingState(allocator) {
		@Override
		protected DecodingState finishDecode(IoBuffer buffer, ProtocolDecoderOutput out) throws Exception {
			String httpReason = buffer.getString(US_ASCII_DECODER);
			out.write(httpReason);
			return null;
		}
	};

	public HttpResponseLineDecodingState(IoBufferAllocatorEx<?> allocator) {
        super(allocator);
    }

    @Override
	protected DecodingState init() throws Exception {
		return READ_VERSION;
	}
	
	@Override
	protected void destroy() throws Exception {
	}
}
