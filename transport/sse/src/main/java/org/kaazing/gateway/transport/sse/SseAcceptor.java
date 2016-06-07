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
package org.kaazing.gateway.transport.sse;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.mina.core.session.IdleStatus.WRITER_IDLE;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.gateway.resource.address.Protocol;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.resource.address.URLUtils;
import org.kaazing.gateway.resource.address.uri.URIUtils;
import org.kaazing.gateway.transport.AbstractBridgeAcceptor;
import org.kaazing.gateway.transport.Bindings;
import org.kaazing.gateway.transport.Bindings.Binding;
import org.kaazing.gateway.transport.BridgeAcceptor;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.transport.BridgeSessionInitializer;
import org.kaazing.gateway.transport.BridgeSessionInitializerAdapter;
import org.kaazing.gateway.transport.CommitFuture;
import org.kaazing.gateway.transport.DefaultTransportMetadata;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.NioBindException;
import org.kaazing.gateway.transport.TypedAttributeKey;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpProtocol;
import org.kaazing.gateway.transport.http.HttpSession;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpUtils;
import org.kaazing.gateway.transport.sse.bridge.SseMessage;
import org.kaazing.gateway.transport.sse.bridge.filter.SseAcceptCodecFilter;
import org.kaazing.gateway.transport.sse.bridge.filter.SseBuffer;
import org.kaazing.gateway.transport.sse.bridge.filter.SseBufferAllocator;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.future.UnbindFuture;
import org.kaazing.mina.core.service.IoProcessorEx;

public class SseAcceptor extends AbstractBridgeAcceptor<SseSession, Binding> {

    public static final AttributeKey CLIENT_BUFFER_KEY = new AttributeKey(SseAcceptor.class, "clientBuffer");
    public static final AttributeKey CLIENT_PADDING_KEY = new AttributeKey(SseAcceptor.class, "clientPadding");
    public static final AttributeKey CLIENT_BLOCK_PADDING_KEY = new AttributeKey(SseAcceptor.class, "clientBlockPadding");
    public static final AttributeKey TIMEOUT_FUTURE_KEY = new AttributeKey(SseAcceptor.class, "timeoutFuture");
    public static final AttributeKey BYTES_WRITTEN_ON_LAST_FLUSH_KEY = new AttributeKey(SseAcceptor.class, "bytesWrittenOnLastFlush");

    private static final DefaultTransportMetadata SSE_TRANSPORT_METADATA =
            new DefaultTransportMetadata(SseProtocol.NAME, SseSessionConfig.class);

    private static final TypedAttributeKey<ResourceAddress> NEXT_PROTOCOL_RESOURCE_ADDRESS =
             new TypedAttributeKey<>(SseAcceptor.class, "nextProtocolResourceAddress");

    private static final String CODEC_FILTER = SseProtocol.NAME + "#codec";

    // TODO: make these settings available via configuration, with a reasonable default
    private static final long TIME_TO_FIRST_WRITE_MILLIS = SECONDS.toMillis(5);
    private static final long TIME_TO_PULSE_MILLIS = SECONDS.toMillis(30L);
    private static final long TIME_TO_TIMEOUT_RECONNECT_MILLIS = SECONDS.toMillis(60L);

    private ScheduledExecutorService scheduler;

    private IoFilter sseCodec;

    private BridgeServiceFactory bridgeServiceFactory;
    private ResourceAddressFactory resourceAddressFactory;

    @Resource(name = "bridgeServiceFactory")
    public void setBridgeServiceFactory(BridgeServiceFactory bridgeServiceFactory) {
        this.bridgeServiceFactory = bridgeServiceFactory;
    }

    @Resource(name = "resourceAddressFactory")
    public void setResourceAddressFactory(ResourceAddressFactory factory) {
        this.resourceAddressFactory = factory;
    }

    public SseAcceptor() {
        super(new DefaultSseSessionConfig());
    }


    @Resource(name = "schedulerProvider")
    public void setSchedulerProvider(SchedulerProvider provider) {
        this.scheduler = provider.getScheduler("KeepAlive-Sse", true);
    }

    @Override
    public void init() {
        super.init();

        sseCodec = new SseAcceptCodecFilter();
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return SSE_TRANSPORT_METADATA;
    }

    @Override
    public void addBridgeFilters(IoFilterChain filterChain) {
        filterChain.addLast(CODEC_FILTER, sseCodec);
    }

    @Override
    public void removeBridgeFilters(IoFilterChain filterChain) {
        removeFilter(filterChain, CODEC_FILTER);
    }

    @Override
    protected Bindings<Binding> initBindings() {
        return new Bindings.Default();
    }

    @Override
    protected IoProcessorEx<SseSession> initProcessor() {
        return new SseAcceptProcessor();
    }

    @Override
    protected boolean canBind(String transportName) {
        return transportName.equals("sse");
    }

    @Override
    protected <T extends IoFuture> void bindInternal(final ResourceAddress address, IoHandler handler,
                                                     final BridgeSessionInitializer<T> initializer) {
        final ResourceAddress transportAddress = address.getTransport();
        if (transportAddress != null) {
            try {
                URI transportURI = transportAddress.getResource();
                Protocol transportProtocol = bridgeServiceFactory.getTransportFactory().getProtocol(transportURI.getScheme());
                final BridgeSessionInitializer<T> httpInitializer = (initializer != null) ? initializer.getParentInitializer(transportProtocol) : null;

                BridgeSessionInitializer<T> wrapper = new BridgeSessionInitializerAdapter<T>() {
                    @Override
                    public void initializeSession(IoSession session, T future) {
                        session.setAttribute(NEXT_PROTOCOL_RESOURCE_ADDRESS, address);
                        if ( httpInitializer != null ) {
                            httpInitializer.initializeSession(session, future);
                        }
                    }
                };

                BridgeAcceptor transportAcceptor = bridgeServiceFactory.newBridgeAcceptor(transportAddress);
                final IoHandler transportHandler = selectTransportHandler(transportAddress);
                transportAcceptor.bind(transportAddress, transportHandler, wrapper);
            } catch (NioBindException e) {
                throw new RuntimeException("Unable to bind address " + address + ": " + e.getMessage(), e);
            }
        }
    }

    @Override
    protected UnbindFuture unbindInternal(ResourceAddress address, IoHandler handler, BridgeSessionInitializer<? extends IoFuture> initializer) {
        final ResourceAddress transportBindAddress = address.getTransport();
        final BridgeAcceptor transportAcceptor = bridgeServiceFactory.newBridgeAcceptor(transportBindAddress);
        return transportAcceptor.unbind(transportBindAddress);
    }

    @Override
    protected IoFuture dispose0() throws Exception {
        scheduler.shutdownNow();
        return super.dispose0();
    }

    private IoHandler selectTransportHandler(ResourceAddress address) {
        Protocol protocol  = bridgeServiceFactory.getTransportFactory().getProtocol(address.getResource());
        if ( protocol instanceof HttpProtocol ) {
            return bridgeHandler;
        }
        throw new RuntimeException(getClass()+": Cannot find handler for address "+address);
    }

    private boolean hasSessionId(HttpAcceptSession session) {
        String path = session.getPathInfo().getPath();
        if ( path != null ) {
            int index = path.lastIndexOf(";s/");
            if ( index != -1 ) {
                return true;
            }
        }
        return false;

    }
    private IoHandler bridgeHandler = new IoHandlerAdapter<HttpAcceptSession>() {

        private final TypedAttributeKey<SseSession> SSE_SESSION_KEY = new TypedAttributeKey<>(SseAcceptor.class, "sseSession");
        @Override
        protected void doSessionOpened(HttpAcceptSession httpSession) throws Exception {
            IoFilterChain filterChain = httpSession.getFilterChain();
            addBridgeFilters(filterChain);

            if (hasSessionId(httpSession)) {
                // avoid potential race between client reconnecting to SSE session after SSE session has closed
                // when SSE session closes, it unbinds the session-specific sub-path automatically, so
                // this generic "create" entry point receives the reconnect request instead
                // since the SSE session has already been closed, fail the reconnect attempt
                httpSession.setStatus(HttpStatus.CLIENT_NOT_FOUND);
                httpSession.close(false);
                return;
            }

            // create new session
            SseReconnectHandler reconnectHandler = createSession(httpSession);
            SseSession sseSession = reconnectHandler.sseSession;
            SSE_SESSION_KEY.set(httpSession, sseSession);

            // TODO: throw exception in HttpSession.setWriteHeader() after HttpSession writes headers
            reconnectHandler.sessionOpened(httpSession);
        }

        @Override
        protected void doExceptionCaught(HttpAcceptSession session, Throwable cause) throws Exception {
            if (logger.isDebugEnabled()) {
                String message = format("Error on SSE connection, closing connection: %s", cause);
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
            SseSession sseSession = SSE_SESSION_KEY.remove(session);
            if (sseSession != null && !sseSession.isClosing()) {
                sseSession.reset(new IOException("Early termination of IO session").fillInStackTrace());
            }

            IoFilterChain filterChain = session.getFilterChain();
            removeBridgeFilters(filterChain);
        }


        private SseReconnectHandler createSession(final HttpAcceptSession httpSession) throws Exception {
    
            String sessionId = HttpUtils.newSessionId();
    
            ResourceAddress httpLocalAddress = httpSession.getLocalAddress();
            URI candidateURI = URLUtils.modifyURIScheme(httpLocalAddress.getResource(), "sse");
            ResourceOptions candidateOptions = ResourceOptions.FACTORY.newResourceOptions();
            candidateOptions.setOption(TRANSPORT, httpLocalAddress);
            // note: nextProtocol is null since SSE specification does not required X-Next-Protocol on the wire
            ResourceAddress candidateAddress = resourceAddressFactory.newResourceAddress(URIUtils.uriToString(candidateURI), candidateOptions);

            final Binding binding = bindings.getBinding(candidateAddress);
            if (binding == null) {
                httpSession.setStatus(HttpStatus.CLIENT_NOT_FOUND);
                httpSession.close(false);
                return null;
            }
            
            final IoBufferAllocatorEx<SseBuffer> allocator = new SseBufferAllocator(httpSession.getBufferAllocator());


            final ResourceAddress sseBindAddress = binding.bindAddress();
            final IoHandler sseHandler = binding.handler();

            ResourceAddress httpBindAddress = httpSession.getLocalAddress();
            ResourceAddress newHttpBindAddress = httpBindAddress.resolve(format("%s;s/%s", httpBindAddress.getResource().getPath(), sessionId));
            ResourceOptions sseRemoteOptions = ResourceOptions.FACTORY.newResourceOptions();
            sseRemoteOptions.setOption(TRANSPORT, newHttpBindAddress);
            // note: nextProtocol is null since SSE specification does not required X-Next-Protocol on the wire
            String sseBindURI = sseBindAddress.getExternalURI();
            final ResourceAddress sseRemoteAddress = resourceAddressFactory.newResourceAddress(sseBindURI, sseRemoteOptions);

            // create the session
            final SseSession sseSession = newSession(new IoSessionInitializer<IoFuture>() {
                @Override
                public void initializeSession(IoSession sseSession, IoFuture future) {
                    // forward encoding
                    String encoding = httpSession.getParameter("encoding");
                    sseSession.setAttribute("encoding", encoding);
                }
            }, new Callable<SseSession>() {
                @Override
                public SseSession call() {
                    // wait to set parent HTTP session until reconnect
                    // avoids race condition with flushing pending writes
                    // versus setting write headers and processing parameters
                    
                    SseSession sseSession = new SseSession(httpSession.getIoLayer(),
                                                           httpSession.getIoThread(),
                                                           httpSession.getIoExecutor(),
                                                           SseAcceptor.this,
                                                           getProcessor(),
                                                           sseBindAddress,
                                                           sseRemoteAddress,
                                                           allocator,
                                                           httpSession);
                    sseSession.setHandler(sseHandler);
                    return sseSession;
                }
            });

            // bind the reconnect handler now that session is opened
            SseReconnectHandler reconnectHandler = new SseReconnectHandler(sseSession);
            BridgeAcceptor httpAcceptor = bridgeServiceFactory.newBridgeAcceptor(newHttpBindAddress);
            httpAcceptor.bind(newHttpBindAddress, reconnectHandler, null);

            // unbind the reconnect handler when session is closed
            sseSession.getCloseFuture().addListener(new IoFutureListener<CloseFuture>() {
                @Override
                public void operationComplete(CloseFuture future) {
                    ResourceAddress sseRemoteAddress = sseSession.getRemoteAddress();
                    ResourceAddress newHttpBindAddress = sseRemoteAddress.getTransport();
                    BridgeAcceptor httpAcceptor = bridgeServiceFactory.newBridgeAcceptor(newHttpBindAddress);
                    httpAcceptor.unbind(newHttpBindAddress);
                }
            });

            return reconnectHandler;
        }
    };


    private final class SseReconnectHandler extends IoHandlerAdapter<HttpAcceptSession> {

        private final SseSession sseSession;

        public SseReconnectHandler(SseSession sseSession) {
            this.sseSession = sseSession;
        }

        @Override
        protected void doSessionIdle(HttpAcceptSession httpSession, IdleStatus status) throws Exception {
            // keep-alive
            if (status == WRITER_IDLE) {
                SseMessage sseMessage = new SseMessage();
                sseMessage.setComment("");
                httpSession.write(sseMessage);
            }
        }

        @Override
        protected void doSessionOpened(final HttpAcceptSession httpSession) throws Exception {

            // cancel the timeout future if it is found
            ScheduledFuture<?> timeoutFuture = (ScheduledFuture<?>) sseSession.removeAttribute(TIMEOUT_FUTURE_KEY);
            if (timeoutFuture != null && !timeoutFuture.isDone()) {
                timeoutFuture.cancel(false);
            }

            // trigger keep-alive on write idle
            IoSessionConfig httpConfig = httpSession.getConfig();
            httpConfig.setIdleTime(WRITER_IDLE, (int) TIME_TO_PULSE_MILLIS);

            // check to see if this session can stream, otherwise force long polling
            boolean useHttpStreaming = HttpUtils.canStream(httpSession);

            // Note: Silverlight does not support redirecting from http to https
            // so check .kd=s parameter (downstream=same) to prevent redirecting to https
            if (!useHttpStreaming && !"s".equals(httpSession.getParameter(".kd"))) {
                // lookup secure acceptURI
                URI secureAcceptURI = locateSecureAcceptURI(httpSession);
                if (secureAcceptURI != null) {
                    URI pathInfo = httpSession.getPathInfo();
                    String secureAuthority = secureAcceptURI.getAuthority();
                    String secureAcceptPath = secureAcceptURI.getPath();
                    String request = "https://" + secureAuthority + secureAcceptPath + pathInfo.toString();

                    // determine how to send the response
                    if (supportsRedirect(httpSession)) {
                        // send redirect response
                        httpSession.setStatus(HttpStatus.REDIRECT_MOVED_PERMANENTLY);
                        httpSession.setWriteHeader("Location", request);
                        httpSession.close(false);
                    }
                    else {
                        SseMessage sseMessage = new SseMessage();
                        sseMessage.setLocation(request);
                        sseMessage.setReconnect(true);
                        httpSession.write(sseMessage);
                        httpSession.close(false);
                    }

                    // we have sent the redirect and closed the HTTP session so end reconnect here
                    return;
                }
            }

            // disable pipelining, avoid the need for HTTP chunking
            httpSession.setWriteHeader("Connection", "close");

            // set the content type header
            // must be exactly "text/event-stream" without charset
            String contentType = "text/event-stream";

            // look for content type override
            String contentTypeOverride = httpSession.getParameter(".kc");
            if (contentTypeOverride != null) {
                if (contentTypeOverride.indexOf(';') == -1) {
                    contentTypeOverride += ";charset=UTF-8";
                }
                contentType = contentTypeOverride;
            }

            httpSession.setWriteHeader("X-Content-Type-Options", "nosniff");
            httpSession.setWriteHeader("Content-Type", contentType);

            // configure cache control strategy
            String cacheControl = "no-cache";

            String cacheControlOverride = httpSession.getParameter(".kcc");
            if (cacheControlOverride != null) {
                cacheControl = cacheControlOverride;
            }

            httpSession.setWriteHeader("Cache-Control", cacheControl);

            if (useHttpStreaming) {
                // check for client buffer setting
                String clientBuffer = httpSession.getParameter(".kb");
                if (clientBuffer != null) {
                    // Note: specifying .kb=X on the URL at the client
                    //       overrides the default client buffer size
                    long bufferSize = Long.parseLong(clientBuffer) * 1024L;
                    httpSession.setAttribute(CLIENT_BUFFER_KEY, bufferSize);
                }
            }
            else {
                httpSession.setAttribute(CLIENT_BUFFER_KEY, 0L);
            }

            // check to see if we need to add a padding message to the end of the sent messages
            String clientPadding = httpSession.getParameter(".kp");
            if (clientPadding != null) {
                int paddingSize = Integer.parseInt(clientPadding);
                httpSession.setAttribute(CLIENT_PADDING_KEY, paddingSize);
                httpSession.setAttribute(BYTES_WRITTEN_ON_LAST_FLUSH_KEY, (long) 0);

                if (paddingSize == 0) {
                    httpSession.setWriteHeader("X-Content-Type-Options", "nosniff");
                }
            }

            // check to see if we need to add block padding to the end of the sent messages
            String clientBlockPadding = httpSession.getParameter(".kbp");
            if (clientBlockPadding != null) {
                int paddingSize = Integer.parseInt(clientBlockPadding);
                httpSession.setAttribute(CLIENT_BLOCK_PADDING_KEY, paddingSize);
                httpSession.setWriteHeader("Content-Encoding", "gzip");
            }

            // hook into http session close so we can cleanup sse session
            httpSession.getCloseFuture().addListener(new IoFutureListener<CloseFuture>() {
                @Override
                public void operationComplete(CloseFuture future) {
                    // TODO: check sse session mode, if not recoverable then close sse session.
                    // for now assume always recoverable, so expect a reconnect
                    sseSession.detach(httpSession);

                    // schedule timeout monitor for this sseSession
                    if (!sseSession.isClosing()) {
                        ScheduledFuture<?> timeoutFuture = scheduler.schedule(new TimeoutCommand(sseSession),
                                TIME_TO_TIMEOUT_RECONNECT_MILLIS, TimeUnit.MILLISECONDS);
                        sseSession.setAttribute(TIMEOUT_FUTURE_KEY, timeoutFuture);
                    }
                }
            });

            // detect "create" scenario, send session-specific location to client
            if (!hasSessionId(httpSession)) {
                // write out the location before any other events
                ResourceAddress sseRemoteAddress = sseSession.getRemoteAddress();
                ResourceAddress httpRemoteAddress = sseRemoteAddress.getTransport();
                String httpRemoteURI = httpRemoteAddress.getExternalURI();

                // update the location for reconnect in-band
                SseMessage sseMessage = new SseMessage();
                sseMessage.setLocation(httpRemoteURI);
                httpSession.write(sseMessage);
            }

            // attach now or attach after commit if header flush is required
            if (!useHttpStreaming) {
                // currently this is required for Silverlight as it seems to want some data to be
                // received before it will start to deliver messages
                // this is also needed to detect that streaming has initialized properly
                // so we don't fall back to encrypted streaming or long polling
                SseMessage sseMessage = new SseMessage();
                sseMessage.setComment("");
                httpSession.write(sseMessage);

                final String flushDelay = httpSession.getParameter(".kf");
                if (flushDelay != null) {
                    // commit session and write out headers and any messages already in the queue
                    CommitFuture commitFuture = httpSession.commit();
                    commitFuture.addListener(new IoFutureListener<CommitFuture>() {
                        @Override
                        public void operationComplete(CommitFuture future) {
                            // attach http session to sse session
                            // after delay to force Silverlight client to notice payload
                            long flushDelayMillis = Integer.parseInt(flushDelay);
                            if (flushDelayMillis > 0L) {
                                Runnable command = new AttachParentCommand(sseSession, httpSession);
                                scheduler.schedule(command, flushDelayMillis, TimeUnit.MILLISECONDS);
                            }
                            else {
                                sseSession.attach(httpSession);
                            }
                        }
                    });

                    // note: attach parent is asynchronous
                    return;
                }
            }

            // attach http session to sse session
            sseSession.attach(httpSession);
        }
    }

    private URI locateSecureAcceptURI(HttpAcceptSession session) throws Exception {
        // TODO: same-origin requests must consider cross-origin access control
        // internal redirect to secure resource should not trigger 403 Forbidden
        ResourceAddress localAddress = session.getLocalAddress();
        URI resource =  localAddress.getResource();
        String scheme = resource.getScheme();
        return (scheme.equals("sse+ssl") || scheme.equals("wss")) ? resource : null;
    }

    // TODO: solve cross site redirect properly, this involves changing the client code

    // TODO: always return false for dragonfire
    // TODO: always return true for excalibar
    private boolean supportsRedirect(HttpSession session) {
        // String bufferSize = session.getParameter(".kb");
        // if (bufferSize != null) {
        // return false;
        // }
        // else {
        // return true;
        // }
        // Note: above code was breaking Flash (and possibly Silverlight & Java)
        // because they do not use cross-site access-control, so we need to
        // propagate the "redirect" information in the body so that a different
        // bridge swf (xap, applet) can be used to make a cross-origin request
        // all of these plugin technologies have streaming-capable HTTP requests
        // so they do not send a .kb buffer capacity query parameter, but that
        // does not imply they can handle cross-origin redirects via 301
        // this change reverts a minor optimization for FF 3.5 beta but is still
        // functionally correct on that platform
        return false;
    }

    private class AttachParentCommand implements Runnable {

        private final SseSession sseSession;
        private final BridgeSession parent;

        private AttachParentCommand(SseSession sseSession, BridgeSession parent) {
            this.sseSession = sseSession;
            this.parent = parent;
        }

        @Override
        public void run() {
            sseSession.attach(parent);

            // attaching the parent flushes buffered writes to HTTP response
            // but if connection has high latency, then intermediate TCP node
            // can cause server-delayed write to be combined into the same TCP packet
            // defeating the purpose of the delay (needed by Silverlight)
            // therefore, write an SSE comment a little later as a backup to make
            // sure that the connection does not get stalled
            scheduler.schedule(new FlushCommand(sseSession), TIME_TO_FIRST_WRITE_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    private class FlushCommand implements Runnable {

        private final SseSession session;

        public FlushCommand(SseSession session) {
            this.session = session;
        }

        @Override
        public void run() {
            IoSession parent = session.getParent();
            if (parent != null && !parent.isClosing()) {
                SseMessage sseMessage = new SseMessage();
                sseMessage.setComment("");
                parent.write(sseMessage);
            }
        }

    }

    // close session if reconnect timer elapses and no parent has been attached
    private class TimeoutCommand implements Runnable {

        private SseSession sseSession;

        public TimeoutCommand(SseSession session) {
            this.sseSession = session;
        }

        @Override
        public void run() {
            // technically if this is being called then we have passed the timeout and no reconnect
            // has happened because it would have canceled this task, but doing a check just in case of a race condition
            if (!sseSession.isClosing()) {
                IoSession parent = sseSession.getParent();
                if (parent == null || parent.isClosing()) {
                    // behave similarly to connection reset by peer at NIO layer
                    sseSession.reset(new IOException("Early termination of IO session").fillInStackTrace());
                }
            }
        }
    }
}
