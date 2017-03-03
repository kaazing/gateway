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

import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT;
import static org.kaazing.gateway.transport.BridgeSession.LOCAL_ADDRESS;
import static org.kaazing.gateway.transport.BridgeSession.NEXT_PROTOCOL_KEY;
import static org.kaazing.gateway.transport.BridgeSession.REMOTE_ADDRESS;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.SortedSet;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.transport.Bindings;
import org.kaazing.gateway.transport.BridgeAcceptHandler;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.BridgeSessionInitializer;
import org.kaazing.gateway.transport.LoggingFilter;
import org.kaazing.gateway.transport.NextProtocolBindings;
import org.kaazing.gateway.transport.NextProtocolFilter;
import org.kaazing.gateway.transport.dispatch.ProtocolDispatcher;
import org.slf4j.Logger;

public class NioBridgeAcceptHandler extends BridgeAcceptHandler {

    private final NextProtocolBindings bindings;
    private final ResourceAddressFactory resourceAddressFactory;
    private final BridgeServiceFactory bridgeServiceFactory;
    protected final Logger logger;
    private final Integer idleTimeout;
    private final String transportName;

    public NioBridgeAcceptHandler(AbstractNioAcceptor abstractNioAcceptor,
                                  ResourceAddressFactory resourceAddressFactory, BridgeServiceFactory bridgeServiceFactory,
                                  NextProtocolBindings bindings, Integer idleTimeout,
                                  Logger logger, String transportName) {
        super(abstractNioAcceptor);
        this.resourceAddressFactory = resourceAddressFactory;
        this.bridgeServiceFactory = bridgeServiceFactory;
        this.bindings = bindings;
        this.idleTimeout = idleTimeout;
        this.logger = logger;
        this.transportName = transportName;
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        LoggingFilter.addIfNeeded(logger, session, transportName);

        ResourceAddress localAddress = asResourceAddress(session.getLocalAddress());
        NextProtocolBindings.NextProtocolBinding nioBinding = bindings.getBinding0(localAddress);
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
        } else if (nextProtocolNames.size() == 1) {
            NEXT_PROTOCOL_KEY.set(session, nextProtocolNames.first());
            sessionCreated0(session);
        } else {
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
            filterChain.addLast(AbstractNioAcceptor.NEXT_PROTOCOL_FILTER, nextProtocol);
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

        Bindings.Binding binding = bindings.getBinding(candidateAddress);
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
        String remoteExternalURI = ((AbstractNioAcceptor) getAcceptor()).asResourceURI((InetSocketAddress) remoteSocketAddress);
        ResourceAddress remoteAddress = resourceAddressFactory.newResourceAddress(remoteExternalURI, nextProtocol);
        REMOTE_ADDRESS.set(session, remoteAddress);

        BridgeSessionInitializer<? extends IoFuture> initializer = binding.initializer();
        if (initializer != null) {
            initializer.initializeSession(session, null);
        }

//        if (nioBinding.getBinding(localAddress) != null) {
//            final ResourceAddress boundAddress = nioBinding.getBinding(localAddress).bindAddress();
            ((AbstractNioAcceptor)getAcceptor()).registerAcceptFilters(localAddress, session);
//        }

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
            return ((AbstractNioAcceptor) getAcceptor()).createResourceAddress(inetSocketAddress);
        }
        return (ResourceAddress) socketAddress;
    }
}
