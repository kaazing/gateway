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

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_IP_TOS;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_KEEP_ALIVE;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_MAXIMUM_READ_BUFFER_SIZE;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_MINIMUM_READ_BUFFER_SIZE;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_NO_DELAY;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_READ_BUFFER_SIZE;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_RECEIVE_BUFFER_SIZE;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_SEND_BUFFER_SIZE;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_SO_LINGER;

import java.util.Properties;

import javax.annotation.Resource;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.transport.socket.SocketConnectorEx;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioWorker;
import org.jboss.netty.channel.socket.nio.WorkerPool;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeConnector;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.mina.core.service.IoConnectorEx;
import org.kaazing.mina.netty.socket.nio.DefaultNioSocketChannelIoSessionConfig;
import org.kaazing.mina.netty.socket.nio.NioSocketChannelIoConnector;
import org.slf4j.LoggerFactory;

public class NioSocketConnector extends AbstractNioConnector {
    static {
        // We must set the select timeout property before Netty class SelectorUtil gets loaded
        NioSocketAcceptor.initSelectTimeout();
    }

    private static final String LOGGER_NAME = String.format("transport.%s.connect", NioProtocol.TCP.name().toLowerCase());

    private BridgeServiceFactory bridgeServiceFactory;
    private ResourceAddressFactory resourceAddressFactory;
    private NioSocketAcceptor tcpAcceptor;
    private BridgeConnector proxyConnector;

    public NioSocketConnector(Properties configuration) {
        super(configuration, LoggerFactory.getLogger(LOGGER_NAME));
    }

    @Resource(name = "bridgeServiceFactory")
    public void setBridgeServiceFactory(BridgeServiceFactory bridgeServiceFactory) {
        this.bridgeServiceFactory = bridgeServiceFactory;
    }

    @Resource(name = "resourceAddressFactory")
    public void setResourceAddressFactory(ResourceAddressFactory factory) {
        this.resourceAddressFactory = factory;
    }

    @Resource(name = "tcp.acceptor")
    public void setTcpAcceptor(NioSocketAcceptor tcpAcceptor) {
        this.tcpAcceptor = tcpAcceptor;
    }

    @Resource(name = "proxy.connector")
    public void setProxyConnector(BridgeConnector proxyConnector) {
        this.proxyConnector = proxyConnector;
    }

    @Override
    protected ResourceAddressFactory initResourceAddressFactory() {
        return resourceAddressFactory;
    }

    @Override
    protected BridgeServiceFactory initBridgeServiceFactory() {
        return bridgeServiceFactory;
    }

    @Override
    protected <T extends ConnectFuture> ConnectFuture connectInternal(
            ResourceAddress address, IoHandler handler,
            IoSessionInitializer<T> initializer) {

        if (proxyConnector != null) {
            ConnectFuture future = proxyConnector.connect(address, handler, initializer);
            if (future != null) {
                return future;
            }
        }
        return super.connectInternal(address, handler, initializer);
    }

    @Override
    protected IoConnectorEx initConnector() {
        String readBufferSize = TCP_READ_BUFFER_SIZE.getProperty(configuration);
        String minimumReadBufferSize = TCP_MINIMUM_READ_BUFFER_SIZE.getProperty(configuration);
        String maximumReadBufferSize = TCP_MAXIMUM_READ_BUFFER_SIZE.getProperty(configuration);

        String keepAlive = TCP_KEEP_ALIVE.getProperty(configuration);
        String tcpNoDelay = TCP_NO_DELAY.getProperty(configuration);
        String receiveBufferSize = TCP_RECEIVE_BUFFER_SIZE.getProperty(configuration);
        String sendBufferSize = TCP_SEND_BUFFER_SIZE.getProperty(configuration);
        String linger = TCP_SO_LINGER.getProperty(configuration);
        String ipTypeOfService = TCP_IP_TOS.getProperty(configuration);

        SocketConnectorEx connector;

        WorkerPool<NioWorker> workerPool = tcpAcceptor.initWorkerPool(logger, "TCP connector: {}", getConfiguration());
        int bossCount = 1;
        NioClientSocketChannelFactory clientChannelFactory = new NioClientSocketChannelFactory(
        		newCachedThreadPool(),
        		bossCount,
        		workerPool);
        connector = new NioSocketChannelIoConnector(new DefaultNioSocketChannelIoSessionConfig(), clientChannelFactory);

        if ("true".equals(keepAlive)) {
            connector.getSessionConfig().setKeepAlive(true);
            logger.debug("KEEP_ALIVE setting for TCP connector: {}", keepAlive);
        }

        if (tcpNoDelay != null) {
            connector.getSessionConfig().setTcpNoDelay(Boolean.parseBoolean(tcpNoDelay));
            logger.debug("TCP_NO_DELAY setting for TCP connector: {}", tcpNoDelay);
        }
        else {
            connector.getSessionConfig().setTcpNoDelay(true);
        }

        if (readBufferSize != null) {
            connector.getSessionConfig().setReadBufferSize(Integer.parseInt(readBufferSize));
            logger.debug("READ_BUFFER_SIZE setting for TCP connector: {}", readBufferSize);
        }

        if (minimumReadBufferSize != null) {
            connector.getSessionConfig().setMinReadBufferSize(Integer.parseInt(minimumReadBufferSize));
            logger.debug("MINIMUM_READ_BUFFER_SIZE setting for TCP connector: {}", minimumReadBufferSize);
        }

        if (maximumReadBufferSize != null) {
            connector.getSessionConfig().setMaxReadBufferSize(Integer.parseInt(maximumReadBufferSize));
            logger.debug("MAXIMUM_READ_BUFFER_SIZE setting for TCP connector: {}", maximumReadBufferSize);
        }

        if (receiveBufferSize != null) {
            connector.getSessionConfig().setReceiveBufferSize(Integer.parseInt(receiveBufferSize));
            logger.debug("SO RECEIVE BUFFER SIZE setting for TCP connector: {}", receiveBufferSize);
        }

        if (sendBufferSize != null) {
            connector.getSessionConfig().setSendBufferSize(Integer.parseInt(sendBufferSize));
            logger.debug("SO SEND BUFFER SIZE setting for TCP connector: {}", sendBufferSize);
        }

        if (linger != null) {
            connector.getSessionConfig().setSoLinger(Integer.parseInt(linger));
            logger.debug("Linger Interval for TCP connector: {}", linger);
        }

        if (ipTypeOfService != null) {
            connector.getSessionConfig().setTrafficClass(Integer.parseInt(ipTypeOfService));
            logger.debug("IP_TOS for TCP connector: {}", ipTypeOfService);
        }

        return connector;
    }

    @Override
    protected String getTransportName() {
        return "tcp";
    }

}
