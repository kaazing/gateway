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

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.junit.Test;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.gateway.transport.http.HttpVersion;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.filter.codec.ProtocolCodecSessionEx;

public class HttpRequestEncoderTest {

    @Test
    public void encodeHttpRequest() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new HttpRequestEncoder(allocator);
        ProtocolEncoderOutput encoderOut = session.getEncoderOutput();

        HttpRequestMessage httpRequest = new HttpRequestMessage();
        httpRequest.setMethod(HttpMethod.POST);
        httpRequest.setRequestURI(URI.create("/sse"));
        httpRequest.setVersion(HttpVersion.HTTP_1_1);
        httpRequest.setHeader("Host", "sse.server.net");
        httpRequest.setParameter(".a", "1");
        httpRequest.addParameter(".a", "1a");
        httpRequest.setParameter(".b ", "2");

        encoder.encode(session, httpRequest, encoderOut);
        encoderOut.mergeAll();

        IoBuffer buf = (IoBuffer)session.getEncoderOutputQueue().poll();
        assertEquals("POST /sse?.a=1&.a=1a&.b+=2 HTTP/1.1\r\n" +
                     "Host: sse.server.net\r\n" +
                     "Content-Length: 0\r\n" +
                     "\r\n",
                     buf.getString(Charset.forName("UTF-8").newDecoder()));
    }

    @Test
    public void encodeHttpRequestIncomplete() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new HttpRequestEncoder(allocator);
        ProtocolEncoderOutput encoderOut = session.getEncoderOutput();

        HttpRequestMessage httpRequest = new HttpRequestMessage();
        httpRequest.setMethod(HttpMethod.POST);
        httpRequest.setRequestURI(URI.create("/sse"));
        httpRequest.setVersion(HttpVersion.HTTP_1_1);

        IoBufferEx data = allocator.wrap(ByteBuffer.wrap("Hello, world".getBytes()));
        httpRequest.setContent(new HttpContentMessage(data, false));

        encoder.encode(session, httpRequest, encoderOut);
        encoderOut.mergeAll();

        IoBuffer buf = (IoBuffer)session.getEncoderOutputQueue().poll();
        assertEquals("POST /sse HTTP/1.1\r\n" +
                     "\r\n" +
                     "Hello, world",
                     buf.getString(Charset.forName("UTF-8").newDecoder()));
    }

    @Test
    public void encodeHttpRequestChunked() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new HttpRequestEncoder(allocator);
        ProtocolEncoderOutput encoderOut = session.getEncoderOutput();

        HttpRequestMessage httpRequest = new HttpRequestMessage();
        httpRequest.setMethod(HttpMethod.POST);
        httpRequest.setRequestURI(URI.create("/sse"));
        httpRequest.setVersion(HttpVersion.HTTP_1_1);
        httpRequest.setHeader("Transfer-Encoding", "chunked");

        IoBufferEx data = allocator.wrap(ByteBuffer.wrap("Hello, world".getBytes()));
        httpRequest.setContent(new HttpContentMessage(data, false, true, false));

        encoder.encode(session, httpRequest, encoderOut);
        encoderOut.mergeAll();

        IoBuffer buf = (IoBuffer)session.getEncoderOutputQueue().poll();
        assertEquals("POST /sse HTTP/1.1\r\n" +
                     "Transfer-Encoding: chunked\r\n" +
                     "\r\n" +
                     "c\r\n" +
                     "Hello, world\r\n",
                     buf.getString(Charset.forName("UTF-8").newDecoder()));
    }

    @Test
    public void encodeHttpRequestComplete() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new HttpRequestEncoder(allocator);
        ProtocolEncoderOutput encoderOut = session.getEncoderOutput();

        HttpRequestMessage httpRequest = new HttpRequestMessage();
        httpRequest.setMethod(HttpMethod.POST);
        httpRequest.setRequestURI(URI.create("/sse"));
        httpRequest.setVersion(HttpVersion.HTTP_1_1);

        IoBufferEx data = allocator.wrap(ByteBuffer.wrap("Hello, world".getBytes()));
        httpRequest.setContent(new HttpContentMessage(data, true));

        encoder.encode(session, httpRequest, encoderOut);
        encoderOut.mergeAll();

        IoBuffer buf = (IoBuffer)session.getEncoderOutputQueue().poll();
        assertEquals("POST /sse HTTP/1.1\r\n" +
                     "Content-Length: 12\r\n" +
                     "\r\n" +
                     "Hello, world",
                     buf.getString(Charset.forName("UTF-8").newDecoder()));
    }

    @Test
    public void encodeContent() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new HttpRequestEncoder(allocator);
        ProtocolEncoderOutput encoderOut = session.getEncoderOutput();

        IoBufferEx data = allocator.wrap(ByteBuffer.wrap("Hello, world".getBytes()));
        HttpContentMessage httpContent = new HttpContentMessage(data, true);

        encoder.encode(session, httpContent, encoderOut);
        encoderOut.mergeAll();

        IoBuffer buf = (IoBuffer)session.getEncoderOutputQueue().poll();
        assertEquals("Hello, world", buf.getString(Charset.forName("UTF-8").newDecoder()));
    }

    @Test
    public void encodeContentChunked() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new HttpRequestEncoder(allocator);
        ProtocolEncoderOutput encoderOut = session.getEncoderOutput();

        IoBufferEx data = allocator.wrap(ByteBuffer.wrap("Hello, world".getBytes()));
        HttpContentMessage httpContent = new HttpContentMessage(data, false, true, false);

        encoder.encode(session, httpContent, encoderOut);
        encoderOut.mergeAll();

        IoBuffer buf = (IoBuffer)session.getEncoderOutputQueue().poll();
        assertEquals("c\r\n" +
                     "Hello, world\r\n", buf.getString(Charset.forName("UTF-8").newDecoder()));
    }

    @Test
    public void encodeContentChunkedComplete() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new HttpRequestEncoder(allocator);
        ProtocolEncoderOutput encoderOut = session.getEncoderOutput();

        IoBufferEx data = allocator.wrap(ByteBuffer.wrap("Hello, world".getBytes()));
        HttpContentMessage httpContent = new HttpContentMessage(data, true, true, false);

        encoder.encode(session, httpContent, encoderOut);
        encoderOut.mergeAll();

        IoBuffer buf = (IoBuffer)session.getEncoderOutputQueue().poll();
        assertEquals("c\r\n" +
                     "Hello, world\r\n" +
                     "0\r\n" +
                     "\r\n", buf.getString(Charset.forName("UTF-8").newDecoder()));
    }

}
