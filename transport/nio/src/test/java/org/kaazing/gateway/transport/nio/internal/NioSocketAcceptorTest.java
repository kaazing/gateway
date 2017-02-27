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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.kaazing.gateway.resource.address.ResourceAddressFactory.newResourceAddressFactory;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_PROCESSOR_COUNT;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.DefaultIoSessionDataStructureFactory;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.util.ConcurrentHashSet;
import org.jboss.netty.channel.socket.Worker;
import org.jboss.netty.channel.socket.nio.NioWorker;
import org.jboss.netty.channel.socket.nio.WorkerPool;
import org.jboss.netty.util.ExternalResourceReleasable;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.Protocol;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeSessionInitializer;
import org.kaazing.gateway.transport.BridgeSessionInitializerAdapter;
import org.kaazing.gateway.transport.IoFilterAdapter;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.nio.TcpExtension;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.future.UnbindFuture;
import org.kaazing.mina.core.service.IoAcceptorEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.MethodExecutionTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NioSocketAcceptorTest {

    private static final String TCP_MAXIMUM_OUTBOUND_RATE = "tcp.maximumOutboundRate";
    private static final String NEXT_PROTOCOL = "nextProtocol";

    @Rule
    public TestRule testExecutionTrace = new MethodExecutionTrace();

    public static final String PROCESSOR_COUNT = TCP_PROCESSOR_COUNT.getPropertyName();

    private static final Logger LOGGER = LoggerFactory.getLogger(NioSocketAcceptorTest.class);

    private NioSocketAcceptor acceptor;
    private SchedulerProvider schedulerProvider;

    @Before
    public void before() throws Exception {
        acceptor = new NioSocketAcceptor(new Properties());
        acceptor.setSchedulerProvider(schedulerProvider = new SchedulerProvider());
        acceptor.setResourceAddressFactory(newResourceAddressFactory());
    }

    @After
    public void after() throws Exception {
        // Make sure we always stop all I/O worker threads
        if (acceptor != null) {
            schedulerProvider.shutdownNow();
            acceptor.dispose();
        }
    }

    @Test
    public void shouldBindAndUnbindLeavingEmptyBindingsMaps() throws Exception {
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();

        Map<String, Object> acceptOptions = new HashMap<>();

        final String connectURIString = "tcp://127.0.0.1:8000";
        final ResourceAddress bindAddress =
                addressFactory.newResourceAddress(
                        connectURIString,
                        acceptOptions);

        final IoHandler ioHandler = new IoHandlerAdapter();

        int[] rounds = new int[]{1,2,10};
        for ( int iterationCount: rounds ) {
            for ( int i = 0; i < iterationCount; i++) {
                System.out.println("accept.bind, i=" + i);
                acceptor.bind(bindAddress, ioHandler, null);
            }
            for (int j = 0; j < iterationCount; j++) {
                UnbindFuture future = acceptor.unbind(bindAddress);
                Assert.assertTrue("Unbind failed", future.await(1, TimeUnit.SECONDS));
            }
            Assert.assertTrue(acceptor.getBindings().isEmpty());

        }
    }

    @Test
    // @Ignore // TODO: this still sometimes fails in a full build
    public void disposeShouldStopWorkerThreads() throws Exception {
        // Check other tests haven't left worker threads still running by not disposing stuff properly
        assertNoWorkerThreads("before test");
        createBindConnectDispose();
        assertNoWorkerThreads("after createBindConnectDispose");
    }

    private void assertNoWorkerThreads(String when) {
        Thread[] threads = new Thread[Thread.activeCount()];
        int enumerated = Thread.enumerate(threads);
        assert enumerated <= threads.length;
        int workersFound = 0;
        int bossesFound = 0;
        System.out.println("List of active threads " + when + ":");
        for(Thread thread: threads) {
            if ( thread != null ) {
                if (thread.getName().matches(".*I/O worker.*")) {
                    workersFound++;
                }
                if (thread.getName().matches(".*boss")) {
                    bossesFound++;
                }
            }
        }
        assertTrue(String.format("No worker or boss threads should be running %s, found %d workers, %d bosses",
                when, workersFound, bossesFound), workersFound == 0 && bossesFound == 0);
    }



    @Test // Test case for KG-5383 (RejectedExecutionException on NioAcceptor reuse on same thread) - fixed in 4.0 only, not 3.x
    public void reusingNioAcceptorShouldWorkWithAlignment() throws Exception {
        createBindConnectDispose();
        createBindConnectDispose();
    }

    @Test
    // Test case for KG-8210 (hang during gateway shutdown due to exception from IoBufferEx.reset if there are pending
    // write requests)
    public void disposeWithUnwrittenDataShouldNotHang() throws Exception {
        createBindConnectDispose(true/*write data to client*/);
    }

    @Test
    public void bindAndAcceptShouldInvokeExtensions() throws Exception {
        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());
        final IoHandler handler = context.mock(IoHandler.class);
        final TcpExtensionFactory extensionFactory = context.mock(TcpExtensionFactory.class, "extensionFactory");
        final CountDownLatch done = new CountDownLatch(1);
        final TcpExtension extension1 = context.mock(TcpExtension.class, "extension1");
        final TcpExtension extension2 = context.mock(TcpExtension.class, "extension2");

        int bindPort = findFreePort();
        String bindURI = format("tcp://localhost:%d", bindPort);
        Map<String, Object> options = new HashMap<>();
        options.put(TCP_MAXIMUM_OUTBOUND_RATE, 0xFFFFFFFEL);
        options.put(NEXT_PROTOCOL, "test-protocol");
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        ResourceAddress bindAddress = addressFactory.newResourceAddress(bindURI, options);

        context.checking(new Expectations() {
            {
                oneOf(extensionFactory).bind(bindAddress);
                will(returnValue(Arrays.asList(extension1, extension2)));
            }
        });

        Properties configuration = new Properties();
        configuration.setProperty("maximum.outbound.rate", "10000");
        acceptor = new NioSocketAcceptor(configuration, extensionFactory);
        acceptor.setResourceAddressFactory(newResourceAddressFactory());

        final IoSession[] sessions = new IoSession[1];
        acceptor.bind(bindAddress, handler, new BridgeSessionInitializer<IoFuture>() {
            @Override
            public void initializeSession(IoSession session, IoFuture future) {
                session.getFilterChain().addLast("test", new IoFilterAdapter<IoSessionEx>(){

                    @Override
                    protected void doSessionOpened(NextFilter nextFilter, IoSessionEx session) throws Exception {
                        sessions[0] = session;
                        done.countDown();
                        super.doSessionOpened(nextFilter, session);
                    }

                });
            }

            @Override
            public BridgeSessionInitializer<IoFuture> getParentInitializer(Protocol protocol) {
                return null;
            }
        });

        context.assertIsSatisfied();

        Sequence order = context.sequence("order");
        context.checking(new Expectations() {
            {
                oneOf(extension1).initializeSession(with(any(IoSession.class))); inSequence(order);
                will(saveParameter("session", 0));
                oneOf(extension2).initializeSession(with(variable("session", IoSession.class))); inSequence(order);
                oneOf(handler).sessionCreated(with(variable("session", IoSession.class))); inSequence(order);
                oneOf(handler).sessionOpened(with(variable("session", IoSession.class))); inSequence(order);
                oneOf(handler).sessionClosed(with(variable("session", IoSession.class))); inSequence(order);
            }
        });

        InetSocketAddress remoteAddress = new InetSocketAddress("localhost", bindPort);
        Socket socket = new Socket();
        socket.connect(remoteAddress);

        try {
            if (!done.await(10,  TimeUnit.SECONDS)) {
                fail("Session not opened in time");
            }
        }
        finally {
            socket.close();

            acceptor.dispose();
        }

        context.assertIsSatisfied();
    }

    @Test
    public void bindAndAcceptShouldWorkWhenThereAreNoExtensions() throws Exception {
        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());
        final IoHandler handler = context.mock(IoHandler.class);
        final TcpExtensionFactory extensionFactory = context.mock(TcpExtensionFactory.class, "extensionFactory");
        final CountDownLatch done = new CountDownLatch(1);

        int bindPort = findFreePort();
        String bindURI = format("tcp://localhost:%d", bindPort);
        Map<String, Object> options = new HashMap<>();
        options.put(TCP_MAXIMUM_OUTBOUND_RATE, 0xFFFFFFFEL);
        options.put(NEXT_PROTOCOL, "test-protocol");
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        ResourceAddress bindAddress = addressFactory.newResourceAddress(bindURI, options);

        context.checking(new Expectations() {
            {
                oneOf(extensionFactory).bind(bindAddress);
                will(returnValue(Collections.emptyList()));
            }
        });

        Properties configuration = new Properties();
        configuration.setProperty("maximum.outbound.rate", "10000");
        acceptor = new NioSocketAcceptor(configuration, extensionFactory);
        acceptor.setResourceAddressFactory(newResourceAddressFactory());

        final IoSession[] sessions = new IoSession[1];
        acceptor.bind(bindAddress, handler, new BridgeSessionInitializer<IoFuture>() {
            @Override
            public void initializeSession(IoSession session, IoFuture future) {
                session.getFilterChain().addLast("test", new IoFilterAdapter<IoSessionEx>(){

                    @Override
                    protected void doSessionOpened(NextFilter nextFilter, IoSessionEx session) throws Exception {
                        sessions[0] = session;
                        done.countDown();
                        super.doSessionOpened(nextFilter, session);
                    }

                });
            }

            @Override
            public BridgeSessionInitializer<IoFuture> getParentInitializer(Protocol protocol) {
                return null;
            }
        });

        context.assertIsSatisfied();

        Sequence order = context.sequence("order");
        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSession.class))); inSequence(order);
                will(saveParameter("session", 0));
                oneOf(handler).sessionOpened(with(variable("session", IoSession.class))); inSequence(order);
                oneOf(handler).sessionClosed(with(variable("session", IoSession.class))); inSequence(order);
            }
        });

        InetSocketAddress remoteAddress = new InetSocketAddress("localhost", bindPort);
        Socket socket = new Socket();
        socket.connect(remoteAddress);

        try {
            if (!done.await(10,  TimeUnit.SECONDS)) {
                fail("Session not opened in time");
            }
        }
        finally {
            socket.close();

            acceptor.dispose();
        }

        context.assertIsSatisfied();
    }

    @Test
    public void initWorkerPoolShouldReturnSamePoolOnSameInstance() throws Exception {
        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());
        final Logger logger = context.mock(Logger.class);

        context.checking(new Expectations() {
            {
                allowing(logger).isDebugEnabled(); will(returnValue(false));
            }
        });

        Properties configuration = new Properties();
        acceptor = new NioSocketAcceptor(configuration);
        WorkerPool<NioWorker> pool1 = null;
        WorkerPool<NioWorker> pool2 = null;
        try {
            pool1 = acceptor.initWorkerPool(logger, "PROCESSOR_COUNT: {}", configuration);
            pool2 = acceptor.initWorkerPool(logger, "PROCESSOR_COUNT: {}", configuration);
            assertSame(pool1, pool2);
        }
        finally {
            ((ExternalResourceReleasable)pool1).releaseExternalResources(); // needed to stop the worker threads
            ((ExternalResourceReleasable)pool2).releaseExternalResources();  // idem
            acceptor.dispose();
        }
        context.assertIsSatisfied();
    }

    @Test
    public void initWorkerPoolShouldReturnDifferentPoolsOnDifferentInstances() throws Exception {
        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());
        final Logger logger = context.mock(Logger.class);

        context.checking(new Expectations() {
            {
                allowing(logger).isDebugEnabled(); will(returnValue(false));
            }
        });

        Properties configuration = new Properties();
        NioSocketAcceptor acceptor1 = new NioSocketAcceptor(configuration);
        NioSocketAcceptor acceptor2 = new NioSocketAcceptor(configuration);
        WorkerPool<NioWorker> pool1 = null;
        WorkerPool<NioWorker> pool2 = null;
        try {
            pool1 = acceptor1.initWorkerPool(logger, "PROCESSOR_COUNT: {}", configuration);
            pool2 = acceptor2.initWorkerPool(logger, "PROCESSOR_COUNT: {}", configuration);
            assertNotSame(pool1, pool2);
        }
        finally {
            ((ExternalResourceReleasable)pool1).releaseExternalResources(); // needed to stop the worker threads
            ((ExternalResourceReleasable)pool2).releaseExternalResources();  // idem
            acceptor1.dispose();
            acceptor2.dispose();
        }
        context.assertIsSatisfied();
    }

    @Test
    public void getWorkerCountShouldDefaultToMachineProcessorCount() throws Exception {
        Properties configuration = new Properties();
        assertEquals(Runtime.getRuntime().availableProcessors(), TCP_PROCESSOR_COUNT.getIntProperty(configuration).intValue());
    }

    @Test
    public void getWorkerCountShouldBeConfiguredProcessorCountIfConfigured() throws Exception {
        Properties configuration = new Properties();
        configuration.setProperty(PROCESSOR_COUNT, "3");
        assertEquals(3, TCP_PROCESSOR_COUNT.getIntProperty(configuration).intValue());
    }

    private void createBindConnectDispose() throws Exception {
        createBindConnectDispose(false);
    }

    private void createBindConnectDispose(final boolean writeData) throws Exception {
        final IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        final CountDownLatch sessionOpened = new CountDownLatch(1);
        final  AtomicReference<IoSessionEx> sessionRef = new AtomicReference<>();

        Properties configuration = new Properties();
        final NioSocketAcceptor acceptor = new NioSocketAcceptor(configuration);

        int bindPort = findFreePort();

        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        acceptor.setResourceAddressFactory(addressFactory);
        String bindURI = format("tcp://localhost:%d", bindPort);
        Map<String,Object> opts = new HashMap<>();
        opts.put(NEXT_PROTOCOL, "test-protocol");

        ResourceAddress bindAddress = addressFactory.newResourceAddress(bindURI, opts);
        acceptor.bind(bindAddress, handler, new BridgeSessionInitializerAdapter<IoFuture>() {
            @Override
            public void initializeSession(IoSession session, IoFuture future) {
                session.getFilterChain().addFirst("test", new IoFilterAdapter<IoSessionEx>(){
                    @Override
                    protected void doSessionOpened(NextFilter nextFilter, IoSessionEx session) throws Exception {
                        sessionRef.set(session);
                        sessionOpened.countDown();
                    }
                    @Override
                    protected void doExceptionCaught(NextFilter nextFilter, IoSessionEx session, Throwable cause)
                            throws Exception {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.warn("Filter chain caught exception.", cause);
                        } else if (LOGGER.isWarnEnabled()) {
                            LOGGER.warn(String.format("Filter chain caught exception: %s", cause.getMessage()));
                        }

                    }
                });
            }
        });

        InetSocketAddress remoteAddress = new InetSocketAddress("localhost", bindPort);
        Socket socket = new Socket();
        socket.connect(remoteAddress);

        assertTrue("Failed to establish connection", sessionOpened.await(5, TimeUnit.SECONDS));

        if (writeData) {
            // KG-8210: push pending write requests onto the write request queue before disposing, should not cause hang in dispose
            int dataSize = 10000;
            ByteBuffer data = ByteBuffer.allocate(dataSize);
            for (int i = 0; i < dataSize; i++) {
                data.put((byte) (i % 100));
            }
            data.flip();
            // Writing 10k 100 times should hopefully actually a TCP buffers and force pending write requests to remain
            // in the session's write request queue
            IoSessionEx session = sessionRef.get();
            System.out.println(String.format("session write request queue length is %s",
                    session.getWriteRequestQueue().isEmpty(session) ? "empty" : "not empty"));
            IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
            for (int i=0; i<100; i++) {
                //IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
                session.write(allocator.wrap(data.duplicate(), IoBufferEx.FLAG_SHARED));
            }
        }

        final CountDownLatch disposed = new CountDownLatch(1);
        new Thread(new Runnable() {

            @Override
            public void run() {
                acceptor.dispose();
                disposed.countDown();
            }

        }).start();

        assertTrue("Failed to dispose in 10 seconds, dispose hanging (KG-8210)", disposed.await(10, TimeUnit.SECONDS));
    }

    @Test
    // If N clients connect where N = number of workers then each client should be assigned to a different worker
    public void allWorkersShouldBeUsed_and_reported_by_getWorkerCount_getWorkers() throws Exception {
        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());
        final IoHandler handler = context.mock(IoHandler.class);
        final RuntimeException[] exception = new RuntimeException[1];
        final Set<Thread> workerThreadsUsed = new ConcurrentHashSet<>();
        final Set<NioWorker> workersUsed = new ConcurrentHashSet<>();
        final int NB_ACCEPTS = 3; // = number of boss threads
        final int NB_WORKERS = 7; // = size of worker pool
        int[] bindPorts = new int[NB_ACCEPTS];

        context.checking(new Expectations() {
            {
                allowing(handler).sessionCreated(with(any(IoSession.class)));
                allowing(handler).sessionOpened(with(any(IoSession.class)));
                allowing(handler).sessionClosed(with(any(IoSession.class)));
            }
        });


        Properties configuration = new Properties();
        configuration.setProperty(PROCESSOR_COUNT, Integer.toString(NB_WORKERS));
        acceptor = new NioSocketAcceptor(configuration);
        acceptor.setResourceAddressFactory(newResourceAddressFactory());
        final CountDownLatch clientsConnected = new CountDownLatch(NB_WORKERS);

        ResourceAddressFactory resourceAddressFactory = ResourceAddressFactory.newResourceAddressFactory();
        for (int i=0; i<NB_ACCEPTS; i++) {
            bindPorts[i] = findFreePort();
            //System.out.println("Binding to " + bindPorts[i]);
            ResourceAddress bindAddress = resourceAddressFactory.newResourceAddress("tcp://localhost:" + bindPorts[i]);
            acceptor.bind(bindAddress, handler, new BridgeSessionInitializer<IoFuture>() {

                @Override
                public void initializeSession(IoSession session, IoFuture future) {
                    session.getFilterChain().addFirst("test", new IoFilterAdapter<IoSessionEx>(){

                        @Override
                        public void doSessionOpened(NextFilter nextFilter, IoSessionEx session) throws Exception {
                                try {
                                    //System.out.println("sessionOpened executing in thread " + Thread.currentThread());
                                    workerThreadsUsed.add(Thread.currentThread());
                                    workersUsed.add(NioSocketAcceptor.CURRENT_WORKER.get());
                                    clientsConnected.countDown();
                                }
                                catch(RuntimeException e) {
                                    exception[0] = e;
                                }
                        }

                    });
                }

                @Override
                public BridgeSessionInitializer<IoFuture> getParentInitializer(Protocol protocol) {
                    return null;
                }
            });
        }
        Socket[] clients = new Socket[NB_WORKERS];
        for (int i=0; i<NB_WORKERS; i++) {
            InetSocketAddress remoteAddress = new InetSocketAddress("localhost", bindPorts[i%NB_ACCEPTS]);
            Socket socket = new Socket();
            socket.setSoLinger(false, 1);
            socket.connect(remoteAddress);
            clients[i] = socket;
        }

        clientsConnected.await(10, TimeUnit.SECONDS);

        assertEquals(NB_WORKERS, workerThreadsUsed.size());
        assertEquals(NB_WORKERS, acceptor.getWorkers().length);
        for (Worker worker : acceptor.getWorkers()) {
            assertTrue(String.format("Worker thread %s reported by getWorkers() was not used", worker),
                    workersUsed.remove(worker));
        }
        assertEquals("getWorkers() did not report all workers used", 0, workersUsed.size());
        assertNull(exception[0]);

        for (Socket socket : clients) {
            socket.close();
        }

        acceptor.dispose();

        context.assertIsSatisfied();
    }

    // When sessionCreated() checks for a binding, it may be null (since a concurrent
    // unbind may have removed it). This test case simulates that behaviour and verifies
    // that session is closed.
    @Test(timeout = 5000L)
    public void unbindDuringConnect() throws Exception {
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        int bindPort = findFreePort();
        String bindURI = format("tcp://localhost:%d", bindPort);
        final ResourceAddress bindAddress = addressFactory.newResourceAddress(bindURI);

        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());

        final IoSessionEx  mockSession = context.mock(IoSessionEx .class);
        final IoFilterChain mockFilterChain = context.mock(IoFilterChain.class);
        // Mocking IoAcceptorEx to get hold of "BridgeAcceptHandler tcpHandler"
        final IoAcceptorEx mockAcceptor = context.mock(IoAcceptorEx.class);
        final IoHandler[] tcpHandlerHolder = new IoHandler[1];
        final TransportMetadata transportMetadata = context.mock(TransportMetadata.class);
        
        context.checking(new Expectations() {
            {
                allowing(mockSession).getLocalAddress(); will(returnValue(bindAddress));
                oneOf(mockSession).close(with(any(boolean.class)));
                allowing(mockSession).getFilterChain(); will(returnValue(mockFilterChain));
                allowing(mockSession).getRemoteAddress(); will(returnValue(bindAddress));
                allowing(mockSession).getService(); will(returnValue(mockAcceptor));

                allowing(mockSession).getTransportMetadata();
                will(returnValue(transportMetadata));
                allowing(mockSession).getSubject();
                allowing(transportMetadata).getAddressType(); will(returnValue(SocketAddress.class));

                allowing(mockFilterChain).addFirst(with(any(String.class)), with(any(IoFilter.class)));
                allowing(mockFilterChain).addLast(with(any(String.class)), with(any(IoFilter.class)));

                allowing(mockAcceptor).setHandler(with(aNonNull(IoHandler.class))); will(saveParameter(tcpHandlerHolder, 0));
                allowing(mockAcceptor).setSessionDataStructureFactory(with(aNonNull(DefaultIoSessionDataStructureFactory.class)));
                allowing(mockAcceptor).bindAsync(with(aNonNull(SocketAddress.class)));
                allowing(mockAcceptor).unbind(with(aNonNull(SocketAddress.class))); 
            }
            public Action saveParameter(final Object[] parameterStorage, final int parameterIndex) {
                return new CustomAction("save parameter") {

                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        parameterStorage[0] = invocation.getParameter(parameterIndex);
                        return null;
                    }
                };
            }
        });

        final TestNioSocketAcceptor acceptor = new TestNioSocketAcceptor(new Properties(), mockAcceptor);
        acceptor.setResourceAddressFactory(addressFactory);
        acceptor.setSchedulerProvider(schedulerProvider);

        acceptor.bind(bindAddress, new IoHandlerAdapter(), new BridgeSessionInitializerAdapter<>());
        UnbindFuture future = acceptor.unbind(bindAddress);
        future.await();
        // Simulating sessionCreated() happens after unbind()
        tcpHandlerHolder[0].sessionCreated(mockSession);

        context.assertIsSatisfied();
    }

    // NioSocketAcceptor is overridden to return mock IoAcceptorEx. That way the test
    // could get hold of BridgeAcceptHandler when IoAcceptorEx.setHandler() is called
    // and can call sessionCreated() on the BridgeAcceptHandler.
    private static class TestNioSocketAcceptor extends NioSocketAcceptor {
        private final IoAcceptorEx ioAcceptorEx;

        public TestNioSocketAcceptor(Properties configuration, IoAcceptorEx ioAcceptorEx) {
            super(configuration);
            this.ioAcceptorEx = ioAcceptorEx;
        }

        @Override
        protected IoAcceptorEx initAcceptor(final IoSessionInitializer<? extends IoFuture> initializer) {
            return ioAcceptorEx;
        }
    }

    /**
     * This method returns a port number that is not currently in use.
     */
    private static int findFreePort() throws IOException {
        ServerSocket server = new ServerSocket(0);
        int port = server.getLocalPort();
        server.close();
        return port;
    }

}
