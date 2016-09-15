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
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static org.kaazing.gateway.resource.address.URLUtils.appendURI;
import static org.kaazing.gateway.resource.address.URLUtils.ensureTrailingSlash;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.INACTIVITY_TIMEOUT;
import static org.kaazing.gateway.transport.BridgeSession.REMOTE_ADDRESS;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CONTENT_LENGTH;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_CONTENT_TYPE;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_X_ACCEPT_COMMANDS;
import static org.kaazing.gateway.transport.ws.util.WsUtils.HEADER_X_WEBSOCKET_EXTENSIONS;
import static org.kaazing.gateway.transport.wseb.WsebAcceptor.WSE_VERSION;
import static org.kaazing.gateway.transport.wseb.util.WseUtils.HEADER_X_WEBSOCKET_PROTOCOL;
import static org.kaazing.gateway.util.InternalSystemProperty.WSE_SPECIFICATION;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

import javax.annotation.Resource;
import javax.security.auth.Subject;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.DefaultConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.gateway.resource.address.IdentityResolver;
import org.kaazing.gateway.resource.address.Protocol;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.resource.address.uri.URIUtils;
import org.kaazing.gateway.resource.address.ws.WsResourceAddress;
import org.kaazing.gateway.security.auth.context.ResultAwareLoginContext;
import org.kaazing.gateway.transport.AbstractBridgeConnector;
import org.kaazing.gateway.transport.BridgeConnector;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.DefaultIoSessionConfigEx;
import org.kaazing.gateway.transport.DefaultTransportMetadata;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.TypedAttributeKey;
import org.kaazing.gateway.transport.http.HttpConnectSession;
import org.kaazing.gateway.transport.http.HttpHeaders;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.gateway.transport.http.HttpProtocol;
import org.kaazing.gateway.transport.http.HttpSession;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.ResponseFuture;
import org.kaazing.gateway.transport.ws.Command;
import org.kaazing.gateway.transport.ws.WsCloseMessage;
import org.kaazing.gateway.transport.ws.WsCommandMessage;
import org.kaazing.gateway.transport.ws.WsConnector;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer;
import org.kaazing.gateway.transport.ws.extension.ExtensionHeaderBuilder;
import org.kaazing.gateway.transport.ws.extension.ExtensionHelper;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtension;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtensionFactory;
import org.kaazing.gateway.transport.wseb.filter.WsebBufferAllocator;
import org.kaazing.gateway.transport.wseb.filter.WsebFrameCodecFilter;
import org.kaazing.gateway.transport.wseb.util.WseUtils;
import org.kaazing.gateway.util.Utils;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.netty.IoSessionIdleTracker;
import org.kaazing.mina.netty.util.threadlocal.VicariousThreadLocal;

@SuppressWarnings("deprecation")
public class WsebConnector extends AbstractBridgeConnector<WsebSession> {

    private static final String CREATE_SUFFIX = WsebAcceptor.EMULATED_SUFFIX + "/cbm";

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private static final TypedAttributeKey<Callable<WsebSession>> WSE_SESSION_FACTORY_KEY = new TypedAttributeKey<>(WsebConnector.class, "wseSessionFactory");
    private static final TypedAttributeKey<ConnectFuture> WSE_CONNECT_FUTURE_KEY = new TypedAttributeKey<>(WsebConnector.class, "wseConnectFuture");
    private static final TypedAttributeKey<WsebSession> WSE_SESSION_KEY = new TypedAttributeKey<>(WsebConnector.class, "wseSession");

    private static final String CODEC_FILTER = WsebProtocol.NAME + "#codec";

    private BridgeServiceFactory bridgeServiceFactory;
    private ResourceAddressFactory resourceAddressFactory;
    private Properties configuration;
    private boolean specCompliant;

    private WebSocketExtensionFactory webSocketExtensionFactory;

    private static final ExtensionHelper extensionHelper = new ExtensionHelper() {

        @Override
        public void setLoginContext(IoSession session, ResultAwareLoginContext loginContext) {
            WsebSession wsebSession = WSE_SESSION_KEY.get(session);
            assert wsebSession !=  null;
            wsebSession.setLoginContext(loginContext);
        }

        @Override
        public void closeWebSocketConnection(IoSession session) {
            WsebSession wsebSession = WSE_SESSION_KEY.get(session);
            assert wsebSession !=  null;
            wsebSession.close(false);
        }

    };

    private static final IoFutureListener<ResponseFuture>
                CHECK_READER_RESPONSE_LISTENER = new IoFutureListener<ResponseFuture>() {

        @Override
        public void operationComplete(ResponseFuture future) {
            HttpConnectSession session = (HttpConnectSession) future.getSession();
            if (session.getStatus() != HttpStatus.SUCCESS_OK ||
                !session.getReadHeader(HEADER_CONTENT_TYPE).equals("application/octet-stream")) {
                String message = session.getStatus() != HttpStatus.SUCCESS_OK ?
                        "Unexpected upstream response status " + session.getStatus() :
                        "Unexpected upstream response content type " + session.getReadHeader(HEADER_CONTENT_TYPE);
                try {
                    session.getFilterChain().fireExceptionCaught(new IOException(message));
                } catch (Exception e) {
                    // Nothing much we can do
                }
            }

        }

    };

    private final List<IoSessionIdleTracker> sessionIdleTrackers
        = Collections.synchronizedList(new ArrayList<>());

    private final ThreadLocal<IoSessionIdleTracker> currentSessionIdleTracker
        = new VicariousThreadLocal<IoSessionIdleTracker>() {
        @Override
        protected IoSessionIdleTracker initialValue() {
            IoSessionIdleTracker result = new WsebTransportSessionIdleTracker(logger);
            sessionIdleTrackers.add(result);
            return result;
        }
    };

    public WsebConnector() {
        super(new DefaultIoSessionConfigEx());
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return new DefaultTransportMetadata(WsebProtocol.NAME);
    }

    @Resource(name = "bridgeServiceFactory")
    public void setBridgeServiceFactory(BridgeServiceFactory bridgeServiceFactory) {
        this.bridgeServiceFactory = bridgeServiceFactory;
    }

    @Resource(name = "configuration")
    public void setConfiguration(Properties configuration) {
        this.configuration = configuration;
        this.specCompliant = "true".equals(WSE_SPECIFICATION.getProperty(configuration));
    }

    @Resource(name = "resourceAddressFactory")
    public void setResourceAddressFactory(ResourceAddressFactory factory) {
        this.resourceAddressFactory = factory;
    }

    @Resource(name = "ws.connector")
    public void setWsConnector(WsConnector connector) {
        this.webSocketExtensionFactory = connector.getWebSocketExtensionFactory();
    }

    @Override
    protected IoProcessorEx<WsebSession> initProcessor() {
        return new WsebConnectProcessor(bridgeServiceFactory, logger, specCompliant);
    }

    @Override
    protected boolean canConnect(String transportName) {
        return transportName.equals("wse") || transportName.equals("ws");
    }

    @Override
    protected <T extends ConnectFuture> ConnectFuture connectInternal(ResourceAddress connectAddress, IoHandler handler,
                                                                      final IoSessionInitializer<T> initializer) {
        final DefaultConnectFuture wseConnectFuture = new DefaultConnectFuture();

        // propagate connection failure, if necessary
        IoFutureListener<ConnectFuture> parentConnectListener = new IoFutureListener<ConnectFuture>() {
            @Override
            public void operationComplete(ConnectFuture future) {
                // fail bridge connect future if parent connect fails
                if (!future.isConnected()) {
                    wseConnectFuture.setException(future.getException());
                }
            }
        };

        IoSessionInitializer<ConnectFuture> parentInitializer = createParentInitializer(connectAddress,
                                                                                        handler,
                                                                                        initializer,
                                                                                        wseConnectFuture);

        ResourceAddress httpxeAddress = connectAddress.getTransport();
        String uri = appendURI(ensureTrailingSlash(httpxeAddress.getExternalURI()), CREATE_SUFFIX);
        String query = URIUtils.getQuery(uri);
        String pathAndQuery = URIUtils.getPath(uri);
        if (query != null) {
            pathAndQuery += "?" + URIUtils.getQuery(uri);
        }
        ResourceAddress createAddress = httpxeAddress.resolve(pathAndQuery);
        BridgeConnector connector = bridgeServiceFactory.newBridgeConnector(createAddress);

        // TODO: proxy detection, append ?.ki=p on timeout
        connector.connect(createAddress, selectConnectHandler(createAddress), parentInitializer).addListener(parentConnectListener);

        return wseConnectFuture;
    }

    @Override
    protected IoFuture dispose0() throws Exception {
        for (IoSessionIdleTracker tracker : sessionIdleTrackers) {
            tracker.dispose();
        }
        return super.dispose0();
    }

    private IoHandler selectConnectHandler(ResourceAddress address) {
        Protocol protocol = bridgeServiceFactory.getTransportFactory().getProtocol(address.getResource());
        if ( protocol instanceof HttpProtocol ) {
            return createHandler;
        }
        throw new RuntimeException(getClass()+
                ": Cannot select a connect handler for address "+address);
    }


    private <T extends ConnectFuture> IoSessionInitializer<ConnectFuture> createParentInitializer(
            final ResourceAddress connectAddressNext, final IoHandler handler, final IoSessionInitializer<T> initializer,
            final DefaultConnectFuture wseConnectFuture) {

        final ResourceAddress connectAddress = connectAddressNext.getTransport();
        Protocol protocol = bridgeServiceFactory.getTransportFactory().getProtocol(connectAddress.getResource());
        if (!(protocol instanceof HttpProtocol)) {
            final String message = format("Cannot create WSEB parent session initializer for address %s", connectAddressNext);
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
                final IoSessionInitializer<T> wseSessionInitializer = new IoSessionInitializer<T>() {
                    @Override
                    public void initializeSession(IoSession session, T future) {
                        WsebSession wseSession = (WsebSession) session;
                        wseSession.setHandler(handler);

                        // TODO: add extension filters when we adopt the new webSocket extension SPI
                        wseSession.getTransportSession().getFilterChain().fireSessionCreated();
                        wseSession.getTransportSession().getFilterChain().fireSessionOpened();

                        if (initializer != null) {
                            initializer.initializeSession(session, future);
                        }
                    }
                };

                final long sequenceNo = 0;

                final HttpConnectSession httpSession = (HttpConnectSession) parent;

                // WSE specification mandates use of POST method.
                if (specCompliant) {
                    httpSession.setMethod(HttpMethod.POST);
                    httpSession.setWriteHeader(HttpHeaders.HEADER_WEBSOCKET_VERSION, WSE_VERSION);
                    // Set content length so the HTTP request can be flushed
                    httpSession.setWriteHeader(HEADER_CONTENT_LENGTH, "0");
                }

                String nextProtocol = connectAddressNext.getOption(NEXT_PROTOCOL);
                if (nextProtocol != null) {
                    httpSession.addWriteHeader(HEADER_X_WEBSOCKET_PROTOCOL, nextProtocol);
                }

                String[] supportedProtocols = connectAddressNext.getOption(WsResourceAddress.SUPPORTED_PROTOCOLS);
                if (supportedProtocols != null) {
                    for (String protocol : supportedProtocols) {
                        if (protocol != null) {
                            httpSession.addWriteHeader(HEADER_X_WEBSOCKET_PROTOCOL, protocol);
                        }
                    }
                }

                List<WebSocketExtension> extensions = webSocketExtensionFactory.offerWebSocketExtensions(
                        (WsResourceAddress) connectAddressNext, extensionHelper);
                for (WebSocketExtension extension : extensions) {
                    httpSession.addWriteHeader(HEADER_X_WEBSOCKET_EXTENSIONS, extension.getExtensionHeader().toString());
                }

                httpSession.setWriteHeader(HEADER_X_ACCEPT_COMMANDS, "ping");
                httpSession.setWriteHeader(HttpHeaders.HEADER_X_SEQUENCE_NO, Long.toString(sequenceNo));

                // Avoid default to httpxe for efficiency (single http transport layer at other end)
                httpSession.setWriteHeader(HttpHeaders.HEADER_X_NEXT_PROTOCOL, "wse/1.0");

                final IoBufferAllocatorEx<WsBuffer> allocator = new WsebBufferAllocator(httpSession.getBufferAllocator());

                // factory to create a new bridge session
                Callable<WsebSession> createSession = new Callable<WsebSession>() {
                    @Override
                    public WsebSession call() throws Exception {

                    Callable<WsebSession> wseSessionFactory = new Callable<WsebSession>() {
                        @Override
                        public WsebSession call() throws Exception {
                            WsebSession wsebSession = new WsebSession(httpSession.getIoLayer(),
                                                                      httpSession.getIoThread(),
                                                                      httpSession.getIoExecutor(),
                                                                      WsebConnector.this,
                                                                      getProcessor(),
                                                                      connectAddressNext,
                                                                      connectAddressNext,
                                                                      allocator,
                                                                      null,
                                                                      0,
                                                                      connectAddressNext.getOption(INACTIVITY_TIMEOUT),
                                                                      false,            /* no sequence validation */
                                                                      sequenceNo,      /* starting sequence no */
                                                                      null,
                                                                      logger,
                                                                      configuration);

                                // ability to write will be reactivated when create response returns with write address
                                wsebSession.suspendWrite();

                                return wsebSession;
                            }
                        };

                        return newSession(wseSessionInitializer, wseConnectFuture, wseSessionFactory);
                    }
                };

                WSE_SESSION_FACTORY_KEY.set(httpSession, createSession);
                WSE_CONNECT_FUTURE_KEY.set(httpSession, wseConnectFuture);
            }
        };
    }


    private static final TypedAttributeKey<IoBufferEx> CREATE_RESPONSE_KEY = new TypedAttributeKey<>(WsebConnector.class, "createResponse");

    private IoHandler createHandler = new IoHandlerAdapter<HttpConnectSession>() {

        @Override
        protected void doSessionOpened(final HttpConnectSession createSession) throws Exception {
            Callable<WsebSession> sessionFactory = WSE_SESSION_FACTORY_KEY.remove(createSession);
            final WsebSession wsebSession = sessionFactory.call();

            // clean up session idle tracker
            wsebSession.getCloseFuture().addListener(new IoFutureListener<CloseFuture>() {
                @Override
                public void operationComplete(CloseFuture future) {
                    currentSessionIdleTracker.get().removeSession(wsebSession);

                    // handle exception during create response
                    createSession.close(false);
                }
            });

            // store created session to process during session close, or exception
            WSE_SESSION_KEY.set(createSession, wsebSession);
            WSE_CONNECT_FUTURE_KEY.remove(createSession);
        }

        @Override
        protected void doMessageReceived(HttpConnectSession createSession, Object message) throws Exception {
            // Handle fragmentation of response body
            IoBufferEx in = (IoBufferEx) message;

            IoBufferEx buf = CREATE_RESPONSE_KEY.get(createSession);
            if (buf == null) {
                IoBufferAllocatorEx<?> allocator = createSession.getBufferAllocator();
                buf = allocator.wrap(allocator.allocate(in.remaining())).setAutoExpander(allocator);
                CREATE_RESPONSE_KEY.set(createSession, buf);
            }

            buf.put(in);
        }

        @Override
        protected void doSessionClosed(HttpConnectSession createSession) throws Exception {
            final WsebSession wsebSession = WSE_SESSION_KEY.remove(createSession);
            assert (wsebSession != null);

            IoBufferEx buf = CREATE_RESPONSE_KEY.remove(createSession);

            String message = null;
            if (createSession.getStatus() != HttpStatus.SUCCESS_CREATED) {
                message = "Create handshake failed: invalid response status: " + createSession.getStatus();
            }
            else if (createSession.getReadHeader(HEADER_CONTENT_TYPE) == null
                    || !createSession.getReadHeader(HEADER_CONTENT_TYPE).matches("(?i)text\\/plain;\\s*charset=utf-8")) {
                message = "Create handshake failed: invalid response content type: " +
                              createSession.getReadHeader(HEADER_CONTENT_TYPE);
            }
            else if (createSession.getReadHeader(HEADER_X_WEBSOCKET_PROTOCOL) != null
                     && (createSession.getWriteHeaders(HEADER_X_WEBSOCKET_PROTOCOL) == null
                     || !createSession.getWriteHeaders(HEADER_X_WEBSOCKET_PROTOCOL).contains(
                    createSession.getReadHeader(HEADER_X_WEBSOCKET_PROTOCOL))) ) {
                message = format("Create handshake failed: negotiated WebSocket protocol %s was not in the requested list %s",
                        createSession.getReadHeader(HEADER_X_WEBSOCKET_PROTOCOL), createSession.getWriteHeaders(HEADER_X_WEBSOCKET_PROTOCOL));
            }
            else {
                List<String> requestedExtensions = createSession.getWriteHeaders(HEADER_X_WEBSOCKET_EXTENSIONS);
                if (requestedExtensions != null) {
                    for (String extension : createSession.getReadHeaders(HEADER_X_WEBSOCKET_EXTENSIONS)) {
                        if (!requestedExtensions.contains(new ExtensionHeaderBuilder(extension).getExtensionToken())) {
                                message = format("Create handshake failed: WebSocket extension %s was not requested",
                                                 extension);
                        }
                    }
                }
            }
            if (message == null && buf == null) {
                message = "Create handshake failed: no response body";
            }
            if (message != null) {
                wsebSession.reset(new Exception(message).fillInStackTrace());
                return;
            }

            buf.flip();

            String responseText = buf.getString(UTF_8.newDecoder());
            String[] locations = responseText.split("\n");

            if (locations.length < 2) {
                wsebSession.reset(new Exception(format("Create handshake failed: invalid response %s",
                        responseText.replace("\n", "\\n"))).fillInStackTrace());
                return;
            }

            URI writeURI = URI.create(locations[0]);
            if (!validateUpstreamOrDownstreamURI(wsebSession.getLocalAddress().getResource(), writeURI)) {
                wsebSession.reset(new Exception(format("Create handshake failed: invalid upstream URI %s",
                        writeURI)).fillInStackTrace());
            }
            URI readURI = URI.create(locations[1]);
            if (!validateUpstreamOrDownstreamURI(wsebSession.getLocalAddress().getResource(),readURI)) {
                wsebSession.reset(new Exception(format("Create handshake failed: invalid downstream URI %s",
                        readURI)).fillInStackTrace());
            }

            ResourceAddress writeAddress = createWriteAddress(writeURI, createSession, wsebSession);
            ResourceAddress readAddress = createReadAddress(readURI, createSession, wsebSession);

            // Continue even if wsebSession.isClosing() so close handshake can complete
            if (!wsebSession.getTransportSession().isClosing()) {
                wsebSession.setWriteAddress(writeAddress);
                wsebSession.setReadAddress(readAddress);

                // attach downstream for read
                final BridgeConnector bridgeConnector = bridgeServiceFactory.newBridgeConnector(readAddress);
                bridgeConnector.connect(readAddress, selectReadHandler(readAddress, wsebSession), new IoSessionInitializer<ConnectFuture>() {
                    @Override
                    public void initializeSession(IoSession ioSession, ConnectFuture connectFuture) {
                        HttpSession httpSession = (HttpSession) ioSession;
                        httpSession.setWriteHeader(HttpHeaders.HEADER_X_SEQUENCE_NO, Long.toString(wsebSession.nextReaderSequenceNo()));

                        // Avoid default to httpxe for efficiency (single http transport layer at other end)
                        httpSession.setWriteHeader(HttpHeaders.HEADER_X_NEXT_PROTOCOL, "wse/1.0");
                    }
                });

                // activate upstream for write
                //TODO: Replace usage of suspendWrite/resumeWrite with a WSEB-specific "send queue" upon which
                //TODO: locking of writes can be achieved.
                wsebSession.resumeWrite();
                // We are always aligned now. if (session.isIoAligned()) {
                wsebSession.getProcessor().flush(wsebSession);
            }
        }

        private ResourceAddress createWriteAddress(URI writeUri, HttpSession transport, WsebSession wsebSession) {
            ResourceAddress httpxeAddress = REMOTE_ADDRESS.get(transport);
            ResourceAddress writeAddress =  httpxeAddress.resolve(writeUri.getPath());

            String writeSessionIdentity = format("%s#%su", getTransportMetadata().getName(), wsebSession.getId());
            IdentityResolver resolver = new FixedIdentityResolver(writeSessionIdentity);

            ResourceOptions options = ResourceOptions.FACTORY.newResourceOptions(writeAddress);
            options.setOption(ResourceAddress.IDENTITY_RESOLVER, resolver);
            return resourceAddressFactory.newResourceAddress(URIUtils.uriToString(writeAddress.getResource()), options);
        }

        private ResourceAddress createReadAddress(URI readUri, HttpSession transport, WsebSession wsebSession) {
            ResourceAddress httpxeAddress = REMOTE_ADDRESS.get(transport);
            ResourceAddress readAddress =  httpxeAddress.resolve(readUri.getPath());

            String readSessionIdentity = format("%s#%sd", getTransportMetadata().getName(), wsebSession.getId());
            IdentityResolver resolver = new FixedIdentityResolver(readSessionIdentity);

            ResourceOptions options = ResourceOptions.FACTORY.newResourceOptions(readAddress);
            options.setOption(ResourceAddress.IDENTITY_RESOLVER, resolver);
            return resourceAddressFactory.newResourceAddress(URIUtils.uriToString(readAddress.getResource()), options);
        }

        @Override
        protected void doExceptionCaught(HttpConnectSession createSession, Throwable cause) throws Exception {
            if (logger.isDebugEnabled()) {
                String message = format("Error on WebSocket WSE connection attempt: %s", cause);
                if (logger.isTraceEnabled()) {
                    // note: still debug level, but with extra detail about the exception
                    logger.debug(message, cause);
                }
                else {
                    logger.debug(message);
                }
            }

            createSession.close(true);
        }

    };

    private IoHandler selectReadHandler(ResourceAddress readAddress, WsebSession session) {
        Protocol protocol = bridgeServiceFactory.getTransportFactory().getProtocol(readAddress.getResource());
        if ( protocol instanceof HttpProtocol ) {
            return new ReadHandler(session);
        }
        throw new RuntimeException("Cannot select read handler for address "+readAddress);
    }

    private boolean validateUpstreamOrDownstreamURI(URI wsURI, URI uri) {
        if (!Arrays.asList("http", "https").contains(uri.getScheme())) {
            return false;
        }
        if (!Utils.sameOrEquals(wsURI.getHost(), uri.getHost())) {
            return false;
        }
        if (!WseUtils.pathPrefixMatches(wsURI, uri)) {
            return false;
        }
        return true;
    }

    private final class ReadHandler extends IoHandlerAdapter<HttpConnectSession> {
        private final WsebSession wsebSession;
        ReadHandler(WsebSession wsebSession) {
            this.wsebSession = wsebSession;
        }

        @Override
        protected void doSessionCreated(HttpConnectSession readSession) throws Exception {
            addBridgeFilters(readSession.getFilterChain());
            super.doSessionCreated(readSession);
        }

        @Override
        protected void doSessionOpened(HttpConnectSession readSession) throws Exception {
            IoFilterChain filterChain = readSession.getFilterChain();
            filterChain.addLast(CODEC_FILTER, new WsebFrameCodecFilter(0, true));
            wsebSession.attachReader(readSession);

            // Activate inactivity timeout only once read session is established
            // We need a session idle tracker to handle ws close handshake, even if ws.inactivity.timeout is not set
            currentSessionIdleTracker.get().addSession(wsebSession);

            readSession.getResponseFuture().addListener(WsebConnector.CHECK_READER_RESPONSE_LISTENER);

        }

        @Override
        protected void doMessageReceived(HttpConnectSession readSession, Object message) throws Exception {
            ResourceAddress readAddress = REMOTE_ADDRESS.get(readSession);
            // handle parallel closure of WSE session during streaming read
            if (readSession != wsebSession.getReader()) {
                throw new Exception("Protocol violation: data received on downstream after reconnect");
            }

            WsMessage wsebMessage = (WsMessage) message;
            IoFilterChain filterChain = wsebSession.getTransportSession().getFilterChain();

            switch (wsebMessage.getKind()) {
            case COMMAND:
                for (Command command : ((WsCommandMessage)wsebMessage).getCommands()) {
                    if (command == Command.reconnect() && !wsebSession.isClosing()) {
                        // received a RECONNECT command
                        wsebSession.detachReader(readSession);
                        // re-attach downstream for read
                        final BridgeConnector bridgeConnector = bridgeServiceFactory.newBridgeConnector(readAddress);
                        bridgeConnector.connect(readAddress, selectReadHandler(readAddress, wsebSession), null);
                        break;
                    }
                    else if (command == Command.close()) {
                        // Following should take care of sending CLOSE response and closing reader (downstream)
                        // Close case was not handled before 3.5.9
                        filterChain.fireMessageReceived(new WsCloseMessage());
                        break;
                    }
                    // no-op (0x00) - continue reading commands
                }
                break;
            default:
                // ignore data after CLOSE or RECONNECT commands
                if (!wsebSession.isCloseReceived() && wsebSession.getReader() == readSession) {
                    filterChain.fireMessageReceived(wsebMessage);
                }
                break;
            }
        }

        @Override
        protected void doExceptionCaught(HttpConnectSession readSession, Throwable cause) throws Exception {
            wsebSession.setCloseException(cause);
            readSession.close(true);
        }

        @Override
        protected void doSessionClosed(HttpConnectSession readSession) throws Exception {
            if (readSession.getStatus() != HttpStatus.SUCCESS_OK
                                        || wsebSession.getCloseException() != null) {
                wsebSession.reset(
                        new IOException("Network connectivity has been lost or transport was closed at other end",
                                wsebSession.getAndClearCloseException()).fillInStackTrace());
            }
            else {
                wsebSession.close(true);
            }
        }
    }

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
