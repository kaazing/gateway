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
package org.kaazing.gateway.service.broadcast;

import static org.kaazing.gateway.resource.address.uri.URIUtils.getHost;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getPort;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.transport.socket.nio.NioSocketConnectorEx;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioWorker;
import org.jboss.netty.channel.socket.nio.NioWorkerPool;
import org.jboss.netty.channel.socket.nio.WorkerPool;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.security.CrossSiteConstraintContext;
import org.kaazing.gateway.security.RealmContext;
import org.kaazing.gateway.service.AcceptOptionsContext;
import org.kaazing.gateway.service.ConnectOptionsContext;
import org.kaazing.gateway.service.MonitoringEntityFactory;
import org.kaazing.gateway.service.Service;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.ServiceProperties;
import org.kaazing.gateway.transport.BridgeSessionInitializer;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.service.IoAcceptorEx;
import org.kaazing.mina.core.service.IoConnectorEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.netty.socket.nio.DefaultNioSocketChannelIoSessionConfig;
import org.kaazing.mina.netty.socket.nio.NioSocketChannelIoAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestServiceContext implements ServiceContext {
    private final Logger logger = LoggerFactory.getLogger(TestServiceContext.class);

    private final Service service;
    private final String name;
    private final TestAcceptor acceptor;
    private final TestConnector connector;
    private final String acceptURI;
    private final String connectURI;
    private final ServiceProperties serviceProperties;

    public TestServiceContext(Service service, String name, String acceptURI, String connectURI) {
        this.service = service;
        this.name = name;
        this.acceptor = new TestAcceptor();
        this.connector = new TestConnector();
        this.acceptURI = acceptURI;
        this.connectURI = connectURI;
        this.serviceProperties = new TestServiceProperties();
    }

    @Override
    public RealmContext getServiceRealm() {
        return null;
    }

    @Override
    public String getAuthorizationMode() {
        return null;
    }

    @Override
    public String getSessionTimeout() {
        return null;
    }

    @Override
    public String getServiceType() {
        return service.getType();
    }

    @Override
    public String getServiceName() {
        return name;
    }

    @Override
    public String getServiceDescription() {
        return null;
    }

    @Override
    public Collection<String> getAccepts() {
        return Collections.singletonList(acceptURI);
    }

    @Override
    public Collection<String> getBalances() {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getConnects() {
        return Collections.singletonList(connectURI);
    }

    @Override
    public Map<String, String> getMimeMappings() {
        return null;
    }

    @Override
    public ServiceProperties getProperties() {
        return serviceProperties;
    }

    @Override
    public Service getService() {
        return service;
    }

    @Override
    public String[] getRequireRoles() {
        return null;
    }

    @Override
    public String getContentType(String fileExtension) {
        return null;
    }

    @Override
    public Map<String, ? extends Map<String, ? extends CrossSiteConstraintContext>> getCrossSiteConstraints() {
        return null;
    }

    @Override
    public File getWebDirectory() {
        return null;
    }

    @Override
    public File getTempDirectory() {
        return null;
    }

    @Override
    public void init() throws Exception {
        service.init(this);
    }

    @Override
    public void start() throws Exception {
        service.start();
    }

    @Override
    public void bind(Collection<String> acceptURIs, IoHandler handler) {
        bind(acceptURIs, handler, null, null);
    }

    @Override
    public void bind(Collection<String> acceptURIs, IoHandler handler, AcceptOptionsContext acceptOptionsContext) {
        bind(acceptURIs, handler, acceptOptionsContext, null);
    }

    @Override
    public void bind(Collection<String> acceptURIs,
                     IoHandler handler,
                     AcceptOptionsContext acceptOptionsContext,
                     BridgeSessionInitializer<ConnectFuture> bridgeSessionInitializer) {
        try {
            acceptor.bind(acceptURIs, handler);
        } catch (IOException ex) {
            throw new RuntimeException("Problems binding TestServiceContext to acceptURIs: " + acceptURIs, ex);
        }
    }

    @Override
    public void bind(Collection<String> acceptURIs,
                     IoHandler handler,
                     BridgeSessionInitializer<ConnectFuture> bridgeSessionInitializer) {
        bind(acceptURIs, handler, null, bridgeSessionInitializer);
    }

    @Override
    public void bindConnectsIfNecessary(Collection<String> connectURIs) {
    }

    @Override
    public void unbind(Collection<String> acceptURIs, IoHandler handler) {
        acceptor.unbind(acceptURIs);
    }

    @Override
    public void unbindConnectsIfNecessary(Collection<String> connectURIs) {
    }

    @Override
    public void stop() throws Exception {
        service.stop();
    }

    @Override
    public void destroy() throws Exception {
        service.destroy();
    }

    @Override
    public ConnectFuture connect(String connectURI,
                                 IoHandler connectHandler,
                                 IoSessionInitializer<ConnectFuture> ioSessionInitializer) {
        return connector.connect(connectURI, connectHandler, ioSessionInitializer);
    }

    @Override
    public ConnectFuture connect(ResourceAddress address,
                                 IoHandler connectHandler,
                                 IoSessionInitializer<ConnectFuture> ioSessionInitializer) {
        return connect(address.getExternalURI(), connectHandler, ioSessionInitializer);
    }

    @Override
    public Collection<IoSessionEx> getActiveSessions() {
        return Collections.emptyList();
    }

    @Override
    public IoSessionEx getActiveSession(Long sessionId) {
        return null;
    }

    @Override
    public void addActiveSession(IoSessionEx session) {
    }

    @Override
    public void removeActiveSession(IoSessionEx session) {
    }

    @Override
    public AcceptOptionsContext getAcceptOptionsContext() {
        return null;
    }

    @Override
    public ConnectOptionsContext getConnectOptionsContext() {
        return null;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public SchedulerProvider getSchedulerProvider() {
        return null;
    }

    @Override
    public String decrypt(String encrypted) throws Exception {
        return null;
    }

    @Override
    public String encrypt(String plaintext) throws Exception {
        return null;
    }

    @Override
    public boolean supportsAccepts() {
        return true;
    }

    @Override
    public boolean supportsConnects() {
        return true;
    }

    @Override
    public boolean supportsMimeMappings() {
        return false;
    }

    @Override
    public int getProcessorCount() {
        return 1;
    }

    @Override
    public void setListsOfAcceptConstraintsByURI(List<Map<String, Map<String, CrossSiteConstraintContext>>> authorityToSetOfAcceptConstraintsByURI) {
    }

    private class TestAcceptor {
        private final Map<SocketAddress, IoHandler> bindings;
        private final IoAcceptorEx ioAcceptor;

        private TestAcceptor() {
            bindings = new HashMap<>();

            WorkerPool<NioWorker> workerPool = new NioWorkerPool(Executors.newCachedThreadPool(), 1);
            NioServerSocketChannelFactory serverChannelFactory = new NioServerSocketChannelFactory(
                    Executors.newCachedThreadPool(),
                    workerPool);
            ioAcceptor = new NioSocketChannelIoAcceptor(new DefaultNioSocketChannelIoSessionConfig(),
                                                      serverChannelFactory,
                                                      new SimpleChannelUpstreamHandler());
            // set SO_LINGER to 0 so that calling session.close(true) forces a TCP reset
            ((NioSocketChannelIoAcceptor)ioAcceptor).getSessionConfig().setSoLinger(0);
            ioAcceptor.setHandler(new IoHandlerAdapter<IoSessionEx>() {

                @Override
                public void doExceptionCaught(IoSessionEx session, Throwable t) throws Exception {
                    SocketAddress localAddress = session.getLocalAddress();
                    IoHandler handler = bindings.get(localAddress);
                    if (handler != null) {
                        handler.exceptionCaught(session, t);
                    }
                }

                @Override
                public void doMessageReceived(IoSessionEx session, Object message) throws Exception {
                    SocketAddress localAddress = session.getLocalAddress();
                    IoHandler handler = bindings.get(localAddress);
                    if (handler != null) {
                        handler.messageReceived(session, message);
                    }
                }

                @Override
                public void doMessageSent(IoSessionEx session, Object message) throws Exception {
                    SocketAddress localAddress = session.getLocalAddress();
                    IoHandler handler = bindings.get(localAddress);
                    if (handler != null) {
                        handler.messageSent(session, message);
                    }
                }

                @Override
                public void doSessionClosed(IoSessionEx session) throws Exception {
                    SocketAddress localAddress = session.getLocalAddress();
                    IoHandler handler = bindings.get(localAddress);
                    if (handler != null) {
                        handler.sessionClosed(session);
                    }
                }

                @Override
                public void doSessionCreated(IoSessionEx session) throws Exception {
                    SocketAddress localAddress = session.getLocalAddress();
                    IoHandler handler = bindings.get(localAddress);
                    if (handler != null) {
                        handler.sessionCreated(session);
                    }
                }

                @Override
                public void doSessionOpened(IoSessionEx session) throws Exception {
                    SocketAddress localAddress = session.getLocalAddress();
                    IoHandler handler = bindings.get(localAddress);
                    if (handler != null) {
                        handler.sessionOpened(session);
                    }
                }
            });
        }

        private void bind(Collection<String> bindURIs, IoHandler handler) throws IOException {
            for (String bindURI : bindURIs) {
                // should be only one since this is a test class
                InetSocketAddress address = new InetSocketAddress(getHost(bindURI), getPort(bindURI));
                bindings.put(address, handler);
                ioAcceptor.bind(address);
            }
        }

        private void unbind(Collection<String> bindURIs) {
            for (String bindURI : bindURIs) {
                // should be only one since this is a test class
                InetSocketAddress address = new InetSocketAddress(getHost(bindURI), getPort(bindURI));
                ioAcceptor.unbind(address);
            }
        }
    }

    private class TestConnector {
        private final IoConnectorEx ioConnector;

        private TestConnector() {
            ioConnector = new NioSocketConnectorEx(1);
        }

        private ConnectFuture connect(String connectURI, IoHandler connectHandler, final IoSessionInitializer<ConnectFuture> ioSessionInitializer) {
            InetSocketAddress address = new InetSocketAddress(getHost(connectURI), getPort(connectURI));
            ioConnector.setHandler(connectHandler);
            return ioConnector.connect(address, new IoSessionInitializer<ConnectFuture>() {
                @Override
                public void initializeSession(IoSession session, ConnectFuture future) {
                    if (ioSessionInitializer != null) {
                        ioSessionInitializer.initializeSession(session, future);
                    }
                }
            });
        }
    }

    private class TestServiceProperties implements ServiceProperties {

        private Map<String, String> simpleProperties;
        private Map<String, List<ServiceProperties>> nestedProperties;

        private TestServiceProperties() {
            simpleProperties = new HashMap<>();
            nestedProperties = new HashMap<>();
        }

        @Override
        public String get(String name) {
            return simpleProperties.get(name);
        }

        @Override
        public List<ServiceProperties> getNested(String name) {
            return nestedProperties.get(name);
        }

        @Override
        public List<ServiceProperties> getNested(String name, boolean create) {
            List<ServiceProperties> nestedPropertyList = nestedProperties.get(name);
            if (create && (nestedPropertyList == null)) {
                nestedPropertyList = new ArrayList<>();
                nestedProperties.put(name, nestedPropertyList);
            }
            return nestedPropertyList;
        }

        @Override
        public Iterable<String> simplePropertyNames() {
            return simpleProperties.keySet();
        }

        @Override
        public Iterable<String> nestedPropertyNames() {
            return nestedProperties.keySet();
        }

        @Override
        public boolean isEmpty() {
            return simpleProperties.isEmpty() && nestedProperties.isEmpty();
        }

        @Override
        public void put(String name, String value) {
            simpleProperties.put(name, value);
        }
    }

    @Override
    public Map<String, Object> getServiceSpecificObjects() {
        return null;
    }

    @Override
    public IoSessionInitializer<ConnectFuture> getSessionInitializor() {
        // Not used in tests
        return null;
    }

    @Override
    public void setSessionInitializor(IoSessionInitializer<ConnectFuture> ioSessionInitializer) {
        // Not used in tests
    }

    @Override
    public MonitoringEntityFactory getMonitoringFactory() {
        // Not used in tests
        return null;
    }

    @Override
    public void setMonitoringFactory(MonitoringEntityFactory monitoringFactory) {
        // Not used in tests
        
    }
}
