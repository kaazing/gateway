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

import java.util.List;
import java.util.regex.Pattern;

import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.transport.TypedAttributeKey;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.gateway.transport.http.bridge.HttpStartMessage;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HttpPersistenceFilter extends HttpFilterAdapter<IoSessionEx> {

    private static final String CONNECTION_CLOSE = "close";
    private static final String HEADER_CONNECTION = "Connection";
    private static final Pattern PATTERN_CONNECTION_CLOSE = Pattern.compile(".*\\s*[c|C][l|L][o|O][s|S][e|E]\\s*");

    private static final AttributeKey CONNECTION_CLOSE_KEY = new AttributeKey(HttpPersistenceFilter.class, "connectionClose");
    private static final TypedAttributeKey<Integer> SESSION_IDLE_TIMEOUT_KEY =
        new TypedAttributeKey<>(HttpPersistenceFilter.class, "sessionIdleTimeout");

    private static final Logger logger = LoggerFactory.getLogger(HttpPersistenceFilter.class);

    @Override
    public void sessionIdle(NextFilter nextFilter, IoSession session,
            IdleStatus status) throws Exception {

        // This should be null for any httpxe session so do not close but DO propagate sessionIdle.
        if (status == IdleStatus.BOTH_IDLE && SESSION_IDLE_TIMEOUT_KEY.get(session) != null) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Closing HTTP parent session %s because http.keepalive.timeout of %d secs is exceeded",
                                           session, SESSION_IDLE_TIMEOUT_KEY.get(session)));
            }
            session.close(false);
        }
        super.sessionIdle(nextFilter, session, status);
    }

    @Override
    protected void httpRequestReceived(NextFilter nextFilter,
            IoSessionEx session, HttpRequestMessage httpRequest) throws Exception {
        // GL.debug("http", getClass().getSimpleName()+" request received.");
        if (isClosing(httpRequest)) {
            session.setAttribute(CONNECTION_CLOSE_KEY);
        }

        Integer keepAliveTimeout = httpRequest.getLocalAddress().getOption(HttpResourceAddress.KEEP_ALIVE_TIMEOUT);
        if (keepAliveTimeout != null && keepAliveTimeout > 0) {
            SESSION_IDLE_TIMEOUT_KEY.set(session, keepAliveTimeout);
        }

        if (session.containsAttribute(SESSION_IDLE_TIMEOUT_KEY)) {
            deactivateSessionIdleTimeout(session);
        }

        super.httpRequestReceived(nextFilter, session, httpRequest);
    }

    @Override
    protected Object doFilterWriteHttpResponse(NextFilter nextFilter,
            IoSessionEx session, WriteRequest writeRequest, HttpResponseMessage httpResponse)
            throws Exception {

        boolean isClosing = isClosing(httpResponse);

        if (isClosing) {
            session.setAttribute(CONNECTION_CLOSE_KEY);
        }

        switch (httpResponse.getVersion()) {
        case HTTP_1_1:
            if (isClosing) {
                // ensure Connection: close is present when necessary
                httpResponse.setHeader(HEADER_CONNECTION, CONNECTION_CLOSE);
            }
            break;
        }

        if (httpResponse.isComplete()) {
            if (session.containsAttribute(CONNECTION_CLOSE_KEY)) {
                writeRequest.getFuture().addListener(new IoFutureListener<WriteFuture>() {
                    @Override
                    public void operationComplete(WriteFuture future) {
                        IoSession session = future.getSession();

                        // close on flush at server
                        session.close(false);
                    }

                });
            }
            else {
                switch (httpResponse.getStatus()) {
                case INFO_SWITCHING_PROTOCOLS:
                    // don't timeout for upgrade to wsn
                    break;
                default:
                    activateSessionIdleTimeout(session);
                    break;
                }
            }
        }

        return super.doFilterWriteHttpResponse(nextFilter, session, writeRequest, httpResponse);
    }

    @Override
    protected Object doFilterWriteHttpContent(NextFilter nextFilter,
            IoSessionEx session, WriteRequest writeRequest, HttpContentMessage httpContent) throws Exception {
        if (httpContent.isComplete()) {
            if (session.containsAttribute(CONNECTION_CLOSE_KEY)) {
                writeRequest.getFuture().addListener(new IoFutureListener<WriteFuture>() {
                    @Override
                    public void operationComplete(WriteFuture future) {
                        IoSession session = future.getSession();
                        if (logger.isTraceEnabled()) {
                            logger.trace(String.format("Closing session %s because of Connection: close header in HTTP request", session));
                        }

                        // close on flush at server
                        session.close(false);
                    }

                });
            }
            else {
                activateSessionIdleTimeout(session);
            }
        }

        return super.doFilterWriteHttpContent(nextFilter, session, writeRequest, httpContent);
    }

    static boolean isClosing(HttpStartMessage httpStart) {
        switch (httpStart.getVersion()) {
        case HTTP_1_0:
            return true;
        case HTTP_1_1:
            boolean isClosing = httpStart.isContentLengthImplicit();

            List<String> connectionValues = httpStart.getHeaderValues(HEADER_CONNECTION, false);
            if (connectionValues != null) {
                for (String connectionValue : connectionValues) {
                    if (PATTERN_CONNECTION_CLOSE.matcher(connectionValue).matches()) {
                        isClosing = true;
                        break;
                    }
                }
            }
            return isClosing;
        default:
            throw new IllegalArgumentException("Unexpected HTTP version: " + httpStart.getVersion());
        }
    }

    private static void activateSessionIdleTimeout(IoSession session) {
        Integer keepaliveTimeout = SESSION_IDLE_TIMEOUT_KEY.get(session);
        if (keepaliveTimeout != null) {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("Setting idle timeout %d on HTTP parent session %s ", keepaliveTimeout, session));
            }
            session.getConfig().setBothIdleTime(keepaliveTimeout);
        }
    }

    private static void deactivateSessionIdleTimeout(IoSession session) {
        // turn off idle timeout until response is complete
        session.getConfig().setBothIdleTime(0);
    }

    public static void cleanup(IoSession session) {
        SESSION_IDLE_TIMEOUT_KEY.remove(session);
        deactivateSessionIdleTimeout(session);
    }
}
