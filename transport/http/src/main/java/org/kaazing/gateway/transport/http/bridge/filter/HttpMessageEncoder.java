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

import static java.lang.String.format;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CONTENT_LENGTH;
import static org.kaazing.mina.core.buffer.IoBufferEx.FLAG_NONE;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.kaazing.gateway.transport.bridge.CachingMessageEncoder;
import org.kaazing.gateway.transport.bridge.MessageEncoder;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.gateway.transport.http.bridge.HttpMessage;
import org.kaazing.gateway.transport.http.bridge.HttpStartMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;

abstract class HttpMessageEncoder<T extends HttpStartMessage> extends ProtocolEncoderAdapter {

	protected static final byte[] HTTP_1_0_BYTES = "HTTP/1.0".getBytes();
	protected static final byte[] HTTP_1_1_BYTES = "HTTP/1.1".getBytes();
	protected static final byte[] ZERO_BYTES = "0".getBytes();
	protected static final byte[] EQUAL_BYTES = "=".getBytes();
	protected static final byte[] SEMI_BYTES = ";".getBytes();
	protected static final byte[] COLON_BYTES = ":".getBytes();
	protected static final byte[] SPACE_BYTES = " ".getBytes();
	protected static final byte[] CRLF_BYTES = "\r\n".getBytes();

    protected static final byte[] ZERO_CHUNK_BYTES = new byte[] { 0x30, 0xd, 0xa, 0xd, 0xa };

    private static final String HEADER_BYTES_PATTERN = "%s: ";
	protected static final byte[] HEADER_CONTENT_LENGTH_BYTES = format(HEADER_BYTES_PATTERN, HEADER_CONTENT_LENGTH).getBytes();

	private static final Charset US_ASCII = Charset.forName("US-ASCII");

	protected final CachingMessageEncoder cachingEncoder;
	protected final IoBufferAllocatorEx<?> allocator;
	protected final CharsetEncoder asciiEncoder;

	protected static final HttpChunkedEncoder chunkedEncoder = new HttpChunkedEncoder();
	protected static final HttpGzipEncoder gzipEncoder = new HttpGzipEncoder();

    protected HttpMessageEncoder(CachingMessageEncoder cachingEncoder, IoBufferAllocatorEx<?> allocator) {
        this.cachingEncoder = cachingEncoder;
        this.allocator = allocator;
		this.asciiEncoder = US_ASCII.newEncoder();
	}

	@Override
	public final void encode(IoSession session, Object message,
							 ProtocolEncoderOutput out) throws Exception {

	    IoSessionEx sessionEx = (IoSessionEx) session;
		HttpMessage httpMessage = (HttpMessage) message;

		asciiEncoder.reset();

		encode(sessionEx, httpMessage, out);
	}

	protected abstract void encode(IoSessionEx session, HttpMessage httpMessage, ProtocolEncoderOutput out) throws Exception;

	protected final void encodeContent(IoSessionEx session, T httpStart, ProtocolEncoderOutput out) throws CharacterCodingException {
		HttpContentMessage httpContent = httpStart.getContent();
		if (httpContent != null) {
			encodeContent(session, httpContent, out);
		}
	}

    protected final void encodeContent(IoSessionEx session, HttpContentMessage httpContent, IoBufferEx buf) throws CharacterCodingException {
        boolean isChunked = httpContent.isChunked();
        boolean isGzipped = httpContent.isGzipped();
        encodeContent(session, httpContent, buf, isChunked, isGzipped);
    }

	protected final void encodeContent(IoSessionEx session, HttpContentMessage httpContent, ProtocolEncoderOutput out) throws CharacterCodingException {
		boolean isChunked = httpContent.isChunked();
		boolean isGzipped = httpContent.isGzipped();
		encodeContent(session, httpContent, out, isChunked, isGzipped);
	}

    protected final void encodeContent(IoSessionEx session, final HttpContentMessage httpContent, IoBufferEx buf,
            final boolean isChunked, final boolean isGzipped) throws CharacterCodingException {

        if (!isGzipped && !isChunked) {
            IoBufferEx content = httpContent.asBuffer();
            if (content.hasRemaining()) {
                buf.put(content.buf().duplicate());
            }
        }
        else {
            MessageEncoder<HttpContentMessage> contentMessageEncoder;
            if (isGzipped) {
                if (isChunked) {
                    contentMessageEncoder = HttpContentMessageEncoder.CHUNKED_GZIPPED;
                }
                else {
                    contentMessageEncoder = HttpContentMessageEncoder.GZIPPED;
                }
            }
            else { // must be chunked
                contentMessageEncoder = HttpContentMessageEncoder.CHUNKED;
            }

            IoBufferEx content;
            if (httpContent.hasCache()) {
                content = cachingEncoder.encode(contentMessageEncoder, httpContent, allocator, FLAG_NONE);
            }
            else {
                content = contentMessageEncoder.encode(allocator, httpContent, FLAG_NONE);
            }

            if (content.hasRemaining()) {
                buf.put(content.buf().duplicate());
            }

            // Write out zero chunk if needed
            if (isChunked && httpContent.isComplete()) {
                buf.put(ByteBuffer.wrap(ZERO_CHUNK_BYTES));
            }
        }
    }
    
	protected final void encodeContent(IoSessionEx session, final HttpContentMessage httpContent, ProtocolEncoderOutput out,
			final boolean isChunked, final boolean isGzipped) throws CharacterCodingException {

		if (!isGzipped && !isChunked) {
			IoBufferEx content = httpContent.asBuffer();
			if (content.hasRemaining()) {
			    IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
	            IoBufferEx buf = allocator.wrap(content.buf(), content.flags());
	            out.write(buf);
			}
		}
		else {
			MessageEncoder<HttpContentMessage> contentMessageEncoder;
			if (isGzipped) {
				if (isChunked) {
					contentMessageEncoder = HttpContentMessageEncoder.CHUNKED_GZIPPED;
				}
				else {
					contentMessageEncoder = HttpContentMessageEncoder.GZIPPED;
				}
			}
			else { // must be chunked
				contentMessageEncoder = HttpContentMessageEncoder.CHUNKED;
			}

			IoBufferEx content;
			if (httpContent.hasCache()) {
				content = cachingEncoder.encode(contentMessageEncoder, httpContent, allocator, FLAG_NONE);
			}
			else {
				content = contentMessageEncoder.encode(allocator, httpContent, FLAG_NONE);
			}

			if (content.hasRemaining()) {
				out.write(content);
			}

			// Write out zero chunk if needed
	        if (isChunked && httpContent.isComplete()) {
	            out.write(allocator.wrap(ByteBuffer.wrap(ZERO_CHUNK_BYTES)));
		    }
		}
	}

	protected final void encodeHeaders(IoSessionEx session, T httpStart, IoBufferEx buf) throws CharacterCodingException {
		for (Map.Entry<String, List<String>> entry : httpStart.getHeaders().entrySet()) {
			String headerName = entry.getKey();
			List<String> headerValues = entry.getValue();
			if (headerName != null && headerValues != null) {
				for (String headerValue : headerValues) {
					encodeHeader(buf, headerName, headerValue);
				}
			}
		}

		encodeCookies(session, httpStart, buf);
		encodeContentLength(session, httpStart, buf);

		buf.put(CRLF_BYTES);
	}

	protected final void encodeHeader(IoBufferEx buf, String headerName,
			String headerValue) throws CharacterCodingException {
		buf.putString(headerName, asciiEncoder);
		buf.put(COLON_BYTES);
		buf.put(SPACE_BYTES);
		buf.putString(headerValue, asciiEncoder);
		buf.put(CRLF_BYTES);
	}

	protected void encodeContentLength(IoSessionEx session,
			T httpStart, IoBufferEx buf) throws CharacterCodingException {

        if (!httpStart.isContentLengthImplicit()) {
            HttpContentMessage httpContent = httpStart.getContent();

            // Do not send Content-Length header if gzipped or chunked
		if (httpContent == null) {
            buf.put(HEADER_CONTENT_LENGTH_BYTES);
            buf.put(ZERO_BYTES);
            buf.put(CRLF_BYTES);
		}
		else if (httpContent.isComplete() && !httpContent.isGzipped() && !httpContent.isChunked()) {
			IoBufferEx data = httpContent.asBuffer();
                int contentLength = data.remaining();

                buf.put(HEADER_CONTENT_LENGTH_BYTES);
                buf.putString(Integer.toString(contentLength), asciiEncoder);
                buf.put(CRLF_BYTES);
            }
        }
	}

	protected abstract void encodeCookies(IoSessionEx session, T httpStart, IoBufferEx buf)
			throws CharacterCodingException;

	enum HttpContentMessageEncoder implements MessageEncoder<HttpContentMessage> {
		NO_ENCODING {
	        @Override
	        public IoBufferEx encode(IoBufferAllocatorEx<?> allocator, HttpContentMessage message, int flags) {
	            return message.asBuffer();
	        }
		},

		GZIPPED {
			@Override
			public IoBufferEx encode(IoBufferAllocatorEx<?> allocator, HttpContentMessage httpContent, int flags) {
			    // No cache or cache miss - encode
				IoBufferEx content = httpContent.asBuffer();

			    if (content.remaining() > 0) {
				    content = gzipEncoder.write(content, allocator);
		        }

				return content;
			}
		},

		CHUNKED {
			@Override
			public IoBufferEx encode(IoBufferAllocatorEx<?> allocator, HttpContentMessage httpContent, int flags) {
				IoBufferEx content = httpContent.asBuffer();

			    if (content.hasRemaining()) {
				    content = chunkedEncoder.write(content, allocator);
		        }

				return content;
			}
		},

		CHUNKED_GZIPPED {
			@Override
			public IoBufferEx encode(IoBufferAllocatorEx<?> allocator, HttpContentMessage httpContent, int flags) {
				IoBufferEx content = httpContent.asBuffer();

			    if (content.remaining() > 0) {
				    content = gzipEncoder.write(content, allocator);
		        }

			    if (content.hasRemaining()) {
				    content = chunkedEncoder.write(content, allocator);
		        }

				return content;
			}
		}
	}
}
