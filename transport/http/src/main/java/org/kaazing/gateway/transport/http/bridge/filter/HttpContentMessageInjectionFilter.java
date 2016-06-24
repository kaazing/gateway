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

import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CONTENT_LENGTH;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;

public class HttpContentMessageInjectionFilter extends HttpFilterAdapter<IoSessionEx> {

    private static final Charset US_ASCII = Charset.forName("US-ASCII");

    @Override
    protected void filterWriteHttpResponse(NextFilter nextFilter,
                                           IoSessionEx session,
                                           WriteRequest writeRequest,
                                           HttpResponseMessage httpResponse)
            throws Exception {
        // GL.debug("http", getClass().getSimpleName() + " filterWriteHttpResponse.");
        HttpContentMessage content = httpResponse.getContent();
        String contentLength = httpResponse.getHeader(HEADER_CONTENT_LENGTH);
        HttpStatus httpStatus = httpResponse.getStatus();
        boolean noContent = content == null || (content.length() == 0 && content.isComplete());
        if (contentLength == null && contentAutomaticallyInjectable(httpStatus) && noContent) {
            if (!httpResponse.isContentExcluded()) {
                IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
                ByteBuffer nioBuf = allocator.allocate(256);
                IoBufferEx buf = allocator.wrap(nioBuf);
                String message = String.format("<html><head></head><body><h1>%d %s</h1></body></html>",
                        httpStatus.code(), httpResponse.getBodyReason());
                buf.putString(message, US_ASCII.newEncoder());
                buf.flip();
                httpResponse.setHeader("Content-Type", "text/html");
                httpResponse.setContent(new HttpContentMessage(buf, true));
            }
        }

        super.filterWriteHttpResponse(nextFilter, session, writeRequest, httpResponse);
    }

    public static boolean contentAutomaticallyInjectable(HttpStatus httpStatus) {
        switch (httpStatus) {
            case CLIENT_BAD_REQUEST:
            case CLIENT_CONFLICT:
            case CLIENT_EXPECTATION_FAILED:
            case CLIENT_FORBIDDEN:
            case CLIENT_GONE:
            case CLIENT_LENGTH_REQUIRED:
            case CLIENT_METHOD_NOT_ALLOWED:
            case CLIENT_NOT_ACCEPTABLE:
            case CLIENT_NOT_FOUND:
            case CLIENT_PAYMENT_REQUIRED:
            case CLIENT_PRECONDITION_FAILED:
            case CLIENT_PROXY_AUTHENTICATION_REQUIRED:
            case CLIENT_REQUEST_ENTITY_TOO_LARGE:
            case CLIENT_REQUEST_RANGE_NOT_SATISFIABLE:
            case CLIENT_REQUEST_TIMEOUT:
            case CLIENT_REQUEST_URI_TOO_LONG:
            case CLIENT_UNAUTHORIZED:
            case CLIENT_UNSUPPORTED_MEDIA_TYPE:
            case SERVER_BAD_GATEWAY:
            case SERVER_GATEWAY_TIMEOUT:
            case SERVER_INTERNAL_ERROR:
            case SERVER_NOT_IMPLEMENTED:
            case SERVER_SERVICE_UNAVAILABLE:
            case SERVER_VERSION_NOT_SUPPORTED:
                return true;
            default:
                return false;
        }
    }
}
