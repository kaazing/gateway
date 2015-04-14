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

package org.kaazing.gateway.transport.wsr;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT;
import static org.kaazing.gateway.resource.address.URLUtils.appendURI;
import static org.kaazing.gateway.resource.address.URLUtils.ensureTrailingSlash;
import static org.kaazing.gateway.resource.address.URLUtils.truncateURI;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.EXTENSIONS;
import static org.kaazing.gateway.transport.ws.extension.WsExtensionUtils.negotiateWebSocketExtensions;
import static org.kaazing.mina.core.future.DefaultUnbindFuture.combineFutures;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.gateway.resource.address.Protocol;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.resource.address.URLUtils;
import org.kaazing.gateway.resource.address.ws.WsResourceAddress;
import org.kaazing.gateway.resource.address.wsr.WsrResourceAddressFactorySpi;
import org.kaazing.gateway.security.auth.context.ResultAwareLoginContext;
import org.kaazing.gateway.transport.AbstractBridgeAcceptor;
import org.kaazing.gateway.transport.Bindings;
import org.kaazing.gateway.transport.BridgeAcceptor;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.transport.BridgeSessionInitializer;
import org.kaazing.gateway.transport.BridgeSessionInitializerAdapter;
import org.kaazing.gateway.transport.DefaultIoSessionConfigEx;
import org.kaazing.gateway.transport.DefaultTransportMetadata;
import org.kaazing.gateway.transport.ExceptionLoggingFilter;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.NioBindException;
import org.kaazing.gateway.transport.ObjectLoggingFilter;
import org.kaazing.gateway.transport.TypedAttributeKey;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpAcceptor;
import org.kaazing.gateway.transport.http.HttpProtocol;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpUtils;
import org.kaazing.gateway.transport.http.bridge.filter.HttpLoginSecurityFilter;
import org.kaazing.gateway.transport.ws.AbstractWsBridgeSession;
import org.kaazing.gateway.transport.ws.bridge.extensions.WsExtensions;
import org.kaazing.gateway.transport.ws.extension.ActiveWsExtensions;
import org.kaazing.gateway.transport.ws.extension.WsExtensionNegotiationResult;
import org.kaazing.gateway.transport.ws.util.WsHandshakeNegotiationException;
import org.kaazing.gateway.transport.ws.util.WsUtils;
import org.kaazing.gateway.transport.wsr.bridge.filter.RtmpChunkCodecFilter;
import org.kaazing.gateway.transport.wsr.bridge.filter.RtmpEncoder;
import org.kaazing.gateway.transport.wsr.bridge.filter.WsrBufferAllocator;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.future.UnbindFuture;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WsrAcceptor extends AbstractBridgeAcceptor<WsrSession, WsrBindings.WsrBinding> {

    // TODO: make this setting available via configuration, with a reasonable default
    static final long TIME_TO_TIMEOUT_CONNECT_MILLIS = TimeUnit.SECONDS.toMillis(60L);

    static final AttributeKey TIMEOUT_FUTURE_KEY = new AttributeKey(WsrAcceptor.class, "timeoutFuture");

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final String CREATE_SUFFIX = "/;e/cr";

    private final RtmpChunkCodecFilter codec;

    private static final TypedAttributeKey<WsrSession> SESSION_KEY = new TypedAttributeKey<>(WsrAcceptor.class, "session");
    private static final AttributeKey HTTP_REQUEST_URI_KEY = new AttributeKey(WsrAcceptor.class, "httpRequestURI");

    private static final int COMMAND_STREAM_ID = 3;

    private static final String FAULT_LOGGING_FILTER = "wsn#fault";
    private static final String TRACE_LOGGING_FILTER = "wsn#logging";
    private static final String LOGGER_NAME = String.format("transport.%s.accept", WsrProtocol.NAME);
    private final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    /** This map is used to find WsrSessions from a RTMP connection */
    private final Map<URI, WsrSession> sessionMap;

    private ScheduledExecutorService scheduler;
    private BridgeServiceFactory bridgeServiceFactory;
    private ResourceAddressFactory resourceAddressFactory;

    public WsrAcceptor() {
        super(new DefaultIoSessionConfigEx());
        sessionMap = new ConcurrentHashMap<>();
        codec = new RtmpChunkCodecFilter();
    }

    @Resource(name = "bridgeServiceFactory")
    public void setBridgeServiceFactory(BridgeServiceFactory bridgeServiceFactory) {
        this.bridgeServiceFactory = bridgeServiceFactory;
    }

    @Resource(name = "resourceAddressFactory")
    public void setResourceAddressFactory(ResourceAddressFactory resourceAddressFactory) {
        this.resourceAddressFactory = resourceAddressFactory;
    }

    @Resource(name = "schedulerProvider")
    public void setSchedulerProvider(SchedulerProvider provider) {
        this.scheduler = provider.getScheduler("Timeout-wsr", false);
    }

    @Override
    protected WsrBindings initBindings() {
        return new WsrBindings();
    }

    /* for test observability only */
    WsrBindings bindings() {
        return (WsrBindings) super.bindings;
    }

    @Override
    protected IoProcessorEx<WsrSession> initProcessor() {
        return new WsrAcceptProcessor();
    }

    @Override
    protected boolean canBind(String transportName) {
        return transportName.equals("wsr") || transportName.equals("ws");
    }

    String createResolvePath(URI httpUri, final String suffixWithLeadingSlash) {
        return appendURI(ensureTrailingSlash(httpUri),suffixWithLeadingSlash).getPath();
    }




    @Override
    protected <T extends IoFuture> void bindInternal(final ResourceAddress address,
                                                     IoHandler handler, BridgeSessionInitializer<T> initializer) {

        try {
            bindCreateAddress(address, initializer);
            bindRtmpAddress(address, handler, initializer);


        } catch (NioBindException e) {
            throw new RuntimeException("Unable to bind address "+address+": "+e.getMessage(),e);
        }
    }

    @Override
    protected UnbindFuture unbindInternal(ResourceAddress address, IoHandler handler,
                                          BridgeSessionInitializer<? extends IoFuture> initializer) {

        UnbindFuture future = unbindCreateAddress(address);
        return combineFutures(future, unbindRtmpAddress(address));
    }


    private <T extends IoFuture> void bindCreateAddress(final ResourceAddress address,
                                                        BridgeSessionInitializer<T> initializer) {
        // bind the create URL accessed using HTTP transport
        ResourceAddress wsrCreateAddress = createWsrCreateAddress(address);
        BridgeAcceptor createAcceptor = bridgeServiceFactory.newBridgeAcceptor(wsrCreateAddress);

        final BridgeSessionInitializer<T> theCreateInitializer = (initializer != null)
                ? initializer.getParentInitializer(HttpProtocol.HTTP)
                : null;

        BridgeSessionInitializer<T> wrapperInitializer = new BridgeSessionInitializerAdapter<T>() {
            @Override
            public void initializeSession(IoSession session, T future) {
                if ( theCreateInitializer != null ) {
                    theCreateInitializer.initializeSession(session, future);
                }
                // Store the next over-the-top of-websocket protocols the server supports for this address on session.
                SUPPORTED_PROTOCOLS.set(session, address.getOption(WsResourceAddress.SUPPORTED_PROTOCOLS));
            }
        };

        createAcceptor.bind(wsrCreateAddress,
                            selectCreateHandler(wsrCreateAddress),
                            wrapperInitializer);
    }


    private <T extends IoFuture> void bindRtmpAddress(ResourceAddress address,
                                                      IoHandler handler,
                                                      BridgeSessionInitializer<T> initializer) {
        // TODO: avoid the global bind for all RTMP streams
        //       instead, dynamically bind RTMP streams, like WSE upstream & downstream
        //       note: may require an RTMP transport

        ResourceAddress wsrRtmpAddress = createWsrRtmpAddress(address);

        // add bind mapping for the wsr rtmp address
        addWsrRtmpBinding(wsrRtmpAddress, handler, initializer);

        // bind the wsr rtmp address transport
        BridgeAcceptor acceptor = bridgeServiceFactory.newBridgeAcceptor(wsrRtmpAddress.getTransport());
        BridgeSessionInitializer<T> theInitializer = (initializer != null)
                ? initializer.getParentInitializer(HttpProtocol.HTTP)
                : null;

        acceptor.bind(wsrRtmpAddress.getTransport(), ioBridgeHandler, theInitializer);
    }



    private IoHandler selectCreateHandler(ResourceAddress transport) {
        Protocol protocol = bridgeServiceFactory.getTransportFactory().getProtocol(transport.getResource());
        if ( protocol instanceof HttpProtocol) {
            return wsrCreateHandler;
        }
        throw new RuntimeException("Unable to locate a WSR create handler for address "+transport);
    }


    @Override
    protected IoFuture dispose0() throws Exception {
        scheduler.shutdownNow();
        return super.dispose0();
    }



    private UnbindFuture unbindCreateAddress(final ResourceAddress address) {
        // bind the create URL accessed using HTTP transport
        ResourceAddress wsrCreateAddress = createWsrCreateAddress(address);
        BridgeAcceptor createAcceptor = bridgeServiceFactory.newBridgeAcceptor(wsrCreateAddress);
        return createAcceptor.unbind(wsrCreateAddress);
    }


    private UnbindFuture unbindRtmpAddress(ResourceAddress address) {
        ResourceAddress wsrRtmpAddress = createWsrRtmpAddress(address);
        removeWsrRtmpBinding(wsrRtmpAddress);
        BridgeAcceptor acceptor = bridgeServiceFactory.newBridgeAcceptor(wsrRtmpAddress.getTransport());
        return acceptor.unbind(wsrRtmpAddress.getTransport());
    }

    private ResourceAddress createWsrCreateAddress(ResourceAddress address) {
        ResourceAddress wsrTransportAddress = address.getTransport();
        URI wsrTransportBindURI = wsrTransportAddress.getResource();
        return wsrTransportAddress.resolve(createResolvePath(wsrTransportBindURI, CREATE_SUFFIX));
    }

    private ResourceAddress createWsrRtmpAddress(ResourceAddress address) {

        // wsr[wsr/1.0] | tcp  or wsr[wsr/1.0] | ssl
        // so find http[http/1.1] layer's transport either way and you'll get ssl or tcp.

        ResourceAddress wsrRtmpTransportAddress;
        wsrRtmpTransportAddress = address.findTransport("http[http/1.1]").getTransport();

        ResourceOptions wsrRtmpTransportAddressOptions =
                ResourceOptions.FACTORY.newResourceOptions(wsrRtmpTransportAddress);
        wsrRtmpTransportAddressOptions.setOption(NEXT_PROTOCOL, "rtmp/1.0");
        wsrRtmpTransportAddressOptions.setOption(ResourceAddress.ALTERNATE, null);

        wsrRtmpTransportAddress = resourceAddressFactory.newResourceAddress(wsrRtmpTransportAddress.getExternalURI(),
                                                                            wsrRtmpTransportAddressOptions);

        ResourceOptions wsrRtmpAddressOptions = ResourceOptions.FACTORY.newResourceOptions(address);
        wsrRtmpAddressOptions.setOption(TRANSPORT, wsrRtmpTransportAddress);
        return resourceAddressFactory.newResourceAddress(address.getExternalURI(), wsrRtmpAddressOptions);
    }

    private <T extends IoFuture> void addWsrRtmpBinding(ResourceAddress wsrRtmpAddress, IoHandler handler,
                                                        BridgeSessionInitializer<T> initializer) {
        Bindings.Binding newBinding = new Bindings.Binding(wsrRtmpAddress, handler, initializer);
        Bindings.Binding oldBinding = bindings.addBinding(newBinding);
        if (oldBinding != null) {
            throw new RuntimeException("Unable to bind address " + wsrRtmpAddress
                                               + " because it collides with an already bound address " + oldBinding.bindAddress());
        }
    }

    private void removeWsrRtmpBinding(ResourceAddress wsrRtmpAddress) {
        Bindings.Binding binding = bindings.getBinding(wsrRtmpAddress);
        bindings.removeBinding(wsrRtmpAddress, binding);
    }

    private static final TypedAttributeKey<String[]> SUPPORTED_PROTOCOLS
            = new TypedAttributeKey<>(WsrAcceptor.class, "supportedProtocols");

    private final WsrCreateHandler wsrCreateHandler = new WsrCreateHandler();

    private class WsrCreateHandler extends IoHandlerAdapter<HttpAcceptSession> {

        @Override
        protected void doSessionOpened(final HttpAcceptSession session)
                throws Exception {

            // validate WebSocket version
            String wsrVersion = session.getReadHeader("X-WebSocket-Version");
            if (!"wsr-1.0".equals(wsrVersion)) {
                session.setStatus(HttpStatus.SERVER_NOT_IMPLEMENTED);
                session.setReason("WebSocket-Version not supported");
                session.close(false);
                return;
            }

        	// negotiate WebSocket protocol
            List<String> wsProtocols = session.getReadHeaders("X-WebSocket-Protocol");

            String wsProtocol = null;
            try {
                wsProtocol = WsUtils.negotiateWebSocketProtocol(session,
                        "X-WebSocket-Protocol",
                        wsProtocols,
                        asList(SUPPORTED_PROTOCOLS.remove(session)));
            } catch (WsHandshakeNegotiationException e) {
                return;
            }

            // find (based on this http session) the local address for the WS session
            // we are about to upgrade to.
            ResourceAddress resourceAddress = getWsrLocalAddress(session, WsrResourceAddressFactorySpi.SCHEME_NAME, wsProtocol);

            // fallback to null protocol as a workaround until we properly inject next protocol from service during bind
            // This is safe as we guard this logic via negotiateWebSocketProtocol function
            // If the client send any bogus protocol that is not in the list of supported protocols,
            // we will fail fast before getting here
            if (resourceAddress == null) {
                wsProtocol = null;
                resourceAddress = getWsrLocalAddress(session, WsrResourceAddressFactorySpi.SCHEME_NAME, wsProtocol);
            }

            final String wsProtocol0 = wsProtocol;
            final ResourceAddress wsrLocalAddress = resourceAddress;

            // negotiate WebSocket extensions
            List<String> clientRequestedWsExtensions =  session.getReadHeaders(WsExtensions.HEADER_X_WEBSOCKET_EXTENSIONS);


            // null next-protocol from client gives null local address when we only have explicitly named next-protocol binds
            List<String> wsExtensions = (wsrLocalAddress != null) ? wsrLocalAddress.getOption(EXTENSIONS) : EXTENSIONS.defaultValue();

            WsExtensionNegotiationResult extNegotiationResult =
                negotiateWebSocketExtensions(
                wsrLocalAddress, session,
                WsExtensions.HEADER_X_WEBSOCKET_EXTENSIONS,
                clientRequestedWsExtensions, wsExtensions);
            if (extNegotiationResult.isFailure()) {
                // This happens when the extension negotiation leads to
                // a fatal failure; the session should be closed because
                // the service REQUIRED some extension that the client
                // did not request.
                if (logger.isDebugEnabled()) {
                    if (logger.isDebugEnabled()) {
                        URI requestURI = session.getRequestURL();
                        logger.debug(String.format(
                                "Rejected %s request for URI \"%s\" on session '%s': failed to negotiate client requested extensions '%s'",
                                session.getMethod(), requestURI, session, clientRequestedWsExtensions));
                    }
                }
                session.setStatus(HttpStatus.CLIENT_NOT_FOUND);
                session.setReason("WebSocket Extensions not found");
                session.close(false);
                return;
            }

            final ActiveWsExtensions wsExtensions0 = extNegotiationResult.getExtensions();

            URI request = session.getRequestURL();
            //URI pathInfo = session.getPathInfo();
            String sessionId = HttpUtils.newSessionId();

            String scheme = request.getScheme();
            String path = request.getPath();
            String acceptPath = path.substring(0, path.length() - CREATE_SUFFIX.length());

            Protocol protocol = bridgeServiceFactory.getTransportFactory().getProtocol(scheme);
            if (protocol.isSecure()) {
                scheme = "rtmps";
            } else {
                scheme = "rtmp";
            }


            final String sessionIdSuffix = '/' + sessionId;

            // Use explicit RTMP port for HTTP default ports
            String authority = HttpUtils.getHostAndPort(request.getAuthority(), protocol.isSecure());
            final URI rtmpAddress = new URI(scheme, authority, acceptPath + sessionIdSuffix, request.getQuery(), request.getFragment());

            // create new address to use as key and session remote address

            final ResourceAddress remoteAddress =
                    resourceAddressFactory.newResourceAddress(
                        wsrLocalAddress.getExternalURI());

            final ResourceAddress localAddress =
                    resourceAddressFactory.newResourceAddress(
                            wsrLocalAddress.getResource());

            ResourceAddress httpCreateAddress = session.getLocalAddress();
            URI httpCreateURI = httpCreateAddress.getResource();

            final URI httpUri = session.getRequestURL();
            if ( !httpUri.getPath().contains(CREATE_SUFFIX)) {
                throw new IllegalStateException("Session created with unexpected URL: "+httpUri.toASCIIString());
            }

            final WsrSession wsrSession = newSession(
                    new IoSessionInitializer<IoFuture>() {
                        @Override
                        public void initializeSession(IoSession wsSession,
                                IoFuture future) {
                            wsSession.setAttribute(HttpAcceptor.SERVICE_REGISTRATION_KEY, session.getAttribute(HttpAcceptor.SERVICE_REGISTRATION_KEY));
                            wsSession.setAttribute(HTTP_REQUEST_URI_KEY, session.getRequestURL());
                            ((AbstractWsBridgeSession)wsSession).setSubject(session.getSubject());
                            wsSession.setAttribute(BridgeSession.NEXT_PROTOCOL_KEY, wsProtocol0);
                            wsExtensions0.set(wsSession);
                        }
                    }, new Callable<WsrSession>() {
                        @Override
                        public WsrSession call() {
                            IoBufferAllocatorEx<?> parentAllocator = session.getBufferAllocator();
                            WsrBufferAllocator wsrAllocator = new WsrBufferAllocator(parentAllocator);
                            ResultAwareLoginContext loginContext = (ResultAwareLoginContext) session.getAttribute(HttpLoginSecurityFilter.LOGIN_CONTEXT_KEY);
                            WsrSession wsrSession = new WsrSession(
                                    session.getIoLayer(), session.getIoThread(), session.getIoExecutor(), WsrAcceptor.this, getProcessor(),
                                    localAddress, remoteAddress, wsrAllocator, loginContext.getLoginResult(), wsExtensions0);
                            wsrSession.setBridgeServiceFactory(bridgeServiceFactory);
                            wsrSession.setResourceAddressFactory(resourceAddressFactory);
                            wsrSession.setScheduler(scheduler);
                            IoHandler handler = getHandler(localAddress);
                            wsrSession.setHandler(handler);
                            wsrSession.suspendWrite();
                            return wsrSession;
                        }
                    });
            sessionMap.put(rtmpAddress, wsrSession);

            // write response that session was created and pass redirect urls

            session.setWriteHeader("Content-Type", "text/plain");
            session.setStatus(HttpStatus.SUCCESS_CREATED);

            IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
            ByteBuffer nioBuf = allocator.allocate(256);
            IoBufferEx buf = allocator.wrap(nioBuf).setAutoExpander(allocator);
            CharsetEncoder utf8Encoder = UTF_8.newEncoder();
            buf.putString(rtmpAddress.toASCIIString(), utf8Encoder);
            buf.flip();

            session.setWriteHeader("Content-Length", Integer.toString(buf.remaining()));
            session.write(buf);

            // TODO: use session.writeComplete() instead
            session.close(false);

            // timeout session if rtmp(s) connection is never established
            ScheduledFuture<?> timeoutFuture = scheduler.schedule(wsrSession.getTimeoutCommand(), TIME_TO_TIMEOUT_CONNECT_MILLIS, TimeUnit.MILLISECONDS);
            wsrSession.setAttribute(TIMEOUT_FUTURE_KEY, timeoutFuture);

            // Cancel commands and clear session maps when a session is closed - avoid session reference leak.
            CloseFuture closeFuture = wsrSession.getCloseFuture();
            closeFuture.addListener(new IoFutureListener<CloseFuture>() {
                @Override
                public void operationComplete(CloseFuture future) {
                    if (logger.isTraceEnabled()) {
                        logger.trace(WsrAcceptor.class.getSimpleName() + " removing enforcement of lifetime for closed session (" + wsrSession.getId() + ").");
                    }
                    sessionMap.remove(rtmpAddress);
                    wsrSession.shutdownScheduledCommands();
                    wsrSession.logout();
                }
            });
        }

//        private AcceptOptionsContext getAcceptOptionsContext(IoSession session) {
//            ServiceRegistration sr = (ServiceRegistration) session
//                                .getAttribute(HttpAcceptor.SERVICE_REGISTRATION_KEY);
//            return sr.getServiceContext().getAcceptOptionsContext();
//        }

        protected ResourceAddress getWsrLocalAddress(HttpAcceptSession session,
                                                    final String schemeName,
                                                    String nextProtocol) {
            URI resource = session.getLocalAddress().getResource();
            if (resource.getPath().endsWith(CREATE_SUFFIX)) {
                resource = truncateURI(resource, CREATE_SUFFIX);
            }

            ResourceOptions options = ResourceOptions.FACTORY.newResourceOptions();
            options.setOption(TRANSPORT, session.getLocalAddress().resolve(resource.getPath()));
            options.setOption(NEXT_PROTOCOL, nextProtocol);


            URI wsLocalAddressLocation = URLUtils.modifyURIScheme(resource,
                                                                  schemeName);

            ResourceAddress candidate = resourceAddressFactory.newResourceAddress(
                    wsLocalAddressLocation, options);

            Bindings.Binding binding = bindings.getBinding(candidate);

            if (binding == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("\n***Did NOT find local address for WSR session:" +
                                 "\n***using candidate:\n" +
                                 candidate +
                                 "\n***with bindings " +
                                 bindings);
                }
                return null;
            }

            if (logger.isTraceEnabled()) {
                logger.trace("\n***Found local address for WSR session:\n" +
                             binding.bindAddress() +
                             "\n***via candidate:\n" +
                             candidate +
                             "\n***with bindings " +
                             bindings);
            }

            return binding.bindAddress();
        }
    }

    private IoHandler ioBridgeHandler = new IoHandlerAdapter<IoSessionEx>() {

        @Override
        protected void doExceptionCaught(IoSessionEx session, Throwable cause) throws Exception {
            WsrSession wsrSession = SESSION_KEY.get(session);
            if (wsrSession != null && !wsrSession.isClosing()) {
                wsrSession.reset(cause);
            }
            else {
                if (logger.isDebugEnabled()) {
                    String message = format("Error on WebSocket (WSR) connection, closing connection: %s", cause);
                    if (logger.isTraceEnabled()) {
                        // note: still debug level, but with extra detail about the exception
                        logger.debug(message, cause);
                    }
                    else {
                        logger.debug(message);
                    }
                }
                session.close(true);
            }
        }

        @Override
        protected void doMessageReceived(IoSessionEx session, Object message)
                throws Exception {
            // KG-3160: avoid causing an exception and an ugly log message if the message was not decoded
            // because the filter chain is being torn down due to a race with session close
            if (session.isClosing() && !(message instanceof RtmpMessage)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("WsrAcceptor: ignoring wrongly typed message {} since session is closing", message);
                }
                return;
            }

            final WsrSession wsrSession = SESSION_KEY.get(session);

            RtmpMessage rtmpMessage = (RtmpMessage) message;

            switch (rtmpMessage.getKind()) {
            case VERSION:
                // TODO assert version is 3
                session.write(new RtmpVersionMessage());
                session.write(new RtmpHandshakeRequestMessage());
                break;
            case HANDSHAKE_REQUEST:
                RtmpHandshakeMessage handshake = (RtmpHandshakeMessage) rtmpMessage;
                // For now, just set the returning timestamp to t+1
                handshake.setTimestamp2(handshake.getTimestamp1() + 1);
                session.write(handshake);
                break;
            case HANDSHAKE_RESPONSE:
                // avoid unnecessary fragmentation
                RtmpSetChunkSizeMessage setChunkSize = new RtmpSetChunkSizeMessage();
                setChunkSize.setChunkStreamId(2);
                setChunkSize.setMessageStreamId(0);
                setChunkSize.setChunkSize(RtmpEncoder.MAXIMUM_CHUNK_SIZE);
                session.write(setChunkSize);
                break;
            case STREAM:
                RtmpStreamMessage streamMessage = (RtmpStreamMessage) message;
                switch (streamMessage.getStreamKind()) {
                case COMMAND_AMF0:
                    RtmpCommandMessage commandMessage = (RtmpCommandMessage) rtmpMessage;
                    int messageStreamId = commandMessage.getMessageStreamId();

                    switch (commandMessage.getCommandKind()) {
                    case CONNECT:
                        doHandleConnect(session, (RtmpConnectCommandMessage) commandMessage);
                        break;
                    case CREATE_STREAM:
                        doHandleCreateStream(session, (RtmpCreateStreamCommandMessage) commandMessage);
                        break;
                    case DELETE_STREAM:
                        // TODO close session
                        break;
                    case PLAY:
                        wsrSession.setDownstreamId(messageStreamId);

                        doHandlePlayStream(session, (RtmpPlayCommandMessage) commandMessage);
                        //TODO: If we deferred session created until after the RTMP handshake has completed,
                        //TODO: then there is no need for suspendWrite/resumeWrite (no early writes would be possible)

                        wsrSession.resumeWrite();

                        if (currentThread() == wsrSession.getIoThread()) {
                            // We are always aligned now. if (session.isIoAligned()) {
                            wsrSession.getProcessor().flush(wsrSession);
                        } else {
                            wsrSession.getIoExecutor().execute(new Runnable() {
                                @Override
                                public void run() {
                                    // We are always aligned now. if (session.isIoAligned()) {
                                    wsrSession.getProcessor().flush(wsrSession);
                                }
                            });
                        }
                        break;
                    case PUBLISH:
                        wsrSession.setUpstreamId(messageStreamId);
                        break;
                    }
                    break;
                case USER:
                    break;
                case DATA_AMF3:
                    RtmpBinaryDataMessage amfData = (RtmpBinaryDataMessage) rtmpMessage;
                    IoBufferEx buf = amfData.getBytes();

                    if (wsrSession != null) {
                        IoFilterChain filterChain = wsrSession.getFilterChain();
                        filterChain.fireMessageReceived(buf);
                    }
                    break;
                case ACKNOWLEDGMENT:
                    // ignore client acks
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Unrecognized stream message kind: "
                                    + streamMessage.getStreamKind());
                }
                break;
            default:
                throw new IllegalArgumentException(
                        "Unrecognized message kind: " + rtmpMessage.getKind());
            }

        }

        private int streamCounter = 1;

        private void doHandleCreateStream(IoSession session,
                RtmpCreateStreamCommandMessage request) {
            RtmpCreateStreamResultCommandMessage response = new RtmpCreateStreamResultCommandMessage();
            response.setTransactionId(request.getTransactionId());
            response.setStreamId(streamCounter++);

            response.setChunkStreamId(COMMAND_STREAM_ID);
            session.write(response);
        }

        private void doHandlePlayStream(IoSession session,
                RtmpPlayCommandMessage request) {

            RtmpPlayResponseCommandMessage response = new RtmpPlayResponseCommandMessage();
            response.setTransactionId(request.getTransactionId());

            response.setChunkStreamId(COMMAND_STREAM_ID);
            response.setMessageStreamId(0);
            session.write(response);

            // start data stream
            doSendRtmpSampleAccess(session, request);
            doSendStreamStart(session, request);
            doSendStreamMetaData(session, request);
        }

        private void doSendRtmpSampleAccess(IoSession session,
                RtmpCommandMessage command) {
            RtmpSampleAccessMessage msg = new RtmpSampleAccessMessage();
            msg.setMessageStreamId(command.getMessageStreamId());
            msg.setChunkStreamId(5);
            session.write(msg);
        }

        private void doSendStreamMetaData(IoSession session,
                RtmpCommandMessage command) {
            RtmpStreamMetaDataMessage msg = new RtmpStreamMetaDataMessage();
            msg.setMessageStreamId(command.getMessageStreamId());
            msg.setChunkStreamId(5);
            session.write(msg);
        }

        private void doSendStreamStart(IoSession session,
                RtmpCommandMessage command) {
            RtmpDataStartDataMessage msg = new RtmpDataStartDataMessage();
            msg.setMessageStreamId(command.getMessageStreamId());
            msg.setChunkStreamId(5);
            session.write(msg);
        }

        private void doHandleConnect(final IoSessionEx session,
                RtmpConnectCommandMessage request) throws Exception {

            RtmpConnectResponseCommandMessage response = new RtmpConnectResponseCommandMessage();
            response.setTransactionId(request.getTransactionId());
            response.setChunkStreamId(COMMAND_STREAM_ID);

            URI rtmpAddress = new URI(request.getTcUrl());
            WsrSession wsrSession = sessionMap.get(rtmpAddress);
            wsrSession.setParent(session);
            SESSION_KEY.set(session, wsrSession);

            // At this point we know the wsrSession, and we can add the escaping filter
            // find the encoder from the session and give it the escape sequencer.
            final ActiveWsExtensions extensions = ActiveWsExtensions.get(wsrSession);
            codec.setExtensions(session, extensions);


            // In order to address KG-2167 and related issues, we need to
            // start the scheduled commands at a point in the session when we have a
            // valid WSR session.
            //
            // Due to the code in AbstractWsBridgeSession, which handles the
            // scheduling of those commands, we need to make sure that the
            // parent session for our WSR session object contains the
            // ServiceRegistration object as an attribute (otherwise an ugly
            // NPE ensues).  Hence the conditional check/copy that happens
            // next...

            if (session.getAttribute(HttpAcceptor.SERVICE_REGISTRATION_KEY) == null) {
                session.setAttribute(HttpAcceptor.SERVICE_REGISTRATION_KEY, wsrSession.getAttribute(HttpAcceptor.SERVICE_REGISTRATION_KEY));
            }

            // ...and now we can officially start our lifetime commands.
            wsrSession.startupSessionTimeoutCommand();

            // TODO: cross origin filter paranoia check
            //IoFilterChain filterChain = wsrSession.getFilterChain();
            //filterChain.remove("rtmp.crossOrigin");

            RtmpConnectResponseCommandMessage result = new RtmpConnectResponseCommandMessage();
            result.setTransactionId(request.getTransactionId());
            result.setMessageStreamId(request.getMessageStreamId());

            // cancel the timeout future if it is found
            ScheduledFuture<?> timeoutFuture = (ScheduledFuture<?>) wsrSession.removeAttribute(TIMEOUT_FUTURE_KEY);
            if (timeoutFuture != null && !timeoutFuture.isDone()) {
                timeoutFuture.cancel(false);
            }
            // avoid temp memory leak while the scheduler performs cleanup of canceled tasks
            wsrSession.clearTimeoutCommand();

            session.write(response);
        }

        @Override
        protected void doSessionClosed(final IoSessionEx session)
                throws Exception {
            WsrSession wsrSession = SESSION_KEY.get(session);
            if (wsrSession != null && !wsrSession.isClosing()) {
                // lifetime of WSR session ends normally with underlying transport session
                wsrSession.reset(new Exception("Early termination of IO session").fillInStackTrace());
            }
        }

        @Override
        protected void doSessionOpened(final IoSessionEx session)
                throws Exception {
            IoFilterChain filterChain = session.getFilterChain();
            addBridgeFilters(filterChain);
        }
    };


    @Override
    public void addBridgeFilters(IoFilterChain filterChain) {
        // setup logging filters for bridge session
        if (logger.isTraceEnabled()) {
            filterChain.addFirst(TRACE_LOGGING_FILTER, new ObjectLoggingFilter(logger, WsrProtocol.NAME + "#%s"));
        } else if (logger.isDebugEnabled()) {
            filterChain.addFirst(FAULT_LOGGING_FILTER, new ExceptionLoggingFilter(logger, WsrProtocol.NAME + "#%s"));
        }

        filterChain.addLast("rtmp", codec);
        // filterChain.addLast("rtmp.window", windowFilter);
        //filterChain.addLast("rtmp.crossOrigin", crossOriginFilter);
    }

    @Override
    public void removeBridgeFilters(IoFilterChain filterChain) {
        removeFilter(filterChain, "rtmp");
        removeFilter(filterChain, "log");
        super.removeBridgeFilters(filterChain);
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return new DefaultTransportMetadata(WsrProtocol.WSR.name());
    }

}
