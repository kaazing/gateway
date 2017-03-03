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
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_IDLE_TIMEOUT;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.util.Collections;
import java.util.Properties;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Resource;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.DefaultIoSessionDataStructureFactory;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.gateway.resource.address.Comparators;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.Bindings;
import org.kaazing.gateway.transport.Bindings.Binding;
import org.kaazing.gateway.transport.BridgeAcceptor;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.BridgeSessionInitializer;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.IoSessionAdapterEx;
import org.kaazing.gateway.transport.NextProtocolBindings;
import org.kaazing.gateway.transport.NioBindException;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.future.BindFuture;
import org.kaazing.mina.core.future.DefaultUnbindFuture;
import org.kaazing.mina.core.future.UnbindFuture;
import org.kaazing.mina.core.service.IoAcceptorEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractNioAcceptor implements BridgeAcceptor {

    public static final Logger LOG = LoggerFactory.getLogger(AbstractNioAcceptor.class);

    public static final String NEXT_PROTOCOL_FILTER = "nio#next-protocol";

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

    private final IoHandlerAdapter<IoSessionEx> tcpBridgeHandler;

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
        final IoProcessorEx<IoSessionAdapterEx> tcpBridgeProcessor = new NioAcceptorTcpBridgeProcessor(this.acceptor);
        tcpBridgeHandler = new NioAcceptorTcpBridgeHandler(bindings, acceptor, resourceAddressFactory, bridgeServiceFactory, logger, getTransportName(), tcpBridgeProcessor);
    }

    @Resource(name = "bridgeServiceFactory")
    public void setBridgeServiceFactory(BridgeServiceFactory bridgeServiceFactory) {
        this.bridgeServiceFactory = bridgeServiceFactory;
    }

    @Resource(name = "resourceAddressFactory")
    public void setResourceAddressFactory(ResourceAddressFactory factory) {
        this.resourceAddressFactory = factory;
    }

    @Resource(name = "schedulerProvider")
    public final void setSchedulerProvider(SchedulerProvider provider) {
        unbindScheduler = provider.getScheduler(this + "_unbind", true);
    }

    /* for testing observability */
    public Bindings<?> getBindings() {
        return bindings;
    }

    public boolean emptyBindings() {
        return bindings.isEmpty();
    }

    protected ResourceAddress createResourceAddress(InetSocketAddress inetSocketAddress) {
        String transport = asResourceURI(inetSocketAddress);
        return resourceAddressFactory.newResourceAddress(transport);
    }

    protected String asResourceURI(InetSocketAddress inetSocketAddress) {
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
        acceptor.setHandler(new NioBridgeAcceptHandler(this, // TODO maybe this reference could be removed
            resourceAddressFactory, bridgeServiceFactory, bindings, idleTimeout, logger, getTransportName()));
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
            if (transport != null) { // Udp over Socks over Tcp might get us here
                Binding newBinding = new Binding(currentAddress, handler, initializer);
                Binding binding = bindings.addBinding(newBinding);
                if (binding != null) {
                    failedAddress = currentAddress;
                }
                ResourceAddress transportAddress = currentAddress.getTransport();
                BridgeAcceptor bridgeAcceptor = bridgeServiceFactory.newBridgeAcceptor(transportAddress);
                bridgeAcceptor.bind(transportAddress, tcpBridgeHandler, null);
            } else {
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
                        bindAcceptor(currentAddress, resource, socketAddress);
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

    private void bindAcceptor(ResourceAddress currentAddress, URI resource, InetSocketAddress socketAddress) {
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

    @Override
    public UnbindFuture unbind(final ResourceAddress address) {
        // KG-3458: do the unbind asynchronously to avoid blocking the IoProcessor thread in cases where a session close
        // future listener does the unbind (e.g. to unbind a dynamic bind done for SSO revalidate). This was causing
        // a hang when there's a race with Gateway.destroy().
        if (!unbindScheduler.isShutdown()) {
            final UnbindFuture future = new DefaultUnbindFuture();
            unbindScheduler.submit(new FutureTask<>(() -> {
                try {
                    unbindInternal(address);
                    future.setUnbound();
                } catch (ThreadDeath td) {
                    throw td;
                } catch (Throwable t) {
                    LOG.error("Exception during unbinding:\n" + address, t);
                    future.setException(t);
                    throw new RuntimeException(t);
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

    // TODO confirm session config should not be used instead
    protected abstract void registerAcceptFilters(ResourceAddress boundAddress, IoSession session);

    private static InetSocketAddress asSocketAddress(ResourceAddress address) {
        URI location = address.getResource();
        return new InetSocketAddress(location.getHost(), location.getPort());
    }

}
