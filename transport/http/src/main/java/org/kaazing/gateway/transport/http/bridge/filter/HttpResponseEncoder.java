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

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;

import org.apache.mina.filter.codec.ProtocolEncoderException;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.kaazing.gateway.transport.bridge.CachingMessageEncoder;
import org.kaazing.gateway.transport.http.HttpCookie;
import org.kaazing.gateway.transport.http.HttpHeaders;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpUtils;
import org.kaazing.gateway.transport.http.HttpVersion;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.gateway.transport.http.bridge.HttpMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;

public final class HttpResponseEncoder extends HttpMessageEncoder<HttpResponseMessage> {

    private static final byte[] COOKIE_HEADER_BYTES = "Set-Cookie: ".getBytes();
    private static final byte[] COOKIE_COMMENT_BYTES = "; Comment=".getBytes();
    private static final byte[] COOKIE_DOMAIN_BYTES = "; Domain=".getBytes();
    private static final byte[] COOKIE_MAX_AGE_BYTES = "; Max-Age=".getBytes();
    private static final byte[] COOKIE_PATH_BYTES = "; Path=".getBytes();
    private static final byte[] COOKIE_SECURE_BYTES = "; Secure".getBytes();
    private static final byte[] COOKIE_VERSION_BYTES = "; Version=".getBytes();



    public HttpResponseEncoder(IoBufferAllocatorEx<?> allocator) {
        this(IO_MESSAGE_ENCODER, allocator);
    }

    public HttpResponseEncoder(CachingMessageEncoder cachingEncoder, IoBufferAllocatorEx<?> allocator) {
        super(cachingEncoder, allocator);
    }

    @Override
    protected void encode(IoSessionEx session, HttpMessage httpMessage, ProtocolEncoderOutput out) throws Exception {
        switch (httpMessage.getKind()) {
        case CONTENT:
            HttpContentMessage httpContent = (HttpContentMessage)httpMessage;
            encodeContent(session, httpContent, out);
            break;
        case RESPONSE:
            HttpResponseMessage httpResponse = (HttpResponseMessage)httpMessage;
            encodeResponse(session, httpResponse, out);
            break;
        default:
            throw new ProtocolEncoderException("Unexpected HTTP message kind: " + httpMessage.getKind());
        }
    }

    private void encodeResponse(IoSessionEx session, HttpResponseMessage httpResponse, ProtocolEncoderOutput out) throws Exception {

        
        boolean isGzipped = HttpUtils.isGzipped(httpResponse);
        boolean isChunked = HttpUtils.isChunked(httpResponse);
            ByteBuffer nioBuf = allocator.allocate(1024);
            IoBufferEx buf = allocator.wrap(nioBuf).setAutoExpander(allocator);
            int allocatedPos = buf.position();
        encodeResponseLine(httpResponse.getVersion(), httpResponse.getStatus(), httpResponse.getReason(), buf);
        encodeHeaders(session, httpResponse, buf);

        // Insert GZIP Start of Frame if necessary
        if (isGzipped) {
            buf.put(HttpGzipEncoder.GZIP_START_OF_FRAME_BYTES);
        }

        HttpContentMessage httpContent = httpResponse.getContent();
        if (httpContent != null) {
            encodeContent(session, httpContent, buf, isChunked, isGzipped);
        }
            
        buf.flip();
            buf.position(allocatedPos);
        out.write(buf);

    }
    
    private void encodeResponseLine(HttpVersion version, HttpStatus status, String reason, IoBufferEx buf) throws CharacterCodingException {

        assert version != null : "version required in response";
        assert status != null : "status required in response";
        
        switch (version) {
        case HTTP_1_0:
            buf.put(HTTP_1_0_BYTES);
            break;
        case HTTP_1_1:
            buf.put(HTTP_1_1_BYTES);
            break;
        }

        buf.put(SPACE_BYTES);
        buf.putString(Integer.toString(status.code()), asciiEncoder);

        if (reason == null) {
            reason = status.reason();
        }
        
        buf.put(SPACE_BYTES);
        buf.putString(reason, asciiEncoder);
        buf.put(CRLF_BYTES);
    }

    @Override
  protected void encodeContentLength(
    IoSessionEx session,
    HttpResponseMessage httpResponse,
    IoBufferEx buf) throws CharacterCodingException {

        switch (httpResponse.getStatus()) {
        case INFO_SWITCHING_PROTOCOLS:
        case REDIRECT_NOT_MODIFIED:
            break;
        default:
            if (httpResponse.getHeader(HttpHeaders.HEADER_CONTENT_LENGTH) == null && httpResponse.getHeader(HttpHeaders.HEADER_TRANSFER_ENCODING) == null ) {
                super.encodeContentLength(session, httpResponse, buf);
            }
            break;
        }
    }

  @Override
  protected void encodeCookies(IoSessionEx session, HttpResponseMessage httpResponse, IoBufferEx buf)
            throws CharacterCodingException {
        for (HttpCookie cookie : httpResponse.getCookies()) {
            String name = cookie.getName();
            String comment = cookie.getComment();
            String domain = cookie.getDomain();
            long maxAge = cookie.getMaxAge();
            String path = cookie.getPath();
            boolean secure = cookie.isSecure();
            String value = cookie.getValue();
            int version = cookie.getVersion();

            buf.put(COOKIE_HEADER_BYTES);
            buf.putString(name, asciiEncoder);
            buf.put(EQUAL_BYTES);
            buf.putString(value, asciiEncoder);
            
            if (comment != null) {
                buf.put(COOKIE_COMMENT_BYTES);
                buf.putString(comment, asciiEncoder);
            }
            
            if (domain != null) {
                buf.put(COOKIE_DOMAIN_BYTES);
                buf.putString(domain, asciiEncoder);
            }
            
            if (maxAge > 0) {
                buf.put(COOKIE_MAX_AGE_BYTES);
                buf.putString(Long.toString(maxAge), asciiEncoder);
            }
            
            if (path != null) {
                buf.put(COOKIE_PATH_BYTES);
                buf.putString(path, asciiEncoder);
            }
            
            if (secure) {
                buf.put(COOKIE_SECURE_BYTES);
            }
            
            if (version > 0) {
                buf.put(COOKIE_VERSION_BYTES);
                buf.putString(Integer.toString(version), asciiEncoder);
            }
            
            buf.put(SEMI_BYTES);
            buf.put(CRLF_BYTES);
        }
    }
}
