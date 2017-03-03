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
import static org.kaazing.gateway.transport.BridgeSession.LOCAL_ADDRESS;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.future.DefaultConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.transport.socket.nio.NioSocketSessionEx;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.IoSessionAdapterEx;
import org.kaazing.gateway.transport.NamedPipeAddress;
import org.kaazing.mina.core.filterchain.DefaultIoFilterChain;
import org.kaazing.mina.core.service.IoConnectorEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.session.IoSessionConfigEx;
import org.kaazing.mina.core.session.IoSessionEx;


class NioTcpBridgeSessionFactory implements Callable<IoSessionAdapterEx> {

    private final IoSession parent;
    private final IoSessionEx parentEx;
    private final IoSessionInitializer<IoFuture> bridgeSessionInitializer;
    private final ResourceAddressFactory addressFactory;
    private final ResourceAddress connectAddress;
    private final AtomicReference<IoConnectorEx> connectorReference;
    private final IoProcessorEx<IoSessionAdapterEx> processor;
    private final DefaultConnectFuture bridgeConnectFuture;

    public NioTcpBridgeSessionFactory(IoSession parent,
                                      IoSessionEx parentEx,
                                      IoSessionInitializer<IoFuture> bridgeSessionInitializer,
                                      ResourceAddressFactory addressFactory,
                                      ResourceAddress connectAddress,
                                      AtomicReference<IoConnectorEx> connectorReference,
                                      IoProcessorEx<IoSessionAdapterEx> processor,
                                      DefaultConnectFuture bridgeConnectFuture) {
        this.parent = parent;
        this.parentEx = parentEx;
        this.bridgeSessionInitializer = bridgeSessionInitializer;
        this.addressFactory = addressFactory;
        this.connectAddress = connectAddress;
        this.connectorReference = connectorReference;
        this.processor = processor;
        this.bridgeConnectFuture = bridgeConnectFuture;
    }

    @Override
    public IoSessionAdapterEx call() throws Exception {
        // support connecting tcp over pipes / socket addresses
        setLocalAddressFromSocketAddress(parent,
            parent instanceof NioSocketSessionEx ? "tcp" : "udp");
        ResourceAddress transportAddress = LOCAL_ADDRESS.get(parent);
        final ResourceAddress localAddress =
            addressFactory.newResourceAddress(connectAddress, transportAddress);


        final IoConnectorEx connector = connectorReference.get();
        IoSessionAdapterEx tcpBridgeSession = new IoSessionAdapterEx(parentEx.getIoThread(),
            parentEx.getIoExecutor(),
            connector,
            processor,
            connector.getSessionDataStructureFactory());

        tcpBridgeSession.setLocalAddress(localAddress);
        tcpBridgeSession.setRemoteAddress(connectAddress);
        tcpBridgeSession.setAttribute(AbstractNioConnector.PARENT_KEY, parent);
        tcpBridgeSession.setTransportMetadata(connector.getTransportMetadata());

        // Propagate changes to idle time to the parent session
        tcpBridgeSession.getConfig().setChangeListener(new IoSessionConfigEx.ChangeListener() {
            @Override
            public void idleTimeInMillisChanged(IdleStatus status, long idleTime) {
                parentEx.getConfig().setIdleTimeInMillis(status, idleTime);
            }
        });
        parent.setAttribute(AbstractNioConnector.TCP_SESSION_KEY, tcpBridgeSession);
        tcpBridgeSession.setAttribute(DefaultIoFilterChain.SESSION_CREATED_FUTURE, bridgeConnectFuture);
        bridgeSessionInitializer.initializeSession(tcpBridgeSession, bridgeConnectFuture);
        connector.getFilterChainBuilder().buildFilterChain(tcpBridgeSession.getFilterChain());
        connector.getListeners().fireSessionCreated(tcpBridgeSession);
        return tcpBridgeSession;
    }

    private void setLocalAddressFromSocketAddress(final IoSession session,
                                                  final String transportName) {
        SocketAddress socketAddress = session.getLocalAddress();
        if (socketAddress instanceof InetSocketAddress) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
            ResourceAddress resourceAddress = newResourceAddress(inetSocketAddress,
                transportName);
            LOCAL_ADDRESS.set(session, resourceAddress);
        } else if (socketAddress instanceof NamedPipeAddress) {
            NamedPipeAddress namedPipeAddress = (NamedPipeAddress) socketAddress;
            ResourceAddress resourceAddress = newResourceAddress(namedPipeAddress,
                "pipe");
            LOCAL_ADDRESS.set(session, resourceAddress);
        }
    }

    private ResourceAddress newResourceAddress(NamedPipeAddress namedPipeAddress,
                                               final String transportName) {
        String addressFormat = "%s://%s";
        String pipeName = namedPipeAddress.getPipeName();
        String transport = format(addressFormat, transportName, pipeName);
        return addressFactory.newResourceAddress(transport);
    }

    private ResourceAddress newResourceAddress(InetSocketAddress inetSocketAddress,
                                               final String transportName) {
        InetAddress inetAddress = inetSocketAddress.getAddress();
        String hostAddress = inetAddress.getHostAddress();
        String addressFormat = (inetAddress instanceof Inet6Address) ? "%s://[%s]:%s" : "%s://%s:%s";
        int port = inetSocketAddress.getPort();
        String transport = format(addressFormat, transportName, hostAddress, port);
        return addressFactory.newResourceAddress(transport);
    }
}
