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

package org.kaazing.gateway.transport.wseb;

import static java.lang.String.format;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CONTENT_LENGTH;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IdleStatus;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.ws.Command;
import org.kaazing.gateway.transport.ws.WsCloseMessage;
import org.kaazing.gateway.transport.ws.WsCommandMessage;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.wseb.filter.EncodingFilter;
import org.kaazing.gateway.transport.wseb.filter.WsebDecodingCodecFilter;
import org.kaazing.gateway.util.Encoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WsebUpstreamHandler extends IoHandlerAdapter<HttpAcceptSession> {
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_TEXT_PLAIN_CHARSET_UTF_8 = "text/plain; charset=utf-8";

    private static final String CODEC_FILTER = WsebProtocol.NAME + "#codec";
    private static final String UTF8_FILTER = WsebProtocol.NAME + "#utf8";

    private static final String LOGGER_NAME = String.format("transport.%s.accept", WsebProtocol.NAME);
    private final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    private final WsebSession wsebSession;
    private final IoFilter codec;
    private final IoFilter utf8;

    public WsebUpstreamHandler(ResourceAddress nextProtocolAddress,  WsebSession wsebSession, int wsMaxMessageSize) {
        this(nextProtocolAddress, wsebSession, null, wsMaxMessageSize);
    }

    public WsebUpstreamHandler(ResourceAddress nextProtocolAddress, WsebSession wsebSession, Encoding utf8Encoding, 
                               int wsMaxMessageSize) {
        this.wsebSession = wsebSession;
        this.codec = new WsebDecodingCodecFilter(wsMaxMessageSize);
        this.utf8 = (utf8Encoding != null) ? new EncodingFilter(utf8Encoding) : null;
    }

    @Override
    protected void doSessionCreated(HttpAcceptSession session) throws Exception {
        // session create/open will be called in doSessionOpen
    }

    @Override
    protected void doSessionOpened(final HttpAcceptSession session) throws Exception {
        WsebSession wsebSession = getSession(session);
        if (wsebSession == null || wsebSession.isClosing()) {
            session.close(false);
            return;
        }

        IoFilterChain filterChain = session.getFilterChain();

        filterChain.addLast(CODEC_FILTER, codec);

        // only supported for non-binary upstream
        if (utf8 != null) {
            // Note: encoding filter needs to be closer to the network than the codec filter
            String contentType = session.getReadHeader(HEADER_CONTENT_TYPE);
            if (CONTENT_TYPE_TEXT_PLAIN_CHARSET_UTF_8.equalsIgnoreCase(contentType)) {
                filterChain.addBefore(CODEC_FILTER, UTF8_FILTER, utf8);
            }
        }

        final CloseFuture wsebCloseFuture = wsebSession.getCloseFuture();
        final IoFutureListener<CloseFuture> listener = new IoFutureListener<CloseFuture>() {
            @Override
            public void operationComplete(CloseFuture future) {
                // Note: this reference to HTTP session is pinned by listener
                //       and must be removed to avoid a memory leak (see below)
                session.close(false);
            }
        };
        // detect when emulated session is closed to force upstream to close
        wsebCloseFuture.addListener(listener);
        // detect when upstream is closed to remove upstream reference from emulated session
        session.getCloseFuture().addListener(new IoFutureListener<CloseFuture>() {
            @Override
            public void operationComplete(CloseFuture future) {
                // Note: a reference to the HTTP upstream session is pinned by listener
                //       and must be removed to avoid a memory leak (see above)
                wsebCloseFuture.removeListener(listener);
            }
        });

        wsebSession.attachReader(session);
    }

    @Override
    protected void doMessageReceived(HttpAcceptSession session, Object message) throws Exception {
        // this can happen if there is an error
        if (!(message instanceof WsMessage)) {
            return;
        }

        WsebSession wsebSession = getSession(session);
        WsMessage wsebMessage = (WsMessage)message;
        IoFilterChain filterChain = wsebSession.getTransportSession().getFilterChain();
        
        switch (wsebMessage.getKind()) {
        case COMMAND:
            for (Command command : ((WsCommandMessage)wsebMessage).getCommands()) {
                if (command == Command.close()) {
                    session.setWriteHeader(HEADER_CONTENT_LENGTH, "0");
                    session.close(false);
                    filterChain.fireMessageReceived(new WsCloseMessage());
                    break;
                }
                else if (command == Command.reconnect()) {
                    session.setWriteHeader(HEADER_CONTENT_LENGTH, "0");
                    session.close(false);
                    break;
                }
                // no-op (0x00) - continue reading commands
            }
            break;
        default:
            filterChain.fireMessageReceived(wsebMessage);
            break;
        }
    }

    @Override
    protected void doExceptionCaught(HttpAcceptSession session, Throwable cause) throws Exception {
        if (logger.isDebugEnabled()) {
            String message = format("Exception while handling HTTP upstream for WsebSession: %s", getSession(session));
            if (logger.isTraceEnabled()) {
                // note: still debug level, but with extra detail about the exception
                logger.debug(message, cause);
            } else {
                logger.debug(message);
            }
        }
        session.setStatus(HttpStatus.SERVER_INTERNAL_ERROR);
        session.close(true);
    }

    @Override
    protected void doSessionClosed(HttpAcceptSession session) throws Exception {
        // session is long lived so we do not want to close it when the http session is closed

        WsebSession wsebSession = getSession(session);
        if (wsebSession != null && session.getStatus() != HttpStatus.SUCCESS_OK) {
            wsebSession.reset(new Exception("Network connectivity has been lost or transport was closed at other end").fillInStackTrace());
        }
    }

    @Override
    protected void doSessionIdle(HttpAcceptSession session, IdleStatus status) throws Exception {
        // do not percolate idle
    }

    private WsebSession getSession(HttpAcceptSession session) throws Exception {
        boolean traceEnabled = logger.isTraceEnabled();

        if (traceEnabled) {
            logger.trace("Remote address resource = '"+session.getRemoteAddress().getResource()+"'");
        }
        return wsebSession;
    }

    protected final void removeFilter(IoFilterChain filterChain, String name) {
        if (filterChain.contains(name)) {
            filterChain.remove(name);
        }
    }

    protected final void removeFilter(IoFilterChain filterChain, IoFilter filter) {
        if (filterChain.contains(filter)) {
            filterChain.remove(filter);
        }
    }
}
