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

package org.kaazing.gateway.transport.wseb;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.kaazing.gateway.resource.address.ResourceAddress.ALTERNATE;
import static org.kaazing.gateway.resource.address.ResourceAddress.BIND_ALTERNATE;
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT;
import static org.kaazing.gateway.resource.address.URLUtils.appendURI;
import static org.kaazing.gateway.resource.address.URLUtils.ensureTrailingSlash;
import static org.kaazing.gateway.resource.address.URLUtils.modifyURIScheme;
import static org.kaazing.gateway.resource.address.URLUtils.truncateURI;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.EXTENSIONS;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.INACTIVITY_TIMEOUT;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.MAX_MESSAGE_SIZE;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CONTENT_LENGTH;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_X_ACCEPT_COMMANDS;
import static org.kaazing.gateway.transport.ws.WsSystemProperty.WSE_IDLE_TIMEOUT;
import static org.kaazing.gateway.transport.ws.bridge.filter.WsCheckAliveFilter.DISABLE_INACTIVITY_TIMEOUT;
import static org.kaazing.gateway.transport.ws.extension.WsExtensionUtils.negotiateWebSocketExtensions;
import static org.kaazing.mina.core.future.DefaultUnbindFuture.combineFutures;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Resource;

import org.kaazing.gateway.transport.http.HttpHeaders;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.gateway.resource.address.Protocol;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ResourceOption;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.resource.address.ws.WsResourceAddress;
import org.kaazing.gateway.security.auth.context.ResultAwareLoginContext;
import org.kaazing.gateway.transport.AbstractBridgeAcceptor;
import org.kaazing.gateway.transport.Bindings;
import org.kaazing.gateway.transport.Bindings.Binding;
import org.kaazing.gateway.transport.BridgeAcceptor;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.transport.BridgeSessionInitializer;
import org.kaazing.gateway.transport.BridgeSessionInitializerAdapter;
import org.kaazing.gateway.transport.CommitFuture;
import org.kaazing.gateway.transport.DefaultIoSessionConfigEx;
import org.kaazing.gateway.transport.DefaultTransportMetadata;
import org.kaazing.gateway.transport.ExceptionLoggingFilter;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.ObjectLoggingFilter;
import org.kaazing.gateway.transport.TypedAttributeKey;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpAcceptor;
import org.kaazing.gateway.transport.http.HttpProtocol;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpUtils;
import org.kaazing.gateway.transport.http.bridge.filter.HttpLoginSecurityFilter;
import org.kaazing.gateway.transport.ws.AbstractWsBridgeSession;
import org.kaazing.gateway.transport.ws.WsProtocol;
import org.kaazing.gateway.transport.ws.bridge.extensions.WsExtensions;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer;
import org.kaazing.gateway.transport.ws.extension.ActiveWsExtensions;
import org.kaazing.gateway.transport.ws.extension.WsExtensionNegotiationResult;
import org.kaazing.gateway.transport.ws.util.WsHandshakeNegotiationException;
import org.kaazing.gateway.transport.ws.util.WsUtils;
import org.kaazing.gateway.transport.wseb.filter.WsebBufferAllocator;
import org.kaazing.gateway.transport.wseb.filter.WsebEncodingCodecFilter.EscapeTypes;
import org.kaazing.gateway.util.Encoding;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.future.UnbindFuture;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.netty.IoSessionIdleTracker;
import org.kaazing.mina.netty.util.threadlocal.VicariousThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: will need some sort of session cleanup timeout
public class WsebAcceptor extends AbstractBridgeAcceptor<WsebSession, Binding> {

    public static final AttributeKey CLIENT_BUFFER_KEY = new AttributeKey(WsebAcceptor.class, "clientBuffer");
    public static final AttributeKey CLIENT_PADDING_KEY = new AttributeKey(WsebAcceptor.class, "clientPadding");
    public static final AttributeKey CLIENT_BLOCK_PADDING_KEY = new AttributeKey(WsebAcceptor.class, "clientBlockPadding");
    public static final AttributeKey BYTES_WRITTEN_ON_LAST_FLUSH_KEY = new AttributeKey(WsebAcceptor.class, "bytesWrittenOnLastFlush");
    private static final AttributeKey HTTP_REQUEST_URI_KEY = new AttributeKey(WsebAcceptor.class, "httpRequestURI");

    public static final String EMULATED_SUFFIX = "/;e";

    private static final String COOKIES_SUFFIX = EMULATED_SUFFIX + "/cookies";

    private static final String CREATE_SUFFIX = EMULATED_SUFFIX + "/cb";
    private static final String UPSTREAM_SUFFIX = EMULATED_SUFFIX + "/ub";
    private static final String DOWNSTREAM_SUFFIX = EMULATED_SUFFIX + "/db";

    private static final String CREATE_TEXT_SUFFIX = EMULATED_SUFFIX + "/ct";
    private static final String UPSTREAM_TEXT_SUFFIX = EMULATED_SUFFIX + "/ut";
    private static final String DOWNSTREAM_TEXT_SUFFIX = EMULATED_SUFFIX + "/dt";

    private static final String CREATE_TEXT_ESCAPED_SUFFIX = EMULATED_SUFFIX + "/cte";
    private static final String UPSTREAM_TEXT_ESCAPED_SUFFIX = EMULATED_SUFFIX + "/ute";
    private static final String DOWNSTREAM_TEXT_ESCAPED_SUFFIX = EMULATED_SUFFIX + "/dte";

    static final String CREATE_MIXED_SUFFIX = EMULATED_SUFFIX + "/cbm";
    private static final String UPSTREAM_MIXED_SUFFIX = EMULATED_SUFFIX + "/ubm";
    static final String DOWNSTREAM_MIXED_SUFFIX = EMULATED_SUFFIX + "/dbm";

    static final String CREATE_MIXED_TEXT_SUFFIX = EMULATED_SUFFIX + "/ctm";
    private static final String UPSTREAM_MIXED_TEXT_SUFFIX = EMULATED_SUFFIX + "/utm";
    static final String DOWNSTREAM_MIXED_TEXT_SUFFIX = EMULATED_SUFFIX + "/dtm";

    static final String CREATE_MIXED_TEXT_ESCAPED_SUFFIX = EMULATED_SUFFIX + "/ctem";
    private static final String UPSTREAM_MIXED_TEXT_ESCAPED_SUFFIX = EMULATED_SUFFIX + "/utem";
    static final String DOWNSTREAM_MIXED_TEXT_ESCAPED_SUFFIX = EMULATED_SUFFIX + "/dtem";

    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    protected static final byte LINEFEED_BYTE = "\n".getBytes()[0];

    private static final TypedAttributeKey<WsebSession> SESSION_KEY =
            new TypedAttributeKey<>(WsebAcceptor.class, "wseSession");

    private static final TypedAttributeKey<String[]> SUPPORTED_PROTOCOLS =
            new TypedAttributeKey<>(WsebAcceptor.class, "supportedProtocols");

    // used to deal with fragmented wseb-create-message content
    private static final TypedAttributeKey<Integer> CREATE_CONTENT_LENGTH_READ =
            new TypedAttributeKey<>(WsebAcceptor.class, "createContentLengthRead");

    private static final String FAULT_LOGGING_FILTER = WsebProtocol.NAME + "#fault";
    private static final String TRACE_LOGGING_FILTER = WsebProtocol.NAME + "#logging";
    private static final String LOGGER_NAME = String.format("transport.%s.accept", WsebProtocol.NAME);

    private Properties configuration;
    private final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    private ScheduledExecutorService scheduler;
    private BridgeServiceFactory bridgeServiceFactory;
    private ResourceAddressFactory resourceAddressFactory;

    private final List<IoSessionIdleTracker> sessionInactivityTrackers
        = Collections.synchronizedList(new ArrayList<IoSessionIdleTracker>());
    private final ThreadLocal<IoSessionIdleTracker> currentSessionInactivityTracker
        = new VicariousThreadLocal<IoSessionIdleTracker>() {
            @Override
            protected IoSessionIdleTracker initialValue() {
                IoSessionIdleTracker result = new WsebInactivityTracker(logger);
                sessionInactivityTrackers.add(result);
                return result;
            }
    };

    public WsebAcceptor() {
        super(new DefaultIoSessionConfigEx());
    }

    @Override
    protected IoProcessorEx<WsebSession> initProcessor() {
        return new WsebAcceptProcessor(scheduler);
    }

    @Resource(name = "configuration")
    public void setConfiguration(Properties configuration) {
        this.configuration = configuration;
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
        this.scheduler = provider.getScheduler("KeepAlive-Wseb", true);
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return new DefaultTransportMetadata(WsebProtocol.NAME);
    }

    @Override
    public void addBridgeFilters(IoFilterChain filterChain) {
        // setup logging filters for bridge session
        if (logger.isTraceEnabled()) {
            filterChain.addFirst(TRACE_LOGGING_FILTER, new ObjectLoggingFilter(logger, WsebProtocol.NAME + "#%s"));
        } else if (logger.isDebugEnabled()) {
            filterChain.addFirst(FAULT_LOGGING_FILTER, new ExceptionLoggingFilter(logger, WsebProtocol.NAME + "#%s"));
        }
    }

    @Override
    public void removeBridgeFilters(IoFilterChain filterChain) {
        if (filterChain.contains(TRACE_LOGGING_FILTER)) {
            filterChain.remove(TRACE_LOGGING_FILTER);
        } else if (filterChain.contains(FAULT_LOGGING_FILTER)) {
            filterChain.remove(FAULT_LOGGING_FILTER);
        }
    }

    @Override
    protected boolean canBind(String transportName) {
        return transportName.equals("wse") || transportName.equals("ws");
    }

    @Override
    protected Bindings<Binding> initBindings() {
        return new Bindings.Default();
    }

    /* for test observability only */
    Bindings<Bindings.Binding> bindings() {
        return super.bindings;
    }

    @Override
    protected <T extends IoFuture> void bindInternal(final ResourceAddress address, IoHandler handler,
                                                     final BridgeSessionInitializer<T> initializer) {
        try {
            bindCookiesHandler(address.findTransport("http[http/1.1]"));

            final ResourceAddress transportAddress = address.getTransport();
            URI transportURI = transportAddress.getExternalURI();

            Protocol httpProtocol = bridgeServiceFactory.getTransportFactory().getProtocol(transportAddress.getResource().getScheme());
            final BridgeSessionInitializer<T> httpInitializer = (initializer != null) ? initializer.getParentInitializer(httpProtocol) : null;

            BridgeSessionInitializer<T> wrapperHttpInitializer = new BridgeSessionInitializerAdapter<T>() {
                @Override
                public void initializeSession(IoSession session, T future) {
                    if ( httpInitializer != null ) {
                        httpInitializer.initializeSession(session, future);
                    }

                    String nextProtocol = address.getOption(ResourceAddress.NEXT_PROTOCOL);
                    String[] supportedProtocols = address.getOption(WsResourceAddress.SUPPORTED_PROTOCOLS);
                    String[] allSupportedProtocols = supportedProtocols;
                    if (nextProtocol != null) {
                        // add next-protocol (if specified) to end of supported protocols (whether empty or not)
                        allSupportedProtocols = new String[supportedProtocols.length + 1];
                        System.arraycopy(supportedProtocols, 0, allSupportedProtocols, 0, supportedProtocols.length);
                        allSupportedProtocols[supportedProtocols.length] = nextProtocol;
                    }

                    // Store the next over-the-top of-websocket protocols the server supports for this address on session.
                    SUPPORTED_PROTOCOLS.set(session, allSupportedProtocols);
                }
            };

            BridgeAcceptor transportAcceptor = bridgeServiceFactory.newBridgeAcceptor(transportAddress);

            final ResourceAddress createAddress = transportAddress.resolve(createResolvePath(transportURI,CREATE_SUFFIX));
            final ResourceAddress createTextAddress = transportAddress.resolve(createResolvePath(transportURI,CREATE_TEXT_SUFFIX));
            final ResourceAddress createTextEscapedAddress = transportAddress.resolve(createResolvePath(transportURI, CREATE_TEXT_ESCAPED_SUFFIX));
            final ResourceAddress createMixedAddress = transportAddress.resolve(createResolvePath(transportURI,CREATE_MIXED_SUFFIX));
            final ResourceAddress createMixedTextAddress = transportAddress.resolve(createResolvePath(transportURI,CREATE_MIXED_TEXT_SUFFIX));
            final ResourceAddress createMixedTextEscapedAddress = transportAddress.resolve(createResolvePath(transportURI, CREATE_MIXED_TEXT_ESCAPED_SUFFIX));

            transportAcceptor.bind(createAddress,
                    selectCreateHandler(createAddress),
                    wrapperHttpInitializer
            );

            transportAcceptor.bind(createTextAddress,
                    selectCreateHandler(createTextAddress),
                    wrapperHttpInitializer
            );

            transportAcceptor.bind(createTextEscapedAddress,
                    selectCreateHandler(createTextEscapedAddress),
                    wrapperHttpInitializer);

            transportAcceptor.bind(createMixedAddress,
                    selectCreateHandler(createMixedAddress),
                    wrapperHttpInitializer
            );

            transportAcceptor.bind(createMixedTextAddress,
                    selectCreateHandler(createMixedTextAddress),
                    wrapperHttpInitializer
            );

            transportAcceptor.bind(createMixedTextEscapedAddress,
                    selectCreateHandler(createMixedTextEscapedAddress),
                    wrapperHttpInitializer);

        } catch (Exception e) {
            throw new RuntimeException("Unable to bind address " + address + ": " + e.getMessage(),e );
        }

    }

    private IoHandler selectCreateHandler(ResourceAddress createAddress) {
        final Protocol protocol = bridgeServiceFactory.getTransportFactory().getProtocol(createAddress.getResource());
        if ( protocol instanceof HttpProtocol ) {
            String transportURI = createAddress.getResource().toASCIIString();
            if (transportURI.endsWith(CREATE_SUFFIX)) {
                return createHandler;
            } else if (transportURI.endsWith(CREATE_TEXT_SUFFIX)) {
                return createTextHandler;
            } else if (transportURI.endsWith(CREATE_TEXT_ESCAPED_SUFFIX)) {
                return createTextEscapedHandler;
            } else if (transportURI.endsWith(CREATE_MIXED_SUFFIX)) {
                return createMixedHandler;
            } else if (transportURI.endsWith(CREATE_MIXED_TEXT_SUFFIX)) {
                return createMixedTextHandler;
            } else if (transportURI.endsWith(CREATE_MIXED_TEXT_ESCAPED_SUFFIX)) {
                return createMixedTextEscapedHandler;
            }
        }
        throw new RuntimeException("Cannot locate a create handler for transport address "+createAddress);
    }

    private void bindCookiesHandler(ResourceAddress address) {
        final ResourceAddress cookiesAddress = createCookiesAddress(address);


        BridgeAcceptor cookiesAcceptor = bridgeServiceFactory.newBridgeAcceptor(cookiesAddress);
        cookiesAcceptor.bind(cookiesAddress, cookiesHandler, null);
    }

    private UnbindFuture unbindCookiesHandler(ResourceAddress address) {
        final ResourceAddress cookiesAddress = createCookiesAddress(address);
        BridgeAcceptor cookiesAcceptor = bridgeServiceFactory.newBridgeAcceptor(cookiesAddress);
        return cookiesAcceptor.unbind(cookiesAddress);
    }


    private ResourceAddress createCookiesAddress(ResourceAddress httpAddress) {
        // /;e/cookies is a terminal endpoint so next protocol should == null
        ResourceOptions cookieOptions = ResourceOptions.FACTORY.newResourceOptions(httpAddress);
        cookieOptions.setOption(NEXT_PROTOCOL, null);
        cookieOptions.setOption(TRANSPORT, httpAddress.getTransport());
        cookieOptions.setOption(BIND_ALTERNATE, Boolean.FALSE);

        URI cookiesLocation = appendURI(httpAddress.getResource(), COOKIES_SUFFIX);
        return resourceAddressFactory.newResourceAddress(cookiesLocation, cookieOptions);
    }

    @Override
    protected UnbindFuture unbindInternal(ResourceAddress address, IoHandler handler,
            BridgeSessionInitializer<? extends IoFuture> initializer) {

        final ResourceAddress transportAddress = address.getTransport();
        URI transportURI = transportAddress.getExternalURI();
        BridgeAcceptor acceptor = bridgeServiceFactory.newBridgeAcceptor(transportAddress);

        UnbindFuture future = unbindCookiesHandler(address.findTransport("http[http/1.1]"));

        final ResourceAddress createAddress =
                transportAddress.resolve(createResolvePath(transportURI,CREATE_SUFFIX));

        final ResourceAddress createTextAddress =
                transportAddress.resolve(createResolvePath(transportURI,CREATE_TEXT_SUFFIX));

        final ResourceAddress createTextEscapedAddress =
                transportAddress.resolve(createResolvePath(transportURI,CREATE_TEXT_ESCAPED_SUFFIX));

        final ResourceAddress createMixedAddress =
                transportAddress.resolve(createResolvePath(transportURI,CREATE_MIXED_SUFFIX));

        final ResourceAddress createMixedTextAddress =
                transportAddress.resolve(createResolvePath(transportURI,CREATE_MIXED_TEXT_SUFFIX));

        final ResourceAddress createMixedTextEscapedAddress =
                transportAddress.resolve(createResolvePath(transportURI,CREATE_MIXED_TEXT_ESCAPED_SUFFIX));

        future = combineFutures(future, acceptor.unbind(createAddress));
        future = combineFutures(future, acceptor.unbind(createTextAddress));
        future = combineFutures(future, acceptor.unbind(createTextEscapedAddress));
        future = combineFutures(future, acceptor.unbind(createMixedAddress));
        future = combineFutures(future, acceptor.unbind(createMixedTextAddress));
        future = combineFutures(future, acceptor.unbind(createMixedTextEscapedAddress));
        return future;
    }

    @Override
    protected IoFuture dispose0() throws Exception {
        for (IoSessionIdleTracker tracker : sessionInactivityTrackers) {
            tracker.dispose();
        }
        scheduler.shutdownNow();
        return super.dispose0();
    }

    String createResolvePath(URI httpUri, final String suffixWithLeadingSlash) {
        return appendURI(ensureTrailingSlash(httpUri),suffixWithLeadingSlash).getPath();
    }


    final class WsebCreateHandler extends IoHandlerAdapter<HttpAcceptSession> {

        private final String createSuffix;
        private final String downstreamSuffix;
        private final String upstreamSuffix;

        public WsebCreateHandler(String createSuffix,
                                 String downstreamSuffix,
                                 String upstreamSuffix) {
            this.createSuffix = createSuffix;
            this.downstreamSuffix = downstreamSuffix;
            this.upstreamSuffix = upstreamSuffix;
        }

        private IoFutureListener<CloseFuture> getWsebCloseListener(final BridgeAcceptor upstreamAcceptor,
                                                                   final BridgeAcceptor downstreamAcceptor,
                                                                   final WsebSession wsebSession,
                                                                   final ResourceAddress downstreamAddress,
                                                                   final ResourceAddress upstreamAddress) {
            return new IoFutureListener<CloseFuture>() {
                @Override
                public void operationComplete(CloseFuture future) {
                    if (wsebSession.getInactivityTimeout() > 0) {
                        currentSessionInactivityTracker.get().removeSession(wsebSession);
                    }

                    downstreamAcceptor.unbind(downstreamAddress);
                    upstreamAcceptor.unbind(upstreamAddress);

                    wsebSession.shutdownScheduledCommands();
                    wsebSession.logout();
                }
            };
        }

        @Override
        protected void doSessionOpened(final HttpAcceptSession session) throws Exception {
        	// validate WebSocket version
            if (! validWsebVersion(session)) return;

            String contentLengthStr = session.getReadHeader("Content-Length");
            if (contentLengthStr == null || "0".equals(contentLengthStr)) {
                createWsebSessionAndFinalizeResponse(session);
            }
        }

        @Override
        protected void doMessageReceived(HttpAcceptSession session, Object message) throws Exception {
            // when we get the content coming up, then we can create and finalize the response.
            String contentLengthStr = session.getReadHeader("Content-Length");
            if (contentLengthStr != null && !"0".equals(contentLengthStr)) {
                Integer expectedContentLength = Integer.valueOf(contentLengthStr);
                IoBufferEx buffer = (IoBufferEx) message;
                Integer readSoFar = CREATE_CONTENT_LENGTH_READ.get(session);
                if (readSoFar == null) {
                    readSoFar=0;
                }

                readSoFar += buffer.remaining();

                if (readSoFar.intValue()==expectedContentLength) {
                    createWsebSessionAndFinalizeResponse(session);
                }
                else if (readSoFar < expectedContentLength) {
                    CREATE_CONTENT_LENGTH_READ.set(session, readSoFar);
                }
                else if (readSoFar > expectedContentLength) {
                    logger.error(String.format("Unexpected content while reading WSE create message content: %s", buffer));
                }
            }
            else {
                super.doMessageReceived(session,message);
            }
        }

        private void createWsebSessionAndFinalizeResponse(final HttpAcceptSession session) throws Exception {
            String sequenceStr = session.getReadHeader(HttpHeaders.HEADER_X_SEQUENCE_NO);
            final boolean validateSequenceNo = (sequenceStr != null);
            final long sequenceNo = validateSequenceNo ? Long.parseLong(sequenceStr) : -1;

            final boolean wasHixieHandshake = wasHixieHandshake(session);


            // negotiate WebSocket protocol
            String wsProtocol;
            List<String> wsProtocols = session.getReadHeaders("X-WebSocket-Protocol");
            try {
                wsProtocol = WsUtils.negotiateWebSocketProtocol(session,
                                                                "X-WebSocket-Protocol",
                                                                wsProtocols,
                                                                asList(SUPPORTED_PROTOCOLS.remove(session)));
            } catch (WsHandshakeNegotiationException e) {
                return;
            }


            ResourceAddress wseLocalAddress = getWseLocalAddress(session, wsProtocol);

            // fallback to null protocol as a workaround until we properly inject next protocol from service during bind
            // This is safe as we guard this logic via negotiateWebSocketProtocol function
            // If the client send any bogus protocol that is not in the list of supported protocols,
            // we will fail fast before getting here
            if (wseLocalAddress == null) {
                wsProtocol = null;
                wseLocalAddress = getWseLocalAddress(session, wsProtocol);
            }

            final ResourceAddress localAddress = wseLocalAddress;

            assert localAddress != null;

            final String wsProtocol0 = wsProtocol;
            List<String> clientRequestedWsExtensions =  session.getReadHeaders(WsExtensions.HEADER_X_WEBSOCKET_EXTENSIONS);



            // null next-protocol from client gives null local address when we only have explicitly named next-protocol binds
            List<String> wsExtensions = (wseLocalAddress != null) ? wseLocalAddress.getOption(EXTENSIONS) : EXTENSIONS.defaultValue();

            WsExtensionNegotiationResult extNegotiationResult =
                negotiateWebSocketExtensions(wseLocalAddress, session,
                    WsExtensions.HEADER_X_WEBSOCKET_EXTENSIONS,
                    clientRequestedWsExtensions, wsExtensions);
            if (extNegotiationResult.isFailure()) {
                // This happens when the extension negotiation leads to
                // a fatal failure; the session should be closed because
                // the service REQUIRED some extension that the client
                // did not request.
                if (logger.isDebugEnabled()) {
                    URI requestURI = session.getRequestURL();
                    logger.debug(String.format(
                            "Rejected %s request for URI \"%s\" on session '%s': failed to negotiate client requested extensions '%s'",
                            session.getMethod(), requestURI, session, clientRequestedWsExtensions));
                }
                session.setStatus(HttpStatus.CLIENT_NOT_FOUND);
                session.setReason("WebSocket Extensions not found");
                session.close(false);
                return;
            }

            final ActiveWsExtensions wsExtensions0 = extNegotiationResult.getExtensions();

            // require POST method with text/plain content-type header
            // to prevent cross-site GETs or form POSTs
            // Warning: enveloped client responses in later cores may be converting POSTs to GETs on redirect

            // Note: temporarily disable strictness checking until balancer can be made functional
            // HttpMethod method = session.getMethod();
            // String contentType = session.getReadHeader(HEADER_CONTENT_TYPE);
            // if (method != HttpMethod.POST || contentType == null || !contentType.startsWith("text/plain")) {
            //    session.setStatus(HttpStatus.CLIENT_BAD_REQUEST);
            //    session.close(false);
            //    return;
            //}

            URI request = session.getRequestURL();
            URI pathInfo = session.getPathInfo();
            String sessionId = HttpUtils.newSessionId();

            // check to see if we can stream to this session
            // Note: Silverlight does not support redirecting from http to https
            // so check .kd=s parameter (downstream=same) to prevent redirecting to https
            if (!HttpUtils.canStream(session) && !"s".equals(session.getParameter(".kd"))) {
                // lookup secure acceptURI
                URI secureAcceptURI = locateSecureAcceptURI(session);
                if (secureAcceptURI != null) {
                    String secureAuthority = secureAcceptURI.getAuthority();
                    String secureAcceptPath = secureAcceptURI.getPath();

                    request = URI.create("https://" + secureAuthority + secureAcceptPath + pathInfo.toString());
                }
            }

            final IoBufferAllocatorEx<WsBuffer> allocator = new WsebBufferAllocator(session.getBufferAllocator());

            // create new address to use as key and session remote address
            ResourceAddress remoteBridgeAddress = BridgeSession.REMOTE_ADDRESS.get(session);
            URI remoteLocation = ensureTrailingSlash(modifyURIScheme(remoteBridgeAddress.getResource(), "wse"));
            ResourceOptions options = ResourceOptions.FACTORY.newResourceOptions();
            options.setOption(TRANSPORT, remoteBridgeAddress);
            final ResourceAddress remoteAddress =  resourceAddressFactory.newResourceAddress(remoteLocation, options, sessionId);


            final URI httpUri = session.getRequestURL();
            if (!httpUri.getPath().contains(createSuffix)) {
                throw new IllegalStateException("Session created with unexpected URL: "+httpUri.toASCIIString());
            }

            // We can only honor inactivity timeout if the client supports PING
            String acceptCommands = session.getReadHeader(HEADER_X_ACCEPT_COMMANDS);
            long configuredInactivityTimeout = localAddress.getOption(INACTIVITY_TIMEOUT);
            final long inactivityTimeout =
                    // TODO: parse acceptCommands properly in case we need to support multiple commands (comma separated)
                    (acceptCommands != null && acceptCommands.equals("ping")) ? configuredInactivityTimeout : DISABLE_INACTIVITY_TIMEOUT;

            // Default IDLE_TIMEOUT to the value of ws inactivity timeout if set, but don't let it be less than 5 secs
            final int clientIdleTimeout;
            if ( !WSE_IDLE_TIMEOUT.isSet(configuration) && configuredInactivityTimeout != DISABLE_INACTIVITY_TIMEOUT ) {
                clientIdleTimeout = configuredInactivityTimeout < 5000 ? 5 : (int)configuredInactivityTimeout / 1000;
            }
            else {
                clientIdleTimeout = WSE_IDLE_TIMEOUT.getIntProperty(configuration);
            }

            // create session
            final WsebSession wsebSession = newSession(new IoSessionInitializer<IoFuture>() {
                @Override
                public void initializeSession(IoSession wsSession, IoFuture future) {
                    wsSession.setAttribute(HttpAcceptor.SERVICE_REGISTRATION_KEY, session
                            .getAttribute(HttpAcceptor.SERVICE_REGISTRATION_KEY));
                    wsSession.setAttribute(HTTP_REQUEST_URI_KEY, session.getRequestURL());
                    ((AbstractWsBridgeSession<?, ?>) wsSession).setSubject(session.getSubject());
                    wsSession.setAttribute(BridgeSession.NEXT_PROTOCOL_KEY, wsProtocol0);
                    wsExtensions0.set(wsSession);
                    HttpLoginSecurityFilter.LOGIN_CONTEXT_KEY.set(wsSession,
                            HttpLoginSecurityFilter.LOGIN_CONTEXT_KEY.get(session));
                }
            }, new Callable<WsebSession>() {
                @Override
                public WsebSession call() {
                    ResultAwareLoginContext loginContext = (ResultAwareLoginContext) session.getAttribute(
                            HttpLoginSecurityFilter.LOGIN_CONTEXT_KEY);
                    WsebSession newWsebSession = new WsebSession(session.getIoLayer(), session.getIoThread(), session.getIoExecutor(), WsebAcceptor.this, getProcessor(),
                            localAddress, remoteAddress, allocator, loginContext.getLoginResult(), wsExtensions0, clientIdleTimeout, inactivityTimeout,
                            validateSequenceNo, sequenceNo);
                    IoHandler handler = getHandler(newWsebSession.getLocalAddress());
                    newWsebSession.setHandler(handler);
                    newWsebSession.setBridgeServiceFactory(bridgeServiceFactory);
                    newWsebSession.setResourceAddressFactory(resourceAddressFactory);
                    newWsebSession.setScheduler(scheduler);
                    return newWsebSession;
                }
            });

            //
            // UP- and DOWN- STREAMS: WIRE
            //


            // Find the http/1.1 session and use it's external uri as the basis for the up and downstream urls on wire.
            // note: we must use the remote address to transfer any query parameters from create to upstream and downstream
            ResourceAddress remoteHttp11Address = remoteAddress.findTransport("http[http/1.1]");
            ResourceAddress localHttp11Address = localAddress.findTransport("http[http/1.1]");
            if (remoteHttp11Address == null || localHttp11Address == null) {
                throw new RuntimeException("Cannot construct up- and down- stream urls: no http/1.1 transport found.");
            }
            URI remoteExternalHttp11 = remoteHttp11Address.getExternalURI();
            URI localExternalHttp11 = localHttp11Address.getExternalURI();

            final String sessionIdSuffix = '/' + sessionId;
            // add path suffixes for upstream and downstream URLs relative to local bind path
            // but retain query parameters
            // Note: to integrate with cross-site access control,
            // it is important for suffixes to be hierarchical
            // (begin with forward slash)

            URI remoteExternalDownstream = new URI(remoteExternalHttp11.getScheme(),
                                                   remoteExternalHttp11.getUserInfo(),
                                                   remoteExternalHttp11.getHost(),
                                                   remoteExternalHttp11.getPort(),
                                                   createResolvePath(localExternalHttp11, downstreamSuffix + sessionIdSuffix),
                                                   remoteExternalHttp11.getQuery(),
                                                   remoteExternalHttp11.getFragment());

            URI remoteExternalUpstream = new URI(remoteExternalHttp11.getScheme(),
                                                 remoteExternalHttp11.getUserInfo(),
                                                 remoteExternalHttp11.getHost(),
                                                 remoteExternalHttp11.getPort(),
                                                 createResolvePath(localExternalHttp11, upstreamSuffix + sessionIdSuffix),
                                                 remoteExternalHttp11.getQuery(),
                                                 remoteExternalHttp11.getFragment());

            //
            // UP- and DOWN- STREAMS: BIND
            //
            final ResourceAddress httpAddress = localAddress.getTransport();
            final ResourceAddress httpxeAddress = localAddress.getTransport().getOption(ALTERNATE);

            ResourceOptions httpxeNoSecurityOptions = new NoSecurityResourceOptions(httpxeAddress);
            httpxeNoSecurityOptions.setOption(ALTERNATE, null);
            ResourceAddress httpxeBaseAddress =
                    resourceAddressFactory.newResourceAddress(httpxeAddress.getExternalURI(),
                                                              httpxeNoSecurityOptions,
                                                              httpxeAddress.getOption(ResourceAddress.QUALIFIER));

            ResourceOptions httpNoSecurityOptions = new NoSecurityResourceOptions(httpAddress);
            httpNoSecurityOptions.setOption(ALTERNATE, httpxeBaseAddress);

            ResourceAddress httpBaseAddress =
                    resourceAddressFactory.newResourceAddress(httpAddress.getExternalURI(),
                                                              httpNoSecurityOptions,
                                                              httpAddress.getOption(ResourceAddress.QUALIFIER));


            ResourceAddress localDownstream = httpBaseAddress.resolve(createResolvePath(httpBaseAddress.getResource(), downstreamSuffix + sessionIdSuffix));
            logger.trace("Binding "+localDownstream.getTransport()+" to downstreamHandler");

            ResourceAddress localUpstream = httpBaseAddress.resolve(createResolvePath(httpBaseAddress.getResource(), upstreamSuffix + sessionIdSuffix));
            logger.trace("Binding "+localUpstream.getTransport()+" to upstreamHandler");

            BridgeAcceptor downstreamAcceptor = bridgeServiceFactory.newBridgeAcceptor(localDownstream);
            downstreamAcceptor.bind(localDownstream,
                    selectDownstreamHandler(localAddress, wsebSession),
                    null);

            BridgeAcceptor upstreamAcceptor = bridgeServiceFactory.newBridgeAcceptor(localUpstream);
            upstreamAcceptor.bind(localUpstream,
                selectUpstreamHandler(localAddress, wsebSession),
                    null);

            //
            // WEBSOCKET SESSION CLOSE
            //
            CloseFuture closeFuture = wsebSession.getCloseFuture();
            closeFuture.addListener(getWsebCloseListener(upstreamAcceptor, downstreamAcceptor, wsebSession, localDownstream, localUpstream));


            //
            // RESPONSE TO THE CREATE REQUEST ON THE HTTPXE SESSION
            //
            // write response that session was created and pass redirect urls
            session.setWriteHeader(HEADER_CONTENT_TYPE, "text/plain;charset=UTF-8");
            session.setWriteHeader(HttpHeaders.HEADER_CACHE_CONTROL, "no-cache");
            session.setStatus(HttpStatus.SUCCESS_CREATED);

            IoBufferAllocatorEx<?> httpAllocator = session.getBufferAllocator();
            IoBufferEx buf = httpAllocator.wrap(httpAllocator.allocate(256)).setAutoExpander(allocator);
            CharsetEncoder utf8Encoder = UTF_8.newEncoder();
            buf.putString(remoteExternalUpstream.toASCIIString(), utf8Encoder);
            if (!remoteExternalDownstream.equals(remoteExternalUpstream)) {
                buf.put(LINEFEED_BYTE);
                buf.putString(remoteExternalDownstream.toASCIIString(), utf8Encoder);
                buf.put(LINEFEED_BYTE);
            }
            buf.flip();

            session.setWriteHeader(HEADER_CONTENT_LENGTH, Integer.toString(buf.remaining()));
            session.write(buf);

            // TODO: use session.writeComplete() instead
            session.close(false);

            // may need to close WseSession if exception occurs
            SESSION_KEY.set(session, wsebSession);

            // timeout session if downstream is never attached
            wsebSession.scheduleTimeout(scheduler);
        }

        private boolean validWsebVersion(HttpAcceptSession session) {
            String wsebVersion = session.getReadHeader("X-WebSocket-Version");
            if (wsebVersion != null && !wsebVersion.equals("wseb-1.0")) {
                session.setStatus(HttpStatus.SERVER_NOT_IMPLEMENTED);
                session.setReason("WebSocket-Version not supported");
                session.close(false);
                return false;
            }
            return true;
        }

        private boolean wasHixieHandshake(HttpAcceptSession session) {
            // see KG-2749
            // Are we receiving a Hixie or a standard Http Websocket upgrade request?
            return session.getReadHeader("Sec-WebSocket-Key1") != null ||
            session.getReadHeader("Sec-WebSocket-Key") == null;
        }

        private IoHandler selectUpstreamHandler(ResourceAddress address,
                                                WsebSession wsebSession) {
            ResourceAddress transportAddress = address.getTransport();
            final Protocol protocol = bridgeServiceFactory.getTransportFactory().getProtocol(transportAddress.getResource());
            if (protocol instanceof HttpProtocol) {
                int wsMaxMessageSize = address.getOption(MAX_MESSAGE_SIZE);
                if (UPSTREAM_SUFFIX.equals(upstreamSuffix)) {
                    return new WsebUpstreamHandler(address, wsebSession, wsMaxMessageSize);
                } else if (UPSTREAM_TEXT_SUFFIX.equals(upstreamSuffix)) {
                    return new WsebUpstreamHandler(address, wsebSession, Encoding.UTF8, wsMaxMessageSize);
                } else if (UPSTREAM_TEXT_ESCAPED_SUFFIX.equals(upstreamSuffix)) {
                    return new WsebUpstreamHandler(address, wsebSession, Encoding.UTF8_ESCAPE_ZERO_AND_NEWLINE, wsMaxMessageSize);
                } else if (UPSTREAM_MIXED_SUFFIX.equals(upstreamSuffix)) {
                    return new WsebUpstreamHandler(address, wsebSession, wsMaxMessageSize);

                } else if (UPSTREAM_MIXED_TEXT_SUFFIX.equals(upstreamSuffix)) {
                    return new WsebUpstreamHandler(address, wsebSession, Encoding.UTF8, wsMaxMessageSize);

                } else if (UPSTREAM_MIXED_TEXT_ESCAPED_SUFFIX.equals(upstreamSuffix)) {
                    return new WsebUpstreamHandler(address, wsebSession, Encoding.UTF8_ESCAPE_ZERO_AND_NEWLINE, wsMaxMessageSize);
                }
            }
            throw new RuntimeException("Cannot locate a upstream handler for transport address "+address);
        }

        private IoHandler selectDownstreamHandler(ResourceAddress address,
                                                  WsebSession wsebSession) {
            ResourceAddress transportAddress = address.getTransport();
            final Protocol protocol = bridgeServiceFactory.getTransportFactory().getProtocol(transportAddress.getResource());
            if (protocol instanceof HttpProtocol) {
                IoSessionIdleTracker inactivityTracker =
                             wsebSession.getInactivityTimeout() > 0 ?  currentSessionInactivityTracker.get() : null;
                if ( DOWNSTREAM_SUFFIX.equals(downstreamSuffix) ) {
                    return new WsebDownstreamHandler(address, wsebSession, scheduler,
                                                     WsebEncodingStrategy.TEXT_AS_BINARY, inactivityTracker, bridgeServiceFactory);

                } else if (DOWNSTREAM_TEXT_SUFFIX.equals(downstreamSuffix)) {
                    return new WsebDownstreamHandler(address, wsebSession, scheduler, "text/plain; charset=windows-1252",
                                  WsebEncodingStrategy.TEXT_AS_BINARY, inactivityTracker, bridgeServiceFactory);

                } else if (DOWNSTREAM_TEXT_ESCAPED_SUFFIX.equals(downstreamSuffix)) {
                    return new WsebDownstreamHandler(address, wsebSession, scheduler, "text/plain; charset=windows-1252",
                                  Encoding.ESCAPE_ZERO_AND_NEWLINE, WsebEncodingStrategy.TEXT_AS_BINARY, inactivityTracker, bridgeServiceFactory);

                } else if ( DOWNSTREAM_MIXED_SUFFIX.equals(downstreamSuffix) ) {
                    return new WsebDownstreamHandler(address, wsebSession, scheduler,
                                                     WsebEncodingStrategy.DEFAULT, inactivityTracker, bridgeServiceFactory);

                } else if (DOWNSTREAM_MIXED_TEXT_SUFFIX.equals(downstreamSuffix)) {
                    return new WsebDownstreamHandler(address, wsebSession, scheduler, "text/plain; charset=windows-1252",
                                  WsebEncodingStrategy.DEFAULT, inactivityTracker, bridgeServiceFactory);

                } else if (DOWNSTREAM_MIXED_TEXT_ESCAPED_SUFFIX.equals(downstreamSuffix)) {
                    wsebSession.setEncodeEscapeType(EscapeTypes.ESCAPE_ZERO_AND_NEWLINES);  //cache key
                    return new WsebDownstreamHandler(address, wsebSession, scheduler, "text/plain; charset=windows-1252",
                                  Encoding.ESCAPE_ZERO_AND_NEWLINE, WsebEncodingStrategy.DEFAULT, inactivityTracker, bridgeServiceFactory);
                }
            }

            throw new RuntimeException("Cannot locate a downstream handler for transport address "+address);
        }

        @Override
        protected void doExceptionCaught(HttpAcceptSession session, Throwable cause) throws Exception {
            // HTTP exception occurred, usually due to underlying connection failure during HTTP response
            WsebSession wseSession = SESSION_KEY.get(session);
            if (wseSession != null && !wseSession.isClosing()) {
                wseSession.reset(cause);
            }
            else {
                if (logger.isDebugEnabled()) {
                    String message = format("Exception while handling HttpSession to create WsebSession: %s", cause);
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

        protected ResourceAddress getWseLocalAddress(HttpAcceptSession session,
                                                     String nextProtocol) {


            URI resource = session.getLocalAddress().getResource();
            if (resource.getPath().endsWith(CREATE_SUFFIX)) {
                resource = truncateURI(resource, CREATE_SUFFIX);
            }
            if (resource.getPath().endsWith(CREATE_TEXT_SUFFIX)) {
                resource = truncateURI(resource, CREATE_TEXT_SUFFIX);
            }
            if (resource.getPath().endsWith(CREATE_TEXT_ESCAPED_SUFFIX)) {
                resource = truncateURI(resource, CREATE_TEXT_ESCAPED_SUFFIX);
            }
            if (resource.getPath().endsWith(CREATE_MIXED_SUFFIX)) {
                resource = truncateURI(resource, CREATE_MIXED_SUFFIX);
            }
            if (resource.getPath().endsWith(CREATE_MIXED_TEXT_SUFFIX)) {
                resource = truncateURI(resource, CREATE_MIXED_TEXT_SUFFIX);
            }
            if (resource.getPath().endsWith(CREATE_MIXED_TEXT_ESCAPED_SUFFIX)) {
                resource = truncateURI(resource, CREATE_MIXED_TEXT_ESCAPED_SUFFIX);
            }

            ResourceOptions options = ResourceOptions.FACTORY.newResourceOptions();
            options.setOption(TRANSPORT, session.getLocalAddress().resolve(resource.getPath()));
            options.setOption(NEXT_PROTOCOL, nextProtocol);

            URI wseLocalAddressLocation = modifyURIScheme(resource, "ws");

            ResourceAddress candidate = resourceAddressFactory.newResourceAddress(
                    wseLocalAddressLocation, options);

            Binding binding = bindings.getBinding(candidate);

            if (binding == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("\n" +
                                 "***FAILED to find local address via candidate:\n" +
                                 candidate +
                                 "\n***with bindings [\n" +
                                 bindings +
                                 "]");
                }
                return null;
            }

            if (logger.isTraceEnabled() && binding != null) {
                logger.trace("\n***Found local address for WSE session:\n" +
                             binding.bindAddress() +
                             "\n***via candidate:\n" +
                             candidate +
                             "\n***with bindings " +
                             bindings);
            }

            return binding.bindAddress();
        }


        // TODO: move this to a helper?
        private URI locateSecureAcceptURI(HttpAcceptSession session) throws Exception {
            // TODO: same-origin requests must consider cross-origin access control
            // internal redirect to secure resource should not trigger 403 Forbidden
            ResourceAddress localAddress = session.getLocalAddress();
            URI resource = localAddress.getResource();
            Protocol resourceProtocol = bridgeServiceFactory.getTransportFactory().getProtocol(resource);
            if (WsebProtocol.WSEB_SSL == resourceProtocol || WsProtocol.WSS == resourceProtocol) {
                return resource;
            }
            return null;
        }

        private class NoSecurityResourceOptions implements ResourceOptions {
            private final ResourceOptions options;

            public NoSecurityResourceOptions(ResourceAddress defaultsAddress) {
                options = ResourceOptions.FACTORY.newResourceOptions(defaultsAddress);
            }

            @Override
            public <T> T setOption(ResourceOption<T> key, T value) {
                if (key == HttpResourceAddress.REALM_NAME) return null;
                return options.setOption(key,value);
            }

            @Override
            public <T> T getOption(ResourceOption<T> key) {
                if (key == HttpResourceAddress.REALM_NAME) return null;
                return options.getOption(key);
            }

            @Override
            public <T> boolean hasOption(ResourceOption<T> key) {
                if (key == HttpResourceAddress.REALM_NAME) return false;
                return options.hasOption(key);
            }
        }
    }

    private final IoHandler cookiesHandler = new IoHandlerAdapter<HttpAcceptSession>() {

        @Override
        protected void doSessionOpened(final HttpAcceptSession httpSession) throws Exception {

            // include Location header to expose redirected cookie preflight before native WebSocket connection
            URI requestURI = httpSession.getRequestURI();
            String scheme = httpSession.isSecure() ? "https" : "http";
            String authority = httpSession.getReadHeader("Host");
            URI locationURI = new URI(scheme, authority, requestURI.getPath(), requestURI.getQuery(), requestURI.getFragment());

            httpSession.setWriteHeader("Content-Type", "text/plain; charset=UTF-8");
            httpSession.setWriteHeader("Location", locationURI.toString());
            httpSession.setStatus(HttpStatus.SUCCESS_OK);


            // check for all cookies
            // return cookie header as payload, if present
            String cookieHeader =  httpSession.getReadHeader("Cookie");
            if (cookieHeader != null && cookieHeader.length() > 0) {
                IoBufferAllocatorEx<?> allocator = httpSession.getBufferAllocator();
                IoBufferEx buf = allocator.wrap(allocator.allocate(cookieHeader.length())).setAutoExpander(allocator);
                CharsetEncoder utf8Encoder = UTF_8.newEncoder();
                buf.putString(cookieHeader, utf8Encoder);
                buf.flip();

                // The following writes a buffer, but we don't have access to an HttpContentMessage to mark it complete
                // However, the httpSession.close (despite waiting until the flush complete) races to close the pipeline
                // before the message is written downstream.  Because the response ends up being chunked, the zero chunk
                // is never written out, and Java Applet does not tolerate the missing zero chunk, preventing authentication
                // from working (KG-1730).
                WriteFuture future = httpSession.write(buf);
                future.addListener(new IoFutureListener<IoFuture>() {
                    @Override
                    public void operationComplete(IoFuture future) {
                        CommitFuture commitFuture = httpSession.commit();
                        commitFuture.addListener(new IoFutureListener<IoFuture>() {
                            @Override
                            public void operationComplete(IoFuture future) {
                                httpSession.close(false);
                            }
                        });
                    }
                });
            }
            else {
                httpSession.close(false);
            }
        }
    };

    private final IoHandler createHandler = new WsebCreateHandler(CREATE_SUFFIX, DOWNSTREAM_SUFFIX, UPSTREAM_SUFFIX);

    private final IoHandler createTextHandler = new WsebCreateHandler(CREATE_TEXT_SUFFIX, DOWNSTREAM_TEXT_SUFFIX, UPSTREAM_TEXT_SUFFIX);

    private final IoHandler createTextEscapedHandler = new WsebCreateHandler(CREATE_TEXT_ESCAPED_SUFFIX, DOWNSTREAM_TEXT_ESCAPED_SUFFIX, UPSTREAM_TEXT_ESCAPED_SUFFIX);

    private final IoHandler createMixedHandler = new WsebCreateHandler(CREATE_MIXED_SUFFIX, DOWNSTREAM_MIXED_SUFFIX, UPSTREAM_MIXED_SUFFIX);

    private final IoHandler createMixedTextHandler = new WsebCreateHandler(CREATE_MIXED_TEXT_SUFFIX, DOWNSTREAM_MIXED_TEXT_SUFFIX, UPSTREAM_MIXED_TEXT_SUFFIX);

    private final IoHandler createMixedTextEscapedHandler = new WsebCreateHandler(CREATE_MIXED_TEXT_ESCAPED_SUFFIX, DOWNSTREAM_MIXED_TEXT_ESCAPED_SUFFIX, UPSTREAM_MIXED_TEXT_ESCAPED_SUFFIX);

}
