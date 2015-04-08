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

import static org.kaazing.gateway.service.TransportOptionNames.INACTIVITY_TIMEOUT;
import static org.kaazing.gateway.service.TransportOptionNames.PIPE_TRANSPORT;
import static org.kaazing.gateway.service.TransportOptionNames.SSL_CIPHERS;
import static org.kaazing.gateway.service.TransportOptionNames.SSL_ENCRYPTION_ENABLED;
import static org.kaazing.gateway.service.TransportOptionNames.SSL_PROTOCOLS;
import static org.kaazing.gateway.service.TransportOptionNames.SSL_TRANSPORT;
import static org.kaazing.gateway.service.TransportOptionNames.TCP_TRANSPORT;
import static org.kaazing.gateway.service.TransportOptionNames.WS_PROTOCOL_VERSION;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.kaazing.gateway.server.config.sep2014.ServiceConnectOptionsType;
import org.kaazing.gateway.service.ConnectOptionsContext;
import org.kaazing.gateway.util.Utils;
import org.kaazing.gateway.util.ssl.SslCipherSuites;
import org.kaazing.gateway.util.ws.WebSocketWireProtocol;
import static org.kaazing.gateway.service.TransportOptionNames.*;

public class DefaultConnectOptionsContext implements ConnectOptionsContext {

    private static final long DEFAULT_WS_INACTIVITY_TIMEOUT_MILLIS = 0L;
    private static int DEFAULT_HTTP_KEEPALIVE_TIMEOUT = 30; //seconds

    private WebSocketWireProtocol webSocketWireProtocol = WebSocketWireProtocol.RFC_6455;
    private String[] sslCiphers;
    private String[] sslProtocols;
    private long wsInactivityTimeoutMillis;
    private String wsVersion; // added so we can expose to Console
    private URI pipeTransportURI;
    private URI tcpTransportURI;
    private URI sslTransportURI;
    private URI httpTransportURI;
    private boolean sslEncryptionEnabled = true; // default to true
    private final boolean httpKeepaliveEnabled;
    private final int httpKeepaliveTimeout;

    private String udpInterface;

    public DefaultConnectOptionsContext() {
        this(ServiceConnectOptionsType.Factory.newInstance(), ServiceConnectOptionsType.Factory.newInstance());
    }

    public DefaultConnectOptionsContext(ServiceConnectOptionsType connectOptions, ServiceConnectOptionsType defaultOptions) {
        Long tmpHttpKeepaliveTimeout = null;
        Boolean tmpHttpKeepaliveEnabled = null;
        if (connectOptions != null) {

            wsVersion = connectOptions.getWsVersion();
            if ("rfc6455".equals(wsVersion)) {
                webSocketWireProtocol = WebSocketWireProtocol.RFC_6455;
            } else if ("draft-75".equals(wsVersion)) {
                webSocketWireProtocol = WebSocketWireProtocol.HIXIE_75;
            }

            udpInterface = connectOptions.getUdpInterface();

            String sslCiphersStr = connectOptions.getSslCiphers();
            this.sslCiphers = sslCiphersStr != null ? SslCipherSuites.resolveCSV(sslCiphersStr) : null;
            String sslProtocolsStr = connectOptions.getSslProtocols();
            this.sslProtocols = sslProtocolsStr != null ? resolveProtocols(sslProtocolsStr) : null;

            String tcpTransport = connectOptions.getTcpTransport();
            if (tcpTransport != null) {
                tcpTransportURI = URI.create(tcpTransport);
                if (!tcpTransportURI.isAbsolute()) {
                    throw new IllegalArgumentException(String.format("tcp.transport must contain an absolute URI, not \"%s\"",
                            tcpTransport));
                }
            }

            String pipeTransport = connectOptions.getPipeTransport();
            if (pipeTransport != null) {
                pipeTransportURI = URI.create(pipeTransport);
                if (!pipeTransportURI.isAbsolute()) {
                    throw new IllegalArgumentException(String.format("pipe.transport must contain an absolute URI, not \"%s\"",
                            pipeTransport));
                }
            }

            String sslTransport = connectOptions.getSslTransport();
            if (sslTransport != null) {
                sslTransportURI = URI.create(sslTransport);
                if (!sslTransportURI.isAbsolute()) {
                    throw new IllegalArgumentException(String.format("ssl.transport must contain an absolute URI, not \"%s\"",
                            sslTransport));
                }
            }

            String httpTransport = connectOptions.getHttpTransport();
            if (httpTransport != null) {
                httpTransportURI = URI.create(httpTransport);
                if (!httpTransportURI.isAbsolute()) {
                    throw new IllegalArgumentException(String.format("http.transport must contain an absolute URI, not \"%s\"",
                            httpTransport));
                }
            }

            ServiceConnectOptionsType.HttpKeepalive.Enum alive = connectOptions.getHttpKeepalive();
            if (alive != null) {
                tmpHttpKeepaliveEnabled = alive != ServiceConnectOptionsType.HttpKeepalive.DISABLED;
            }

            String timeoutValue = connectOptions.getHttpKeepaliveTimeout();
            if (timeoutValue != null) {
                long val = Utils.parseTimeInterval(timeoutValue, TimeUnit.SECONDS);
                if (val > 0) {
                    tmpHttpKeepaliveTimeout = val;
                }
            }

        }

        // Set default values via service-defaults
        if (defaultOptions != null) {
            if (wsVersion == null) {
                wsVersion = defaultOptions.getWsVersion();
                if ("rfc6455".equals(wsVersion)) {
                    webSocketWireProtocol = WebSocketWireProtocol.RFC_6455;
                } else if ("draft-75".equals(wsVersion)) {
                    webSocketWireProtocol = WebSocketWireProtocol.HIXIE_75;
                }
            }

            if (udpInterface == null) {
                udpInterface = defaultOptions.getUdpInterface();
            }

            if (this.sslCiphers == null) {
                String sslCiphersStr = defaultOptions.getSslCiphers();
                this.sslCiphers = sslCiphersStr != null ? SslCipherSuites.resolveCSV(sslCiphersStr) : null;
            }
            if (this.sslProtocols == null) {
                String sslProtocolsStr = defaultOptions.getSslProtocols();
                this.sslProtocols = sslProtocolsStr != null ? resolveProtocols(sslProtocolsStr) : null;
            }

            if (this.tcpTransportURI == null) {
                String tcpTransport = defaultOptions.getTcpTransport();
                if (tcpTransport != null) {
                    tcpTransportURI = URI.create(tcpTransport);
                    if (!tcpTransportURI.isAbsolute()) {
                        throw new IllegalArgumentException(String.format(
                                "tcp.transport must contain an absolute URI, not \"%s\"", tcpTransport));
                    }
                }
            }

            if (this.pipeTransportURI == null) {
                String pipeTransport = defaultOptions.getPipeTransport();
                if (pipeTransport != null) {
                    pipeTransportURI = URI.create(pipeTransport);
                    if (!pipeTransportURI.isAbsolute()) {
                        throw new IllegalArgumentException(String.format(
                                "pipe.transport must contain an absolute URI, not \"%s\"", pipeTransport));
                    }
                }
            }

            if (this.sslTransportURI == null) {
                String sslTransport = defaultOptions.getSslTransport();
                if (sslTransport != null) {
                    sslTransportURI = URI.create(sslTransport);
                    if (!sslTransportURI.isAbsolute()) {
                        throw new IllegalArgumentException(String.format(
                                "ssl.transport must contain an absolute URI, not \"%s\"", sslTransport));
                    }
                }
            }

            if (this.httpTransportURI == null) {
                String httpTransport = defaultOptions.getHttpTransport();
                if (httpTransport != null) {
                    httpTransportURI = URI.create(httpTransport);
                    if (!httpTransportURI.isAbsolute()) {
                        throw new IllegalArgumentException(String.format(
                                "http.transport must contain an absolute URI, not \"%s\"", httpTransport));
                    }
                }
            }

            if (tmpHttpKeepaliveEnabled == null) {
                ServiceConnectOptionsType.HttpKeepalive.Enum alive = defaultOptions.getHttpKeepalive();
                if (alive != null) {
                    tmpHttpKeepaliveEnabled = alive != ServiceConnectOptionsType.HttpKeepalive.DISABLED;
                }
            }

            if (tmpHttpKeepaliveTimeout == null) {
                String timeoutValue = defaultOptions.getHttpKeepaliveTimeout();
                if (timeoutValue != null) {
                    long val = Utils.parseTimeInterval(timeoutValue, TimeUnit.SECONDS);
                    if (val > 0) {
                        tmpHttpKeepaliveTimeout = val;
                    }
                }
            }
        }

        // Set properties that have default values, needs special logic for ServiceDefaults

        // inactivity timeout
        Long wsInactivityTimeout = null;
        String value = null;
        if (connectOptions != null) {
            value = connectOptions.getWsInactivityTimeout();
            // if null use ServiceDefaults
        }
        if (value == null && defaultOptions != null) {
            value = defaultOptions.getWsInactivityTimeout();
        }
        if (value != null) {
            long val = Utils.parseTimeInterval(value, TimeUnit.MILLISECONDS);
            if (val > 0) {
                wsInactivityTimeout = val;
            }
        }

        this.wsInactivityTimeoutMillis =
                (wsInactivityTimeout == null) ? DEFAULT_WS_INACTIVITY_TIMEOUT_MILLIS : wsInactivityTimeout;

        // ssl encryption enabled
        Boolean sslEncryptionEnabled = null;
        ServiceConnectOptionsType.SslEncryption.Enum encrypted = null;
        if (connectOptions != null) {
            encrypted = connectOptions.getSslEncryption();
        }
        // if null use ServiceDefaults
        if (encrypted == null && defaultOptions != null) {
            encrypted = defaultOptions.getSslEncryption();
        }
        if (encrypted != null) {
            sslEncryptionEnabled = encrypted != ServiceConnectOptionsType.SslEncryption.DISABLED;
        }

        this.sslEncryptionEnabled = (sslEncryptionEnabled == null) ? true : sslEncryptionEnabled;

        this.httpKeepaliveTimeout = (tmpHttpKeepaliveTimeout == null)
                ? DEFAULT_HTTP_KEEPALIVE_TIMEOUT : tmpHttpKeepaliveTimeout.intValue();
        this.httpKeepaliveEnabled = (tmpHttpKeepaliveEnabled == null) ? true : tmpHttpKeepaliveEnabled;
    }

    @Override
    public WebSocketWireProtocol getWebSocketWireProtocol() {
        return webSocketWireProtocol;
    }

    @Override
    public String[] getSslCiphers() {
        return sslCiphers;
    }

    @Override
    public String[] getSslProtocols() {
        return sslProtocols;
    }

    @Override
    public String getWsVersion() {
        return wsVersion;
    }

    @Override
    public URI getPipeTransport() {
        return pipeTransportURI;
    }

    @Override
    public URI getTcpTransport() {
        return tcpTransportURI;
    }

    @Override
    public URI getSslTransport() {
        return sslTransportURI;
    }

    @Override
    public URI getHttpTransport() {
        return httpTransportURI;
    }

    @Override
    public long getWsInactivityTimeout() {
        return wsInactivityTimeoutMillis;
    }

    @Override
    public boolean isSslEncryptionEnabled() {
        return sslEncryptionEnabled;
    }

    @Override
    public Integer getHttpKeepaliveTimeout() {
        return httpKeepaliveTimeout;
    }

    @Override
    public boolean isHttpKeepaliveEnabled() {
        return httpKeepaliveEnabled;
    }

    @Override
    public Map<String, Object> asOptionsMap() {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put(SSL_CIPHERS, getSslCiphers());
        result.put(SSL_PROTOCOLS, getSslProtocols());
        result.put(WS_PROTOCOL_VERSION, webSocketWireProtocol);
        result.put(INACTIVITY_TIMEOUT, getWsInactivityTimeout());

        result.put(PIPE_TRANSPORT, getPipeTransport());
        result.put(TCP_TRANSPORT, getTcpTransport());
        result.put(SSL_TRANSPORT, getSslTransport());
        result.put("http[http/1.1].transport", getHttpTransport());

        result.put(SSL_ENCRYPTION_ENABLED, isSslEncryptionEnabled());
        result.put("udp.interface", getUdpInterface());
        result.put(HTTP_KEEP_ALIVE_TIMEOUT_KEY, getHttpKeepaliveTimeout());
        result.put(HTTP_KEEP_ALIVE, isHttpKeepaliveEnabled());

        return result;
    }

    public String getUdpInterface() {
        return udpInterface;
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

}
