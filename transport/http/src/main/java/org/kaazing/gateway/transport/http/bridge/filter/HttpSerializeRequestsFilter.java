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
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.transport.http.HttpProtocol;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;

public class HttpSerializeRequestsFilter extends HttpFilterAdapter<IoSessionEx> {

    private final Queue<Object> messageQueue = new ConcurrentLinkedQueue<>();
    private final Logger logger;

    private final AtomicInteger requestsCompleted = new AtomicInteger();
    private final AtomicInteger responsesCompleted = new AtomicInteger();

    private boolean lastHttpResponseComplete;

    public HttpSerializeRequestsFilter() {
        this(getLogger(HttpSerializeRequestsFilter.class));
    }

    public HttpSerializeRequestsFilter(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        if (requestsCompleted.get() == responsesCompleted.get()) {
            super.messageReceived(nextFilter, session, message);
        } else {
            if (messageQueue.isEmpty()) {
                if (logger.isTraceEnabled()) {
                    logger.trace(format("[%s#%s] Suspending reads for HTTP pipelined request", HttpProtocol.NAME, session.getId()));
    }
                session.suspendRead();
            }
            messageQueue.add(message);
        }
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        super.filterWrite(nextFilter, session, writeRequest);

        while (!messageQueue.isEmpty() && requestsCompleted.get() == responsesCompleted.get()) {
            Object message = messageQueue.poll();
            if (message == null) {
                if (logger.isTraceEnabled()) {
                    logger.trace(format("[%s#%s] Resuming reads for HTTP pipelined request", HttpProtocol.NAME, session.getId()));
                }
                session.resumeRead();
                break;
            }
            super.messageReceived(nextFilter, session, message);
        }
    }

    @Override
    protected void httpRequestReceived(NextFilter nextFilter, IoSessionEx session, HttpRequestMessage httpRequest)
            throws Exception {

        if (httpRequest.isComplete()) {
            requestsCompleted.incrementAndGet();
        }

        super.httpRequestReceived(nextFilter, session, httpRequest);
        }

    @Override
    protected void httpContentReceived(NextFilter nextFilter, IoSessionEx session, HttpContentMessage httpContent)
            throws Exception {

        if (httpContent.isComplete()) {
            requestsCompleted.incrementAndGet();
    }

        super.httpContentReceived(nextFilter, session, httpContent);
    }

    @Override
    protected Object doFilterWriteHttpResponse(NextFilter nextFilter,
                                               IoSessionEx session,
                                               WriteRequest writeRequest,
                                               HttpResponseMessage httpResponse) throws Exception {

        lastHttpResponseComplete = httpResponse.isComplete();

        if (httpResponse.isComplete()) {
            responsesCompleted.incrementAndGet();
    }

        return super.doFilterWriteHttpResponse(nextFilter, session, writeRequest, httpResponse);
    }

    @Override
    protected Object doFilterWriteHttpContent(NextFilter nextFilter,
                                              IoSessionEx session,
                                              WriteRequest writeRequest,
                                              HttpContentMessage httpContent) throws Exception {

        if (httpContent.isComplete() && !lastHttpResponseComplete) {
            responsesCompleted.incrementAndGet();
                }

        return super.doFilterWriteHttpContent(nextFilter, session, writeRequest, httpContent);
            }
        }
