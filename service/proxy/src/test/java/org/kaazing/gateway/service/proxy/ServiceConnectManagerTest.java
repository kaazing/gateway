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
package org.kaazing.gateway.service.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.future.DefaultConnectFuture;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jboss.netty.channel.socket.Worker;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.Transport;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.slf4j.Logger;

public class ServiceConnectManagerTest {

    @SuppressWarnings("unchecked")
    @Test
    public void getConnectFutureShouldReturnPreparedConnectionFromThreadSpecificPool() throws Exception {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        context.setThreadingPolicy(new Synchroniser());
        final AbstractProxyHandler handler = context.mock(AbstractProxyHandler.class);
        final ServiceContext service = context.mock(ServiceContext.class);
        final Logger logger = context.mock(Logger.class);
        final BridgeServiceFactory bridgeServiceFactory = context.mock(BridgeServiceFactory.class);
        final TransportFactory transportFactory = context.mock(TransportFactory.class);
        final Transport transport = context.mock(Transport.class);
        final NioSocketAcceptor acceptor = context.mock(NioSocketAcceptor.class);
        final int PREPARED_CONNECTION_COUNT = 7;
        final int IO_THREADS = 3;
        final Worker[] workers = new Worker[IO_THREADS];
        for (int i=0; i<IO_THREADS; i++) {
            workers[i] = context.mock(Worker.class, "worker" + (i+1));
        }
        // The following executors represent the worker threads
        final CompletionService<Throwable>[] executors = new CompletionService[IO_THREADS];
        for (int i = 0; i < IO_THREADS; i++) {
            executors[i] = new ExecutorCompletionService<>(Executors.newFixedThreadPool(1)); // single thread
        }

        final int maxConnectionsPerThread = PREPARED_CONNECTION_COUNT/IO_THREADS + 1;
        final String CONNECT_URI = "http://localhost:8051";
        final Object threadAttributeKey = new Object();

        context.checking(new Expectations() {
            {
                allowing(service).getLogger(); will(returnValue(logger));
                allowing(service).getSchedulerProvider(); will(returnValue(null));
                allowing(service).getProcessorCount(); will(returnValue(IO_THREADS));
                allowing(logger).isDebugEnabled(); will(returnValue(false));
                allowing(logger).isWarnEnabled(); will(returnValue(false));
                allowing(logger).isTraceEnabled(); will(returnValue(false));
                allowing(bridgeServiceFactory).getTransportFactory(); will(returnValue(transportFactory));
                allowing(transportFactory).getTransport("tcp"); will(returnValue(transport));
                allowing(transport).getAcceptor(); will(returnValue(acceptor));
                allowing(acceptor).getWorkers(); will(returnValue(workers));
                oneOf(workers[0]).executeInIoThread(with(any(Runnable.class))); will(new ExecuteRunnable(executors[0]));
                oneOf(workers[1]).executeInIoThread(with(any(Runnable.class))); will(new ExecuteRunnable(executors[1]));
                oneOf(workers[2]).executeInIoThread(with(any(Runnable.class))); will(new ExecuteRunnable(executors[2]));
                allowing(service).connect(with(CONNECT_URI), with(handler), with(any(IoSessionInitializer.class)));
                will(new CustomAction("return next future (fulfilled)") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        DefaultConnectFuture future = new DefaultConnectFuture();
                        future.setSession(new DummySession());  // fulfills the connect future
                        future.getSession().setAttribute(threadAttributeKey, Thread.currentThread());
                        return future;
                    }
                });
            }
        });

        final ServiceConnectManager manager = new ServiceConnectManager(service, handler, bridgeServiceFactory, CONNECT_URI,
                0 /*interval*/, PREPARED_CONNECTION_COUNT /*prepared connections*/);
        manager.start();

        // Take the results of "set connection pool on each worker thread" done during start()
        String errors = null;
        for (int i = 0; i < IO_THREADS; i++) {
            Throwable result =  executors[i].take().get();
            if (result != null) {
                System.err.println(String.format("Task %s result %s", i, result));
                errors = errors == null ? result.toString() : errors + ", " + result;
            }
        }
        assertNull("Errors were detected while setting connection pool on each worker thread: " + errors, errors);

        // The following simulates an I/O thread with client connections coming in
        Callable<Throwable> task = new Callable<Throwable>() {
            @Override
            public Throwable call() throws Exception {
                // getNextConnectFuture should give us a connection that was fulfilled on this thread.
                Throwable result = null;
                for (int i = 0; i < maxConnectionsPerThread-1; i++) {
                    // System.out.println(String.format("Thread %s calling getNextConnectFuture", Thread.currentThread()));
                    DefaultConnectFuture future = (DefaultConnectFuture)manager.getNextConnectFuture(null);
                    try {
                        assertTrue("pre-connected future should be connected", future.isConnected());
                        assertSame("pre-connected future should have connected on the same thread",
                            future.getSession().getAttribute(threadAttributeKey), Thread.currentThread());
                    }
                    catch(Throwable t) {
                        t.printStackTrace();
                        result = t;
                        break;
                    }
                }
                return result;
            }
        };
        for (int i = 0; i < IO_THREADS; i++) {
            executors[i].submit(task);
        }
        for (int i = 0; i < IO_THREADS; i++) {
            Throwable result =  executors[i].take().get();
            if (result != null) {
                System.err.println(String.format("Task %s result %s", i, result));
                errors = errors == null ? result.toString() : errors + ", " + result;
            }
        }
        assertNull("Errors were detected: " + errors, errors);

        context.assertIsSatisfied();
    }

    @Test
    public void preparedConnectionCountShouldBeAutomaticallySetToAtLeastNumberOfIoThreads() throws Exception {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        final AbstractProxyHandler handler = context.mock(AbstractProxyHandler.class);
        final ServiceContext service = context.mock(ServiceContext.class);
        final Logger logger = context.mock(Logger.class);
        final BridgeServiceFactory bridgeServiceFactory = context.mock(BridgeServiceFactory.class);
        final TransportFactory transportFactory = context.mock(TransportFactory.class);
        final Transport transport = context.mock(Transport.class);
        final NioSocketAcceptor acceptor = context.mock(NioSocketAcceptor.class);
        final String CONNECT_URI = "http://localhost:8051";
        final int PREPARED_CONNECTION_COUNT = 2; // LT IO_THREADS
        final int IO_THREADS = 3;

        context.checking(new Expectations() {
            {
                allowing(service).getLogger(); will(returnValue(logger));
                allowing(service).getProcessorCount(); will(returnValue(IO_THREADS));
                allowing(logger).isDebugEnabled(); will(returnValue(true));
                allowing(service).getSchedulerProvider(); will(returnValue(null));
                allowing(service).getServiceType(); will(returnValue("proxy"));
                oneOf(logger).debug(with(any(String.class)));
                allowing(logger).isWarnEnabled(); will(returnValue(true));
                oneOf(logger).warn(with(any(String.class)));
                allowing(logger).isTraceEnabled(); will(returnValue(false));
                allowing(bridgeServiceFactory).getTransportFactory(); will(returnValue(transportFactory));
                allowing(transportFactory).getTransport("tcp"); will(returnValue(transport));
                allowing(transport).getAcceptor(); will(returnValue(acceptor));
            }
        });

        final ServiceConnectManager manager = new ServiceConnectManager(service, handler, bridgeServiceFactory, CONNECT_URI,
                0 /*interval*/, PREPARED_CONNECTION_COUNT /*prepared connections*/);
        assertEquals(IO_THREADS, manager.getPreparedConnectionCount());

        context.assertIsSatisfied();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getConnectFutureShouldReturnNonNullWhenCalledFromNonIOThread() throws Exception {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        context.setThreadingPolicy(new Synchroniser());
        final AbstractProxyHandler handler = context.mock(AbstractProxyHandler.class);
        final ServiceContext service = context.mock(ServiceContext.class);
        final Logger logger = context.mock(Logger.class);
        final BridgeServiceFactory bridgeServiceFactory = context.mock(BridgeServiceFactory.class);
        final TransportFactory transportFactory = context.mock(TransportFactory.class);
        final Transport transport = context.mock(Transport.class);
        final NioSocketAcceptor acceptor = context.mock(NioSocketAcceptor.class);
        final Worker worker = context.mock(Worker.class, "worker");

        // The following executors represent the worker threads
        final CompletionService<Throwable> executor = new ExecutorCompletionService<>(Executors.newFixedThreadPool(1)); // single thread

        final String CONNECT_URI = "http://localhost:8051";
        final Object threadAttributeKey = new Object();

        context.checking(new Expectations() {
            {
                allowing(service).getLogger(); will(returnValue(logger));
                allowing(service).getSchedulerProvider(); will(returnValue(null));
                allowing(service).getProcessorCount(); will(returnValue(1));
                allowing(logger).isDebugEnabled(); will(returnValue(false));
                allowing(logger).isWarnEnabled(); will(returnValue(false));
                allowing(logger).isTraceEnabled(); will(returnValue(false));
                allowing(bridgeServiceFactory).getTransportFactory(); will(returnValue(transportFactory));
                allowing(transportFactory).getTransport("tcp"); will(returnValue(transport));
                allowing(transport).getAcceptor(); will(returnValue(acceptor));
                allowing(acceptor).getWorkers(); will(returnValue(new Worker[] { worker }));
                allowing(worker).executeInIoThread(with(any(Runnable.class))); will(new ExecuteRunnable(executor));
                allowing(service).connect(with(CONNECT_URI), with(handler), with(any(IoSessionInitializer.class)));
                will(new CustomAction("return next future (fulfilled)") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        DefaultConnectFuture future = new DefaultConnectFuture();
                        future.setSession(new DummySession());  // fulfills the connect future
                        future.getSession().setAttribute(threadAttributeKey, Thread.currentThread());
                        return future;
                    }
                });
            }
        });

        final ServiceConnectManager manager = new ServiceConnectManager(service, handler, bridgeServiceFactory, CONNECT_URI,
                0 /*interval*/, 0 /*prepared connections*/);
        manager.start();

        // Take the results of "set connection pool on each worker thread" done during start()
        {
            String errors = null;
            Throwable result = executor.take().get();
            if (result != null) {
                System.err.println(String.format("Task result %s", result));
                errors = errors == null ? result.toString() : errors + ", " + result;
            }

            assertNull("Errors were detected while setting connection pool on each worker thread: " + errors, errors);
        }

        // The following simulates a client connection coming in on a non-I/O thread
        {
            DefaultConnectFuture future = (DefaultConnectFuture)manager.getNextConnectFuture(null);
            try {
                assertTrue("pre-connected future should be connected", future.isConnected());
            } catch(Throwable t) {
                t.printStackTrace();
                fail("unexpected exception getting next connect future " + t);
            }
        }

        {
            String errors = null;
            Throwable backendResult =  executor.take().get();
            if (backendResult != null) {
                System.err.println(String.format("Task result %s", backendResult));
                errors = errors == null ? backendResult.toString() : errors + ", " + backendResult;
            }

            assertNull("Errors were detected: " + errors, errors);
        }

        context.assertIsSatisfied();
    }

    @Test @SuppressWarnings("rawtypes")
    public void getConnectFutureShouldReturnNonNullAfterPreviousSessionClose() throws Exception {
        // This test is related to KG-13851:  connection leak with prepared connection count and is
        // designed to 
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        context.setThreadingPolicy(new Synchroniser());
        final AbstractProxyHandler handler = context.mock(AbstractProxyHandler.class);
        final ServiceContext service = context.mock(ServiceContext.class);
        final Logger logger = context.mock(Logger.class);
        final BridgeServiceFactory bridgeServiceFactory = context.mock(BridgeServiceFactory.class);
        final TransportFactory transportFactory = context.mock(TransportFactory.class);
        final Transport transport = context.mock(Transport.class);
        final NioSocketAcceptor acceptor = context.mock(NioSocketAcceptor.class);
        final Worker worker = context.mock(Worker.class, "worker");

        // The following executors represent the worker threads
        final CompletionService<Throwable> executor = new ExecutorCompletionService<>(Executors.newFixedThreadPool(1)); // single thread

        final String CONNECT_URI = "http://localhost:8051";
        final Object threadAttributeKey = new Object();
        final AtomicInteger connectionCount = new AtomicInteger(0);
        final SchedulerProvider schedulerProvider = new SchedulerProvider();

        context.checking(new Expectations() {
            {
                allowing(service).getLogger(); will(returnValue(logger));
                allowing(service).getSchedulerProvider(); will(returnValue(schedulerProvider));
                allowing(service).getProcessorCount(); will(returnValue(1));
                allowing(logger).isDebugEnabled(); will(returnValue(false));
                allowing(logger).isWarnEnabled(); will(returnValue(false));
                allowing(logger).isTraceEnabled(); will(returnValue(false));
                allowing(bridgeServiceFactory).getTransportFactory(); will(returnValue(transportFactory));
                allowing(transportFactory).getTransport("tcp"); will(returnValue(transport));
                allowing(transport).getAcceptor(); will(returnValue(acceptor));
                allowing(acceptor).getWorkers(); will(returnValue(new Worker[] { worker }));
                allowing(worker).executeInIoThread(with(any(Runnable.class))); will(new ExecuteRunnable(executor));
                allowing(service).connect(with(CONNECT_URI), with(handler), with(any(IoSessionInitializer.class)));
                will(new CustomAction("return next future (fulfilled)") {
                    @Override @SuppressWarnings("unchecked")
                    public Object invoke(Invocation invocation) throws Throwable {
                        DefaultConnectFuture future = new DefaultConnectFuture();
                        future.setSession(new DummySession());  // fulfills the connect future
                        future.getSession().setAttribute(threadAttributeKey, Thread.currentThread());

                        // increment connection count
                        connectionCount.incrementAndGet();

                        // get the session initializer, and if non-null then invoke it
                        IoSessionInitializer initializer = (IoSessionInitializer)invocation.getParameter(2);
                        if (initializer != null) {
                            initializer.initializeSession(future.getSession(), future);
                        }

                        return future;
                    }
                });
            }
        });

        final ServiceConnectManager manager = new ServiceConnectManager(service, handler, bridgeServiceFactory, CONNECT_URI,
                0 /*interval*/, 1 /*prepared connections*/);
        manager.start();

        // Take the results of "set connection pool on each worker thread" done during start()
        {
            String errors = null;
            Throwable result = executor.take().get();
            if (result != null) {
                System.err.println(String.format("Task result %s", result));
                errors = errors == null ? result.toString() : errors + ", " + result;
            }

            assertNull("Errors were detected while setting connection pool on each worker thread: " + errors, errors);
        }

        // The following simulates a client connection coming in on a non-I/O thread
        {
            DefaultConnectFuture future = (DefaultConnectFuture)manager.getNextConnectFuture(null);
            try {
                assertTrue("pre-connected future should be connected", future.isConnected());

                // now close the session
                future.getSession().close(false);
            } catch(Throwable t) {
                t.printStackTrace();
                fail("unexpected exception getting next connect future " + t);
            }
        }

        // Simulate a client connection coming in on a non-I/O thread
        {
            DefaultConnectFuture future = (DefaultConnectFuture)manager.getNextConnectFuture(null);
            try {
                assertTrue("second pre-connected future should be connected", future.isConnected());
            } catch(Throwable t) {
                t.printStackTrace();
                fail("unexpected exception getting next connect future " + t);
            }
        }

        {
            String errors = null;
            Throwable backendResult =  executor.take().get();
            if (backendResult != null) {
                System.err.println(String.format("Task result %s", backendResult));
                errors = errors == null ? backendResult.toString() : errors + ", " + backendResult;
            }

            assertNull("Errors were detected: " + errors, errors);
        }

        assertEquals("Should have two consumed connections and one remaining preconnect, but total connections is " + connectionCount.intValue(),
                connectionCount.intValue(), 3);

        context.assertIsSatisfied();
    }

    private static class ExecuteRunnable extends CustomAction {
        private final CompletionService<Throwable> completionService;

        public ExecuteRunnable(CompletionService<Throwable> completionService) {
            super("run");
            this.completionService = completionService;
        }

        @Override
        public Object invoke(Invocation invocation) throws Throwable {
            completionService.submit((Runnable)invocation.getParameter(0), null);
            return null;
        }
    }

}
