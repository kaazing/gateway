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

import static org.kaazing.gateway.transport.bridge.CachingMessageEncoder.IO_MESSAGE_ENCODER;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.util.Iterator;
import java.util.Set;

import org.apache.mina.filter.codec.ProtocolEncoderException;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.kaazing.gateway.transport.bridge.CachingMessageEncoder;
import org.kaazing.gateway.transport.http.HttpCookie;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.gateway.transport.http.HttpVersion;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.gateway.transport.http.bridge.HttpMessage;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;


public class HttpRequestEncoder extends HttpMessageEncoder<HttpRequestMessage> {

	private static final byte[] COOKIE_HEADER_BYTES = "Cookie: ".getBytes();
	private static final byte[] COOKIE_DOMAIN_BYTES = "; Domain=".getBytes();
	private static final byte[] COOKIE_PATH_BYTES = "; Path=".getBytes();
	private static final byte[] COOKIE_VERSION_BYTES = "; Version=".getBytes();

    public HttpRequestEncoder(IoBufferAllocatorEx<?> allocator) {
        this(IO_MESSAGE_ENCODER, allocator);
    }

    public HttpRequestEncoder(CachingMessageEncoder cachingEncoder, IoBufferAllocatorEx<?> allocator) {
        super(cachingEncoder, allocator);
    }

	@Override
	protected void encode(IoSessionEx session, HttpMessage httpMessage, ProtocolEncoderOutput out) throws Exception {
		switch (httpMessage.getKind()) {
		case CONTENT:
			HttpContentMessage httpContent = (HttpContentMessage)httpMessage;
			encodeContent(session, httpContent, out);
			break;
		case REQUEST:
			HttpRequestMessage httpRequest = (HttpRequestMessage)httpMessage;
			encodeRequest(session, httpRequest, out);
			break;
		default:
			throw new ProtocolEncoderException("Unexpected HTTP message kind: " + httpMessage.getKind());
		}
	}

	protected void encodeRequest(IoSessionEx session, HttpRequestMessage httpRequest, ProtocolEncoderOutput out) throws Exception {
        ByteBuffer nioBuf = allocator.allocate(1024);
        IoBufferEx buf = allocator.wrap(nioBuf).setAutoExpander(allocator);
        encodeRequestLine(httpRequest, buf);
        encodeHeaders(session, httpRequest, buf);

        HttpContentMessage httpContent = httpRequest.getContent();
		if (httpContent != null) {
			encodeContent(session, httpContent, buf);
		}
		
		buf.flip();
		out.write(buf);
	}

	private void encodeRequestLine(HttpRequestMessage httpRequest, IoBufferEx buf) throws CharacterCodingException {

		HttpMethod method = httpRequest.getMethod();
		URI requestURI = httpRequest.getRequestURI();
		HttpVersion version = httpRequest.getVersion();
		
		buf.putString(method.name(), asciiEncoder);
		buf.put(SPACE_BYTES);

		switch (method) {
		case CONNECT:
			buf.putString(requestURI.getAuthority(), asciiEncoder);
			buf.put(SPACE_BYTES);
			break;
		default:
			// TODO: encode parameters
			buf.putString(requestURI.toString(), asciiEncoder);
			buf.put(SPACE_BYTES);
			break;
		}

		switch (version) {
		case HTTP_1_0:
			buf.put(HTTP_1_0_BYTES);
			break;
		case HTTP_1_1:
			buf.put(HTTP_1_1_BYTES);
			break;
		}

		buf.put(CRLF_BYTES);
	}

	@Override
	protected void encodeCookies(IoSessionEx session, HttpRequestMessage httpRequest,
			IoBufferEx buf) throws CharacterCodingException {
		
		Set<HttpCookie> cookies = httpRequest.getCookies();
		if (!cookies.isEmpty()) {
			buf.put(COOKIE_HEADER_BYTES);
			for (Iterator<HttpCookie> i=cookies.iterator(); i.hasNext(); ) {
				
				HttpCookie cookie = i.next();
				String name = cookie.getName();
				String domain = cookie.getDomain();
				String path = cookie.getPath();
				String value = cookie.getValue();
				int version = cookie.getVersion();
	
				buf.putString(name, asciiEncoder);
				buf.put(EQUAL_BYTES);
				buf.putString(value, asciiEncoder);
				
				if (domain != null) {
					buf.put(COOKIE_DOMAIN_BYTES);
					buf.putString(domain, asciiEncoder);
				}
				
				if (path != null) {
					buf.put(COOKIE_PATH_BYTES);
					buf.putString(path, asciiEncoder);
				}
				
				if (version > 0) {
					buf.put(COOKIE_VERSION_BYTES);
					buf.putString(Integer.toString(version), asciiEncoder);
				}
				
				if (i.hasNext()) {
					buf.put(SEMI_BYTES);
				}
			}
			buf.put(CRLF_BYTES);
		}
	}
	
	@Override
	protected void encodeContentLength(IoSessionEx session,
			HttpRequestMessage httpStart, IoBufferEx buf)
			throws CharacterCodingException {
		switch (httpStart.getMethod()) {
		case GET:
		case HEAD:
			HttpContentMessage httpContent = httpStart.getContent();
            if (httpContent != null && httpContent.length() > 0) {
				super.encodeContentLength(session, httpStart, buf);
			}
			break;
		default:
			super.encodeContentLength(session, httpStart, buf);
			break;
		}
	}

}
