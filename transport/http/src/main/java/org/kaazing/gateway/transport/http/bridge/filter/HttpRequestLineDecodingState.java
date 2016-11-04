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
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.regex.Pattern;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.statemachine.CrLfDecodingState;
import org.apache.mina.filter.codec.statemachine.DecodingState;
import org.apache.mina.filter.codec.statemachine.LinearWhitespaceSkippingState;
import org.kaazing.gateway.transport.DecodingStateMachine;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpVersion;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.filter.codec.statemachine.ConsumeToDynamicTerminatorDecodingState;
import org.kaazing.mina.filter.codec.statemachine.ConsumeToLinearWhitespaceDecodingState;


public abstract class HttpRequestLineDecodingState extends DecodingStateMachine {

    private static final Pattern MULTIPLE_LEADING_SLASHES = Pattern.compile("^/[/]+");
    private static final Pattern MULTIPLE_SLASHES = Pattern.compile("/[/]+");
    private static final String SINGLE_SLASH = "/";
    private static final int MAX_HTTP_URI_LENGTH_ALLOWED = 8192; // 8KB

    private static final Charset US_ASCII = Charset.forName("US-ASCII");
    private static final CharsetDecoder US_ASCII_DECODER = US_ASCII
            .newDecoder();

    private final Charset UTF_8 = Charset.forName("UTF-8");
    private final CharsetDecoder UTF_8_DECODER = UTF_8.newDecoder();

    private static final byte[] INITIAL_METHOD_BYTES;
    
    static {
        HttpMethod[] httpMethods = HttpMethod.values();
        byte[] initialMethodBytes = new byte[httpMethods.length];
        for (int i=0; i < initialMethodBytes.length; i++) {
            initialMethodBytes[i] = (byte)httpMethods[i].name().charAt(0);
        }
        INITIAL_METHOD_BYTES = initialMethodBytes;
    }
    
    private final DecodingState VALIDATE_METHOD_START = new DecodingState() {

        @Override
        public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out) throws Exception {
            if (in.hasRemaining()) {
                byte initialByte = in.get(in.position());
                for (byte initialMethodByte : INITIAL_METHOD_BYTES) {
                    if (initialByte == initialMethodByte) {
                        return READ_METHOD;
                    }
                }
                throw new ProtocolDecoderException("Unexpected start of HTTP request: " + in.getHexDump());
            } 
            else {
                return this;
            }
        }

        @Override
        public DecodingState finishDecode(ProtocolDecoderOutput out) throws Exception {
            return null;
        }
        
    };
    
    private final DecodingState READ_METHOD = new ConsumeToLinearWhitespaceDecodingState(allocator) {
        @Override
        protected DecodingState finishDecode(IoBuffer buffer,
                ProtocolDecoderOutput out) throws Exception {
            if (!buffer.hasRemaining()) {
                return this;
            }

            String httpMethod = buffer.getString(US_ASCII_DECODER);
            HttpMethod method;
            try
            {
                method = HttpMethod.valueOf(httpMethod);
            } catch(IllegalArgumentException e) {
                throw new HttpProtocolDecoderException(HttpStatus.SERVER_NOT_IMPLEMENTED);
            }
            out.write(method);
            return AFTER_READ_METHOD;
        }

        @Override
        protected void validate(IoBufferEx buffer, int startPos) throws Exception {
            int oldPos = buffer.position();
            try {
                buffer.position(startPos);
                String partialHttpMethod = buffer.getString(US_ASCII_DECODER);
                for (HttpMethod httpMethod: HttpMethod.values()) {
                    if (httpMethod.toString().startsWith(partialHttpMethod.toUpperCase())) {
                        return;
                    }
                }
            } finally {
                buffer.position(oldPos);  
            }

            throw new HttpProtocolDecoderException(HttpStatus.CLIENT_BAD_REQUEST);
        };

    };

    private final DecodingState AFTER_READ_METHOD = new LinearWhitespaceSkippingState() {
        @Override
        protected DecodingState finishDecode(int skippedBytes) throws Exception {
            return READ_REQUEST_URI;
        }
    };

    private final DecodingState READ_REQUEST_URI = new ConsumeToLinearWhitespaceDecodingState(allocator) {
        @Override
        protected DecodingState finishDecode(IoBuffer buffer,
                ProtocolDecoderOutput out) throws Exception {

            // extract the request string from the byte buffer
            String request = buffer.getString(UTF_8_DECODER);

            // check the length of the request
            if(request.length() > MAX_HTTP_URI_LENGTH_ALLOWED) {
                throw new HttpProtocolDecoderException(HttpStatus.CLIENT_REQUEST_URI_TOO_LONG);
            }

            // handle special case of leading double slash
            // to avoid mistakenly parsing the URI as having an authority
            request = MULTIPLE_LEADING_SLASHES.matcher(request).replaceAll(SINGLE_SLASH);

            // parse request as URI
            URI requestURI = new URI(request);
            
            // canonicalize slashes in request path
            String path = requestURI.getPath();
            String canonicalPath = MULTIPLE_SLASHES.matcher(path).replaceAll(SINGLE_SLASH);
            if (!path.equals(canonicalPath)) {
                String scheme = requestURI.getScheme();
                String authority = requestURI.getAuthority();
                String query = requestURI.getQuery();
                String fragment = requestURI.getFragment();
                requestURI = new URI(scheme, authority, canonicalPath, query, fragment);
            }
            
            // output the canonical request URI
            out.write(requestURI);
            
            return AFTER_READ_URI;
        }

    };

    private final DecodingState AFTER_READ_URI = new LinearWhitespaceSkippingState() {
        @Override
        protected DecodingState finishDecode(int skippedBytes) throws Exception {
            return READ_VERSION;
        }
    };

    private final DecodingState READ_VERSION = new ConsumeToDynamicTerminatorDecodingState(allocator) {
        @Override
        protected DecodingState finishDecode(IoBuffer buffer,
                ProtocolDecoderOutput out) throws Exception {
            String httpVersion = buffer.getString(US_ASCII_DECODER);
            HttpVersion version;
            try
            {
                version = HttpVersion.valueOf(httpVersion
                        .replaceAll("\\/|\\.", "_"));
            } catch(IllegalArgumentException e) {
                throw new HttpProtocolDecoderException(HttpStatus.CLIENT_BAD_REQUEST);
            }
            out.write(version);
            return AFTER_READ_VERSION;
        }

        @Override
        protected boolean isTerminator(byte b) {
            return (Character.isWhitespace(b));
        }
    };

    private final DecodingState AFTER_READ_VERSION = new LinearWhitespaceSkippingState() {
        @Override
        protected DecodingState finishDecode(int skippedBytes) throws Exception {
            return READ_END_OF_LINE;
        }
    };

    private final DecodingState READ_END_OF_LINE = new CrLfDecodingState() {
        @Override
        protected DecodingState finishDecode(boolean foundCRLF,
                ProtocolDecoderOutput out) throws Exception {
            if (!foundCRLF) {
                throw new ProtocolDecoderException(
                        "Expected CRLF at end of line");
            }

            return null;
        }
    };

    public HttpRequestLineDecodingState(IoBufferAllocatorEx<?> allocator) {
        super(allocator);
    }

    @Override
    protected DecodingState init() throws Exception {
        return VALIDATE_METHOD_START;
    }

    @Override
    protected void destroy() throws Exception {
    }
}
