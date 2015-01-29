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

package org.kaazing.mina.netty;

import static java.lang.String.format;
import static org.jboss.netty.channel.Channels.pipeline;
import static org.jboss.netty.channel.Channels.pipelineFactory;
import static org.jboss.netty.logging.InternalLoggerFactory.setDefaultFactory;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kaazing.mina.netty.socket.nio.DefaultNioSocketChannelIoSessionConfig;
import org.kaazing.mina.netty.socket.nio.NioSocketChannelIoAcceptor;

public class ABasicTcpUnbindIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ABasicTcpUnbindIT.class);

    private boolean checkIfUnbound(URI connectToURI) {
        return checkIfUnbound(connectToURI.getHost(), connectToURI.getPort());
    }

    private boolean checkIfUnbound(String host, int port) {
        Socket socket = null;
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
    public void shouldUnBindToSpecificLocalIpAddress() throws Exception {
        ServerSocket serverSocket = null;
        try {
            // final URI bindURI = URI.create("tcp://[0:0:0:0:0:0:0:1]:8000");
            final URI bindURI = URI.create("tcp://localhost:8000");
            final InetSocketAddress address = new InetSocketAddress(bindURI.getHost(), bindURI.getPort());
            LOGGER.info(format("Binding to %s\n", address));

            serverSocket = new ServerSocket();
            serverSocket.bind(address);

            assert serverSocket.isBound();
            Assert.assertTrue("Server socket is not bound.", serverSocket.isBound());

            serverSocket.close();
            Assert.assertTrue("socket is unbound", checkIfUnbound(bindURI));

            LOGGER.info(format("Successfully unbound to %s\n", bindURI));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (serverSocket != null) {
                serverSocket.close();
            }
        }

    }

    @Test
    public void shouldBindToIPv6AddressUsingNIO() throws Exception {
        ServerSocketChannel serverSocketChannel = null;
        try {
            URI bindURI = URI.create("tcp://[0:0:0:0:0:0:0:1]:8000");
            final InetSocketAddress address = new InetSocketAddress(bindURI.getHost(), bindURI.getPort());
            LOGGER.info(format("Binding to %s\n", address));

            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.socket().bind(address);

            assertTrue("Server socket is not open.", serverSocketChannel.isOpen());

            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, null);
            serverSocketChannel.close();
            // On Windows, you need to do a select for the selector key to be unregistered and only then is the socket
            // actually closed
            selector.select(1);
            assertTrue("socket is bound", checkIfUnbound(bindURI));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (serverSocketChannel != null) {
                serverSocketChannel.close();
            }
        }

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
