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
import static org.kaazing.gateway.service.TransportOptionNames.HTTP_KEEP_ALIVE;
import static org.kaazing.gateway.service.TransportOptionNames.HTTP_KEEP_ALIVE_TIMEOUT_KEY;
import static org.kaazing.gateway.service.TransportOptionNames.INACTIVITY_TIMEOUT;
import static org.kaazing.gateway.service.TransportOptionNames.PIPE_TRANSPORT;
import static org.kaazing.gateway.service.TransportOptionNames.SSL_CIPHERS;
import static org.kaazing.gateway.service.TransportOptionNames.SSL_ENCRYPTION_ENABLED;
import static org.kaazing.gateway.service.TransportOptionNames.SSL_PROTOCOLS;
import static org.kaazing.gateway.service.TransportOptionNames.SSL_TRANSPORT;
import static org.kaazing.gateway.service.TransportOptionNames.TCP_TRANSPORT;
import static org.kaazing.gateway.service.TransportOptionNames.WS_PROTOCOL_VERSION;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.kaazing.gateway.server.config.sep2014.ServiceConnectOptionsType;
import org.kaazing.gateway.service.ConnectOptionsContext;
import org.kaazing.gateway.util.Utils;
import org.kaazing.gateway.util.ssl.SslCipherSuites;
import org.kaazing.gateway.util.ws.WebSocketWireProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DefaultConnectOptionsContext implements ConnectOptionsContext {

    private static final Logger logger = LoggerFactory.getLogger(DefaultConnectOptionsContext.class);

    private static final long DEFAULT_WS_INACTIVITY_TIMEOUT_MILLIS = 0L;
    private static final int DEFAULT_HTTP_KEEPALIVE_TIMEOUT = 30; //seconds

    private Map<String, String> options;

    public DefaultConnectOptionsContext() {
        this.options = new HashMap<String, String>();
    }

    public DefaultConnectOptionsContext(ServiceConnectOptionsType connectOptions, ServiceConnectOptionsType defaultOptions) {
        this();

        parseConnectOptionsType(connectOptions, defaultOptions);
    }

    @Override
    public void setOptions(Map<String, String> options) {
        this.options = options;
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
    }

    @Override
    public Map<String, Object> asOptionsMap() {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put(SSL_CIPHERS, getSslCiphers());
        result.put(SSL_PROTOCOLS, getSslProtocols());
        result.put(WS_PROTOCOL_VERSION, getWebSocketWireProtocol());
        result.put(INACTIVITY_TIMEOUT, getWsInactivityTimeout());

        result.put(PIPE_TRANSPORT, getTransportURI("pipe.transport"));
        result.put(TCP_TRANSPORT, getTransportURI("tcp.transport"));
        result.put(SSL_TRANSPORT, getTransportURI("ssl.transport"));
        result.put("http[http/1.1].transport", getTransportURI("http.transport"));

        result.put(SSL_ENCRYPTION_ENABLED, isSslEncryptionEnabled());
        result.put("udp.interface", getUdpInterface());

        result.put(HTTP_KEEP_ALIVE_TIMEOUT_KEY, getHttpKeepaliveTimeout());
        result.put(HTTP_KEEP_ALIVE, isHttpKeepaliveEnabled());

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

    private String getUdpInterface() {
        return options.get("udp.interface");
    }

    private WebSocketWireProtocol getWebSocketWireProtocol() {
        WebSocketWireProtocol protocol = WebSocketWireProtocol.RFC_6455;
        String wsVersion = options.get("ws.version");
        if ("draft-75".equals(wsVersion)) {
            protocol = WebSocketWireProtocol.HIXIE_75;
        }
        return protocol;
    }

    private String[] getSslCiphers() {
        String sslCiphersStr = options.get("ssl.ciphers");
        if (sslCiphersStr != null) {
            return SslCipherSuites.resolveCSV(sslCiphersStr);
        }
        return null;
    }

    private String[] getSslProtocols() {
        String sslProtocolsStr = options.get("ssl.protocols");
        if (sslProtocolsStr != null) {
            return resolveProtocols(sslProtocolsStr);
        }
        return null;
    }

    private long getWsInactivityTimeout() {
        long wsInactivityTimeout = DEFAULT_WS_INACTIVITY_TIMEOUT_MILLIS;
        String wsInactivityTimeoutValue = options.get("ws.inactivity.timeout");
        if (wsInactivityTimeoutValue != null) {
            long val = Utils.parseTimeInterval(wsInactivityTimeoutValue, TimeUnit.MILLISECONDS);
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

    private boolean isSslEncryptionEnabled() {
        boolean sslEncryptionEnabled = true;
        String sslEncryptionEnabledValue = options.get("ssl.encryption");
        if (sslEncryptionEnabledValue != null) {
            sslEncryptionEnabled = !sslEncryptionEnabledValue.equalsIgnoreCase("disabled");
        }
        return sslEncryptionEnabled;
    }

    private Integer getHttpKeepaliveTimeout() {
        int httpKeepaliveTimeout = DEFAULT_HTTP_KEEPALIVE_TIMEOUT;
        String timeoutValue = options.get("http.keepalive.timeout");
        if (timeoutValue != null) {
            long val = Utils.parseTimeInterval(timeoutValue, TimeUnit.SECONDS);
            if (val > 0) {
                httpKeepaliveTimeout = (int) val;
            }
        }

        return httpKeepaliveTimeout;
    }

    private boolean isHttpKeepaliveEnabled() {
        boolean httpKeepaliveEnabled = true;
        String httpKeepaliveEnabledValue = options.get("http.keepalive");
        if (httpKeepaliveEnabledValue != null) {
            httpKeepaliveEnabled = !httpKeepaliveEnabledValue.equalsIgnoreCase("disabled");
        }

        return httpKeepaliveEnabled;
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

    private void parseConnectOptionsType(ServiceConnectOptionsType connectOptionsType,
                                         ServiceConnectOptionsType defaultOptionsType) {
        if (connectOptionsType != null) {
            Map<String, String> connectOptionsMap = new HashMap<String, String>();
            parseOptions(connectOptionsType.getDomNode(), connectOptionsMap);
            setOptions(connectOptionsMap);
        }

        if (defaultOptionsType != null) {
            Map<String, String> defaultConnectOptionsMap = new HashMap<String, String>();
            parseOptions(defaultOptionsType.getDomNode(), defaultConnectOptionsMap);
            setDefaultOptions(defaultConnectOptionsMap);
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
