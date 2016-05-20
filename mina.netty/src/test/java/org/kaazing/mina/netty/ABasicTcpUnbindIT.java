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
package org.kaazing.mina.netty;

import static java.lang.String.format;
import static org.jboss.netty.channel.Channels.pipeline;
import static org.jboss.netty.channel.Channels.pipelineFactory;
import static org.jboss.netty.logging.InternalLoggerFactory.setDefaultFactory;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioWorker;
import org.jboss.netty.channel.socket.nio.NioWorkerPool;
import org.jboss.netty.channel.socket.nio.WorkerPool;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assume;
import org.kaazing.mina.netty.socket.nio.DefaultNioSocketChannelIoSessionConfig;
import org.kaazing.mina.netty.socket.nio.NioSocketChannelIoAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ABasicTcpUnbindIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ABasicTcpUnbindIT.class);

    private boolean checkIfUnbound(URI connectToURI) {
        return checkIfUnbound(connectToURI.getHost(), connectToURI.getPort());
    }

    private boolean checkIfUnbound(String host, int port) {
        Socket socket;
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(format("Testing to see if the %s:%s is unbound", host, port));
        }
        try {
            socket = new Socket(host, port);
            socket.close();
            LOGGER.error(format("Should not be able to connect to %s:%d because it should have been unbound, however we did",
                    host, port));
            return false;

        } catch (IOException e) {
            // Test passes if we get an IOException since the host:port should be unbound
            return true;
        }
    }

    @BeforeClass
    public static void setLoggingFactory() throws Exception {
        setDefaultFactory(new Slf4JLoggerFactory());
    }

    @Test
    public void shouldBindToIPv6AddressUsingNetty() throws Exception {

        ServerBootstrap bootstrap = null;
        try {
            URI bindURI = URI.create("tcp://[0:0:0:0:0:0:0:1]:8000");
            final InetSocketAddress address = new InetSocketAddress(bindURI.getHost(), bindURI.getPort());
            LOGGER.info(format("Binding to %s\n", address));

            ChannelFactory factory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors
                    .newCachedThreadPool());

            bootstrap = new ServerBootstrap(factory);
            bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
                @Override
                public ChannelPipeline getPipeline() {
                    return Channels.pipeline();
                }
            });
            bootstrap.setOption("child.tcpNoDelay", true);
            bootstrap.setOption("child.keepAlive", true);
            bootstrap.setOption("reuseAddress", true);
            Channel boundChannel = bootstrap.bind(address);


            LOGGER.info(format("Successfully bound to %s\n", bindURI));

            boundChannel.close();
            assertTrue("socket is bound", checkIfUnbound(bindURI));

            bootstrap.releaseExternalResources();
            assertTrue("socket is bound", checkIfUnbound(bindURI));
        } catch (Exception e) {
            // Some build environments do not support IPv6 at all, including TravisCI
            // This essentually disables these tests for that build environment
            Assume.assumeFalse(e.getMessage().contains("Protocol family unavailable"));
            Throwable cause = e.getCause();
            if (cause != null) {
                Assume.assumeFalse(cause.getMessage().contains("Protocol family unavailable"));
            }
            e.printStackTrace();
            throw e;
        } finally {
            if (bootstrap != null) {
                bootstrap.releaseExternalResources();
            }
        }

    }

    @Test
    public void shouldUnbindOnMinaNetty() throws Exception {
        String host = "localhost";
        int port = 8018;
        InetSocketAddress bindTo = new InetSocketAddress(host, port);
        final AtomicInteger acceptExceptionsCaught = new AtomicInteger(0);

        // Mimic what NioSocketAcceptor does (in initAcceptor)
        WorkerPool<NioWorker> workerPool = new NioWorkerPool(
                Executors.newCachedThreadPool(), // worker executor
                3); // number of workers

        NioServerSocketChannelFactory serverChannelFactory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(),
                workerPool);

        final NioSocketChannelIoAcceptor acceptor = new NioSocketChannelIoAcceptor(new DefaultNioSocketChannelIoSessionConfig(),
                                                  serverChannelFactory);

        try {
            DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();
            acceptor.setPipelineFactory(pipelineFactory(pipeline(new LoggingHandler(InternalLogLevel.INFO))));
            acceptor.setFilterChainBuilder(builder);
            acceptor.setHandler(new IoHandlerAdapter() {

                @Override
                public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
                    acceptExceptionsCaught.incrementAndGet();
                }
            });

            acceptor.bind(bindTo);

            acceptor.unbind();

            assertTrue("socket is bound", checkIfUnbound(host, port));
        } finally {
            if (acceptor != null) {
                acceptor.dispose();
            }
        }
    }
}
