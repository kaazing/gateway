/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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
package org.kaazing.gateway.service.http.proxy;

import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.service.proxy.AbstractProxyAcceptHandler;
import org.kaazing.gateway.service.proxy.AbstractProxyHandler;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.http.DefaultHttpSession;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpConnectSession;
import org.kaazing.gateway.transport.http.HttpSession;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static java.lang.String.format;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CONNECTION;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_FORWARDED;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_UPGRADE;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_VIA;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_X_FORWARDED_FOR;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_X_FORWARDED_HOST;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_X_FORWARDED_PORT;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_X_FORWARDED_PROTO;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_X_FORWARDED_SERVER;
import static org.kaazing.gateway.transport.http.HttpStatus.INFO_SWITCHING_PROTOCOLS;
import static org.kaazing.gateway.transport.http.HttpStatus.CLIENT_NOT_FOUND;

class HttpProxyServiceHandler extends AbstractProxyAcceptHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("http.proxy");

    private static final String VIA_HEADER_VALUE = "1.1 kaazing";
    private static final String FORWARDED_INJECT = "inject";
    private static final String FORWARDED_EXCLUDE = "exclude";
    private static final String FORWARDED_FOR = "for";
    private static final String FORWARDED_BY = "by";
    private static final String FORWARDED_PROTO = "proto";
    private static final String FORWARDED_HOST = "host";
    private static final String FORWARDED_PORT = "port";

    private URI connectURI;
    private String forwardedProperty;

    @Override
    protected AbstractProxyHandler createConnectHandler() {
        return new ConnectHandler();
    }

    public void initServiceConnectManager() {
        connectURI = getConnectURIs().iterator().next();
    }

    public void setUseForwardedHeaders(String forwardedProperty) {
        this.forwardedProperty = forwardedProperty;
    }

    @Override
    public void sessionOpened(IoSession session) {
        if (!session.isClosing()) {
            final DefaultHttpSession acceptSession = (DefaultHttpSession) session;
            // final Subject subject = ((IoSessionEx) acceptSession).getSubject();

            if (!validateRequestPath(acceptSession)) {
                acceptSession.setStatus(CLIENT_NOT_FOUND);
                acceptSession.close(false);
                return;
            }

            ConnectSessionInitializer sessionInitializer = new ConnectSessionInitializer(acceptSession,
                    forwardedProperty);
            ConnectFuture future = getServiceContext().connect(connectURI, getConnectHandler(), sessionInitializer);
            future.addListener(new ConnectListener(acceptSession));
            super.sessionOpened(acceptSession);
        }
    }

    private boolean validateRequestPath(DefaultHttpSession acceptSession) {
        URI requestURI = acceptSession.getRequestURI();
        String acceptPath = acceptSession.getServicePath().getPath();
        String requestPath = requestURI.normalize().getPath();

        return requestPath.startsWith(acceptPath);
    }

    /*
     * Initializer for connect session. It adds the processed accept session headers on the connect session
     */
    private static class ConnectSessionInitializer implements IoSessionInitializer<ConnectFuture> {
        private final DefaultHttpSession acceptSession;
        private final String forwardedProperty;

        ConnectSessionInitializer(DefaultHttpSession acceptSession, String forwardedProperty) {
            this.acceptSession = acceptSession;
            this.forwardedProperty = forwardedProperty;
        }

        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setVersion(acceptSession.getVersion());
            connectSession.setMethod(acceptSession.getMethod());
            URI connectURI = computeConnectPath(connectSession.getRequestURI());
            connectSession.setRequestURI(connectURI);
            processRequestHeaders(acceptSession, connectSession, forwardedProperty);
        }

        private URI computeConnectPath(URI connectURI) {
            String acceptPath = acceptSession.getServicePath().getPath();
            String requestUri = acceptSession.getRequestURI().toString();
            String connectPath = connectURI.getPath();
            return URI.create(connectPath + requestUri.substring(acceptPath.length()));
        }

    }

    private class ConnectListener implements IoFutureListener<ConnectFuture> {
        private final DefaultHttpSession acceptSession;

        ConnectListener(DefaultHttpSession acceptSession) {
            this.acceptSession = acceptSession;
        }

        @Override
        public void operationComplete(ConnectFuture future) {
            if (future.isConnected()) {
                DefaultHttpSession connectSession = (DefaultHttpSession) future.getSession();

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Connected to " + getConnectURIs().iterator().next() + " [" + acceptSession + "->"
                            + connectSession + "]");
                }
                if (acceptSession == null || acceptSession.isClosing()) {
                    connectSession.close(true);
                } else {
                    AttachedSessionManager attachedSessionManager = attachSessions(acceptSession, connectSession);
                    connectSession.getCloseFuture().addListener(new Upgrader(connectSession, acceptSession));
                    acceptSession.getCloseFuture().addListener(new Upgrader(acceptSession, connectSession));
                    flushQueuedMessages(acceptSession, attachedSessionManager);
                }
            } else {
                LOGGER.warn(
                        "Connection to " + getConnectURIs().iterator().next() + " failed [" + acceptSession + "->]");
                acceptSession.setStatus(HttpStatus.SERVER_GATEWAY_TIMEOUT);
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
                if (acceptSession.getWrittenBytes() == 0L && !acceptSession.isCommitting()
                        && !acceptSession.isClosing()) {
                    acceptSession.setStatus(connectSession.getStatus());
                    acceptSession.setReason(connectSession.getReason());
                    acceptSession.setVersion(connectSession.getVersion());

                    boolean upgrade = processHopByHopHeaders(connectSession, acceptSession);
                    // Add Connection: upgrade to acceptSession
                    if (upgrade) {
                        acceptSession.setWriteHeader(HEADER_CONNECTION, HEADER_UPGRADE);
                    }
                }
            }
        }
    }

    /*
     * Write all (except hop-by-hop) headers from source session to destination session.
     *
     * If the header is an upgrade one, let the Upgrade header go through as this service supports upgrade
     */
    private static boolean processHopByHopHeaders(HttpSession src, HttpSession dest) {
        Set<String> hopByHopHeaders = getHopByHopHeaders(src);
        boolean upgrade = src.getReadHeader(HEADER_UPGRADE) != null;
        if (upgrade) {
            hopByHopHeaders.remove(HEADER_UPGRADE);
        }

        // Add source session headers to destination session
        for (Map.Entry<String, List<String>> e : src.getReadHeaders().entrySet()) {
            String name = e.getKey();
            for (String value : e.getValue()) {
                if (!hopByHopHeaders.contains(name)) {
                    dest.addWriteHeader(name, value);
                }
            }
        }

        return upgrade;
    }

    /*
     * Write all (except hop-by-hop) request headers from accept session to connect session. If the request is an
     * upgrade one, let the Upgrade header go through as this service supports upgrade
     */
    private static void processRequestHeaders(HttpAcceptSession acceptSession,
                                              HttpConnectSession connectSession,
                                              String forwardedProperty) {
        boolean upgrade = processHopByHopHeaders(acceptSession, connectSession);

        // Add Connection: upgrade or Connection: close header
        if (upgrade) {
            connectSession.setWriteHeader(HEADER_CONNECTION, HEADER_UPGRADE);
        } else {
            ResourceAddress address = connectSession.getRemoteAddress();
            // If keep-alive is disabled, add Connection: close header
            if (!address.getOption(HttpResourceAddress.KEEP_ALIVE)) {
                connectSession.setWriteHeader(HEADER_CONNECTION, "close");
            }
        }

        // Add Via: 1.1 kaazing header
        connectSession.addWriteHeader(HEADER_VIA, VIA_HEADER_VALUE);

        // Add forwarded headers
        setupForwardedHeaders(acceptSession, connectSession, forwardedProperty);

    }

    /**
     * Compose the Forwarded header and the X-Forwarded headers and add them to the connect session write headers.
     * 
     * @param acceptSession
     * @param connectSession
     * @param forwardedProperty the value of the 'use-forwarded' property used for http.proxy type in gateway-config:
     * inject ( add the corresponding data to the Forwarded/X-Forwarded headers ), ignore ( this proxy is anonymous, no
     * forwarded header data is added ), or exclude ( delete any existing Forwarded/X-Forwarded headers received, and do
     * not add any new data )
     */
    private static void setupForwardedHeaders(HttpAcceptSession acceptSession,
                                              HttpConnectSession connectSession,
                                              String forwardedProperty) {
        if (FORWARDED_EXCLUDE.equalsIgnoreCase(forwardedProperty)) {
            excludeForwardedHeaders(connectSession);
            return;
        }
        StringBuilder forwardedSb = new StringBuilder();
        String forwarded = acceptSession.getReadHeader(HEADER_FORWARDED);
        String remoteIpAddress = getResourceIpAddress(acceptSession, FORWARDED_FOR);
        if (remoteIpAddress != null) {
            // 'Forwarded: for=' is added to the connectSession in HttpSubjecSecurityFilter if the 'Forwarded' header in
            // the request is empty, so we remove it for now, and inject it along with all the other parameters (by,
            // proto, host, port)
            String forAddress = format("%s=%s", FORWARDED_FOR, remoteIpAddress);
            if (forwarded.equalsIgnoreCase(forAddress)) {
                connectSession.clearWriteHeaders(HEADER_FORWARDED);
            }
            if (FORWARDED_INJECT.equalsIgnoreCase(forwardedProperty)) {
                connectSession.addWriteHeader(HEADER_X_FORWARDED_FOR, remoteIpAddress);
                forwardedSb.append(forAddress + ";");
            }
        }

        if (FORWARDED_INJECT.equalsIgnoreCase(forwardedProperty)) {

            String serverIpAddress = getResourceIpAddress(acceptSession, FORWARDED_BY);
            if (serverIpAddress != null) {
                connectSession.addWriteHeader(HEADER_X_FORWARDED_SERVER, serverIpAddress);
                forwardedSb.append(format("%s=%s;", FORWARDED_BY, serverIpAddress));
            }

            URI externalURI = acceptSession.getLocalAddress().getExternalURI();
            String protocol = externalURI.getScheme();
            connectSession.addWriteHeader(HEADER_X_FORWARDED_PROTO, protocol);
            forwardedSb.append(format("%s=%s;", FORWARDED_PROTO, protocol));

            String host = externalURI.getHost();
            connectSession.addWriteHeader(HEADER_X_FORWARDED_HOST, host);
            forwardedSb.append(format("%s=%s;", FORWARDED_HOST, host));

            String port = format("%d", externalURI.getPort());
            connectSession.addWriteHeader(HEADER_X_FORWARDED_PORT, port);
            forwardedSb.append(format("%s=%s", FORWARDED_PORT, port));

            connectSession.addWriteHeader(HEADER_FORWARDED, forwardedSb.toString());
        }
    }

    /**
     * Remove the Forwarded headers from the connect session if the 'use-forwarded' property for the http.proxy type is
     * set to 'exclude'.
     * 
     * @param connectSession
     */
    private static void excludeForwardedHeaders(HttpConnectSession connectSession) {
        connectSession.clearWriteHeaders(HEADER_FORWARDED);
        connectSession.clearWriteHeaders(HEADER_X_FORWARDED_FOR);
        connectSession.clearWriteHeaders(HEADER_X_FORWARDED_SERVER);
        connectSession.clearWriteHeaders(HEADER_X_FORWARDED_HOST);
        connectSession.clearWriteHeaders(HEADER_X_FORWARDED_PROTO);
        connectSession.clearWriteHeaders(HEADER_X_FORWARDED_PORT);
    }

    /**
     * Get the IP address of the resource based on the parameter name
     * 
     * @param acceptSession
     * @param parameterName can be either 'for' ( the IP address of the client/server making the request to this service
     * ), or 'by' ( the IP address of this proxy )
     * @return the IP address based on the parameter name received
     */
    private static String getResourceIpAddress(HttpAcceptSession acceptSession, String parameterName) {
        String resourceIpAddress = null;
        ResourceAddress resourceAddress = null;
        switch (parameterName) {
        case FORWARDED_FOR:
            resourceAddress = acceptSession.getRemoteAddress();
            break;
        case FORWARDED_BY:
            resourceAddress = acceptSession.getLocalAddress();
            break;
        }
        ResourceAddress tcpResourceAddress = resourceAddress.findTransport("tcp");
        if (tcpResourceAddress != null) {
            URI resource = tcpResourceAddress.getResource();
            resourceIpAddress = resource.getHost();
        }

        return resourceIpAddress;
    }

    /*
     * Get all hop-by-hop headers from Connection header value. Also add Connection header itself to the set
     */
    private static Set<String> getHopByHopHeaders(HttpSession session) {
        List<String> connectionHeaders = session.getReadHeaders(HEADER_CONNECTION);
        if (connectionHeaders == null) {
            connectionHeaders = Collections.emptyList();
        }
        Set<String> hopByHopHeaders = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String conHeader : connectionHeaders) {
            hopByHopHeaders.add(conHeader);
        }
        hopByHopHeaders.add(HEADER_CONNECTION);
        return hopByHopHeaders;
    }

    /*
     * An upgrade handler that connects transport sessions of http accept and connect sessions.
     */
    private static class ProxyUpgradeHandler extends IoHandlerAdapter<IoSessionEx> {
        final IoSession attachedSession;

        ProxyUpgradeHandler(IoSession attachedSession) {
            this.attachedSession = attachedSession;
        }

        @Override
        protected void doSessionOpened(final IoSessionEx session) throws Exception {
            session.resumeRead();
        }

        @Override
        protected void doMessageReceived(IoSessionEx session, Object message) throws Exception {
            attachedSession.write(message);
        }

        @Override
        protected void doExceptionCaught(IoSessionEx session, Throwable cause) throws Exception {
            attachedSession.close(false);
        }

        @Override
        protected void doSessionClosed(IoSessionEx session) throws Exception {
            attachedSession.close(false);
        }

    }

    /*
     * A close listener that upgrades underlying transport connection at the end of http session close.
     */
    private static class Upgrader implements IoFutureListener<CloseFuture> {
        private final DefaultHttpSession session;
        private final DefaultHttpSession attachedSession;

        Upgrader(DefaultHttpSession session, DefaultHttpSession attachedSession) {
            this.session = session;
            this.attachedSession = attachedSession;
        }

        @Override
        public void operationComplete(CloseFuture future) {
            if (session.getStatus() == INFO_SWITCHING_PROTOCOLS) {
                ProxyUpgradeHandler handler = new ProxyUpgradeHandler(attachedSession.getParent());
                session.suspendRead();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("http.proxy service is upgrading session %s", session));
                }
                session.upgrade(handler);
            }
        }
    }

}
