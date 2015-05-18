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

package org.kaazing.gateway.service.proxy;

import java.util.ArrayList;
import java.util.List;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.transport.http.HttpAcceptor;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyServiceHandler extends AbstractProxyAcceptHandler {
    private final Logger logger = LoggerFactory.getLogger(ProxyServiceHandler.class);
    private final List<ProxyServiceExtensionSpi> extensions;

    public ProxyServiceHandler() {
        super();
        extensions = new ArrayList<ProxyServiceExtensionSpi>();
    }

    // package private method for registering proxy service extensions so that
    // the proxy service interallly manages the extensions
    void registerExtension(ProxyServiceExtensionSpi extension) {
        assert extension != null;
        extensions.add(extension);
    }

    @Override
    public void sessionCreated(IoSession serverSession) {
        super.sessionCreated(serverSession);
    }

    @Override
    public void sessionClosed(IoSession serverSession) {
        super.sessionClosed(serverSession);
    }

    @Override
    public void sessionOpened(final IoSession acceptSession) {
        if (!acceptSession.isClosing()) {
            final Object serviceRegistration = acceptSession.getAttribute(HttpAcceptor.SERVICE_REGISTRATION_KEY);
            final String nextProtocol = BridgeSession.NEXT_PROTOCOL_KEY.get(acceptSession);

            // TODO: implement ServiceContext.connect(Collection<URI> connectURIs, connectHandler, sessionInitializer)
            // see commented ProxyConnectManager below for implementation hint.
            // Note: simpler to randomize order into a copy before initial connect, then consume until no connectURI
            // alternatives left
            ConnectFuture future = getNextConnectFuture(new IoSessionInitializer<ConnectFuture>() {
                @Override
                public void initializeSession(IoSession connectSession, ConnectFuture future) {
                    if (acceptSession.isClosing()) {
                        connectSession.close(true);
                    } else {
                        connectSession.setAttribute(HttpAcceptor.SERVICE_REGISTRATION_KEY, serviceRegistration);
                        BridgeSession.NEXT_PROTOCOL_KEY.set(connectSession, nextProtocol);

                        // guarantee strongly-typed buffers; this is the connect-side where
                        // the service is the client of the broker.
                        initFilterChain(connectSession, true);
                    }
                }
            });

            if (future == null) {
                acceptSession.close(false);
            } else {
                // flush queued messages and attach the accept and connected sessions together
                future.addListener(new ConnectListener(acceptSession));
            }

            super.sessionOpened(acceptSession);
        }
    }

    @Override
    protected AbstractProxyHandler createConnectHandler() {
        return new ConnectHandler();
    }

    private class ConnectListener implements IoFutureListener<ConnectFuture> {
        private final IoSession acceptSession;

        public ConnectListener(IoSession acceptSession) {
            this.acceptSession = acceptSession;
        }

        public void operationComplete(ConnectFuture future) {
            if (future.isConnected()) {
                IoSession connectedSession = future.getSession();

                if (logger.isTraceEnabled()) {
                    logger.trace("Connected to " + getConnectURIs().iterator().next() + " ["+acceptSession+"->"+connectedSession+"]");
                }
                if (acceptSession == null || acceptSession.isClosing()) {
                    connectedSession.close(true);
                } else {
                    AttachedSessionManager attachedSessionManager = attachSessions(acceptSession, connectedSession);
                    flushQueuedMessages(acceptSession, attachedSessionManager);

                    // notify extensions that the proxied connection is ready
                    IoSessionEx acceptSessionEx = (IoSessionEx)acceptSession;
                    IoSessionEx connectedSessionEx = (IoSessionEx)connectedSession;
                    for (ProxyServiceExtensionSpi extension : extensions) {
                        extension.proxiedConnectionEstablished(acceptSessionEx, connectedSessionEx);
                    }
                }
            } else {
                logger.warn("Connection to " + getConnectURIs().iterator().next() + " failed ["+acceptSession+"->]");
                acceptSession.close(true);
            }
        }
    }

//    private class ProxyConnectManager {
//        private final IoSession acceptSession;
//        private final Object serviceRegistration;
//        private final Object subject;
//        private final int numEntries;
//        private boolean multipleConnects;
//        private int numRemaining;
//        boolean[] triedEntryFlags;
//
//        public ProxyConnectManager(IoSession session, Object subject, Object serviceRegistration, int numEntries) {
//            this.acceptSession = session;
//            this.subject = subject;
//            this.serviceRegistration = serviceRegistration;
//            this.numEntries = numEntries;
//            this.numRemaining = numEntries;
//            this.multipleConnects = !(numEntries == 1);
//            triedEntryFlags = new boolean[numEntries];
//            if (multipleConnects) {
//                for (int i = 0; i < numEntries; i++) {
//                    triedEntryFlags[i] = false;
//                }
//            }
//        }
//
//        private URI getNextConnectURI() {
//            if (!multipleConnects) {
//                return connectURIs[0];
//            }
//            else {
//                while (true) {
//                    int next = Math.abs(Utils.randomInt()) % numEntries;
//                    if (!triedEntryFlags[next]) {
//                        triedEntryFlags[next] = true;
//                        numRemaining = numRemaining - 1;
//                        return connectURIs[next];
//                    }
//                    if (numRemaining > 0) {
//                        continue;
//                    }
//                    break;
//                }
//                return null;
//            }
//        }
//
//        private void connect() {
//            URI connectURI = getNextConnectURI();
//            if (connectURI == null) {
//                // no more fallback URLs to connect to close the session
//                logger.warn("Connection to proxy server unavailable");
//                acceptSession.close(false);
//                acceptSession.resumeRead();
//                acceptSession.resumeWrite();
//                return;
//            }
//
//            ConnectFuture future = service.connect(connectURI, connectHandler, new IoSessionInitializer<ConnectFuture>() {
//                @Override
//                public void initializeSession(IoSession clientSession, ConnectFuture future) {
//                    clientSession.setAttribute(HttpSubjectSecurityFilter.SUBJECT_KEY, subject);
//                    clientSession.setAttribute(HttpAcceptor.SERVICE_REGISTRATION_KEY, serviceRegistration);
//                }
//            });
//            future.addListener(new ConnectListener(acceptSession, connectURI));
//        }
//
//        private class ConnectListener implements IoFutureListener<ConnectFuture> {
//            private final IoSession acceptSession;
//            private final URI connectURI;
//
//            public ConnectListener(IoSession acceptSession, URI connectURI) {
//                this.acceptSession = acceptSession;
//                this.connectURI = connectURI;
//            }
//
//            public void operationComplete(ConnectFuture future) {
//                if (future.isConnected()) {
//                    logger.info("Incoming connection routed to : " + connectURI);
//                    IoSession connectSession = future.getSession();
//                    AbstractProxyHandler.attachSessions(acceptSession, connectSession);
//                    connectSession.resumeRead();
//                    connectSession.resumeWrite();
//
//                    acceptSession.resumeRead();
//                    acceptSession.resumeWrite();
//                }
//                else {
//                    logger.warn("Connection to " + connectURI + " failed trying the next one in the list");
//                    connect();
//                }
//
//            }
//        }
//
//    }

    private class ConnectHandler extends AbstractProxyHandler {
    }
}
