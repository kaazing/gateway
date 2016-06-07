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

import static java.lang.Long.parseLong;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static org.kaazing.gateway.transport.http.HttpAcceptFilter.CONTENT_LENGTH_ADJUSTMENT;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CACHE_CONTROL;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CONTENT_LENGTH;
import static org.kaazing.gateway.transport.http.HttpMethod.POST;
import static org.kaazing.gateway.transport.http.HttpStatus.SUCCESS_OK;

import java.net.URI;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolEncoderException;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpConnectSession;
import org.kaazing.gateway.transport.http.HttpCookie;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.gateway.transport.http.HttpSession;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpVersion;
import org.kaazing.gateway.transport.http.bridge.HttpHeaderNameComparator;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;

public class HttpxeProtocolFilter extends HttpFilterAdapter<IoSession> {

    private static final String CONTENT_TYPE_TEXT_PLAIN_CHARSET_UTF_8 = "text/plain;charset=UTF-8";

    private static final String CONTENT_TYPE_APPLICATION_OCTET_STREAM = "application/octet-stream";

    private static final String CONTENT_TYPE_APPLICATION_X_MESSAGE_HTTP = "application/x-message-http";

    private static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";

    private static final String CONTENT_TYPE_PREFIX_TEXT = "text/";

    private static final SortedSet<String> RESTRICTED_ENVELOPE_HEADERS;

    static {
        // note: restricted headers are case-insensitive (!)
        // see: http://www.w3.org/TR/XMLHttpRequest/#dom-xmlhttprequest-setrequestheader
        SortedSet<String> restrictedEnvelopeHeaders = new TreeSet<>(HttpHeaderNameComparator.INSTANCE);
        restrictedEnvelopeHeaders.addAll(asList("Accept-Charset", "Accept-Encoding", "Access-Control-Request-Headers", "Access-Control-Request-Method",
                                                "Connection", "Cookie", "Cookie2", "Date", "DNT", "Expect", "Host", "Keep-Alive", "Origin", "Referer",
                                                "TE", "Trailer", "Transfer-Encoding", "Upgrade", "User-Agent", "Via"));
        RESTRICTED_ENVELOPE_HEADERS = restrictedEnvelopeHeaders;
    }

    private static final Collection<String> ASCII_COMPATIBLE = Arrays.asList("charset=ascii", "charset=utf-8", "charset=windows-1252");

    private enum Mode { CLIENT, SERVER }

    private final Mode mode;

    public HttpxeProtocolFilter(boolean client) {
        this.mode = client ? Mode.CLIENT : Mode.SERVER;
    }

    @Override
    protected void filterWriteHttpRequest(NextFilter nextFilter, IoSession session, WriteRequest writeRequest,
            HttpRequestMessage httpRequest) throws Exception {

        switch (mode) {
        case CLIENT:
            HttpConnectSession httpSession = (HttpConnectSession) session;
            filterWriteAndInjectHttpRequest(nextFilter, httpSession, writeRequest, httpRequest);
            break;
        default:
            super.filterWriteHttpRequest(nextFilter, session, writeRequest, httpRequest);
            break;
        }

    }

    @Override
    protected void httpRequestReceived(NextFilter nextFilter, IoSession session, HttpRequestMessage httpRequest)
            throws Exception {
        // GL.debug("http", getClass().getSimpleName() + " request received.");

        switch (mode) {
        case SERVER:
            HttpAcceptSession httpSession = (HttpAcceptSession)session;
            receiveAndExtractHttpRequest(nextFilter, httpSession, httpRequest);
            break;
        default:
            super.httpRequestReceived(nextFilter, session, httpRequest);
            break;
        }

    }

    @Override
    protected void filterWriteHttpResponse(NextFilter nextFilter, IoSession session, WriteRequest writeRequest,
            HttpResponseMessage httpResponse) throws Exception {

        switch (mode) {
        case SERVER:
            HttpAcceptSession httpSession = (HttpAcceptSession)session;
            filterWriteAndInjectHttpResponse(nextFilter, httpSession, writeRequest, httpResponse);
            break;
        default:
            super.filterWriteHttpResponse(nextFilter, session, writeRequest, httpResponse);
            break;
        }

    }

    @Override
    protected void httpResponseReceived(NextFilter nextFilter, IoSession session, HttpResponseMessage httpResponse)
            throws Exception {

        switch (mode) {
        case CLIENT:
            HttpSession httpSession = (HttpSession) session;
            receiveAndExtractHttpResponse(nextFilter, httpSession, httpResponse);
            break;
        default:
            super.httpResponseReceived(nextFilter, session, httpResponse);
            break;
        }

    }

    private void filterWriteAndInjectHttpRequest(NextFilter nextFilter, HttpConnectSession session, WriteRequest writeRequest, HttpRequestMessage httpRequest) throws Exception {

        // inject version
        HttpVersion version = httpRequest.getVersion();
        session.setVersion(version);

        // inject method
        session.setMethod(POST);

        // inject requestURI
        URI requestURI = httpRequest.getRequestURI();
        session.setRequestURI(requestURI);

        // inject content-type
        String contentType = httpRequest.getHeader("Content-Type");
        String newContentType = calculateContentType(contentType);
        session.setWriteHeader("Content-Type", newContentType);

        // inject headers
        for (Iterator<String> iterator = httpRequest.iterateHeaderNames(); iterator.hasNext(); ) {

            String headerName = iterator.next();

            // skip headers that must not be inserted
            switch (headerName.charAt(0)) {
            case 'a':
            case 'A':
                if ("Authorization".equalsIgnoreCase(headerName)) {
                    continue;
                }
                break;
            case 'c':
            case 'C':
                if (HEADER_CONTENT_LENGTH.equalsIgnoreCase(headerName)) {
                    IoFilterChain filterChain = session.getFilterChain();
                    filterChain.addFirst(CONTENT_LENGTH_ADJUSTMENT.filterName(), CONTENT_LENGTH_ADJUSTMENT.filter());
                    break;
                }
                else if ("Content-Type".equalsIgnoreCase(headerName)) {
                    continue;
                }
                break;
            }

            // inject header
            String headerValue = httpRequest.getHeader(headerName);
            session.setWriteHeader(headerName, headerValue);
            iterator.remove();
        }

        // inject cookies
        if (httpRequest.hasCookies()) {
            Set<HttpCookie> writeCookies = session.getWriteCookies();
            for (Iterator<HttpCookie> iterator = httpRequest.iterateCookies(); iterator.hasNext(); ) {
                HttpCookie cookie = iterator.next();
                writeCookies.add(cookie);
                iterator.remove();
            }
        }

        nextFilter.filterWrite(session, writeRequest);
    }

    private void receiveAndExtractHttpRequest(NextFilter nextFilter, HttpAcceptSession session, HttpRequestMessage httpRequest) throws Exception {
        // set implicit content length
        httpRequest.setContentLengthImplicit(true);

        // validate version
        if (session.getVersion() != httpRequest.getVersion()) {
            throw new ProtocolDecoderException("HTTP version mismatch");
        }

        // validate method
        if (session.getMethod() != HttpMethod.POST) {
            throw new ProtocolDecoderException("Unexpected HTTP method");
        }

        // validate request URI
        if (!URLDecoder.decode(session.getRequestURI().toString(), "UTF-8").equals(URLDecoder.decode(httpRequest.getRequestURI().toString(), "UTF-8"))) {
            throw new ProtocolDecoderException("HTTP request URI mismatch");
        }

        // validate content type
        String contentType = session.getReadHeader("Content-Type");
        if (contentType == null) {
            throw new ProtocolDecoderException("Expected HTTP content-type");
        }
        else if (!CONTENT_TYPE_APPLICATION_X_MESSAGE_HTTP.equals(contentType)) {
            throw new ProtocolDecoderException("Unexpected HTTP content-type");
        }

        // validate enveloped header names
        for (String headerName : httpRequest.getHeaderNames()) {
            if (RESTRICTED_ENVELOPE_HEADERS.contains(headerName)) {
                throw new ProtocolDecoderException("Unsupported HTTP header(s)");
            }
        }

        // extract read headers
        for (String readHeaderName : session.getReadHeaderNames()) {

            // skip headers that must not be extracted
            switch (readHeaderName.charAt(0)) {
            case 'a':
            case 'A':
                if ("Authorization".equalsIgnoreCase(readHeaderName)) {
                    continue;
                }
                break;
            case 'c':
            case 'C':
                if (HEADER_CONTENT_LENGTH.equalsIgnoreCase(readHeaderName)) {
                    switch (httpRequest.getMethod()) {
                    case GET:
                    case HEAD:
                        break;
                    default:
                        if (!httpRequest.hasHeader(readHeaderName)) {
                            long contentLength = parseLong(session.getReadHeader(readHeaderName));
                            long newContentLength = contentLength - session.getReadBytes();
                            httpRequest.setHeader(readHeaderName, valueOf(newContentLength));
                        }
                        break;
                    }
                    continue;
                }
                else if ("Content-Type".equalsIgnoreCase(readHeaderName)) {
                    continue;
                }
                break;
            case 'x':
            case 'X':
                if ("X-Next-Protocol".equalsIgnoreCase(readHeaderName)) {
                    // avoid propagating next protocol header (!)
                    continue;
                }
                break;
            }

            // extract header
            List<String> readHeaderValues = session.getReadHeaders(readHeaderName);
            if (readHeaderValues != null && !readHeaderValues.isEmpty()) {
                for(String headerValue : readHeaderValues) {
                    httpRequest.addHeader(readHeaderName, headerValue);
                }
            }
        }

        // extract cookies
        for (HttpCookie readCookie : session.getReadCookies()) {
            httpRequest.addCookie(readCookie);
        }

        // propagate message
        nextFilter.messageReceived(session, httpRequest);
    }

    private HttpResponseMessage filterWriteAndInjectHttpResponse(NextFilter nextFilter, HttpAcceptSession session, WriteRequest writeRequest, HttpResponseMessage httpResponse) throws ProtocolEncoderException {
        // set implicit content length
        httpResponse.setContentLengthImplicit(true);
        httpResponse.setBlockPadding(false);

        // inject version
        HttpVersion version = httpResponse.getVersion();
        session.setVersion(version);

        // inject status
        HttpStatus status = httpResponse.getStatus();
        switch (status) {
        case CLIENT_NOT_FOUND:
            session.setStatus(status);
            if (httpResponse.hasReason()) {
                session.setReason(httpResponse.getReason());
            }
            break;
        default:
            session.setStatus(SUCCESS_OK);
            break;
        }

        // note: this logic appears duplicated/derived from HttpMessageEncoder.encodeContentLength(...)
        boolean adjustContentLength = httpResponse.hasHeader(HEADER_CONTENT_LENGTH) ||
                                     (httpResponse.isComplete() &&
                                      !"gzip".equals(httpResponse.getHeader("Content-Encoding")) &&
                                      !"chunked".equals(httpResponse.getHeader("Transfer-Encoding")));

        // inject headers
        for (Iterator<String> iterator = httpResponse.iterateHeaderNames(); iterator.hasNext(); ) {
            String headerName = iterator.next();

            if (headerName.length() > 0) {
                outer:
                switch (headerName.charAt(0)) {
                case 'a':
                case 'A':
                    if (headerName.length() > 21) {
                        switch(headerName.charAt(21)) {
                        case 'c':
                        case 'C':
                            if ("Access-Control-Allow-Credentials".equalsIgnoreCase(headerName)) {
                                break outer;
                            }
                            break;
                        case 'h':
                        case 'H':
                            if ("Access-Control-Allow-Headers".equalsIgnoreCase(headerName)) {
                                break outer;
                            }
                            break;
                        case 'o':
                        case 'O':
                            if ("Access-Control-Allow-Origin".equalsIgnoreCase(headerName)) {
                                break outer;
                            }
                            break;
                        }
                    }
                    continue;
                case 'c':
                case 'C':
                    if (headerName.length() > 1) {
                        switch (headerName.charAt(1)) {
                        case 'a':
                        case 'A':
                            if ("Cache-Control".equalsIgnoreCase(headerName)) {
                                break outer;
                            }
                            break;
                        case 'o':
                        case 'O':
                            // character 2 is same for both
                            if (headerName.length() > 3) {
                                switch(headerName.charAt(3)) {
                                case 'n':
                                case 'N':
                                    if ("Connection".equalsIgnoreCase(headerName)) {
                                        break outer;
                                    }
                                    break;
                                case 't':
                                case 'T':
                                    if (HEADER_CONTENT_LENGTH.equalsIgnoreCase(headerName)) {
                                        break outer;
                                    }
                                    else if ("Content-Encoding".equalsIgnoreCase(headerName)) {
                                        break outer;
                                    }
                                    else if ("Content-Type".equalsIgnoreCase(headerName)) {
                                        break;
                                    }
                                    break;
                                }
                            }
                            break;
                        }
                    }
                    continue;
                case 'd':
                case 'D':
                    if ("Date".equalsIgnoreCase(headerName)) {
                        break outer;
                    }
                    continue;
                case 'e':
                case 'E':
                    if ("ETag".equalsIgnoreCase(headerName)) {
                        break outer;
                    }
                    continue;
                case 'l':
                case 'L':
                    if ("Last-Modified".equalsIgnoreCase(headerName)) {
                        break outer;
                    }
                    continue;
                case 'p':
                case 'P':
                    if ("Pragma".equalsIgnoreCase(headerName)) {
                        break outer;
                    }
                    continue;
                case 's':
                case 'S':
                    if (headerName.length() > 2) {
                        switch (headerName.charAt(2)) {
                        case 'r':
                        case 'R':
                            if ("Server".equalsIgnoreCase(headerName)) {
                                break outer;
                            }
                            break;
                        case 't':
                        case 'T':
                            if ("Set-Cookie".equalsIgnoreCase(headerName)) {
                                break outer;
                            }
                            break;
                        }
                    }
                    continue;
                case 't':
                case 'T':
                    if ("Transfer-Encoding".equalsIgnoreCase(headerName)) {
                        break outer;
                    }
                    continue;
                case 'x':
                case 'X':
                    if ("X-Content-Type-Options".equalsIgnoreCase(headerName)) {
                        break outer;
                    }
                    continue;
                default:
                    continue;
                }
            }

            // inject header
            List<String> headerValues = httpResponse.getHeaderValues(headerName);

            session.setWriteHeaders(headerName, headerValues);
            iterator.remove();
        }

        if (session.getStatus() == HttpStatus.SUCCESS_OK && httpResponse.getStatus() != HttpStatus.SUCCESS_OK
                && session.getWriteHeader(HEADER_CACHE_CONTROL) == null) {
            session.setWriteHeader(HEADER_CACHE_CONTROL, "no-cache");
        }

        // inject cookies
        if (httpResponse.hasCookies()) {
            Set<HttpCookie> writeCookies = session.getWriteCookies();
            for (Iterator<HttpCookie> iterator = httpResponse.iterateCookies(); iterator.hasNext(); ) {
                HttpCookie cookie = iterator.next();
                writeCookies.add(cookie);
                iterator.remove();
            }
        }

        // handle content-length
        if (adjustContentLength) {
            IoFilterChain filterChain = session.getFilterChain();
            filterChain.addFirst(CONTENT_LENGTH_ADJUSTMENT.filterName(), CONTENT_LENGTH_ADJUSTMENT.filter());
        }

        // handle content-type
        String contentType = httpResponse.getHeader("Content-Type");
        if ( HttpContentMessageInjectionFilter.contentAutomaticallyInjectable(httpResponse.getStatus())) {
            contentType = null;
        }
        String newContentType = calculateContentType(contentType);
        session.setWriteHeader("Content-Type", newContentType);

        nextFilter.filterWrite(session, writeRequest);

        return httpResponse;
    }

    private void receiveAndExtractHttpResponse(NextFilter nextFilter, HttpSession session, HttpResponseMessage httpResponse) throws Exception {

        // validate version
        if (session.getVersion() != httpResponse.getVersion()) {
            throw new ProtocolDecoderException("HTTP version mismatch");
        }

        // validate status
        switch (session.getStatus()) {
        case SUCCESS_OK:
            break;
        default:
            throw new ProtocolDecoderException("HTTP status mismatch");
        }

        // extract read headers
        for (String readHeaderName : session.getReadHeaderNames()) {

            // skip headers that must not be extracted
            switch (readHeaderName.charAt(0)) {
            case 'c':
            case 'C':
                if (HEADER_CONTENT_LENGTH.equalsIgnoreCase(readHeaderName)) {
                    if (!httpResponse.hasHeader(readHeaderName)) {
                        long contentLength = parseLong(session.getReadHeader(readHeaderName));
                        long newContentLength = contentLength - session.getReadBytes();
                        httpResponse.setHeader(readHeaderName, valueOf(newContentLength));
                    }
                    continue;
                }
                if ("Content-Type".equalsIgnoreCase(readHeaderName)) {

                    // validate content type
                    String contentType = session.getReadHeader("Content-Type");
                    String innerContentType = httpResponse.getHeader("Content-Type");
                    if (innerContentType != null) {
                        if (innerContentType.startsWith(CONTENT_TYPE_PREFIX_TEXT)) {
                            int charsetAt = innerContentType.indexOf(';');
                            if (charsetAt == -1) {
                                if (CONTENT_TYPE_TEXT_PLAIN.equals(contentType) == false) {
                                    throw new ProtocolDecoderException("Inconsistent HTTP content-type");
                                }
                            }
                            else {
                                String charset = innerContentType.substring(charsetAt + 1);
                                if (!ASCII_COMPATIBLE.contains(charset.toLowerCase())) {
                                    throw new ProtocolEncoderException("HTTP enveloping not compatible with charset: " + charset);
                                }
                                if (format("%s;%s", CONTENT_TYPE_TEXT_PLAIN, charset).equals(contentType) == false) {
                                    throw new ProtocolDecoderException("Inconsistent HTTP content-type");
                                }
                            }
                        }
                        else if (innerContentType.equals(contentType) == false) {
                            throw new ProtocolDecoderException("Inconsistent HTTP content-type");
                        }
                    }

                    continue;
                }
                break;
            case 'w':
            case 'W':
                if ("WWW-Authenticate".equalsIgnoreCase(readHeaderName)) {
                    // note: do not extract this challenge to avoid collision between http / httpxe 401s
                    continue;
                }
                break;
            }

            // extract header
            String readHeaderValue = session.getReadHeader(readHeaderName);
            httpResponse.setHeader(readHeaderName, readHeaderValue);
        }

        // extract cookies
        for (HttpCookie readCookie : session.getReadCookies()) {
            httpResponse.addCookie(readCookie);
        }

        // propagate message
        nextFilter.messageReceived(session, httpResponse);
    }

    private String calculateContentType(String contentType) throws ProtocolEncoderException {

        if (contentType == null) {
            return CONTENT_TYPE_TEXT_PLAIN_CHARSET_UTF_8;
        }

        // text/???[;charset=???] => text/plain[;charset=???]
        if (contentType.startsWith(CONTENT_TYPE_PREFIX_TEXT)) {
            int charsetAt = contentType.indexOf(';');
            if (charsetAt != -1) {
                String charsetName = contentType.substring(charsetAt + 1).trim();
                if (!ASCII_COMPATIBLE.contains(charsetName.toLowerCase())) {
                    throw new ProtocolEncoderException("HTTP enveloping not compatible with charset: " + charsetName);
                }
                return format("%s;%s", CONTENT_TYPE_TEXT_PLAIN, charsetName);
            }
            else {
                return CONTENT_TYPE_TEXT_PLAIN;
            }
        }

        // non-text => application/octet-stream
        return CONTENT_TYPE_APPLICATION_OCTET_STREAM;
    }

}
