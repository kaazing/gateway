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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.DefaultIoSessionConfigEx;
import org.kaazing.gateway.transport.DefaultTransportMetadata;
import org.kaazing.gateway.transport.http.DefaultHttpCookie;
import org.kaazing.gateway.transport.http.DefaultHttpSession;
import org.kaazing.gateway.transport.http.HttpConnector;
import org.kaazing.gateway.transport.http.HttpCookie;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.gateway.transport.http.HttpProtocol;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpVersion;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.gateway.transport.http.bridge.HttpMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.service.IoServiceEx;
import org.kaazing.mina.core.write.DefaultWriteRequestEx;
import org.kaazing.mina.filter.codec.ProtocolCodecSessionEx;
import org.kaazing.test.util.Mockery;


public class HttpResponseDecoderTest {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    @Test
    public void decodeHttpResponse() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        ProtocolDecoder decoder = new HttpResponseDecoder();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

        ByteBuffer in = ByteBuffer.wrap(("HTTP/1.1 200 OK (Test)\r\n" +
                                         "Server: Test\r\n" +
                                         "Content-Length: 0\r\n" +
                                         "\r\n").getBytes());

        IoBufferEx buf = allocator.wrap(in);
        decoder.decode(session, (IoBuffer) buf, session.getDecoderOutput());

        assertFalse(session.getDecoderOutputQueue().isEmpty());
        HttpResponseMessage httpResponse = (HttpResponseMessage)session.getDecoderOutputQueue().poll();
        assertEquals(HttpVersion.HTTP_1_1, httpResponse.getVersion());
        assertEquals(HttpStatus.SUCCESS_OK, httpResponse.getStatus());
        assertEquals("OK (Test)", httpResponse.getReason());
        assertNull(httpResponse.getContent());
        assertEquals(Collections.singletonList("Test"), httpResponse.getHeaderValues("Server"));

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        assertFalse(in.hasRemaining());
    }

    @Test
    public void decodeHttpResponseComplete() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        ProtocolDecoder decoder = new HttpResponseDecoder();

        ByteBuffer in = ByteBuffer.wrap(("HTTP/1.1 200 OK\r\n" +
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
        HttpResponseMessage httpResponse = (HttpResponseMessage)session.getDecoderOutputQueue().poll();
        HttpContentMessage httpContent = httpResponse.getContent();
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
    public void decodeLargeHttpResponseComplete() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        ProtocolDecoder decoder = new HttpResponseDecoder();

        int bodyLength1 = 4096;
        int bodyLength2 = 4096;
        int bodyLength3 = 1;
        int contentLength = bodyLength1 + bodyLength2 + bodyLength3;

        String headers =
                "HTTP/1.1 200 OK\r\n" +
                "Content-Length: " + contentLength + "\r\n" +
                "\r\n";

        HttpResponseMessage httpResponse = (HttpResponseMessage) parse(session, decoder, headers);
        HttpContentMessage httpContent = httpResponse.getContent();
        assertFalse(httpContent.isComplete());

        String body1 = getBody(bodyLength1, '1');
        httpContent = (HttpContentMessage) parse(session, decoder, body1);
        assertFalse(httpContent.isComplete());
        assertEquals(body1, httpContent.asText(UTF_8.newDecoder()));

        String body2 = getBody(bodyLength2, '2');
        httpContent = (HttpContentMessage) parse(session, decoder, body2);
        assertFalse(httpContent.isComplete());
        assertEquals(body2, httpContent.asText(UTF_8.newDecoder()));

        String body3 = getBody(bodyLength3, '3');
        httpContent = (HttpContentMessage) parse(session, decoder, body3);
        assertTrue(httpContent.isComplete());
        assertEquals(body3, httpContent.asText(UTF_8.newDecoder()));

        decoder.finishDecode(session, session.getDecoderOutput());
        assertTrue(session.getDecoderOutputQueue().isEmpty());
    }

    private static String getBody(int size, char ch) {
        char[] chars = new char[size];
        Arrays.fill(chars, ch);
        return new String(chars);
    }

    private static HttpMessage parse(ProtocolCodecSessionEx session, ProtocolDecoder decoder, String part) throws Exception {
        ByteBuffer in = ByteBuffer.wrap((part).getBytes(StandardCharsets.UTF_8));

        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        IoBufferEx buf = allocator.wrap(in);
        decoder.decode(session, (IoBuffer) buf, session.getDecoderOutput());

        assertFalse(in.hasRemaining());
        assertFalse(session.getDecoderOutputQueue().isEmpty());

        return (HttpMessage) session.getDecoderOutputQueue().poll();
    }

    @Test
    public void decodeHeadResponseWihtContentLengthButNoContent() throws Exception {
        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());

        IoServiceEx httpService = context.mock(IoServiceEx.class);
        IoHandler httpHandler = context.mock(IoHandler.class);
        IoProcessorEx<DefaultHttpSession> processor = context.mock(IoProcessorEx.class);
        context.checking(new Expectations() {{
            allowing(httpService).getTransportMetadata(); will(returnValue(new DefaultTransportMetadata(HttpProtocol.NAME)));
            allowing(httpService).getHandler(); will(returnValue(httpHandler));
            allowing(httpService).getSessionConfig(); will(returnValue(new DefaultIoSessionConfigEx()));
            allowing(httpService).getThreadLocalWriteRequest(with(any(int.class))); will(returnValue(new DefaultWriteRequestEx.ShareableWriteRequest()));
        }});

        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        ResourceAddress address = addressFactory.newResourceAddress("http://localhost:4232/");
        ResourceAddress remoteAddress = addressFactory.newResourceAddress("http://localhost:8080/");

        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        DefaultHttpSession httpSession = new DefaultHttpSession(httpService, processor, address, remoteAddress, session, null, new Properties());
        httpSession.setMethod(HttpMethod.HEAD);
        HttpConnector.HTTP_SESSION_KEY.set(session, httpSession);
        ProtocolDecoder decoder = new HttpResponseDecoder();

        ByteBuffer in = ByteBuffer.wrap((
                "HTTP/1.1 200 OK\r\n" +
                "Content-Length: 55\r\n" +
                "\r\n").getBytes());

        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        IoBufferEx buf = allocator.wrap(in);
        decoder.decode(session, (IoBuffer) buf, session.getDecoderOutput());

        assertFalse(session.getDecoderOutputQueue().isEmpty());
        HttpResponseMessage httpResponse = (HttpResponseMessage)session.getDecoderOutputQueue().poll();
        HttpContentMessage httpContent = httpResponse.getContent();
        assertTrue(httpContent.isComplete());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        assertFalse(in.hasRemaining());

        context.assertIsSatisfied();
    }

    @Test
    public void decodeHttpResponseChunkedComplete() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        ProtocolDecoder decoder = new HttpResponseDecoder();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

        ByteBuffer in = ByteBuffer.wrap(("HTTP/1.1 200 OK\r\n" +
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
        HttpResponseMessage httpResponse = (HttpResponseMessage)session.getDecoderOutputQueue().poll();
        assertEquals(allocator.wrap(allocator.allocate(0)), httpResponse.getContent().asBuffer());
        assertFalse(httpResponse.getContent().isComplete());
        assertTrue(httpResponse.getCookies().isEmpty());

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
    public void decodeHttpResponseIncomplete() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        ProtocolDecoder decoder = new HttpResponseDecoder();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

        ByteBuffer[] in = new ByteBuffer[] {
                ByteBuffer.wrap(("HTTP/1.1 200 OK\r\n" +
                                 "Transfer-Encoding: chunked\r\n" +
                                  "\r\n").getBytes()),
                ByteBuffer.wrap(("37\r\n" +
                                 "retry:2500\r\n" +
                                 "event:TCPSend\r\n" +
                                 "id:24\r\n" +
                                 "data:Hello, world\r\n" +
                                 "\r\n" +
                                 "\r\n").getBytes()) };

        IoBufferEx[] buf = new IoBufferEx[] {
                allocator.wrap(in[0]),
                allocator.wrap(in[1]),
        };

        assertTrue(session.getDecoderOutputQueue().isEmpty());

        decoder.decode(session, (IoBuffer) buf[0], session.getDecoderOutput());

        assertFalse(session.getDecoderOutputQueue().isEmpty());
        assertFalse(in[0].hasRemaining());

        HttpResponseMessage httpResponse = (HttpResponseMessage)session.getDecoderOutputQueue().poll();
        assertEquals(allocator.wrap(allocator.allocate(0)), httpResponse.getContent().asBuffer());
        assertFalse(httpResponse.getContent().isComplete());
        assertTrue(httpResponse.getCookies().isEmpty());

        assertTrue(session.getDecoderOutputQueue().isEmpty());

        decoder.decode(session, (IoBuffer) buf[1], session.getDecoderOutput());

        assertFalse(session.getDecoderOutputQueue().isEmpty());
        assertFalse(in[1].hasRemaining());

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

    @Test
    public void decodeHttpResponseCookiesNoValue() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        ProtocolDecoder decoder = new HttpResponseDecoder();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

        ByteBuffer in = ByteBuffer.wrap(("HTTP/1.1 200 OK\r\n" + 
                                         "Server: Apache-Coyote/1.1\r\n" + 
                                         "Set-Cookie: name=; Path=/path/; domain=somedomain.com\r\n" + 
                                         "Transfer-Encoding: chunked\r\n" +
                                         "\r\n").getBytes());

        IoBufferEx buf = allocator.wrap(in);
        decoder.decode(session, (IoBuffer) buf, session.getDecoderOutput());

        assertFalse(session.getDecoderOutputQueue().isEmpty());
        HttpResponseMessage httpResponse = (HttpResponseMessage) session.getDecoderOutputQueue().poll();
        Set<HttpCookie> cookies = httpResponse.getCookies();
        assertFalse("Empty cookies", cookies.isEmpty());

        HttpCookie receivedCookie = cookies.iterator().next();
        HttpCookie expectedCookie = new DefaultHttpCookie("name", "somedomain.com", "/path/", null);
        assertEquals("Wrong parsing of the received cookies", expectedCookie, receivedCookie);
    }

    @Test
    public void decodeHttpResponseCookiesPropertiesNoValues() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        ProtocolDecoder decoder = new HttpResponseDecoder();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

        ByteBuffer in = ByteBuffer.wrap(("HTTP/1.1 200 OK\r\n" + 
                                         "Server: Apache-Coyote/1.1\r\n" + 
                                         "Set-Cookie: cookieName=testCookie; comment=; domain=; max-age=; path=; version=\r\n" +
                                         "Transfer-Encoding: chunked\r\n" +
                                         "\r\n").getBytes());
        IoBufferEx buf = allocator.wrap(in);
        decoder.decode(session, (IoBuffer) buf, session.getDecoderOutput());

        assertFalse(session.getDecoderOutputQueue().isEmpty());
        HttpResponseMessage httpResponse = (HttpResponseMessage) session.getDecoderOutputQueue().poll();
        Set<HttpCookie> cookies = httpResponse.getCookies();
        assertFalse("Empty cookies", cookies.isEmpty());

        HttpCookie receivedCookie = cookies.iterator().next();
        HttpCookie expectedCookie = new DefaultHttpCookie("cookieName", null, null, "testCookie");
        assertEquals("Wrong parsing of the received cookies", expectedCookie, receivedCookie);
    }

    @Test
    public void decodeHttpResponseCookiesPropertiesWithValues() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        ProtocolDecoder decoder = new HttpResponseDecoder();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

        ByteBuffer in = ByteBuffer.wrap(("HTTP/1.1 200 OK\r\n" + 
                                         "Server: Apache-Coyote/1.1\r\n" + 
                                         "Set-Cookie: cookieName=cookieValue; comment=c; domain=somedomain.com; max-age=2; path=/path/; version=1\r\n" + 
                                         "Transfer-Encoding: chunked\r\n" +
                                         "\r\n").getBytes());
        IoBufferEx buf = allocator.wrap(in);
        decoder.decode(session, (IoBuffer) buf, session.getDecoderOutput());

        assertFalse(session.getDecoderOutputQueue().isEmpty());
        HttpResponseMessage httpResponse = (HttpResponseMessage) session.getDecoderOutputQueue().poll();
        Set<HttpCookie> cookies = httpResponse.getCookies();
        assertFalse("Empty cookies", cookies.isEmpty());

        DefaultHttpCookie receivedCookie = (DefaultHttpCookie) cookies.iterator().next();
        DefaultHttpCookie expectedCookie = new DefaultHttpCookie("cookieName", "somedomain.com", "/path/", "cookieValue");
        expectedCookie.setComment("c");
        expectedCookie.setMaxAge(2);
        expectedCookie.setVersion(1);
        assertEquals("Wrong parsing of the received cookies", expectedCookie, receivedCookie);
    }

    @Test(expected = ProtocolDecoderException.class)
    public void decodeIncorrectHttpVersion() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        ProtocolDecoder decoder = new HttpResponseDecoder();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

        ByteBuffer in = ByteBuffer.wrap(new byte[]{0x03, 0x00, 0x00, 0x00, 0x00});

        IoBufferEx buf = allocator.wrap(in);
        decoder.decode(session, (IoBuffer) buf, session.getDecoderOutput());
    }
}
