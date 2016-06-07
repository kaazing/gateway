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
package org.kaazing.gateway.transport.bio;

import static java.lang.String.format;
import static org.kaazing.gateway.resource.address.ResourceAddress.ALTERNATE;
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT;
import static org.kaazing.gateway.transport.BridgeSession.LOCAL_ADDRESS;
import static org.kaazing.gateway.transport.BridgeSession.NEXT_PROTOCOL_KEY;
import static org.kaazing.gateway.transport.BridgeSession.REMOTE_ADDRESS;
import static org.kaazing.mina.core.future.DefaultUnbindFuture.combineFutures;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.transport.Bindings.Binding;
import org.kaazing.gateway.transport.BridgeAcceptHandler;
import org.kaazing.gateway.transport.BridgeAcceptor;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.BridgeSessionInitializer;
import org.kaazing.gateway.transport.NamedPipeAddress;
import org.kaazing.gateway.transport.NextProtocolBindings;
import org.kaazing.gateway.transport.NextProtocolBindings.NextProtocolBinding;
import org.kaazing.gateway.transport.NextProtocolFilter;
import org.kaazing.gateway.transport.SocketAddressFactory;
import org.kaazing.gateway.transport.dispatch.ProtocolDispatcher;
import org.kaazing.mina.core.future.DefaultUnbindFuture;
import org.kaazing.mina.core.future.UnbindFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBioAcceptor<T extends SocketAddress> implements BridgeAcceptor {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractBioAcceptor.class);

    private static final String NEXT_PROTOCOL_FILTER = "bio#next-protocol";

    private final NextProtocolBindings bindings;
    private IoAcceptor acceptor;
    private SocketAddressFactory<T> socketAddressFactory;
    private BridgeServiceFactory bridgeServiceFactory;
    private ResourceAddressFactory resourceAddressFactory;

    private final AtomicBoolean started;

    public AbstractBioAcceptor() {
        bindings = new NextProtocolBindings();
        started = new AtomicBoolean(false);
    }

    public boolean emptyBindings() {
        return bindings.isEmpty();
    }

    protected final void init() {
        socketAddressFactory = initSocketAddressFactory();
        bridgeServiceFactory = initBridgeServiceFactory();
        resourceAddressFactory = initResourceAddressFactory();
        acceptor = initAcceptor();
        acceptor.setHandler(new BridgeAcceptHandler(this) {
            @Override
            public void sessionCreated(IoSession session) throws Exception {
                ResourceAddress localAddress = asResourceAddress(session.getLocalAddress());
                NextProtocolBinding nioBinding = bindings.getBinding0(localAddress);
                if (nioBinding == null) {
                    // Not currently bound (unbind may have happened concurrently)
                    session.close(true);
                    return;
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
                ResourceAddress remoteAddress = asResourceAddress(remoteSocketAddress, nextProtocol);
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

            private ResourceAddress asResourceAddress(SocketAddress socketAddress, String nextProtocol) {

                if (socketAddress instanceof InetSocketAddress) {
                    InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
                    return createResourceAddress(inetSocketAddress, nextProtocol);
                }
                else if (socketAddress instanceof NamedPipeAddress) {
                    NamedPipeAddress pipeAddress = (NamedPipeAddress) socketAddress;
                    return createResourceAddress(pipeAddress, nextProtocol);
                }
                else if (socketAddress instanceof MulticastAddress) {
                    MulticastAddress multicastAddress = (MulticastAddress) socketAddress;
                    return createResourceAddress(multicastAddress, nextProtocol);
                }

                return (ResourceAddress) socketAddress;
            }

            private ResourceAddress asResourceAddress(SocketAddress socketAddress) {
                return asResourceAddress(socketAddress, null);
            }

            private ResourceAddress createResourceAddress(MulticastAddress multicastAddress, String nextProtocol) {
                String transportName = getTransportName();
                InetAddress inetAddress = multicastAddress.getGroupAddress();
                String hostAddress = inetAddress.getHostAddress();
                String addressFormat = (inetAddress instanceof Inet6Address) ? "%s://[%s]:%s" : "%s://%s:%s";
                int port = multicastAddress.getBindPort();
                String transport = format(addressFormat, transportName, hostAddress, port);
                return resourceAddressFactory.newResourceAddress(transport, nextProtocol);
            }

            private ResourceAddress createResourceAddress(NamedPipeAddress namedPipeAddress, String nextProtocol) {
                String transportName = getTransportName();
                String addressFormat = "%s://%s";
                String pipeName = namedPipeAddress.getPipeName();
                String transport = format(addressFormat, transportName, pipeName);
                return resourceAddressFactory.newResourceAddress(transport, nextProtocol);
            }

            private ResourceAddress createResourceAddress(InetSocketAddress inetSocketAddress, String nextProtocol) {
                String transportName = getTransportName();
                InetAddress inetAddress = inetSocketAddress.getAddress();
                String hostAddress = inetAddress.getHostAddress();
                String addressFormat = (inetAddress instanceof Inet6Address) ? "%s://[%s]:%s" : "%s://%s:%s";
                int port = inetSocketAddress.getPort();
                String transport = format(addressFormat, transportName, hostAddress, port);
                return resourceAddressFactory.newResourceAddress(transport, nextProtocol);
            }
        });
    }

    protected abstract SocketAddressFactory<T> initSocketAddressFactory();
    protected abstract ResourceAddressFactory initResourceAddressFactory();
    protected abstract BridgeServiceFactory initBridgeServiceFactory();

    @Override
    public IoHandler getHandler(ResourceAddress address) {
        Binding binding = bindings.getBinding(address);
        return (binding != null) ? binding.handler() : null;
    }

    @Override
    public void bind(ResourceAddress address, IoHandler handler, BridgeSessionInitializer<? extends IoFuture> initializer) {

        if (!started.get()) {
            synchronized (started) {
                if (!started.get()) {
                    init();
                    started.set(true);
                }
            }
        }

        ResourceAddress failedAddress = null;

        while (address != null) {

            URI resource =  address.getResource();
            T socketAddress = socketAddressFactory.createSocketAddress(address);
            
            ResourceAddress transport = address.getTransport();
            if (transport != null) {
                BridgeAcceptor acceptor = bridgeServiceFactory.newBridgeAcceptor(transport);
                acceptor.bind(transport, handler, initializer);
            }
            else {
                NextProtocolBinding nextBinding = bindings.getProtocolBinding(address);
                boolean needsAcceptorBind = (nextBinding == null);

                Binding newBinding = new Binding(address, handler, initializer);
                Binding binding = bindings.addBinding(newBinding);
                if (binding != null) {
                    failedAddress = address;
                }

                if (needsAcceptorBind) {
                    try {
                        acceptor.bind(socketAddress);
                        LOG.info("Bound to resource: " + resource);
                    }
                    catch (IOException e) {
                        String error = "Unable to bind to resource: " + resource
                                + " cause: " + e.getMessage();
                        LOG.error(error);
                        throw new RuntimeException(error);
                    }
                }
            }
            
            address = address.getOption(ALTERNATE);
        }
        
        if (failedAddress != null) {
            throw new BioBindException("Address already bound to different handlers, error in the configuration: "+failedAddress+" address: "+address,
                    Collections.singletonList(failedAddress));
        }
    }

    @Override
    public UnbindFuture unbind(ResourceAddress address) {
        UnbindFuture unbindFuture = DefaultUnbindFuture.succeededFuture();

        while (address != null) {
            ResourceAddress transport = address.getTransport();
            if (transport != null) {
                BridgeAcceptor bridgeAcceptor = bridgeServiceFactory.newBridgeAcceptor(transport);
                UnbindFuture newUnbindFuture = bridgeAcceptor.unbind(transport);
                unbindFuture = combineFutures(unbindFuture, newUnbindFuture);
            } else {
                try {
                    // note: [Tcp,Udp]ResourceAddressFactorySpi resolves bind option (then network context) already
        
                    // ref count this binding so we don't unbind until every acceptor is unbound
                    Binding binding = bindings.getBinding(address);
                    if (binding != null) {
                        boolean removed = bindings.removeBinding(address, binding);
                        if (removed) {
                            ResourceAddress bindAddress = binding.bindAddress();
                            T socketAddress = socketAddressFactory.createSocketAddress(bindAddress);
                            acceptor.unbind(socketAddress);
                        }
                    }
        //            if ( LOG.isDebugEnabled() ) {
        //                debugBindings(address);
        //            }
                } catch (RuntimeException e) {
                    LOG.error("Error while unbinding "+address, e);
                }
            }
            
            address = address.getOption(ALTERNATE);
        }
        return unbindFuture; // acceptor.unbind is synchronous since acceptor is not a BridgeAcceptor
    }

    @Override
    public void dispose() {
        if (acceptor != null) {
            acceptor.dispose();
        }
    }

    protected IoAcceptor getAcceptor() {
    	return acceptor;
    }

    protected abstract IoAcceptor initAcceptor();

    protected abstract String getTransportName();
}
