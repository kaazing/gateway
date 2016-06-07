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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.net.ServerSocket;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.util.ConcurrentHashSet;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.test.util.MethodExecutionTrace;

public class NioSocketConnectorTest {
    private final ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
    @Rule
    public TestRule testExecutionTrace = new MethodExecutionTrace();

    private NioSocketConnector connector = null;
    private NioSocketAcceptor acceptor = new NioSocketAcceptor(new Properties());

    @After
    public void after() {
        if (connector != null) {
            // Make sure we always shut down I/O worker threads
            connector.dispose();
        }
        acceptor.dispose();
    }

    @Test
    public void allWorkerThreadsShouldBeUsed() throws Exception {
        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());
        final IoHandler handler = context.mock(IoHandler.class);
        final RuntimeException[] exception = new RuntimeException[1];
        final Set<Thread> workerThreadsUsed = new ConcurrentHashSet<>();
        final int NB_WORKERS = 7; // = size of worker pool

        context.checking(new Expectations() {
            {
                allowing(handler).sessionCreated(with(any(IoSession.class)));
                allowing(handler).sessionOpened(with(any(IoSession.class)));
                allowing(handler).sessionClosed(with(any(IoSession.class)));
            }
        });

        Properties configuration = new Properties();
        configuration.setProperty("org.kaazing.gateway.server.transport.tcp.PROCESSOR_COUNT", Integer.toString(NB_WORKERS));
        TransportFactory transportFactory = TransportFactory.newTransportFactory((Map)configuration);
        BridgeServiceFactory bridgeServiceFactory = new BridgeServiceFactory(transportFactory);
        connector = (NioSocketConnector)transportFactory.getTransport("tcp").getConnector();
        connector.setBridgeServiceFactory(bridgeServiceFactory);
        connector.setTcpAcceptor(acceptor);
        connector.setResourceAddressFactory(addressFactory);

        ServerSocket server = new ServerSocket(0);
        int port = server.getLocalPort();

        try {
            for (int i=0; i<NB_WORKERS; i++) {
                ResourceAddress bindAddress = addressFactory.newResourceAddress("tcp://localhost:" + port);
                connector.connect(bindAddress, handler, new IoSessionInitializer<ConnectFuture>() {

                    @Override
                    public void initializeSession(IoSession session, ConnectFuture future) {
                        session.getFilterChain().addFirst("test", new IoFilterAdapter(){

                            @Override
                            public void sessionOpened(NextFilter nextFilter, IoSession session) throws Exception {
                                    try {
                                        //System.out.println("sessionOpened executing in thread " + Thread.currentThread());
                                        workerThreadsUsed.add(Thread.currentThread());
                                    }
                                    catch(RuntimeException e) {
                                        exception[0] = e;
                                    }
                            }

                        });
                    }
                });
            }

            Thread.sleep(1000);

            assertEquals(NB_WORKERS, workerThreadsUsed.size());
            assertNull(exception[0]);

            context.assertIsSatisfied();
        }
        finally {
            connector.dispose();
            server.close();
        }
    }

}
