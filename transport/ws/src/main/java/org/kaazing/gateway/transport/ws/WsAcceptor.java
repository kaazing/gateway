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
package org.kaazing.gateway.transport.ws;

import static java.lang.String.format;
import static org.kaazing.gateway.resource.address.ResourceAddress.ALTERNATE;
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static org.kaazing.gateway.util.InternalSystemProperty.WS_ENABLED_TRANSPORTS;
import static org.kaazing.mina.core.future.DefaultUnbindFuture.combineFutures;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.uri.URIUtils;
import org.kaazing.gateway.transport.BridgeAcceptor;
import org.kaazing.gateway.transport.BridgeSessionInitializer;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtensionFactory;
import org.kaazing.mina.core.future.DefaultUnbindFuture;
import org.kaazing.mina.core.future.UnbindFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dispatching bind/unbind acceptor that explodes the "ws" scheme into "wsn", "wseb" schemes
 * and binds a configurable subset of them (all are bound by default, overridable
 * using the system property {@link WsSystemProperty#WS_ENABLED_TRANSPORTS}.
 */
public class WsAcceptor implements BridgeAcceptor {

    public static final String CLOSE_FILTER = "wsn#close";

    private static final String WSE_PROTOCOL_NAME = "wse/1.0";
    private static final String WS_DRAFT_PROTOCOL_NAME = "ws/draft-7x";
    private static final String WS_NATIVE_PROTOCOL_NAME = "ws/rfc6455";

    // handler for /;api endpoint requests
    public static final IoHandler API_PATH_HANDLER = new IoHandlerAdapter<IoSession>() {};

    private static Logger logger = LoggerFactory.getLogger(WsAcceptor.class);

    private final WebSocketExtensionFactory extensionFactory;

    private Map<String, BridgeAcceptor> wsBridgeAcceptorMap;

    private BridgeAcceptor wsebAcceptor;
    private BridgeAcceptor wsnAcceptor;
    private Properties configuration;

    @Resource(name = "configuration")
    public void setConfiguration(Properties configuration) {
        this.configuration = configuration;
    }

    @Resource(name = "wseb.acceptor")
    public void setWsebAcceptor(BridgeAcceptor wsebAcceptor) {
        this.wsebAcceptor = wsebAcceptor;
    }

    @Resource(name = "wsn.acceptor")
    public void setWsnAcceptor(BridgeAcceptor wsnAcceptor) {
        this.wsnAcceptor = wsnAcceptor;
    }

    public WsAcceptor(WebSocketExtensionFactory extensionFactory) {
        this.extensionFactory = extensionFactory;
    }

    @Override
    public void bind(ResourceAddress address,
                     IoHandler handler,
                     BridgeSessionInitializer<? extends IoFuture> initializer) {

        // bind only address with matching scheme
        URI location = address.getResource();
        String schemeName = location.getScheme();
        if (!canBind(schemeName)) {
            throw new IllegalArgumentException(format("Unexpected scheme \"%s\" for URI: %s", schemeName, location));
        }

        // note: ignore BIND_ALTERNATE (used by delegate acceptors)
        do {
            BridgeAcceptor bridgeAcceptor = selectWsAcceptor(address);
            if ( bridgeAcceptor != null ) {
                bridgeAcceptor.bind(address, handler, initializer);
            } else {
                if (logger.isDebugEnabled()) {
                    String format = "No bridge acceptor found for address '%s'.  Is the %s transport enabled?";
                    logger.warn(format(format, address.getExternalURI(), URIUtils.getScheme(address.getExternalURI())));
                }
            }
            address = address.getOption(ALTERNATE);
        } while (address != null);

    }

    protected boolean canBind(String transportName) {
        return "ws".equals(transportName);
    }

    private BridgeAcceptor selectWsAcceptor(ResourceAddress bindAddress) {
        if (wsBridgeAcceptorMap == null) {
            initWsBridgeAcceptorMap();
        }

        if ( bindAddress.getTransport() == null ) {
            throw new RuntimeException(format("Cannot select a WebSocket acceptor for address '%s'.", bindAddress));
        }

        String nextProtocol = bindAddress.getTransport().getOption(NEXT_PROTOCOL);
        if (nextProtocol == null || nextProtocol.isEmpty()) {
            throw new RuntimeException(format("Cannot find a transport nextProtocol for address '%s'.", bindAddress));
        }
        return wsBridgeAcceptorMap.get(nextProtocol);
    }



    private void initWsBridgeAcceptorMap() {
        wsBridgeAcceptorMap = new HashMap<>();
        Set<EnabledWsTransport> enabledTransports  = resolveEnabledTransports();

        if ( enabledTransports.contains(EnabledWsTransport.wseb)) {
            wsBridgeAcceptorMap.put(WSE_PROTOCOL_NAME, wsebAcceptor);
        }
        if ( enabledTransports.contains(EnabledWsTransport.wsn)) {
            wsBridgeAcceptorMap.put(WS_DRAFT_PROTOCOL_NAME, wsnAcceptor);
            wsBridgeAcceptorMap.put(WS_NATIVE_PROTOCOL_NAME, wsnAcceptor);
        }
    }

    @Override
    public UnbindFuture unbind(ResourceAddress address) {
        UnbindFuture future = DefaultUnbindFuture.succeededFuture();

        // note: ignore BIND_ALTERNATE (used by delegate acceptors)
        do {
            BridgeAcceptor bridgeAcceptor = selectWsAcceptor(address);
            if ( bridgeAcceptor == null ) {
                if (logger.isTraceEnabled()) {
                    logger.trace(format("The acceptor for %s is not enabled.", address.getExternalURI()));
                }
            } else {
                future = combineFutures(future, bridgeAcceptor.unbind(address));
            }
            address = address.getOption(ALTERNATE);
        } while (address != null);

        return future;
    }

    @Override
    public IoHandler getHandler(ResourceAddress address) {
        return null;
    }


    @Override
    public void dispose() {
    }

    public WebSocketExtensionFactory getWebSocketExtensionFactory() {
        return extensionFactory;
    }

    //
    // Code for enabling a subset of these transports.
    //

    /**
     * A tiny enum for capturing the possible enabled WS transports
     */
    enum EnabledWsTransport {
        wsn,
        wseb
    }


    /**
     * Set of WS transports enabled by default.
     */
    static Set<EnabledWsTransport> DEFAULT_ENABLED_WS_TRANSPORTS =
            new HashSet<>(Arrays.asList(EnabledWsTransport.values()));

    /**
     * Which WS transports are enabled?
     * @return a set of enabled ws transports or all available ones if none are explicitly specified
     * @throws IllegalArgumentException when invalid values are configured.
     *
     * (see KG-7250 for rules for defaulting, {@link WsSystemProperty#WS_ENABLED_TRANSPORTS}).
     */
    Set<EnabledWsTransport> resolveEnabledTransports() {
        String enabledTransportsStr = configuration.getProperty(WS_ENABLED_TRANSPORTS.getPropertyName());
        Set<EnabledWsTransport> enabledTransports = new HashSet<>();

        if ( enabledTransportsStr == null ) {
            return DEFAULT_ENABLED_WS_TRANSPORTS;
        }

        String[] inputTransports = enabledTransportsStr.split(",");
        if ( inputTransports == null || inputTransports.length == 0 ) {
            throw new IllegalArgumentException(String.format("No values provided for property '%s'. ",WS_ENABLED_TRANSPORTS.toString()));
        }

        boolean seenAValue = false;
        for ( String inputTransport: inputTransports ) {
            String s = inputTransport.trim();
            if ( s != null && s.length() > 0 ) {
                EnabledWsTransport wsTransport;
                try {
                    wsTransport = EnabledWsTransport.valueOf(s);
                    enabledTransports.add(wsTransport);
                    seenAValue = true;
                } catch (IllegalArgumentException cause ){
                    IllegalArgumentException x = new IllegalArgumentException(String.format("Invalid value '%s' for property '%s'.", s, WS_ENABLED_TRANSPORTS.getPropertyName()));
                    x.initCause(cause);
                    throw x;
                }
            }
        }
        if ( !seenAValue ) {
            throw new IllegalArgumentException(String.format("No valid values provided for property '%s'.", WS_ENABLED_TRANSPORTS.getPropertyName()));
        }

        return enabledTransports;

    }




}
