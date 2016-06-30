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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.transport.TypedAttributeKey;
import org.kaazing.gateway.transport.http.HttpProtocol;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpUtils;
import org.kaazing.gateway.transport.http.WsHandshakeValidator;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.gateway.util.ws.WebSocketWireProtocol;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.future.DefaultWriteFutureEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.core.write.DefaultWriteRequestEx;
import org.slf4j.Logger;

/**
 * If we detect a web socket handshake, that also contains the x-kaazing-handshake protocol,
 * this filter is responsible for:
 * <ul>
 * <li>remembering the http request and merging with the second request</li>
 * </ul>
 * <p>
 * It should pass the Http request body data through the HttpCodec back to this  {@link HttpMergeRequestFilter}.
 * This filter then verifies that the new request matches the initial request, and sends the new request up the
 * Http filter chain to create a WsSession.  In turn, that process should see an HttpResponse come back along the chain,
 * in which case the HttpExtendedHandshakeFilter at the end of the chain will upgrade the packet to a WebSocket message and
 * send it back out.  This filter then removes the HttpExtendedHandshakeFilter if the HTTP response was anything, except a 401
 * in which case negotiation of authorization is still underway.</p>
 * <p/>
 * <p>This class uses a state machine to track whether it is dealing with the initial or extended handshake requests and responses.</p>
 * <p/>
 */
// note: used at http/1.1 and httpx/1.1 layers, no-op at httpxe/1.1 layer
public class HttpMergeRequestFilter extends HttpFilterAdapter<IoSessionEx> {

    public static final TypedAttributeKey<IoBufferEx> DRAFT76_KEY3_BUFFER_KEY
            = new TypedAttributeKey<>(HttpMergeRequestFilter.class, "draft76Key3Buffer");

    public static final String HEADER_X_WEBSOCKET_EXTENSIONS = "X-WebSocket-Extensions";
    public static final String HEADER_WEBSOCKET_EXTENSIONS = "WebSocket-Extensions";
    public static final String HEADER_SEC_WEBSOCKET_EXTENSIONS = "Sec-WebSocket-Extensions";

    private static final List<String> NATIVE_EXTENSION_HEADERS = Arrays.asList(HEADER_SEC_WEBSOCKET_EXTENSIONS,
           HEADER_WEBSOCKET_EXTENSIONS);

    private static final String HEADER_WEBSOCKET_PROTOCOL = "WebSocket-Protocol";
    private static final String HEADER_SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";


    /**
     * The name of this filter.
     */
    public static final String NAME = HttpProtocol.NAME + "#mergeRequestFilter";

    /**
     * The name of the extended handshake protocol to be sent on the wire.
     */
    public static final String EXTENDED_HANDSHAKE_PROTOCOL_NAME = "x-kaazing-handshake";

    /**
     * The logger to use to log upgrade event information.
     */
    private final Logger logger;

    /**
     * Session attribute: HttpRequestMessage initialHttpRequest;
     * <p/>
     * The initial WebSocket Upgrade request.
     */
    public static final TypedAttributeKey<HttpRequestMessage> INITIAL_HTTP_REQUEST_KEY =
            new TypedAttributeKey<>(HttpMergeRequestFilter.class, "initialHttpRequest");


    /**
     * Install a logger.
     *
     * @param logger the logger to used for logging events.
     */
    public HttpMergeRequestFilter(Logger logger) {
        this.logger = logger;
    }
    
    public HttpMergeRequestFilter() {
        this.logger = null;
    }


    @Override
    protected void httpRequestReceived(NextFilter nextFilter, IoSessionEx session, HttpRequestMessage httpRequest)
            throws Exception {
        // GL.debug("http", getClass().getSimpleName() + " request received.");

        HttpRequestMessage initialHttpRequest = INITIAL_HTTP_REQUEST_KEY.get(session);

        if ( loggerEnabled() ) {
            logger.trace(String.format("HttpMergeRequestFilter: Entering Request is '%s'.", toLogString(httpRequest)));
        }

        HttpContentMessage mergedDraft76Key3 = null;
        //
        // Handle the state where we are receiving an extended request....
        // Process this early so that we construct the "full" request to profile and pass up the chain.
        //
        if (initialHttpRequest != null) {


            if ( loggerEnabled() ) {
                logger.trace(String.format("HttpMergeRequestFilter: Retrieved initial request '%s'.", toLogString(initialHttpRequest)));
            }

            if (!mergeRequests(initialHttpRequest, httpRequest)) {
                if ( loggerEnabled() ) {
                    logger.trace("The opening handshake ('"+initialHttpRequest.getRequestURI()+"') cannot be reconciled with the extended handshake '"+httpRequest.getRequestURI()+"'.");
                }
                WriteFuture f = writeHttpResponse(nextFilter, session, httpRequest, HttpStatus.CLIENT_BAD_REQUEST);
                f.addListener(IoFutureListener.CLOSE);
                return;
            }

            if ( loggerEnabled() ) {
                logger.trace(String.format("HttpMergeRequestFilter: Merged httpRequest is '%s'.", toLogString(httpRequest)));
            }


            if ( loggerEnabled() ) {
                logger.trace("HttpMergeRequestFilter: 'Extended request received'.");
            }

            // Do we need to find a key3 to complete the merged request?
            IoBufferEx key3 = DRAFT76_KEY3_BUFFER_KEY.get(session);
            if (key3 != null ) {
                mergedDraft76Key3 = new HttpContentMessage(key3, true);
            }

            if ( loggerEnabled() ) {
                logger.trace("EHS: Suspend reads until the extended request has been responded to.");
            }

            // We are ok to pass the request through to do the real work.
        }

        //
        // We cannot accept native (opening handshake) extensions for protocol
        // versions that do not support them. In extended requests they are
        // fine though.
        //
        WebSocketWireProtocol protocol = WsHandshakeValidator.guessWireProtocolVersion(httpRequest);
        if ( protocol != null &&
             initialHttpRequest == null  &&
             !protocol.areNativeExtensionsSupported() &&
             nativeExtensionsRequested(httpRequest)) {
            if ( loggerEnabled() ) {
                logger.trace("WebSocket Protocol "+protocol+" clients cannot negotiate extensions.");
            }
            WriteFuture f = writeHttpResponse(nextFilter, session, httpRequest, HttpStatus.CLIENT_BAD_REQUEST);
            f.addListener(IoFutureListener.CLOSE);
            return;
        }

        //
        // We have a WebSocket upgrade request now.
        // Check for the extended handshake protocol with old and new headers.
        //
        List<String> protocols = httpRequest.getHeaderValues(HEADER_WEBSOCKET_PROTOCOL, false);
        if (protocols == null || protocols.isEmpty()) {
            protocols = httpRequest.getHeaderValues(HEADER_SEC_WEBSOCKET_PROTOCOL, false);
        }


        //
        // Handle the state where no extended handshake requirement has yet been detected...
        // We are receiving the initial WebSocket upgrade request...and see the extended handshake requested
        //
        if (protocols != null &&
            protocols.contains(EXTENDED_HANDSHAKE_PROTOCOL_NAME) &&
            initialHttpRequest == null) {

            if (loggerEnabled()) {
                logger.trace(EXTENDED_HANDSHAKE_PROTOCOL_NAME +" detected.");
            }

            // Remember the request for subsequent 'verification' of the extended request.
            INITIAL_HTTP_REQUEST_KEY.set(session, httpRequest);
        }


        // Add on a WebSocket Draft 76 key3 content message if needed.
        if ( mergedDraft76Key3 != null ) {
            HttpContentMessage httpContent = httpRequest.getContent();
            if (httpContent == null || (httpContent.isComplete() && httpContent.asBuffer().capacity() == 0)) {
                httpRequest.setContent(mergedDraft76Key3);
            }
        }

        // Send the HTTP request up the chain.
        super.httpRequestReceived(nextFilter, session, httpRequest);

    }

    public static boolean nativeExtensionsRequested(HttpRequestMessage httpRequest) {

        final Map<String,List<String>> headers = httpRequest.getHeaders();
        Set<String> headerNames = headers == null? null : headers.keySet();

        if ( headerNames == null) {
            return false;
        }

        for ( String h: NATIVE_EXTENSION_HEADERS) {
            if (headerNames.contains(h)) {
                return true;
            }
        }

        return false;
    }




    /**
     * Clean up ourselves from the filter chain after we have written a non-401 extended response.
     * A 401-coded extended response means that the extended handshake is still ongoing (state "EXTENDED_RESPONSE_WRITTEN").
     * A non-401-coded extended response means that the extended handshake is complete (state "EXTENDED_RESPONSE_COMPLETE").
     *
     * @param nextFilter   the next filter
     * @param session      the IO session
     * @param writeRequest the write request
     * @throws Exception when a problem occurs
     */
    @Override
    protected void filterWriteHttpResponse(NextFilter nextFilter, final IoSessionEx session, WriteRequest writeRequest,
            final HttpResponseMessage httpResponse) throws Exception {

        if ( session instanceof BridgeSession ) {
            writeRequest.getFuture().addListener(new IoFutureListener<IoFuture>() {
                @Override
                public void operationComplete(IoFuture future) {
                    if (httpResponse.getStatus() != HttpStatus.CLIENT_UNAUTHORIZED) {
                        DRAFT76_KEY3_BUFFER_KEY.remove(session);
                        removeFilter(session.getFilterChain(), HttpMergeRequestFilter.this);
                        if ( loggerEnabled()) {
                            logger.trace(String.format("HttpMergeRequestFilter: response complete; removed merge request filter from session '%s'.", session));
                        }
                    } else {
                        if ( loggerEnabled()) {
                            logger.trace(String.format("HttpMergeRequestFilter: 401 response written to session '%s'.", session));
                        }
                    }
                }
            });
        }

        super.filterWriteHttpResponse(nextFilter, session, writeRequest, httpResponse);
    }


    /* For logging and debugging only */
    private Object toLogString(HttpRequestMessage httpRequest) {
        StringBuilder b = new StringBuilder();
        b.append(httpRequest.getRequestURI()).append('\n');
        final Map<String,List<String>> headers = httpRequest.getHeaders();
        for ( String h: headers.keySet()) {
            b.append(h).append(':').append(' ').append(headers.get(h)).append('\n');
        }
        return b.toString();
    }


    /**
     * Write a fresh HttpResponse from this filter down the filter chain, based on the provided request, with the specified http status code.
     *
     * @param nextFilter  the next filter in the chain
     * @param session     the IO session
     * @param httpRequest the request that the response corresponds to
     * @param httpStatus  the desired status of the http response
     * @param reason      the reason description (optional)
     * @return  a write future for the response written
     */
    private WriteFuture writeHttpResponse(NextFilter nextFilter, IoSession session, HttpRequestMessage httpRequest, final HttpStatus httpStatus, final String reason) {
        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(httpRequest.getVersion());
        httpResponse.setStatus(httpStatus);
        if (reason != null) {
            httpResponse.setReason(reason);
        }
        final DefaultWriteFutureEx future = new DefaultWriteFutureEx(session);
        nextFilter.filterWrite(session, new DefaultWriteRequestEx(httpResponse, future));
        return future;
    }

    /**
     * Write a fresh HttpResponse from this filter down the filter chain, based on the provided request, with the specified http status code.
     *
     * @param nextFilter  the next filter in the chain
     * @param session     the IO session
     * @param httpRequest the request that the response corresponds to
     * @param httpStatus  the desired status of the http response
     * @return a writeFuture for the written response
     */
    private WriteFuture writeHttpResponse(NextFilter nextFilter, IoSession session, HttpRequestMessage httpRequest, final HttpStatus httpStatus) {
        return writeHttpResponse(nextFilter, session, httpRequest, httpStatus, null);
    }


    /**
     * Return true iff the extended request matches the initial request.
     * <p/>
     * This function, when returning true, additionally ensures that the extended request
     * contains no illegal headers, and has had selected header elements from the initial
     * request merged into the extended request.
     *
     * @param from  the initial request
     * @param to the extended request, possibly amended with a merged set of headers.
     * @return true iff the extended request is the same as the initial request.
     */
    private boolean mergeRequests(HttpRequestMessage from, HttpRequestMessage to) {

        if (from == null || to == null) {
            return false;
        }

        final URI initialRequestURI = from.getRequestURI();
        final URI extendedRequestURI = to.getRequestURI();

        boolean uriPathOk = initialRequestURI.getPath().equals(extendedRequestURI.getPath());

        if ( !uriPathOk ) {
            return false;
        }

        // Note: the fact that "Host" is an allowance was required by KG-3481.
        final String[] allowances = {"Host", "Sec-WebSocket-Protocol", HEADER_SEC_WEBSOCKET_EXTENSIONS, "Connection"};
        if ( HttpUtils.containsForbiddenHeaders(to, allowances) ) {
            return false;
        }

        final String[] restrictions = {"Authorization", "X-WebSocket-Protocol", "WebSocket-Protocol", "Sec-WebSocket-Protocol",
                                       HEADER_X_WEBSOCKET_EXTENSIONS, HEADER_WEBSOCKET_EXTENSIONS, HEADER_SEC_WEBSOCKET_EXTENSIONS};

        HttpUtils.restrictHeaders(to, restrictions);

        final String[] ignoreHeaders = {HEADER_X_WEBSOCKET_EXTENSIONS,
                                        HEADER_WEBSOCKET_EXTENSIONS,
                                        HEADER_SEC_WEBSOCKET_EXTENSIONS};
        HttpUtils.mergeHeaders(from, to, ignoreHeaders);

        final String[] protocolHeaders = {"X-WebSocket-Protocol", "WebSocket-Protocol", "Sec-WebSocket-Protocol"};
        HttpUtils.removeValueFromHeaders(to, protocolHeaders, EXTENDED_HANDSHAKE_PROTOCOL_NAME);

        HttpUtils.mergeParameters(from, to);

        HttpUtils.mergeCookies(from, to);

        return true;
        
    }

    /**
     * A utility method to remove a filter from a filter chain, without
     * complaining if it is not in the chain.
     *
     * @param filterChain the filter chain
     * @param filter      the filter to remove
     */
    protected final void removeFilter(IoFilterChain filterChain, IoFilter filter) {
        if (filterChain.contains(filter)) {
            filterChain.remove(filter);
        }
    }




    protected boolean loggerEnabled() {
        return logger != null && logger.isTraceEnabled();
    }



}


