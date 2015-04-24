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

package org.kaazing.gateway.server.context.resolve;

import static java.lang.String.format;
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.kaazing.gateway.server.config.sep2014.ServiceAcceptOptionsType;
import org.kaazing.gateway.service.AcceptOptionsContext;
import org.kaazing.gateway.util.Utils;
import org.kaazing.gateway.util.ssl.SslCipherSuites;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DefaultAcceptOptionsContext implements AcceptOptionsContext {
    private static final Logger logger = LoggerFactory.getLogger(DefaultAcceptOptionsContext.class);

    private static int DEFAULT_WEBSOCKET_MAXIMUM_MESSAGE_SIZE = 128 * 1024; //128KB
    private static int DEFAULT_HTTP_KEEPALIVE_TIMEOUT = 30; //seconds
    private static final long UNLIMITED_MAX_OUTPUT_RATE = 0xFFFFFFFFL;
    private static long DEFAULT_TCP_MAXIMUM_OUTBOUND_RATE = UNLIMITED_MAX_OUTPUT_RATE; //unlimited

    /**
     * The name of the extended handshake protocol to be sent on the wire.
     */
    public static final String EXTENDED_HANDSHAKE_PROTOCOL_NAME = "x-kaazing-handshake";

    private static final String PING_PONG = "x-kaazing-ping-pong";
    private static final String IDLE_TIMEOUT = "x-kaazing-idle-timeout";
    private static final long DEFAULT_WS_INACTIVITY_TIMEOUT_MILLIS = 0L;

    private static List<String> DEFAULT_WEBSOCKET_PROTOCOLS;
    private static List<String> DEFAULT_WEBSOCKET_EXTENSIONS;


    static {
        // Note: including null in these arrays permits us to process no protocols/extensions
        // without throwing an exception.
        DEFAULT_WEBSOCKET_PROTOCOLS = Arrays.asList(EXTENDED_HANDSHAKE_PROTOCOL_NAME, null);
        DEFAULT_WEBSOCKET_EXTENSIONS = Arrays.asList(PING_PONG, null);
    }

    private final Map<String, String> binds;
    private Map<String, String> options;

    public DefaultAcceptOptionsContext() {
        this.binds = new HashMap<>();
        this.options = new HashMap<>();
    }

    public DefaultAcceptOptionsContext(ServiceAcceptOptionsType acceptOptions, ServiceAcceptOptionsType defaultOptions) {
        this();

        parseAcceptOptionsType(acceptOptions, defaultOptions);
    }

    @Override
    public void setOptions(Map<String, String> options) {
        this.options = options;

        // process the binds specially to be referenced by scheme rather than
        // the <protocol>.bind key in the options map
        addBind("ws", options.get("ws.bind"));
        addBind("wss", options.get("wss.bind"));
        addBind("http", options.get("http.bind"));
        addBind("https", options.get("https.bind"));
        addBind("ssl", options.get("ssl.bind"));
        addBind("tcp", options.get("tcp.bind"));
    }

    @Override
    public void setDefaultOptions(Map<String, String> defaultOptions) {
        if (options == null) {
            options = defaultOptions;
        } else if (defaultOptions != null) {
            for (Entry<String, String> entry : defaultOptions.entrySet()) {
                if (!options.containsKey(entry.getKey())) {
                    options.put(entry.getKey(), entry.getValue());
                }
            }
        }

        // process the binds specially to be referenced by scheme rather than
        // the <protocol>.bind key in the options map
        addBind("ws", defaultOptions.get("ws.bind"));
        addBind("wss", defaultOptions.get("wss.bind"));
        addBind("http", defaultOptions.get("http.bind"));
        addBind("https", defaultOptions.get("https.bind"));
        addBind("ssl", defaultOptions.get("ssl.bind"));
        addBind("tcp", defaultOptions.get("tcp.bind"));
    }

    @Override
    public Map<String, String> getBinds() {
        return binds;
    }

    @Override
    public URI getInternalURI(URI externalURI) {
        String authority = externalURI.getAuthority();
        String internalAuthority = binds.get(externalURI.getScheme());
        if (internalAuthority != null) {
            if (!internalAuthority.equals(authority)) {
                try {
                    return new URI(externalURI.getScheme(), internalAuthority, externalURI.getPath(),
                            externalURI.getQuery(), externalURI.getFragment());
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

    public Map<String, Object> asOptionsMap() {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put(SUPPORTED_PROTOCOLS, DEFAULT_WEBSOCKET_PROTOCOLS.toArray(
                new String[DEFAULT_WEBSOCKET_PROTOCOLS.size()]));

        long wsInactivityTimeout = getWsInactivityTimeout();
        result.put("ws.inactivityTimeout", wsInactivityTimeout);
        result.put("ws[ws/rfc6455].ws[ws/rfc6455].inactivityTimeout", wsInactivityTimeout);
        result.put("ws[ws/draft-7x].ws[ws/draft-7x].inactivityTimeout", wsInactivityTimeout);

        List<String> wsExtensions = getWsExtensions(wsInactivityTimeout);
        result.put("ws.extensions", wsExtensions);
        result.put("ws[ws/rfc6455].ws[ws/rfc6455].extensions", wsExtensions);
        result.put("ws[ws/draft-7x].ws[ws/draft-7x].extensions", wsExtensions);

        int wsMaxMessageSize = getWsMaximumMessageSize();
        result.put("ws.maxMessageSize", wsMaxMessageSize);
        result.put("ws[ws/rfc6455].ws[ws/rfc6455].maxMessageSize", wsMaxMessageSize);
        result.put("ws[ws/draft-7x].ws[ws/draft-7x].maxMessageSize", wsMaxMessageSize);

        result.put("http[http/1.1].keepAliveTimeout", getHttpKeepaliveTimeout());
        result.put(SSL_CIPHERS, getSslCiphers());
        result.put(SSL_PROTOCOLS, getSslProtocols());
        result.put(SSL_ENCRYPTION_ENABLED, isSslEncryptionEnabled());

        result.put(HTTP_SERVER_HEADER_ENABLED, isHttpServerHeaderEnabled());

        boolean[] clientAuth = getVerifyClientProperties();
        result.put(SSL_WANT_CLIENT_AUTH, clientAuth[0]);
        result.put(SSL_NEED_CLIENT_AUTH, clientAuth[1]);

        result.put(PIPE_TRANSPORT, getTransportURI("pipe.transport"));
        result.put(TCP_TRANSPORT, getTransportURI("tcp.transport"));
        result.put(SSL_TRANSPORT, getTransportURI("ssl.transport"));
        result.put("http[http/1.1].transport", getTransportURI("http.transport"));

        result.put(TCP_MAXIMUM_OUTBOUND_RATE, getTcpMaximumOutboundRate());

        result.put("udp.interface", options.get("udp.interface"));

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
        for (Entry<String, String> entry : options.entrySet()) {
            String key = entry.getKey();
            if (!result.containsKey(key)) {
                // Special check for *.transport which should be validated as a URI
                if (key.endsWith(".transport")) {
                    try {
                        URI transportURI = URI.create(entry.getValue());
                        result.put(key, transportURI);
                    } catch (IllegalArgumentException ex) {
                        if (logger.isInfoEnabled()) {
                            logger.info(String.format("Skipping option %s, expected valid URI but recieved: %s",
                                    key, entry.getValue()));
                        }
                    }
                } else {
                    result.put(key, entry.getValue());
                }
            }
        }

        return result;
    }

    private String resolveInternalBindOptionName(String externalBindOptionName) {
        if (externalBindOptionName.equals("tcp")) {
            return "tcp.bind";
        } else if (externalBindOptionName.equals("ssl")) {
            return "ssl.tcp.bind";
        } else if (externalBindOptionName.equals("http")) {
            return "http.tcp.bind";
        } else if (externalBindOptionName.equals("https")) {
            return "http.ssl.tcp.bind";
        } else if (externalBindOptionName.equals("ws")) {
            return "ws.http.tcp.bind";
        } else if (externalBindOptionName.equals("wss")) {
            return "ws.http.ssl.tcp.bind";
        } else if (externalBindOptionName.equals("wsn")) {
            return "wsn.http.tcp.bind";
        } else if (externalBindOptionName.equals("wsn+ssl")) {
            return "wsn.http.ssl.tcp.bind";
        } else if (externalBindOptionName.equals("wsx")) {
            return "wsn.http.wsn.http.tcp.bind";
        } else if (externalBindOptionName.equals("wsx+ssl")) {
            return "wsn.http.wsn.http.ssl.tcp.bind";
        } else if (externalBindOptionName.equals("httpxe")) {
            return "http.http.tcp.bind";
        } else if (externalBindOptionName.equals("httpxe+ssl")) {
            return "http.http.ssl.tcp.bind";
        }
        return null;
    }

    // We return a String array here, rather than a list, because the
    // javax.net.ssl.SSLEngine.setEnabledProtocols() method wants a
    // String array.
    public static String[] resolveProtocols(String csv) {
        if (csv != null && !csv.equals("")) {
            return csv.split(",");
        } else {
            return null;
        }
    }

    private long getWsInactivityTimeout() {
        long wsInactivityTimeout = DEFAULT_WS_INACTIVITY_TIMEOUT_MILLIS;
        String value = options.get("ws.inactivity.timeout");
        if (value != null) {
            long val = Utils.parseTimeInterval(value, TimeUnit.MILLISECONDS);
            if (val > 0) {
                wsInactivityTimeout = val;
            }
        }
        return wsInactivityTimeout;
    }

    private URI getTransportURI(String transportKey) {
        URI transportURI = null;
        String transport = options.get(transportKey);
        if (transport != null) {
            transportURI = URI.create(transport);
            if (!transportURI.isAbsolute()) {
                throw new IllegalArgumentException(format(
                        "%s must contain an absolute URI, not \"%s\"", transportKey, transport));
            }
        }

        return transportURI;
    }

    private List<String> getWsExtensions(long wsInactivityTimeout) {
        List<String> wsExtensions = null;
        if (wsInactivityTimeout > 0) {
            ArrayList<String> extensions = new ArrayList<>(DEFAULT_WEBSOCKET_EXTENSIONS);
            extensions.add(IDLE_TIMEOUT);
            wsExtensions = extensions;
        } else {
            wsExtensions = DEFAULT_WEBSOCKET_EXTENSIONS;
        }
        return wsExtensions;
    }

    private int getWsMaximumMessageSize() {
        int wsMaxMessageSize = DEFAULT_WEBSOCKET_MAXIMUM_MESSAGE_SIZE;
        String wsMaxMessageSizeValue = options.get("ws.maximum.message.size");
        if (wsMaxMessageSizeValue != null) {
            wsMaxMessageSize = Utils.parseDataSize(wsMaxMessageSizeValue);
        }
        return wsMaxMessageSize;
    }

    private long getTcpMaximumOutboundRate() {
        long tcpMaximumOutboundRate = DEFAULT_TCP_MAXIMUM_OUTBOUND_RATE;
        String tcpMaxOutboundRate = options.get("tcp.maximum.outbound.rate");
        if (tcpMaxOutboundRate != null) {
            tcpMaximumOutboundRate = Utils.parseDataRate(tcpMaxOutboundRate);

            if ((tcpMaximumOutboundRate == 0) || (tcpMaximumOutboundRate > UNLIMITED_MAX_OUTPUT_RATE)) {
                tcpMaximumOutboundRate = UNLIMITED_MAX_OUTPUT_RATE;
            }
        }
        return tcpMaximumOutboundRate;
    }

    private boolean[] getVerifyClientProperties() {
        boolean[] clientAuth = { false, false };

        String sslVerifyClientValue = options.get("ssl.verify-client");
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

    private boolean isSslEncryptionEnabled() {
        boolean sslEncryptionEnabled = true;
        String sslEncryptionEnabledValue = options.get("ssl.encryption");
        if (sslEncryptionEnabledValue != null) {
            sslEncryptionEnabled = !sslEncryptionEnabledValue.equalsIgnoreCase("disabled");
        }
        return sslEncryptionEnabled;
    }

    private boolean isHttpServerHeaderEnabled() {
        String serverHeaderEnabled = options.get("http.server.header");
        return serverHeaderEnabled == null || !serverHeaderEnabled.equalsIgnoreCase("disabled");
    }

    private String[] getSslProtocols() {
        String[] sslProtocols = null;
        String sslProtocolsValue = options.get("ssl.protocols");
        if (sslProtocolsValue != null) {
            sslProtocols = resolveProtocols(sslProtocolsValue);
        }
        return sslProtocols;
    }

    private String[] getSslCiphers() {
        String[] sslCiphers = null;
        String sslCiphersValue = options.get("ssl.ciphers");
        if (sslCiphersValue != null) {
            sslCiphers = SslCipherSuites.resolveCSV(sslCiphersValue);
        }
        return sslCiphers;
    }

    private int getHttpKeepaliveTimeout() {
        int httpKeepaliveTimeout = DEFAULT_HTTP_KEEPALIVE_TIMEOUT;
        String httpKeepaliveTimeoutValue = options.get("http.keepalive.timeout");
        if (httpKeepaliveTimeoutValue != null) {
            long val = Utils.parseTimeInterval(httpKeepaliveTimeoutValue, TimeUnit.SECONDS);
            if (val > 0) {
                httpKeepaliveTimeout = (int) val;
            }
        }
        return httpKeepaliveTimeout;
    }

    private void parseAcceptOptionsType(ServiceAcceptOptionsType acceptOptionsType,
                                        ServiceAcceptOptionsType defaultOptionsType) {
        if (acceptOptionsType != null) {
            Map<String, String> acceptOptionsMap = new HashMap<String, String>();
            parseOptions(acceptOptionsType.getDomNode(), acceptOptionsMap);
            setOptions(acceptOptionsMap);
        }

        if (defaultOptionsType != null) {
            Map<String, String> defaultAcceptOptionsMap = new HashMap<String, String>();
            parseOptions(defaultOptionsType.getDomNode(), defaultAcceptOptionsMap);
            setDefaultOptions(defaultAcceptOptionsMap);
        }
    }

    private void parseOptions(Node parent, Map<String, String> optionsMap) {
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
    }
}
