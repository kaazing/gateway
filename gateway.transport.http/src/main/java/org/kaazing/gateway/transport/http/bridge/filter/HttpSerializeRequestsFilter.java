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

import static java.lang.String.format;
import static org.kaazing.gateway.transport.http.HttpMethod.GET;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.gateway.transport.http.bridge.HttpMessage;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;

public class HttpSerializeRequestsFilter extends HttpFilterAdapter<IoSessionEx> {

    private final Queue<HttpMessage> messageQueue = new ConcurrentLinkedQueue<HttpMessage>();
    private final Logger logger;

    private final AtomicInteger pendingRequests = new AtomicInteger();

    public HttpSerializeRequestsFilter() {
        this(getLogger(HttpSerializeRequestsFilter.class));
    }

    public HttpSerializeRequestsFilter(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {
        Object message = writeRequest.getMessage();
        // Write the message out first
        nextFilter.filterWrite(session, writeRequest);
        // And now release any queued messages or resume reads if needed.
        super.doFilterWrite(nextFilter, session, writeRequest, message);
    }

    protected void httpRequestReceived(NextFilter nextFilter, IoSessionEx session, HttpRequestMessage httpRequest)
            throws Exception {

        // extended handshake filter relies on managing suspendRead / resumeRead directly
        boolean isWebSocketHandshake = GET == httpRequest.getMethod() &&
                "websocket".equalsIgnoreCase(httpRequest.getHeader("Upgrade"));

        // not legal to pipeline HTTP requests after WebSocket handshake
        // note: still possible that a failed WebSocket handshake could
        //       have more than subsequent pipelined request (very unlikely, okay with this scenario failing for now)
        if (isWebSocketHandshake) {
            // remove from filter chain to prevent unexpected mismatch of pending complete requests in filterWrite
            session.getFilterChain().remove(this);

            // propagate WebSocket handshake request to filter chain
            nextFilter.messageReceived(session, httpRequest);

            // behave as if this filter is already removed from filter chain
            return;
        }
        httpMessageReceived(nextFilter, session, httpRequest);
    }

    @Override
    protected void httpContentReceived(NextFilter nextFilter, IoSessionEx session, HttpContentMessage httpContent)
            throws Exception {
        httpMessageReceived(nextFilter, session, httpContent);
    }

    @Override
    protected Object doFilterWriteHttpResponse(NextFilter nextFilter,
                                               IoSessionEx session,
                                               WriteRequest writeRequest,
                                               HttpResponseMessage httpResponse) throws Exception {

        resumeHttpRequests(nextFilter, session, httpResponse);
        return null;
    }

    @Override
    protected Object doFilterWriteHttpContent(NextFilter nextFilter,
                                              IoSessionEx session,
                                              WriteRequest writeRequest,
                                              HttpContentMessage httpContent) throws Exception {
        resumeHttpRequests(nextFilter, session, httpContent);
        return null;
    }



    private void httpMessageReceived(NextFilter nextFilter, IoSessionEx session, HttpMessage message) throws Exception {
        int pendingCompleteRequests;
        boolean messageIsComplete = message.isComplete();

        if (messageIsComplete) {
            // We need to synchronized across the increment and suspendRead to prevent the race of filterWrite
            // decrementing first but then messageReceived incrementing and suspendRead before filterWrite calls
            // resumeRead
            synchronized (pendingRequests) {
                pendingCompleteRequests = pendingRequests.incrementAndGet();
                if (pendingCompleteRequests == 1) {
                    // suspend reads if the first one
                    session.suspendRead();
                }
            }
        } else {
            pendingCompleteRequests = pendingRequests.get();
        }

        // send the message up if there is one pending or none (in which case we do not have a complete request yet)
        if ((pendingCompleteRequests == 1 && messageIsComplete) || pendingCompleteRequests == 0) {
            nextFilter.messageReceived(session, message);
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace(format("[tcp#%s] HTTP pipelined message received, queueing message.", session.getId()));
            }
            messageQueue.add(message);
        }
    }

    private void resumeHttpRequests(NextFilter nextFilter, IoSessionEx session, HttpMessage message)
            throws Exception {

        int pendingCompleteRequests;
        boolean completeResponse = message.isComplete();

        if (completeResponse) {
            // We need to synchronized across the decrement and readResume to prevent the race of filterWrite
            // decrementing first but then messageReceived incrementing and then call suspendRead before filterWrite
            // calls
            // resumeRead
            synchronized (pendingRequests) {
                pendingCompleteRequests = pendingRequests.decrementAndGet();
                if (pendingCompleteRequests == 0) {
                    // Resume reads if there are no more.
                    session.resumeRead();
                } else if (pendingCompleteRequests < 0) {
                    // Shouldn't happen. Unless the filter is added late mistakenly
                    throw new IllegalStateException("Wrote a response but there were no requests pending.");
                }
            }
        } else {
            pendingCompleteRequests = pendingRequests.get();
        }

        // Send another message up if this one was complete
        if (completeResponse && pendingCompleteRequests != 0) {
            if (logger.isTraceEnabled()) {
                logger.trace(format("[tcp#%s] Dequeuing HTTP pipelined message.", session.getId()));
            }
            nextFilter.messageReceived(session, messageQueue.poll());
        }
    }
}
