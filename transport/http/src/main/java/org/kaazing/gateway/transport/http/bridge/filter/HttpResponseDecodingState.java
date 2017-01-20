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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.statemachine.DecodingState;
import org.kaazing.gateway.transport.DecodingStateMachine;
import org.kaazing.gateway.transport.http.DefaultHttpCookie;
import org.kaazing.gateway.transport.http.HttpCookie;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.gateway.transport.http.HttpSession;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpVersion;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.filter.codec.statemachine.FixedLengthDecodingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpResponseDecodingState extends DecodingStateMachine {
    private static final int MAXIMUM_NON_STREAMING_CONTENT_LENGTH = 4096;

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpResponseDecodingState.class);
    private final HttpSession httpSession;

    HttpResponseDecodingState(IoBufferAllocatorEx<?> allocator, HttpSession httpSession) {
        super(allocator);
        this.httpSession = httpSession;
    }

    private final DecodingState READ_RESPONSE_MESSAGE = new DecodingStateMachine(allocator) {

        @Override
        protected DecodingState init() throws Exception {
            return READ_RESPONSE_LINE;
        }

        @Override
        protected void destroy() throws Exception {
        }

        @Override
        @SuppressWarnings("unchecked")
        protected DecodingState finishDecode(List<Object> childProducts, ProtocolDecoderOutput out) throws Exception {

            if (childProducts.isEmpty()) {
                return this;
            }

            HttpVersion version = (HttpVersion) childProducts.get(0);
            HttpStatus status = (HttpStatus) childProducts.get(1);
            String reason = (String) childProducts.get(2);
            Map<String, List<String>> headers = (Map<String, List<String>>) childProducts.get(3);
            Set<HttpCookie> cookies = (Set<HttpCookie>) childProducts.get(4);

            final HttpResponseMessage httpResponse = new HttpResponseMessage();
            httpResponse.setVersion(version);
            httpResponse.setStatus(status);
            httpResponse.setReason(reason);
            httpResponse.setHeaders(headers);
            httpResponse.setCookies(cookies);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("\"" + status + " " + httpResponse.getReason() + " " + version + "\"");
            }

            if (status == HttpStatus.REDIRECT_NOT_MODIFIED) {
                httpResponse.setContent(new HttpContentMessage(allocator.wrap(allocator.allocate(0)), true));
                out.write(httpResponse);
                return null;
            } else if (httpSession != null && httpSession.getMethod() == HttpMethod.HEAD) {
                httpResponse.setContent(new HttpContentMessage(allocator.wrap(allocator.allocate(0)), true));
                out.write(httpResponse);
                return null;
            } else if ((version == HttpVersion.HTTP_1_1) && isChunked(httpResponse)) {
                httpResponse.setContent(new HttpContentMessage(allocator.wrap(allocator.allocate(0)), false));
                out.write(httpResponse);
                return READ_CHUNK;
            } else {
                String lengthValue = httpResponse.getHeader(HEADER_CONTENT_LENGTH);
                if (lengthValue != null) {
                    int length = parseContentLength(lengthValue);
                    if (length > 0) {
                        if (length < MAXIMUM_NON_STREAMING_CONTENT_LENGTH) {
                            return new FixedLengthDecodingState(allocator, length) {
                                @Override
                                protected DecodingState finishDecode(IoBuffer readData, ProtocolDecoderOutput out)
                                        throws Exception {
                                    HttpContentMessage content = new HttpContentMessage((IoBufferEx) readData, true);
                                    httpResponse.setContent(content);
                                    out.write(httpResponse);
                                    return null;
                                }
                            };
                        } else {
                            // down-streaming
                            httpResponse
                                    .setContent(new HttpContentMessage(allocator.wrap(allocator.allocate(0)), false));
                            out.write(httpResponse);
                            return new MaximumLengthDecodingState(length);
                        }
                    } else {
                        out.write(httpResponse);
                        return null;
                    }
                } else if (HttpPersistenceFilter.isClosing(httpResponse)) {
                    // missing content length
                    httpResponse.setContent(new HttpContentMessage(allocator.wrap(allocator.allocate(0)), false));
                    out.write(httpResponse);

                    // deliver each received IoBuffer as an HttpContentMessage until end-of-session
                    return READ_CONTENT;
                }

                // assume no content following
                out.write(httpResponse);

                // handle 101 Switching Protocols upgrade
                if (status == HttpStatus.INFO_SWITCHING_PROTOCOLS) {
                    // Note: the httpResponse above will be flushed first, triggering the upgrade
                    // and remove the HTTP codec filter from the filter chain
                    // but the codec filter will see bytes are remaining (if any WebSocket
                    // frame bytes were passed into this HTTP decoder along with the preceding
                    // HTTP 101 response bytes) and then continue to loop until all bytes are
                    // consumed. So, we return the after-upgrade decoder state to pass any
                    // remaining bytes through to the WebSocket frame decoding logic.
                    // These pass-through bytes will be flushed from the codec filter even
                    // though it has already been removed from the filter chain while processing
                    // the 101 HTTP response message.
                    return AFTER_UPGRADE;
                }

                return null;
            }
        }

        private int parseContentLength(String lengthValue) throws ProtocolDecoderException {
            try {
                return Integer.parseInt(lengthValue);
            } catch (NumberFormatException e) {
                throw new ProtocolDecoderException("Invalid content length: " + lengthValue);
            }
        }

        private boolean isChunked(HttpResponseMessage httpResponse) throws ProtocolDecoderException {

            String transferEncoding = httpResponse.getHeader("Transfer-Encoding");
            if (transferEncoding != null) {
                int semicolonAt = transferEncoding.indexOf(';');
                if (semicolonAt != -1) {
                    transferEncoding = transferEncoding.substring(0, semicolonAt);
                }

                if ("chunked".equalsIgnoreCase(transferEncoding)) {
                    return true;
                }

                throw new ProtocolDecoderException("Unexpected transfer coding: " + transferEncoding);
            }

            return false;
        }
    };

    private final DecodingState READ_RESPONSE_LINE = new HttpResponseLineDecodingState(allocator) {
        @Override
        protected DecodingState finishDecode(List<Object> childProducts, ProtocolDecoderOutput out) throws Exception {

            if (childProducts.isEmpty()) {
                return this;
            }

            HttpVersion httpVersion = (HttpVersion) childProducts.get(0);
            HttpStatus httpStatus = (HttpStatus) childProducts.get(1);
            String httpReason = (String) childProducts.get(2);

            out.write(httpVersion);
            out.write(httpStatus);
            out.write(httpReason);

            return READ_HEADERS;
        }
    };

    private final DecodingState READ_HEADERS = new HttpHeaderDecodingState(allocator) {
        @Override
        @SuppressWarnings("unchecked")
        protected DecodingState finishDecode(List<Object> childProducts, ProtocolDecoderOutput out) throws Exception {
            Map<String, List<String>> headers = (Map<String, List<String>>) childProducts.get(0);

            // parse cookies
            Set<HttpCookie> cookies = new HashSet<>();
            List<String> cookieHeaderValues = headers.get("Set-Cookie");
            if (cookieHeaderValues != null && !cookieHeaderValues.isEmpty()) {
                String cookieHeaderValue = cookieHeaderValues.get(0);
                String[] cookieValues = cookieHeaderValue.split(",");
                for (String cookieValue : cookieValues) {
                    String[] nvPairs = cookieValue.split(";");
                    int nvPairCount = nvPairs.length;
                    if (nvPairCount > 0) {
                        String[] nvPair = nvPairs[0].split("=");

                        // create the cookie name and value
                        DefaultHttpCookie cookie = new DefaultHttpCookie(nvPair[0].trim());
                        if (nvPair.length > 1) {
                            cookie.setValue(nvPair[1].trim());
                        }

                        // read the cookie properties
                        for (int i = 1; i < nvPairCount; i++) {
                            nvPair = nvPairs[i].split("=");
                            boolean hasValue = nvPair.length > 1;
                            String avName = nvPair[0].trim();
                            if (avName.length() > 0) {
                                switch (avName.charAt(0)) {
                                case 'c':
                                case 'C':
                                    if ("Comment".equalsIgnoreCase(avName) && hasValue) {
                                        cookie.setComment(nvPair[1].trim());
                                    }
                                    break;
                                case 'd':
                                case 'D':
                                    if ("Domain".equalsIgnoreCase(avName) && hasValue) {
                                        cookie.setDomain(nvPair[1].trim());
                                    }
                                    break;
                                case 'm':
                                case 'M':
                                    if ("Max-Age".equalsIgnoreCase(avName) && hasValue) {
                                        cookie.setMaxAge(Integer.parseInt(nvPair[1].trim()));
                                    }
                                    break;
                                case 'p':
                                case 'P':
                                    if ("Path".equalsIgnoreCase(avName) && hasValue) {
                                        cookie.setPath(nvPair[1].trim());
                                    }
                                    break;
                                case 's':
                                case 'S':
                                    if ("Secure".equalsIgnoreCase(avName)) {
                                        cookie.setSecure(true);
                                    }
                                    break;
                                case 'v':
                                case 'V':
                                    if ("Version".equalsIgnoreCase(avName) && hasValue) {
                                        cookie.setVersion(Integer.parseInt(nvPair[1].trim()));
                                    }
                                    break;
                                }
                            }
                        }

                        cookies.add(cookie);
                    }
                }
            }

            out.write(headers);
            out.write(cookies);

            return null;
        }
    };

    private final DecodingState READ_CHUNK = new HttpChunkDecodingState(allocator) {
        @Override
        protected DecodingState finishDecode(List<Object> childProducts, ProtocolDecoderOutput out) throws Exception {

            if (childProducts.isEmpty()) {
                throw new ProtocolDecoderException("Expected a chunk");
            }

            IoBufferEx data = (IoBufferEx) childProducts.get(0);
            if (data.hasRemaining()) {
                out.write(new HttpContentMessage(data, false));
                return READ_CHUNK;
            } else {
                out.write(new HttpContentMessage(allocator.wrap(allocator.allocate(0)), true));
                return null;
            }
        }
    };

    private static final DecodingState READ_CONTENT = new DecodingState() {
        @Override
        public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out) throws Exception {
            HttpContentMessage content = new HttpContentMessage((IoBufferEx) in.duplicate(), false);
            out.write(content);
            in.position(in.limit());
            return this;
        }

        @Override
        public DecodingState finishDecode(ProtocolDecoderOutput out) throws Exception {
            // out.write(TERMINATOR);
            return null;
        }
    };

    private final DecodingState AFTER_UPGRADE = new DecodingState() {
        @Override
        public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out) throws Exception {
            out.write(in.duplicate());
            in.position(in.limit());
            return this;
        }

        @Override
        public DecodingState finishDecode(ProtocolDecoderOutput out) throws Exception {
            return null;
        }
    };

    @Override
    protected DecodingState init() throws Exception {
        return READ_RESPONSE_MESSAGE;
    }

    @Override
    protected void destroy() throws Exception {
    }

    @Override
    public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        DecodingState decodingState = super.decode(in, out);
        flush(childProducts, out);
        return decodingState;
    }

    @Override
    protected DecodingState finishDecode(List<Object> childProducts, ProtocolDecoderOutput out) throws Exception {
        flush(childProducts, out);
        return null;
    }

    private void flush(List<Object> childProducts, ProtocolDecoderOutput out) {
        // flush child products to parent output before decode is complete
        for (Iterator<Object> i = childProducts.iterator(); i.hasNext();) {
            Object product = i.next();
            i.remove();
            out.write(product);
        }
    }
}
