/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.junit.Test;
import org.kaazing.gateway.transport.http.DefaultHttpCookie;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpVersion;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.filter.codec.ProtocolCodecSessionEx;


public class HttpResponseDecoderTest {

	private static final Charset UTF_8 = Charset.forName("UTF-8");
	
	@Test
	public void decodeHttpResponse() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
		ProtocolDecoder decoder = new HttpResponseDecoder();
		IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

		ByteBuffer in = ByteBuffer.wrap(("HTTP/1.1 200 OK (Test)\r\n" +
    			                         "Server: Test\r\n" +
    			                         "Set-Cookie: KSESSIONID=0123456789abcdef\r\n" +
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
		assertEquals(Collections.singleton(new DefaultHttpCookie("KSESSIONID", "0123456789abcdef")), httpResponse.getCookies());
		assertEquals(Arrays.asList("Test"), httpResponse.getHeaderValues("Server"));
		
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
}
