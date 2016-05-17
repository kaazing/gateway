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
package org.kaazing.gateway.transport.wseb;

import static java.lang.Integer.parseInt;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CONTENT_LENGTH;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CONTENT_TYPE;
import static org.kaazing.gateway.transport.wseb.WsebEncodingStrategy.TEXT_AS_BINARY;

import java.io.IOException;
import java.net.URI;
import java.util.EnumSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IdleStatus;
import org.kaazing.gateway.resource.address.Protocol;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpHeaders;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpUtils;
import org.kaazing.gateway.transport.ws.WsCommandMessage;
import org.kaazing.gateway.transport.ws.WsProtocol;
import org.kaazing.gateway.transport.wseb.filter.EncodingFilter;
import org.kaazing.gateway.transport.wseb.filter.WsebEncodingCodecFilter;
import org.kaazing.gateway.transport.wseb.filter.WsebEncodingCodecFilter.EscapeTypes;
import org.kaazing.gateway.transport.wseb.filter.WsebTextAsBinaryEncodingCodecFilter;
import org.kaazing.gateway.util.Encoding;
import org.kaazing.mina.netty.IoSessionIdleTracker;

@SuppressWarnings("deprecation")
class WsebDownstreamHandler extends IoHandlerAdapter<HttpAcceptSession> {

    private static final String CODEC_FILTER = WsebProtocol.NAME + "#codec";
    private static final String ENCODING_FILTER = WsebProtocol.NAME + "#escape";
    private static final EnumSet<HttpMethod> PERMITTED_REQUEST_METHODS = EnumSet.of(HttpMethod.GET, HttpMethod.POST);

    // TODO: make this setting available via configuration, with a reasonable default
    static final long TIME_TO_TIMEOUT_RECONNECT_MILLIS = TimeUnit.SECONDS.toMillis(60L);

    private final String contentType;
    private final WsebSession wsebSession;
    private final WsebEncodingCodecFilter codec;
    private final IoFilter encoding;
    private IoSessionIdleTracker inactivityTracker = null;
    private final BridgeServiceFactory bridgeServiceFactory;

    public WsebDownstreamHandler(ResourceAddress nextProtocolAddress,  WsebSession wsebSession, ScheduledExecutorService scheduler,
                                 WsebEncodingStrategy encodingStrategy, IoSessionIdleTracker inactivityTracker, BridgeServiceFactory bridgeServiceFactory) {
        this(nextProtocolAddress, wsebSession, scheduler, "application/octet-stream", encodingStrategy, inactivityTracker, bridgeServiceFactory);
    }

    public WsebDownstreamHandler(ResourceAddress nextProtocolAddress, WsebSession wsebSession, ScheduledExecutorService scheduler,
                                 String contentType, WsebEncodingStrategy encodingStrategy, IoSessionIdleTracker inactivityTracker, BridgeServiceFactory bridgeServiceFactory) {
        this(nextProtocolAddress, wsebSession, scheduler, contentType, null, encodingStrategy, inactivityTracker, bridgeServiceFactory);
    }

    public WsebDownstreamHandler(ResourceAddress nextProtocolAddress, WsebSession wsebSession, ScheduledExecutorService scheduler, String contentType,
                                 Encoding escapeEncoding, WsebEncodingStrategy encodingStrategy, IoSessionIdleTracker inactivityTracker, BridgeServiceFactory bridgeServiceFactory) {
        this.wsebSession = wsebSession;
        this.contentType = contentType;
        if (encodingStrategy == TEXT_AS_BINARY) {
            // 3.5 clients
            this.codec = new WsebTextAsBinaryEncodingCodecFilter();
            this.encoding = (escapeEncoding != null) ? new EncodingFilter(escapeEncoding) : null;
        }
        else {
            // 4.0 clients - escape is inside codecFilter
            this.codec = escapeEncoding != null ? new WsebEncodingCodecFilter(EscapeTypes.ESCAPE_ZERO_AND_NEWLINES) : new WsebEncodingCodecFilter();
            this.encoding = null;
        }
        this.inactivityTracker = inactivityTracker;
        this.bridgeServiceFactory = bridgeServiceFactory;
    }

    @Override
    protected void doExceptionCaught(HttpAcceptSession session, Throwable cause) throws Exception {
        wsebSession.setCloseException(cause);
        HttpStatus status = HttpStatus.SERVER_INTERNAL_ERROR;
        session.setStatus(status);
        session.setWriteHeader(HEADER_CONTENT_LENGTH, "0");
        session.close(true);
    }

    @Override
    protected void doSessionClosed(HttpAcceptSession session) throws Exception {
        if (wsebSession != null && (session.getStatus() != HttpStatus.SUCCESS_OK || wsebSession.getCloseException() != null)) {
            wsebSession.reset(new IOException("Network connectivity has been lost or transport was closed at other end",
                    wsebSession.getAndClearCloseException()).fillInStackTrace());
        }

        IoFilterChain filterChain = session.getFilterChain();
        removeBridgeFilters(filterChain);
    }

    @Override
    protected void doSessionOpened(HttpAcceptSession session) throws Exception {
        if (!PERMITTED_REQUEST_METHODS.contains(session.getMethod())) {
            wsebSession.setCloseException(
                    new IOException("Unsupported downstream request method: " + session.getMethod()));
            HttpStatus status = HttpStatus.CLIENT_BAD_REQUEST;
            session.setStatus(status);
            session.setWriteHeader(HEADER_CONTENT_LENGTH, "0");
            session.close(true);
        }

        IoFilterChain bridgeFilterChain = session.getFilterChain();
        addBridgeFilters(bridgeFilterChain, wsebSession); // pass in wseb session in case bridge filters need access to ws session data (e.g. extensions)

        // check if this session requests a short or a long keepalive timeout
        int clientIdleTimeout = wsebSession.getClientIdleTimeout();
        String requestedKeepAliveIntervalInSeconds = session.getParameter(".kkt");
        if (requestedKeepAliveIntervalInSeconds != null) {
            clientIdleTimeout = Integer.parseInt(requestedKeepAliveIntervalInSeconds);
        }

        // we don't need to send idletimeout keep-alive messages if we're already sending PINGs for inactivity timeout
        // at high enough frequency (these are sent every inactivity timeout / 2)
        int inactivityPingIntervalInSeconds = (int) (wsebSession.getInactivityTimeout() / 2000);
        if (inactivityPingIntervalInSeconds == 0 || inactivityPingIntervalInSeconds > clientIdleTimeout) {
            session.getConfig().setWriterIdleTime(clientIdleTimeout);
        }

        // most clients use GET for downstream (empty POST okay too)
        // defer attach writer to message received for non-empty POSTs (such as Flash client)
        String contentLength = session.getReadHeader(HttpHeaders.HEADER_CONTENT_LENGTH);
        if (contentLength == null || parseInt(contentLength) == session.getReadBytes()) {
            reconnectSession(session, wsebSession);
        }

        if (inactivityTracker != null) {
            // Activate inactivity timeout only when downstream has connected (timeout between create and downstream is handled separately)
            inactivityTracker.addSession(wsebSession);
        }
    }

    @Override
    protected void doMessageReceived(HttpAcceptSession session, Object message)
            throws Exception {
        String contentLength = session.getReadHeader(HttpHeaders.HEADER_CONTENT_LENGTH);
        if (contentLength != null && parseInt(contentLength) == session.getReadBytes()) {
            reconnectSession(session, wsebSession);
        }
    }

    @Override
    protected void doSessionIdle(HttpAcceptSession session, IdleStatus status) throws Exception {
        if (status == IdleStatus.WRITER_IDLE) {
            session.write(WsCommandMessage.NOOP);
        }
        super.doSessionIdle(session, status);
    }

    public void addBridgeFilters(IoFilterChain bridgeFilterChain, WsebSession wsebSession) {
        bridgeFilterChain.addLast(CODEC_FILTER, codec);

        if (encoding != null) {
            bridgeFilterChain.addBefore(CODEC_FILTER, ENCODING_FILTER, encoding);
        }
    }

    public void removeBridgeFilters(IoFilterChain filterChain) {
        removeFilter(filterChain, CODEC_FILTER);
        removeFilter(filterChain, ENCODING_FILTER);
    }

    private void reconnectSession(final HttpAcceptSession session, final WsebSession wsebSession) throws Exception {
        // KG-10590 (Nascar) For backwards compatibility with Flash client from HTML5 release 3.5.1.19 when running on IE 11,
        // we pretend downstream url parameter contains ".kp=2048&.kcc=private&.kf=200"
        String userAgent = session.getReadHeader("User-Agent");
        boolean isClientIE11 = false;
        if (userAgent != null && userAgent.contains("Trident/7.0")) {
        	isClientIE11 = true;
        }
        // check for polling strategy
        boolean longPoll = false;

        // check to see if this session can stream, otherwise force long polling
        if (!HttpUtils.canStream(session)) {
            // Note: Silverlight does not support redirecting from http to https
            //       so check .kd=s parameter (downstream=same) to prevent redirecting to https
            if (!"s".equals(session.getParameter(".kd"))) {
                // lookup secure acceptURI
                URI secureAcceptURI = locateSecureAcceptURI(session);
                if (secureAcceptURI != null) {
                    URI pathInfo = session.getPathInfo();
                    String secureAuthority = secureAcceptURI.getAuthority();
                    String secureAcceptPath = secureAcceptURI.getPath();
                    URI request = URI.create("https://" + secureAuthority + secureAcceptPath + pathInfo.toString());

                    // send redirect response
                    session.setStatus(HttpStatus.REDIRECT_MOVED_PERMANENTLY);
                    session.setWriteHeader("Location", request.toString());
                    session.close(false);

                    // we have sent the redirect and closed the session so end reconnect here
                    return;
                }
            }

            longPoll = true;
        }

        if (!longPoll) {
            // In long-polling, Content-Length would be sent. So, don't send Connection:close
            // disable pipelining, avoid the need for HTTP chunking
            session.setWriteHeader("Connection", "close");
        }

        // set the content type header
        String contentType = this.contentType;

        // look for content type override
        String contentTypeOverride = session.getParameter(".kc");
        if (contentTypeOverride != null) {
            if (contentTypeOverride.indexOf(';') == -1) {
                contentTypeOverride += ";charset=UTF-8";
            }
            contentType = contentTypeOverride;
        }

        session.setWriteHeader("X-Content-Type-Options", "nosniff");
        session.setWriteHeader(HEADER_CONTENT_TYPE, contentType);
        session.setWriteHeader("X-Idle-Timeout", String.valueOf(wsebSession.getClientIdleTimeout()));

        // look for mime detection padding override
        if(session.getParameter(".kns") != null) {
            // this prevents IE7 from interpreting WSEB as application/octet-stream
            // the exact content and minimum length needed are not yet known
            session.setWriteHeader("X-Content-Type-Nosniff", "abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234");
        }

        // configure cache control strategy
        String cacheControl = "no-cache";

        String cacheControlOverride = session.getParameter(".kcc");
        if (isClientIE11 && cacheControlOverride == null) {
        	cacheControlOverride = "private"; //KG-10590 add .kcc=private for IE11 client
        }
        if (cacheControlOverride != null) {
            cacheControl = cacheControlOverride;
        }

        session.setWriteHeader("Cache-Control", cacheControl);

        if (longPoll) {
            session.setAttribute(WsebAcceptor.CLIENT_BUFFER_KEY, 0L);
        }
        else {
            // check for client buffer setting
            String clientBuffer = session.getParameter(".kb");
            if (clientBuffer != null) {
                // Note: specifying .kb=X on the URL at the client
                //       overrides the default client buffer size
                long bufferSize = Long.parseLong(clientBuffer) * 1024L;
                session.setAttribute(WsebAcceptor.CLIENT_BUFFER_KEY, bufferSize);
            }
        }

        // check to see if we need to add a padding message to the end of the sent messages
        String clientPadding = session.getParameter(".kp");
        if (isClientIE11 && clientPadding == null) {
        	clientPadding = "2048"; //KG-10590 add .kp=2048 for IE11 client
        }
        if (clientPadding != null) {
            int paddingSize = Integer.parseInt(clientPadding);
            session.setAttribute(WsebAcceptor.CLIENT_PADDING_KEY, paddingSize);
            session.setAttribute(WsebAcceptor.BYTES_WRITTEN_ON_LAST_FLUSH_KEY, (long) 0);

            if (paddingSize == 0) {
                session.setWriteHeader("X-Content-Type-Options", "nosniff");
            }
        }

        // check to see if we need to add block padding to the end of the sent messages
        String clientBlockPadding = session.getParameter(".kbp");
        if (clientBlockPadding != null) {
            int paddingSize = Integer.parseInt(clientBlockPadding);
            session.setAttribute(WsebAcceptor.CLIENT_BLOCK_PADDING_KEY, paddingSize);
            session.setWriteHeader("Content-Encoding", "gzip");
        }

        // WseSession may have been closed asynchronously
        // and possibly also removed from the session map
        if (wsebSession == null || wsebSession.isClosing()) {
            if (!wsebSession.isCloseSent()) {
                session.write(WsCommandMessage.CLOSE);
                session.write(WsCommandMessage.RECONNECT);
            }
            session.close(false);
            return;
        }

        wsebSession.attachWriter(session);
    }

    private URI locateSecureAcceptURI(HttpAcceptSession session) throws Exception {
        // TODO: same-origin requests must consider cross-origin access control
        //       internal redirect to secure resource should not trigger 403 Forbidden
        ResourceAddress localAddress = session.getLocalAddress();
        URI resource = localAddress.getResource();
        Protocol resourceProtocol = bridgeServiceFactory.getTransportFactory().getProtocol(resource);
        if (WsebProtocol.WSEB_SSL == resourceProtocol || WsProtocol.WSS == resourceProtocol) {
            return resource;
        }
        return null;
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
