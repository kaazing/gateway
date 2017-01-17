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
package org.kaazing.gateway.service.http.proxy;

import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CONNECTION;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_LOCATION;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_SET_COOKIE;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_UPGRADE;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_VIA;
import static org.kaazing.gateway.transport.http.HttpStatus.CLIENT_NOT_FOUND;
import static org.kaazing.gateway.transport.http.HttpStatus.INFO_SWITCHING_PROTOCOLS;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_FORWARDED;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_X_FORWARDED_FOR;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_X_FORWARDED_HOST;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_X_FORWARDED_PROTO;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_X_FORWARDED_SERVER;
import static java.lang.String.format;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.resource.address.uri.URIUtils;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.ServiceProperties;
import org.kaazing.gateway.service.proxy.AbstractProxyAcceptHandler;
import org.kaazing.gateway.service.proxy.AbstractProxyHandler;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.http.DefaultHttpSession;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpConnectSession;
import org.kaazing.gateway.transport.http.HttpSession;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HttpProxyServiceHandler extends AbstractProxyAcceptHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("service.http.proxy");
    private static final String VIA_HEADER_FORMATTER = "1.1 kaazing-%s";
    private static final String FORWARDED_INJECT = "inject";
    private static final String FORWARDED_EXCLUDE = "exclude";
    private static final String FORWARDED_IGNORE = "ignore";
    private static final String FORWARDED_FOR = "for";
    private static final String FORWARDED_BY = "by";
    private static final String FORWARDED_PROTO = "proto";
    private static final String FORWARDED_HOST = "host";

    private static final Set KNOWN_SIMPLE_PROPERTIES;
    static {
        Set<String> set = new HashSet<>();
        set.add("rewrite-cookie-domain");
        set.add("rewrite-cookie-path");
        set.add("rewrite-location");
        set.add("use-forwarded");
        KNOWN_SIMPLE_PROPERTIES = Collections.unmodifiableSet(set);
    }
    private static final Set KNOWN_NESTED_PROPERTIES;
    static {
        Set<String> set = new HashSet<>();
        set.add("cookie-domain-mapping");
        set.add("cookie-path-mapping");
        set.add("location-mapping");
        KNOWN_NESTED_PROPERTIES = Collections.unmodifiableSet(set);
    }

    private final String viaHeader;
    private static final Set<String> USE_FORWARDED_VALUES;
    static {
        Set<String> set = new HashSet<>();
        set.add(FORWARDED_INJECT);
        set.add(FORWARDED_EXCLUDE);
        set.add(FORWARDED_IGNORE);
        USE_FORWARDED_VALUES = Collections.unmodifiableSet(set);
    }

    private String connectURI;
    private String useForwarded;
    private String serviceName;
    private int remoteClientPort;
    private boolean rewriteCookieDomain;
    private boolean rewriteCookiePath;
    private boolean rewriteLocation;
    private Map<String, String> cookieDomainMap;
    private Map<String, String> cookiePathMap;
    private Map<String, String> locationMap;

    public HttpProxyServiceHandler() {
        viaHeader = String.format(VIA_HEADER_FORMATTER, UUID.randomUUID());
    }

    void init() {
        ServiceContext serviceContext = getServiceContext();
        serviceName = serviceContext.getServiceName();

        Collection<String> acceptURIs = serviceContext.getAccepts();
        Collection<String> connectURIs = serviceContext.getConnects();

        String acceptURI = acceptURIs.iterator().next();
        connectURI = connectURIs.iterator().next();

        validateProperties(serviceContext);

        ServiceProperties properties = serviceContext.getProperties();

        rewriteCookieDomain = "enabled".equals(properties.get("rewrite-cookie-domain"));
        rewriteCookiePath = "enabled".equals(properties.get("rewrite-cookie-path"));
        rewriteLocation = !"disabled".equals(properties.get("rewrite-location"));

        cookieDomainMap = new HashMap<>();
        if (rewriteCookieDomain) {
            List<ServiceProperties> cookieDomainProperties = properties.getNested("cookie-domain-mapping");
            for (ServiceProperties sp : cookieDomainProperties) {
                cookieDomainMap.put(sp.get("from"), sp.get("to"));
            }
        }

        cookiePathMap = new HashMap<>();
        if (rewriteCookiePath) {
            List<ServiceProperties> cookiePathProperties = properties.getNested("cookie-path-mapping");
            for (ServiceProperties sp : cookiePathProperties) {
                cookiePathMap.put(sp.get("from"), sp.get("to"));
            }
        }

        locationMap = new HashMap<>();
        if (rewriteLocation) {
            List<ServiceProperties> locationProperties = properties.getNested("location-mapping");
            for (ServiceProperties sp : locationProperties) {
                locationMap.put(sp.get("from"), sp.get("to"));
            }
            locationMap.put(connectURI, acceptURI);
        }

        useForwarded = properties.get("use-forwarded");
        if (useForwarded == null) {
            useForwarded = FORWARDED_IGNORE;
        }
        if (!USE_FORWARDED_VALUES.contains(useForwarded)) {
            throw new IllegalArgumentException(serviceContext.getServiceName()
                    + " http.proxy service specifies unknown property value : " + useForwarded + " for use-forwarded");
        }
    }

    private void validateProperties(ServiceContext serviceContext) {
        ServiceProperties properties = serviceContext.getProperties();

        // validate all properties: rewrite-cookie-domain, rewrite-cookie-path, rewrite-location
        Iterable<String> simpleProperties = properties.simplePropertyNames();
        Set<String> unknownProperties = StreamSupport.stream(simpleProperties.spliterator(), false)
                .filter(p -> !KNOWN_SIMPLE_PROPERTIES.contains(p))
                .collect(Collectors.toSet());
        Iterable<String> nestedProperties = properties.nestedPropertyNames();
        StreamSupport.stream(nestedProperties.spliterator(), false)
                .filter(p -> !KNOWN_NESTED_PROPERTIES.contains(p))
                .forEach(unknownProperties::add);
        if (!unknownProperties.isEmpty()) {
            throw new IllegalArgumentException(serviceContext.getServiceName() +
                    " http.proxy service specifies unknown properties : " + unknownProperties);
        }
    }

    

    @Override
    protected AbstractProxyHandler createConnectHandler() {
        return new ConnectHandler();
    }

    @Override
    public void sessionOpened(IoSession session) {
    	// get the port number of the remote client
    	BridgeSession bridgeSession = (BridgeSession) session;
    	remoteClientPort = BridgeSession.REMOTE_ADDRESS.get(bridgeSession).getTransport().getResource().getPort();
        if (!session.isClosing()) {
            final DefaultHttpSession acceptSession = (DefaultHttpSession) session;
            // final Subject subject = ((IoSessionEx) acceptSession).getSubject();

            // log warning first time we see Http 1.0 request
            if (acceptSession.getVersion().toString().equals("HTTP/1.0")) {
                if (this.serviceName != null) {
                    LOGGER.warn(String.format(
                            "http.proxy service %s received an HTTP 1.0 request. HTTP 1.0 is not explicitly supported.",
                            this.serviceName));
                } else {
                    LOGGER.warn("http.proxy service received an HTTP 1.0 request. HTTP 1.0 is not explicitly supported.");
                }
            }

            if (!validateRequestPath(acceptSession)) {
                acceptSession.setStatus(CLIENT_NOT_FOUND);
                acceptSession.close(false);
                return;
            }
            if (!validateNoLoopDetected(acceptSession)) {
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


    /**
     * Helper method performing loop detection
     * @param acceptSession - session parameter
     * @return - whether a loop was detected or not
     */
    private boolean validateNoLoopDetected(DefaultHttpSession acceptSession) {
        List<String> viaHeaders = acceptSession.getReadHeaders(HEADER_VIA);
        if (viaHeaders != null && viaHeaders.stream().anyMatch(h -> h.equals(viaHeader))) {
                LOGGER.warn("Connection to " + getConnectURIs().iterator().next() +
                        " failed due to loop detection [" + acceptSession + "->]");
                acceptSession.setStatus(HttpStatus.SERVER_LOOP_DETECTED);
                acceptSession.close(true);
                return false;
            }
        return true;
    }

    /*
     * Initializer for connect session. It adds the processed accept session headers on the connect session
     */
    private class ConnectSessionInitializer implements IoSessionInitializer<ConnectFuture> {
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
            String connectURI = getConnectURIs().iterator().next();
            if (future.isConnected()) {
                DefaultHttpSession connectSession = (DefaultHttpSession) future.getSession();

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Connected to " + connectURI + " [" + acceptSession + "->" + connectSession + "]");
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
                LOGGER.warn("Connection to " + connectURI + " failed [" + acceptSession + "->]");
                acceptSession.setStatus(HttpStatus.SERVER_GATEWAY_TIMEOUT);
                acceptSession.close(true);
            }
        }

    }

    private class ConnectHandler extends AbstractProxyHandler {

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

                    processResponseHeaders(connectSession, acceptSession);
                }

            }
        }

        private void processResponseHeaders(HttpSession connectSession, HttpSession acceptSession) {

            Set<String> hopByHopHeaders = getHopByHopHeaders(connectSession);
            boolean upgrade = connectSession.getReadHeader(HEADER_UPGRADE) != null;
            if (upgrade) {
                hopByHopHeaders.remove(HEADER_UPGRADE);
            }

            // Add processed connect session headers to accept session
            for (Map.Entry<String, List<String>> e : connectSession.getReadHeaders().entrySet()) {
                String name = e.getKey();
                // don't add hop-by-hop response headers
                if (hopByHopHeaders.contains(name)) {
                    continue;
                }
                for (String value : e.getValue()) {
                    if (name.equalsIgnoreCase(HEADER_SET_COOKIE)) {
                        if (rewriteCookieDomain) {
                            value = processCookieDomain(value, cookieDomainMap);
                        }
                        if (rewriteCookiePath) {
                            value = processCookiePath(value, cookiePathMap);
                        }
                        acceptSession.addWriteHeader(name, value);
                    } else if (name.equalsIgnoreCase(HEADER_LOCATION)) {
                        if (rewriteLocation) {
                            value = processLocationHeader(value, locationMap);
                        }
                        acceptSession.addWriteHeader(name, value);
                    } else {
                        acceptSession.addWriteHeader(name ,value);
                    }
                }
            }

            // Add Connection: upgrade to acceptSession
            if (upgrade) {
                acceptSession.setWriteHeader(HEADER_CONNECTION, HEADER_UPGRADE);
            }

        }

        private String processCookieDomain(String cookie, Map<String, String> cookieDomainMap) {
            String lowerCookie = cookie.toLowerCase();
            if (lowerCookie.contains("domain=")) {
                return cookieDomainMap.entrySet().stream()
                        .filter(e -> lowerCookie.contains("domain="+e.getKey()))
                        .findFirst()
                        .map(e -> {
                            int index = lowerCookie.indexOf("domain="+e.getKey());
                            return cookie.substring(0, index+7)+e.getValue()+cookie.substring(index+7+e.getKey().length());
                        })
                        .orElse(cookie);
            }
            return cookie;
        }

        private String processCookiePath(String cookie, Map<String, String> cookiePathMap) {
            String lowerCookie = cookie.toLowerCase();
            if (lowerCookie.contains("path=")) {
                return cookiePathMap.entrySet().stream()
                        .filter(e -> lowerCookie.contains("path="+e.getKey()))
                        .findFirst()
                        .map(e -> {
                            int index = lowerCookie.indexOf("path="+e.getKey());
                            return cookie.substring(0, index+5)+e.getValue()+cookie.substring(index+5+e.getKey().length());
                        })
                        .orElse(cookie);
            }
            return cookie;
        }

        private String processLocationHeader(String location, Map<String, String> locationMap) {
            return locationMap.entrySet().stream()
                    .filter(e -> location.startsWith(e.getKey()))
                    .findFirst()
                    .map(e -> location.replaceFirst(Pattern.quote(e.getKey()), e.getValue()))
                    .orElse(location);
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
    private void processRequestHeaders(HttpAcceptSession acceptSession, HttpConnectSession connectSession) {
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

        // Add Via: 1.1 kaazing + uuid header
        connectSession.addWriteHeader(HEADER_VIA, viaHeader);

        // Add forwarded headers
        setupForwardedHeaders(acceptSession, connectSession);

    }

    /**
     * Compose the Forwarded header and the X-Forwarded headers and add them to the connect session write headers.
     * 
     * @param acceptSession
     * @param connectSession
     * @param forwardedProperty the value of the 'use-forwarded' property used for http.proxy type in gateway-config:
     * inject (add the corresponding data to the Forwarded/X-Forwarded headers), ignore (this proxy is anonymous, no
     * forwarded header data is added), or exclude (delete any existing Forwarded/X-Forwarded headers received, and do
     * not add any new data)
     */
    private void setupForwardedHeaders(HttpAcceptSession acceptSession, HttpConnectSession connectSession) {
        if (FORWARDED_EXCLUDE.equalsIgnoreCase(useForwarded)) {
            excludeForwardedHeaders(connectSession);
            return;
        }

        if (FORWARDED_INJECT.equalsIgnoreCase(useForwarded)) {
            String remoteIpWithPort = format("%s:%d", getResourceIpAddress(acceptSession, FORWARDED_FOR), remoteClientPort);
            if (remoteIpWithPort != null) {
                connectSession.addWriteHeader(HEADER_X_FORWARDED_FOR, remoteIpWithPort);
            }

            String serverIpAddress = getResourceIpAddress(acceptSession, FORWARDED_BY);
            if (serverIpAddress != null) {
                connectSession.addWriteHeader(HEADER_X_FORWARDED_SERVER, serverIpAddress);
            }

            String protocol = acceptSession.isSecure() ? "https" : "http";
            connectSession.addWriteHeader(HEADER_X_FORWARDED_PROTO, protocol);

            String externalURI = acceptSession.getLocalAddress().getExternalURI();
            String host = URIUtils.getHost(externalURI);
            String port = format("%d", URIUtils.getPort(externalURI));
            connectSession.addWriteHeader(HEADER_X_FORWARDED_HOST, format("%s:%s", host, port));
            
            connectSession.addWriteHeader(HEADER_FORWARDED,
                    format("%s=%s;%s=%s;%s=%s;%s=%s:%s", FORWARDED_FOR, remoteIpWithPort, FORWARDED_BY, serverIpAddress,
                            FORWARDED_PROTO, protocol, FORWARDED_HOST, host, port));
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
    }

    /**
     * Get the IP address of the resource based on the parameter name
     * 
     * @param acceptSession
     * @param parameterName can be either 'for' (the IP address of the client/server making the request to this
     * service), or 'by' (the IP address of this proxy)
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
