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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.kaazing.gateway.server.config.june2016.ServiceAcceptOptionsType;
import org.kaazing.gateway.service.AcceptOptionsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultAcceptOptionsContext extends DefaultOptionsContext implements AcceptOptionsContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAcceptOptionsContext.class);

    /**
     * The name of the extended handshake protocol to be sent on the wire.
     */
    private static final String EXTENDED_HANDSHAKE_PROTOCOL_NAME = "x-kaazing-handshake";

    private static final List<String> DEFAULT_WEBSOCKET_PROTOCOLS;

    static {
        // Note: including null in these arrays permits us to process no protocols/extensions
        // without throwing an exception.
        DEFAULT_WEBSOCKET_PROTOCOLS = Arrays.asList(EXTENDED_HANDSHAKE_PROTOCOL_NAME, null);
    }

    private final Map<String, String> binds;        // gets modified by balancer service
    private final Map<String, String> options;      // unmodifiable map without bind options like tcp.bind etc

    public DefaultAcceptOptionsContext() {
        this.binds = new HashMap<>();
        this.options = Collections.emptyMap();
    }

    public DefaultAcceptOptionsContext(ServiceAcceptOptionsType acceptOptions, ServiceAcceptOptionsType defaultOptions) {
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

        int wsMaxMessageSize = getWsMaximumMessageSize(optionsCopy.remove("ws.maximum.message.size"));
        result.put("ws.maxMessageSize", wsMaxMessageSize);
        result.put("ws[ws/rfc6455].ws[ws/rfc6455].maxMessageSize", wsMaxMessageSize);
        result.put("ws[ws/draft-7x].ws[ws/draft-7x].maxMessageSize", wsMaxMessageSize);

        int httpKeepaliveTimeout = getHttpKeepaliveTimeout(httpKeepaliveTimeoutStr);
        result.put("http[http/1.1].keepAliveTimeout", httpKeepaliveTimeout);
        if (wsInactivityTimeoutStr != null &&
            MILLISECONDS.convert(httpKeepaliveTimeout, SECONDS) < wsInactivityTimeout) {
            LOGGER.warn("http.keealive.timeout={} should be greater than ws.inactivity.timeout={}",
                    wsInactivityTimeoutStr, httpKeepaliveTimeoutStr);
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

    private Map<String, String> parseAcceptOptionsType(ServiceAcceptOptionsType acceptOptionsType) {
        return acceptOptionsType != null ? parseOptions(acceptOptionsType.getDomNode()) : new HashMap<>();
    }

}
