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
import java.util.HashMap;
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
            final DefaultHttpSession acceptSession = (DefaultHttpSession) session;
            //final Subject subject = ((IoSessionEx) acceptSession).getSubject();

            if (!validateRequestPath(acceptSession)) {
                acceptSession.setStatus(CLIENT_NOT_FOUND);
                acceptSession.close(false);
                return;
            }

            ConnectSessionInitializer sessionInitializer = new ConnectSessionInitializer(acceptSession);
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
     * Initializer for connect session. It adds the processed accept session headers
     * on the connect session
     */
    private static class ConnectSessionInitializer implements IoSessionInitializer<ConnectFuture> {
        private final DefaultHttpSession acceptSession;

        ConnectSessionInitializer(DefaultHttpSession acceptSession) {
            this.acceptSession = acceptSession;
        }

        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setVersion(acceptSession.getVersion());
            connectSession.setMethod(acceptSession.getMethod());
            URI connectURI = computeConnectPath(connectSession.getRequestURI());
            connectSession.setRequestURI(connectURI);
            processRequestHeaders(acceptSession, connectSession);
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
                DefaultHttpSession connectSession = (DefaultHttpSession)future.getSession();

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Connected to " + getConnectURIs().iterator().next() + " ["+acceptSession+"->"+connectSession+"]");
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
                LOGGER.warn("Connection to " + getConnectURIs().iterator().next() + " failed ["+acceptSession+"->]");
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
                if (acceptSession.getWrittenBytes() == 0L && !acceptSession.isCommitting() && !acceptSession.isClosing()) {
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
    private static void processRequestHeaders(HttpAcceptSession acceptSession, HttpConnectSession connectSession) {
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
        setupForwardedHeaders(acceptSession, connectSession);
        
    }

    private static void setupForwardedHeaders(HttpAcceptSession acceptSession, HttpConnectSession connectSession) {
        String forwarded = acceptSession.getReadHeader(HEADER_FORWARDED);
        Map<String, String> forwardedNameValue = parseForwardedInfo(forwarded);

        String remoteIpAddress = null;
        ResourceAddress resourceAddress = acceptSession.getRemoteAddress();
        ResourceAddress tcpResourceAddress = resourceAddress.findTransport("tcp");
        if (tcpResourceAddress != null) {
            URI resource = tcpResourceAddress.getResource();
            remoteIpAddress = resource.getHost();
        }
        if (remoteIpAddress != null) {
            setForwardedHeader(forwardedNameValue, "for", remoteIpAddress, HEADER_X_FORWARDED_FOR, acceptSession, connectSession);
        }

        ResourceAddress httpAddress = acceptSession.getLocalAddress();
        String host = httpAddress.getExternalURI().getHost();
        setForwardedHeader(forwardedNameValue, "host", host, HEADER_X_FORWARDED_HOST, acceptSession, connectSession);

        ResourceAddress serverTcpResourceAddress = httpAddress.findTransport("tcp");
        String serverIpAddress = null;
        if (serverTcpResourceAddress != null) {
            URI resource = serverTcpResourceAddress.getResource();
            serverIpAddress = resource.getHost();
        }
        if (serverIpAddress != null) {
            setForwardedHeader(forwardedNameValue, "server", serverIpAddress, HEADER_X_FORWARDED_SERVER, acceptSession, connectSession);
        }

        String protocol = httpAddress.getExternalURI().getScheme();
        setForwardedHeader(forwardedNameValue, "proto", protocol, HEADER_X_FORWARDED_PROTO, acceptSession, connectSession);

        String port = format("%d", httpAddress.getExternalURI().getPort());
        setForwardedHeader(forwardedNameValue, "port", port, HEADER_X_FORWARDED_PORT, acceptSession, connectSession);

        String computedForwarded = forwardedHeader(forwardedNameValue);
        connectSession.setWriteHeader(HEADER_FORWARDED, computedForwarded);
    }

    private static Map<String, String> parseForwardedInfo(String forwarded) {
        Map<String, String> forwardedHeaderInfo = new HashMap<String, String>();
        if (forwarded != null) {
            String[] forwardedPairLists = forwarded.split(";"); 
            for (String forwardedPairList : forwardedPairLists) {
                String[] forwardedPairs = forwardedPairList.split(",");
                String[] nameValue = forwardedPairs[0].split("=");
                String name = nameValue[0].trim();
                forwardedHeaderInfo.put(name, forwardedPairList.trim());
            }
        }
        return forwardedHeaderInfo;
    }

    private static void setForwardedHeader(Map<String, String> forwardedNameValue,
                                    String headerKey,
                                    String headerValue,
                                    String xHeaderName,
                                    HttpAcceptSession acceptSession,
                                    HttpConnectSession connectSession) {
        String forwarded = format("%s=%s", headerKey, headerValue);
        String forwardedReceived = forwardedNameValue.get(headerKey);
        String xForwarded = acceptSession.getReadHeader(xHeaderName);
        if (forwardedReceived != null) {
            forwarded = forwardedReceived + ", " + forwarded;
        } else {
            if (xForwarded != null) {
                String[] values = xForwarded.split(",");
                String computedXforwarded = format("%s=%s", headerKey, values[0]);
                for (int i = 1; i < values.length; i++) {
                    computedXforwarded = computedXforwarded + ", " + format("%s=%s", headerKey, values[i].trim());
                }
                forwarded = computedXforwarded + ", " + forwarded;
            }
        }
        forwardedNameValue.put(headerKey, forwarded);

        if (xForwarded != null) {
            xForwarded = xForwarded + ", " + headerValue;
        } else {
            xForwarded = headerValue; // add info from 'forwarded: for=' ?
        }
        connectSession.setWriteHeader(xHeaderName, xForwarded);
    }

    private static String forwardedHeader(Map<String, String> fwNV) {
        StringBuilder fw = new StringBuilder();
        for (Map.Entry<String, String> entry : fwNV.entrySet()) {
            fw.append(entry.getValue() + "; ");
        }

        return fw.substring(0, fw.length() - 2);
    }
    
    /*
     * Get all hop-by-hop headers from Connection header value.
     * Also add Connection header itself to the set
     */
    private static Set<String> getHopByHopHeaders(HttpSession session) {
        List<String> connectionHeaders = session.getReadHeaders(HEADER_CONNECTION);
        if (connectionHeaders == null) {
            connectionHeaders = Collections.emptyList();
        }
        Set<String> hopByHopHeaders = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for(String conHeader : connectionHeaders) {
            hopByHopHeaders.add(conHeader);
        }
        hopByHopHeaders.add(HEADER_CONNECTION);
        return hopByHopHeaders;
    }

    /*
     * An upgrade handler that connects transport sessions of http accept and connect
     * sessions.
     */
    private static class ProxyUpgradeHandler extends IoHandlerAdapter<IoSessionEx>  {
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
     * A close listener that upgrades underlying transport connection
     * at the end of http session close.
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
