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
package org.kaazing.gateway.transport.sse;

import static java.lang.String.format;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
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
import org.kaazing.gateway.transport.AbstractBridgeConnector;
import org.kaazing.gateway.transport.BridgeConnector;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.DefaultTransportMetadata;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.TypedAttributeKey;
import org.kaazing.gateway.transport.http.HttpProtocol;
import org.kaazing.gateway.transport.http.HttpSession;
import org.kaazing.gateway.transport.sse.bridge.SseMessage;
import org.kaazing.gateway.transport.sse.bridge.filter.SseBuffer;
import org.kaazing.gateway.transport.sse.bridge.filter.SseBufferAllocator;
import org.kaazing.gateway.transport.sse.bridge.filter.SseConnectCodecFilter;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;

public class SseConnector extends AbstractBridgeConnector<SseSession> {

    private static final TypedAttributeKey<Callable<SseSession>> SSE_SESSION_FACTORY_KEY = new TypedAttributeKey<>(SseConnector.class, "sseSessionFactory");
    private static final TypedAttributeKey<ConnectFuture> SSE_CONNECT_FUTURE_KEY = new TypedAttributeKey<>(SseConnector.class, "sseConnectFuture");
    private static final TypedAttributeKey<SseSession> SSE_SESSION_KEY = new TypedAttributeKey<>(SseConnector.class, "sseSession");

    private static final String CODEC_FILTER = SseProtocol.NAME + "#codec";

    private ScheduledExecutorService scheduler;

    private BridgeServiceFactory bridgeServiceFactory;
    private ResourceAddressFactory resourceAddressFactory;

    @Resource(name = "bridgeServiceFactory")
    public void setBridgeServiceFactory(BridgeServiceFactory bridgeServiceFactory) {
        this.bridgeServiceFactory = bridgeServiceFactory;
    }

    private IoFilter sseCodec;

    public SseConnector() {
        super(new DefaultSseSessionConfig());
    }

    @Resource(name = "schedulerProvider")
    public void setSchedulerProvider(SchedulerProvider provider) {
        this.scheduler = provider.getScheduler("SseConnector_reconnect", false);
    }

    @Resource(name = "resourceAddressFactory")
    public void setResourceAddressFactory(ResourceAddressFactory factory) {
        this.resourceAddressFactory = factory;
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return new DefaultTransportMetadata(SseProtocol.NAME, SseSessionConfig.class);
    }

    @Override
    public void init() {
        super.init();

        sseCodec = new SseConnectCodecFilter();
    }

    @Override
    public void addBridgeFilters(IoFilterChain filterChain) {
        filterChain.addLast(CODEC_FILTER, sseCodec);
    }

    @Override
    public void removeBridgeFilters(IoFilterChain filterChain) {
        removeFilter(filterChain, CODEC_FILTER);
    }

    @Override
    protected boolean canConnect(String transportName) {
        return transportName.equals("sse");
    }

    @Override
    protected <T extends ConnectFuture> ConnectFuture connectInternal(ResourceAddress connectAddress, IoHandler handler,
                                                                      final IoSessionInitializer<T> initializer) {
        final DefaultConnectFuture sseConnectFuture = new DefaultConnectFuture();

        // propagate connection failure, if necessary
        IoFutureListener<ConnectFuture> parentConnectListener = new IoFutureListener<ConnectFuture>() {
            @Override
            public void operationComplete(ConnectFuture future) {
                // fail bridge connect future if parent connect fails
                if (!future.isConnected()) {
                    sseConnectFuture.setException(future.getException());
                }
            }
        };

        IoSessionInitializer<ConnectFuture> parentInitializer = createParentInitializer(connectAddress, handler, initializer,
                sseConnectFuture);

        final ResourceAddress transportAddress = connectAddress.getTransport();
        if (transportAddress == null) {
            throw new RuntimeException("Cannot find transport for resource address "+connectAddress);
        }

        BridgeConnector connector = bridgeServiceFactory.newBridgeConnector(transportAddress);

        connector.connect(transportAddress,
                          selectConnectHandler(transportAddress),
                          parentInitializer).addListener(parentConnectListener);

        return sseConnectFuture;
    }

    private IoHandler selectConnectHandler(ResourceAddress address) {
        Protocol protocol = bridgeServiceFactory.getTransportFactory().getProtocol(address.getResource());
        if ( protocol instanceof HttpProtocol ) {
            return httpHandler;
        }
        throw new RuntimeException(getClass()+
                ": Cannot select a connect handler for address "+address);
    }


    @Override
    protected IoFuture dispose0() throws Exception {
        scheduler.shutdownNow();
        return super.dispose0();
    }

    private <T extends ConnectFuture> IoSessionInitializer<ConnectFuture> createParentInitializer(final ResourceAddress connectAddress,
            final IoHandler handler, final IoSessionInitializer<T> initializer, final DefaultConnectFuture sseConnectFuture) {
        // initialize parent session before connection attempt
        return new IoSessionInitializer<ConnectFuture>() {
            @Override
            public void initializeSession(final IoSession parent, ConnectFuture future) {
                // initializer for bridge session to specify bridge handler,
                // and call user-defined bridge session initializer if present
                final IoSessionInitializer<T> sseSessionInitializer = new IoSessionInitializer<T>() {
                    @Override
                    public void initializeSession(IoSession session, T future) {
                        SseSession sseSession = (SseSession) session;
                        sseSession.setHandler(handler);

                        if (initializer != null) {
                            initializer.initializeSession(session, future);
                        }
                    }
                };

                final HttpSession httpSession = (HttpSession) parent;
                final IoBufferAllocatorEx<SseBuffer> allocator = new SseBufferAllocator(httpSession.getBufferAllocator());

                // factory to create a new bridge session
                Callable<SseSession> createSession = new Callable<SseSession>() {
                    @Override
                    public SseSession call() throws Exception {

                        Callable<SseSession> sseSessionFactory = new Callable<SseSession>() {
                            @Override
                            public SseSession call() throws Exception {
                                return new SseSession(SseConnector.this, getProcessor(), connectAddress, connectAddress, httpSession, allocator);
                            }
                        };

                        return newSession(sseSessionInitializer, sseConnectFuture, sseSessionFactory);
                    }
                };

                SSE_SESSION_FACTORY_KEY.set(httpSession, createSession);
                SSE_CONNECT_FUTURE_KEY.set(httpSession, sseConnectFuture);
            }
        };
    }

    private void reconnectOrClose(final SseSession sseSession) {
        SseSessionConfig config = sseSession.getConfig();
        int retry = config.getRetry();
        if (retry > 0 || config.isReconnecting()) {
            logger.debug("Reconnecting: {}", sseSession.getRemoteAddress());
            config.setReconnecting(false);
            if (retry <= 0) {
                // reconnect immediately
                ResourceAddress connectAddress = sseSession.getRemoteAddress();
                ReconnectListener connectListener = new ReconnectListener(sseSession);
                final ResourceAddress transportAddress = connectAddress.getTransport();
                BridgeConnector connector = bridgeServiceFactory.newBridgeConnector(transportAddress);
                connector.connect(connectAddress, httpHandler, null).addListener(connectListener);
            } else {
                // reconnect after "retry" milliseconds
                scheduler.schedule(new ReconnectCommand(sseSession), retry, TimeUnit.MILLISECONDS);
            }
        }
        else {
            sseSession.reset(new IOException("Early termination of IO session").fillInStackTrace());
        }
    }

    private IoHandler httpHandler = new IoHandlerAdapter<HttpSession>() {

        @Override
        protected void doSessionOpened(HttpSession session) throws Exception {
            // TODO session.get[Ready]Future().addListener(...) to check
            // response status / headers
            IoFilterChain filterChain = session.getFilterChain();
            addBridgeFilters(filterChain);

            SseSession sseSession = SSE_SESSION_KEY.get(session);
            if (sseSession == null) {
                Callable<SseSession> sessionFactory = SSE_SESSION_FACTORY_KEY.remove(session);
                SseSession newSseSession = sessionFactory.call();
                SSE_SESSION_KEY.set(session, newSseSession);
            }
        }

        @Override
        protected void doMessageReceived(final HttpSession session, Object message) throws Exception {

            SseMessage sseMessage = (SseMessage) message;
            String type = sseMessage.getType();
            IoBufferEx data = sseMessage.getData();
            String id = sseMessage.getId();
            boolean reconnect = sseMessage.isReconnect();
            int retry = sseMessage.getRetry();

            SseSession sseSession = SSE_SESSION_KEY.get(session);
            SseSessionConfig config = sseSession.getConfig();
            config.setReconnecting(reconnect);
            if (retry >= 0) {
                config.setRetry(retry);
            }
            if (id != null) {
                config.setLastId(id);
            }

            if (data != null && data.hasRemaining() && (type == null || "message".equals(type))) {
                IoFilterChain filterChain = sseSession.getFilterChain();
                filterChain.fireMessageReceived(data);
            }
        }

        @Override
        protected void doSessionClosed(HttpSession session) throws Exception {
            final SseSession sseSession = SSE_SESSION_KEY.remove(session);
            assert (sseSession != null);

            // TODO: move redirect handling to HttpConnector (optionally)
            switch (session.getStatus()) {
            case REDIRECT_MOVED_PERMANENTLY:
                String location = session.getReadHeader("Location");
                if (location == null) {
                    sseSession.reset(new Exception("Redirect attempted without Location header").fillInStackTrace());
                } else {
                    ResourceAddress newConnectAddress = resourceAddressFactory.newResourceAddress(location);
                    BridgeConnector connector = bridgeServiceFactory.newBridgeConnector(newConnectAddress);
                    connector.connect(newConnectAddress, httpHandler, new IoSessionInitializer<ConnectFuture>() {
                        @Override
                        public void initializeSession(IoSession session, ConnectFuture future) {
                            SSE_SESSION_FACTORY_KEY.set(session, new Callable<SseSession>() {
                                @Override
                                public SseSession call() throws Exception {
                                    return sseSession;
                                }
                            });
                        }
                    }).addListener(new ReconnectListener(sseSession));
                }
                break;
            default:
                reconnectOrClose(sseSession);
                break;
            }
        }

        @Override
        protected void doExceptionCaught(HttpSession session, Throwable cause) throws Exception {
            if (logger.isDebugEnabled()) {
                String message = format("Error on SSE connection attempt: %s", cause);
                if (logger.isTraceEnabled()) {
                    // note: still debug level, but with extra detail about the exception
                    logger.debug(message, cause);
                }
                else {
                    logger.debug(message);
                }
            }

            session.close(true);

            ConnectFuture sseConnectFuture = SSE_CONNECT_FUTURE_KEY.remove(session);
            if (sseConnectFuture != null) {
                sseConnectFuture.setException(cause);
            }
        }

    };

    private class ReconnectCommand implements Runnable {

        private final SseSession sseSession;

        public ReconnectCommand(SseSession sseSession) {
            this.sseSession = sseSession;
        }

        @Override
        public void run() {
            ResourceAddress connectAddress = sseSession.getRemoteAddress();
            ReconnectListener connectListener = new ReconnectListener(sseSession);
            BridgeConnector connector = bridgeServiceFactory.newBridgeConnector(connectAddress);
            connector.connect(connectAddress, httpHandler, null).addListener(connectListener);
        }
    }

    private final class ReconnectListener implements IoFutureListener<ConnectFuture> {
        private final SseSession sseSession;

        private ReconnectListener(SseSession sseSession) {
            this.sseSession = sseSession;
        }

        @Override
        public void operationComplete(ConnectFuture future) {
            if (future.isConnected()) {
                IoSession session = future.getSession();
                session.setAttribute(SSE_SESSION_KEY, sseSession);
                logger.debug("Reconnected: {}", sseSession.getRemoteAddress());
            } else {
                reconnectOrClose(sseSession);
            }
        }
    }
}
