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

package org.kaazing.gateway.transport.http.bridge.filter;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.statemachine.CrLfDecodingState;
import org.apache.mina.filter.codec.statemachine.DecodingState;
import org.apache.mina.filter.codec.statemachine.LinearWhitespaceSkippingState;
import org.kaazing.gateway.transport.DecodingStateMachine;
import org.kaazing.gateway.transport.http.bridge.HttpHeaderNameComparator;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.filter.codec.statemachine.ConsumeToCrLfDecodingState;
import org.kaazing.mina.filter.codec.statemachine.ConsumeToTerminatorDecodingState;

public abstract class HttpHeaderDecodingState extends DecodingStateMachine {

	private static final String HEADER_WEBSOCKET_KEY_PREFIX = "Sec-WebSocket-Key";

	private static final Charset US_ASCII = Charset.forName("US-ASCII");
	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private final CharsetDecoder asciiDecoder = US_ASCII.newDecoder();
	private final CharsetDecoder utf8Decoder = UTF_8.newDecoder();

	// use list to preserve header value ordering
	private Map<String, List<String>> headers;
	private String lastHeaderName;

	private final DecodingState FIND_EMPTY_LINE = new CrLfDecodingState() {
		@Override
		protected DecodingState finishDecode(boolean foundCRLF,
				ProtocolDecoderOutput out) throws Exception {
			if (foundCRLF) {
			    out.write(headers);
				initHeaders();
				return null;
			} else {
				return READ_HEADER_NAME;
			}
		}
	};

	private final DecodingState READ_HEADER_NAME = new ConsumeToTerminatorDecodingState(allocator,
			(byte) ':') {
		@Override
		protected DecodingState finishDecode(IoBuffer buffer,
				ProtocolDecoderOutput out) throws Exception {
		    if (buffer == null || !buffer.hasRemaining()) {
		        throw new ProtocolDecoderException("Invalid header name in the request");
		    }
			lastHeaderName = buffer.getString(asciiDecoder);
			return AFTER_READ_HEADER_NAME;
		}
	};

	private final DecodingState AFTER_READ_HEADER_NAME = new LinearWhitespaceSkippingState() {
		@Override
		protected DecodingState finishDecode(int skippedBytes) throws Exception {
			return READ_HEADER_VALUE;
		}
	};

	private final DecodingState READ_HEADER_VALUE = new ConsumeToCrLfDecodingState(allocator) {
		@Override
		protected DecodingState finishDecode(IoBuffer buffer,
				ProtocolDecoderOutput out) throws Exception {
			String value = buffer.getString(utf8Decoder);
			List<String> values = headers.get(lastHeaderName);
			if (values == null) {
				values = new ArrayList<String>();
				headers.put(lastHeaderName, values);
			}

			// multiple values for the same header has comma separator
			// WebSocket upgrade for Draft 76 fails if the value is split
			if (value.indexOf(',') == -1 || lastHeaderName.startsWith(HEADER_WEBSOCKET_KEY_PREFIX)) {
				values.add(value);
			}
			else {
				String[] separatedValues = value.split(",");
				for (String separatedValue : separatedValues) {
					values.add(separatedValue.trim());
				}
			}
			
			return AFTER_READ_HEADER_VALUE;
		}
	};

	private final DecodingState AFTER_READ_HEADER_VALUE = new LinearWhitespaceSkippingState() {
		@Override
		protected DecodingState finishDecode(int skippedBytes) throws Exception {
			if (skippedBytes == 0) {
				return FIND_EMPTY_LINE;
			} else {
				return READ_HEADER_VALUE;
			}
		}
	};

	public HttpHeaderDecodingState(IoBufferAllocatorEx<?> allocator) {
        super(allocator);
    }

    @Override
	protected DecodingState init() throws Exception {
	    initHeaders();
		return FIND_EMPTY_LINE;
	}

	@Override
	protected void destroy() throws Exception {
	}

    private void initHeaders() {
        headers = new TreeMap<String, List<String>>(HttpHeaderNameComparator.INSTANCE);
    }
}
