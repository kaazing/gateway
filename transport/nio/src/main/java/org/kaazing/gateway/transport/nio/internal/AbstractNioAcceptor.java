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
package org.kaazing.gateway.transport.nio.internal;

import static java.lang.String.format;
import static java.util.Collections.singleton;
import static org.kaazing.gateway.resource.address.ResourceAddress.ALTERNATE;
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORTED_URI;
import static org.kaazing.gateway.transport.BridgeSession.LOCAL_ADDRESS;
import static org.kaazing.gateway.transport.BridgeSession.NEXT_PROTOCOL_KEY;
import static org.kaazing.gateway.transport.BridgeSession.REMOTE_ADDRESS;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_IDLE_TIMEOUT;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Resource;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.DefaultIoSessionDataStructureFactory;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.resource.address.Comparators;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.resource.address.uri.URIUtils;
import org.kaazing.gateway.transport.Bindings;
import org.kaazing.gateway.transport.Bindings.Binding;
import org.kaazing.gateway.transport.BridgeAcceptHandler;
import org.kaazing.gateway.transport.BridgeAcceptor;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.BridgeSessionInitializer;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.IoSessionAdapterEx;
import org.kaazing.gateway.transport.LoggingFilter;
import org.kaazing.gateway.transport.NextProtocolBindings;
import org.kaazing.gateway.transport.NextProtocolBindings.NextProtocolBinding;
import org.kaazing.gateway.transport.NextProtocolFilter;
import org.kaazing.gateway.transport.NioBindException;
import org.kaazing.gateway.transport.dispatch.ProtocolDispatcher;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.future.BindFuture;
import org.kaazing.mina.core.future.DefaultUnbindFuture;
import org.kaazing.mina.core.future.UnbindFuture;
import org.kaazing.mina.core.service.IoAcceptorEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.session.IoSessionConfigEx.ChangeListener;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractNioAcceptor implements BridgeAcceptor {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractNioAcceptor.class);

    private static final String NEXT_PROTOCOL_FILTER = "nio#next-protocol";

    private final AtomicBoolean started;
    private final NextProtocolBindings bindings;
    private final SortedSet<ResourceAddress> boundAuthorities;

    private IoAcceptorEx acceptor;
    private ScheduledExecutorService unbindScheduler;
    private boolean skipIPv6Addresses = false;

    protected ResourceAddressFactory resourceAddressFactory;
    protected BridgeServiceFactory bridgeServiceFactory;

    protected final Properties configuration;
    protected final Logger logger;

    private final Integer idleTimeout;

    public AbstractNioAcceptor(Properties configuration, Logger logger) {
        if (configuration == null) {
            throw new NullPointerException("configuration");
        }
        if (logger == null) {
            throw new NullPointerException("logger");
        }
        this.configuration = configuration;
        this.logger = logger;
        started = new AtomicBoolean(false);
        bindings = new NextProtocolBindings();
        boundAuthorities = new ConcurrentSkipListSet<>(Comparators.compareResourceOrigin());

        String preferIPv4NetworkStack = System.getProperty("java.net.preferIPv4Stack");
        if ("true".equalsIgnoreCase(preferIPv4NetworkStack)) {
            skipIPv6Addresses = true;
        }

        idleTimeout = TCP_IDLE_TIMEOUT.getIntProperty(configuration);
    }

    @Resource(name = "bridgeServiceFactory")
    public void setBridgeServiceFactory(BridgeServiceFactory bridgeServiceFactory) {
        this.bridgeServiceFactory = bridgeServiceFactory;
    }

    @Resource(name = "resourceAddressFactory")
    public void setResourceAddressFactory(ResourceAddressFactory factory) {
        this.resourceAddressFactory = factory;
    }

    /* for testing observability */
    Bindings<?> getBindings() {
        return bindings;
    }

    public boolean emptyBindings() {
        return bindings.isEmpty();
    }

    private final BridgeAcceptHandler tcpHandler = new BridgeAcceptHandler(this) {
        @Override
        public void sessionCreated(IoSession session) throws Exception {
            LoggingFilter.addIfNeeded(logger, session, getTransportName());

            ResourceAddress localAddress = asResourceAddress(session.getLocalAddress());
            NextProtocolBinding nioBinding = bindings.getBinding0(localAddress);
            if (nioBinding == null) {
                // Not currently bound (A concurrent unbind may have removed the binding)
                session.close(true);
                return;
            }

            if (idleTimeout != null && idleTimeout > 0) {
                session.getFilterChain().addLast("idle", new NioIdleFilter(logger, idleTimeout, session));
            }

            // note: defer sessionCreated until sessionOpened to support (optional) protocol dispatch
            SortedSet<String> nextProtocolNames = nioBinding.getNextProtocolNames();
            if (nextProtocolNames == null || nextProtocolNames.isEmpty()) {
                NEXT_PROTOCOL_KEY.set(session, null);
                sessionCreated0(session);
            }
            else if (nextProtocolNames.size() == 1) {
                NEXT_PROTOCOL_KEY.set(session, nextProtocolNames.first());
                sessionCreated0(session);
            }
            else {
                Collection<ProtocolDispatcher> dispatchers = bridgeServiceFactory.getTransportFactory().getProtocolDispatchers().values();

                // sessionCreated will be sent down pipeline again when next-protocol has been determined
                NextProtocolFilter nextProtocol = new NextProtocolFilter(dispatchers) {
                    @Override
                    protected void flushInboundEvents(NextFilter nextFilter, IoSession session) throws Exception {
                        // defer sessionCreated until next-protocol determined
                        sessionCreated0(session);
                        // flush other events after sessionCreated
                        super.flushInboundEvents(nextFilter, session);
                    }
                };
                IoFilterChain filterChain = session.getFilterChain();
                filterChain.addLast(NEXT_PROTOCOL_FILTER, nextProtocol);
            }

        }

        private void sessionCreated0(IoSession session) throws Exception {
            SocketAddress boundAddress0 = session.getLocalAddress();
            ResourceAddress boundAddress = asResourceAddress(boundAddress0);
            String candidateURI = boundAddress.getExternalURI();

            ResourceOptions candidateOptions = ResourceOptions.FACTORY.newResourceOptions(boundAddress);
            String nextProtocol = NEXT_PROTOCOL_KEY.get(session);
            candidateOptions.setOption(NEXT_PROTOCOL, nextProtocol);
            candidateOptions.setOption(TRANSPORT, LOCAL_ADDRESS.get(session));
            ResourceAddress candidateAddress = resourceAddressFactory.newResourceAddress(candidateURI, candidateOptions);

            Binding binding = bindings.getBinding(candidateAddress);
            if (binding == null) {
                // next-protocol not currently bound for this address
                session.close(true);
                return;
            }

            IoHandler handler = binding.handler();
            DELEGATE_KEY.set(session, handler);

            // This is added to store the associated resource address
            // with the nio socket session.  An NioSocketSession localAddress
            // is still an InetSocketAddress, but we can count on this
            // attribute to give us the bound resource address.
            ResourceAddress localAddress = binding.bindAddress();
            LOCAL_ADDRESS.set(session, localAddress);

            SocketAddress remoteSocketAddress = session.getRemoteAddress();
            String remoteExternalURI = asResourceURI((InetSocketAddress) remoteSocketAddress);
            ResourceAddress remoteAddress = resourceAddressFactory.newResourceAddress(remoteExternalURI, nextProtocol);
            REMOTE_ADDRESS.set(session, remoteAddress);

            BridgeSessionInitializer<? extends IoFuture> initializer = binding.initializer();
            if (initializer != null) {
                initializer.initializeSession(session, null);
            }

            // next-protocol has been determined
            super.sessionCreated(session);
        }

        @Override
        public void sessionClosed(IoSession session) throws Exception {
            NEXT_PROTOCOL_KEY.remove(session);
            LOCAL_ADDRESS.remove(session);
            REMOTE_ADDRESS.remove(session);

            super.sessionClosed(session);
        }

        private ResourceAddress asResourceAddress(SocketAddress socketAddress) {
            if (socketAddress instanceof InetSocketAddress) {
                InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
                return createResourceAddress(inetSocketAddress);
            }

            return (ResourceAddress) socketAddress;
        }


    };

    private ResourceAddress createResourceAddress(InetSocketAddress inetSocketAddress) {
        String transport = asResourceURI(inetSocketAddress);
        return resourceAddressFactory.newResourceAddress(transport);
    }

    private String asResourceURI(InetSocketAddress inetSocketAddress) {
        String transportName = getTransportName();
        InetAddress inetAddress = inetSocketAddress.getAddress();
        String hostAddress = inetAddress.getHostAddress();
        String addressFormat = (inetAddress instanceof Inet6Address) ? "%s://[%s]:%s" : "%s://%s:%s";
        int port = inetSocketAddress.getPort();
        return format(addressFormat, transportName, hostAddress, port);
    }

    protected final void init() {
        acceptor = initAcceptor(null);

        acceptor.setSessionDataStructureFactory(new DefaultIoSessionDataStructureFactory());
        acceptor.setHandler(tcpHandler);
    }

    @Resource(name = "schedulerProvider")
    public final void setSchedulerProvider(SchedulerProvider provider) {
        unbindScheduler = provider.getScheduler(this + "_unbind", true);
    }

    @Override
    public IoHandler getHandler(ResourceAddress address) {
        Binding binding = bindings.getBinding(address);
        return (binding != null) ? binding.handler() : null;
    }



    @Override
    public void bind(final ResourceAddress address,
                     IoHandler handler,
                     BridgeSessionInitializer<? extends IoFuture> initializer) throws NioBindException {

        initIfNecessary();

        ResourceAddress failedAddress = null;
        ResourceAddress currentAddress = address;
        while (currentAddress != null) {
            ResourceAddress transport = currentAddress.getTransport();
            if (transport != null) {

                Binding newBinding = new Binding(currentAddress, handler, initializer);
                Binding binding = bindings.addBinding(newBinding);
                if (binding != null) {
                    failedAddress = currentAddress;
                }

                ResourceAddress transportAddress = currentAddress.getTransport();

                BridgeAcceptor acceptor = bridgeServiceFactory.newBridgeAcceptor(transportAddress);

                acceptor.bind(transportAddress, tcpBridgeHandler, null);
            }
            else {
                // note: [Tcp,Udp]ResourceAddressFactorySpi resolves bind option (then network context) already
                URI resource = currentAddress.getResource();
                InetSocketAddress socketAddress = asSocketAddress(currentAddress);

                if (!(skipIPv6Addresses && (socketAddress.getAddress() instanceof Inet6Address))) {
                    if (socketAddress.isUnresolved()) {
                        failedAddress = currentAddress;
                        throw new NioBindException("Unable to resolve address " + resource.getHost() + ":" + resource.getPort(), singleton(failedAddress));
                    }

                    boolean needsAcceptorBind = !boundAuthorities.contains(currentAddress);

                    Binding newBinding = new Binding(currentAddress, handler, initializer);
                    Binding binding = bindings.addBinding(newBinding);
                    if (binding != null) {
                        failedAddress = currentAddress;
                    }

                    if (needsAcceptorBind) {
                        // Asynchronous bind is needed to avoid a Netty error if bind is called from an IO worker thread.
                        // This can happens from connect in SocksConnector in reverse mode (see KG-7179 for details).
                        BindFuture bound = acceptor.bindAsync(socketAddress);
                        bound.awaitUninterruptibly();
                        Throwable e = bound.getException();
                        if (e != null) {
                            boolean preferIPv6Stack = "true".equalsIgnoreCase(System.getProperty("java.net.preferIPv6Stack"));
                            if (!preferIPv6Stack && (e instanceof SocketException) && (socketAddress.getAddress() instanceof Inet6Address)) {
                                skipIPv6Addresses = true;
                            } else {
                                String error = "Unable to bind resource: " + resource + " cause: " + e.getMessage();
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug(error, e);
                                } else {
                                    LOG.error(error + " "+ e.getMessage());
                                }
                                throw new RuntimeException(error);
                            }
                        } else {
                            boundAuthorities.add(currentAddress);
                            LOG.info("Bound to resource: " + resource);
                        }
                    }
                }
            }

            // bind alternates as well
            currentAddress = currentAddress.getOption(ALTERNATE);
        }

        if (failedAddress != null) {
            throw new NioBindException("Address already bound to different handlers, error in the configuration: " + failedAddress + " address: " + address,
                                       Collections.singletonList(failedAddress));
        }
    }

    @Override
    public UnbindFuture unbind(final ResourceAddress address) {
        // KG-3458: do the unbind asynchronously to avoid blocking the IoProcessor thread in cases where a session close
        // future listener does the unbind (e.g. to unbind a dynamic bind done for SSO revalidate). This was causing
        // a hang when there's a race with Gateway.destroy().
        if (!unbindScheduler.isShutdown()) {
            final UnbindFuture future = new DefaultUnbindFuture();
            unbindScheduler.submit(new FutureTask<>(new Runnable() {
                @Override
                public void run() {
                    try {
                        unbindInternal(address);
                        future.setUnbound();
                    } catch (ThreadDeath td) {
                        throw td;
                    } catch (Throwable t) {
                        LOG.error("Exception during unbinding:\n"+address, t);
                        //System.out.println("Exception during unbinding:\n"+address+"\n");
                        //t.printStackTrace(System.out);
                        future.setException(t);
                        throw new RuntimeException(t);
                    }
                }
            }, Boolean.TRUE));
            return future;
        }
        else {
            return DefaultUnbindFuture.succeededFuture();
        }
    }

    protected final void initIfNecessary() {
        if (!started.get()) {
            synchronized (started) {
                if (!started.get()) {
                    init();
                    started.set(true);
                }
            }
        }
    }

    private void unbindInternal(ResourceAddress address) {

        while (address != null) {

            ResourceAddress transport = address.getTransport();
            if (transport != null) {

                Binding nioBinding = bindings.getBinding(address);
                if (nioBinding != null) {
                    bindings.removeBinding(address, nioBinding);
                }

                BridgeAcceptor bridgeAcceptor = bridgeServiceFactory.newBridgeAcceptor(transport);
                bridgeAcceptor.unbind(address.getTransport());
            }
            else {
                try {
                    // note: [Tcp,Udp]ResourceAddressFactorySpi resolves bind option (then network context) already

                    // ref count this binding so we don't unbind until every acceptor is unbound
                    Binding nioBinding = bindings.getBinding(address);
                    if (nioBinding != null) {
                        boolean removed = bindings.removeBinding(address, nioBinding);
                        if (removed) {
                            ResourceAddress bindAddress = nioBinding.bindAddress();
                            InetSocketAddress socketAddress = asSocketAddress(bindAddress);
                            acceptor.unbind(socketAddress);
                            boundAuthorities.remove(bindAddress);
                        }
                    }
                } catch (RuntimeException e) {
                    LOG.error("Error while unbinding " + address, e);
                }
            }

            address = address.getOption(ALTERNATE);
        }
    }

    @Override
    public void dispose() {
        if (acceptor != null) {
            acceptor.dispose();
        }
    }

    protected abstract IoAcceptorEx initAcceptor(final IoSessionInitializer<? extends IoFuture> initializer);

    protected abstract String getTransportName();

    private static InetSocketAddress asSocketAddress(ResourceAddress address) {
        URI location = address.getResource();
        return new InetSocketAddress(location.getHost(), location.getPort());
    }


    //
    // Tcp as a "virtual" bridge session when we specify tcp.transport option in a resource address.
    //

    public static final String TCP_SESSION_KEY = "tcp.bridgeSession.key"; // holds tcp session acting as a bridge
    public static final String PARENT_KEY = "tcp.parentKey.key"; // holds parent of tcp bridge session

    private final IoProcessorEx<IoSessionAdapterEx> tcpBridgeProcessor = new IoProcessorEx<IoSessionAdapterEx>() {
        @Override
        public void add(IoSessionAdapterEx session) {
            // Do nothing
        }

        @Override
        public void flush(IoSessionAdapterEx session) {
            IoSession parent = (IoSession) session.getAttribute(PARENT_KEY);
            WriteRequest req = session.getWriteRequestQueue().poll(session);

            // Check that the request is not null. If the session has been closed,
            // we may not have any pending requests.
            if (req != null) {
                final WriteFuture tcpBridgeWriteFuture = req.getFuture();
                Object m = req.getMessage();
                if (m instanceof IoBufferEx && ((IoBufferEx) m).remaining() == 0) {
                    session.setCurrentWriteRequest(null);
                    tcpBridgeWriteFuture.setWritten();
                } else {
                    WriteFuture parentWriteFuture = parent.write(m);
                    parentWriteFuture.addListener(new IoFutureListener<WriteFuture>() {
                        @Override
                        public void operationComplete(WriteFuture future) {
                            if (future.isWritten()) {
                                tcpBridgeWriteFuture.setWritten();
                            }
                            else {
                                tcpBridgeWriteFuture.setException(future.getException());
                            }
                        }
                    });
                }
            }
        }

        @Override
        public void remove(IoSessionAdapterEx session) {
            LOG.debug("AbstractNioAcceptor Fake Processor remove session "+session);
            IoSession parent = (IoSession) session.getAttribute(PARENT_KEY);
            parent.close(false);
            acceptor.getListeners().fireSessionDestroyed(session);
        }

        @Override
        public void updateTrafficControl(IoSessionAdapterEx session) {
            // Do nothing
        }

        @Override
        public void dispose() {
            // Do nothing
        }

        @Override
        public boolean isDisposed() {
            return false;
        }

        @Override
        public boolean isDisposing() {
            return false;
        }

    };

    private final IoHandlerAdapter<IoSessionEx> tcpBridgeHandler =
            new IoHandlerAdapter<IoSessionEx>() {

                @Override
                public void doSessionClosed(IoSessionEx session) throws Exception {
                    LOG.debug("AbstractNioAcceptor.doSessionClosed for session "+session);
                    NEXT_PROTOCOL_KEY.remove(session);
                    LOCAL_ADDRESS.remove(session);
                    REMOTE_ADDRESS.remove(session);
                    IoSession tcpBridgeSession = getTcpBridgeSession(session);
                    if ( tcpBridgeSession != null ) {
                        tcpBridgeSession.getFilterChain().fireSessionClosed();
                    }
                }

                @Override
                protected void doSessionCreated(IoSessionEx session) throws Exception {
                    LoggingFilter.addIfNeeded(logger, session, getTransportName());

                    ResourceAddress candidateAddress = getCandidateResourceAddress(session);
                    NextProtocolBinding nextBinding = bindings.getBinding0(candidateAddress);
                    if (nextBinding == null) {
                        // next-protocol not currently bound for this address
                        session.close(true);
                        return;
                    }
                    SortedSet<String> nextProtocolNames = nextBinding.getNextProtocolNames();
                    if (nextProtocolNames == null || nextProtocolNames.isEmpty()) {
                        NEXT_PROTOCOL_KEY.set(session, null);
                    }
                    else if (nextProtocolNames.size() == 1) {
                        NEXT_PROTOCOL_KEY.set(session, nextProtocolNames.first());
                    }
                    else {
                        Collection<ProtocolDispatcher> dispatchers = bridgeServiceFactory.getTransportFactory().getProtocolDispatchers().values();

                        // sessionCreated will be sent down pipeline again when next-protocol has been determined
                        NextProtocolFilter nextProtocol = new NextProtocolFilter(dispatchers);
                        IoFilterChain filterChain = session.getFilterChain();
                        filterChain.addLast(NEXT_PROTOCOL_FILTER, nextProtocol);
                    }

                }

                @Override
                protected void doSessionOpened(final IoSessionEx session) throws Exception {
                    ResourceAddress candidateAddress = getCandidateResourceAddress(session);
                    final Binding binding = bindings.getBinding(candidateAddress);
                    if (binding == null) {
                        // next-protocol not currently bound for this address
                        session.close(true);
                        return;
                    }

                    IoSessionAdapterEx newTcpBridgeSession = new IoSessionAdapterEx(session.getIoThread(),
                                                                                    session.getIoExecutor(),
                                                                                    acceptor,
                                                                                    tcpBridgeProcessor,
                                                                                    acceptor.getSessionDataStructureFactory());
                    newTcpBridgeSession.setLocalAddress(binding.bindAddress());
                    newTcpBridgeSession.setRemoteAddress(resourceAddressFactory.newResourceAddress(binding.bindAddress(),
                                                                                                   REMOTE_ADDRESS.get(session)));

                    newTcpBridgeSession.setHandler(binding.handler());

                    newTcpBridgeSession.setTransportMetadata(acceptor.getTransportMetadata());

                    newTcpBridgeSession.setAttribute(PARENT_KEY, session);
                    newTcpBridgeSession.setTransportMetadata(acceptor.getTransportMetadata());

                    // Propagate changes to idle time to the parent session
                    newTcpBridgeSession.getConfig().setChangeListener(new ChangeListener() {
                        @Override
                        public void idleTimeInMillisChanged(IdleStatus status, long idleTime) {
                            session.getConfig().setIdleTimeInMillis(status, idleTime);
                        }
                    });

                    session.setAttribute(TCP_SESSION_KEY, newTcpBridgeSession);

                    if ( binding.initializer() != null ) {
                        binding.initializer().initializeSession(newTcpBridgeSession, null);
                    }

                    acceptor.getListeners().fireSessionCreated(newTcpBridgeSession);
                }

                private ResourceAddress getCandidateResourceAddress(IoSessionEx session) {
                    // Build candidate address from session
                    final ResourceAddress candidateTransportAddress = REMOTE_ADDRESS.get(session);
                    URI candidateURI = candidateTransportAddress.getOption(TRANSPORTED_URI);

                    ResourceOptions candidateOptions = ResourceOptions.FACTORY.newResourceOptions();

                    candidateOptions.setOption(NEXT_PROTOCOL, NEXT_PROTOCOL_KEY.get(session));
                    candidateOptions.setOption(TRANSPORT, candidateTransportAddress);
                    return resourceAddressFactory.newResourceAddress(URIUtils.uriToString(candidateURI), candidateOptions);
                }

                private IoSession getTcpBridgeSession(IoSession session) {
                    return (IoSession) session.getAttribute(TCP_SESSION_KEY);
                }


                @Override
                protected void doExceptionCaught(IoSessionEx session, Throwable cause) throws Exception {
                    //TODO: consider tcpBridgeSession.reset as an alternate impl.
                    LOG.debug("NioSocketAcceptor exception caught", cause);

                    if (!session.isClosing()) {
                        IoSession tcpBridgeSession = getTcpBridgeSession(session);
                        if (tcpBridgeSession != null) {
                            tcpBridgeSession.getFilterChain().fireExceptionCaught(cause);
                        }
                    } else {
                        LOG.debug("Unexpected exception while session is closing", cause);
                    }
                }

                @Override
                public void doSessionIdle(IoSessionEx session, IdleStatus status) throws Exception {
                    IoSession tcpBridgeSession = getTcpBridgeSession(session);
                    tcpBridgeSession.getFilterChain().fireSessionIdle(status);
                }


                @Override
                protected void doMessageReceived(IoSessionEx session, Object message) throws Exception {
                    IoSession tcpBridgeSession = getTcpBridgeSession(session);
                    tcpBridgeSession.getFilterChain().fireMessageReceived(message);
                }


            };
}
