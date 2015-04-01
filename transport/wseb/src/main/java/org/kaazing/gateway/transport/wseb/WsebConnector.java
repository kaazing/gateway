/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.transport.wseb;

import static java.lang.String.format;
import static org.kaazing.gateway.resource.address.URLUtils.appendURI;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.INACTIVITY_TIMEOUT;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_X_ACCEPT_COMMANDS;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.DefaultConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.gateway.resource.address.Protocol;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.transport.AbstractBridgeConnector;
import org.kaazing.gateway.transport.BridgeConnector;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.transport.DefaultIoSessionConfigEx;
import org.kaazing.gateway.transport.DefaultTransportMetadata;
import org.kaazing.gateway.transport.ExceptionLoggingFilter;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.ObjectLoggingFilter;
import org.kaazing.gateway.transport.TypedAttributeKey;
import org.kaazing.gateway.transport.http.HttpHeaders;
import org.kaazing.gateway.transport.http.HttpProtocol;
import org.kaazing.gateway.transport.http.HttpSession;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.ws.Command;
import org.kaazing.gateway.transport.ws.WsCommandMessage;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer;
import org.kaazing.gateway.transport.wseb.filter.WsebBufferAllocator;
import org.kaazing.gateway.transport.wseb.filter.WsebFrameCodecFilter;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.netty.IoSessionIdleTracker;
import org.kaazing.mina.netty.util.threadlocal.VicariousThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WsebConnector extends AbstractBridgeConnector<WsebSession> {

    private static final String CREATE_SUFFIX = WsebAcceptor.EMULATED_SUFFIX + "/cb";

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private static final TypedAttributeKey<Callable<WsebSession>> WSE_SESSION_FACTORY_KEY = new TypedAttributeKey<>(WsebConnector.class, "wseSessionFactory");
    private static final TypedAttributeKey<ConnectFuture> WSE_CONNECT_FUTURE_KEY = new TypedAttributeKey<>(WsebConnector.class, "wseConnectFuture");
    private static final TypedAttributeKey<WsebSession> WSE_SESSION_KEY = new TypedAttributeKey<>(WsebConnector.class, "wseSession");

    private static final String CODEC_FILTER = WsebProtocol.NAME + "#codec";

    private static final String FAULT_LOGGING_FILTER = WsebProtocol.NAME + "#fault";
    private static final String TRACE_LOGGING_FILTER = WsebProtocol.NAME + "#logging";
    private static final String LOGGER_NAME = String.format("transport.%s.connect", WsebProtocol.NAME);

    private final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    private final Map<ResourceAddress, WsebSession> sessionMap;

    private BridgeServiceFactory bridgeServiceFactory;
    private ResourceAddressFactory resourceAddressFactory;
    
    private final List<IoSessionIdleTracker> sessionInactivityTrackers
        = Collections.synchronizedList(new ArrayList<IoSessionIdleTracker>());
    private final ThreadLocal<IoSessionIdleTracker> currentSessionInactivityTracker
        = new VicariousThreadLocal<IoSessionIdleTracker>() {
        @Override
        protected IoSessionIdleTracker initialValue() {
            IoSessionIdleTracker result = new WsebInactivityTracker(logger);
            sessionInactivityTrackers.add(result);
            return result;
        }
    };

    public WsebConnector() {
        super(new DefaultIoSessionConfigEx());
        sessionMap = new ConcurrentHashMap<>();
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return new DefaultTransportMetadata(WsebProtocol.NAME);
    }

    @Resource(name = "bridgeServiceFactory")
    public void setBridgeServiceFactory(BridgeServiceFactory bridgeServiceFactory) {
        this.bridgeServiceFactory = bridgeServiceFactory;
    }


    @Resource(name = "resourceAddressFactory")
    public void setResourceAddressFactory(ResourceAddressFactory factory) {
        this.resourceAddressFactory = factory;
    }


    @Override
    protected IoProcessorEx<WsebSession> initProcessor() {
        return new WsebConnectProcessor(bridgeServiceFactory);
    }

    @Override
    public void addBridgeFilters(IoFilterChain filterChain) {
        // setup logging filters for bridge session
        if (logger.isTraceEnabled()) {
            filterChain.addFirst(TRACE_LOGGING_FILTER, new ObjectLoggingFilter(logger, WsebProtocol.NAME + "#%s"));
        } else {
            filterChain.addFirst(FAULT_LOGGING_FILTER, new ExceptionLoggingFilter(logger, WsebProtocol.NAME + "#%s"));
        }
    }

    @Override
    protected boolean canConnect(String transportName) {
        return transportName.equals("wse") || transportName.equals("ws");
    }

    @Override
    protected <T extends ConnectFuture> ConnectFuture connectInternal(ResourceAddress connectAddress, IoHandler handler,
                                                                      final IoSessionInitializer<T> initializer) {
        final DefaultConnectFuture wseConnectFuture = new DefaultConnectFuture();

        // propagate connection failure, if necessary
        IoFutureListener<ConnectFuture> parentConnectListener = new IoFutureListener<ConnectFuture>() {
            @Override
            public void operationComplete(ConnectFuture future) {
                // fail bridge connect future if parent connect fails
                if (!future.isConnected()) {
                    wseConnectFuture.setException(future.getException());
                }
            }
        };

        IoSessionInitializer<ConnectFuture> parentInitializer = createParentInitializer(connectAddress,
                                                                                        handler,
                                                                                        initializer,
                                                                                        wseConnectFuture);

        ResourceAddress httpxeAddress = connectAddress.getTransport();

        URI createURI = appendURI(httpxeAddress.getExternalURI(), CREATE_SUFFIX);

        // default options but clear the transports so they get rebuilt by default
        ResourceOptions createOptions = ResourceOptions.FACTORY.newResourceOptions(httpxeAddress);
        createOptions.setOption(ResourceAddress.TRANSPORT, null);
        createOptions.setOption(ResourceAddress.TRANSPORT_URI, null);

        ResourceAddress createAddress = resourceAddressFactory.newResourceAddress(createURI, createOptions);
        BridgeConnector connector = bridgeServiceFactory.newBridgeConnector(createAddress);

        // TODO: proxy detection, append ?.ki=p on timeout
        connector.connect(createAddress, selectConnectHandler(createAddress), parentInitializer).addListener(parentConnectListener);

        return wseConnectFuture;
    }

    @Override
    protected IoFuture dispose0() throws Exception {
        for (IoSessionIdleTracker tracker : sessionInactivityTrackers) {
            tracker.dispose();
        }
        return super.dispose0();
    }

    private IoHandler selectConnectHandler(ResourceAddress address) {
        Protocol protocol = bridgeServiceFactory.getTransportFactory().getProtocol(address.getResource());
        if ( protocol instanceof HttpProtocol ) {
            return createHandler;
        }
        throw new RuntimeException(getClass()+
                ": Cannot select a connect handler for address "+address);
    }


    private <T extends ConnectFuture> IoSessionInitializer<ConnectFuture> createParentInitializer(
            final ResourceAddress connectAddressNext, final IoHandler handler, final IoSessionInitializer<T> initializer,
            final DefaultConnectFuture wseConnectFuture) {

        final ResourceAddress connectAddress = connectAddressNext.getTransport();
        Protocol protocol = bridgeServiceFactory.getTransportFactory().getProtocol(connectAddress.getResource());
        if (!(protocol instanceof HttpProtocol)) {
            final String message = format("Cannot create WSEB parent session initializer for address %s", connectAddressNext);
            if (logger.isInfoEnabled()) {
                logger.info(message);
            }
            throw new RuntimeException(message);
        }
        // initialize parent session before connection attempt
        return new IoSessionInitializer<ConnectFuture>() {
            @Override
            public void initializeSession(final IoSession parent, ConnectFuture future) {
                // initializer for bridge session to specify bridge handler,
                // and call user-defined bridge session initializer if present
                final IoSessionInitializer<T> wseSessionInitializer = new IoSessionInitializer<T>() {
                    @Override
                    public void initializeSession(IoSession session, T future) {
                        WsebSession wseSession = (WsebSession) session;
                        wseSession.setHandler(handler);

                        if (initializer != null) {
                            initializer.initializeSession(session, future);
                        }
                    }
                };

                final long sequenceNo = 0;

                final HttpSession httpSession = (HttpSession) parent;
                httpSession.setWriteHeader(HEADER_X_ACCEPT_COMMANDS, "ping");
                httpSession.setWriteHeader(HttpHeaders.HEADER_X_SEQUENCE_NO, Long.toString(sequenceNo));
                final IoBufferAllocatorEx<WsBuffer> allocator = new WsebBufferAllocator(httpSession.getBufferAllocator());

                // factory to create a new bridge session
                Callable<WsebSession> createSession = new Callable<WsebSession>() {
                    @Override
                    public WsebSession call() throws Exception {

                    Callable<WsebSession> wseSessionFactory = new Callable<WsebSession>() {
                        @Override
                        public WsebSession call() throws Exception {
                            WsebSession wsebSession = new WsebSession(httpSession.getIoLayer(),
                                                                      httpSession.getIoThread(), 
                                                                      httpSession.getIoExecutor(), 
                                                                      WsebConnector.this,
                                                                      getProcessor(), 
                                                                      connectAddressNext, 
                                                                      connectAddressNext, 
                                                                      allocator, 
                                                                      null, 
                                                                      null, 
                                                                      0,
                                                                      connectAddressNext.getOption(INACTIVITY_TIMEOUT),
                                                                      false,            /* no sequence validation */
                                                                      sequenceNo);      /* starting sequence no */

                                // ability to write will be reactivated when create response returns with write address
                                wsebSession.suspendWrite();

                                return wsebSession;
                            }
                        };

                        return newSession(wseSessionInitializer, wseConnectFuture, wseSessionFactory);
                    }
                };

                WSE_SESSION_FACTORY_KEY.set(httpSession, createSession);
                WSE_CONNECT_FUTURE_KEY.set(httpSession, wseConnectFuture);
            }
        };
    }


    private static final TypedAttributeKey<IoBufferEx> CREATE_RESPONSE_KEY = new TypedAttributeKey<>(WsebConnector.class, "createResponse");

    private IoHandler createHandler = new IoHandlerAdapter<HttpSession>() {

        @Override
        protected void doSessionOpened(final HttpSession createSession) throws Exception {
            Callable<WsebSession> sessionFactory = WSE_SESSION_FACTORY_KEY.remove(createSession);
            final WsebSession wsebSession = sessionFactory.call();

            // clean up session map
            wsebSession.getCloseFuture().addListener(new IoFutureListener<CloseFuture>() {
                @Override
                public void operationComplete(CloseFuture future) {
                    ResourceAddress readAddress = wsebSession.getReadAddress();
                    if (readAddress != null) {
                        sessionMap.remove(readAddress);
                    }

                    ResourceAddress writeAddress = wsebSession.getWriteAddress();
                    if (writeAddress != null) {
                        sessionMap.remove(writeAddress);
                    }
                    
                    if (wsebSession.getInactivityTimeout() > 0) {
                        currentSessionInactivityTracker.get().removeSession(wsebSession);
                    }

                    // handle exception during create response
                    createSession.close(false);
                }
            });

            // store created session to process during session close, or exception
            WSE_SESSION_KEY.set(createSession, wsebSession);
            WSE_CONNECT_FUTURE_KEY.remove(createSession);
        }

        @Override
        protected void doMessageReceived(HttpSession createSession, Object message) throws Exception {
            // Handle fragmentation of response body
            IoBufferEx in = (IoBufferEx) message;

            IoBufferEx buf = CREATE_RESPONSE_KEY.get(createSession);
            if (buf == null) {
                IoBufferAllocatorEx<?> allocator = createSession.getBufferAllocator();
                buf = allocator.wrap(allocator.allocate(in.remaining())).setAutoExpander(allocator);
                CREATE_RESPONSE_KEY.set(createSession, buf);
            }

            buf.put(in);
        }

        @Override
        protected void doSessionClosed(HttpSession createSession) throws Exception {
            final WsebSession wsebSession = WSE_SESSION_KEY.get(createSession);
            assert (wsebSession != null);

            IoBufferEx buf = CREATE_RESPONSE_KEY.remove(createSession);
            if (buf == null || createSession.getStatus() != HttpStatus.SUCCESS_CREATED) {
                throw new IllegalStateException("Create handshake failed: invalid response");
            }

            buf.flip();

            String responseText = buf.getString(UTF_8.newDecoder());
            String[] locations = responseText.split("\n");

            if (locations.length < 2) {
                throw new IllegalStateException("Create handshake failed: invalid response");
            }

            URI writeURI = URI.create(locations[0]);
            URI readURI = URI.create(locations[1]);

            ResourceAddress writeAddress = resourceAddressFactory.newResourceAddress(writeURI);
            ResourceAddress readAddress = resourceAddressFactory.newResourceAddress(readURI);

            if (!wsebSession.isClosing()) {
                wsebSession.setWriteAddress(writeAddress);
                wsebSession.setReadAddress(readAddress);

                sessionMap.put(writeAddress, wsebSession);
                sessionMap.put(readAddress, wsebSession);

                // attach downstream for read
                final BridgeConnector bridgeConnector = bridgeServiceFactory.newBridgeConnector(readAddress);
                bridgeConnector.connect(readAddress, selectReadHandler(readAddress), new IoSessionInitializer<ConnectFuture>() {
                    @Override
                    public void initializeSession(IoSession ioSession, ConnectFuture connectFuture) {
                        HttpSession httpSession = (HttpSession) ioSession;
                        httpSession.setWriteHeader(HttpHeaders.HEADER_X_SEQUENCE_NO, Long.toString(wsebSession.nextReaderSequenceNo()));
                    }
                });

                // activate upstream for write
                //TODO: Replace usage of suspendWrite/resumeWrite with a WSEB-specific "send queue" upon which
                //TODO: locking of writes can be achieved.
                wsebSession.resumeWrite();
                // We are always aligned now. if (session.isIoAligned()) {
                wsebSession.getProcessor().flush(wsebSession);
            }
        }



        @Override
        protected void doExceptionCaught(HttpSession createSession, Throwable cause) throws Exception {
            WsebSession wsebSession = WSE_SESSION_KEY.get(createSession);
            if (wsebSession != null && !wsebSession.isClosing()) {
                wsebSession.reset(cause);
            }
            else {
                if (logger.isDebugEnabled()) {
                    String message = format("Error on WebSocket WSE connection attempt: %s", cause);
                    if (logger.isTraceEnabled()) {
                        // note: still debug level, but with extra detail about the exception
                        logger.debug(message, cause);
                    }
                    else {
                        logger.debug(message);
                    }
                }
                createSession.close(true);
            }
        }

    };

     private IoHandler selectReadHandler(ResourceAddress readAddress) {
            Protocol protocol = bridgeServiceFactory.getTransportFactory().getProtocol(readAddress.getResource());
            if ( protocol instanceof HttpProtocol ) {
                return readHandler;
            }
            throw new RuntimeException("Cannot select read handler for address "+readAddress);
        }

    private IoHandler readHandler = new IoHandlerAdapter<HttpSession>() {
        @Override
        protected void doSessionCreated(HttpSession readSession) throws Exception {
            addBridgeFilters(readSession.getFilterChain());
            super.doSessionCreated(readSession);
        }

        @Override
        protected void doSessionOpened(HttpSession readSession) throws Exception {
            IoFilterChain filterChain = readSession.getFilterChain();
            filterChain.addLast(CODEC_FILTER, new WsebFrameCodecFilter(0));

            ResourceAddress readAddress = readSession.getRemoteAddress();
            final WsebSession wsebSession = sessionMap.get(readAddress);
            assert (wsebSession != null);
            wsebSession.attachReader(readSession);
            
            if (wsebSession.getInactivityTimeout() > 0) {
                // Activate inactivity timeout only once read session is established
                currentSessionInactivityTracker.get().addSession(wsebSession);
            }

            readSession.getCloseFuture().addListener(new IoFutureListener<CloseFuture>() {
                @Override
                public void operationComplete(CloseFuture future) {
                    wsebSession.close(true);
                }
            });
        }

        @Override
        protected void doMessageReceived(HttpSession readSession, Object message) throws Exception {
            ResourceAddress readAddress = BridgeSession.REMOTE_ADDRESS.get(readSession);
            WsebSession wsebSession = sessionMap.get(readAddress);

            // handle parallel closure of WSE session during streaming read
            if (wsebSession == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Could not find WsebSession for read address:\n"+readAddress));
                }
                return;
            }

            WsMessage wsebMessage = (WsMessage) message;
            IoBufferEx messageBytes = wsebMessage.getBytes();
            
            if (wsebSession.getInactivityTimeout() > 0) {
                WsebInactivityTracker.messageReceived(wsebSession, wsebMessage);
            }

            switch (wsebMessage.getKind()) {
            case BINARY:
            case TEXT:
                IoFilterChain filterChain = wsebSession.getFilterChain();
                filterChain.fireMessageReceived(messageBytes);
                break;
            case COMMAND:
                for (Command command : ((WsCommandMessage)wsebMessage).getCommands()) {
                    if (command == Command.reconnect()) {
                        // received a RECONNECT command
                        wsebSession.detachReader(readSession);
                        // re-attach downstream for read
                        final BridgeConnector bridgeConnector = bridgeServiceFactory.newBridgeConnector(readAddress);
                        bridgeConnector.connect(readAddress, selectReadHandler(readAddress), null);
                        break;
                    }
                    else if (command == Command.close()) {
                        // Following should take care of sending CLOSE response and closing reader (downstream)
                        // Close case was not handled before 3.5.9
                        wsebSession.close(false);
                        break;
                    }
                    // no-op (0x00) - continue reading commands
                }
                break;
            case PING:
                wsebSession.issuePongRequest();
                break;
            case PONG:
                break;
            default:
                throw new IllegalArgumentException("Unrecognized message kind: " + wsebMessage.getKind());
            }
        }

        @Override
        protected void doExceptionCaught(HttpSession readSession, Throwable cause) throws Exception {
            ResourceAddress readAddress = readSession.getLocalAddress();
            WsebSession wsebSession = sessionMap.get(readAddress);

            if (wsebSession != null && !wsebSession.isClosing()) {
                wsebSession.reset(cause);
            }
            else {
                if (logger.isDebugEnabled()) {
                    String message = format("Error on WebSocket WSE connection: %s", cause);
                    if (logger.isTraceEnabled()) {
                        // note: still debug level, but with extra detail about the exception
                        logger.debug(message, cause);
                    }
                    else {
                        logger.debug(message);
                    }
                }
                readSession.close(true);
            }
        }
    };
}
