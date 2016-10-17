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
package org.kaazing.gateway.transport.http;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableMap;
import static java.util.EnumSet.allOf;
import static java.util.EnumSet.complementOf;
import static java.util.EnumSet.of;
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static org.kaazing.gateway.resource.address.ResourceAddress.QUALIFIER;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT_URI;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.BALANCE_ORIGINS;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.GATEWAY_ORIGIN_SECURITY;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.ORIGIN_SECURITY;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.TEMP_DIRECTORY;
import static org.kaazing.gateway.transport.http.HttpAcceptFilter.CONDITIONAL_WRAPPED_RESPONSE;
import static org.kaazing.gateway.transport.http.HttpAcceptFilter.CONTENT_LENGTH_ADJUSTMENT;
import static org.kaazing.gateway.transport.http.HttpAcceptFilter.ELEVATE_EMULATED_REQUEST;
import static org.kaazing.gateway.transport.http.HttpAcceptFilter.HOST_HEADER;
import static org.kaazing.gateway.transport.http.HttpAcceptFilter.HTTP_SERIALIZE_REQUEST_FILTER;
import static org.kaazing.gateway.transport.http.HttpAcceptFilter.MERGE_REQUEST;
import static org.kaazing.gateway.transport.http.HttpAcceptFilter.PROTOCOL_HTTP;
import static org.kaazing.gateway.transport.http.HttpAcceptFilter.PROTOCOL_HTTPXE;
import static org.kaazing.gateway.transport.http.HttpStatus.CLIENT_NOT_FOUND;
import static org.kaazing.gateway.transport.http.bridge.filter.HttpNextProtocolHeaderFilter.PROTOCOL_HTTPXE_1_1;
import static org.kaazing.gateway.transport.http.bridge.filter.HttpProtocolFilter.PROTOCOL_HTTP_1_1;
import static org.kaazing.gateway.transport.http.resource.HttpDynamicResourceFactory.newHttpDynamicResourceFactory;
import static org.kaazing.gateway.util.InternalSystemProperty.HTTPXE_SPECIFICATION;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.annotation.Resource;
import javax.security.auth.Subject;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.gateway.resource.address.Protocol;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.resource.address.uri.URIUtils;
import org.kaazing.gateway.security.auth.context.ResultAwareLoginContext;
import org.kaazing.gateway.server.ExpiringState;
import org.kaazing.gateway.transport.AbstractBridgeAcceptor;
import org.kaazing.gateway.transport.Bindings;
import org.kaazing.gateway.transport.BridgeAcceptor;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.transport.BridgeSessionInitializer;
import org.kaazing.gateway.transport.DefaultIoSessionConfigEx;
import org.kaazing.gateway.transport.DefaultTransportMetadata;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.TypedAttributeKey;
import org.kaazing.gateway.transport.http.HttpBindings.HttpBinding;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.gateway.transport.http.bridge.HttpMessage;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.gateway.transport.http.bridge.filter.HttpBuffer;
import org.kaazing.gateway.transport.http.bridge.filter.HttpBufferAllocator;
import org.kaazing.gateway.transport.http.bridge.filter.HttpNextAddressFilter;
import org.kaazing.gateway.transport.http.bridge.filter.HttpProtocolDecoderException;
import org.kaazing.gateway.transport.http.bridge.filter.HttpSerializeRequestsFilter;
import org.kaazing.gateway.transport.http.bridge.filter.HttpSubjectSecurityFilter;
import org.kaazing.gateway.transport.http.resource.HttpDynamicResource;
import org.kaazing.gateway.transport.http.resource.HttpDynamicResourceFactory;
import org.kaazing.gateway.util.LoggingUtils;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.future.UnbindFuture;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.LoggerFactory;
@SuppressWarnings("deprecation")
public class HttpAcceptor extends AbstractBridgeAcceptor<DefaultHttpSession, HttpBinding> {

    private static final String LOGGER_NAME = format("transport.%s.accept", HttpProtocol.NAME);

    public static final String SECURITY_LOGGER_NAME = format("%s.security", LOGGER_NAME);
    public static final String MERGE_REQUEST_LOGGER_NAME = format("%s.mergeRequest", LOGGER_NAME);
    public static final AttributeKey SERVICE_REGISTRATION_KEY = new AttributeKey(HttpAcceptor.class, "serviceRegistration");

    public static final TypedAttributeKey<Boolean> HTTPXE_SPEC_KEY = new TypedAttributeKey<>(HttpAcceptor.class, "httpxeSpec");
    static final TypedAttributeKey<DefaultHttpSession> SESSION_KEY = new TypedAttributeKey<>(HttpAcceptor.class, "session");
	public static final AttributeKey BALANCEES_KEY = new AttributeKey(HttpAcceptor.class, "balancees");

    private final Map<String, Set<HttpAcceptFilter>> acceptFiltersByProtocol;
    private final Set<HttpAcceptFilter> allAcceptFilters;

    private BridgeServiceFactory bridgeServiceFactory;
    private ResourceAddressFactory addressFactory;

    private IoFilter httpNextAddress;

    private SchedulerProvider schedulerProvider;

	private ExpiringState expiringState;

    private Properties configuration;

    private boolean httpxeSpecCompliant;

    @Resource(name = "schedulerProvider")
    public void setSchedulerProvider(SchedulerProvider provider) {
        this.schedulerProvider = provider;
    }

    @Resource(name = "expiringState")
    public void setExpiringState(ExpiringState expiringState) {
        this.expiringState = expiringState;
    }

    @Resource(name = "configuration")
    public void setConfiguration(Properties configuration) {
        this.configuration = configuration;
        httpxeSpecCompliant = HTTPXE_SPECIFICATION.getBooleanProperty(configuration);
    }

    public HttpAcceptor() {
        super(new DefaultIoSessionConfigEx());

        // note: content length adjustment filter is added dynamically for httpxe/1.1, and not needed by http/1.1
        // note: empty packet filter is added dynamically for httpx/1.1, and not needed by httpxe/1.1 nor http/1.1
        // note: serialize request filter only needed for http , not httpxe nor httpx

        Map<String, Set<HttpAcceptFilter>> acceptFiltersByProtocol = new HashMap<>();
        acceptFiltersByProtocol.put(PROTOCOL_HTTP_1_1, complementOf(of(CONTENT_LENGTH_ADJUSTMENT,
                                                                       PROTOCOL_HTTPXE,
                                                                       ELEVATE_EMULATED_REQUEST,
                                                                       CONDITIONAL_WRAPPED_RESPONSE)));

        acceptFiltersByProtocol.put(PROTOCOL_HTTPXE_1_1, complementOf(of(CONTENT_LENGTH_ADJUSTMENT,
                                                                         // wsx-filter-only
                                                                         MERGE_REQUEST,
                                                                         // do not serialize again
                                                                         HTTP_SERIALIZE_REQUEST_FILTER,
                                                                         PROTOCOL_HTTP,
                                                                         HOST_HEADER,
                                                                         ELEVATE_EMULATED_REQUEST,
                                                                         CONDITIONAL_WRAPPED_RESPONSE)));

        acceptFiltersByProtocol.put("x-kaazing-handshake", complementOf(of(CONTENT_LENGTH_ADJUSTMENT,
                                                                           PROTOCOL_HTTPXE,
                                                                           HOST_HEADER,
                                                                           ELEVATE_EMULATED_REQUEST,
                                                                           CONDITIONAL_WRAPPED_RESPONSE)));

        this.acceptFiltersByProtocol = unmodifiableMap(acceptFiltersByProtocol);
        this.allAcceptFilters = allOf(HttpAcceptFilter.class);
    }

    @Override
    protected Bindings<HttpBinding> initBindings() {
        return new HttpBindings() {

            @Override
            protected HttpBinding bindAdditionalAddressesIfNecessary(HttpBinding newHttpBinding) {
                HttpBinding httpBinding = addBinding0(newHttpBinding);

                // detect first bind (without race)
                if (httpBinding == null) {
                    // bind the resources handler by default (no security)
                    ResourceAddress resourcesAddress = getResourcesAddress(newHttpBinding);
                    HttpAcceptor.this.bind(resourcesAddress, httpResourcesHandler, null);
                }

                return httpBinding;
            }

            @Override
            protected boolean unbindAdditionalAddressesIfNecessary(ResourceAddress address, HttpBinding newHttpBinding) {
                ResourceAddress resourcesAddress = getResourcesAddress(newHttpBinding);
                if ( newHttpBinding.size()==1 && newHttpBinding.get(resourcesAddress.getResource().getPath()) != null) {
                    HttpAcceptor.this.unbind(resourcesAddress);
                    return true;
                }
                return false;
            }

            private ResourceAddress getResourcesAddress(HttpBinding newHttpBinding) {
                ResourceAddress bindAddress = newHttpBinding.bindAddress();
                String location = bindAddress.getExternalURI();
                String resourcesURI = URIUtils.resolve(location, "/;resource");
                ResourceOptions options = ResourceOptions.FACTORY.newResourceOptions();
                options.setOption(TRANSPORT_URI, bindAddress.getOption(TRANSPORT_URI));
                options.setOption(TRANSPORT, bindAddress.getOption(TRANSPORT));
                options.setOption(TEMP_DIRECTORY, bindAddress.getOption(TEMP_DIRECTORY));
                options.setOption(NEXT_PROTOCOL, bindAddress.getOption(NEXT_PROTOCOL));
                options.setOption(ORIGIN_SECURITY, bindAddress.getOption(ORIGIN_SECURITY));
                options.setOption(GATEWAY_ORIGIN_SECURITY, bindAddress.getOption(GATEWAY_ORIGIN_SECURITY));
                options.setOption(BALANCE_ORIGINS, bindAddress.getOption(BALANCE_ORIGINS));
                return addressFactory.newResourceAddress(resourcesURI, options);
            }

        };
    }

    @Resource(name = "bridgeServiceFactory")
    public void setBridgeServiceFactory(BridgeServiceFactory bridgeServiceFactory) {
        this.bridgeServiceFactory = bridgeServiceFactory;
    }

    @Resource(name = "resourceAddressFactory")
      public void setResourceAddressFactory(ResourceAddressFactory resourceAddressFactory) {
        this.addressFactory = resourceAddressFactory;
    }

    @Override
    public void init() {
        super.init();

        HttpNextAddressFilter httpNextAddress = new HttpNextAddressFilter();
        httpNextAddress.setResourceAddressFactory(addressFactory);
        httpNextAddress.setBindings(bindings);
        this.httpNextAddress = httpNextAddress;

        // TODO: verify injections and throw exception if not in a valid start state
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return new DefaultTransportMetadata(HttpProtocol.NAME);
    }

    @Override
    protected IoProcessorEx<DefaultHttpSession> initProcessor() {
        return new HttpAcceptProcessor();
    }

    @Override
    protected boolean canBind(String transportName) {
        return transportName.equals("http");
    }

    @Override
    protected <T extends IoFuture>
    void bindInternal(final ResourceAddress address, IoHandler handler,
                      final BridgeSessionInitializer<T> initializer) {

        if (logger.isTraceEnabled()) {
            logger.trace(format("binding: '%s' %s", address.getExternalURI(), address.getOption(NEXT_PROTOCOL)));
        }

        //
        // Bind the transport of the address.
        //
        final ResourceAddress transportAddress = address.getTransport();

        final URI transportURI = transportAddress.getResource();

        final Protocol transportProtocol = bridgeServiceFactory.getTransportFactory().getProtocol(transportURI.getScheme());

        final BridgeSessionInitializer<T> sessionInitializer =
                 (initializer != null) ?
                   initializer.getParentInitializer(transportProtocol)
                 : null;

        BridgeAcceptor acceptor = bridgeServiceFactory.newBridgeAcceptor(transportAddress);
        acceptor.bind(transportAddress, bridgeHandler, sessionInitializer);
    }

    @Override
    protected UnbindFuture unbindInternal(ResourceAddress address, IoHandler handler, BridgeSessionInitializer<? extends IoFuture> initializer) {
        if (logger.isTraceEnabled()) {
            logger.trace(format("unbinding: '%s' %s", address.getExternalURI(), address.getOption(NEXT_PROTOCOL)));
        }
        ResourceAddress transport = address.getTransport();
        BridgeAcceptor acceptor = bridgeServiceFactory.newBridgeAcceptor(transport);
        return acceptor.unbind(transport);
    }


    private final IoHandler httpResourcesHandler = new IoHandlerAdapter<HttpAcceptSession>() {

        private final HttpDynamicResourceFactory dynamicResourceFactory = newHttpDynamicResourceFactory();

        @Override
        protected void doSessionOpened(HttpAcceptSession session) throws Exception {

            URI pathInfo = session.getPathInfo();
            String path = pathInfo.getPath();  // includes leading '/' after "/;resource"
            String resourceName = (path != null && path.length() > 0) ? path.substring(1) : "";

            Collection<String> resourceNames = dynamicResourceFactory.getResourceNames();
            if (!resourceNames.contains(resourceName)) {
                session.setStatus(CLIENT_NOT_FOUND);
                session.close(false);
            }
            else {
                HttpDynamicResource dynamicResource = dynamicResourceFactory.newHttpDynamicResource(resourceName);
                dynamicResource.writeFile(session);
                session.close(false);
            }
        }
    };


    private final IoHandler bridgeHandler = new IoHandlerAdapter<IoSessionEx>() {

        @Override
        protected void doSessionOpened(IoSessionEx session) throws Exception {
        }

        @Override
        protected void doSessionCreated(IoSessionEx session) throws Exception {
            HTTPXE_SPEC_KEY.set(session, httpxeSpecCompliant);
            IoFilterChain filterChain = session.getFilterChain();
            addBridgeFilters(filterChain);
        }

        @Override
        protected void doSessionIdle(IoSessionEx session, IdleStatus status) throws Exception {
            DefaultHttpSession httpSession = SESSION_KEY.get(session);
            if (httpSession != null) {
                IoFilterChain filterChain = httpSession.getFilterChain();
                filterChain.fireSessionIdle(status);
            }
        }

        @Override
        protected void doSessionClosed(IoSessionEx session) throws Exception {
            // if iosession is not being closed then a higher level is asking to close out this http session, likely for upgrade
            if (!session.isClosing()) {
                IoFilterChain filterChain = session.getFilterChain();
                removeBridgeFilters(filterChain);
            }

            DefaultHttpSession httpSession = SESSION_KEY.remove(session);
            if (httpSession != null && !httpSession.isClosing()) {
                httpSession.reset(new IOException("Early termination of IO session").fillInStackTrace());
            }
        }

        @Override
        protected void doExceptionCaught(final IoSessionEx session, Throwable cause) throws Exception {
            DefaultHttpSession httpSession = SESSION_KEY.get(session);
            if (httpSession != null && !httpSession.isClosing()) {
                // see AbstractPollingIoProcessor.read(T session)
                // if the cause is an IOException, then the session is scheduled for removal
                // but the session is not yet marked as closing
                if (!session.isClosing() && !(cause instanceof IOException) && !httpSession.getCommitFuture().isCommitted()) {
                    HttpResponseMessage httpResponse = new HttpResponseMessage();
                    httpResponse.setVersion(HttpVersion.HTTP_1_1);
                    httpResponse.setStatus(HttpStatus.SERVER_INTERNAL_ERROR);
                    HttpAcceptProcessor.setServerHeader(httpSession, httpResponse);
                    httpResponse.setHeader("Date", HttpUtils.formatDateHeader(System.currentTimeMillis()));
                    session.write(httpResponse);
                    session.close(false);

                    String message = String.format("Unexpected HTTP exception: %s", cause);
                    LoggingUtils.log(logger, message, cause);
                }

                // Note: we must trigger doSessionClosed here to avoid recursion of exceptionCaught
                session.close(true);
            }
            else {
                if (logger.isDebugEnabled()) {
                // handle malformed HTTP scenario
                    String message = format("Error on HTTP connection, closing connection: %s", cause);
                    LoggingUtils.log(logger, message, cause);
                }

                if (!session.isClosing() && cause instanceof HttpProtocolDecoderException) {
                    HttpResponseMessage httpResponse = new HttpResponseMessage();
                    httpResponse.setVersion(HttpVersion.HTTP_1_1);
                    httpResponse.setStatus(((HttpProtocolDecoderException)cause).getHttpStatus());
                    HttpAcceptProcessor.setServerHeader(httpSession, httpResponse);
                    httpResponse.setHeader("Date", HttpUtils.formatDateHeader(System.currentTimeMillis()));
                    session.write(httpResponse);
                }
                session.close(false);
            }
        }

        @Override
        protected void doMessageReceived(final IoSessionEx session, Object message) throws Exception {
            // TODO: if content is complete then suspendRead on iosession
            // TODO: in processor when complete resume iosession read (parent)

            DefaultHttpSession httpSession;

            HttpMessage httpMessage = (HttpMessage) message;
            switch (httpMessage.getKind()) {
            case REQUEST:
                final HttpRequestMessage httpRequest = (HttpRequestMessage) message;
                final URI requestURI = httpRequest.getRequestURI();

                if (logger.isInfoEnabled()) {
                    String host = httpRequest.getHeader("Host");
                    String userAgent = httpRequest.getHeader("User-Agent");
                    if (userAgent == null) {
                        userAgent = "-";
                    }
                    logger.info(format("%s - [%s] \"%s %s %s \" \"%s\"", session.getRemoteAddress(), host, httpRequest.getMethod(), requestURI.toString(), httpRequest.getVersion(), userAgent));
                }

                final ResourceAddress localAddress = httpRequest.getLocalAddress();
                if (localAddress == null && logger.isDebugEnabled()) {
                    logger.debug(String.format("Failed to find a binding local address for request URI %s", requestURI));
                }
                assert (localAddress != null);


                ResourceAddress transportAddress = BridgeSession.REMOTE_ADDRESS.get(session);
                ResourceOptions options = ResourceOptions.FACTORY.newResourceOptions();
                options.setOption(TRANSPORT, transportAddress);
                options.setOption(NEXT_PROTOCOL, localAddress.getOption(NEXT_PROTOCOL));

                final ResourceAddress remoteAddress = addressFactory.newResourceAddress(httpRequest.getExternalURI(), options);

                // percolate subject
                final Subject subject = httpRequest.getSubject();

                // percolate login context
                final ResultAwareLoginContext loginContext = httpRequest.getLoginContext();
                // create new http session and store it in this io session
                httpSession = newSession(new IoSessionInitializer<IoFuture>() {
                    @Override
                    public void initializeSession(IoSession httpSession, IoFuture future) {
                        ((DefaultHttpSession)httpSession).setSubject(subject);
                        ((DefaultHttpSession)httpSession).setLoginContext(loginContext);
                    }
                }, new Callable<DefaultHttpSession>() {
                    @Override
                    public DefaultHttpSession call() {
                        IoBufferAllocatorEx<?> parentAllocator = session.getBufferAllocator();
                        DefaultHttpSession newHttpSession = new DefaultHttpSession(HttpAcceptor.this,
                                getProcessor(),
                                localAddress,
                                remoteAddress,
                                session,
                                new HttpBufferAllocator(parentAllocator),
                                httpRequest,
                                localAddress.getResource(),
                                configuration);

                        IoHandler handler = getHandler(newHttpSession.getLocalAddress());
                        if ( handler == null && logger.isTraceEnabled() ) {
                            logger.warn("Unable to find handler for new HTTP session with local address:\n{}\nbindings:\n{}\n", newHttpSession.getLocalAddress(), bindings);
                        }
                        newHttpSession.setHandler(handler);
                        // need to set here so that exceptions during session created|opened are properly handled as 50x
                        SESSION_KEY.set(session, newHttpSession);
                        return newHttpSession;
                    }
                });

                // TODO: do we need to fire session opened here?

                // fire content data on to http session
                HttpContentMessage httpContent = httpRequest.getContent();
                if (httpContent == null) {
                    IoBufferAllocatorEx<? extends HttpBuffer> allocator = httpSession.getBufferAllocator();
                    httpContent = new HttpContentMessage(allocator.wrap(allocator.allocate(0)), true);
                }

                fireContentReceived(httpSession, httpContent);
                break;
            case CONTENT:
                httpSession = SESSION_KEY.get(session);
                if (httpSession != null) {
                    fireContentReceived(httpSession, (HttpContentMessage) message);
                }
                else {
                    throw new Exception("HttpSession not available for HttpContent");
                }
                break;
            default:
                // RESPONSE not possible on HTTP acceptor
                throw new IllegalStateException("Unexpected message kind: " + httpMessage.getKind());
            }
        }

        @Override
        protected void doMessageSent(IoSessionEx session, Object message)
                throws Exception {

            // KG-9201: thread re-alignment is complete
            if (message == IoSessionEx.REGISTERED_EVENT) {
                DefaultHttpSession httpSession = SESSION_KEY.get(session);
                if (httpSession != null) {
                    // propagate any deferred content to the HTTP session on this new I/O thread
                    HttpAcceptProcessor processor = (HttpAcceptProcessor) httpSession.getProcessor();
                    processor.consume(httpSession);
                }
            }
        }

        private void fireContentReceived(DefaultHttpSession session, HttpContentMessage content) throws Exception {
            IoBufferEx buffer = content.asBuffer();
            if (buffer != null && buffer.hasRemaining()) {
                // if suspended add this to session deferred read queue
                // KG-9201: if HTTP session is in the middle of thread re-alignment,
                //          defer message received until re-alignment is complete
                if (!session.isIoRegistered() || session.isReadSuspended()) {
                    session.addDeferredRead(buffer);
                }
                else {
                    // direct read for now, in the future this should always get buffered
                    IoFilterChain filterChain = session.getFilterChain();
                    filterChain.fireMessageReceived(buffer);
                }
            }
        }
    };

    @Override
    public void addBridgeFilters(IoFilterChain chain) {
        IoSession transport = chain.getSession();

        SocketAddress localAddress = transport.getLocalAddress();

        String nextProtocol = null;
        if (localAddress instanceof ResourceAddress) {
            ResourceAddress address = (ResourceAddress) localAddress;
            if (!address.hasOption(QUALIFIER)) {
                nextProtocol = address.getOption(NEXT_PROTOCOL);
            }
        }
        if (nextProtocol == null) {
            nextProtocol = PROTOCOL_HTTP_1_1;
        }

        if (logger.isTraceEnabled()) {
            logger.trace(format("Adding http accept bridge filters using nextProtocol: %s", nextProtocol));
        }

        Set<HttpAcceptFilter> acceptFilters = acceptFiltersByProtocol.get(nextProtocol);
        assert (acceptFilters != null && !acceptFilters.isEmpty());

        for (HttpAcceptFilter acceptFilter : acceptFilters) {
            switch (acceptFilter) {
            case NEXT_ADDRESS:
                chain.addLast(acceptFilter.filterName(), httpNextAddress);
                break;
            case ELEVATE_EMULATED_REQUEST:
                // a session-specific filter added when necessary by the protocol compatibility filter
                break;
            case HTTP_SERIALIZE_REQUEST_FILTER:
                // session-specific always-added filter.
                chain.addLast(acceptFilter.filterName(), new HttpSerializeRequestsFilter(logger));
                break;
            case SUBJECT_SECURITY:
                // One instance of HttpSubjectSecurityFilter per session
                HttpSubjectSecurityFilter filter = new HttpSubjectSecurityFilter(LoggerFactory.getLogger(SECURITY_LOGGER_NAME), expiringState);
                filter.setSchedulerProvider(schedulerProvider);
                chain.addLast(acceptFilter.filterName(), filter);
                break;
            default:
                chain.addLast(acceptFilter.filterName(), acceptFilter.filter());
                break;
            }
        }
    }

    @Override
    public void removeBridgeFilters(IoFilterChain filterChain) {
        for (HttpAcceptFilter filter : allAcceptFilters) {
            removeFilter(filterChain, filter.filterName());
        }

    }

}

