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
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORTED_URI;
import static org.kaazing.gateway.transport.BridgeSession.LOCAL_ADDRESS;
import static org.kaazing.gateway.transport.BridgeSession.NEXT_PROTOCOL_KEY;
import static org.kaazing.gateway.transport.BridgeSession.REMOTE_ADDRESS;

import java.net.URI;
import java.util.Collection;
import java.util.SortedSet;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.resource.address.uri.URIUtils;
import org.kaazing.gateway.transport.Bindings;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.IoSessionAdapterEx;
import org.kaazing.gateway.transport.LoggingFilter;
import org.kaazing.gateway.transport.NextProtocolBindings;
import org.kaazing.gateway.transport.NextProtocolFilter;
import org.kaazing.gateway.transport.dispatch.ProtocolDispatcher;
import org.kaazing.mina.core.service.IoAcceptorEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.session.IoSessionConfigEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;


class NioTcpBridgeHandler extends IoHandlerAdapter<IoSessionEx> {

    private final NextProtocolBindings bindings;
    private final IoAcceptorEx acceptor;
    private final ResourceAddressFactory resourceAddressFactory;
    private final BridgeServiceFactory bridgeServiceFactory;
    private final IoProcessorEx tcpBridgeProcessor;
    protected final Logger logger;
    private final String transportName;

    public NioTcpBridgeHandler(NextProtocolBindings bindings, IoAcceptorEx acceptor, ResourceAddressFactory resourceAddressFactory, BridgeServiceFactory bridgeServiceFactory, Logger logger, String transportName, IoProcessorEx tcpBridgeProcessor) {
        this.bindings = bindings;
        this.acceptor = acceptor;
        this.resourceAddressFactory = resourceAddressFactory;
        this.bridgeServiceFactory = bridgeServiceFactory;
        this.logger = logger;
        this.transportName = transportName;
        this.tcpBridgeProcessor = tcpBridgeProcessor;
    }

    @Override
    public void doSessionClosed(IoSessionEx session) throws Exception {
        AbstractNioAcceptor.LOG.debug("AbstractNioAcceptor.doSessionClosed for session " + session);
        NEXT_PROTOCOL_KEY.remove(session);
        LOCAL_ADDRESS.remove(session);
        REMOTE_ADDRESS.remove(session);
        IoSession tcpBridgeSession = getTcpBridgeSession(session);
        if (tcpBridgeSession != null) {
            tcpBridgeSession.getFilterChain().fireSessionClosed();
        }
    }

    @Override
    protected void doSessionCreated(IoSessionEx session) throws Exception {
        LoggingFilter.addIfNeeded(logger, session, transportName);

        ResourceAddress candidateAddress = getCandidateResourceAddress(session);
        NextProtocolBindings.NextProtocolBinding nextBinding = bindings.getBinding0(candidateAddress);
        if (nextBinding == null) {
            // next-protocol not currently bound for this address
            session.close(true);
            return;
        }
        SortedSet<String> nextProtocolNames = nextBinding.getNextProtocolNames();
        if (nextProtocolNames == null || nextProtocolNames.isEmpty()) {
            NEXT_PROTOCOL_KEY.set(session, null);
        } else if (nextProtocolNames.size() == 1) {
            NEXT_PROTOCOL_KEY.set(session, nextProtocolNames.first());
        } else {
            Collection<ProtocolDispatcher> dispatchers = bridgeServiceFactory.getTransportFactory().getProtocolDispatchers().values();

            // sessionCreated will be sent down pipeline again when next-protocol has been determined
            NextProtocolFilter nextProtocol = new NextProtocolFilter(dispatchers);
            IoFilterChain filterChain = session.getFilterChain();
            filterChain.addLast(AbstractNioAcceptor.NEXT_PROTOCOL_FILTER, nextProtocol);
        }

    }

    @Override
    protected void doSessionOpened(final IoSessionEx session) throws Exception {
        ResourceAddress candidateAddress = getCandidateResourceAddress(session);
        final Bindings.Binding binding = bindings.getBinding(candidateAddress);
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

        newTcpBridgeSession.setAttribute(AbstractNioAcceptor.PARENT_KEY, session);
        newTcpBridgeSession.setTransportMetadata(acceptor.getTransportMetadata());

        // Propagate changes to idle time to the parent session
        newTcpBridgeSession.getConfig().setChangeListener(new IoSessionConfigEx.ChangeListener() {
            @Override
            public void idleTimeInMillisChanged(IdleStatus status, long idleTime) {
                session.getConfig().setIdleTimeInMillis(status, idleTime);
            }
        });

        session.setAttribute(AbstractNioAcceptor.TCP_SESSION_KEY, newTcpBridgeSession);

        if (binding.initializer() != null) {
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
        return (IoSession) session.getAttribute(AbstractNioAcceptor.TCP_SESSION_KEY);
    }


    @Override
    protected void doExceptionCaught(IoSessionEx session, Throwable cause) throws Exception {
        //TODO: consider tcpBridgeSession.reset as an alternate impl.
        AbstractNioAcceptor.LOG.debug("NioSocketAcceptor exception caught", cause);

        if (!session.isClosing()) {
            IoSession tcpBridgeSession = getTcpBridgeSession(session);
            if (tcpBridgeSession != null) {
                tcpBridgeSession.getFilterChain().fireExceptionCaught(cause);
            }
        } else {
            AbstractNioAcceptor.LOG.debug("Unexpected exception while session is closing", cause);
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
}

