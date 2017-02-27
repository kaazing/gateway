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
/** The copyright above pertains to portions created by Kaazing */

package org.kaazing.gateway.transport.http.bridge.filter;

import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CONTENT_LENGTH;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CONTENT_TYPE;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.statemachine.CrLfDecodingState;
import org.apache.mina.filter.codec.statemachine.DecodingState;
import org.kaazing.gateway.transport.DecodingStateMachine;
import org.kaazing.gateway.transport.http.DefaultHttpCookie;
import org.kaazing.gateway.transport.http.HttpCookie;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpUtils;
import org.kaazing.gateway.transport.http.HttpVersion;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.filter.codec.statemachine.FixedLengthDecodingState;


public class HttpRequestDecodingState extends DecodingStateMachine {
    private static final int MAXIMUM_NON_STREAMING_CONTENT_LENGTH = 4096;
    private static final String HEADER_WEBSOCKET_KEY1 = "Sec-WebSocket-Key1";
    private static final String HEADER_HOST = "Host";
    private static final String HEADER_ORIGIN = "Origin";
    private static final String HEADER_REFERER = "Referer";
    private static final String QUERY_PARAM_DEFAULT_CONTENT_TYPE = ".kct";
    private final List<String> NULL_ORIGIN;

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
//            out.write(TERMINATOR);
            return null;
        }
    };

	private final DecodingState READ_REQUEST_MESSAGE = new DecodingStateMachine(allocator) {

		@Override
		protected DecodingState init() throws Exception {
			return READ_REQUEST_LINE;
		}

		@Override
		protected void destroy() throws Exception {
		}

		@Override
		@SuppressWarnings("unchecked")
		protected DecodingState finishDecode(List<Object> childProducts,
				ProtocolDecoderOutput out) throws Exception {

			if (childProducts.size() < 5) {
				return this;
			}

			HttpMethod method = (HttpMethod) childProducts.get(0);
			URI requestURI = (URI) childProducts.get(1);
			HttpVersion version = (HttpVersion) childProducts.get(2);
			Map<String, List<String>> headers = (Map<String, List<String>>) childProducts
					.get(3);
			Set<HttpCookie> cookies = (Set<HttpCookie>) childProducts.get(4);

            final HttpRequestMessage httpRequest = new HttpRequestMessage();
			List<String> hostHeaderValues = headers.get(HEADER_HOST);
            if (requestURI.isAbsolute()) {
                httpRequest.setAbsoluteRequestURI(requestURI);
                String expectedHostHeader = requestURI.getHost()
                        + (requestURI.getPort() == -1 ? "" : ":" + requestURI.getPort());

                if (hostHeaderValues != null) {
                    String gotHostHeader = hostHeaderValues.get(0);
                    if (!expectedHostHeader.equals(gotHostHeader)) {
                        String msg = String.format("Request URI %s is in absolute-form, hence expecting Host header %s, but got %s",
                                requestURI, expectedHostHeader, gotHostHeader);
                        throw new HttpProtocolDecoderException(msg, HttpStatus.CLIENT_BAD_REQUEST);

                    }
                } else {
                    hostHeaderValues = new ArrayList<>(1);
                    headers.put(HEADER_HOST, hostHeaderValues);
                    hostHeaderValues.add(expectedHostHeader);
                }

                String query = requestURI.getQuery();
                requestURI = (query == null)
                    ? URI.create(requestURI.getPath())
                    : URI.create(requestURI.getPath() + "?" + query);
            }   

			// KG-1469 Canonicalize Host header to make hostname lowercase to ensure correct lookup in service registry
			if (hostHeaderValues != null) {
			    int size = hostHeaderValues.size();
			    for (int i = 0; i<size; i++) {
    			    String hostPort = hostHeaderValues.get(i);
    			    String hostPortLC = hostPort.toLowerCase();
    			    if (!hostPortLC.equals(hostPort)) {
    			        hostHeaderValues.set(i, hostPortLC);
    			        headers.put(HEADER_HOST, hostHeaderValues);
    			    }
			    }
			}
			
			// KG-1474 Canonicalize hostname portion of Origin and Referer headers to lowercase to avoid spurious same origin rejection
			// by HttpCrossSiteFilter due to use of mixed case client-side WebSocket connect URI

			try { 
   				canonicalizeURIHeaders(headers, HEADER_ORIGIN);

			} catch (IllegalArgumentException iae) {
				// KG-5521: If the Origin value is malformed/unrecognized, treat it as "null".  This
				// follows the instructions of Section 6.1 of RFC 6454.
				headers.put(HEADER_ORIGIN, NULL_ORIGIN);
			}

			canonicalizeURIHeaders(headers, HEADER_REFERER);
			
			httpRequest.setSecure(secure);
			httpRequest.setMethod(method);
			httpRequest.setRequestURI(requestURI);
			httpRequest.setVersion(version);
			httpRequest.setHeaders(headers);
			httpRequest.setCookies(cookies);
           
	        // default the content-type based on query parameter for XDR which cannot specify content-type request header
	        String contentTypeHeader = httpRequest.getHeader(HEADER_CONTENT_TYPE);
	        String contentTypeParam = httpRequest.removeParameter(QUERY_PARAM_DEFAULT_CONTENT_TYPE);
	        if (contentTypeHeader == null && contentTypeParam != null) {
	            httpRequest.setHeader(HEADER_CONTENT_TYPE, contentTypeParam);
	        }

            if ((version == HttpVersion.HTTP_1_1) && isChunked(httpRequest)) {
                httpRequest.setContent(new HttpContentMessage(allocator.wrap(allocator.allocate(0)), false));
                out.write(httpRequest);
                return READ_CHUNK;
            } else {
                long length = getContentLength(httpRequest);
                String lengthValue = httpRequest.getHeader(HEADER_CONTENT_LENGTH);
                if (length > 0) {
                    if (length < MAXIMUM_NON_STREAMING_CONTENT_LENGTH) {
                        return new FixedLengthDecodingState(allocator, (int) length) {

                            @Override
                            protected DecodingState finishDecode(IoBuffer product, ProtocolDecoderOutput out) throws Exception {
                                HttpContentMessage content = new HttpContentMessage((IoBufferEx) product, true);
                                httpRequest.setContent(content);
                                out.write(httpRequest);
                                return null;
                            }
                        };
                    } else {
                        // up-streaming
                        httpRequest.setContent(new HttpContentMessage(allocator.wrap(allocator.allocate(0)), false));
                        out.write(httpRequest);
                        return new MaximumLengthDecodingState(length);
                    }
                } else if (lengthValue == null && HttpPersistenceFilter.isClosing(httpRequest)) {
                    // missing content length
                    httpRequest.setContent(new HttpContentMessage(allocator.wrap(allocator.allocate(0)), false));
                    out.write(httpRequest);

                    // deliver each received IoBuffer as an HttpContentMessage until end-of-session
                    return READ_CONTENT;
                }

                // assume no content following
                out.write(httpRequest);

                return null;
            }
        }

		private long getContentLength(HttpRequestMessage httpRequest) throws ProtocolDecoderException {
			String lengthValue = httpRequest.getHeader(HEADER_CONTENT_LENGTH);
		
			if (lengthValue != null) {
				return parseContentLength(lengthValue);
			}
			else if (httpRequest.hasHeader(HEADER_WEBSOCKET_KEY1)) {
				// If a WebSocket-76+ header is present, there are 8 bytes of trailing message
				return 8L;
			}
			else {
				return 0L;
			}
		}

		private long parseContentLength(String lengthValue)
				throws ProtocolDecoderException {
			try {
				return Long.parseLong(lengthValue);
			} catch (NumberFormatException e) {
				throw new ProtocolDecoderException("Invalid content length: " + lengthValue);
			}
		}

		private boolean isChunked(HttpRequestMessage httpRequest)
				throws ProtocolDecoderException {
			
			String transferEncoding = httpRequest.getHeader("Transfer-Encoding");
			if (transferEncoding != null) {
				int semicolonAt = transferEncoding.indexOf(';');
				if (semicolonAt != -1) {
					transferEncoding = transferEncoding.substring(0, semicolonAt);
				}
				
				if ("chunked".equalsIgnoreCase(transferEncoding)) {
					return true;
				}
			}
			
			return false;
		}
	};

	private final DecodingState READ_REQUEST_LINE = new HttpRequestLineDecodingState(allocator) {
		@Override
		protected DecodingState finishDecode(List<Object> childProducts,
				ProtocolDecoderOutput out) throws Exception {

			if (childProducts.isEmpty()) {
				return this;
			}

			HttpMethod httpMethod = (HttpMethod) childProducts.get(0);
			URI requestURI = (URI) childProducts.get(1);
			/**
			 * There to maintain backwards compatibility with 3.x clients,
			 * who may set query parameters but no path
			 */
			String path = requestURI.getPath();
            if(path == null || path.equals("")){
			    requestURI = new URI(requestURI.getScheme(), requestURI.getUserInfo(), requestURI.getHost(), requestURI.getPort(), "/".concat(path), requestURI.getQuery(), requestURI.getFragment());
			}
			HttpVersion httpVersion = (HttpVersion) childProducts.get(2);

			out.write(httpMethod);
			out.write(requestURI);
			out.write(httpVersion);

			return READ_HEADERS;
		}
	};

	private final DecodingState READ_HEADERS = new HttpHeaderDecodingState(allocator) {
		@Override
		@SuppressWarnings("unchecked")
		protected DecodingState finishDecode(List<Object> childProducts,
				ProtocolDecoderOutput out) throws Exception {

		    if (childProducts.isEmpty()) {
                return this;
            }

		    Map<String, List<String>> headers = (Map<String, List<String>>)childProducts.get(0);
			List<String> cookieHeaderValues = headers.get("Cookie");
            Set<HttpCookie> cookies = parseCookies(cookieHeaderValues);

			out.write(headers);
			out.write(cookies);
			return null;
		}
	};

	private final DecodingState READ_CHUNK = new HttpChunkDecodingState(allocator) {
		@Override
		protected DecodingState finishDecode(List<Object> childProducts,
				ProtocolDecoderOutput out) throws Exception {

		    if (childProducts.isEmpty()) {
	            throw new ProtocolDecoderException("Expected a chunk");
		    }
		    
			IoBufferEx data = (IoBufferEx) childProducts.get(0);
			boolean terminator = !data.hasRemaining();
			HttpContentMessage content = new HttpContentMessage(data, terminator);
			out.write(content);

			return terminator ? null : READ_CHUNK;
		}
	};

	private final boolean secure;

	public HttpRequestDecodingState(IoBufferAllocatorEx<?> allocator, boolean secure) {
	    super(allocator);
        this.NULL_ORIGIN = new ArrayList<>(1);
        this.NULL_ORIGIN.add("null"); 
		this.secure = secure;
	}

	@Override
	protected DecodingState init() throws Exception {
		return READ_REQUEST_MESSAGE;
	}

	@Override
	protected void destroy() throws Exception {
	}

	@Override
	public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out)
			throws Exception {
		DecodingState decodingState = super.decode(in, out);
		flush(childProducts, out);
		return decodingState;
	}

	@Override
	protected DecodingState finishDecode(List<Object> childProducts,
			ProtocolDecoderOutput out) throws Exception {
		flush(childProducts, out);
		return null;
	}
	
	private void canonicalizeURIHeaders(Map<String, List<String>> headers, String... headerNames) {
	    for (String headerName : headerNames) {
            List<String> headerValues = headers.get(headerName);
            if (headerValues != null) {
                int size = headerValues.size();
                for (int i = 0; i<size; i++) {
                    String value = headerValues.get(i);
                    if (value.isEmpty())
                        continue; // KG-11212: NullPointerException when header value is empty, effect: client
                                  // connection closed abruptly
                    String valueLC = HttpUtils.getCanonicalURI(value, false).toString();
                    if (!valueLC.equals(value)) {
                        headerValues.set(i, valueLC);
                        headers.put(headerName, headerValues);
                    }
                }
            }
	    }
	}

	private void flush(List<Object> childProducts, ProtocolDecoderOutput out) {
		// flush child products to parent output before decode is complete
		for (Iterator<Object> i = childProducts.iterator(); i.hasNext();) {
			Object product = i.next();
			i.remove();
			out.write(product);
		}
	}

    private Set<HttpCookie> parseCookies(List<String> cookieHeaderValues) {
        // parse cookies
        Set<HttpCookie> cookies = new HashSet<>();
        if (cookieHeaderValues != null && !cookieHeaderValues.isEmpty()) {

        	String cookieHeaderValue = cookieHeaderValues.get(0);
        	DefaultHttpCookie currentCookie = null;

        	int version = -1; // -1 means version is not parsed yet.
        	int fieldIdx = 0;

        	StringTokenizer tk = new StringTokenizer(cookieHeaderValue, ";,");
        	while (tk.hasMoreTokens()) {
        		String pair = tk.nextToken();
        		String key;
        		String value;

        		int equalsPos = pair.indexOf('=');
        		if (equalsPos >= 0) {
        			key = pair.substring(0, equalsPos).trim();
        			value = pair.substring(equalsPos + 1).trim();
        		} else {
        			key = pair.trim();
        			value = "";
        		}

        		if (version < 0) {
        			if (!key.equalsIgnoreCase("$Version")) {
        				// $Version is not specified. Use the default (0).
        				version = 0;
        			} else {
        				version = Integer.parseInt(value);
        				if (version != 0 && version != 1) {
        					throw new IllegalArgumentException(
        							"Invalid version: " + version + " ("
        									+ cookieHeaderValue + ")");
        				}
        			}
        		}

        		if (version >= 0) {
        			try {
        				switch (fieldIdx) {
        				case 1:
        					if (key.equalsIgnoreCase("$Path")) {
        						currentCookie.setPath(value);
        						fieldIdx++;
        					} else {
        						fieldIdx = 0;
        					}
        					break;
        				case 2:
        					if (key.equalsIgnoreCase("$Domain")) {
        						currentCookie.setDomain(value);
        						fieldIdx++;
        					} else {
        						fieldIdx = 0;
        					}
        					break;
        				}
        			} catch (NullPointerException e) {
        				throw new IllegalArgumentException(
        						"Cookie key-value pair not found ("
        								+ cookieHeaderValue + ")");
        			}

        			if (fieldIdx == 0) {
        				currentCookie = new DefaultHttpCookie(key);
        				currentCookie.setVersion(version);
        				currentCookie.setValue(value);
        				cookies.add(currentCookie);
        				fieldIdx++;
        			}
        		}
        	}
        }
        return cookies;
    }

}
