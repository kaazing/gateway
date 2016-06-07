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

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.kaazing.gateway.transport.bridge.MessageBuffer;
import org.kaazing.gateway.transport.http.bridge.HttpMessage;
import org.kaazing.mina.core.buffer.IoBufferEx;

public abstract class HttpBuffer extends MessageBuffer<HttpMessage> {

    private final ConcurrentMap<String, HttpMessage> messages = new ConcurrentHashMap<>();

	HttpBuffer(MessageBuffer<HttpMessage> parent, ByteBuffer buf) {
        super(parent, buf);
	}

    HttpBuffer(ByteBuffer buf) {
        super(buf);
    }

	HttpBuffer(byte[] bytes) {
		this(ByteBuffer.wrap(bytes));
	}

    @Override
	public final HttpMessage getMessage() {
		throw new Error("Use keyed getMessage()");
	}

    @Override
	public final boolean setMessage(HttpMessage newMessage) {
		throw new Error("Use keyed setMessage()");
    }

	public HttpMessage getMessage(String key) {
		return messages.get(key);
	}

	public HttpMessage putMessage(String key, HttpMessage newMessage) {
        HttpMessage oldMessage = messages.putIfAbsent(key, newMessage);
        return oldMessage != null ? oldMessage : newMessage;
	}

	public static String getEncodingKey(boolean isGzipped, boolean isChunked) {
		return isChunked ? (isGzipped ? "CHUNKED_GZIPPED" : "CHUNKED") : (isGzipped ? "GZIPPED" : "NONE");
	}


    static final class HttpSharedBuffer extends HttpBuffer {

        HttpSharedBuffer(ByteBuffer buf) {
            super(buf);
        }

        HttpSharedBuffer(MessageBuffer<HttpMessage> parent, ByteBuffer buf) {
            super(parent, buf);
        }

        @Override
        public int flags() {
            return IoBufferEx.FLAG_SHARED;
        }

        @Override
        protected HttpSharedBuffer asSharedBuffer0() {
            return this;
        }

        @Override
        protected HttpUnsharedBuffer asUnsharedBuffer0() {
            return new HttpUnsharedBuffer(buf());
        }

        @Override
        protected HttpBuffer create0(MessageBuffer<HttpMessage> parent, ByteBuffer buf) {
            return new HttpSharedBuffer(parent, buf);
        }

    }

    static final class HttpUnsharedBuffer extends HttpBuffer {

        HttpUnsharedBuffer(ByteBuffer buf) {
            super(buf);
        }

        HttpUnsharedBuffer(MessageBuffer<HttpMessage> parent, ByteBuffer buf) {
            super(parent, buf);
        }

        @Override
        public int flags() {
            return IoBufferEx.FLAG_NONE;
        }

        @Override
        protected HttpSharedBuffer asSharedBuffer0() {
            return new HttpSharedBuffer(buf());
        }

        @Override
        protected HttpUnsharedBuffer asUnsharedBuffer0() {
            return this;
        }

        @Override
        protected HttpBuffer create0(MessageBuffer<HttpMessage> parent, ByteBuffer buf) {
            return new HttpUnsharedBuffer(parent, buf);
        }

    }
}
