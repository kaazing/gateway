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

import static org.kaazing.gateway.util.InternalSystemProperty.TCP_MINIMUM_READ_BUFFER_SIZE;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_MAXIMUM_READ_BUFFER_SIZE;
import static org.kaazing.gateway.util.InternalSystemProperty.UDP_IDLE_TIMEOUT;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jboss.netty.channel.socket.nio.NioServerDatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioWorker;
import org.jboss.netty.channel.socket.nio.WorkerPool;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.uri.URIUtils;
import org.kaazing.gateway.transport.BridgeSessionInitializer;
import org.kaazing.gateway.transport.NioBindException;
import org.kaazing.gateway.transport.bio.MulticastAcceptor;
import org.kaazing.mina.core.service.IoAcceptorEx;
import org.kaazing.mina.netty.socket.DatagramChannelIoSessionConfig;
import org.kaazing.mina.netty.socket.DefaultDatagramChannelIoSessionConfig;
import org.kaazing.mina.netty.socket.nio.NioDatagramChannelIoAcceptor;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.net.InetAddress;
import java.util.Properties;

import static java.util.concurrent.Executors.newCachedThreadPool;

public class NioDatagramAcceptor extends AbstractNioAcceptor {

    private static final String LOGGER_NAME = String.format("transport.%s.accept", NioProtocol.UDP.name().toLowerCase());

    private NioSocketAcceptor tcpAcceptor;

    @Resource(name = "tcp.acceptor")
    public void setTcpAcceptor(NioSocketAcceptor tcpAcceptor) {
        this.tcpAcceptor = tcpAcceptor;
    }

    public NioDatagramAcceptor(Properties configuration) {
        super(configuration, LoggerFactory.getLogger(LOGGER_NAME));
    }
    @Override
    protected String getTransportName() {
        return "udp";
    }

	@Override
    protected IoAcceptorEx initAcceptor(final IoSessionInitializer<? extends IoFuture> initializer) {
	    DatagramChannelIoSessionConfig config = new DefaultDatagramChannelIoSessionConfig();
        WorkerPool<NioWorker> workerPool = tcpAcceptor.initWorkerPool(logger, "UDP acceptor: {}", configuration);
        NioServerDatagramChannelFactory channelFactory = new NioServerDatagramChannelFactory(newCachedThreadPool(), 1, workerPool);
        NioDatagramChannelIoAcceptor acceptor = new NioDatagramChannelIoAcceptor(config, channelFactory);
        acceptor.setIoSessionInitializer(initializer);

        String readBufferSize = configuration.getProperty("org.kaazing.gateway.transport.udp.READ_BUFFER_SIZE");
        if (readBufferSize != null) {
            acceptor.getSessionConfig().setReadBufferSize(Integer.parseInt(readBufferSize));
            logger.debug("READ_BUFFER_SIZE setting for UDP acceptor: {}", readBufferSize);
        }

        String minimumReadBufferSize = configuration.getProperty(TCP_MINIMUM_READ_BUFFER_SIZE.getPropertyName());
        if (minimumReadBufferSize != null) {
            acceptor.getSessionConfig().setMinReadBufferSize(Integer.parseInt(minimumReadBufferSize));
            logger.debug("MINIMUM_READ_BUFFER_SIZE setting for UDP acceptor: {}", minimumReadBufferSize);
        }

        String maximumReadBufferSize = configuration.getProperty(TCP_MAXIMUM_READ_BUFFER_SIZE.getPropertyName());
        if (maximumReadBufferSize != null) {
            acceptor.getSessionConfig().setMaxReadBufferSize(Integer.parseInt(maximumReadBufferSize));
            logger.debug("MAXIMUM_READ_BUFFER_SIZE setting for UDP acceptor: {}", maximumReadBufferSize);
        }

        int idleTimeout = UDP_IDLE_TIMEOUT.getIntProperty(configuration);
        acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, idleTimeout);

        return acceptor;
    }

    @Override
    public void bind(final ResourceAddress address,
                     IoHandler handler,
                     BridgeSessionInitializer<? extends IoFuture> initializer) throws NioBindException {
        boolean useMCP = false;
        try {
            String uri = address.getExternalURI();
            InetAddress inet = InetAddress.getByName(URIUtils.getHost(uri));
            if (inet.isMulticastAddress()) {
                useMCP = true;
            }
        } catch (Exception e) {
            // do nothing
        }
        if (useMCP) {
            MulticastAcceptor acceptor = new MulticastAcceptor();
            acceptor.setConfiguration(new Properties());
            acceptor.setResourceAddressFactory(resourceAddressFactory);
            acceptor.setBridgeServiceFactory(bridgeServiceFactory);
            acceptor.bind(address, handler, initializer);
        } else {
            super.bind(address, handler, initializer);
        }
    }

}
