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

import org.kaazing.gateway.server.config.june2016.ServiceConnectOptionsType;
import org.kaazing.gateway.service.ConnectOptionsContext;
import org.kaazing.gateway.util.ws.WebSocketWireProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kaazing.gateway.service.TransportOptionNames.PIPE_TRANSPORT;
import static org.kaazing.gateway.service.TransportOptionNames.SSL_CIPHERS;
import static org.kaazing.gateway.service.TransportOptionNames.SSL_ENCRYPTION_ENABLED;
import static org.kaazing.gateway.service.TransportOptionNames.SSL_NEED_CLIENT_AUTH;
import static org.kaazing.gateway.service.TransportOptionNames.SSL_PROTOCOLS;
import static org.kaazing.gateway.service.TransportOptionNames.SSL_TRANSPORT;
import static org.kaazing.gateway.service.TransportOptionNames.SSL_WANT_CLIENT_AUTH;
import static org.kaazing.gateway.service.TransportOptionNames.TCP_TRANSPORT;
import static org.kaazing.gateway.service.TransportOptionNames.WS_PROTOCOL_VERSION;

public class DefaultConnectOptionsContext extends DefaultOptionsContext implements ConnectOptionsContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConnectOptionsContext.class);

    private final Map<String, String> options;  // unmodifiable map

    public DefaultConnectOptionsContext() {
        this.options = Collections.emptyMap();
    }

    public DefaultConnectOptionsContext(ServiceConnectOptionsType connectOptions, ServiceConnectOptionsType defaultOptions) {
        Map<String, String> options = parseConnectOptionsType(connectOptions);
        parseConnectOptionsType(defaultOptions).entrySet()
                .stream()
                .forEach(e -> options.putIfAbsent(e.getKey(), e.getValue()));
        this.options = Collections.unmodifiableMap(options);
    }

    @Override
    public Map<String, Object> asOptionsMap() {
        Map<String, String> optionsCopy = new HashMap<>(options);

        Map<String, Object> result = new LinkedHashMap<>();

        WebSocketWireProtocol wsVersion = getWebSocketWireProtocol(optionsCopy.remove("ws.version"));
        result.put(WS_PROTOCOL_VERSION, wsVersion);

        String wsInactivityTimeoutStr = optionsCopy.remove("ws.inactivity.timeout");
        String httpKeepaliveTimeoutStr = optionsCopy.remove("http.keepalive.timeout");
        // ws.inactivity.timeout is used for http.keepalive.timeout so that connections
        // are kept alive in wse case
        if (wsInactivityTimeoutStr != null && httpKeepaliveTimeoutStr == null) {
            httpKeepaliveTimeoutStr = wsInactivityTimeoutStr;
        }

        long wsInactivityTimeout = getWsInactivityTimeout(wsInactivityTimeoutStr);
        result.put("ws.inactivityTimeout", wsInactivityTimeout);

        int httpKeepaliveTimeout = getHttpKeepaliveTimeout(httpKeepaliveTimeoutStr);
        result.put("http[http/1.1].keepAliveTimeout", httpKeepaliveTimeout);
        if (wsInactivityTimeoutStr != null &&
                MILLISECONDS.convert(httpKeepaliveTimeout, SECONDS) < wsInactivityTimeout) {
            LOGGER.warn("http.keepalive.timeout={} should be greater-than-or-equal-to ws.inactivity.timeout={} in connect-options",
                    httpKeepaliveTimeoutStr, wsInactivityTimeoutStr);
        }

        boolean keepAlive = isHttpKeepaliveEnabled(optionsCopy.remove("http.keepalive"));
        result.put("http[http/1.1].keepAlive", keepAlive);

        Integer keepaliveConnections = getHttpKeepaliveConnections(optionsCopy.remove("http.keepalive.connections"));
        if (keepaliveConnections != null) {
            result.put("http[http/1.1].keepalive.connections", keepaliveConnections);
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

        // for now just put in the rest of the options as strings
        result.putAll(optionsCopy);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(format("Connect options map = %s", result));
        }

        return result;
    }

    private Map<String, String> parseConnectOptionsType(ServiceConnectOptionsType connectOptionsType) {
        return connectOptionsType != null
                ? parseOptions(connectOptionsType.getDomNode())
                : new HashMap<>();
    }

}
