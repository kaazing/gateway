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
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.DefaultConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.transport.socket.nio.NioSocketSessionEx;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeConnectHandler;
import org.kaazing.gateway.transport.BridgeConnector;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.IoSessionAdapterEx;
import org.kaazing.gateway.transport.LoggingFilter;
import org.kaazing.gateway.transport.NamedPipeAddress;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.filterchain.DefaultIoFilterChain;
import org.kaazing.mina.core.service.IoConnectorEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.session.IoSessionConfigEx.ChangeListener;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;

public abstract class AbstractNioConnector implements BridgeConnector {

    private AtomicReference<IoConnectorEx> connector = new AtomicReference<>();
    private final AtomicBoolean started;
    protected final Properties configuration;
    protected final Logger logger;
    private BridgeServiceFactory bridgeServiceFactory;
    private ResourceAddressFactory addressFactory;

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

        this.connector.set(connector);
        bridgeServiceFactory = initBridgeServiceFactory();
        addressFactory = initResourceAddressFactory();
    }

    protected abstract ResourceAddressFactory initResourceAddressFactory();
    protected abstract BridgeServiceFactory initBridgeServiceFactory();

    protected final Properties getProperties() {
        return configuration;
    }


    @Override
    public void dispose() {
        IoConnector connector = this.connector.getAndSet(null);
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

            // propagate connection failure, if necessary
            IoFutureListener<ConnectFuture> parentConnectListener = new IoFutureListener<ConnectFuture>() {
                @Override
                public void operationComplete(ConnectFuture future) {
                    // fail bridge connect future if parent connect fails
                    if (!future.isConnected()) {
                        bridgeConnectFuture.setException(future.getException());
                    }
                }
            };

            final BridgeConnector connector = bridgeServiceFactory.newBridgeConnector(transport);

            IoSessionInitializer<ConnectFuture> parentInitializer = createParentInitializer(address,
                                                                                            handler,
                                                                                            (IoSessionInitializer)initializer,
                                                                                            bridgeConnectFuture);

            connector.connect(transport, tcpBridgeHandler, parentInitializer).addListener(parentConnectListener);

            return bridgeConnectFuture;
        }

        final URI resource = address.getResource();
        final InetSocketAddress inetAddress = new InetSocketAddress(resource.getHost(), resource.getPort());

        if (logger.isTraceEnabled()) {
        	logger.trace(format("AbstractNioConnector.connectInternal(), resource: %s", resource));
        }

        // KG-1452: Avoid deadlock between dispose and connectInternal
        IoConnector connector = this.connector.get();
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


    //
    // Code to support "virtual" bridge tcp sessions when we have a transport defined.
    //

    private static final AttributeKey CREATE_SESSION_CALLABLE_KEY = new AttributeKey(AbstractNioConnector.class, "createSession");
    public static final String PARENT_KEY = "tcp.connector.parent.key";
    public static final String TCP_SESSION_KEY = "tcp.connector.bridgeSession.key";


    private final IoProcessorEx<IoSessionAdapterEx> processor = new IoProcessorEx<IoSessionAdapterEx>() {
        @Override
        public void add(IoSessionAdapterEx session) {
            // Do nothing
        }

        @Override
        public void flush(IoSessionAdapterEx session) {
            IoSession parent = (IoSession) session.getAttribute(PARENT_KEY);
            WriteRequest req = session.getWriteRequestQueue().poll(session);

            // Chek that the request is not null. If the session has been closed,
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
                            if ( future.isWritten() ) {
                                tcpBridgeWriteFuture.setWritten();
                            } else {
                                tcpBridgeWriteFuture.setException(future.getException());
                            }
                        }
                    });
                }
            }
        }

        @Override
        public void remove(IoSessionAdapterEx session) {
            logger.debug("AbstractNioConnector.fake processor remove for session "+session);

            IoSession parent = (IoSession) session.getAttribute(PARENT_KEY);
            parent.close(false);
            doFireSessionDestroyed(session);
        }

        protected void doFireSessionDestroyed(IoSessionAdapterEx session) {
            final IoConnectorEx connector = AbstractNioConnector.this.connector.get();
            if (connector != null) {
                connector.getListeners().fireSessionDestroyed(session);
            }
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

    private <T extends ConnectFuture> IoSessionInitializer<ConnectFuture> createParentInitializer(final ResourceAddress connectAddress,
                                                                                                  final IoHandler handler,
                                                                                                  final IoSessionInitializer<IoFuture> initializer,
                                                                                                  final DefaultConnectFuture bridgeConnectFuture) {

        // initialize parent session before connection attempt
        return new IoSessionInitializer<ConnectFuture>() {


            @Override
            public void initializeSession(final IoSession parent, ConnectFuture future) {
                final IoSessionEx parentEx = (IoSessionEx) parent;

                // initializer for bridge session to specify bridge handler,
                // and call user-defined bridge session initializer if present
                final IoSessionInitializer<IoFuture> bridgeSessionInitializer = new IoSessionInitializer<IoFuture> () {
                    @Override
                    public void initializeSession(IoSession bridgeSession, IoFuture future) {
                        ((IoSessionAdapterEx)bridgeSession).setHandler(handler);

                        if (initializer != null) {
                            initializer.initializeSession(bridgeSession, future);
                        }
                    }
                };

                // factory to create a new tcp bridge session
                Callable<IoSessionAdapterEx> createSession = new Callable<IoSessionAdapterEx>() {
                    @Override
                    public IoSessionAdapterEx call() throws Exception {

                        // support connecting tcp over pipes / socket addresses
                        setLocalAddressFromSocketAddress(parent,
                                                         parent instanceof NioSocketSessionEx ? "tcp" : "udp");

                        ResourceAddress transportAddress = LOCAL_ADDRESS.get(parent);
                        final ResourceAddress localAddress =
                                addressFactory.newResourceAddress(connectAddress, transportAddress);


                        final IoConnectorEx connector = AbstractNioConnector.this.connector.get();
                        IoSessionAdapterEx tcpBridgeSession = new IoSessionAdapterEx(parentEx.getIoThread(),
                                                                                     parentEx.getIoExecutor(),
                                                                                     connector,
                                                                                 processor,
                                                                                 connector.getSessionDataStructureFactory());

                        tcpBridgeSession.setLocalAddress(localAddress);
                        tcpBridgeSession.setRemoteAddress(connectAddress);
                        tcpBridgeSession.setAttribute(PARENT_KEY, parent);
                        tcpBridgeSession.setTransportMetadata(connector.getTransportMetadata());

                        // Propagate changes to idle time to the parent session
                        tcpBridgeSession.getConfig().setChangeListener(new ChangeListener() {
                            @Override
                            public void idleTimeInMillisChanged(IdleStatus status, long idleTime) {
                                parentEx.getConfig().setIdleTimeInMillis(status, idleTime);
                            }
                        });

                        parent.setAttribute(TCP_SESSION_KEY, tcpBridgeSession);

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
                        }
                        else if (socketAddress instanceof NamedPipeAddress) {
                            NamedPipeAddress namedPipeAddress = (NamedPipeAddress) socketAddress;
                            ResourceAddress resourceAddress = newResourceAddress(namedPipeAddress,
                                                                                 "pipe");
                            LOCAL_ADDRESS.set(session, resourceAddress);
                        }
                    }


                    public  ResourceAddress newResourceAddress(NamedPipeAddress namedPipeAddress,
                                                               final String transportName) {
                        String addressFormat = "%s://%s";
                        String pipeName = namedPipeAddress.getPipeName();
                        String transport = format(addressFormat, transportName, pipeName);
                        return addressFactory.newResourceAddress(transport);
                    }

                    public  ResourceAddress newResourceAddress(InetSocketAddress inetSocketAddress,
                                                               final String transportName) {
                        InetAddress inetAddress = inetSocketAddress.getAddress();
                        String hostAddress = inetAddress.getHostAddress();
                        String addressFormat = (inetAddress instanceof Inet6Address) ? "%s://[%s]:%s" : "%s://%s:%s";
                        int port = inetSocketAddress.getPort();
                        String transport = format(addressFormat, transportName, hostAddress, port);
                        return addressFactory.newResourceAddress(transport);
                    }

                };

                parent.setAttribute(CREATE_SESSION_CALLABLE_KEY, createSession);
            }

        };
    }

    private final IoHandlerAdapter<IoSessionEx> tcpBridgeHandler = new IoHandlerAdapter<IoSessionEx>() {
        @Override
        public void doSessionCreated(IoSessionEx session) throws Exception {
            LoggingFilter.addIfNeeded(logger, session, getTransportName());

            super.doSessionCreated(session);
        }

        @Override
        public void doSessionClosed(IoSessionEx session) throws Exception {
            if (logger.isDebugEnabled()) {
                logger.debug("AbstractNioConnector.doSessionClosed for session "+session);
            }
            IoSession tcpBridgeSession = getTcpBridgeSession(session);
            tcpBridgeSession.getFilterChain().fireSessionClosed();
        }

        @Override
        protected void doSessionOpened(final IoSessionEx session) throws Exception {
            Callable<IoSessionAdapterEx> sessionFactory = (Callable<IoSessionAdapterEx>) session
                    .removeAttribute(CREATE_SESSION_CALLABLE_KEY);
            IoSessionAdapterEx tcpBridgeSession = sessionFactory.call();

            // already added in session creator, in case of synchronous pipe write from sessionOpened
            assert (session.getAttribute(TCP_SESSION_KEY) == tcpBridgeSession);

        }

        private IoSession getTcpBridgeSession(IoSession session) {
            return (IoSession) session.getAttribute(TCP_SESSION_KEY);
        }

        @Override
        protected void doExceptionCaught(IoSessionEx session, Throwable cause) throws Exception {
            // TODO: reset implementation should inform this one.
            IoSession tcpBridgeSession = getTcpBridgeSession(session);
            if (tcpBridgeSession != null) {
                tcpBridgeSession.getFilterChain().fireExceptionCaught(cause);
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
