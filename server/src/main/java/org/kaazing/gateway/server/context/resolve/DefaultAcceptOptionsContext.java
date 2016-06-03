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
package org.kaazing.gateway.server.context.resolve;

import static java.lang.String.format;
import static org.kaazing.gateway.resource.address.uri.URIUtils.buildURIAsString;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getAuthority;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getFragment;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getPath;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getQuery;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getScheme;
import static org.kaazing.gateway.service.TransportOptionNames.HTTP_SERVER_HEADER_ENABLED;
import static org.kaazing.gateway.service.TransportOptionNames.PIPE_TRANSPORT;
import static org.kaazing.gateway.service.TransportOptionNames.SSL_CIPHERS;
import static org.kaazing.gateway.service.TransportOptionNames.SSL_ENCRYPTION_ENABLED;
import static org.kaazing.gateway.service.TransportOptionNames.SSL_NEED_CLIENT_AUTH;
import static org.kaazing.gateway.service.TransportOptionNames.SSL_PROTOCOLS;
import static org.kaazing.gateway.service.TransportOptionNames.SSL_TRANSPORT;
import static org.kaazing.gateway.service.TransportOptionNames.SSL_WANT_CLIENT_AUTH;
import static org.kaazing.gateway.service.TransportOptionNames.SUPPORTED_PROTOCOLS;
import static org.kaazing.gateway.service.TransportOptionNames.TCP_MAXIMUM_OUTBOUND_RATE;
import static org.kaazing.gateway.service.TransportOptionNames.TCP_TRANSPORT;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.kaazing.gateway.resource.address.uri.URIUtils;
import org.kaazing.gateway.server.config.nov2015.ServiceAcceptOptionsType;
import org.kaazing.gateway.service.AcceptOptionsContext;
import org.kaazing.gateway.util.Utils;
import org.kaazing.gateway.util.ssl.SslCipherSuites;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class DefaultAcceptOptionsContext implements AcceptOptionsContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAcceptOptionsContext.class);

    private static final int DEFAULT_WEBSOCKET_MAXIMUM_MESSAGE_SIZE = 128 * 1024; //128KB
    private static final int DEFAULT_HTTP_KEEPALIVE_TIMEOUT = 30; //seconds
    private static final long UNLIMITED_MAX_OUTPUT_RATE = 0xFFFFFFFFL;
    private static final long DEFAULT_TCP_MAXIMUM_OUTBOUND_RATE = UNLIMITED_MAX_OUTPUT_RATE; //unlimited

    /**
     * The name of the extended handshake protocol to be sent on the wire.
     */
    private static final String EXTENDED_HANDSHAKE_PROTOCOL_NAME = "x-kaazing-handshake";

    private static final String PING_PONG = "x-kaazing-ping-pong";
    private static final String IDLE_TIMEOUT = "x-kaazing-idle-timeout";
    private static final long DEFAULT_WS_INACTIVITY_TIMEOUT_MILLIS = 0L;

    private static final List<String> DEFAULT_WEBSOCKET_PROTOCOLS;
    private static final List<String> DEFAULT_WEBSOCKET_EXTENSIONS;

    static {
        // Note: including null in these arrays permits us to process no protocols/extensions
        // without throwing an exception.
        DEFAULT_WEBSOCKET_PROTOCOLS = Arrays.asList(EXTENDED_HANDSHAKE_PROTOCOL_NAME, null);
        DEFAULT_WEBSOCKET_EXTENSIONS = Arrays.asList(PING_PONG, null);
    }

    private final Map<String, String> binds;        // gets modified by balancer service
    private final Map<String, String> options;      // unmodifiable map without bind options like tcp.bind etc

    DefaultAcceptOptionsContext() {
        this.binds = new HashMap<>();
        this.options = Collections.emptyMap();
    }

    DefaultAcceptOptionsContext(ServiceAcceptOptionsType acceptOptions, ServiceAcceptOptionsType defaultOptions) {
        Map<String, String> options = parseAcceptOptionsType(acceptOptions);
        parseAcceptOptionsType(defaultOptions).entrySet()
                .stream()
                .forEach(e -> options.putIfAbsent(e.getKey(), e.getValue()));

        this.binds = new HashMap<>();

        // process the binds specially to be referenced by scheme rather than
        // the <protocol>.bind key in the options map
        addBind("ws", options.remove("ws.bind"));
        addBind("wss", options.remove("wss.bind"));
        addBind("http", options.remove("http.bind"));
        addBind("https", options.remove("https.bind"));
        addBind("ssl", options.remove("ssl.bind"));
        addBind("tcp", options.remove("tcp.bind"));

        this.options = Collections.unmodifiableMap(options);
    }

    @Override
    public Map<String, String> getBinds() {
        return binds;
    }

    @Override
    public String getInternalURI(String externalURI) {
        String authority = getAuthority(externalURI);
        String internalAuthority = binds.get(getScheme(externalURI));
        if (internalAuthority != null) {
            if (!internalAuthority.equals(authority)) {
                try {
                    return buildURIAsString(getScheme(externalURI), internalAuthority,
                           getPath(externalURI), getQuery(externalURI), getFragment(externalURI));
                } catch (URISyntaxException e) {
                    // ignore
                }
            }
            return externalURI;
        }

        // there's no binding for this URI, return null
        return null;
    }

    @Override
    public void addBind(String scheme, String hostPort) {
        // if the given host/port is non-null, and the bindings map does not
        // already contain the given bind, add the binding to the map.
        if ((hostPort != null) && !binds.containsKey(scheme)) {
            if (!hostPort.contains(":")) {
                try {
                    int port = Integer.parseInt(hostPort);
                    this.binds.put(scheme, "0.0.0.0:" + port);
                } catch (NumberFormatException ex) {
                    throw new RuntimeException("Failed to add bind for scheme " + scheme + " to port " + hostPort, ex);
                }
            } else {
                this.binds.put(scheme, hostPort);
            }
        }
    }

    @Override
    public Map<String, Object> asOptionsMap() {
        Map<String, String> optionsCopy = new HashMap<>(options);

        Map<String, Object> result = new LinkedHashMap<>();

        result.put(SUPPORTED_PROTOCOLS, DEFAULT_WEBSOCKET_PROTOCOLS.toArray(
                new String[DEFAULT_WEBSOCKET_PROTOCOLS.size()]));

        String wsInactivityTimeoutStr = optionsCopy.remove("ws.inactivity.timeout");
        String httpKeepaliveTimeoutStr = optionsCopy.remove("http.keepalive.timeout");
        // ws.inactivity.timeout is used for http.keepalive.timeout so that connections
        // are kept alive in wse case
        if (wsInactivityTimeoutStr != null && httpKeepaliveTimeoutStr == null) {
            httpKeepaliveTimeoutStr = wsInactivityTimeoutStr;
        }

        long wsInactivityTimeout = getWsInactivityTimeout(wsInactivityTimeoutStr);
        result.put("ws.inactivityTimeout", wsInactivityTimeout);
        result.put("ws[ws/rfc6455].ws[ws/rfc6455].inactivityTimeout", wsInactivityTimeout);
        result.put("ws[ws/draft-7x].ws[ws/draft-7x].inactivityTimeout", wsInactivityTimeout);

        List<String> wsExtensions = getWsExtensions(wsInactivityTimeout);
        result.put("ws.extensions", wsExtensions);
        result.put("ws[ws/rfc6455].ws[ws/rfc6455].extensions", wsExtensions);
        result.put("ws[ws/draft-7x].ws[ws/draft-7x].extensions", wsExtensions);

        int wsMaxMessageSize = getWsMaximumMessageSize(optionsCopy.remove("ws.maximum.message.size"));
        result.put("ws.maxMessageSize", wsMaxMessageSize);
        result.put("ws[ws/rfc6455].ws[ws/rfc6455].maxMessageSize", wsMaxMessageSize);
        result.put("ws[ws/draft-7x].ws[ws/draft-7x].maxMessageSize", wsMaxMessageSize);

        int httpKeepaliveTimeout = getHttpKeepaliveTimeout(httpKeepaliveTimeoutStr);
        result.put("http[http/1.1].keepAliveTimeout", httpKeepaliveTimeout);
        if (httpKeepaliveTimeout < wsInactivityTimeout) {
            LOGGER.warn("ws.inactivity.timeout={} should be greater than http.keealive.timeout={}",
                    wsInactivityTimeout, httpKeepaliveTimeout);
        }

        String[] sslCiphers = getSslCiphers(optionsCopy.remove("ssl.ciphers"));
        if (sslCiphers != null) {
            result.put(SSL_CIPHERS, sslCiphers);
        }

        String[] sslProtocols = getSslProtocols(optionsCopy.remove("ssl.protocols"));
        if (sslProtocols != null) {
            result.put(SSL_PROTOCOLS, sslProtocols);
        }

        boolean sslEncryptionEnabled = isSslEncryptionEnabled(optionsCopy.remove("ssl.encryption"));
        result.put(SSL_ENCRYPTION_ENABLED, sslEncryptionEnabled);

        boolean serverHeaderEnabled = isHttpServerHeaderEnabled(optionsCopy.remove("http.server.header"));
        result.put(HTTP_SERVER_HEADER_ENABLED, serverHeaderEnabled);

        String sslVerifyClientValue = optionsCopy.remove("ssl.verify-client");
        boolean[] clientAuth = getVerifyClientProperties(sslVerifyClientValue);
        result.put(SSL_WANT_CLIENT_AUTH, clientAuth[0]);
        result.put(SSL_NEED_CLIENT_AUTH, clientAuth[1]);

        String pipeTransport = getTransportURI("pipe.transport", optionsCopy.remove("pipe.transport"));
        if (pipeTransport != null) {
            result.put(PIPE_TRANSPORT, pipeTransport);
        }

        String tcpTransport = getTransportURI("tcp.transport", optionsCopy.remove("tcp.transport"));
        if (tcpTransport != null) {
            result.put(TCP_TRANSPORT, tcpTransport);
        }

        String sslTransport = getTransportURI("ssl.transport", optionsCopy.remove("ssl.transport"));
        if (sslTransport != null) {
            result.put(SSL_TRANSPORT, sslTransport);
        }

        String httpTransport = getTransportURI("http.transport", optionsCopy.remove("http.transport"));
        if (httpTransport != null) {
            result.put("http[http/1.1].transport", httpTransport);
        }

        long tcpMaximumOutboundRate = getTcpMaximumOutboundRate(optionsCopy.remove("tcp.maximum.outbound.rate"));
        result.put(TCP_MAXIMUM_OUTBOUND_RATE, tcpMaximumOutboundRate);

        String udpInterface = optionsCopy.remove("udp.interface");
        if (udpInterface != null) {
            result.put("udp.interface", udpInterface);
        }

        for (Map.Entry<String, String> entry : getBinds().entrySet()) {
            /* For lookups out of this COPY of the options, we need to
             * translate the scheme names into hierarchical transport names,
             * i.e.:
             *
             *  ssl -> ssl.tcp.bind
             *  http -> http.tcp.bind
             *  https -> http.ssl.tcp.bind
             *  ws -> ws.tcp.bind
             *  wss -> ws.ssl.tcp.bind
             */

            String internalBindOptionName = resolveInternalBindOptionName(entry.getKey());
            if (internalBindOptionName != null) {
                result.put(internalBindOptionName, entry.getValue());
            } else {
                throw new RuntimeException("Cannot apply unknown bind option '" + entry.getKey() + "'.");
            }
        }

        // for now just put in the rest of the options as strings
        result.putAll(optionsCopy);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(format("Accept options map = %s", result));
        }

        return result;
    }

    private static String resolveInternalBindOptionName(String externalBindOptionName) {
        switch (externalBindOptionName) {
            case "tcp":
                return "tcp.bind";
            case "ssl":
                return "ssl.tcp.bind";
            case "http":
                return "http.tcp.bind";
            case "https":
                return "http.ssl.tcp.bind";
            case "ws":
                return "ws.http.tcp.bind";
            case "wss":
                return "ws.http.ssl.tcp.bind";
            case "wsn":
                return "wsn.http.tcp.bind";
            case "wsn+ssl":
                return "wsn.http.ssl.tcp.bind";
            case "wsx":
                return "wsn.http.wsn.http.tcp.bind";
            case "wsx+ssl":
                return "wsn.http.wsn.http.ssl.tcp.bind";
            case "httpxe":
                return "http.http.tcp.bind";
            case "httpxe+ssl":
                return "http.http.ssl.tcp.bind";
        }
        return null;
    }

    // We return a String array here, rather than a list, because the
    // javax.net.ssl.SSLEngine.setEnabledProtocols() method wants a
    // String array.
    private static String[] resolveProtocols(String csv) {
        if (csv != null && !csv.equals("")) {
            return csv.split(",");
        } else {
            return null;
        }
    }

    private static long getWsInactivityTimeout(String value) {
        long wsInactivityTimeout = DEFAULT_WS_INACTIVITY_TIMEOUT_MILLIS;
        if (value != null) {
            long val = Utils.parseTimeInterval(value, TimeUnit.MILLISECONDS);
            if (val > 0) {
                wsInactivityTimeout = val;
            }
        }
        return wsInactivityTimeout;
    }

    private static String getTransportURI(String transportKey, String transport) {
        String transportURI = null;
        if (transport != null) {
            transportURI = transport;
            if (!URIUtils.isAbsolute(transportURI)) {
                throw new IllegalArgumentException(format(
                        "%s must contain an absolute URI, not \"%s\"", transportKey, transport));
            }
        }

        return transportURI;
    }

    private static List<String> getWsExtensions(long wsInactivityTimeout) {
        List<String> wsExtensions;
        if (wsInactivityTimeout > 0) {
            ArrayList<String> extensions = new ArrayList<>(DEFAULT_WEBSOCKET_EXTENSIONS);
            extensions.add(IDLE_TIMEOUT);
            wsExtensions = extensions;
        } else {
            wsExtensions = DEFAULT_WEBSOCKET_EXTENSIONS;
        }
        return wsExtensions;
    }

    private static int getWsMaximumMessageSize(String wsMaxMessageSizeValue) {
        int wsMaxMessageSize = DEFAULT_WEBSOCKET_MAXIMUM_MESSAGE_SIZE;
        if (wsMaxMessageSizeValue != null) {
            wsMaxMessageSize = Utils.parseDataSize(wsMaxMessageSizeValue);
        }
        return wsMaxMessageSize;
    }

    private static long getTcpMaximumOutboundRate(String tcpMaxOutboundRate) {
        long tcpMaximumOutboundRate = DEFAULT_TCP_MAXIMUM_OUTBOUND_RATE;
        if (tcpMaxOutboundRate != null) {
            tcpMaximumOutboundRate = Utils.parseDataRate(tcpMaxOutboundRate);

            if ((tcpMaximumOutboundRate == 0) || (tcpMaximumOutboundRate > UNLIMITED_MAX_OUTPUT_RATE)) {
                tcpMaximumOutboundRate = UNLIMITED_MAX_OUTPUT_RATE;
            }
        }
        return tcpMaximumOutboundRate;
    }

    private static boolean[] getVerifyClientProperties(String sslVerifyClientValue) {
        boolean[] clientAuth = { false, false };

        if (sslVerifyClientValue != null) {
            if (sslVerifyClientValue.equalsIgnoreCase("required")) {
                clientAuth[0] = false;
                clientAuth[1] = true;
            } else if (sslVerifyClientValue.equalsIgnoreCase("optional")) {
                clientAuth[0] = true;
                clientAuth[1] = false;
            }
        }
        return clientAuth;
    }

    private static boolean isSslEncryptionEnabled(String sslEncryptionEnabledValue) {
        boolean sslEncryptionEnabled = true;
        if (sslEncryptionEnabledValue != null) {
            sslEncryptionEnabled = !sslEncryptionEnabledValue.equalsIgnoreCase("disabled");
        }
        return sslEncryptionEnabled;
    }

    private static boolean isHttpServerHeaderEnabled(String serverHeaderEnabled) {
        return serverHeaderEnabled == null || !serverHeaderEnabled.equalsIgnoreCase("disabled");
    }

    private static String[] getSslProtocols(String sslProtocolsValue) {
        String[] sslProtocols = null;
        if (sslProtocolsValue != null) {
            sslProtocols = resolveProtocols(sslProtocolsValue);
        }
        return sslProtocols;
    }

    private static String[] getSslCiphers(String sslCiphersValue) {
        String[] sslCiphers = null;
        if (sslCiphersValue != null) {
            sslCiphers = SslCipherSuites.resolveCSV(sslCiphersValue);
        }
        return sslCiphers;
    }

    private static int getHttpKeepaliveTimeout(String httpKeepaliveTimeoutValue) {
        int httpKeepaliveTimeout = DEFAULT_HTTP_KEEPALIVE_TIMEOUT;
        if (httpKeepaliveTimeoutValue != null) {
            long val = Utils.parseTimeInterval(httpKeepaliveTimeoutValue, TimeUnit.SECONDS);
            if (val > 0) {
                httpKeepaliveTimeout = (int) val;
            }
        }
        return httpKeepaliveTimeout;
    }

    private Map<String, String> parseAcceptOptionsType(ServiceAcceptOptionsType acceptOptionsType) {
        return acceptOptionsType != null ? parseOptions(acceptOptionsType.getDomNode()) : new HashMap<>();
    }

    private Map<String, String> parseOptions(Node parent) {
        Map<String, String> optionsMap = new HashMap<>();

        NodeList childNodes = parent.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (Node.ELEMENT_NODE == node.getNodeType()) {
                NodeList content = node.getChildNodes();
                String nodeValue = "";
                for (int j = 0; j < content.getLength(); j++) {
                    Node child = content.item(j);
                    if (child != null) {
                        if (child.getNodeType() == Node.TEXT_NODE) {
                            // GatewayConfigParser skips white space so we don't need to trim. We concatenate in case
                            // the parser coughs up text content as more than one Text node.
                            String fragment = child.getNodeValue();
                            if (fragment != null) {
                                nodeValue = nodeValue + fragment;
                            }
                        }
                        // Skip over other node types
                    }
                }
                optionsMap.put(node.getLocalName(), nodeValue);
            }
        }
        return optionsMap;
    }
}
