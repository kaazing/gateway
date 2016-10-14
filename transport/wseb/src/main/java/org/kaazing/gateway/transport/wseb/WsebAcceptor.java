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
package org.kaazing.gateway.transport.wseb;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.kaazing.gateway.resource.address.ResourceAddress.ALTERNATE;
import static org.kaazing.gateway.resource.address.ResourceAddress.BIND_ALTERNATE;
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT;
import static org.kaazing.gateway.resource.address.URLUtils.appendURI;
import static org.kaazing.gateway.resource.address.URLUtils.ensureTrailingSlash;
import static org.kaazing.gateway.resource.address.URLUtils.modifyURIPath;
import static org.kaazing.gateway.resource.address.URLUtils.modifyURIScheme;
import static org.kaazing.gateway.resource.address.URLUtils.truncateURI;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.INACTIVITY_TIMEOUT;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.MAX_MESSAGE_SIZE;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CACHE_CONTROL;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CONTENT_LENGTH;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CONTENT_TYPE;
import static org.kaazing.gateway.transport.ws.WsSystemProperty.WSE_IDLE_TIMEOUT;
import static org.kaazing.gateway.transport.ws.bridge.filter.WsCheckAliveFilter.DISABLE_INACTIVITY_TIMEOUT;
import static org.kaazing.gateway.util.InternalSystemProperty.WSE_SPECIFICATION;
import static org.kaazing.mina.core.future.DefaultUnbindFuture.combineFutures;

import java.io.IOException;
import java.net.ProtocolException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Resource;
import javax.security.auth.Subject;

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
import org.kaazing.gateway.resource.address.IdentityResolver;
import org.kaazing.gateway.resource.address.Protocol;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ResourceOption;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.resource.address.http.HttpRealmInfo;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.resource.address.uri.URIUtils;
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
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.TypedAttributeKey;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpAcceptor;
import org.kaazing.gateway.transport.http.HttpHeaders;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.gateway.transport.http.HttpProtocol;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpUtils;
import org.kaazing.gateway.transport.http.bridge.filter.HttpProtocolCompatibilityFilter;
import org.kaazing.gateway.transport.ws.WsAcceptor;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer;
import org.kaazing.gateway.transport.ws.extension.ExtensionHelper;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtension;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtensionFactory;
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
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.netty.IoSessionIdleTracker;
import org.kaazing.mina.netty.util.threadlocal.VicariousThreadLocal;

@SuppressWarnings("deprecation")
public class WsebAcceptor extends AbstractBridgeAcceptor<WsebSession, Binding> {

    public static final String WSE_VERSION = "wseb-1.0";

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

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    protected static final byte LINEFEED_BYTE = "\n".getBytes()[0];

    private static final TypedAttributeKey<WsebSession> SESSION_KEY =
            new TypedAttributeKey<>(WsebAcceptor.class, "wseSession");

    private static final TypedAttributeKey<String[]> SUPPORTED_PROTOCOLS =
            new TypedAttributeKey<>(WsebAcceptor.class, "supportedProtocols");

    // used to deal with fragmented wseb-create-message content
    private static final TypedAttributeKey<Integer> CREATE_CONTENT_LENGTH_READ =
            new TypedAttributeKey<>(WsebAcceptor.class, "createContentLengthRead");

    // GET is tolerated for non-spec compliant clients
    private static final EnumSet<HttpMethod> PERMITTED_CREATE_METHODS = EnumSet.of(HttpMethod.POST, HttpMethod.GET);

    private static final Long MAX_SEQUENCE_NUMBER = 0x1fffffffffffffL; // 2 ^ 53 - 1

    private Properties configuration;
    private boolean specCompliant;

    private ScheduledExecutorService scheduler;
    private BridgeServiceFactory bridgeServiceFactory;
    private ResourceAddressFactory resourceAddressFactory;

    private WebSocketExtensionFactory webSocketExtensionFactory;

    private final List<IoSessionIdleTracker> sessionInactivityTrackers
        = Collections.synchronizedList(new ArrayList<>());
    private final ThreadLocal<IoSessionIdleTracker> currentSessionIdleTracker
        = new VicariousThreadLocal<IoSessionIdleTracker>() {
            @Override
            protected IoSessionIdleTracker initialValue() {
                IoSessionIdleTracker result = new WsebTransportSessionIdleTracker(logger);
                sessionInactivityTrackers.add(result);
                return result;
            }
    };

    private final IoHandler createHandler = new WsebCreateHandler();

    public WsebAcceptor() {
        super(new DefaultIoSessionConfigEx());
    }

    @Override
    protected IoProcessorEx<WsebSession> initProcessor() {
        return new WsebAcceptProcessor(scheduler, logger);
    }

    @Resource(name = "configuration")
    public void setConfiguration(Properties configuration) {
        this.configuration = configuration;
        this.specCompliant = "true".equals(WSE_SPECIFICATION.getProperty(configuration));
    }

    @Resource(name = "bridgeServiceFactory")
    public void setBridgeServiceFactory(BridgeServiceFactory bridgeServiceFactory) {
        this.bridgeServiceFactory = bridgeServiceFactory;
    }

    @Resource(name = "resourceAddressFactory")
    public void setResourceAddressFactory(ResourceAddressFactory resourceAddressFactory) {
        this.resourceAddressFactory = resourceAddressFactory;
    }

    @Resource(name = "ws.acceptor")
    public void setWsAcceptor(WsAcceptor acceptor) {
        this.webSocketExtensionFactory = acceptor.getWebSocketExtensionFactory();
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

            bindApiPath(address);

            BridgeAcceptor transportAcceptor = bridgeServiceFactory.newBridgeAcceptor(transportAddress);
            transportAcceptor.bind(transportAddress, createHandler, wrapperHttpInitializer);

        } catch (Exception e) {
            throw new RuntimeException("Unable to bind address " + address + ": " + e.getMessage(),e );
        }

    }

    private void bindApiPath(ResourceAddress address) {
        ResourceAddress apiHttpAddress = createApiHttpAddress(address.getTransport());
        bridgeServiceFactory.newBridgeAcceptor(apiHttpAddress).bind(apiHttpAddress, WsAcceptor.API_PATH_HANDLER, null);

        ResourceAddress apiHttpxeAddress = address.getTransport().getOption(ALTERNATE);
        if (apiHttpxeAddress != null) {
            apiHttpxeAddress = createApiHttpxeAddress(apiHttpxeAddress);
            bridgeServiceFactory.newBridgeAcceptor(apiHttpxeAddress).bind(apiHttpxeAddress, WsAcceptor.API_PATH_HANDLER, null);
        }
    }

    private void unbindApiPath(ResourceAddress address) {
        ResourceAddress apiHttpAddress = createApiHttpAddress(address.getTransport());
        bridgeServiceFactory.newBridgeAcceptor(apiHttpAddress).unbind(apiHttpAddress);

        ResourceAddress apiHttpxeAddress = address.getTransport().getOption(ALTERNATE);
        if (apiHttpxeAddress != null) {
            apiHttpxeAddress = createApiHttpxeAddress(apiHttpxeAddress);
            bridgeServiceFactory.newBridgeAcceptor(apiHttpxeAddress).unbind(apiHttpxeAddress);
        }
    }

    private ResourceAddress createApiHttpAddress(ResourceAddress httpTransport) {
        String path = URIUtils.getPath(appendURI(ensureTrailingSlash(httpTransport.getExternalURI()), HttpProtocolCompatibilityFilter.API_PATH));

        String httpLocation = modifyURIPath(httpTransport.getExternalURI(), path);
        ResourceOptions httpOptions = ResourceOptions.FACTORY.newResourceOptions(httpTransport);
        httpOptions.setOption(NEXT_PROTOCOL, null);       // terminal endpoint, so next protocol null
        httpOptions.setOption(ALTERNATE, null);
        return resourceAddressFactory.newResourceAddress(httpLocation, httpOptions);
    }

    private ResourceAddress createApiHttpxeAddress(ResourceAddress httpxeTransport) {
        String path = URIUtils.getPath(appendURI(ensureTrailingSlash(httpxeTransport.getExternalURI()), HttpProtocolCompatibilityFilter.API_PATH));

        httpxeTransport = httpxeTransport.resolve(path);
        String httpxeLocation = modifyURIPath(httpxeTransport.getExternalURI(), path);
        ResourceOptions httpxeOptions = ResourceOptions.FACTORY.newResourceOptions(httpxeTransport);
        httpxeOptions.setOption(NEXT_PROTOCOL, null);       // terminal endpoint, so next protocol null
        return resourceAddressFactory.newResourceAddress(httpxeLocation, httpxeOptions);
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
        return resourceAddressFactory.newResourceAddress(URIUtils.uriToString(cookiesLocation), cookieOptions);
    }

    @Override
    protected UnbindFuture unbindInternal(ResourceAddress address, IoHandler handler,
            BridgeSessionInitializer<? extends IoFuture> initializer) {

        unbindApiPath(address);

        final ResourceAddress transportAddress = address.getTransport();
        BridgeAcceptor acceptor = bridgeServiceFactory.newBridgeAcceptor(transportAddress);

        UnbindFuture future = unbindCookiesHandler(address.findTransport("http[http/1.1]"));
        future = combineFutures(future, acceptor.unbind(transportAddress));
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

    private String createResolvePath(String httpUri, final String suffixWithLeadingSlash) {
        return URIUtils.getPath(appendURI(ensureTrailingSlash(httpUri),suffixWithLeadingSlash));
    }

    private static final ExtensionHelper extensionHelper = new ExtensionHelper() {

        @Override
        public void setLoginContext(IoSession session, ResultAwareLoginContext loginContext) {
            ((WsebSession.TransportSession)session).getWsebSession().setLoginContext(loginContext);
        }

        @Override
        public void closeWebSocketConnection(IoSession session) {
            ((WsebSession.TransportSession)session).getWsebSession().close(false);
        }

    };

    final class WsebCreateHandler extends IoHandlerAdapter<HttpAcceptSession> {

        private IoFutureListener<CloseFuture> getWsebCloseListener(final BridgeAcceptor upstreamAcceptor,
                                                                   final BridgeAcceptor downstreamAcceptor,
                                                                   final WsebSession wsebSession,
                                                                   final ResourceAddress downstreamAddress,
                                                                   final ResourceAddress upstreamAddress) {
            return new IoFutureListener<CloseFuture>() {
                @Override
                public void operationComplete(CloseFuture future) {
                    currentSessionIdleTracker.get().removeSession(wsebSession);

                    downstreamAcceptor.unbind(downstreamAddress);
                    upstreamAcceptor.unbind(upstreamAddress);

                    wsebSession.shutdownScheduledCommands();
                    wsebSession.logout();
                }
            };
        }

        @Override
        protected void doSessionOpened(final HttpAcceptSession session) throws Exception {
            if (!PERMITTED_CREATE_METHODS.contains(session.getMethod())) {
                HttpStatus status = HttpStatus.CLIENT_BAD_REQUEST;
                session.setStatus(status);
                session.setReason("Forbidden wse create request method " + session.getMethod());
                session.setWriteHeader(HEADER_CONTENT_LENGTH, "0");
                session.close(false);
                return;
            }
            if (specCompliant && !validateSequenceNumber(session)) {
                return;
            }
            if (! validateWsebVersion(session)) {
                return;
            }
            if (!validateAcceptCommands(session)) {
                return;
            }

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
                super.doMessageReceived(session, message);
            }
        }

        private void createWsebSessionAndFinalizeResponse(final HttpAcceptSession session) throws Exception {
            String createSuffix;
            String downstreamSuffix;
            String upstreamSuffix;

            String path = session.getPathInfo().getPath();
            if (CREATE_SUFFIX.endsWith(path)) {
                createSuffix = CREATE_SUFFIX;
                downstreamSuffix = DOWNSTREAM_SUFFIX;
                upstreamSuffix = UPSTREAM_SUFFIX;
            } else if (CREATE_TEXT_SUFFIX.endsWith(path)) {
                createSuffix = CREATE_TEXT_SUFFIX;
                downstreamSuffix = DOWNSTREAM_TEXT_SUFFIX;
                upstreamSuffix = UPSTREAM_TEXT_SUFFIX;
            } else if (CREATE_TEXT_ESCAPED_SUFFIX.endsWith(path)) {
                createSuffix = CREATE_TEXT_ESCAPED_SUFFIX;
                downstreamSuffix = DOWNSTREAM_TEXT_ESCAPED_SUFFIX;
                upstreamSuffix = UPSTREAM_TEXT_ESCAPED_SUFFIX;
            } else if (CREATE_MIXED_SUFFIX.endsWith(path)) {
                createSuffix = CREATE_MIXED_SUFFIX;
                downstreamSuffix = DOWNSTREAM_MIXED_SUFFIX;
                upstreamSuffix = UPSTREAM_MIXED_SUFFIX;
            } else if (CREATE_MIXED_TEXT_SUFFIX.endsWith(path)) {
                createSuffix = CREATE_MIXED_TEXT_SUFFIX;
                downstreamSuffix = DOWNSTREAM_MIXED_TEXT_SUFFIX;
                upstreamSuffix = UPSTREAM_MIXED_TEXT_SUFFIX;
            } else if (CREATE_MIXED_TEXT_ESCAPED_SUFFIX.endsWith(path)) {
                createSuffix = CREATE_MIXED_TEXT_ESCAPED_SUFFIX;
                downstreamSuffix = DOWNSTREAM_MIXED_TEXT_ESCAPED_SUFFIX;
                upstreamSuffix = UPSTREAM_MIXED_TEXT_ESCAPED_SUFFIX;
            } else {
                logger.info(String.format("Sending HTTP status 404 as the request=%s is not wse create request", path));
                session.setStatus(HttpStatus.CLIENT_NOT_FOUND);
                session.close(false);
                return;
            }

            String sequenceStr = session.getReadHeader(HttpHeaders.HEADER_X_SEQUENCE_NO);
            final boolean validateSequenceNo = (sequenceStr != null);
            final long sequenceNo = validateSequenceNo ? Long.parseLong(sequenceStr) : -1;

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

            if (wseLocalAddress == null) {
                logger.info("Sending HTTP status 404 as no local address is found");
                session.setStatus(HttpStatus.CLIENT_NOT_FOUND);
                session.close(false);
                return;
            }

            final ResourceAddress localAddress = wseLocalAddress;

            final String wsProtocol0 = wsProtocol;
            List<String> clientRequestedExtensions =  session.getReadHeaders(WsUtils.HEADER_X_WEBSOCKET_EXTENSIONS);

            // negotiate extensions
            final List<WebSocketExtension> negotiated;
            try {
                negotiated = WsUtils.negotiateExtensionsAndSetResponseHeader(
                        webSocketExtensionFactory, (WsResourceAddress) wseLocalAddress, clientRequestedExtensions,
                        session, WsUtils.HEADER_X_WEBSOCKET_EXTENSIONS, extensionHelper);
            }
            catch(ProtocolException e) {
                WsUtils.handleExtensionNegotiationException(session, clientRequestedExtensions, e, logger);
                return;
            }

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

            String sessionId = HttpUtils.newSessionId();

            final IoBufferAllocatorEx<WsBuffer> allocator = new WsebBufferAllocator(session.getBufferAllocator());

            // create new address to use as key and session remote address
            ResourceAddress remoteBridgeAddress = BridgeSession.REMOTE_ADDRESS.get(session);
            URI remoteLocation = ensureTrailingSlash(modifyURIScheme(remoteBridgeAddress.getResource(), "wse"));
            ResourceOptions options = ResourceOptions.FACTORY.newResourceOptions();
            options.setOption(TRANSPORT, remoteBridgeAddress);
            final ResourceAddress remoteAddress =  resourceAddressFactory.newResourceAddress(URIUtils.uriToString(remoteLocation), options, sessionId);


            final URI httpUri = session.getRequestURL();
            if (!httpUri.getPath().contains(createSuffix)) {
                throw new IllegalStateException("Session created with unexpected URL: "+httpUri.toASCIIString());
            }

            // We can only honor inactivity timeout if the client supports PING
            String acceptCommands = session.getReadHeader(HttpHeaders.HEADER_X_ACCEPT_COMMANDS);
            boolean pingEnabled = acceptCommands != null && acceptCommands.equals("ping");
            long configuredInactivityTimeout = localAddress.getOption(INACTIVITY_TIMEOUT);
            final long inactivityTimeout =
                    pingEnabled ? configuredInactivityTimeout : DISABLE_INACTIVITY_TIMEOUT;

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
                    WsebSession wsebSession = (WsebSession)wsSession;
                    wsSession.setAttribute(BridgeSession.NEXT_PROTOCOL_KEY, wsProtocol0);
                    wsebSession.setLoginContext(session.getLoginContext());
                    wsebSession.setPingEnabled(pingEnabled);
                    IoSessionEx extensionsSession = wsebSession.getTransportSession();
                    IoFilterChain extensionsFilterChain = extensionsSession.getFilterChain();

                    // we don't want IdleTimeoutFilter in the filter chain. It sends 0x8A (PONG)
                    // and some clients don't understand it. WSEB anyway sends NOOP command when
                    // writer is idle.
                    List<WebSocketExtension> negotiatedCopy = new ArrayList<>(negotiated);
                    negotiatedCopy.removeIf(e -> e.getExtensionHeader().getExtensionToken().equals("x-kaazing-idle-timeout"));
                    WsUtils.addExtensionFilters(negotiatedCopy, extensionHelper, extensionsFilterChain, false);

                    extensionsFilterChain.fireSessionCreated();
                    extensionsFilterChain.fireSessionOpened();
                }
            }, new Callable<WsebSession>() {
                @Override
                public WsebSession call() {
                    ResultAwareLoginContext loginContext = session.getLoginContext();
                    WsebSession newWsebSession = new WsebSession(session.getIoLayer(), session.getIoThread(), session.getIoExecutor(), WsebAcceptor.this, getProcessor(),
                            localAddress, remoteAddress, allocator, loginContext, clientIdleTimeout, inactivityTimeout,
                            validateSequenceNo, sequenceNo, negotiated, logger, configuration);
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
            String remoteExternalHttp11 = remoteHttp11Address.getExternalURI();
            String localExternalHttp11 = localHttp11Address.getExternalURI();

            final String sessionIdSuffix = '/' + sessionId;
            // add path suffixes for upstream and downstream URLs relative to local bind path
            // but retain query parameters
            // Note: to integrate with cross-site access control,
            // it is important for suffixes to be hierarchical
            // (begin with forward slash)

            URI remoteExternalDownstream = new URI(URIUtils.getScheme(remoteExternalHttp11),
                                                   URIUtils.getUserInfo(remoteExternalHttp11),
                                                   URIUtils.getHost(remoteExternalHttp11),
                                                   URIUtils.getPort(remoteExternalHttp11),
                                                   createResolvePath(localExternalHttp11, downstreamSuffix + sessionIdSuffix),
                                                   URIUtils.getQuery(remoteExternalHttp11),
                                                   URIUtils.getFragment(remoteExternalHttp11));

            URI remoteExternalUpstream = new URI(URIUtils.getScheme(remoteExternalHttp11),
                                                 URIUtils.getUserInfo(remoteExternalHttp11),
                                                 URIUtils.getHost(remoteExternalHttp11),
                                                 URIUtils.getPort(remoteExternalHttp11),
                                                 createResolvePath(localExternalHttp11, upstreamSuffix + sessionIdSuffix),
                                                 URIUtils.getQuery(remoteExternalHttp11),
                                                 URIUtils.getFragment(remoteExternalHttp11));

            //
            // UP- and DOWN- STREAMS: BIND
            //
            final ResourceAddress httpAddress = localAddress.getTransport();
            final ResourceAddress httpxeAddress = localAddress.getTransport().getOption(ALTERNATE);

            // upstream and downstream requests shouldn't go through authentication/authorization
            // as the create request already went through it and established wseb session
         // But for logging purposes we do want to set an IdentityResolver
            String wsebSessionIdentity = format("%s#%d", getTransportMetadata().getName(), wsebSession.getId());
            final IdentityResolver downstreamResolver = new FixedIdentityResolver(wsebSessionIdentity + "d");
            // tcp | http | httpxe | wse - apply no security to http layer
            ResourceAddress httpxeBaseAddress = httpxeAddressNoSecurity(httpxeAddress, downstreamResolver);

            // tcp | http | wse - apply no security to http layer, also sets the httpxe alternate
            ResourceAddress httpBaseAddress = httpAddressNoSecurity(httpAddress, httpxeBaseAddress, downstreamResolver);

            ResourceAddress localDownstream = httpBaseAddress.resolve(createResolvePath(httpBaseAddress.getResource(), downstreamSuffix + sessionIdSuffix));
            logger.trace("Binding "+localDownstream.getTransport()+" to downstreamHandler");

            // Now repeat for upstream
            final IdentityResolver upstreamResolver = new FixedIdentityResolver(wsebSessionIdentity + "u");
            httpxeBaseAddress = httpxeAddressNoSecurity(httpxeAddress, upstreamResolver);
            httpBaseAddress = httpAddressNoSecurity(httpAddress, httpxeBaseAddress, upstreamResolver);

            ResourceAddress localUpstream = httpBaseAddress.resolve(createResolvePath(httpBaseAddress.getResource(), upstreamSuffix + sessionIdSuffix));
            logger.trace("Binding "+localUpstream.getTransport()+" to upstreamHandler");

            BridgeAcceptor downstreamAcceptor = bridgeServiceFactory.newBridgeAcceptor(localDownstream);
            downstreamAcceptor.bind(localDownstream,
                    selectDownstreamHandler(localAddress, wsebSession, downstreamSuffix),
                    null);

            BridgeAcceptor upstreamAcceptor = bridgeServiceFactory.newBridgeAcceptor(localUpstream);
            upstreamAcceptor.bind(localUpstream,
                selectUpstreamHandler(localAddress, wsebSession, upstreamSuffix),
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
            session.setWriteHeader(HEADER_CACHE_CONTROL, "no-cache");
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

        private ResourceAddress httpAddressNoSecurity(ResourceAddress httpAddress, ResourceAddress httpxeAddressNoSecurity, IdentityResolver resolver) {
            ResourceOptions noSecurityOptions = new NoSecurityResourceOptions(httpAddress);
            noSecurityOptions.setOption(ALTERNATE, httpxeAddressNoSecurity);

            noSecurityOptions.setOption(ResourceAddress.IDENTITY_RESOLVER, resolver);
            noSecurityOptions.setOption(HttpResourceAddress.REALMS, new HttpRealmInfo[0]);
            return resourceAddressFactory.newResourceAddress(httpAddress.getExternalURI(),
                    noSecurityOptions, httpAddress.getOption(ResourceAddress.QUALIFIER));
        }

        private ResourceAddress httpxeAddressNoSecurity(ResourceAddress httpxeAddress, IdentityResolver resolver) {
            // Remove REALM_NAME option at http layer (upstream and downstream requests shouldn't have to
            // go through authentication/authorization)
            ResourceAddress httpAddress = httpxeAddress.getTransport();
            ResourceOptions noSecurityOptions = new NoSecurityResourceOptions(httpAddress);

            noSecurityOptions.setOption(ResourceAddress.IDENTITY_RESOLVER, resolver);
            ResourceAddress httpAddressNoSecurity = resourceAddressFactory.newResourceAddress(
                    httpAddress.getExternalURI(), noSecurityOptions, httpAddress.getOption(ResourceAddress.QUALIFIER));

            // Remove REALM_NAME  option at httpxe layer but preserve all other options like
            // ORIGIN_SECURITY etc. Otherwise, upstream and downstream requests will be subjected
            // to different origin security constraints. Then finally add http as transport to httpxe
            ResourceOptions httpxeOptions = ResourceOptions.FACTORY.newResourceOptions(httpxeAddress);
            httpxeOptions.setOption(TRANSPORT, httpAddressNoSecurity);

            httpxeOptions.setOption(ResourceAddress.IDENTITY_RESOLVER, resolver);

            httpxeOptions = new NoSecurityResourceOptions(httpxeOptions);
            return resourceAddressFactory.newResourceAddress(URIUtils.uriToString(httpxeAddress.getResource()), httpxeOptions);
        }

        private boolean validateAcceptCommands(HttpAcceptSession session) {
            String commands = session.getReadHeader("X-Accept-Commands");
            if (commands != null && !"ping".equals(commands)) {
               session.setStatus(HttpStatus.CLIENT_BAD_REQUEST);
               session.setReason("X-Accept-Commands header value is invalid: " + commands);
               session.setWriteHeader(HEADER_CONTENT_LENGTH, "0");
               session.close(false);
               return false;
            }
            return true;
        }

        private boolean validateSequenceNumber(HttpAcceptSession session) {
            String sequenceHeader = session.getReadHeader("X-Sequence-No");
            if (!specCompliant) {
                return true;
            }
            boolean valid = sequenceHeader != null;
            if (valid)
            {
                try {
                    int sequence = Integer.parseInt(sequenceHeader);
                    if (sequence < 0 || sequence > MAX_SEQUENCE_NUMBER) {
                        valid = false;
                    }
                }
                catch (NumberFormatException e) {
                    valid = false;
                }
            }
            if (!valid)
            {
               session.setStatus(HttpStatus.CLIENT_BAD_REQUEST);
               session.setReason("X-Sequence-Nop header missing or invalid");
               session.setWriteHeader(HEADER_CONTENT_LENGTH, "0");
               session.close(false);
            }
            return valid;
        }

        private boolean validateWsebVersion(HttpAcceptSession session) {
            String wsebVersion = session.getReadHeader("X-WebSocket-Version");
            if (specCompliant && wsebVersion == null) {
                   session.setStatus(HttpStatus.CLIENT_BAD_REQUEST);
                   session.setReason("X-WebSocket-Version header missing");
                   session.setWriteHeader(HEADER_CONTENT_LENGTH, "0");
                   session.close(false);
                   return false;
               }
            if (wsebVersion != null && !wsebVersion.equals(WSE_VERSION)) {
                session.setStatus(HttpStatus.CLIENT_BAD_REQUEST);
                session.setReason("WebSocket-Version not supported");
                session.setWriteHeader(HEADER_CONTENT_LENGTH, "0");
                session.close(false);
                return false;
            }
            return true;
        }

        private IoHandler selectUpstreamHandler(ResourceAddress address,
                                                WsebSession wsebSession, String upstreamSuffix) {
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
                                                  WsebSession wsebSession, String downstreamSuffix) {
            ResourceAddress transportAddress = address.getTransport();
            final Protocol protocol = bridgeServiceFactory.getTransportFactory().getProtocol(transportAddress.getResource());
            if (protocol instanceof HttpProtocol) {
                // We need a session idle tracker to handle ws close handshake, even if ws.inactivity.timeout is not set
                IoSessionIdleTracker sesionIdleTracker = currentSessionIdleTracker.get();
                if ( DOWNSTREAM_SUFFIX.equals(downstreamSuffix) ) {
                    return new WsebDownstreamHandler(address, wsebSession, scheduler,
                                                     WsebEncodingStrategy.TEXT_AS_BINARY, sesionIdleTracker, bridgeServiceFactory);

                } else if (DOWNSTREAM_TEXT_SUFFIX.equals(downstreamSuffix)) {
                    return new WsebDownstreamHandler(address, wsebSession, scheduler, "text/plain; charset=windows-1252",
                                  WsebEncodingStrategy.TEXT_AS_BINARY, sesionIdleTracker, bridgeServiceFactory);

                } else if (DOWNSTREAM_TEXT_ESCAPED_SUFFIX.equals(downstreamSuffix)) {
                    return new WsebDownstreamHandler(address, wsebSession, scheduler, "text/plain; charset=windows-1252",
                                  Encoding.ESCAPE_ZERO_AND_NEWLINE, WsebEncodingStrategy.TEXT_AS_BINARY, sesionIdleTracker, bridgeServiceFactory);

                } else if ( DOWNSTREAM_MIXED_SUFFIX.equals(downstreamSuffix) ) {
                    return new WsebDownstreamHandler(address, wsebSession, scheduler,
                                                     WsebEncodingStrategy.DEFAULT, sesionIdleTracker, bridgeServiceFactory);

                } else if (DOWNSTREAM_MIXED_TEXT_SUFFIX.equals(downstreamSuffix)) {
                    return new WsebDownstreamHandler(address, wsebSession, scheduler, "text/plain; charset=windows-1252",
                                  WsebEncodingStrategy.DEFAULT, sesionIdleTracker, bridgeServiceFactory);

                } else if (DOWNSTREAM_MIXED_TEXT_ESCAPED_SUFFIX.equals(downstreamSuffix)) {
                    wsebSession.setEncodeEscapeType(EscapeTypes.ESCAPE_ZERO_AND_NEWLINES);  //cache key
                    return new WsebDownstreamHandler(address, wsebSession, scheduler, "text/plain; charset=windows-1252",
                                  Encoding.ESCAPE_ZERO_AND_NEWLINE, WsebEncodingStrategy.DEFAULT, sesionIdleTracker, bridgeServiceFactory);
                }
            }

            throw new RuntimeException("Cannot locate a downstream handler for transport address "+address);
        }

        @Override
        protected void doExceptionCaught(HttpAcceptSession session, Throwable cause) throws Exception {
            // HTTP exception occurred, usually due to underlying connection failure during HTTP response

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

        @Override
        protected void doSessionClosed(HttpAcceptSession session) throws Exception {
            WsebSession wsebSession = SESSION_KEY.remove(session);
            if (wsebSession != null && !wsebSession.isClosing()) {
                wsebSession.reset(new IOException("Network connectivity has been lost or transport was closed at other end").fillInStackTrace());
            }

            IoFilterChain filterChain = session.getFilterChain();
            removeBridgeFilters(filterChain);
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
                    URIUtils.uriToString(wseLocalAddressLocation), options);

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

        private class NoSecurityResourceOptions implements ResourceOptions {
            private final ResourceOptions options;

            public NoSecurityResourceOptions(ResourceOptions defaultsAddress) {
                options = ResourceOptions.FACTORY.newResourceOptions(defaultsAddress);
            }

            @Override
            public <T> T setOption(ResourceOption<T> key, T value) {
                if (key == HttpResourceAddress.REALMS) return null;
                return options.setOption(key,value);
            }

            @Override
            public <T> T getOption(ResourceOption<T> key) {
                if (key == HttpResourceAddress.REALMS) return null;
                return options.getOption(key);
            }

            @Override
            public <T> boolean hasOption(ResourceOption<T> key) {
                if (key == HttpResourceAddress.REALMS) return false;
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

            httpSession.setWriteHeader(HEADER_CONTENT_TYPE, "text/plain; charset=UTF-8");
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

    private static class FixedIdentityResolver extends IdentityResolver {
        final String identity;

        private FixedIdentityResolver(String identity) {
            this.identity = identity;
        }

        @Override
        public String resolve(Subject subject) {
            return identity;
        }

    }

}
