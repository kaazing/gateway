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

package org.kaazing.gateway.transport.http;

import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CONNECTION;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CONTENT_LENGTH;
import static org.kaazing.gateway.transport.http.HttpStatus.INFO_SWITCHING_PROTOCOLS;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.transport.AbstractBridgeSession;
import org.kaazing.gateway.transport.BridgeConnectHandler;
import org.kaazing.gateway.transport.BridgeConnectProcessor;
import org.kaazing.gateway.transport.IoSessionAdapterEx;
import org.kaazing.gateway.transport.UpgradeFuture;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.filter.HttpBuffer;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.buffer.SimpleBufferAllocator;
import org.kaazing.mina.core.session.IoSessionEx;

public class HttpConnectProcessor extends BridgeConnectProcessor<DefaultHttpSession> {

    public static final IoBufferEx WRITE_COMPLETE = SimpleBufferAllocator.BUFFER_ALLOCATOR.wrap(ByteBuffer.allocate(0));
    private static final String FILTER_PREFIX = HttpProtocol.NAME + "#";
    private final ThreadLocal<PersistentConnectionPool> persistentConnectionPool;

    HttpConnectProcessor(ThreadLocal<PersistentConnectionPool> persistentConnectionPool) {
        this.persistentConnectionPool = persistentConnectionPool;
    }

    protected void finishConnect(DefaultHttpSession session) {
        // GET methods implicitly have no payload
        // when payload is empty, proactively send the request start
        // as there will be no subsequent writes to trigger the send
        boolean isChunked = "chunked".equals(session.getWriteHeader("Transfer-Encoding"));
        if (session.getMethod() == HttpMethod.GET || session.getMethod() == HttpMethod.HEAD || "0".equals(session.getWriteHeader(HEADER_CONTENT_LENGTH)) || isChunked) {
            URI resource = session.getRemoteAddress().getResource();

            // create HttpRequestMessage
            HttpRequestMessage httpRequest = new HttpRequestMessage();
            httpRequest.setMethod(session.getMethod());
            httpRequest.setRequestURI(session.getRequestURI());
            httpRequest.setVersion(session.getVersion());
            Map<String, List<String>> parameters = session.getParameters();
            if (!parameters.isEmpty()) {
                httpRequest.setParameters(session.getParameters());
            }

            // default headers
            if (session.getWriteHeader(HttpHeaders.HEADER_USER_AGENT) == null) {
                httpRequest.setHeader("User-Agent", "Kaazing Gateway");
            }

            if (session.getWriteHeader(HttpHeaders.HEADER_HOST) == null) {
                // TODO: strip port if default
                httpRequest.setHeader(HttpHeaders.HEADER_HOST, resource.getAuthority());
            }

            // override headers
            httpRequest.putHeaders(session.getWriteHeaders());

            httpRequest.setCookies(session.getWriteCookies());

            if (isChunked) {
                IoBufferAllocatorEx<? extends HttpBuffer> allocator = session.getBufferAllocator();
                HttpBuffer unsharedEmpty = allocator.wrap(allocator.allocate(0));
                HttpContentMessage flush = new HttpContentMessage(unsharedEmpty, false);
                httpRequest.setContent(flush);
            }

            session.commit();

            IoSession parent = session.getParent();
            parent.write(httpRequest);
        }
    }

    @Override
    protected void flushInternal(final DefaultHttpSession session) {
        IoFilterChain filterChain = session.getFilterChain();
        WriteRequestQueue writeRequestQueue = session.getWriteRequestQueue();
        do {
            WriteRequest request = writeRequestQueue.poll(session);
            if (request == null) {
                break;
            }
            Object message = request.getMessage();
            if (message instanceof IoBufferEx) {
                IoBufferEx buf = (IoBufferEx) message;
                try {
                    // flush the buffer out to the parent
                    IoSessionEx parent = session.getParent();
                    if (parent.isClosing()) {
                        // TODO: throw write to close session exception
                        break;
                    }

                    // hold current remaining bytes so we know how much was written
                    int remaining = buf.remaining();

                    if (remaining == 0 && buf != WRITE_COMPLETE) {
                        throw new IllegalStateException("Unexpected empty buffer");
                    }
                    
                    // TODO: may want to fragment outbound buffer here in the future
                    if (session.isCommitting()) {
                        // create HttpContentMessage
                        HttpContentMessage content = new HttpContentMessage(buf, buf == WRITE_COMPLETE);
                        flushNowInternal(parent, content, buf, filterChain, request);
                    }
                    else {
                        // create HttpRequestMessage
                        HttpRequestMessage httpRequest = new HttpRequestMessage();
                        httpRequest.setMethod(session.getMethod());
                        httpRequest.setRequestURI(session.getRequestURI());
                        httpRequest.setVersion(session.getVersion());

                        // default headers
                        httpRequest.setHeader("User-Agent", "Kaazing Gateway");

                        // override headers
                        httpRequest.putHeaders(session.getWriteHeaders());
                        if (session.getWriteHeader(HttpHeaders.HEADER_HOST) == null) {
                            httpRequest.setHeader(HttpHeaders.HEADER_HOST, session.getRemoteAddress().getResource().getAuthority());
                        }

                        httpRequest.setCookies(session.getWriteCookies());

                        // wrap buffer and add it to response
                        HttpContentMessage content = new HttpContentMessage(buf, buf == WRITE_COMPLETE);
                        httpRequest.setContent(content);

                        session.commit();

                        flushNowInternal(parent, httpRequest, buf, filterChain, request);
                    }

                    // increment session written bytes
                    int written = remaining;
                    session.increaseWrittenBytes(written, System.currentTimeMillis());
                }
                catch (Exception e) {
                    request.getFuture().setException(e);
                }
            } else {
                throw new IllegalStateException("Don't know how to handle message of type '" + message.getClass().getName()
                        + "'.  Are you missing a protocol encoder?");
            }
        } while (true);
    }

    @Override
    protected void removeInternal(DefaultHttpSession httpSession) {
        // Make sure the closeFuture fires now since the listener on the CloseFuture
        // is where the decision to upgrade or not occurs.
        httpSession.getCloseFuture().setClosed();

        IoHandler upgradeHandler = httpSession.getUpgradeHandler();
        if (upgradeHandler == null) {
            removeInternal0(httpSession);
        } else {
            final UpgradeFuture upgradeFuture = httpSession.getUpgradeFuture();
            IoSessionEx parent = httpSession.getParent();
            try {
                if (parent instanceof AbstractBridgeSession<?, ?>) {
                    // this occurs for https
                    AbstractBridgeSession<?, ?> bridgeSession = (AbstractBridgeSession<?, ?>) parent;
                    IoHandler oldHandler = bridgeSession.getHandler();
                    oldHandler.sessionClosed(parent);
                    bridgeSession.setHandler(upgradeHandler);
                } else if (parent instanceof IoSessionAdapterEx) {
                    IoSessionAdapterEx bridgeSession = (IoSessionAdapterEx) parent;
                    IoHandler oldHandler = bridgeSession.getHandler();
                    oldHandler.sessionClosed(parent);
                    bridgeSession.setHandler(upgradeHandler);
                }
                else {
                    // this occurs for http
                    IoHandler oldHandler = (IoHandler) parent.getAttribute(BridgeConnectHandler.DELEGATE_KEY);
                    oldHandler.sessionClosed(parent);
                    parent.setAttribute(BridgeConnectHandler.DELEGATE_KEY, upgradeHandler);
                }
                upgradeFuture.setUpgraded();
                upgradeHandler.sessionOpened(parent);
            } catch (Exception e) {
                throw new RuntimeException("Error during http connector upgrade.", e);
            }
        }
    }

    private void removeInternal0(DefaultHttpSession session) {
        IoSession parent = session.getParent();
        if (parent == null || parent.isClosing()) {
            return;
        }

        Integer keepAliveTimeout = session.getRemoteAddress().getOption(HttpResourceAddress.KEEP_ALIVE_TIMEOUT);
        assert keepAliveTimeout != null;

        boolean http10 = session.getVersion() == HttpVersion.HTTP_1_0;
        boolean gatewayToClose = hasCloseHeader(session.getWriteHeaders(HEADER_CONNECTION));
        boolean serverToClose = hasCloseHeader(session.getReadHeaders(HEADER_CONNECTION));
        boolean upgrade = session.getStatus() == INFO_SWITCHING_PROTOCOLS;

        if (http10 || gatewayToClose || serverToClose || upgrade) {
            // will not recycle the transport session, but may need to close the transport
            if (gatewayToClose) {
                // close transport connection when write complete
                parent.close(false);
            } else if (serverToClose) {
                // Let server close transport session. Add idle filter to close the connection,
                // in case server doesn't close it.
                persistentConnectionPool.get().addIdleFilter(parent, keepAliveTimeout);
            }
        } else {
            if ("chunked".equals(session.getWriteHeader("Transfer-Encoding"))) {
                // write the empty chunk
                IoBufferAllocatorEx<? extends HttpBuffer> allocator = session.getBufferAllocator();
                HttpBuffer unsharedEmpty = allocator.wrap(allocator.allocate(0));
                HttpContentMessage completeMessage = new HttpContentMessage(unsharedEmpty, true, session.isChunked(), session.isGzipped());
                parent.write(completeMessage);
            }

            // Clean up the transport session before recycling

            // Remove all the http filters on the transport session
            IoFilterChain filterChain = parent.getFilterChain();
            List<IoFilterChain.Entry> filterList = filterChain.getAll();
            for(IoFilterChain.Entry e : filterList) {
                if (e.getName().startsWith(FILTER_PREFIX)) {        // http# prefixed filter
                    filterChain.remove(e.getName());
                }
            }

            // Remove any HTTP related attributes ??
            // TODO should we make sure that complete response is read before recycling ??

            // recycle the transport connection
            persistentConnectionPool.get().recycle(parent, keepAliveTimeout);
        }
    }

    /*
     * Returns whether there is Connection: close header
     *
     * @param list of Connection header values
     * @return true if Connection: close header is part of the values
     */
    private boolean hasCloseHeader(List<String> connectionValues) {
        if (connectionValues != null) {
            for (String value : connectionValues) {
                if (value.equalsIgnoreCase("close")) {
                    return true;
                }
            }
        }
        return false;
    }

}
