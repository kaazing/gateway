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
import static org.kaazing.gateway.transport.BridgeSession.REMOTE_ADDRESS;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.DefaultConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.udp.UdpResourceAddress;
import org.kaazing.gateway.transport.BridgeConnectHandler;
import org.kaazing.gateway.transport.BridgeConnector;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.IoSessionAdapterEx;
import org.kaazing.gateway.transport.LoggingFilter;
import org.kaazing.gateway.transport.NamedPipeAddress;
import org.kaazing.mina.core.service.IoConnectorEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;

public abstract class AbstractNioConnector implements BridgeConnector {

    private AtomicReference<IoConnectorEx> connectorReference = new AtomicReference<>();
    private final AtomicBoolean started;
    protected final Properties configuration;
    protected final Logger logger;
    private BridgeServiceFactory bridgeServiceFactory;
    private ResourceAddressFactory addressFactory;
    private IoHandlerAdapter<IoSessionEx> tcpBridgeHandler;
    private IoProcessorEx<IoSessionAdapterEx> processor;

    //
    // Code to support "virtual" bridge tcp sessions when we have a transport defined.
    //

    public static final AttributeKey CREATE_SESSION_CALLABLE_KEY = new AttributeKey(AbstractNioConnector.class, "createSession");
    public static final String PARENT_KEY = "tcp.connector.parent.key";
    public static final String TCP_SESSION_KEY = "tcp.connector.bridgeSession.key";

    protected AbstractNioConnector(Properties configuration, Logger logger) {
        if (configuration == null) {
            throw new NullPointerException("configuration");
        }
    	if (logger == null) {
    		throw new NullPointerException("logger");
    	}
        this.configuration = configuration;
    	this.logger = logger;
        this.started = new AtomicBoolean(false);
    }

    protected void init() {
        if (logger.isTraceEnabled()) {
            logger.trace("AbstractNioConnector.init()");
        }
        IoConnectorEx connector = initConnector();
        connector.setHandler(new BridgeConnectHandler() {
            @Override
            public void sessionCreated(IoSession session) throws Exception {
                LoggingFilter.addIfNeeded(logger, session, getTransportName());

                super.sessionCreated(session);
            }
        });

        this.connectorReference.set(connector);
        bridgeServiceFactory = initBridgeServiceFactory();
        addressFactory = initResourceAddressFactory();
        tcpBridgeHandler =  new NioConnectorTcpBridgeHandler(logger, getTransportName());
        processor = new NioConnectorTcpBridgeProcessor(this, logger);
    }

    protected abstract ResourceAddressFactory initResourceAddressFactory();
    protected abstract BridgeServiceFactory initBridgeServiceFactory();

    protected final Properties getProperties() {
        return configuration;
    }

    public AtomicReference<IoConnectorEx> getConnectorReference() {
        return connectorReference;
    }

    @Override
    public void dispose() {
        IoConnector connector = this.connectorReference.getAndSet(null);
    	if (connector != null) {
    		connector.dispose();
    	}
    }

    @Override
    public ConnectFuture connect(ResourceAddress address, IoHandler handler, IoSessionInitializer<? extends ConnectFuture> initializer) {
        if (!started.get()) {
            synchronized (started) {
                if (!started.get()) {
                    init();
                    started.set(true);
                }
            }
        }
        return connectInternal(address, handler, initializer);
    }

    @Override
    public void connectInit(ResourceAddress address) {
        // no-op by default
    }


    @Override
    public void connectDestroy(ResourceAddress address) {
        // no-op by default
    }

    // THIS IS A BUG IN JAVA, should not need two methods just to capture the type
    protected <T extends ConnectFuture> ConnectFuture connectInternal(final ResourceAddress address, final IoHandler handler, final IoSessionInitializer<T> initializer) {
        ConnectFuture future;

        // TODO: throw exception if address contains more then one resource
        ResourceAddress transport = address.getTransport();
        if (transport != null) {
            final DefaultConnectFuture bridgeConnectFuture = new DefaultConnectFuture();
            IoSessionInitializer<ConnectFuture> parentInitializer = createParentInitializer(
                address,
                handler,
                (IoSessionInitializer) initializer,
                bridgeConnectFuture,
                addressFactory,
                processor,
                connectorReference
            );
            // propagate connection failure, if necessary
            bridgeServiceFactory.newBridgeConnector(transport)
                .connect(transport, tcpBridgeHandler, parentInitializer)
                .addListener(
                    new IoFutureListener<ConnectFuture>() {
                        @Override
                        public void operationComplete(ConnectFuture future) {
                            // fail bridge connect future if parent connect fails
                            if (!future.isConnected()) {
                                bridgeConnectFuture.setException(future.getException());
                            }
                        }
                    }
                );
            return bridgeConnectFuture;
        }

        final URI resource = address.getResource();
        final InetSocketAddress inetAddress = new InetSocketAddress(resource.getHost(), resource.getPort());

        if (logger.isTraceEnabled()) {
        	logger.trace(format("AbstractNioConnector.connectInternal(), resource: %s", resource));
        }

        // KG-1452: Avoid deadlock between dispose and connectInternal
        IoConnector connector = this.connectorReference.get();
        if (connector == null) {
            return DefaultConnectFuture.newFailedFuture(new IllegalStateException("Connector is being shut down"));
        }

        final String nextProtocol = address.getOption(ResourceAddress.NEXT_PROTOCOL);
        future = connector.connect(inetAddress, new IoSessionInitializer<T>() {
            @Override
            public void initializeSession(IoSession session, T future) {

                if (logger.isTraceEnabled()) {
                    logger.trace(format("AbstractNioConnector.connectInternal()$initializeSession(), session: %s, resource: %s", session, resource));
                }

                if (address instanceof UdpResourceAddress) {
                    int align = address.getOption(UdpResourceAddress.ALIGN);
                    if (align > 0) {
                        session.getFilterChain().addFirst("align", new UdpAlignFilter(logger, align, session));
                    }
                }

                // connectors don't need lookup so set this directly on the session
                session.setAttribute(BridgeConnectHandler.DELEGATE_KEY, handler);

                // Currrently, the underlying TCP session has the remote
                // address being an InetSocketAddress.  Our top-level
                // ResourceAddress has more information than just that
                // remote InetSocketAddress -- so we set that as the
                // remote address in the created session.
                REMOTE_ADDRESS.set(session, address);
                LOCAL_ADDRESS.set(session, createResourceAddress(inetAddress, nextProtocol));

                if (initializer != null) {
                    initializer.initializeSession(session, future);
                }

            }
        });

        future.addListener(new IoFutureListener<ConnectFuture>() {
            @Override
            public void operationComplete(ConnectFuture future) {
                if (future.isConnected()) {
                    IoSession session = future.getSession();
                    SocketAddress localAddress = session.getLocalAddress();
                    if (localAddress instanceof InetSocketAddress) {
                        InetSocketAddress inetSocketAddress = (InetSocketAddress) localAddress;
                        ResourceAddress resourceAddress = createResourceAddress(inetSocketAddress, nextProtocol);
                        LOCAL_ADDRESS.set(session, resourceAddress);
                    }
                    else if (localAddress instanceof NamedPipeAddress) {
                        NamedPipeAddress namedPipeAddress = (NamedPipeAddress) localAddress;
                        ResourceAddress resourceAddress = createResourceAddress(namedPipeAddress, nextProtocol);
                        LOCAL_ADDRESS.set(session, resourceAddress);
                    }
                }
            }
        });
        return future;
    }

    private ResourceAddress createResourceAddress(NamedPipeAddress namedPipeAddress, String nextProtocol) {
        String transportName = getTransportName();
        String addressFormat = "%s://%s";
        String pipeName = namedPipeAddress.getPipeName();
        String transport = format(addressFormat, transportName, pipeName);
        return addressFactory.newResourceAddress(transport, nextProtocol);
    }

    private ResourceAddress createResourceAddress(InetSocketAddress inetSocketAddress, String nextProtocol) {
        String transportName = getTransportName();
        InetAddress inetAddress = inetSocketAddress.getAddress();
        String hostAddress = inetAddress.getHostAddress();
        String addressFormat = (inetAddress instanceof Inet6Address) ? "%s://[%s]:%s" : "%s://%s:%s";
        int port = inetSocketAddress.getPort();
        String transport = format(addressFormat, transportName, hostAddress, port);
        return addressFactory.newResourceAddress(transport, nextProtocol);
    }

    protected abstract IoConnectorEx initConnector();

    protected abstract String getTransportName();

    final Properties getConfiguration() {
        return configuration;
    }

    private <T extends ConnectFuture> IoSessionInitializer<ConnectFuture> createParentInitializer(final ResourceAddress connectAddress,
                                                                                                  final IoHandler handler,
                                                                                                  final IoSessionInitializer<IoFuture> initializer,
                                                                                                  final DefaultConnectFuture bridgeConnectFuture,
                                                                                                  final ResourceAddressFactory addressFactory,
                                                                                                  final IoProcessorEx<IoSessionAdapterEx> processor,
                                                                                                  final AtomicReference<IoConnectorEx> connectorReference) {
        // initialize parent session before connection attempt
        return new NioConnectorParentSessionInitializer(handler, initializer, connectAddress, bridgeConnectFuture, addressFactory, processor, connectorReference);
    }

}
