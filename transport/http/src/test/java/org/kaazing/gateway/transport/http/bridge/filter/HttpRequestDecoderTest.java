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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.junit.Test;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.filter.codec.ProtocolCodecSessionEx;


public class HttpRequestDecoderTest {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    @Test
    public void decodeDefaultContentTypeAndRemoveQueryParameter() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        ProtocolDecoder decoder = new HttpRequestDecoder();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

        ByteBuffer in = ByteBuffer.wrap(("POST /;e/ut?.kct=text/plain;charset=UTF-8 HTTP/1.1\r\n" +
                                         "Host: example.com\r\n" +
                                         "Content-Length: 19\r\n" +
                                         "\r\n" +
                                         "Hello, Content-Type").getBytes());

        IoBufferEx buf = allocator.wrap(in);
        decoder.decode(session, (IoBuffer) buf, session.getDecoderOutput());

        assertFalse(session.getDecoderOutputQueue().isEmpty());
        HttpRequestMessage httpRequest = (HttpRequestMessage)session.getDecoderOutputQueue().poll();
        HttpContentMessage httpContent = httpRequest.getContent();
        assertEquals("/;e/ut", httpRequest.getRequestURI().toASCIIString());
        assertEquals("text/plain;charset=UTF-8", httpRequest.getHeader("Content-Type"));
        assertTrue(httpContent.isComplete());
        assertEquals("Hello, Content-Type", httpContent.asText(UTF_8.newDecoder()));
        assertTrue(httpRequest.getCookies().isEmpty());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        assertFalse(in.hasRemaining());
    }

    @Test
    public void decodeButNotOverrideContentTypeAndRemoveQueryParameter() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        ProtocolDecoder decoder = new HttpRequestDecoder();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

        ByteBuffer in = ByteBuffer.wrap(("POST /;e/ut?.kct=to-be-ignored HTTP/1.1\r\n" +
                                         "Host: example.com\r\n" +
                                         "Content-Type: text/plain;charset=UTF-8\r\n" +
                                         "Content-Length: 19\r\n" +
                                         "\r\n" +
                                         "Hello, Content-Type").getBytes());

        IoBufferEx buf = allocator.wrap(in);
        decoder.decode(session, (IoBuffer) buf, session.getDecoderOutput());

        assertFalse(session.getDecoderOutputQueue().isEmpty());
        HttpRequestMessage httpRequest = (HttpRequestMessage)session.getDecoderOutputQueue().poll();
        HttpContentMessage httpContent = httpRequest.getContent();
        assertEquals("/;e/ut", httpRequest.getRequestURI().toASCIIString());
        assertEquals("text/plain;charset=UTF-8", httpRequest.getHeader("Content-Type"));
        assertTrue(httpContent.isComplete());
        assertEquals("Hello, Content-Type", httpContent.asText(UTF_8.newDecoder()));
        assertTrue(httpRequest.getCookies().isEmpty());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        assertFalse(in.hasRemaining());
    }

    @Test
    public void decodeNonCanonicalHttpRequest() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        ProtocolDecoder decoder = new HttpRequestDecoder();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

        ByteBuffer in = ByteBuffer.wrap(("GET //;e//ct HTTP/1.1\r\n" +
                                         "Host: EXAMPLE.com\r\n" +
                                         "Host: anotherHost.com:888\r\n" +
                                         "\r\n").getBytes());

        IoBufferEx buf = allocator.wrap(in);
        decoder.decode(session, (IoBuffer) buf, session.getDecoderOutput());

        assertFalse(session.getDecoderOutputQueue().isEmpty());
        HttpRequestMessage httpRequest = (HttpRequestMessage)session.getDecoderOutputQueue().poll();
        assertEquals("/;e/ct", httpRequest.getRequestURI().toASCIIString());
        assertNull(httpRequest.getContent());
        assertTrue(httpRequest.getCookies().isEmpty());
        // KG-1469: make sure we convert host header value to lower case
        assertEquals(Arrays.asList("example.com", "anotherhost.com:888"), httpRequest.getHeaderValues("Host"));

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        assertFalse(in.hasRemaining());
    }

    @Test
    public void decodeHttpRequestMixedCaseURIs() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        ProtocolDecoder decoder = new HttpRequestDecoder();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

        ByteBuffer in = ByteBuffer.wrap(("GET //;e//ct HTTP/1.1\r\n" +
                                         "Host: eXample.com\r\n" +
                                         "Origin: http://LOCALHOST:8000\r\n" +
                                         "Referer: http://My.Host.com\r\n" +
                                         "\r\n").getBytes());

        IoBufferEx buf = allocator.wrap(in);
        decoder.decode(session, (IoBuffer) buf, session.getDecoderOutput());

        assertFalse(session.getDecoderOutputQueue().isEmpty());
        HttpRequestMessage httpRequest = (HttpRequestMessage)session.getDecoderOutputQueue().poll();
        assertEquals("/;e/ct", httpRequest.getRequestURI().toASCIIString());
        assertNull(httpRequest.getContent());
        assertTrue(httpRequest.getCookies().isEmpty());
        assertEquals(Collections.singletonList("example.com"), httpRequest.getHeaderValues("Host"));
        assertEquals(Collections.singletonList("http://localhost:8000"), httpRequest.getHeaderValues("Origin"));
        assertEquals(Collections.singletonList("http://my.host.com"), httpRequest.getHeaderValues("Referer"));

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        assertFalse(in.hasRemaining());
    }

    @Test
    public void decodeHttpRequest() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        ProtocolDecoder decoder = new HttpRequestDecoder();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

        ByteBuffer in = ByteBuffer.wrap(("POST /sse?.a=1&.a=1b&.b+=2 HTTP/1.1\r\n" +
                                         "Host: sse.server.net\r\n" +
                                         "Content-Type: text/event-stream\r\n" +
                                         "\r\n").getBytes());

        IoBufferEx buf = allocator.wrap(in);
        decoder.decode(session, (IoBuffer) buf, session.getDecoderOutput());

        assertFalse(session.getDecoderOutputQueue().isEmpty());
        HttpRequestMessage httpRequest = (HttpRequestMessage)session.getDecoderOutputQueue().poll();
        assertEquals("/sse?.a=1&.a=1b&.b+=2", httpRequest.getRequestURI().toASCIIString());
        assertNull(httpRequest.getContent());
        assertEquals(Arrays.asList("1", "1b"), httpRequest.getParameterValues(".a"));
        assertEquals(Collections.singletonList("2"), httpRequest.getParameterValues(".b "));
        assertEquals(Collections.singletonList("sse.server.net"), httpRequest.getHeaderValues("Host"));
        assertEquals(Collections.singletonList("text/event-stream"), httpRequest.getHeaderValues("Content-Type"));

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        assertFalse(in.hasRemaining());
    }

    @Test
    public void decodeHttpRequestComplete() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        ProtocolDecoder decoder = new HttpRequestDecoder();

        ByteBuffer in = ByteBuffer.wrap(("POST / HTTP/1.1\r\n" +
                                         "Content-Length: 55\r\n" +
                                         "\r\n" +
                                         "retry:2500\r\n" +
                                         "event:TCPSend\r\n" +
                                         "id:24\r\n" +
                                         "data:Hello, world\r\n" +
                                         "\r\n").getBytes());

        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        IoBufferEx buf = allocator.wrap(in);
        decoder.decode(session, (IoBuffer) buf, session.getDecoderOutput());

        assertFalse(session.getDecoderOutputQueue().isEmpty());
        HttpRequestMessage httpRequest = (HttpRequestMessage)session.getDecoderOutputQueue().poll();
        HttpContentMessage httpContent = httpRequest.getContent();
        assertTrue(httpContent.isComplete());
        assertEquals("retry:2500\r\n" +
                     "event:TCPSend\r\n" +
                     "id:24\r\n" +
                     "data:Hello, world\r\n" +
                     "\r\n", httpContent.asText(UTF_8.newDecoder()));

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        assertFalse(in.hasRemaining());
    }

    @Test
    public void decodeHttpRequestChunkedComplete() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        ProtocolDecoder decoder = new HttpRequestDecoder();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

        ByteBuffer in = ByteBuffer.wrap(("POST / HTTP/1.1\r\n" +
                                         "Transfer-Encoding: chunked\r\n" +
                                         "\r\n" +
                                         "37\r\n" +
                                         "retry:2500\r\n" +
                                         "event:TCPSend\r\n" +
                                         "id:24\r\n" +
                                         "data:Hello, world\r\n" +
                                         "\r\n" +
                                         "\r\n" +
                                         "0\r\n" +
                                         "\r\n").getBytes());

        IoBufferEx buf = allocator.wrap(in);
        decoder.decode(session, (IoBuffer) buf, session.getDecoderOutput());

        assertFalse(session.getDecoderOutputQueue().isEmpty());
        HttpRequestMessage httpRequest = (HttpRequestMessage)session.getDecoderOutputQueue().poll();
        assertEquals(allocator.wrap(allocator.allocate(0)), httpRequest.getContent().asBuffer());
        assertFalse(httpRequest.getContent().isComplete());
        assertTrue(httpRequest.getCookies().isEmpty());

        assertFalse(session.getDecoderOutputQueue().isEmpty());
        HttpContentMessage httpContent = (HttpContentMessage)session.getDecoderOutputQueue().poll();
        assertFalse(httpContent.isComplete());
        assertEquals("retry:2500\r\n" +
                     "event:TCPSend\r\n" +
                     "id:24\r\n" +
                     "data:Hello, world\r\n" +
                     "\r\n", httpContent.asText(UTF_8.newDecoder()));

        assertFalse(session.getDecoderOutputQueue().isEmpty());
        assertEquals(new HttpContentMessage(allocator.wrap(allocator.allocate(0)), true), session.getDecoderOutputQueue().poll());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        assertFalse(in.hasRemaining());
    }

    @Test
    public void decodeHttpRequestIncomplete() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        ProtocolDecoder decoder = new HttpRequestDecoder();

        ByteBuffer[] in = new ByteBuffer[] {
                ByteBuffer.wrap(("POST / HTTP/1.1\r\n" +
                                 "Transfer-Encoding: chunked\r\n" +
                                 "\r\n").getBytes()),
                ByteBuffer.wrap(("37\r\n" +
                                 "retry:2500\r\n" +
                                 "event:TCPSend\r\n" +
                                 "id:24\r\n" +
                                 "data:Hello, world\r\n" +
                                 "\r\n" +
                                 "\r\n").getBytes()) };

        assertTrue(session.getDecoderOutputQueue().isEmpty());

        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        IoBufferEx[] buf = new IoBufferEx[] {
                allocator.wrap(in[0]),
                allocator.wrap(in[1]),
        };

        decoder.decode(session, (IoBuffer) buf[0], session.getDecoderOutput());

        assertFalse(session.getDecoderOutputQueue().isEmpty());
        assertFalse(buf[0].hasRemaining());

        HttpRequestMessage httpRequest = (HttpRequestMessage)session.getDecoderOutputQueue().poll();
        assertEquals(allocator.wrap(allocator.allocate(0)), httpRequest.getContent().asBuffer());
        assertFalse(httpRequest.getContent().isComplete());
        assertTrue(httpRequest.getCookies().isEmpty());

        assertTrue(session.getDecoderOutputQueue().isEmpty());

        decoder.decode(session, (IoBuffer) buf[1], session.getDecoderOutput());

        assertFalse(session.getDecoderOutputQueue().isEmpty());
        assertFalse(buf[1].hasRemaining());

        HttpContentMessage httpContent = (HttpContentMessage)session.getDecoderOutputQueue().poll();
        assertFalse(httpContent.isComplete());
        assertEquals("retry:2500\r\n" +
                     "event:TCPSend\r\n" +
                     "id:24\r\n" +
                     "data:Hello, world\r\n" +
                     "\r\n", httpContent.asText(UTF_8.newDecoder()));

        assertTrue(session.getDecoderOutputQueue().isEmpty());

        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
    }


    @Test(expected = ProtocolCodecException.class)
    public void decodeHttpRequestInvalidHeadername() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        ProtocolDecoder decoder = new HttpRequestDecoder();

        ByteBuffer in = ByteBuffer.wrap(("GET /echo HTTP/1.1\r\n" +
                                         "Upgrade: WebSocket\r\nConnection: Upgrade\r\n:abc\r\n").getBytes());

        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        IoBufferEx buf = allocator.wrap(in);
        decoder.decode(session, (IoBuffer) buf, session.getDecoderOutput());
        decoder.finishDecode(session, session.getDecoderOutput());
        HttpRequestMessage httpRequest = (HttpRequestMessage)session.getDecoderOutputQueue().poll();
        assertEquals(httpRequest, null);
    }

    @Test
    public void decodeHttpRequestInvalidHeader() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        ProtocolDecoder decoder = new HttpRequestDecoder();

        ByteBuffer in = ByteBuffer.wrap(("GET /echo HTTP/1.1\r\n" +
                                         "Upgrade: WebSocket\r\nConnection: Upgrade\n\r\n").getBytes());

        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        IoBufferEx buf = allocator.wrap(in);
        decoder.decode(session, (IoBuffer) buf, session.getDecoderOutput());
        // for this case we don't expect an exception nor do we expect a valid request to come through
        // but the bug is that there is an expensive loop that takes a long time, so failing here based
        // on the time taken to finishDecode is more than a second
        long begin = System.currentTimeMillis();
        decoder.finishDecode(session, session.getDecoderOutput());
        long end = System.currentTimeMillis();
        assertTrue( (end - begin) < 1000 );
        HttpRequestMessage httpRequest = (HttpRequestMessage)session.getDecoderOutputQueue().poll();
        assertEquals(httpRequest, null);
    }

    @Test
    public void shouldDecodeHttpRequestDuplicateHeadersWithSingleValues()
        throws Exception {

        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        ProtocolDecoder decoder = new HttpRequestDecoder();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

        ByteBuffer in = ByteBuffer.wrap(("GET /echo HTTP/1.1\r\n" +
                                         "Sec-WebSocket-Extensions: x-kaazing-foo\r\n" +
                                         "Sec-WebSocket-Extensions: x-kaazing-bar\r\n" +
                                         "\r\n").getBytes());

        IoBufferEx buf = allocator.wrap(in);
        decoder.decode(session, (IoBuffer) buf, session.getDecoderOutput());

        assertFalse(session.getDecoderOutputQueue().isEmpty());

        HttpRequestMessage request = (HttpRequestMessage) session.getDecoderOutputQueue().poll();
        assertEquals("/echo", request.getRequestURI().toASCIIString());
        assertNull(request.getContent());
        assertTrue(request.getCookies().isEmpty());

        assertEquals(Arrays.asList("x-kaazing-foo", "x-kaazing-bar"), request.getHeaderValues("Sec-WebSocket-Extensions"));

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        assertFalse(in.hasRemaining());
    }

    @Test
    public void shouldDecodeHttpRequestDuplicateHeadersWithMultipleValues()
        throws Exception {

        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        ProtocolDecoder decoder = new HttpRequestDecoder();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

        ByteBuffer in = ByteBuffer.wrap(("GET /echo HTTP/1.1\r\n" +
                                         "Sec-WebSocket-Extensions: x-kaazing-foo; a=1; b=2, x-kaazing-bar\r\n" +
                                         "Sec-WebSocket-Extensions: x-kaazing-baz, x-kaazing-quxx; x=y\r\n" +
                                         "\r\n").getBytes());

        IoBufferEx buf = allocator.wrap(in);
        decoder.decode(session, (IoBuffer) buf, session.getDecoderOutput());

        assertFalse(session.getDecoderOutputQueue().isEmpty());

        HttpRequestMessage request = (HttpRequestMessage) session.getDecoderOutputQueue().poll();
        assertEquals("/echo", request.getRequestURI().toASCIIString());
        assertNull(request.getContent());
        assertTrue(request.getCookies().isEmpty());

        assertEquals(Arrays.asList("x-kaazing-foo; a=1; b=2", "x-kaazing-bar", "x-kaazing-baz", "x-kaazing-quxx; x=y"), request.getHeaderValues("Sec-WebSocket-Extensions"));

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        assertFalse(in.hasRemaining());
    }

}
