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
package org.kaazing.gateway.transport.wsn;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.kaazing.gateway.util.Utils.asByteBuffer;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.http.HttpAcceptor;
import org.kaazing.gateway.transport.http.HttpConnector;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.transport.nio.internal.NioSocketConnector;
import org.kaazing.gateway.transport.ws.WsAcceptor;
import org.kaazing.gateway.transport.ws.WsConnector;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBufferAllocator;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtensionFactory;
import org.kaazing.gateway.util.Utils;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.buffer.SimpleBufferAllocator;
import org.kaazing.mina.core.session.IoSessionEx;

public class WsnTransportTest {
    private static final int NETWORK_OPERATION_WAIT_SECS = 10; // was 3, increasing for loaded environments

    @Rule
    public TestRule cain = createRuleChain(30, SECONDS);

    private ResourceAddressFactory addressFactory;

    private NioSocketConnector tcpConnector;
	private HttpConnector httpConnector;
	private WsnConnector  wsnConnector;

	private NioSocketAcceptor tcpAcceptor;
	private HttpAcceptor httpAcceptor;
	private WsnAcceptor wsnAcceptor;
    private WsConnector wsConnector;

//	private Service service;
//	private ServiceContext serviceContext;
//	private ServiceRegistry serviceRegistry;

	@Before
	public void init() {
//        serviceRegistry = new ServiceRegistry();
//		service = new DummyService();
//		serviceContext = new DefaultServiceContext(service.getType(), service);

        SchedulerProvider schedulerProvider = new SchedulerProvider();

		addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        TransportFactory transportFactory = TransportFactory.newTransportFactory(Collections.emptyMap());
        BridgeServiceFactory serviceFactory = new BridgeServiceFactory(transportFactory);

		tcpAcceptor = (NioSocketAcceptor)transportFactory.getTransport("tcp").getAcceptor();

		tcpAcceptor.setResourceAddressFactory(addressFactory);
		tcpAcceptor.setBridgeServiceFactory(serviceFactory);
		tcpAcceptor.setSchedulerProvider(schedulerProvider);

		tcpConnector = (NioSocketConnector)transportFactory.getTransport("tcp").getConnector();
		tcpConnector.setResourceAddressFactory(addressFactory);
		tcpConnector.setBridgeServiceFactory(serviceFactory);
        tcpConnector.setTcpAcceptor(tcpAcceptor);

		httpAcceptor = (HttpAcceptor)transportFactory.getTransport("http").getAcceptor();
		httpAcceptor.setBridgeServiceFactory(serviceFactory);
//		httpAcceptor.setServiceRegistry(serviceRegistry);
        httpAcceptor.setResourceAddressFactory(addressFactory);
        httpAcceptor.setSchedulerProvider(schedulerProvider);

		httpConnector = (HttpConnector)transportFactory.getTransport("http").getConnector();
		httpConnector.setBridgeServiceFactory(serviceFactory);
        httpConnector.setResourceAddressFactory(addressFactory);

		wsnAcceptor = (WsnAcceptor)transportFactory.getTransport("wsn").getAcceptor();
        wsnAcceptor.setBridgeServiceFactory(serviceFactory);
		wsnAcceptor.setResourceAddressFactory(addressFactory);
		wsnAcceptor.setSchedulerProvider(schedulerProvider);
        WsAcceptor wsAcceptor = new WsAcceptor(WebSocketExtensionFactory.newInstance());
        wsnAcceptor.setWsAcceptor(wsAcceptor);

		wsnConnector = (WsnConnector)transportFactory.getTransport("wsn").getConnector();
		wsnConnector.setBridgeServiceFactory(serviceFactory);
        wsnConnector.setResourceAddressFactory(addressFactory);
        wsnConnector.setSchedulerProvider(schedulerProvider);
        wsConnector = (WsConnector) transportFactory.getTransport("ws").getConnector();
        wsnConnector.setWsConnector(wsConnector);
	}

	@After
	public void disposeConnector() {
		if (tcpAcceptor != null) {
			tcpAcceptor.dispose();
		}
		if (httpAcceptor != null) {
			httpAcceptor.dispose();
		}
		if (wsnAcceptor != null) {
			wsnAcceptor.dispose();
		}
		if (tcpConnector != null) {
			tcpConnector.dispose();
		}
		if (httpConnector != null) {
			httpConnector.dispose();
		}
		if (wsnConnector != null) {
			wsnConnector.dispose();
		}
        if (wsConnector != null) {
            wsConnector.dispose();
        }
	}


    @Test
    public void connectorShouldReceiveMessageFromAcceptor() throws Exception {

        String location = "ws://localhost:8000/echo";
        Map<String, Object> addressOptions = Collections.emptyMap(); //Collections.<String, Object>singletonMap("http.transport", URI.create("pipe://internal"));
        ResourceAddress address = addressFactory.newResourceAddress(location, addressOptions);
        final CountDownLatch acceptSessionClosed = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<IoSessionEx>() {

            @Override
            public void doSessionOpened(final IoSessionEx session) throws Exception {
                WsnSession wsnSession = (WsnSession) session;

                ResourceAddress localWsnAddress = wsnSession.getLocalAddress();
                ResourceAddress localHttpAddress = localWsnAddress.getTransport();
                ResourceAddress localTcpAddress = localHttpAddress.getTransport();
                assertEquals(localHttpAddress.getResource(), URI.create("http://localhost:8000/echo"));
                assertEquals(localHttpAddress.getOption(ResourceAddress.NEXT_PROTOCOL), "ws/rfc6455");
                assertTrue(localTcpAddress.getResource().getAuthority().contains("8000"));
                assertEquals(localTcpAddress.getOption(ResourceAddress.NEXT_PROTOCOL), "http/1.1");

                ResourceAddress remoteWsnAddress = wsnSession.getRemoteAddress();
                ResourceAddress remoteHttpAddress = remoteWsnAddress.getTransport();
                ResourceAddress remoteTcpAddress = remoteHttpAddress.getTransport();
                assertEquals(remoteHttpAddress.getResource(), URI.create("http://localhost:8000/echo"));
                assertEquals(remoteHttpAddress.getOption(ResourceAddress.NEXT_PROTOCOL), "ws/rfc6455");
                assertFalse(remoteTcpAddress.getResource().getAuthority().contains("8000"));
                assertEquals(remoteTcpAddress.getOption(ResourceAddress.NEXT_PROTOCOL), "http/1.1");

                // send a message
                session.write(SimpleBufferAllocator.BUFFER_ALLOCATOR.wrap(asByteBuffer("Message from acceptor")))
                        .addListener(new IoFutureListener<IoFuture>() {
                            @Override
                            public void operationComplete(IoFuture future) {
                                session.close(false);
                            }
                        });
            }

            @Override
            public void doSessionClosed(IoSessionEx session) throws Exception {
                acceptSessionClosed.countDown();
            }

        };
        wsnAcceptor.bind(address, acceptHandler, null);
//        serviceRegistry.register(location, serviceContext);

        final CountDownLatch messageReceived = new CountDownLatch(1);
        IoHandler connectHandler = new IoHandlerAdapter<IoSessionEx>() {

            @Override
            public void doSessionOpened(IoSessionEx session) throws Exception {
                //session.write(wrap("Hello, world".getBytes()));
            }

            @Override
            public void doMessageReceived(IoSessionEx session, Object message) throws Exception {
                WsnSession wsnSession = (WsnSession) session;

                ResourceAddress localWsnAddress = wsnSession.getLocalAddress();
                ResourceAddress localHttpAddress = localWsnAddress.getTransport();
                ResourceAddress localTcpAddress = localHttpAddress.getTransport();
                assertEquals(localHttpAddress.getResource(), URI.create("http://localhost:8000/echo"));
                assertEquals(localHttpAddress.getOption(ResourceAddress.NEXT_PROTOCOL), "ws/rfc6455");
                assertFalse(localTcpAddress.getResource().getAuthority().contains("8000"));
                assertEquals(localTcpAddress.getOption(ResourceAddress.NEXT_PROTOCOL), "http/1.1");

                ResourceAddress remoteWsnAddress = wsnSession.getRemoteAddress();
                ResourceAddress remoteHttpAddress = remoteWsnAddress.getTransport();
                ResourceAddress remoteTcpAddress = remoteHttpAddress.getTransport();
                assertEquals(remoteHttpAddress.getResource(), URI.create("http://localhost:8000/echo"));
                assertEquals(remoteHttpAddress.getOption(ResourceAddress.NEXT_PROTOCOL), "ws/rfc6455");
                assertTrue(remoteTcpAddress.getResource().getAuthority().contains("8000"));
                assertEquals(remoteTcpAddress.getOption(ResourceAddress.NEXT_PROTOCOL), "http/1.1");

                messageReceived.countDown();
            }

        };

        ConnectFuture connectFuture = wsnConnector.connect(address, connectHandler, null);
        final WsnSession session = (WsnSession)connectFuture.await().getSession();
        waitForLatch(messageReceived, NETWORK_OPERATION_WAIT_SECS, TimeUnit.SECONDS,
                "message from acceptor not received");
        session.close(false);
        // TODO: fix bridge acceptor to propagate sessionClosed to the child session instead of
        //       of calling childSession.close, and uncomment the next statement.
//        waitForLatch(acceptSessionClosed, NETWORK_OPERATION_WAIT_SECS, TimeUnit.SECONDS,
//                "sessionClosed did not fire on the acceptor");
    }

    @Test
    public void connectorShouldWriteAndReceiveMessage() throws Exception {

        String location = "wsn://localhost:8000/echo";
        Map<String, Object> addressOptions = Collections.emptyMap(); // Collections.<String,
                                                                                      // Object>singletonMap("http.transport",
                                                                                      // URI.create("pipe://internal"));
        ResourceAddress address = addressFactory.newResourceAddress(location, addressOptions);
        final CountDownLatch acceptSessionClosed = new CountDownLatch(1);
        IoHandler acceptHandler = new IoHandlerAdapter<IoSessionEx>() {
            @Override
            public void doMessageReceived(IoSessionEx session, Object message) throws Exception {
                // echo message
                IoBufferEx buf = (IoBufferEx) message;
                IoSessionEx sessionEx = session;
                System.out.println("Acceptor: received message: " + Utils.asString(buf.buf()));
                IoBufferAllocatorEx<?> allocator = sessionEx.getBufferAllocator();
                session.write(allocator.wrap(asByteBuffer("Reply from acceptor")));
            }

            @Override
            public void doSessionClosed(IoSessionEx session) throws Exception {
                acceptSessionClosed.countDown();
            }
        };
        wsnAcceptor.bind(address, acceptHandler, null);
        // serviceRegistry.register(location, serviceContext);

        final CountDownLatch echoReceived = new CountDownLatch(1);
        IoHandler connectHandler = new IoHandlerAdapter<IoSessionEx>() {

            @Override
            public void doSessionOpened(IoSessionEx session) throws Exception {
                // session.write(wrap("Hello, world".getBytes()));
            }

            @Override
            public void doMessageReceived(IoSessionEx session, Object message) throws Exception {
                echoReceived.countDown();
            }

        };

        ConnectFuture connectFuture = wsnConnector.connect(address, connectHandler, null);
        final WsnSession session = (WsnSession) connectFuture.await().getSession();
        session.write(new WsBufferAllocator(SimpleBufferAllocator.BUFFER_ALLOCATOR).wrap(Utils
                .asByteBuffer("Message from connector")));
        waitForLatch(echoReceived, NETWORK_OPERATION_WAIT_SECS, TimeUnit.SECONDS, "echo not received");
        session.close(true);
        // TODO: fix bridge acceptor to propagate sessionClosed to the child session instead of
        // of calling childSession.close, and uncomment the next statement.
        // waitForLatch(acceptSessionClosed, NETWORK_OPERATION_WAIT_SECS, TimeUnit.SECONDS,
        // "sessionClosed did not fire on the acceptor");
    }

    private static void waitForLatch(CountDownLatch l,
                                    final int delay,
                                    final TimeUnit unit,
                                    final String failureMessage)
            throws InterruptedException {

        l.await(delay, unit);
        if ( l.getCount() != 0 ) {
            fail(failureMessage);
        }
    }
}
