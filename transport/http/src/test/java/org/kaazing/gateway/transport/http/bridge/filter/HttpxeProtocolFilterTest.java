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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.jboss.netty.util.CharsetUtil.UTF_8;
import static org.kaazing.gateway.transport.http.HttpMethod.GET;
import static org.kaazing.gateway.transport.http.HttpMethod.POST;
import static org.kaazing.gateway.transport.http.HttpStatus.CLIENT_NOT_FOUND;
import static org.kaazing.gateway.transport.http.HttpStatus.CLIENT_UNAUTHORIZED;
import static org.kaazing.gateway.transport.http.HttpStatus.REDIRECT_FOUND;
import static org.kaazing.gateway.transport.http.HttpStatus.REDIRECT_MULTIPLE_CHOICES;
import static org.kaazing.gateway.transport.http.HttpStatus.REDIRECT_NOT_MODIFIED;
import static org.kaazing.gateway.transport.http.HttpStatus.SUCCESS_OK;
import static org.kaazing.gateway.transport.http.HttpVersion.HTTP_1_0;
import static org.kaazing.gateway.transport.http.HttpVersion.HTTP_1_1;
import static org.kaazing.gateway.transport.http.bridge.HttpContentMessage.EMPTY;
import static org.kaazing.gateway.util.Utils.join;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.junit.Test;
import org.kaazing.gateway.transport.http.DefaultHttpCookie;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpConnectSession;
import org.kaazing.gateway.transport.http.HttpCookie;
import org.kaazing.gateway.transport.http.HttpHeaders;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.test.util.Mockery;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.buffer.SimpleBufferAllocator;

public class HttpxeProtocolFilterTest {

    private Mockery context = new Mockery();
    private HttpAcceptSession serverSession = context.mock(HttpAcceptSession.class);
    private HttpConnectSession clientSession = context.mock(HttpConnectSession.class);
    @SuppressWarnings("unchecked")
    private Set<HttpCookie> writeCookies = context.mock(Set.class);
    private IoFilterChain filterChain = context.mock(IoFilterChain.class);
    private NextFilter nextFilter = context.mock(NextFilter.class);

    @Test
    public void shouldWriteResponseWithInsertedStatusNotFound() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(CLIENT_NOT_FOUND);

        context.checking(new Expectations() { {
            oneOf(serverSession).setVersion(HTTP_1_1);
            oneOf(serverSession).setStatus(CLIENT_NOT_FOUND);
            oneOf(serverSession).setWriteHeader("Content-Type", "text/plain;charset=UTF-8");
            oneOf(serverSession).getStatus(); will(returnValue(SUCCESS_OK));
            oneOf(serverSession).getWriteHeader(HttpHeaders.HEADER_CACHE_CONTROL); will(returnValue(null));
            oneOf(serverSession).setWriteHeader(HttpHeaders.HEADER_CACHE_CONTROL, "no-cache");
            oneOf(serverSession).getFilterChain(); will(returnValue(filterChain));
            oneOf(filterChain).addFirst(with(equal("http#content-length")), with(aNonNull(HttpContentLengthAdjustmentFilter.class)));
            oneOf(nextFilter).filterWrite(with(serverSession), with(hasMessage(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(CLIENT_NOT_FOUND);

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(false);
        filter.filterWrite(nextFilter, serverSession, new DefaultWriteRequest(httpResponse));

        context.assertIsSatisfied();
    }

    @Test
    public void shouldWriteResponseWithoutInsertingStatusClientUnauthorized() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(CLIENT_UNAUTHORIZED);
        expectedResponse.setHeader("WWW-Authenticate", "Basic realm=\"WallyWorld\"");

        context.checking(new Expectations() { {
            oneOf(serverSession).setVersion(HTTP_1_1);
            oneOf(serverSession).setStatus(SUCCESS_OK);
            oneOf(serverSession).setWriteHeader("Content-Type", "text/plain;charset=UTF-8");
            oneOf(serverSession).getStatus(); will(returnValue(SUCCESS_OK));
            oneOf(serverSession).getWriteHeader(HttpHeaders.HEADER_CACHE_CONTROL); will(returnValue(null));
            oneOf(serverSession).setWriteHeader(HttpHeaders.HEADER_CACHE_CONTROL, "no-cache");

            oneOf(serverSession).getFilterChain(); will(returnValue(filterChain));
            oneOf(filterChain).addFirst(with(equal("http#content-length")), with(aNonNull(HttpContentLengthAdjustmentFilter.class)));
            oneOf(nextFilter).filterWrite(with(serverSession), with(hasMessage(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(CLIENT_UNAUTHORIZED);
        httpResponse.setHeader("WWW-Authenticate", "Basic realm=\"WallyWorld\"");

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(false);
        filter.filterWrite(nextFilter, serverSession, new DefaultWriteRequest(httpResponse));

        context.assertIsSatisfied();
    }

    @Test
    public void shouldWriteResponseWithoutInsertingStatusRedirectFound() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(REDIRECT_FOUND);
        expectedResponse.setHeader("Location", "https://www.w3.org/");

        context.checking(new Expectations() { {
            oneOf(serverSession).setVersion(HTTP_1_1);
            oneOf(serverSession).setStatus(SUCCESS_OK);
            oneOf(serverSession).setWriteHeader("Content-Type", "text/plain;charset=UTF-8");
            oneOf(serverSession).getStatus(); will(returnValue(SUCCESS_OK));
            oneOf(serverSession).getWriteHeader(HttpHeaders.HEADER_CACHE_CONTROL); will(returnValue(null));
            oneOf(serverSession).setWriteHeader(HttpHeaders.HEADER_CACHE_CONTROL, "no-cache");
            oneOf(serverSession).getFilterChain(); will(returnValue(filterChain));
            oneOf(filterChain).addFirst(with(equal("http#content-length")), with(aNonNull(HttpContentLengthAdjustmentFilter.class)));
            oneOf(nextFilter).filterWrite(with(serverSession), with(hasMessage(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(REDIRECT_FOUND);
        httpResponse.setHeader("Location", "https://www.w3.org/");

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(false);
        filter.filterWrite(nextFilter, serverSession, new DefaultWriteRequest(httpResponse));

        context.assertIsSatisfied();
    }


    private static HttpBufferAllocator httpBufferAllocator = new HttpBufferAllocator(SimpleBufferAllocator.BUFFER_ALLOCATOR);

    private static IoBufferEx wrap(byte[] array) {
        return httpBufferAllocator.wrap(ByteBuffer.wrap(array));
    }

    @Test
    public void shouldWriteResponseWithoutInsertingStatusRedirectMultipleChoices() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(REDIRECT_MULTIPLE_CHOICES);
        expectedResponse.setHeader("Location", "https://www.w3.org/");

        String[] alternatives = new String[] { "https://www.w3.org/", "http://www.w3.org/" };
        byte[] array = join(alternatives, "\n").getBytes(UTF_8);
        final HttpContentMessage expectedContent = new HttpContentMessage(wrap(array), true);
        expectedResponse.setContent(expectedContent);

        context.checking(new Expectations() { {
            oneOf(serverSession).setVersion(HTTP_1_1);
            oneOf(serverSession).setStatus(SUCCESS_OK);
            oneOf(serverSession).setWriteHeader("Content-Type", "text/plain;charset=UTF-8");
            oneOf(serverSession).getStatus(); will(returnValue(SUCCESS_OK));
            oneOf(serverSession).getWriteHeader(HttpHeaders.HEADER_CACHE_CONTROL); will(returnValue(null));
            oneOf(serverSession).setWriteHeader(HttpHeaders.HEADER_CACHE_CONTROL, "no-cache");

            oneOf(serverSession).getFilterChain(); will(returnValue(filterChain));
            oneOf(filterChain).addFirst(with(equal("http#content-length")), with(aNonNull(HttpContentLengthAdjustmentFilter.class)));
            oneOf(nextFilter).filterWrite(with(serverSession), with(hasMessage(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(REDIRECT_MULTIPLE_CHOICES);
        httpResponse.setHeader("Location", "https://www.w3.org/");

        HttpContentMessage httpContent = new HttpContentMessage(wrap(array), true);
        httpResponse.setContent(httpContent);

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(false);
        filter.filterWrite(nextFilter, serverSession, new DefaultWriteRequest(httpResponse));

        context.assertIsSatisfied();
    }

    @Test
    public void shouldWriteResponseWithInsertedCookies() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(REDIRECT_FOUND);
        expectedResponse.setReason("Cross-Origin Redirect");
        expectedResponse.setHeader("Location", "https://www.w3.org/");

        context.checking(new Expectations() { {
            oneOf(serverSession).setVersion(HTTP_1_1);
            oneOf(serverSession).setStatus(SUCCESS_OK);
            oneOf(serverSession).getStatus(); will(returnValue(SUCCESS_OK));
            oneOf(serverSession).getWriteHeader(HttpHeaders.HEADER_CACHE_CONTROL); will(returnValue(null));
            oneOf(serverSession).setWriteHeader(HttpHeaders.HEADER_CACHE_CONTROL, "no-cache");
            oneOf(serverSession).setWriteHeader("Content-Type", "text/plain;charset=UTF-8");
            oneOf(serverSession).setWriteHeaders(with(stringMatching("Set-Cookie")), with(stringListMatching("KSSOID=12345;")));

            oneOf(serverSession).getFilterChain(); will(returnValue(filterChain));
            oneOf(filterChain).addFirst(with(equal("http#content-length")), with(aNonNull(HttpContentLengthAdjustmentFilter.class)));
            oneOf(nextFilter).filterWrite(with(serverSession), with(hasMessage(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(REDIRECT_FOUND);
        httpResponse.setReason("Cross-Origin Redirect");
        httpResponse.setHeader("Location", "https://www.w3.org/");
        httpResponse.setHeader("Set-Cookie", "KSSOID=12345;");

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(false);
        filter.filterWrite(nextFilter, serverSession, new DefaultWriteRequest(httpResponse));

        context.assertIsSatisfied();
    }

    @Test
    public void shouldWriteResponseWithInsertedTextPlainContentType() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(SUCCESS_OK);
        expectedResponse.setHeader("Content-Type", "text/plain");

        context.checking(new Expectations() { {
            oneOf(serverSession).setVersion(HTTP_1_1);
            oneOf(serverSession).setStatus(SUCCESS_OK);
            oneOf(serverSession).setWriteHeader("Content-Type", "text/plain");
            oneOf(serverSession).getStatus(); will(returnValue(SUCCESS_OK));

            oneOf(serverSession).getFilterChain(); will(returnValue(filterChain));
            oneOf(filterChain).addFirst(with(equal("http#content-length")), with(aNonNull(HttpContentLengthAdjustmentFilter.class)));
            oneOf(nextFilter).filterWrite(with(serverSession), with(hasMessage(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(SUCCESS_OK);
        httpResponse.setHeader("Content-Type", "text/plain");

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(false);
        filter.filterWrite(nextFilter, serverSession, new DefaultWriteRequest(httpResponse));

        context.assertIsSatisfied();
    }

    @Test
    public void shouldWriteResponseWithTextContentTypeInsertedAsTextPlain() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(SUCCESS_OK);
        expectedResponse.setHeader("Content-Type", "text/xyz;charset=windows-1252");

        context.checking(new Expectations() { {
            oneOf(serverSession).setVersion(HTTP_1_1);
            oneOf(serverSession).setStatus(SUCCESS_OK);
            oneOf(serverSession).setWriteHeader("Content-Type", "text/plain;charset=windows-1252");
            oneOf(serverSession).getStatus(); will(returnValue(SUCCESS_OK));

            oneOf(serverSession).getFilterChain(); will(returnValue(filterChain));
            oneOf(filterChain).addFirst(with(equal("http#content-length")), with(aNonNull(HttpContentLengthAdjustmentFilter.class)));
            oneOf(nextFilter).filterWrite(with(serverSession), with(hasMessage(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(SUCCESS_OK);
        httpResponse.setHeader("Content-Type", "text/xyz;charset=windows-1252");

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(false);
        filter.filterWrite(nextFilter, serverSession, new DefaultWriteRequest(httpResponse));

        context.assertIsSatisfied();
    }

    @Test
    public void shouldWriteResponseWithInsertedAccessControlAllowHeaders() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(SUCCESS_OK);
        expectedResponse.setHeader("Content-Type", "text/xyz;charset=windows-1252");

        context.checking(new Expectations() { {
            oneOf(serverSession).setVersion(HTTP_1_1);
            oneOf(serverSession).setStatus(SUCCESS_OK);
            oneOf(serverSession).setWriteHeader("Content-Type", "text/plain;charset=windows-1252");
            oneOf(serverSession).setWriteHeaders(with(stringMatching("Access-Control-Allow-Headers")), with(stringListMatching("x-websocket-extensions")));
            oneOf(serverSession).getStatus(); will(returnValue(SUCCESS_OK));

            oneOf(serverSession).getFilterChain(); will(returnValue(filterChain));
            oneOf(filterChain).addFirst(with(equal("http#content-length")), with(aNonNull(HttpContentLengthAdjustmentFilter.class)));
            oneOf(nextFilter).filterWrite(with(serverSession), with(hasMessage(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(SUCCESS_OK);
        httpResponse.setHeader("Content-Type", "text/xyz;charset=windows-1252");
        httpResponse.setHeader("Access-Control-Allow-Headers", "x-websocket-extensions");

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(false);
        filter.filterWrite(nextFilter, serverSession, new DefaultWriteRequest(httpResponse));

        context.assertIsSatisfied();
    }

    @Test
    public void shouldWriteResponseWithInsertedContentEncoding() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(SUCCESS_OK);
        expectedResponse.setHeader("Content-Type", "text/xyz;charset=windows-1252");

        context.checking(new Expectations() { {
            oneOf(serverSession).setVersion(HTTP_1_1);
            oneOf(serverSession).setStatus(SUCCESS_OK);
            oneOf(serverSession).setWriteHeader("Content-Type", "text/plain;charset=windows-1252");
            oneOf(serverSession).setWriteHeaders(with(stringMatching("Content-Encoding")), with(stringListMatching("gzip")));
            oneOf(serverSession).getStatus(); will(returnValue(SUCCESS_OK));

            oneOf(nextFilter).filterWrite(with(serverSession), with(hasMessage(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(SUCCESS_OK);
        httpResponse.setHeader("Content-Type", "text/xyz;charset=windows-1252");
        httpResponse.setHeader("Content-Encoding", "gzip");

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(false);
        filter.filterWrite(nextFilter, serverSession, new DefaultWriteRequest(httpResponse));

        context.assertIsSatisfied();
    }

    @Test
    public void shouldWriteResponseWithInsertedCacheControl() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(REDIRECT_FOUND);
        expectedResponse.setReason("Cross-Origin Redirect");
        expectedResponse.setHeader("Location", "https://www.w3.org/");

        context.checking(new Expectations() { {
            oneOf(serverSession).setVersion(HTTP_1_1);
            oneOf(serverSession).setStatus(SUCCESS_OK);
            oneOf(serverSession).setWriteHeader("Content-Type", "text/plain;charset=UTF-8");
            oneOf(serverSession).setWriteHeaders(with(stringMatching("Cache-Control")), with(stringListMatching("private")));
            oneOf(serverSession).getStatus(); will(returnValue(SUCCESS_OK));
            oneOf(serverSession).getWriteHeader(HttpHeaders.HEADER_CACHE_CONTROL); will(returnValue("private"));

            oneOf(serverSession).getFilterChain(); will(returnValue(filterChain));
            oneOf(filterChain).addFirst(with(equal("http#content-length")), with(aNonNull(HttpContentLengthAdjustmentFilter.class)));
            oneOf(nextFilter).filterWrite(with(serverSession), with(hasMessage(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(HttpStatus.REDIRECT_FOUND);
        httpResponse.setReason("Cross-Origin Redirect");
        httpResponse.setHeader("Cache-Control", "private");
        httpResponse.setHeader("Location", "https://www.w3.org/");

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(false);
        filter.filterWrite(nextFilter, serverSession, new DefaultWriteRequest(httpResponse));

        context.assertIsSatisfied();
    }

    @Test
    public void shouldWriteResponseWithInsertedContentTypeApplicationOctetStream() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(SUCCESS_OK);
        expectedResponse.setHeader("Content-Type", "application/octet-stream");

        context.checking(new Expectations() { {
            oneOf(serverSession).setVersion(HTTP_1_1);
            oneOf(serverSession).setStatus(SUCCESS_OK);
            oneOf(serverSession).setWriteHeader("Content-Type", "application/octet-stream");
            oneOf(serverSession).getStatus(); will(returnValue(SUCCESS_OK));

            oneOf(serverSession).getFilterChain(); will(returnValue(filterChain));
            oneOf(filterChain).addFirst(with(equal("http#content-length")), with(aNonNull(HttpContentLengthAdjustmentFilter.class)));
            oneOf(nextFilter).filterWrite(with(serverSession), with(hasMessage(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(SUCCESS_OK);
        httpResponse.setHeader("Content-Type", "application/octet-stream");

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(false);
        filter.filterWrite(nextFilter, serverSession, new DefaultWriteRequest(httpResponse));

        context.assertIsSatisfied();
    }

    @Test
    public void shouldWriteResponseWithIncompleteContent() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(SUCCESS_OK);
        expectedResponse.setHeader("Content-Type", "text/plain;charset=UTF-8");

        byte[] array = "Hello, world".getBytes(UTF_8);
        HttpContentMessage expectedContent = new HttpContentMessage(wrap(array), false);
        expectedResponse.setContent(expectedContent);

        context.checking(new Expectations() { {
            allowing(serverSession).getWriteHeader("Content-Length"); will(returnValue(null));
            allowing(serverSession).setWriteHeader("Content-Length", "0");

            oneOf(serverSession).setVersion(HTTP_1_1);
            oneOf(serverSession).setStatus(SUCCESS_OK);
            oneOf(serverSession).setWriteHeader("Content-Type", "text/plain;charset=UTF-8");
            oneOf(serverSession).getStatus(); will(returnValue(SUCCESS_OK));

            oneOf(nextFilter).filterWrite(with(serverSession), with(hasMessage(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(SUCCESS_OK);
        httpResponse.setHeader("Content-Type", "text/plain;charset=UTF-8");

        HttpContentMessage httpContent = new HttpContentMessage(wrap(array), false);
        httpResponse.setContent(httpContent);

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(false);
        filter.filterWrite(nextFilter, serverSession, new DefaultWriteRequest(httpResponse));

        context.assertIsSatisfied();
    }

    @Test
    public void shouldWriteResponseWithInsertedTransferEncodingChunked() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(SUCCESS_OK);
        expectedResponse.setHeader("Content-Type", "text/plain;charset=UTF-8");

        byte[] array = "Hello, world".getBytes(UTF_8);
        HttpContentMessage expectedContent = new HttpContentMessage(wrap(array), false, true, false);
        expectedResponse.setContent(expectedContent);

        context.checking(new Expectations() { {
            allowing(serverSession).getWriteHeader("Content-Length"); will(returnValue(null));
            allowing(serverSession).setWriteHeader("Content-Length", "0");

            oneOf(serverSession).setVersion(HTTP_1_1);
            oneOf(serverSession).setStatus(SUCCESS_OK);
            oneOf(serverSession).setWriteHeaders(with(stringMatching("Transfer-Encoding")), with(stringListMatching("chunked")));
            oneOf(serverSession).setWriteHeader("Content-Type", "text/plain;charset=UTF-8");
            oneOf(serverSession).getStatus(); will(returnValue(SUCCESS_OK));

            oneOf(nextFilter).filterWrite(with(serverSession), with(hasMessage(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(SUCCESS_OK);
        httpResponse.setHeader("Content-Type", "text/plain;charset=UTF-8");
        httpResponse.setHeader("Transfer-Encoding", "chunked");

        HttpContentMessage httpContent = new HttpContentMessage(wrap(array), false, true, false);
        httpResponse.setContent(httpContent);

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(false);
        filter.filterWrite(nextFilter, serverSession, new DefaultWriteRequest(httpResponse));

        context.assertIsSatisfied();
    }

    @Test
    public void shouldWriteResponseWithCompleteContent() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(SUCCESS_OK);
        expectedResponse.setHeader("Content-Type", "text/plain;charset=UTF-8");

        byte[] array = "Hello, world".getBytes(UTF_8);
        HttpContentMessage expectedContent = new HttpContentMessage(wrap(array), true);
        expectedResponse.setContent(expectedContent);

        context.checking(new Expectations() { {
            allowing(serverSession).getWriteHeader("Content-Length"); will(returnValue(null));
            allowing(serverSession).setWriteHeader("Content-Length", "0");

            oneOf(serverSession).setVersion(HTTP_1_1);
            oneOf(serverSession).setStatus(SUCCESS_OK);
            oneOf(serverSession).setWriteHeader("Content-Type", "text/plain;charset=UTF-8");
            oneOf(serverSession).getStatus(); will(returnValue(SUCCESS_OK));

            oneOf(serverSession).getFilterChain(); will(returnValue(filterChain));
            oneOf(filterChain).addFirst(with(equal("http#content-length")), with(aNonNull(HttpContentLengthAdjustmentFilter.class)));
            oneOf(nextFilter).filterWrite(with(serverSession), with(hasMessage(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(SUCCESS_OK);
        httpResponse.setHeader("Content-Type", "text/plain;charset=UTF-8");

        HttpContentMessage httpContent = new HttpContentMessage(wrap(array), true);
        httpResponse.setContent(httpContent);

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(false);
        filter.filterWrite(nextFilter, serverSession, new DefaultWriteRequest(httpResponse));

        context.assertIsSatisfied();
    }

    @Test
    public void shouldWriteResponseAfterPrependingContentLengthFilter() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(SUCCESS_OK);
        expectedResponse.setContent(new HttpContentMessage(wrap(new byte[0]), false));

        byte[] array = "Hello, world".getBytes(UTF_8);
        final HttpContentMessage expectedContent = new HttpContentMessage(wrap(array), true);
        expectedResponse.setContent(expectedContent);

        context.checking(new Expectations() { {
            oneOf(serverSession).setVersion(HTTP_1_1);
            oneOf(serverSession).setStatus(SUCCESS_OK);
            oneOf(serverSession).setWriteHeader("Content-Type", "text/plain;charset=UTF-8");
            oneOf(serverSession).setWriteHeaders(with(stringMatching("Content-Length")), with(stringListMatching("12")));
            allowing(serverSession).getWriteHeader("Content-Length"); will(returnValue("12"));
            oneOf(serverSession).getStatus(); will(returnValue(SUCCESS_OK));

            oneOf(serverSession).getFilterChain(); will(returnValue(filterChain));
            oneOf(filterChain).addFirst(with(equal("http#content-length")), with(aNonNull(HttpContentLengthAdjustmentFilter.class)));
            oneOf(nextFilter).filterWrite(with(serverSession), with(hasMessage(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(SUCCESS_OK);
        httpResponse.setHeader("Content-Length", "12");

        HttpContentMessage httpContent = new HttpContentMessage(wrap(array), true);
        httpResponse.setContent(httpContent);

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(false);
        filter.filterWrite(nextFilter, serverSession, new DefaultWriteRequest(httpResponse));

        context.assertIsSatisfied();
    }


    @Test
    public void shouldReceivePostRequestWithExtractedHeadersAndContent() throws Exception {

        byte[] array = ">|<".getBytes(UTF_8);

        final HttpRequestMessage expectedRequest = new HttpRequestMessage();
        expectedRequest.setVersion(HTTP_1_1);
        expectedRequest.setMethod(POST);
        expectedRequest.setRequestURI(URI.create("/kerberos5/;api/get-cookies?.kv=10.05"));
        expectedRequest.setHeader("Accept", "*/*");
        expectedRequest.setHeader("Accept-Language", "en-us");
        expectedRequest.addHeader("Accept-Language", "fr-fr");
        expectedRequest.setHeader("Content-Length", "3");
        expectedRequest.setHeader("Content-Type", "text/plain");
        expectedRequest.setHeader("Host", "gateway.kzng.net:8003");
        expectedRequest.setHeader("Referer", "http://gateway.kzng.net:8003/?.kr=xsa");
        expectedRequest.setHeader("User-Agent", "Shockwave Flash");
        expectedRequest.setHeader("X-Origin", "http://gateway.kzng.net:8000");
        expectedRequest.setHeader("x-flash-version", "9,0,124,0");
        expectedRequest.setContent(new HttpContentMessage(wrap(array), true));

        context.checking(new Expectations() { {
            oneOf(serverSession).getVersion(); will(returnValue(HTTP_1_1));
            oneOf(serverSession).getMethod(); will(returnValue(POST));
            oneOf(serverSession).getRequestURI(); will(returnValue(URI.create("/kerberos5/;api/get-cookies?.kv=10.05")));
            oneOf(serverSession).getReadHeaderNames(); will(returnValue(asList("Accept", "Accept-Language", "Content-Length", "Content-Type", "Host", "X-Origin", "Referer", "User-Agent", "x-flash-version")));
            oneOf(serverSession).getReadHeaders(with("Accept")); will(returnValue(Collections.singletonList("*/*")));
            oneOf(serverSession).getReadHeaders(with("Accept-Language")); will(returnValue(asList("en-us","fr-fr")));
            oneOf(serverSession).getReadHeader(with("Content-Type")); will(returnValue("application/x-message-http"));
            oneOf(serverSession).getReadHeaders(with("Host")); will(returnValue(Collections.singletonList("gateway.kzng.net:8003")));
            oneOf(serverSession).getReadHeaders(with("Referer")); will(returnValue(Collections.singletonList("http://gateway.kzng.net:8003/?.kr=xsa")));
            oneOf(serverSession).getReadHeaders(with("User-Agent")); will(returnValue(Collections.singletonList("Shockwave Flash")));
            oneOf(serverSession).getReadHeaders(with("X-Origin")); will(returnValue(Collections.singletonList("http://gateway.kzng.net:8000")));
            oneOf(serverSession).getReadHeaders(with("x-flash-version")); will(returnValue(Collections.singletonList("9,0,124,0")));
            oneOf(serverSession).getReadCookies(); will(returnValue(emptyList()));

            oneOf(nextFilter).messageReceived(with(serverSession), with(equal(expectedRequest)));
        } });

        HttpRequestMessage httpRequest = new HttpRequestMessage();
        httpRequest.setVersion(HTTP_1_1);
        httpRequest.setMethod(POST);
        httpRequest.setRequestURI(URI.create("/kerberos5/;api/get-cookies?.kv=10.05"));
        httpRequest.setHeader("Content-Length", "3");
        httpRequest.setHeader("Content-Type", "text/plain");
        httpRequest.setContent(new HttpContentMessage(wrap(array), true));

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(false);
        filter.messageReceived(nextFilter, serverSession, httpRequest);

        context.assertIsSatisfied();
    }

    @Test
    public void shouldReceiveGetRequestWithExtractedHeadersIncludingMultiValuedHeaderAndCookie() throws Exception {

        final HttpRequestMessage expectedRequest = new HttpRequestMessage();
        expectedRequest.setVersion(HTTP_1_1);
        expectedRequest.setMethod(GET);
        expectedRequest.setRequestURI(URI.create("/"));
        expectedRequest.setHeader("Authorization", "restricted-usage");
        expectedRequest.setHeader("X-Header", "value1");
        expectedRequest.addHeader("X-Header", "value2");
        expectedRequest.addCookie(new DefaultHttpCookie("KSSOID", "0123456789abcdef"));
        expectedRequest.setContent(EMPTY);

        context.checking(new Expectations() { {
            oneOf(serverSession).getVersion(); will(returnValue(HTTP_1_1));
            oneOf(serverSession).getMethod(); will(returnValue(POST));
            oneOf(serverSession).getRequestURI(); will(returnValue(URI.create("/")));
            oneOf(serverSession).getReadHeaderNames(); will(returnValue(asList("Content-Length", "Content-Type", "X-Header")));
            oneOf(serverSession).getReadHeader(with("Content-Type")); will(returnValue("application/x-message-http"));
            oneOf(serverSession).getReadHeaders(with("X-Header")); will(returnValue(asList("value1","value2")));
            oneOf(serverSession).getReadCookies(); will(returnValue(Collections.singletonList(new DefaultHttpCookie("KSSOID", "0123456789abcdef"))));

            oneOf(nextFilter).messageReceived(with(serverSession), with(equal(expectedRequest)));
        } });

        HttpRequestMessage httpRequest = new HttpRequestMessage();
        httpRequest.setVersion(HTTP_1_1);
        httpRequest.setMethod(GET);
        httpRequest.setRequestURI(URI.create("/"));
        httpRequest.setHeader("Authorization", "restricted-usage");
        httpRequest.setContent(EMPTY);

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(false);
        filter.messageReceived(nextFilter, serverSession, httpRequest);

        context.assertIsSatisfied();
    }

    @Test (expected = ProtocolCodecException.class)
    public void shouldRejectReceivedRequestWithInconsistentURI() throws Exception {
        final HttpRequestMessage expectedRequest = new HttpRequestMessage();
        expectedRequest.setVersion(HTTP_1_1);
        expectedRequest.setMethod(GET);
        expectedRequest.setRequestURI(URI.create("/"));
        expectedRequest.setContent(EMPTY);

        context.checking(new Expectations() { {
            oneOf(serverSession).getVersion(); will(returnValue(HTTP_1_1));
            oneOf(serverSession).getMethod(); will(returnValue(POST));
            oneOf(serverSession).getRequestURI(); will(returnValue(URI.create("/")));
            oneOf(serverSession).getReadHeaderNames(); will(returnValue(asList("Content-Length", "Content-Type")));
            oneOf(serverSession).getReadHeader(with("Content-Length")); will(returnValue("102"));
            oneOf(serverSession).getReadHeader(with("Content-Type")); will(returnValue("application/x-message-http"));
            oneOf(serverSession).getStatus(); will(returnValue(SUCCESS_OK));

            oneOf(nextFilter).messageReceived(with(serverSession), with(equal(expectedRequest)));
        } });

        HttpRequestMessage httpRequest = new HttpRequestMessage();
        httpRequest.setVersion(HTTP_1_1);
        httpRequest.setMethod(GET);
        httpRequest.setRequestURI(URI.create("/different/path"));
        httpRequest.setContent(EMPTY);

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(false);
        filter.messageReceived(nextFilter, serverSession, httpRequest);

        context.assertIsSatisfied();
    }

    @Test (expected = ProtocolCodecException.class)
    public void shouldRejectReceivedRequestWithInconsistentVersion() throws Exception {
        final HttpRequestMessage expectedRequest = new HttpRequestMessage();
        expectedRequest.setVersion(HTTP_1_1);
        expectedRequest.setMethod(GET);
        expectedRequest.setRequestURI(URI.create("/"));
        expectedRequest.setContent(EMPTY);

        context.checking(new Expectations() { {
            oneOf(serverSession).getVersion(); will(returnValue(HTTP_1_1));
            oneOf(serverSession).getMethod(); will(returnValue(POST));
            oneOf(serverSession).getRequestURI(); will(returnValue(URI.create("/")));
            oneOf(serverSession).getReadHeaderNames(); will(returnValue(asList("Content-Length", "Content-Type")));
            oneOf(serverSession).getReadHeader(with("Content-Length")); will(returnValue("102"));
            oneOf(serverSession).getReadHeader(with("Content-Type")); will(returnValue("application/x-message-http"));
            oneOf(serverSession).getStatus(); will(returnValue(SUCCESS_OK));

            oneOf(nextFilter).messageReceived(with(serverSession), with(equal(expectedRequest)));
        } });

        HttpRequestMessage httpRequest = new HttpRequestMessage();
        httpRequest.setVersion(HTTP_1_0);
        httpRequest.setMethod(GET);
        httpRequest.setRequestURI(URI.create("/"));
        httpRequest.setContent(EMPTY);

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(false);
        filter.messageReceived(nextFilter, serverSession, httpRequest);

        context.assertIsSatisfied();
    }

    @Test (expected = ProtocolCodecException.class)
    public void shouldRejectReceivedRequestWithInvalidHeader() throws Exception {
        final HttpRequestMessage expectedRequest = new HttpRequestMessage();
        expectedRequest.setVersion(HTTP_1_1);
        expectedRequest.setMethod(GET);
        expectedRequest.setRequestURI(URI.create("/"));
        expectedRequest.setContent(EMPTY);

        context.checking(new Expectations() { {
            oneOf(serverSession).getVersion(); will(returnValue(HTTP_1_1));
            oneOf(serverSession).getMethod(); will(returnValue(POST));
            oneOf(serverSession).getRequestURI(); will(returnValue(URI.create("/")));
            oneOf(serverSession).getReadHeaderNames(); will(returnValue(asList("Content-Length", "Content-Type")));
            oneOf(serverSession).getReadHeader(with("Content-Length")); will(returnValue("102"));
            oneOf(serverSession).getReadHeader(with("Content-Type")); will(returnValue("application/x-message-http"));
            oneOf(serverSession).getStatus(); will(returnValue(SUCCESS_OK));

            oneOf(nextFilter).messageReceived(with(serverSession), with(equal(expectedRequest)));
        } });

        HttpRequestMessage httpRequest = new HttpRequestMessage();
        httpRequest.setVersion(HTTP_1_1);
        httpRequest.setMethod(GET);
        httpRequest.setRequestURI(URI.create("/"));
        httpRequest.setHeader("Accept-Charset", "value");
        httpRequest.setContent(EMPTY);

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(false);
        filter.messageReceived(nextFilter, serverSession, httpRequest);

        context.assertIsSatisfied();
    }

    @Test
    public void shouldReceiveResponseWithStatusRedirectNotModified() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(REDIRECT_NOT_MODIFIED);

        context.checking(new Expectations() { {
            allowing(clientSession).getVersion(); will(returnValue(HTTP_1_1));
            allowing(clientSession).getStatus(); will(returnValue(SUCCESS_OK));
            allowing(clientSession).getReadHeaderNames(); will(returnValue(emptyList()));
            allowing(clientSession).getReadCookies(); will(returnValue(emptyList()));

            oneOf(nextFilter).messageReceived(with(clientSession), with(equal(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(REDIRECT_NOT_MODIFIED);

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(true);
        filter.messageReceived(nextFilter, clientSession, httpResponse);

        context.assertIsSatisfied();
    }

    @Test
    public void shouldReceiveResponseWithStatusClientUnauthorized() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(CLIENT_UNAUTHORIZED);
        expectedResponse.setHeader("WWW-Autheticate", "Basic realm=\"WallyWorld\"");

        context.checking(new Expectations() { {
            allowing(clientSession).getVersion(); will(returnValue(HTTP_1_1));
            allowing(clientSession).getStatus(); will(returnValue(SUCCESS_OK));
            allowing(clientSession).getReadHeaderNames(); will(returnValue(emptyList()));
            allowing(clientSession).getReadCookies(); will(returnValue(emptyList()));

            oneOf(nextFilter).messageReceived(with(clientSession), with(equal(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(CLIENT_UNAUTHORIZED);
        httpResponse.setHeader("WWW-Autheticate", "Basic realm=\"WallyWorld\"");

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(true);
        filter.messageReceived(nextFilter, clientSession, httpResponse);

        context.assertIsSatisfied();
    }

    @Test
    public void shouldReceiveResponseWithExtractedCookies() throws Exception {

        final List<HttpCookie> expectedCookies =
                Collections.singletonList(new DefaultHttpCookie("KSSOID", "12345"));

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(REDIRECT_FOUND);
        expectedResponse.setReason("Cross-Origin Redirect");
        expectedResponse.setHeader("Location", "https://www.w3.org/");
        expectedResponse.setCookies(expectedCookies);

        context.checking(new Expectations() { {
            allowing(clientSession).getVersion(); will(returnValue(HTTP_1_1));
            allowing(clientSession).getStatus(); will(returnValue(SUCCESS_OK));
            allowing(clientSession).getReadHeaderNames(); will(returnValue(Collections.<String>emptySet()));

            oneOf(clientSession).getReadCookies(); will(returnValue(expectedCookies));

            oneOf(nextFilter).messageReceived(with(clientSession), with(equal(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(REDIRECT_FOUND);
        httpResponse.setReason("Cross-Origin Redirect");
        httpResponse.setHeader("Location", "https://www.w3.org/");

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(true);
        filter.messageReceived(nextFilter, clientSession, httpResponse);

        context.assertIsSatisfied();
    }

    @Test
    public void shouldReceiveResponseWithContentTypeTextPlain() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(SUCCESS_OK);
        expectedResponse.setHeader("Content-Type", "text/plain");

        context.checking(new Expectations() { {
            allowing(clientSession).getVersion(); will(returnValue(HTTP_1_1));
            allowing(clientSession).getStatus(); will(returnValue(SUCCESS_OK));
            allowing(clientSession).getReadCookies(); will(returnValue(emptyList()));

            oneOf(clientSession).getReadHeaderNames(); will(returnValue(Collections.singletonList("Content-Type")));
            oneOf(clientSession).getReadHeader("Content-Type"); will(returnValue("text/plain"));

            oneOf(nextFilter).messageReceived(with(clientSession), with(equal(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(SUCCESS_OK);
        httpResponse.setHeader("Content-Type", "text/plain");

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(true);
        filter.messageReceived(nextFilter, clientSession, httpResponse);

        context.assertIsSatisfied();
    }

    @Test
    public void shouldReceiveResponseWithTextContentTypeInsertedAsTextPlain() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(SUCCESS_OK);
        expectedResponse.setHeader("Content-Type", "text/xyz;charset=windows-1252");

        context.checking(new Expectations() { {
            allowing(clientSession).getVersion(); will(returnValue(HTTP_1_1));
            allowing(clientSession).getStatus(); will(returnValue(SUCCESS_OK));
            allowing(clientSession).getReadCookies(); will(returnValue(emptyList()));

            oneOf(clientSession).getReadHeaderNames(); will(returnValue(Collections.singletonList("Content-Type")));
            oneOf(clientSession).getReadHeader("Content-Type"); will(returnValue("text/plain;charset=windows-1252"));

            oneOf(nextFilter).messageReceived(with(clientSession), with(equal(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(SUCCESS_OK);
        httpResponse.setHeader("Content-Type", "text/xyz;charset=windows-1252");

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(true);
        filter.messageReceived(nextFilter, clientSession, httpResponse);

        context.assertIsSatisfied();
    }

    @Test (expected = ProtocolCodecException.class)
    public void shouldRejectReceivedResponseWithIncompatibleTextContentType() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(SUCCESS_OK);
        expectedResponse.setHeader("Content-Type", "text/xyz;charset=windows-1252");

        context.checking(new Expectations() { {
            allowing(clientSession).getVersion(); will(returnValue(HTTP_1_1));
            allowing(clientSession).getStatus(); will(returnValue(SUCCESS_OK));
            allowing(clientSession).getReadCookies(); will(returnValue(emptyList()));

            oneOf(clientSession).getReadHeaderNames(); will(returnValue(Collections.singletonList("Content-Type")));
            oneOf(clientSession).getReadHeader("Content-Type"); will(returnValue("text/pdf"));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(SUCCESS_OK);
        httpResponse.setHeader("Content-Type", "text/xyz;charset=windows-1252");

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(true);
        filter.messageReceived(nextFilter, clientSession, httpResponse);

        context.assertIsSatisfied();
    }

    @Test
    public void shouldReceiveResponseWithExtractedAccessControlAllowHeaders() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(SUCCESS_OK);
        expectedResponse.setHeader("Content-Type", "text/xyz;charset=windows-1252");
        expectedResponse.setHeader("Access-Control-Allow-Headers", "x-websocket-extensions");

        context.checking(new Expectations() { {
            allowing(clientSession).getVersion(); will(returnValue(HTTP_1_1));
            allowing(clientSession).getStatus(); will(returnValue(SUCCESS_OK));
            allowing(clientSession).getReadCookies(); will(returnValue(emptyList()));

            oneOf(clientSession).getReadHeaderNames(); will(returnValue(asList("Content-Type", "Access-Control-Allow-Headers")));
            oneOf(clientSession).getReadHeader("Content-Type"); will(returnValue("text/plain;charset=windows-1252"));
            oneOf(clientSession).getReadHeader("Access-Control-Allow-Headers"); will(returnValue("x-websocket-extensions"));

            oneOf(nextFilter).messageReceived(with(clientSession), with(equal(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(SUCCESS_OK);
        httpResponse.setHeader("Content-Type", "text/xyz;charset=windows-1252");

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(true);
        filter.messageReceived(nextFilter, clientSession, httpResponse);

        context.assertIsSatisfied();
    }

    @Test
    public void shouldReceiveResponseWithExtractedContentEncoding() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(SUCCESS_OK);
        expectedResponse.setHeader("Content-Type", "text/xyz;charset=windows-1252");
        expectedResponse.setHeader("Content-Encoding", "gzip");

        context.checking(new Expectations() { {
            allowing(clientSession).getVersion(); will(returnValue(HTTP_1_1));
            allowing(clientSession).getStatus(); will(returnValue(SUCCESS_OK));
            allowing(clientSession).getReadCookies(); will(returnValue(emptyList()));

            oneOf(clientSession).getReadHeaderNames(); will(returnValue(asList("Content-Type", "Content-Encoding")));
            oneOf(clientSession).getReadHeader("Content-Type"); will(returnValue("text/plain;charset=windows-1252"));
            oneOf(clientSession).getReadHeader("Content-Encoding"); will(returnValue("gzip"));

            oneOf(nextFilter).messageReceived(with(clientSession), with(equal(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(SUCCESS_OK);
        httpResponse.setHeader("Content-Type", "text/xyz;charset=windows-1252");

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(true);
        filter.messageReceived(nextFilter, clientSession, httpResponse);

        context.assertIsSatisfied();
    }

    @Test
    public void shouldReceiveResponseWithExtractedCacheControl() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(REDIRECT_FOUND);
        expectedResponse.setHeader("Location", "https://www.w3.org/");
        expectedResponse.setHeader("Cache-Control", "private");

        context.checking(new Expectations() { {
            allowing(clientSession).getVersion(); will(returnValue(HTTP_1_1));
            allowing(clientSession).getStatus(); will(returnValue(SUCCESS_OK));
            allowing(clientSession).getReadCookies(); will(returnValue(emptyList()));

            oneOf(clientSession).getReadHeaderNames(); will(returnValue(asList("Content-Type", "Cache-Control")));
            oneOf(clientSession).getReadHeader("Content-Type"); will(returnValue("text/plain;charset=windows-1252"));
            oneOf(clientSession).getReadHeader("Cache-Control"); will(returnValue("private"));

            oneOf(nextFilter).messageReceived(with(clientSession), with(equal(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(REDIRECT_FOUND);
        httpResponse.setHeader("Location", "https://www.w3.org/");

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(true);
        filter.messageReceived(nextFilter, clientSession, httpResponse);

        context.assertIsSatisfied();
    }

    @Test
    public void shouldReceiveResponseWithExtractedContentTypeApplicationOctetStream() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(SUCCESS_OK);
        expectedResponse.setHeader("Content-Type", "application/octet-stream");
        expectedResponse.setHeader("Content-Length", "0");

        context.checking(new Expectations() { {
            allowing(clientSession).getVersion(); will(returnValue(HTTP_1_1));
            allowing(clientSession).getStatus(); will(returnValue(SUCCESS_OK));
            allowing(clientSession).getReadCookies(); will(returnValue(emptyList()));

            oneOf(clientSession).getReadHeaderNames(); will(returnValue(asList("Content-Type", "Content-Length")));
            oneOf(clientSession).getReadHeader("Content-Type"); will(returnValue("application/octet-stream"));

            oneOf(nextFilter).messageReceived(with(clientSession), with(equal(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(SUCCESS_OK);
        httpResponse.setHeader("Content-Type", "application/octet-stream");
        httpResponse.setHeader("Content-Length", "0");

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(true);
        filter.messageReceived(nextFilter, clientSession, httpResponse);

        context.assertIsSatisfied();
    }

    @Test
    public void shouldReceiveResponseWithIncompleteContent() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(SUCCESS_OK);

        byte[] array = "Hello, world".getBytes(UTF_8);
        HttpContentMessage expectedContent = new HttpContentMessage(wrap(array), false);
        expectedResponse.setContent(expectedContent);

        context.checking(new Expectations() { {
            allowing(clientSession).getVersion(); will(returnValue(HTTP_1_1));
            allowing(clientSession).getStatus(); will(returnValue(SUCCESS_OK));
            allowing(clientSession).getReadCookies(); will(returnValue(emptyList()));

            oneOf(clientSession).getReadHeaderNames(); will(returnValue(emptyList()));
            oneOf(nextFilter).messageReceived(with(clientSession), with(equal(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(SUCCESS_OK);

        HttpContentMessage httpContent = new HttpContentMessage(wrap(array), false);
        httpResponse.setContent(httpContent);

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(true);
        filter.messageReceived(nextFilter, clientSession, httpResponse);

        context.assertIsSatisfied();
    }

    @Test
    public void shouldReceiveResponseWithExtractedTransferEncodingChunked() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(SUCCESS_OK);
        expectedResponse.setHeader("Content-Type", "text/plain;charset=UTF-8");
        expectedResponse.setHeader("Transfer-Encoding", "chunked");

        byte[] array = "Hello, world".getBytes(UTF_8);
        HttpContentMessage expectedContent = new HttpContentMessage(wrap(array), false, true, false);
        expectedResponse.setContent(expectedContent);

        context.checking(new Expectations() { {
            allowing(clientSession).getVersion(); will(returnValue(HTTP_1_1));
            allowing(clientSession).getStatus(); will(returnValue(SUCCESS_OK));
            allowing(clientSession).getReadCookies(); will(returnValue(emptyList()));

            oneOf(clientSession).getReadHeaderNames(); will(returnValue(asList("Transfer-Encoding", "Content-Type")));
            oneOf(clientSession).getReadHeader("Transfer-Encoding"); will(returnValue("chunked"));
            oneOf(clientSession).getReadHeader("Content-Type"); will(returnValue("text/plain;charset=UTF-8"));

            oneOf(nextFilter).messageReceived(with(clientSession), with(equal(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(SUCCESS_OK);
        httpResponse.setHeader("Content-Type", "text/plain;charset=UTF-8");

        HttpContentMessage httpContent = new HttpContentMessage(wrap(array), false, true, false);
        httpResponse.setContent(httpContent);

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(true);
        filter.messageReceived(nextFilter, clientSession, httpResponse);

        context.assertIsSatisfied();
    }

    @Test
    public void shouldReceiveResponseWithCompleteContent() throws Exception {

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(SUCCESS_OK);
        expectedResponse.setHeader("Content-Type", "text/plain;charset=UTF-8");

        byte[] array = "Hello, world".getBytes(UTF_8);
        HttpContentMessage expectedContent = new HttpContentMessage(wrap(array), true);
        expectedResponse.setContent(expectedContent);

        context.checking(new Expectations() { {
            allowing(clientSession).getVersion(); will(returnValue(HTTP_1_1));
            allowing(clientSession).getStatus(); will(returnValue(SUCCESS_OK));
            allowing(clientSession).getReadCookies(); will(returnValue(emptyList()));

            oneOf(clientSession).getReadHeaderNames(); will(returnValue(Collections.singletonList("Content-Type")));
            oneOf(clientSession).getReadHeader("Content-Type"); will(returnValue("text/plain;charset=UTF-8"));

            oneOf(nextFilter).messageReceived(with(clientSession), with(equal(expectedResponse)));
        } });

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HTTP_1_1);
        httpResponse.setStatus(SUCCESS_OK);
        httpResponse.setHeader("Content-Type", "text/plain;charset=UTF-8");

        HttpContentMessage httpContent = new HttpContentMessage(wrap(array), true);
        httpResponse.setContent(httpContent);

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(true);
        filter.messageReceived(nextFilter, clientSession, httpResponse);

        context.assertIsSatisfied();
    }

    @Test
    public void shouldWriteRequestAfterPrependingContentLengthFilter() throws Exception {

        final HttpRequestMessage expectedRequest = new HttpRequestMessage();
        expectedRequest.setVersion(HTTP_1_1);
        expectedRequest.setMethod(POST);
        expectedRequest.setRequestURI(URI.create("/"));
        expectedRequest.setContent(new HttpContentMessage(wrap(new byte[0]), false));

        byte[] array = "Hello, world".getBytes(UTF_8);
        final HttpContentMessage expectedContent = new HttpContentMessage(wrap(array), true);
        expectedRequest.setContent(expectedContent);

        context.checking(new Expectations() { {
            oneOf(clientSession).setVersion(HTTP_1_1);
            oneOf(clientSession).setMethod(POST);
            oneOf(clientSession).setRequestURI(URI.create("/"));
            oneOf(clientSession).setWriteHeader("Content-Type", "text/plain;charset=UTF-8");
            oneOf(clientSession).setWriteHeader("Content-Length", "12");

            oneOf(clientSession).getFilterChain(); will(returnValue(filterChain));
            oneOf(filterChain).addFirst(with("http#content-length"), with(any(HttpContentLengthAdjustmentFilter.class)));
            oneOf(nextFilter).filterWrite(with(clientSession), with(hasMessage(expectedRequest)));
        } });

        HttpRequestMessage httpRequest = new HttpRequestMessage();
        httpRequest.setVersion(HTTP_1_1);
        httpRequest.setMethod(POST);
        httpRequest.setRequestURI(URI.create("/"));
        httpRequest.setHeader("Content-Length", "12");

        HttpContentMessage httpContent = new HttpContentMessage(wrap(array), true);
        httpRequest.setContent(httpContent);

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(true);
        filter.filterWrite(nextFilter, clientSession, new DefaultWriteRequest(httpRequest));

        context.assertIsSatisfied();
    }

    @Test
    public void shouldWritePostRequestWithInsertedHeadersAndContent() throws Exception {

        byte[] array = ">|<".getBytes(UTF_8);

        final HttpRequestMessage expectedRequest = new HttpRequestMessage();
        expectedRequest.setVersion(HTTP_1_1);
        expectedRequest.setMethod(POST);
        expectedRequest.setRequestURI(URI.create("/kerberos5/;api/get-cookies?.kv=10.05"));
        expectedRequest.setHeader("Content-Type", "text/plain");
        expectedRequest.setContent(new HttpContentMessage(wrap(array), true));

        context.checking(new Expectations() { {
            oneOf(clientSession).setVersion(HTTP_1_1);
            oneOf(clientSession).setMethod(POST);
            oneOf(clientSession).setRequestURI(URI.create("/kerberos5/;api/get-cookies?.kv=10.05"));
            oneOf(clientSession).setWriteHeader("Accept", "*/*");
            oneOf(clientSession).setWriteHeader("Accept-Language", "en-us");
            oneOf(clientSession).setWriteHeader("Content-Length", "3");
            oneOf(clientSession).setWriteHeader("Content-Type", "text/plain");
            oneOf(clientSession).setWriteHeader("Host", "gateway.kzng.net:8003");
            oneOf(clientSession).setWriteHeader("Referer", "http://gateway.kzng.net:8003/?.kr=xsa");
            oneOf(clientSession).setWriteHeader("User-Agent", "Shockwave Flash");
            oneOf(clientSession).setWriteHeader("X-Origin", "http://gateway.kzng.net:8000");
            oneOf(clientSession).setWriteHeader("x-flash-version", "9,0,124,0");

            oneOf(clientSession).getFilterChain(); will(returnValue(filterChain));
            oneOf(filterChain).addFirst(with("http#content-length"), with(any(HttpContentLengthAdjustmentFilter.class)));
            oneOf(nextFilter).filterWrite(with(clientSession), with(hasMessage(expectedRequest)));
        } });

        HttpRequestMessage httpRequest = new HttpRequestMessage();
        httpRequest.setVersion(HTTP_1_1);
        httpRequest.setMethod(POST);
        httpRequest.setRequestURI(URI.create("/kerberos5/;api/get-cookies?.kv=10.05"));
        httpRequest.setHeader("Accept", "*/*");
        httpRequest.setHeader("Accept-Language", "en-us");
        httpRequest.setHeader("Content-Length", "3");
        httpRequest.setHeader("Content-Type", "text/plain");
        httpRequest.setHeader("Host", "gateway.kzng.net:8003");
        httpRequest.setHeader("Referer", "http://gateway.kzng.net:8003/?.kr=xsa");
        httpRequest.setHeader("User-Agent", "Shockwave Flash");
        httpRequest.setHeader("X-Origin", "http://gateway.kzng.net:8000");
        httpRequest.setHeader("x-flash-version", "9,0,124,0");
        httpRequest.setContent(new HttpContentMessage(wrap(array), true));

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(true);
        filter.filterWrite(nextFilter, clientSession, new DefaultWriteRequest(httpRequest));

        context.assertIsSatisfied();
    }

    @Test
    public void shouldWriteGetRequestWithInsertedHeadersAndCookies() throws Exception {

        final HttpRequestMessage expectedRequest = new HttpRequestMessage();
        expectedRequest.setVersion(HTTP_1_1);
        expectedRequest.setMethod(GET);
        expectedRequest.setRequestURI(URI.create("/"));
        expectedRequest.setHeader("Authorization", "restricted-usage");
        expectedRequest.setContent(EMPTY);

        context.checking(new Expectations() { {
            oneOf(clientSession).setVersion(HTTP_1_1);
            oneOf(clientSession).setMethod(POST);
            oneOf(clientSession).setRequestURI(URI.create("/"));
            oneOf(clientSession).setWriteHeader("Content-Type", "text/plain;charset=UTF-8");
            oneOf(clientSession).setWriteHeader("X-Header", "value");

            oneOf(nextFilter).filterWrite(with(clientSession), with(hasMessage(expectedRequest)));
        } });

        HttpRequestMessage httpRequest = new HttpRequestMessage();
        httpRequest.setVersion(HTTP_1_1);
        httpRequest.setMethod(GET);
        httpRequest.setRequestURI(URI.create("/"));
        httpRequest.setHeader("Authorization", "restricted-usage");
        httpRequest.setHeader("X-Header", "value");
        httpRequest.setContent(EMPTY);

        HttpxeProtocolFilter filter = new HttpxeProtocolFilter(true);
        filter.filterWrite(nextFilter, clientSession, new DefaultWriteRequest(httpRequest));

        context.assertIsSatisfied();
    }

}
