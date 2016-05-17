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

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChain.Entry;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.transport.TypedAttributeKey;
import org.kaazing.gateway.transport.http.HttpCookie;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpVersion;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.future.DefaultWriteFutureEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.core.write.DefaultWriteRequestEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpOperationFilter extends HttpFilterAdapter<IoSessionEx> {

    private static final String EXTENSION_API_PATH_PREFIX = HttpProtocolCompatibilityFilter.API_PATH + "/";
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final byte[] EQUAL_BYTES = "=".getBytes(UTF_8);
    private static final byte[] CRLF_BYTES = "\r\n".getBytes(UTF_8);

    private static final Map<String, HttpOperation> OPERATIONS;


    private static final Logger logger = LoggerFactory.getLogger("transport.http.accept");

    static {
        Map<String, HttpOperation> operations = new HashMap<>();
        operations.put("set-cookies", new HttpSetCookiesOperation());
        operations.put("get-cookies", new HttpGetCookiesOperation());
        operations.put("delete-cookies", new HttpDeleteCookiesOperation());
        OPERATIONS = operations;
    }


    @Override
    protected void httpRequestReceived(NextFilter nextFilter, IoSessionEx session, HttpRequestMessage httpRequest) throws Exception {
        // GL.debug("http", getClass().getSimpleName() + " request received.");

        // determine if an operation has been invoked
        URI requestURI = httpRequest.getRequestURI();
        String requestPath = requestURI.getPath();
        int apiAt = requestPath.indexOf(EXTENSION_API_PATH_PREFIX);
        if (apiAt != -1) {
            String operationName = requestPath.substring(apiAt + EXTENSION_API_PATH_PREFIX.length());
            HttpOperation operation = OPERATIONS.get(operationName);
            if (operation != null) {
                // operation found, add next in filter chain and propagate request
                IoFilterChain filterChain = session.getFilterChain();
                Entry entry = filterChain.getEntry(this);
                entry.addAfter(entry.getName() + "#" + operationName, operation);
                super.httpRequestReceived(nextFilter, session, httpRequest);
            }
            else {
                // unrecognized operation found or httpe-1.0
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Rejected unrecognized operation \"%s\" for URI \"%s\" on session %s",
                            operationName, requestURI, session));
                }
                HttpResponseMessage httpResponse = new HttpResponseMessage();
                httpResponse.setVersion(HttpVersion.HTTP_1_1);
                httpResponse.setStatus(HttpStatus.CLIENT_NOT_FOUND);
                filterWrite(nextFilter, session, new DefaultWriteRequestEx(httpResponse, new DefaultWriteFutureEx(session)));
            }
        }
        else {
            // request path does not attempt to invoke an operation
            super.httpRequestReceived(nextFilter, session, httpRequest);
        }
    }

    private static String discoverServiceDomain(HttpRequestMessage request) throws Exception {
        ResourceAddress address = request.getLocalAddress();
        return address.getOption(HttpResourceAddress.SERVICE_DOMAIN);
    }

    private static class HttpOperation extends HttpFilterAdapter<IoSessionEx> {

        @Override
        protected void httpRequestReceived(NextFilter nextFilter, IoSessionEx session, HttpRequestMessage httpRequest)
                throws Exception {
            if (httpRequest.isComplete()) {
                IoFilterChain filterChain = session.getFilterChain();
                filterChain.remove(this);
            }
        }

        @Override
        protected void httpContentReceived(NextFilter nextFilter, IoSessionEx session, HttpContentMessage httpContent)
                throws Exception {
            if (httpContent.isComplete()) {
                IoFilterChain filterChain = session.getFilterChain();
                if (filterChain.contains(this)) {
                    filterChain.remove(this);
                }
            }
        }

        protected final HttpResponseMessage createResponse(HttpStatus status) {
            HttpResponseMessage httpResponse = new HttpResponseMessage();
            httpResponse.setVersion(HttpVersion.HTTP_1_1);
            httpResponse.setStatus(status);
            return httpResponse;
        }

    }

    /**
     * Reads all the cookies in the request body and sets them as Set-Cookie headers in the HTTP response
     *
     */
    static class HttpSetCookiesOperation extends HttpOperation {

        private static final TypedAttributeKey<IoBufferEx> BUFFER_KEY = new TypedAttributeKey<>(HttpSetCookiesOperation.class, "buffer");

        @Override
        protected void httpRequestReceived(NextFilter nextFilter, IoSessionEx session, HttpRequestMessage httpRequest)
                throws Exception {
            super.httpRequestReceived(nextFilter, session, httpRequest);

            String contentType = httpRequest.getHeader("Content-Type");
            if (contentType == null || !contentType.startsWith("text/plain")) {
                if ( logger.isTraceEnabled() ) {
                    logger.trace("Expecting request to have a text/plain content type; this request has: '"+contentType+"'.");
                }
                HttpResponseMessage httpResponse = createResponse(HttpStatus.CLIENT_BAD_REQUEST);
                filterWrite(nextFilter, session, new DefaultWriteRequestEx(httpResponse, new DefaultWriteFutureEx(session)));
            }

            HttpContentMessage httpContent = httpRequest.getContent();
            if (httpContent == null) {
                IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
                httpContent = new HttpContentMessage(allocator.wrap(allocator.allocate(0)), true);
            }
            httpContentReceived(nextFilter, session, httpContent);
        }

        @Override
        protected void httpContentReceived(NextFilter nextFilter, IoSessionEx session, HttpContentMessage httpContent)
                throws Exception {

            IoBufferEx buf = BUFFER_KEY.get(session);
            if (buf != null) {
                // if previously fragmented, aggregate buffer
                buf.put(httpContent.asBuffer());
            }
            else if (!httpContent.isComplete()) {
                // if not previously fragmented, start aggregating buffer
                IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
                ByteBuffer nioBuf = allocator.allocate(4096);  // max 4K set cookie
                buf = allocator.wrap(nioBuf);
                buf.put(httpContent.asBuffer());
                BUFFER_KEY.set(session, buf);
            }

            if (httpContent.isComplete()) {
                // if all payload content available
                if (buf != null) {
                    BUFFER_KEY.remove(session);
                    // prepare aggregated buffer
                    buf.flip();
                }
                else {
                    // extract entire buffer from content
                    buf = httpContent.asBuffer();
                }

                // process aggregated buffer
                String payload = buf.getString(UTF_8.newDecoder());
                String[] lines = payload.split("\r\n");
                HttpResponseMessage httpResponse = createResponse(HttpStatus.SUCCESS_OK);
                for (String line : lines) {
                    httpResponse.addHeader("Set-Cookie", line);
                }
                filterWrite(nextFilter, session, new DefaultWriteRequestEx(httpResponse, new DefaultWriteFutureEx(session)));
            }

            super.httpContentReceived(nextFilter, session, httpContent);
        }

    }

    /**
     * Returns in the response body all cookies set in Cookie headers in the HttpRequest (by the web browser).
     */
    static class HttpGetCookiesOperation extends HttpOperation {

        @Override
        protected void httpRequestReceived(NextFilter nextFilter, IoSessionEx session, HttpRequestMessage httpRequest)
        throws Exception {
            super.httpRequestReceived(nextFilter, session, httpRequest);

            Set<HttpCookie> cookies = httpRequest.getCookies();

            HttpResponseMessage httpResponse = createResponse(HttpStatus.SUCCESS_OK);
            httpResponse.setHeader("Content-Type", "text/plain; charset=UTF-8");

            if (cookies != null && !cookies.isEmpty()) {

                int allocation = 2;

                for (HttpCookie cookie : cookies) {
                    String cookieName = cookie.getName();
                    String value = cookie.getValue();

                    allocation += cookieName.length() + value.length() + 2;
                }

                CharsetEncoder utf8Encoder = UTF_8.newEncoder();
                IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
                IoBufferEx buf = allocator.wrap(allocator.allocate(allocation));
                for (HttpCookie cookie : cookies) {
                    String cookieName = cookie.getName();
                    String value = cookie.getValue();

                    buf.putString(cookieName, utf8Encoder);
                    buf.put(EQUAL_BYTES);
                    buf.putString(value, utf8Encoder);
                    buf.put(CRLF_BYTES);
                }
                buf.flip();

                httpResponse.setContent(new HttpContentMessage(buf, true));
            }

            // ensure response always written, even if no cookies available
            filterWrite(nextFilter, session, new DefaultWriteRequestEx(httpResponse, new DefaultWriteFutureEx(session)));
        }

    }

    static class HttpDeleteCookiesOperation extends HttpOperation {

        private static final TypedAttributeKey<IoBufferEx> BUFFER_KEY = new TypedAttributeKey<>(HttpDeleteCookiesOperation.class, "buffer");

        @Override
        protected void httpRequestReceived(NextFilter nextFilter, IoSessionEx session, HttpRequestMessage httpRequest)
                throws Exception {
            super.httpRequestReceived(nextFilter, session, httpRequest);

            String contentType = httpRequest.getHeader("Content-Type");
            if (contentType == null || !contentType.startsWith("text/plain")) {
                if ( logger.isTraceEnabled() ) {
                    logger.trace("Expecting request to have a text/plain content type; this request has: '"+contentType+"'.");
                }
                HttpResponseMessage httpResponse = createResponse(HttpStatus.CLIENT_BAD_REQUEST);
                filterWrite(nextFilter, session, new DefaultWriteRequestEx(httpResponse, new DefaultWriteFutureEx(session)));
            }

            HttpContentMessage httpContent = httpRequest.getContent();
            if (httpContent == null) {
                IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
                httpContent = new HttpContentMessage(allocator.wrap(allocator.allocate(0)), true);
            }

            HttpContentMessage rewrittenContent =
                    rewriteCookieContent(session,
                            httpContent,
                            discoverServiceDomain(httpRequest));

            if ( rewrittenContent != null ) {
                httpContentReceived(nextFilter, session, rewrittenContent);
            }
        }

        private HttpContentMessage rewriteCookieContent(IoSessionEx session,
                                                        HttpContentMessage httpContent,
                                                        String serviceDomain) throws CharacterCodingException {
            IoBufferEx buf = readContentChunk(session, httpContent);

            if (httpContent.isComplete()) {
                // if all payload content available
                if (buf != null) {
                    BUFFER_KEY.remove(session);
                    // prepare aggregated buffer
                    // deal with zero-copy allocation
                    buf.flip();
                }
                else {
                    // extract entire buffer from content
                    buf = httpContent.asBuffer();
                }

                // process aggregated buffer
                String payload = buf.getString(UTF_8.newDecoder());
                String[] lines = payload.split("\r\n");
                StringBuilder sb = new StringBuilder();
                for (String line : lines) {
                    sb.append(line);
                    if ( serviceDomain != null ) {
                        sb.append("; Domain=").append(serviceDomain);
                    }
                    sb.append("\r\n");
                }
                if ( sb.toString().endsWith("\r\n")) {
                    sb.deleteCharAt(sb.length()-1);
                    sb.deleteCharAt(sb.length()-1);
                }
                IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
                ByteBuffer byteBuf = UTF_8.encode(sb.toString());
                IoBufferEx ioBuf = allocator.wrap(byteBuf);
                return new HttpContentMessage(ioBuf, true, httpContent.isChunked(), httpContent.isGzipped());
            }

            return null;
        }

        @Override
        protected void httpContentReceived(NextFilter nextFilter, IoSessionEx session, HttpContentMessage httpContent)
                throws Exception {

            IoBufferEx buf = httpContent.asBuffer();
            // process aggregated buffer
            String payload = buf.getString(UTF_8.newDecoder());
            String[] lines = payload.split("\r\n");
            HttpResponseMessage httpResponse = createResponse(HttpStatus.SUCCESS_OK);
            for (String line : lines) {
                httpResponse.addHeader("Set-Cookie", line);
            }
            filterWrite(nextFilter, session, new DefaultWriteRequestEx(httpResponse, new DefaultWriteFutureEx(session)));
            super.httpContentReceived(nextFilter, session, httpContent);
        }

        private IoBufferEx readContentChunk(IoSessionEx session, HttpContentMessage httpContent) {
            IoBufferEx buf = BUFFER_KEY.get(session);
            if (buf != null) {
                // if previously fragmented, aggregate buffer
                buf.put(httpContent.asBuffer());
            }
            else if (!httpContent.isComplete()) {
                // if not previously fragmented, start aggregating buffer
                IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
                ByteBuffer nioBuf = allocator.allocate(4096);  // max 4K set cookie
                buf = allocator.wrap(nioBuf);
                buf.put(httpContent.asBuffer());
                BUFFER_KEY.set(session, buf);
            }
            return buf;
        }

    }

}
