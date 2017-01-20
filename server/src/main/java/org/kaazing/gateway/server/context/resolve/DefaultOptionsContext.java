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
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getCanonicalizedURI;

import java.util.HashMap;
import java.util.Map;

import org.kaazing.gateway.resource.address.uri.URIUtils;
import org.kaazing.gateway.util.Utils;
import org.kaazing.gateway.util.ssl.SslCipherSuites;
import org.kaazing.gateway.util.ws.WebSocketWireProtocol;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class DefaultOptionsContext {

    private static final int DEFAULT_WEBSOCKET_MAXIMUM_MESSAGE_SIZE = 128 * 1024; //128KB
    private static final int DEFAULT_HTTP_KEEPALIVE_TIMEOUT = 30; //seconds
    private static final long UNLIMITED_MAX_OUTPUT_RATE = 0xFFFFFFFFL;
    private static final long DEFAULT_TCP_MAXIMUM_OUTBOUND_RATE = UNLIMITED_MAX_OUTPUT_RATE; //unlimited
    private static final long DEFAULT_WS_INACTIVITY_TIMEOUT_MILLIS = 0L;

    static String resolveInternalBindOptionName(String externalBindOptionName) {
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

    static long getWsInactivityTimeout(String value) {
        long wsInactivityTimeout = DEFAULT_WS_INACTIVITY_TIMEOUT_MILLIS;
        if (value != null) {
            long val = Utils.parseTimeInterval(value, MILLISECONDS);
            if (val > 0) {
                wsInactivityTimeout = val;
            }
        }
        return wsInactivityTimeout;
    }

    static String getTransportURI(String transportKey, String transport) {
        String transportURI = null;
        if (transport != null) {
            transportURI = transport;
            if (!URIUtils.isAbsolute(transportURI)) {
                throw new IllegalArgumentException(format(
                        "%s must contain an absolute URI, not \"%s\"", transportKey, transport));
            }
            transportURI = getCanonicalizedURI(transportURI, false);
        }

        return transportURI;
    }

    static int getWsMaximumMessageSize(String wsMaxMessageSizeValue) {
        int wsMaxMessageSize = DEFAULT_WEBSOCKET_MAXIMUM_MESSAGE_SIZE;
        if (wsMaxMessageSizeValue != null) {
            wsMaxMessageSize = Utils.parseDataSize(wsMaxMessageSizeValue);
        }
        return wsMaxMessageSize;
    }

    static long getTcpMaximumOutboundRate(String tcpMaxOutboundRate) {
        long tcpMaximumOutboundRate = DEFAULT_TCP_MAXIMUM_OUTBOUND_RATE;
        if (tcpMaxOutboundRate != null) {
            tcpMaximumOutboundRate = Utils.parseDataRate(tcpMaxOutboundRate);

            if ((tcpMaximumOutboundRate == 0) || (tcpMaximumOutboundRate > UNLIMITED_MAX_OUTPUT_RATE)) {
                tcpMaximumOutboundRate = UNLIMITED_MAX_OUTPUT_RATE;
            }
        }
        return tcpMaximumOutboundRate;
    }

    static boolean[] getVerifyClientProperties(String sslVerifyClientValue) {
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

    static boolean isSslEncryptionEnabled(String sslEncryptionEnabledValue) {
        boolean sslEncryptionEnabled = true;
        if (sslEncryptionEnabledValue != null) {
            sslEncryptionEnabled = !sslEncryptionEnabledValue.equalsIgnoreCase("disabled");
        }
        return sslEncryptionEnabled;
    }

    static boolean isHttpServerHeaderEnabled(String serverHeaderEnabled) {
        return serverHeaderEnabled == null || !serverHeaderEnabled.equalsIgnoreCase("disabled");
    }

    static String[] getSslProtocols(String sslProtocolsValue) {
        String[] sslProtocols = null;
        if (sslProtocolsValue != null) {
            sslProtocols = resolveProtocols(sslProtocolsValue);
        }
        return sslProtocols;
    }

    static String[] getSslCiphers(String sslCiphersValue) {
        String[] sslCiphers = null;
        if (sslCiphersValue != null) {
            sslCiphers = SslCipherSuites.resolveCSV(sslCiphersValue);
        }
        return sslCiphers;
    }

    static int getHttpKeepaliveTimeout(String httpKeepaliveTimeoutValue) {
        int httpKeepaliveTimeout = DEFAULT_HTTP_KEEPALIVE_TIMEOUT;
        if (httpKeepaliveTimeoutValue != null) {
            long val = Utils.parseTimeInterval(httpKeepaliveTimeoutValue, SECONDS);
            if (val > 0) {
                httpKeepaliveTimeout = (int) val;
            }
        }
        return httpKeepaliveTimeout;
    }

    static Integer getHttpKeepaliveConnections(String connectionsValue) {
        Integer maxConnections = null;
        if (connectionsValue != null) {
            int val = Integer.parseInt(connectionsValue);
            if (val > 0) {
                maxConnections = val;
            } else {
                String msg = String.format("http.keepalive.connections = %s must be > 0", connectionsValue);
                throw new IllegalArgumentException(msg);
            }
        }

        return maxConnections;
    }

    static boolean isHttpKeepaliveEnabled(String httpKeepaliveEnabledValue) {
        boolean httpKeepaliveEnabled = true;
        if (httpKeepaliveEnabledValue != null) {
            httpKeepaliveEnabled = !httpKeepaliveEnabledValue.equalsIgnoreCase("disabled");
        }

        return httpKeepaliveEnabled;
    }



    static WebSocketWireProtocol getWebSocketWireProtocol(String wsVersion) {
        WebSocketWireProtocol protocol = WebSocketWireProtocol.RFC_6455;
        if ("draft-75".equals(wsVersion)) {
            protocol = WebSocketWireProtocol.HIXIE_75;
        }
        return protocol;
    }

    static Map<String, String> parseOptions(Node parent) {
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
                String localName = node.getLocalName();
                // convert options like tls.ciphers converted to ssl.ciphers
                if (localName.contains("tls")) {
                    localName = localName.replace("tls", "ssl");
                }
                // convert options like pipe.transport=socks+tls://foo to pipe.transport=socks+ssl://foo
                if (localName.contains(".transport") && nodeValue.contains("tls://")) {
                    nodeValue = nodeValue.replace("tls://", "ssl://");
                }
                optionsMap.put(localName, nodeValue);
            }
        }
        return optionsMap;
    }
}
