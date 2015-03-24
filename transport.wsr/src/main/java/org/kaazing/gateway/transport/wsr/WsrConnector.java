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

package org.kaazing.gateway.transport.wsr;

import static java.lang.String.format;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.Callable;

import javax.annotation.Resource;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.DefaultConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.filter.logging.LoggingFilter;
import org.kaazing.gateway.resource.address.Protocol;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.AbstractBridgeConnector;
import org.kaazing.gateway.transport.BridgeConnectHandler;
import org.kaazing.gateway.transport.BridgeConnector;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.DefaultIoSessionConfigEx;
import org.kaazing.gateway.transport.DefaultTransportMetadata;
import org.kaazing.gateway.transport.ExceptionLoggingFilter;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.ObjectLoggingFilter;
import org.kaazing.gateway.transport.TypedAttributeKey;
import org.kaazing.gateway.transport.http.HttpProtocol;
import org.kaazing.gateway.transport.http.HttpSession;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.wsr.bridge.filter.RtmpChunkCodecFilter;
import org.kaazing.gateway.transport.wsr.bridge.filter.RtmpEncoder;
import org.kaazing.gateway.transport.wsr.bridge.filter.RtmpPublishCommandMessage;
import org.kaazing.gateway.transport.wsr.bridge.filter.WsrBufferAllocator;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WsrConnector extends AbstractBridgeConnector<WsrSession> {

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final TypedAttributeKey<IoBufferEx> CREATE_RESPONSE_KEY = new TypedAttributeKey<>(
            WsrConnector.class, "createResponse");
    private static final String CREATE_SUFFIX = "/;e/cr";
    private static final TypedAttributeKey<WsrSession> WSR_SESSION_KEY = new TypedAttributeKey<>(WsrConnector.class, "rtmp.session");
    private static final TypedAttributeKey<ConnectRequest<?>> CONNECT_REQUEST_KEY = new TypedAttributeKey<>(WsrConnector.class, "createSession");


    private final RtmpChunkCodecFilter codec;

    private static final String FAULT_LOGGING_FILTER = "wsn#fault";
    private static final String TRACE_LOGGING_FILTER = "wsn#logging";
    private static final String LOGGER_NAME = String.format("transport.%s.connect", WsrProtocol.NAME);
    private final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    private BridgeServiceFactory bridgeServiceFactory;
    private ResourceAddressFactory resourceAddressFactory;


    public WsrConnector() {
        super(new DefaultIoSessionConfigEx());
        codec = new RtmpChunkCodecFilter();
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
    protected IoProcessorEx<WsrSession> initProcessor() {
        return new WsrConnectProcessor();
    }

    @Override
    protected void init() {
        super.init();

    }

    @Override
    protected boolean canConnect(String transportName) {
        return transportName.equals("wsr") || transportName.equals("ws");
    }

    @Override
    protected <T extends ConnectFuture> ConnectFuture connectInternal(
            ResourceAddress connectAddress, IoHandler handler,
            final IoSessionInitializer<T> initializer) {
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

        IoSessionInitializer<ConnectFuture> parentInitializer = createParentInitializer(
                connectAddress, handler, initializer, bridgeConnectFuture);

        URI connectURI = connectAddress.getResource();

        ResourceAddress createAddress = connectAddress.resolve(connectURI.getPath()+CREATE_SUFFIX);

        final ResourceAddress transportAddress = createAddress.getTransport();
        BridgeConnector connector = bridgeServiceFactory.newBridgeConnector(transportAddress);
        connector.connect(transportAddress,
                          selectConnectHandler(transportAddress),
                          parentInitializer)
                .addListener(parentConnectListener);

        return bridgeConnectFuture;
    }

    private IoHandler selectConnectHandler(ResourceAddress address) {
          Protocol protocol = bridgeServiceFactory.getTransportFactory().getProtocol(address.getResource());
          if ( protocol instanceof HttpProtocol) {
              return createHandler;
          }
          throw new RuntimeException(getClass()+
                  ": Cannot select a connect handler for address "+address);
    }

    private <T extends ConnectFuture> IoSessionInitializer<ConnectFuture> createParentInitializer(
            final ResourceAddress connectAddress, final IoHandler handler,
            final IoSessionInitializer<T> initializer,
            final DefaultConnectFuture bridgeConnectFuture) {
        // initialize parent session before connection attempt
        return new IoSessionInitializer<ConnectFuture>() {
            @Override
            public void initializeSession(final IoSession parent,
                    ConnectFuture future) {
                // initializer for bridge session to specify bridge handler,
                // and call user-defined bridge session initializer if present
                final IoSessionInitializer<T> bridgeSessionInitializer = new IoSessionInitializer<T>() {
                    @Override
                    public void initializeSession(IoSession bridgeSession,
                            T future) {
                        bridgeSession.setAttribute(
                                BridgeConnectHandler.DELEGATE_KEY, handler);

                        if (initializer != null) {
                            initializer
                                    .initializeSession(bridgeSession, future);
                        }
                    }
                };

                parent.setAttribute(CONNECT_REQUEST_KEY, new ConnectRequest<>(connectAddress, bridgeConnectFuture, bridgeSessionInitializer));

                // WebSocket-Version required by WsrAcceptor
                HttpSession httpParent = (HttpSession)parent;
                httpParent.addWriteHeader("X-WebSocket-Version", "wsr-1.0");
            }
        };
    }

    private static class ConnectRequest<T extends ConnectFuture> {
    	public final ResourceAddress connectAddress;
    	public final ConnectFuture connectFuture;
    	public final IoSessionInitializer<T> initializer;

    	public ConnectRequest(ResourceAddress connectAddress, ConnectFuture connectFuture, IoSessionInitializer<T> initializer) {
    		this.connectAddress = connectAddress;
    		this.connectFuture = connectFuture;
    		this.initializer = initializer;
    	}
    }

    private IoHandler createHandler = new IoHandlerAdapter<HttpSession>() {

        @Override
        protected void doMessageReceived(HttpSession createSession,
                Object message) throws Exception {
            // Handle fragmentation of response body
            IoBufferEx in = (IoBufferEx) message;
            IoBufferEx buf = CREATE_RESPONSE_KEY.get(createSession);

            if (buf == null) {
                IoBufferAllocatorEx<?> allocator = createSession.getBufferAllocator();
                ByteBuffer nioBuf = allocator.allocate(in.remaining());
                buf = allocator.wrap(nioBuf).setAutoExpander(allocator);
                CREATE_RESPONSE_KEY.set(createSession, buf);
            }

            buf.put(in);
        }

        @Override
        protected void doSessionClosed(final HttpSession createSession)
                throws Exception {
            IoBufferEx buf = CREATE_RESPONSE_KEY.remove(createSession);

            if (buf == null || createSession.getStatus() != HttpStatus.SUCCESS_CREATED) {
                ConnectRequest<?> connectRequest = CONNECT_REQUEST_KEY.get(createSession);
                ConnectFuture connectFuture = connectRequest.connectFuture;
               	connectFuture.setException(new IllegalStateException(
                "Create handshake failed: invalid response").fillInStackTrace());
               	return;
            }

            buf.flip();

            String responseText = buf.getString(UTF_8.newDecoder());

            final ResourceAddress rtmpAddress = resourceAddressFactory.newResourceAddress(URI.create(responseText));


            IoFutureListener<ConnectFuture> parentConnectListener = new IoFutureListener<ConnectFuture>() {
            	@Override
            	public void operationComplete(ConnectFuture future) {

            		ConnectRequest<?> connectRequest = CONNECT_REQUEST_KEY.remove(createSession);
            		try {
            			final ResourceAddress connectAddress = connectRequest.connectAddress;
            			final IoSessionEx session = (IoSessionEx) future.getSession();

            			Callable<WsrSession> bridgeSessionFactory = new Callable<WsrSession>() {
            				@Override
            				public WsrSession call() throws Exception {
            				    IoBufferAllocatorEx<?> parentAllocator = session.getBufferAllocator();
            				    WsrBufferAllocator wsrAllocator = new WsrBufferAllocator(parentAllocator);
            					WsrSession wsrSession = new WsrSession(
            							WsrConnector.this, getProcessor(),
            							connectAddress, connectAddress, session,
            							wsrAllocator, null, null);

            					wsrSession.setRtmpAddress(rtmpAddress);

            					// ability to write will be reactivated when
            					// the rtmp publish stream has been created
            					wsrSession.suspendWrite();

            					return wsrSession;
            				}
            			};

            			WsrSession wsrSession = newSession(connectRequest.initializer, connectRequest.connectFuture, bridgeSessionFactory);
            			session.setAttribute(WSR_SESSION_KEY, wsrSession);
            		} catch (Exception e) {
            			connectRequest.connectFuture.setException(e);
            		}
            	}
            };

            ResourceAddress remoteAddress = createSession.getRemoteAddress();
            ResourceAddress connectAddress = remoteAddress.getTransport();
            BridgeConnector connector = bridgeServiceFactory.newBridgeConnector(connectAddress);
            ConnectFuture connectFuture = connector.connect(connectAddress, ioBridgeHandler, null);
            connectFuture.addListener(parentConnectListener);

        }

        @Override
        protected void doExceptionCaught(HttpSession createSession,
                Throwable cause) throws Exception {
            ConnectRequest<?> connectRequest = CONNECT_REQUEST_KEY.get(createSession);
            ConnectFuture connectFuture = connectRequest.connectFuture;
            if (!connectFuture.isDone()) {
            	connectFuture.setException(cause);
            }
        }

    };

    public void addBridgeFilters(
            org.apache.mina.core.filterchain.IoFilterChain filterChain) {
        // setup logging filters for bridge session
        if (logger.isTraceEnabled()) {
            filterChain.addFirst(TRACE_LOGGING_FILTER, new ObjectLoggingFilter(logger, WsrProtocol.NAME + "#%s"));

        } else if (logger.isDebugEnabled()) {
            filterChain.addFirst(FAULT_LOGGING_FILTER, new ExceptionLoggingFilter(logger, WsrProtocol.NAME + "#%s"));
        }

        filterChain.addLast("rtmp", codec);
        filterChain.addLast("log", new LoggingFilter("transport.rtmp"));
    };

    @Override
    public TransportMetadata getTransportMetadata() {
        return new DefaultTransportMetadata(WsrProtocol.WSR.name());
    }

    private IoHandler ioBridgeHandler = new IoHandlerAdapter<IoSession>() {

        @Override
        protected void doSessionOpened(IoSession session) throws Exception {
            IoFilterChain filterChain = session.getFilterChain();
            addBridgeFilters(filterChain);

            RtmpVersionMessage version = new RtmpVersionMessage();
            session.write(version);
            RtmpHandshakeRequestMessage handshakeRequest = new RtmpHandshakeRequestMessage();
            session.write(handshakeRequest);
        }

        @Override
        protected void doSessionClosed(IoSession session) throws Exception {
            WsrSession wsrSession = WSR_SESSION_KEY.get(session);
            if (wsrSession != null && !wsrSession.isClosing()) {
                // TODO: require RTMP controlled close handshake
                wsrSession.reset(new Exception("Early termination of IO session").fillInStackTrace());
            }
        }

        @Override
        protected void doExceptionCaught(IoSession session, Throwable cause)
                throws Exception {
            WsrSession wsrSession = WSR_SESSION_KEY.get(session);
            if (wsrSession != null && !wsrSession.isClosing()) {
                wsrSession.reset(cause);
            }
            else {
                if (logger.isDebugEnabled()) {
                    String message = format("Error on WebSocket WSR connection attempt: %s", cause);
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
        }

        @Override
        protected void doSessionIdle(IoSession session, IdleStatus status)
                throws Exception {
            WsrSession wsrSession = WSR_SESSION_KEY.get(session);
            IoFilterChain filterChain = wsrSession.getFilterChain();
            filterChain.fireSessionIdle(status);
        }

        @Override
        protected void doMessageReceived(IoSession session, Object message)
                throws Exception {
            WsrSession wsrSession = WSR_SESSION_KEY.get(session);
            RtmpMessage rtmpMessage = (RtmpMessage) message;

            switch (rtmpMessage.getKind()) {
            case VERSION:
                // ignore
                break;
            case HANDSHAKE_REQUEST:
                RtmpHandshakeMessage handshake = (RtmpHandshakeMessage) message;
                handshake.setTimestamp2(handshake.getTimestamp1() + 1);
                session.write(handshake);

                RtmpSetChunkSizeMessage setChunkSize = new RtmpSetChunkSizeMessage();
                setChunkSize.setChunkStreamId(2);
                setChunkSize.setMessageStreamId(0);
                setChunkSize.setChunkSize(RtmpEncoder.MAXIMUM_CHUNK_SIZE);
                session.write(setChunkSize);

                RtmpConnectCommandMessage connectCommand = new RtmpConnectCommandMessage();
                // note: additional properties are probably necessary to connect to other RTMP Servers
                String originUrl = "";
                String tcUrl = wsrSession.getRtmpAddress().getResource().toString();
                connectCommand.setSwfUrl(originUrl);
                connectCommand.setTcUrl(tcUrl);

                session.write(connectCommand);
                break;
            case HANDSHAKE_RESPONSE:
                // ignore
                break;
            case STREAM:
                RtmpStreamMessage streamMessage = (RtmpStreamMessage) rtmpMessage;
                switch (streamMessage.getStreamKind()) {
                case COMMAND_AMF0:
                case COMMAND_AMF3:
                    RtmpCommandMessage commandMessage = (RtmpCommandMessage) streamMessage;
                    switch (commandMessage.getCommandKind()) {
                    case CONNECT_RESULT:
                        doCreateStreams(session);
                        break;
                    case CREATE_STREAM_RESULT:
                    	RtmpCreateStreamResultCommandMessage createResult = (RtmpCreateStreamResultCommandMessage) commandMessage;
                    	// which stream?
                    	double streamId = createResult.getStreamId();
                    	if ((int) streamId == 1) {
                    		doPlay(session);
                    	} else {
                    		doPublish(session);
                            //TODO: If we deferred opening the client until after both the HTTP and RTMP connections
                            //TODO: are established - that way there is no longer any need to protect against early writes
                            //TODO: and we can eliminate this usage of suspendWrite/resumeWrite.
                    		wsrSession.resumeWrite();
                            // We are always aligned now. if (session.isIoAligned()) {
                            wsrSession.getProcessor().flush(wsrSession);
                    	}
                        break;
                    case PLAY_RESONSE:
                    	break;
                    case PUBLISH_RESPONSE:
                        // activate for writes
                        break;
                    default:
                        throw new Exception("Unexpected command");
                    }
                    break;
                case DATA_AMF3:
                    RtmpBinaryDataMessage amfData = (RtmpBinaryDataMessage) rtmpMessage;
                    IoBufferEx buf = amfData.getBytes();

                    if (wsrSession != null) {
                        IoFilterChain filterChain = wsrSession.getFilterChain();
                        filterChain.fireMessageReceived(buf);
                    }
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Unrecognized stream message kind: "
                                    + streamMessage.getStreamKind());
                }
                break;
            }

        }

        private void doPlay(IoSession session) {
        	RtmpPlayCommandMessage play = new RtmpPlayCommandMessage();
        	play.setChunkStreamId(5);
        	play.setTransactionId(3);
        	play.setMessageStreamId(1);
        	session.write(play);
        }

        private void doPublish(IoSession session) {
        	RtmpPublishCommandMessage publish = new RtmpPublishCommandMessage();
        	publish.setChunkStreamId(5);
        	publish.setTransactionId(4);
        	publish.setMessageStreamId(2);
        	session.write(publish);
        }

        private void doCreateStreams(IoSession session) {
            RtmpCreateStreamCommandMessage createUpstream = new RtmpCreateStreamCommandMessage();
            createUpstream.setChunkStreamId(3);
            createUpstream.setTransactionId(1);
            session.write(createUpstream);

            RtmpCreateStreamCommandMessage createDownstream = new RtmpCreateStreamCommandMessage();
            createDownstream.setChunkStreamId(3);
            createDownstream.setTransactionId(2);
            session.write(createDownstream);
        }
    };

}
