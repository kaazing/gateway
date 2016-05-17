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
import static org.junit.Assert.assertSame;
import static org.kaazing.mina.core.buffer.IoBufferEx.FLAG_SHARED;
import static org.kaazing.mina.core.buffer.IoBufferEx.FLAG_ZERO_COPY;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.junit.Test;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpVersion;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.filter.codec.ProtocolCodecSessionEx;


public class HttpResponseEncoderTest {

    @Test
    public void encodeHttpResponse() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new HttpResponseEncoder(allocator);
        ProtocolEncoderOutput encoderOut = session.getEncoderOutput();

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HttpVersion.HTTP_1_1);
        httpResponse.setStatus(HttpStatus.REDIRECT_FOUND);
        httpResponse.setReason("Cross-Origin Redirect");
        httpResponse.setHeader("Location", "https://www.w3.org/");

        encoder.encode(session, httpResponse, encoderOut);
        encoderOut.mergeAll();

        IoBufferEx buf = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals("HTTP/1.1 302 Cross-Origin Redirect\r\n" +
                     "Location: https://www.w3.org/\r\n" +
                     "Content-Length: 0\r\n" +
                     "\r\n",
                     buf.getString(Charset.forName("UTF-8").newDecoder()));
    }

    @Test
    public void encodeHttpResponseIncomplete() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new HttpResponseEncoder(allocator);
        ProtocolEncoderOutput encoderOut = session.getEncoderOutput();

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HttpVersion.HTTP_1_1);
        httpResponse.setStatus(HttpStatus.SUCCESS_OK);

        IoBufferEx data = allocator.wrap(ByteBuffer.wrap("Hello, world".getBytes()));
        httpResponse.setContent(new HttpContentMessage(data, false));

        encoder.encode(session, httpResponse, encoderOut);
        encoderOut.mergeAll();

        IoBufferEx buf = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals("HTTP/1.1 200 OK\r\n" +
                     "\r\n" +
                     "Hello, world",
                     buf.getString(Charset.forName("UTF-8").newDecoder()));
    }

    @Test
    public void encodeHttpResponseChunked() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new HttpResponseEncoder(allocator);
        ProtocolEncoderOutput encoderOut = session.getEncoderOutput();

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HttpVersion.HTTP_1_1);
        httpResponse.setStatus(HttpStatus.SUCCESS_OK);
        httpResponse.setHeader("Transfer-Encoding", "chunked");

        IoBufferEx data = allocator.wrap(ByteBuffer.wrap("Hello, world".getBytes()));
        httpResponse.setContent(new HttpContentMessage(data, false, true, false));

        encoder.encode(session, httpResponse, encoderOut);
        encoderOut.mergeAll();

        IoBufferEx buf = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals("HTTP/1.1 200 OK\r\n" +
                     "Transfer-Encoding: chunked\r\n" +
                     "\r\n" +
                     "c\r\n" +
                     "Hello, world\r\n",
                     buf.getString(Charset.forName("UTF-8").newDecoder()));
    }

    @Test
    public void encodeHttpResponseChunkedZeroBytes() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new HttpResponseEncoder(allocator);
        ProtocolEncoderOutput encoderOut = session.getEncoderOutput();

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HttpVersion.HTTP_1_1);
        httpResponse.setStatus(HttpStatus.SUCCESS_OK);
        httpResponse.setHeader("Transfer-Encoding", "chunked");

        IoBufferEx data = allocator.wrap(ByteBuffer.wrap("Hello, world".getBytes()));
        httpResponse.setContent(new HttpContentMessage(data, false, true, false));

        encoder.encode(session, httpResponse, encoderOut);
        encoderOut.mergeAll();

        IoBufferEx buf = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals("HTTP/1.1 200 OK\r\n" +
                     "Transfer-Encoding: chunked\r\n" +
                     "\r\n" +
                     "c\r\n" +
                     "Hello, world\r\n",
                     buf.getString(Charset.forName("UTF-8").newDecoder()));
    }

    @Test
    public void encodeHttpResponseComplete() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new HttpResponseEncoder(allocator);
        ProtocolEncoderOutput encoderOut = session.getEncoderOutput();

        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(HttpVersion.HTTP_1_1);
        httpResponse.setStatus(HttpStatus.SUCCESS_OK);

        IoBufferEx data = allocator.wrap(ByteBuffer.wrap("Hello, world".getBytes()));
        httpResponse.setContent(new HttpContentMessage(data, true));

        encoder.encode(session, httpResponse, encoderOut);
        encoderOut.mergeAll();

        IoBufferEx buf = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals("HTTP/1.1 200 OK\r\n" +
                     "Content-Length: 12\r\n" +
                     "\r\n" +
                     "Hello, world",
                     buf.getString(Charset.forName("UTF-8").newDecoder()));
    }

    @Test
    public void encodeContent() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new HttpResponseEncoder(allocator);

        IoBufferEx data = allocator.wrap(ByteBuffer.wrap("Hello, world".getBytes()));
        HttpContentMessage httpContent = new HttpContentMessage(data, true);

        encoder.encode(session, httpContent, session.getEncoderOutput());

        IoBufferEx buf = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals("Hello, world", buf.getString(Charset.forName("UTF-8").newDecoder()));
    }

    @Test
    public void encodeContentShared() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new HttpResponseEncoder(allocator);

        HttpBufferAllocator httpAllocator = new HttpBufferAllocator(allocator);
        IoBufferEx data = httpAllocator.wrap(ByteBuffer.wrap("Hello, world".getBytes()), FLAG_SHARED);
        HttpContentMessage httpContent = new HttpContentMessage(data, true);

        encoder.encode(session, httpContent, session.getEncoderOutput());

        IoBufferEx buf = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals("Hello, world", buf.getString(Charset.forName("UTF-8").newDecoder()));
        assertSame(data.array(), buf.array());
    }

    @Test
    public void encodeContentChunked() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new HttpResponseEncoder(allocator);
        ProtocolEncoderOutput encoderOut = session.getEncoderOutput();

        IoBufferEx data = allocator.wrap(ByteBuffer.wrap("Hello, world".getBytes()));
        HttpContentMessage httpContent = new HttpContentMessage(data, false, true, false);

        encoder.encode(session, httpContent, encoderOut);
        encoderOut.mergeAll();

        IoBufferEx buf = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals("c\r\n" +
                     "Hello, world\r\n", buf.getString(Charset.forName("UTF-8").newDecoder()));
    }

    @Test
    public void encodeContentChunkedZeroCopy() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new HttpResponseEncoder(allocator);
        ProtocolEncoderOutput encoderOut = session.getEncoderOutput();

        HttpBufferAllocator httpAllocator = new HttpBufferAllocator(allocator);
        byte[] textBytes = "Hello, world".getBytes();
        IoBufferEx data = httpAllocator.wrap(httpAllocator.allocate(textBytes.length, FLAG_ZERO_COPY));
        int pos = data.position();
        data.put(textBytes);
        data.position(pos);
        HttpContentMessage httpContent = new HttpContentMessage(data, false, true, false);

        encoder.encode(session, httpContent, encoderOut);
        encoderOut.mergeAll();

        IoBufferEx buf = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals("c\r\n" +
                     "Hello, world\r\n", buf.getString(Charset.forName("UTF-8").newDecoder()));
        assertSame(data.array(), buf.array());
    }

    @Test
    public void encodeContentChunkedComplete() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new HttpResponseEncoder(allocator);
        ProtocolEncoderOutput encoderOut = session.getEncoderOutput();

        IoBufferEx data = allocator.wrap(ByteBuffer.wrap("Hello, world".getBytes()));
        HttpContentMessage httpContent = new HttpContentMessage(data, true, true, false);

        encoder.encode(session, httpContent, encoderOut);
        encoderOut.mergeAll();

        IoBufferEx buf = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals("c\r\n" +
                     "Hello, world\r\n" +
                     "0\r\n" +
                     "\r\n", buf.getString(Charset.forName("UTF-8").newDecoder()));
    }

    @Test
    public void encodeContentChunkedCompleteZeroCopy() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new HttpResponseEncoder(allocator);
        ProtocolEncoderOutput encoderOut = session.getEncoderOutput();

        HttpBufferAllocator httpAllocator = new HttpBufferAllocator(allocator);
        byte[] textBytes = "Hello, world".getBytes();
        IoBufferEx data = httpAllocator.wrap(httpAllocator.allocate(textBytes.length, FLAG_ZERO_COPY));
        int pos = data.position();
        data.put(textBytes);
        data.position(pos);
        HttpContentMessage httpContent = new HttpContentMessage(data, true, true, false);

        encoder.encode(session, httpContent, encoderOut);
        IoBufferEx msgBuf = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals("c\r\n" +
                     "Hello, world\r\n", msgBuf.getString(Charset.forName("UTF-8").newDecoder()));
        assertSame(data.array(), msgBuf.array());

        IoBufferEx zeroChunkBuf = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals("0\r\n" +
                     "\r\n", zeroChunkBuf.getString(Charset.forName("UTF-8").newDecoder()));
    }

    @Test
    public void encodeContentGzipped() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new HttpResponseEncoder(allocator);
        ProtocolEncoderOutput encoderOut = session.getEncoderOutput();

        IoBufferEx data = allocator.wrap(ByteBuffer.wrap("Hello, world".getBytes()));
        HttpContentMessage httpContent = new HttpContentMessage(data, false, false, true);

        encoder.encode(session, httpContent, encoderOut);
        encoderOut.mergeAll();

        IoBufferEx buf = (IoBufferEx) session.getEncoderOutputQueue().poll();
        IoBufferEx expected = allocator.wrap(allocator.allocate(17));
        expected.put(new byte[] { 0, (byte)12, 0, (byte)~12, (byte)~0 });
        expected.put("Hello, world".getBytes());
        expected.flip();
        assertEquals(expected, buf);
    }

    @Test
    public void encodeContentGzippedZeroCopy() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new HttpResponseEncoder(allocator);
        ProtocolEncoderOutput encoderOut = session.getEncoderOutput();

        HttpBufferAllocator httpAllocator = new HttpBufferAllocator(allocator);
        byte[] textBytes = "Hello, world".getBytes();
        IoBufferEx data = httpAllocator.wrap(httpAllocator.allocate(textBytes.length, FLAG_ZERO_COPY));
        int pos = data.position();
        data.put(textBytes);
        data.position(pos);
        HttpContentMessage httpContent = new HttpContentMessage(data, false, false, true);

        encoder.encode(session, httpContent, encoderOut);
        encoderOut.mergeAll();

        IoBufferEx buf = (IoBufferEx) session.getEncoderOutputQueue().poll();
        IoBufferEx expected = allocator.wrap(allocator.allocate(17));
        expected.put(new byte[] { 0, (byte)12, 0, (byte)~12, (byte)~0 });
        expected.put("Hello, world".getBytes());
        expected.flip();
        assertEquals(expected, buf);
    }

    @Test
    public void encodeContentGzippedSharedZeroCopy() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new HttpResponseEncoder(allocator);
        ProtocolEncoderOutput encoderOut = session.getEncoderOutput();

        HttpBufferAllocator httpAllocator = new HttpBufferAllocator(allocator);
        byte[] textBytes = "Hello, world".getBytes();
        IoBufferEx data = httpAllocator.wrap(httpAllocator.allocate(textBytes.length, FLAG_ZERO_COPY), FLAG_SHARED);
        int pos = data.position();
        data.put(textBytes);
        data.position(pos);
        HttpContentMessage httpContent = new HttpContentMessage(data, false, false, true);

        encoder.encode(session, httpContent, encoderOut);
        encoderOut.mergeAll();

        IoBufferEx buf = (IoBufferEx) session.getEncoderOutputQueue().poll();
        IoBufferEx expected = allocator.wrap(allocator.allocate(17), FLAG_SHARED);
        expected.put(new byte[] { 0, (byte)12, 0, (byte)~12, (byte)~0 });
        expected.put("Hello, world".getBytes());
        expected.flip();
        assertEquals(expected, buf);
    }

}
