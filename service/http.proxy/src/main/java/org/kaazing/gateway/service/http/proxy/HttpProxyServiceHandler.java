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

package org.kaazing.gateway.service.http.proxy;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.gateway.service.proxy.AbstractProxyAcceptHandler;
import org.kaazing.gateway.service.proxy.AbstractProxyHandler;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpConnectSession;
import org.kaazing.gateway.transport.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

class HttpProxyServiceHandler extends AbstractProxyAcceptHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("http.proxy");
    
    private static final String VIA_HEADER_VALUE = "1.1 kaazing";

    private URI connectURI;

    @Override
    protected AbstractProxyHandler createConnectHandler() {
        return new ConnectHandler();
    }

    public void initServiceConnectManager() {
        connectURI = getConnectURIs().iterator().next();
    }

    @Override
    public void sessionOpened(IoSession session) {
        if (!session.isClosing()) {
            final HttpAcceptSession acceptSession = (HttpAcceptSession) session;
            //final Subject subject = ((IoSessionEx) acceptSession).getSubject();

            ConnectFuture future = getServiceContext().connect(connectURI, getConnectHandler(), new IoSessionInitializer<ConnectFuture>() {
                @Override
                public void initializeSession(IoSession session, ConnectFuture future) {
                    HttpConnectSession connectSession = (HttpConnectSession) session;
                    connectSession.setVersion(acceptSession.getVersion());
                    connectSession.setMethod(acceptSession.getMethod());
                    connectSession.setRequestURI(acceptSession.getRequestURI());
                    processRequestHeaders(acceptSession, connectSession);
                }
            });
            future.addListener(new ConnectListener(acceptSession));

            super.sessionOpened(acceptSession);
        }
    }

    private class ConnectListener implements IoFutureListener<ConnectFuture> {
        private final HttpAcceptSession acceptSession;

        ConnectListener(HttpAcceptSession acceptSession) {
            this.acceptSession = acceptSession;
        }

        @Override
        public void operationComplete(ConnectFuture future) {
            if (future.isConnected()) {
                IoSession connectedSession = future.getSession();

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Connected to " + getConnectURIs().iterator().next() + " ["+acceptSession+"->"+connectedSession+"]");
                }
                if (acceptSession == null || acceptSession.isClosing()) {
                    connectedSession.close(true);
                } else {
                    AttachedSessionManager attachedSessionManager = attachSessions(acceptSession, connectedSession);
                    flushQueuedMessages(acceptSession, attachedSessionManager);
                }
            } else {
                LOGGER.warn("Connection to " + getConnectURIs().iterator().next() + " failed ["+acceptSession+"->]");
                acceptSession.close(true);
            }
        }

    }

    private static class ConnectHandler extends AbstractProxyHandler {

        @Override
        public void messageReceived(IoSession session, Object message) {
            processResponseHeaders(session);
            super.messageReceived(session, message);
        }

        @Override
        public void sessionClosed(IoSession session) {
            processResponseHeaders(session);
            super.sessionClosed(session);
        }

        private void processResponseHeaders(IoSession session) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            AttachedSessionManager attachedSessionManager = getAttachedSessionManager(session);
            if (attachedSessionManager != null) {
                HttpAcceptSession acceptSession = (HttpAcceptSession) attachedSessionManager.getAttachedSession();
                if (acceptSession.getWrittenBytes() == 0L) {
                    if (!(acceptSession.isCommitting() || acceptSession.isClosing())) {
                        acceptSession.setStatus(connectSession.getStatus());
                        acceptSession.setReason(connectSession.getReason());
                        acceptSession.setVersion(connectSession.getVersion());
                        Map<String, List<String>> headers = connectSession.getReadHeaders();
                        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                            String name = e.getKey();
                            for (String value : e.getValue()) {
                                // client<-->gateway may use keep-alive, so don't send gateway<-->origin's connection hdr
                                // Note that origin server may send Connection: Upgrade
                                if (name.equalsIgnoreCase("Connection") && value.equals("close")) {
                                    continue;
                                }
                                acceptSession.addWriteHeader(name, value);
                            }
                        }
                    }
                }
            }
        }
    }
    
    
    // Write all request headers from accept session to connect session
    private void processRequestHeaders(HttpAcceptSession acceptSession, HttpConnectSession connectSession) {
        Set<String> hopByHopHeaders = getHopByHopHeaders(acceptSession);
        boolean upgrade = false;
        String upgradeHeader = acceptSession.getReadHeader(HttpHeaders.HEADER_UPGRADE);
        if ("WebSocket".equalsIgnoreCase(upgradeHeader)) {
            upgrade = true;
            hopByHopHeaders.remove(HttpHeaders.HEADER_UPGRADE);
        }

        for(Map.Entry<String, List<String>> e : acceptSession.getReadHeaders().entrySet()) {
            String name = e.getKey();
            for(String value : e.getValue()) {
                if (!hopByHopHeaders.contains(name)) {
                    connectSession.addWriteHeader(name, value);
                }
            }
        }
        if (upgrade) {
            connectSession.setWriteHeader(HttpHeaders.HEADER_CONNECTION, HttpHeaders.HEADER_UPGRADE);
        } else {
            connectSession.setWriteHeader(HttpHeaders.HEADER_CONNECTION, "close");
        }
        connectSession.addWriteHeader(HttpHeaders.HEADER_VIA, VIA_HEADER_VALUE);
    }
    
    // Get all hop-by-hop headers from Connection header. Also add Connection header itself to the set
    private Set<String> getHopByHopHeaders(HttpAcceptSession acceptSession) {
        List<String> connectionHeaders = acceptSession.getReadHeaders(HttpHeaders.HEADER_CONNECTION);
        if (connectionHeaders == null) {
            connectionHeaders = Collections.emptyList();
        }
        Set<String> hopByHopHeaders = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for(String conHeader : connectionHeaders) {
            hopByHopHeaders.add(conHeader);
        }
        hopByHopHeaders.add(HttpHeaders.HEADER_CONNECTION);
        return hopByHopHeaders;
    }
    
}
