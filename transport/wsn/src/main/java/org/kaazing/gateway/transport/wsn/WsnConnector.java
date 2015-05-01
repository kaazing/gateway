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

package org.kaazing.gateway.transport.wsn;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.kaazing.gateway.resource.address.ResourceAddress.ALTERNATE;
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static org.kaazing.gateway.transport.wsn.WsnSession.SESSION_KEY;

import java.net.ConnectException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Resource;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.DefaultConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.gateway.resource.address.Protocol;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ws.WsResourceAddress;
import org.kaazing.gateway.transport.AbstractBridgeConnector;
import org.kaazing.gateway.transport.BridgeConnector;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.transport.DefaultIoSessionConfigEx;
import org.kaazing.gateway.transport.DefaultTransportMetadata;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.ObjectLoggingFilter;
import org.kaazing.gateway.transport.TypedAttributeKey;
import org.kaazing.gateway.transport.UpgradeFuture;
import org.kaazing.gateway.transport.http.HttpConnectSession;
import org.kaazing.gateway.transport.http.HttpProtocol;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.bridge.filter.HttpPostUpgradeFilter;
import org.kaazing.gateway.transport.ws.WsAcceptor;
import org.kaazing.gateway.transport.ws.WsBinaryMessage;
import org.kaazing.gateway.transport.ws.WsContinuationMessage;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.WsPingMessage;
import org.kaazing.gateway.transport.ws.WsPongMessage;
import org.kaazing.gateway.transport.ws.WsTextMessage;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBufferAllocator;
import org.kaazing.gateway.transport.ws.bridge.filter.WsCheckAliveFilter;
import org.kaazing.gateway.transport.ws.bridge.filter.WsCodecFilter;
import org.kaazing.gateway.transport.ws.bridge.filter.WsFrameBase64Filter;
import org.kaazing.gateway.transport.ws.bridge.filter.WsFrameTextFilter;
import org.kaazing.gateway.util.Encoding;
import org.kaazing.gateway.util.Utils;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.gateway.util.ws.WebSocketWireProtocol;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WsnConnector extends AbstractBridgeConnector<WsnSession> {

    private static final String POST_UPGRADE_FILTER = WsnProtocol.NAME + "#postUpgrade";
    private static final String CODEC_FILTER = WsnProtocol.NAME + "#codec";
    private static final String BASE64_FILTER = WsnProtocol.NAME + "#base64";

    private static final String TEXT_FILTER = WsnProtocol.NAME + "#text";
    private static final String FAULT_LOGGING_FILTER = WsnProtocol.NAME + "#fault";
    private static final String TRACE_LOGGING_FILTER = WsnProtocol.NAME + "#logging";
    // chosen key from RFC 6455
    private static final String STATIC_WEBSOCKET_KEY = "dGhlIHNhbXBsZSBub25jZQ==";

    private static final TypedAttributeKey<Callable<WsnSession>> WSN_SESSION_FACTORY_KEY = new TypedAttributeKey<>(WsnConnector.class, "wsnSessionFactory");
    private static final AttributeKey ENCODING_KEY = new AttributeKey(WsnConnector.class, "encoding");
    private static final TypedAttributeKey<IoSessionInitializer<?>> WSN_SESSION_INITIALIZER_KEY = new TypedAttributeKey<>(WsnConnector.class, "wsnSessionInitializer");
    private static final TypedAttributeKey<ConnectFuture> WSN_CONNECT_FUTURE_KEY = new TypedAttributeKey<>(WsnConnector.class, "wsnConnectFuture");
    private static final TypedAttributeKey<ResourceAddress> WSN_CONNECT_ADDRESS_KEY = new TypedAttributeKey<>(WsnConnector.class, "wsnConnectAddress");

    private static final String LOGGER_NAME = String.format("transport.%s.connect", WsnProtocol.NAME);

	private final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    private final HttpPostUpgradeFilter postUpgrade;
    private final WsCodecFilter codec;
    private final WsFrameBase64Filter base64;
    private final WsFrameTextFilter text;

    private BridgeServiceFactory bridgeServiceFactory;
    private ResourceAddressFactory resourceAddressFactory;
    private Properties configuration = new Properties();
    private ScheduledExecutorService scheduler;

    public WsnConnector() {
        super(new DefaultIoSessionConfigEx());

        postUpgrade = new HttpPostUpgradeFilter();

        codec = new WsCodecFilter(0, true);
        base64 = new WsFrameBase64Filter();
        text = new WsFrameTextFilter();
    }

    @Resource(name = "bridgeServiceFactory")
    public void setBridgeServiceFactory(BridgeServiceFactory bridgeServiceFactory) {
        this.bridgeServiceFactory = bridgeServiceFactory;
    }

    @Resource(name = "configuration")
    public void setConfiguration(Properties configuration) {
        this.configuration = configuration;
    }

    @Resource(name = "schedulerProvider")
    public void setSchedulerProvider(SchedulerProvider provider) {
        this.scheduler = provider.getScheduler("Scheduler-wsn", false);
    }

    @Resource(name = "resourceAddressFactory")
    public void setResourceAddressFactory(ResourceAddressFactory factory) {
        this.resourceAddressFactory = factory;
    }


    @Override
    public TransportMetadata getTransportMetadata() {
        return new DefaultTransportMetadata(WsnProtocol.NAME);
    }

    @Override
    public void addBridgeFilters(IoFilterChain filterChain) {
        // setup logging filters for bridge session
        if (logger.isTraceEnabled()) {
            filterChain.addFirst(TRACE_LOGGING_FILTER, new ObjectLoggingFilter(logger, WsnProtocol.NAME + "#%s"));
        } else if (logger.isDebugEnabled()) {
            filterChain.addFirst(FAULT_LOGGING_FILTER, new ObjectLoggingFilter(logger, WsnProtocol.NAME + "#%s"));
        }

        IoSession session = filterChain.getSession();
        Encoding encoding = (Encoding) session.getAttribute(ENCODING_KEY);

        switch (encoding) {
        case BASE64:
            // add framing before encoding
            filterChain.addLast(CODEC_FILTER, codec);
            filterChain.addLast(BASE64_FILTER, base64);
            break;
        case TEXT:
            // add framing before encoding
            filterChain.addLast(CODEC_FILTER, codec);
            filterChain.addLast(TEXT_FILTER, text);
            break;
        default:
            filterChain.addLast(CODEC_FILTER, codec);
            break;
        }

        // We speak a new enough version of the WebSocket protocol that
        // we need to conform to the proper CLOSE semantics.
        filterChain.addLast(WsAcceptor.CLOSE_FILTER, new WsCloseFilter(WebSocketWireProtocol.RFC_6455, configuration, logger, scheduler));

        // post-upgrade filter is added before WebSocket codec filter
        // where the type of this filter chain is still IoBuffer
        filterChain.addBefore(CODEC_FILTER, POST_UPGRADE_FILTER, postUpgrade);

        // (KG-7391) Use ping and pong to detect and close dead connections, if ws inactivity timeout is active
        final ResourceAddress connectAddress = (ResourceAddress) session.removeAttribute(WSN_CONNECT_ADDRESS_KEY);
        WsCheckAliveFilter.addIfFeatureEnabled(filterChain, WsnAcceptor.CHECK_ALIVE_FILTER,
                connectAddress.getOption(WsResourceAddress.INACTIVITY_TIMEOUT), logger);
    }

    @Override
    public void removeBridgeFilters(IoFilterChain filterChain) {
        removeFilter(filterChain, postUpgrade);
        removeFilter(filterChain, codec);
        removeFilter(filterChain, base64);
        removeFilter(filterChain, text);
        removeFilter(filterChain, WsAcceptor.CLOSE_FILTER);
    }

    @Override
    protected IoProcessorEx<WsnSession> initProcessor() {
        return new WsnAcceptProcessor();
    }

    @Override
    protected boolean canConnect(String transportName) {
        return transportName.equals("wsn") || transportName.equals("ws");
    }

    @Override
    protected <T extends ConnectFuture> ConnectFuture connectInternal(final ResourceAddress connectAddress,
                                                                      final IoHandler handler,
                                                                      final IoSessionInitializer<T> initializer) {

        final ConnectFuture connectFuture = new DefaultConnectFuture();

        final ConnectFuture wsnConnectFuture = wsnConnectInternal(connectAddress, handler, initializer);

        IoFutureListener<ConnectFuture> wsnConnectListener = new IoFutureListener<ConnectFuture>() {
            @Override
            public void operationComplete(ConnectFuture wsnFuture) {
                // fallback to "first alternate address == wse" if wsn connection fails
                if (!wsnFuture.isConnected()) {

                    IoFutureListener<ConnectFuture> fallbackConnectListener = new IoFutureListener<ConnectFuture>() {
                        @Override
                        public void operationComplete(ConnectFuture fallbackFuture) {
                            // fail wsn connect future if fallback connect fails
                            if (!fallbackFuture.isConnected()) {
                                connectFuture.setException(fallbackFuture.getException());
                            } else {
                                connectFuture.setSession(fallbackFuture.getSession());
                            }
                        }
                    };

                    ResourceAddress fallbackAddress = connectAddress.getOption(ALTERNATE);
                    if ( fallbackAddress != null && fallbackAddress.getResource().getScheme().equals("wse")) {
                        BridgeConnector fallbackConnector  = bridgeServiceFactory.newBridgeConnector(fallbackAddress);

                        if ( fallbackConnector != null ) {
                            fallbackConnector.connect(fallbackAddress, handler, initializer).
                                    addListener(fallbackConnectListener);
                        } else {
                            connectFuture.setException(new ConnectException("No fallback connector available."));
                        }
                    } else {
                        connectFuture.setException(new ConnectException("No fallback address available."));
                    }

                } else {
                    connectFuture.setSession(wsnConnectFuture.getSession());
                }
            }
        };

        wsnConnectFuture.addListener(wsnConnectListener);
        return connectFuture;
    }

    protected <T extends ConnectFuture> ConnectFuture wsnConnectInternal(ResourceAddress connectAddress,
                                                                         IoHandler handler,
                                                                         final IoSessionInitializer<T> initializer
                                                                         // TODO: connect options context??
    ) {
        final DefaultConnectFuture wsnConnectFuture = new DefaultConnectFuture();

        // propagate connection failure, if necessary
        IoFutureListener<ConnectFuture> parentConnectListener = new IoFutureListener<ConnectFuture>() {
            @Override
            public void operationComplete(ConnectFuture future) {
                // fail bridge connect future if parent connect fails
                if ( !future.isConnected() ) {
                    wsnConnectFuture.setException(future.getException());
                }
                else {
                    // (KG-5691) fail bridge connect future if we get an unexpected HTTP response code
                    // (like 404 not found)
                    IoSession httpSession = future.getSession();
                    httpSession.getCloseFuture().addListener(new IoFutureListener<CloseFuture>() {
                        @Override
                        public void operationComplete(CloseFuture future) {
                            HttpConnectSession httpSession = (HttpConnectSession) future.getSession();
                            HttpStatus httpStatus = httpSession.getStatus();
                            switch (httpStatus) {
                            case INFO_SWITCHING_PROTOCOLS:
                                doUpgrade(httpSession);
                                break;
                            default:
                                Throwable exception = new ConnectException(String.format("Unexpected HTTP response %d - %s",
                                        httpStatus.code(), httpStatus.reason())).fillInStackTrace();
                                if (logger.isDebugEnabled()) {
                                    logger.debug("WsnConnector: failing connection with exception " + exception);
                                }
                                wsnConnectFuture.setException(exception);
                                break;
                            }
                        }

                        private void doUpgrade(final HttpConnectSession httpSession) {
                            final IoSessionInitializer<? extends IoFuture> wsnSessionInitializer = WSN_SESSION_INITIALIZER_KEY.remove(httpSession);
                            final ConnectFuture wsnConnectFuture = WSN_CONNECT_FUTURE_KEY.get(httpSession);
                            final ResourceAddress wsnConnectAddress = WSN_CONNECT_ADDRESS_KEY.remove(httpSession);


                            UpgradeFuture upgrade = httpSession.upgrade(ioBridgeHandler);
                            upgrade.addListener(new IoFutureListener<UpgradeFuture>() {
                                @Override
                                public void operationComplete(UpgradeFuture future) {
                                    final IoSessionEx parent = (IoSessionEx) future.getSession();

                                    // factory to create a new bridge session
                                    final Callable<WsnSession> createSession = new Callable<WsnSession>() {
                                        @Override
                                        public WsnSession call() throws Exception {

                                            Callable<WsnSession> wsnSessionFactory = new Callable<WsnSession>() {
                                                @Override
                                                public WsnSession call() throws Exception {
                                                    final ResourceAddress localAddress =
                                                            resourceAddressFactory.newResourceAddress(wsnConnectAddress,
                                                                                                      BridgeSession.LOCAL_ADDRESS.get(httpSession));
                                                    IoBufferAllocatorEx<?> parentAllocator = parent.getBufferAllocator();
                                                    WsBufferAllocator wsAllocator = new WsBufferAllocator(parentAllocator);
                                                    return new WsnSession(WsnConnector.this, getProcessor(), localAddress, wsnConnectAddress,
                                                            parent, wsAllocator, httpSession.getRequestURI(), null, null, WebSocketWireProtocol.RFC_6455);
                                                }
                                            };

                                            return newSession(wsnSessionInitializer, wsnConnectFuture, wsnSessionFactory);
                                        }
                                    };

                                    String wsAcceptHeader = httpSession.getReadHeader("Sec-WebSocket-Accept");
                                    // fail wsn connection if accept header is not correct
                                    // TODO check hash
                                    if (wsAcceptHeader == null) {
                                        logger.info("WebSocket connection failed: missing Sec-WebSocket-Accept response header, does not comply with RFC 6455 - use connect options or another protocol");
                                        wsnConnectFuture.setException(new Exception("WebSocket Upgrade Failed"));
                                        return;
                                    }

                                    String frameType = httpSession.getReadHeader("X-Frame-Type");
                                    if ("binary".equals(frameType)) {
                                        parent.setAttribute(ENCODING_KEY, Encoding.BINARY);
                                    } else if ("base64".equals(frameType)) {
                                        parent.setAttribute(ENCODING_KEY, Encoding.BASE64);
                                    } else {
                                        parent.setAttribute(ENCODING_KEY, Encoding.TEXT);
                                    }

                                    WSN_SESSION_FACTORY_KEY.set(parent, createSession);
                                    parent.setAttribute(WSN_CONNECT_ADDRESS_KEY, wsnConnectAddress);
                                }
                            });
                        }
                    });
                }
            }
        };

        IoSessionInitializer<ConnectFuture> parentInitializer =
                createParentInitializer(connectAddress,
                                        handler,
                                        initializer,
                                        wsnConnectFuture);

        final ResourceAddress createAddress = connectAddress.getTransport();
        BridgeConnector connector = bridgeServiceFactory.newBridgeConnector(createAddress);
        connector.connect(createAddress, selectConnectHandler(createAddress),
                          parentInitializer).addListener(parentConnectListener);

        return wsnConnectFuture;
    }

    private IoHandler selectConnectHandler(ResourceAddress address) {
        Protocol protocol = bridgeServiceFactory.getTransportFactory().getProtocol(address.getResource());
        if ( protocol instanceof HttpProtocol) {
            return httpBridgeHandler;
        }
        throw new RuntimeException(getClass()+
                ": Cannot select a connect handler for address "+address);
    }

    private <T extends ConnectFuture> IoSessionInitializer<ConnectFuture> createParentInitializer(
            final ResourceAddress wsnConnectAddress, final IoHandler handler, final IoSessionInitializer<T> initializer,
            final DefaultConnectFuture wsnConnectFuture) {

        ResourceAddress httpConnectAddress = wsnConnectAddress.getTransport();
        Protocol protocol = bridgeServiceFactory.getTransportFactory().getProtocol(httpConnectAddress.getResource());
        if (!(protocol instanceof HttpProtocol)) {
            String message = format("Cannot create WSN parent session initializer for address %s", wsnConnectAddress);
            if (logger.isInfoEnabled()) {
                logger.info(message);
            }
            throw new RuntimeException(message);
        }

        // initialize parent session before connection attempt
        return new IoSessionInitializer<ConnectFuture>() {
            @Override
            public void initializeSession(final IoSession parent, ConnectFuture future) {
                // initializer for bridge session to specify bridge handler,
                // and call user-defined bridge session initializer if present
                final IoSessionInitializer<T> wsnSessionInitializer = new IoSessionInitializer<T>() {
                    @Override
                    public void initializeSession(IoSession session, T future) {
                        WsnSession wsnSession = (WsnSession) session;
                        wsnSession.setHandler(handler);

                        if (initializer != null) {
                            initializer.initializeSession(session, future);
                        }
                    }
                };

                // emulation sends same-origin header
                // TODO: determine appropriate HTML5 Origin header for desktop
                // clients
                URI resource = wsnConnectAddress.getExternalURI();
                Protocol protocol = bridgeServiceFactory.getTransportFactory().getProtocol(resource.getScheme());
                String wsScheme = protocol.isSecure() ? "https" : "http";
                String origin = wsScheme + "://" + resource.getAuthority();

                // note: WebSocket Version 13 upgrades to "websocket" (not "WebSocket")
                // see http://tools.ietf.org/html/draft-ietf-hybi-thewebsocketprotocol-13
                HttpConnectSession httpSession = (HttpConnectSession) parent;
                httpSession.setWriteHeader("Upgrade", "websocket");
                httpSession.setWriteHeader("Connection", "Upgrade");
                httpSession.setWriteHeader("Origin", origin);
                httpSession.setWriteHeader("Sec-WebSocket-Version", "13");
                httpSession.setWriteHeader("Sec-WebSocket-Key", STATIC_WEBSOCKET_KEY); // TODO use random key?
                
                List<String> protocols = asList(wsnConnectAddress.getOption(WsResourceAddress.SUPPORTED_PROTOCOLS));

                // include next-protocol (if specified) with supported protocols
                String wsNextProtocol = wsnConnectAddress.getOption(NEXT_PROTOCOL);
                if (wsNextProtocol != null) {
                    List<String> allProtocols = new ArrayList<>(protocols);
                    allProtocols.add(wsNextProtocol);
                    protocols = allProtocols;
                }
                
                if (!protocols.isEmpty()) {
                    httpSession.setWriteHeader("Sec-WebSocket-Protocol", Utils.asCommaSeparatedString(protocols));
                }
                WSN_SESSION_INITIALIZER_KEY.set(httpSession, wsnSessionInitializer);
                WSN_CONNECT_FUTURE_KEY.set(httpSession, wsnConnectFuture);
                WSN_CONNECT_ADDRESS_KEY.set(httpSession, wsnConnectAddress);
            }
        };
    }

    private IoHandler ioBridgeHandler = new IoHandlerAdapter<IoSessionEx>() {

        @Override
        protected void doSessionOpened(IoSessionEx session) throws Exception {
            IoFilterChain filterChain = session.getFilterChain();
            addBridgeFilters(filterChain);

            // defer creation of WsnSession until WebSocket handshake completed
            Callable<WsnSession> sessionFactory = WSN_SESSION_FACTORY_KEY.remove(session);
            WsnSession wsnSession = sessionFactory.call();

            SESSION_KEY.set(session, wsnSession);
        }

        @Override
        protected void doMessageReceived(IoSessionEx session, Object message) throws Exception {
            WsnSession wsnSession = SESSION_KEY.get(session);
            IoFilterChain filterChain = wsnSession.getFilterChain();

            WsMessage wsMessage = (WsMessage) message;
            switch (wsMessage.getKind()) {
            case CONTINUATION:
                WsContinuationMessage wsCont = (WsContinuationMessage) wsMessage;
                filterChain.fireMessageReceived(wsCont.getBytes());
                break;
            case TEXT:
                WsTextMessage wsText = (WsTextMessage) wsMessage;
                filterChain.fireMessageReceived(wsText.getBytes());
                break;
            case BINARY:
                WsBinaryMessage wsBinary = (WsBinaryMessage) wsMessage;
                filterChain.fireMessageReceived(wsBinary.getBytes());
                break;
            case PING:
                // Manage the WebSocket control message here; do not
                // propagate control messages to application-space.

                // Respond with a PONG directly here.
                WsPingMessage ping = (WsPingMessage) wsMessage;
                IoBufferEx payload = ping.getBytes();
                WsPongMessage pong = new WsPongMessage(payload);
                session.write(pong);
                break;
            case PONG:
                // Manage the WebSocket control message here; do not
                // propagate control messages to application-space.

                // ignore pongs to clients - not required.
                break;
            case CLOSE:
                // CLOSE frames should be handled by the WsCloseFilter.
                // However, we need to make sure that we do not choke if we
                // receive one.  (See KG-6745.)
                break;
            default:
                throw new IllegalArgumentException("Unrecognized message kind: " + wsMessage.getKind());
            }
        }

        @Override
        protected void doExceptionCaught(IoSessionEx session, Throwable cause) throws Exception {
            WsnSession wsnSession = SESSION_KEY.get(session);
            if (wsnSession != null && !wsnSession.isClosing()) {
                wsnSession.reset(cause);
            }
            else {
                ConnectFuture wsnConnectFuture = WSN_CONNECT_FUTURE_KEY.remove(session);
                if (wsnConnectFuture != null) {
                    wsnConnectFuture.setException(cause);
                }
                else {
                    if (logger.isDebugEnabled()) {
                        String message = format("Error on WebSocket connection attempt: %s", cause);
                        if (logger.isTraceEnabled()) {
                            // note: still debug level, but with extra detail about the exception
                            logger.debug(message, cause);
                        }
                        else {
                            logger.debug(message);
                        }
                    }
                }
                session.close(true);
            }
        }

        /*
         * error here when we get message sent for http session durring upgrade
         *
         * @Override protected void doMessageSent(IoSession session, Object message) throws Exception { IoFilterChain filterChain
         * = getSessionFilterChain(session); filterChain.fireMessageSent(new DefaultWriteRequest(message)); }
         */

        @Override
        protected void doSessionClosed(IoSessionEx session) throws Exception {
            WsnSession wsnSession = SESSION_KEY.get(session);
            if (wsnSession != null && !wsnSession.isClosing()) {
                // TODO: require WebSocket controlled close handshake
                wsnSession.reset(new Exception("Early termination of IO session").fillInStackTrace());
            }
        }

        @Override
        protected void doSessionIdle(IoSessionEx session, IdleStatus status) throws Exception {
            WsnSession wsnSession = SESSION_KEY.get(session);
            IoFilterChain filterChain = wsnSession.getFilterChain();
            filterChain.fireSessionIdle(status);
        }

        @Override
        protected void doSessionCreated(IoSessionEx session) throws Exception {
            // TODO: might need to move code in open to here
        }
    };

    private IoHandler httpBridgeHandler = new IoHandlerAdapter<HttpConnectSession>() {
        @Override
        protected void doSessionClosed(HttpConnectSession httpSession) throws Exception {
            // if WebSocket handshake incomplete, fail the WsnSession connect future
            ConnectFuture wsnConnectFuture = WSN_CONNECT_FUTURE_KEY.get(httpSession);
            assert (wsnConnectFuture != null);
            if (!wsnConnectFuture.isDone() && httpSession.getParent().getReadBytes() < 10L) {
                wsnConnectFuture.setException(new Exception("WSN connection failed"));
            }
        }

        @Override
        protected void doExceptionCaught(HttpConnectSession httpSession, Throwable cause) throws Exception {
            if (logger.isDebugEnabled()) {
                // log the toString of the cause on DEBUG level
                logger.debug("exception on httpSession: " + httpSession + ", cause: " + cause);
            }
        }
    };
}
