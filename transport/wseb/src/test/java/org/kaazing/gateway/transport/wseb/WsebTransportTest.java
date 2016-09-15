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
package org.kaazing.gateway.transport.wseb;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.fail;
import static org.kaazing.gateway.util.Utils.asByteBuffer;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.http.HttpAcceptor;
import org.kaazing.gateway.transport.http.HttpConnector;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.transport.nio.internal.NioSocketConnector;
import org.kaazing.gateway.transport.ws.WsAcceptor;
import org.kaazing.gateway.transport.ws.WsConnector;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtensionFactory;
import org.kaazing.gateway.transport.wseb.filter.WsebBufferAllocator;
import org.kaazing.gateway.util.Utils;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.SimpleBufferAllocator;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.MethodExecutionTrace;

public class WsebTransportTest {
	private static final int NETWORK_OPERATION_WAIT_SECS = 10; // was 3, increasing for loaded environments

	@Rule
    public final TestRule testExecutionTrace = new MethodExecutionTrace("log4j-trace.properties");

    @Rule
    public final TestRule timeout = new DisableOnDebug(new Timeout(20, SECONDS));

	private ResourceAddressFactory addressFactory;

	private NioSocketConnector tcpConnector;
	private HttpConnector httpConnector;
	private WsebConnector wsebConnector;

	private NioSocketAcceptor tcpAcceptor;
	private HttpAcceptor httpAcceptor;
	private WsebAcceptor wsebAcceptor;
	private WsConnector wsConnector;

	@Before
	public void init() {
		SchedulerProvider schedulerProvider = new SchedulerProvider();

		addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        Map<String, ?> config = Collections.emptyMap();
        TransportFactory transportFactory = TransportFactory.newTransportFactory(config);
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
        httpAcceptor.setResourceAddressFactory(addressFactory);
        httpAcceptor.setSchedulerProvider(schedulerProvider);

		httpConnector = (HttpConnector)transportFactory.getTransport("http").getConnector();
		httpConnector.setBridgeServiceFactory(serviceFactory);
        httpConnector.setResourceAddressFactory(addressFactory);

		wsebAcceptor = new WsebAcceptor();
        wsebAcceptor.setBridgeServiceFactory(serviceFactory);
		wsebAcceptor.setResourceAddressFactory(addressFactory);
		wsebAcceptor.setSchedulerProvider(schedulerProvider);
		wsebAcceptor.setConfiguration(new Properties());
		WsAcceptor wsAcceptor = new WsAcceptor(WebSocketExtensionFactory.newInstance());
		wsebAcceptor.setWsAcceptor(wsAcceptor);

		wsConnector = (WsConnector)transportFactory.getTransport("ws").getConnector();

		wsebConnector = new WsebConnector();
		wsebConnector.setBridgeServiceFactory(serviceFactory);
        wsebConnector.setResourceAddressFactory(addressFactory);
        wsebConnector.setConfiguration(new Properties());
		wsebConnector.setWsConnector(wsConnector);
	}

	@After
	public void disposeConnector() {
		if (tcpAcceptor != null) {
			tcpAcceptor.dispose();
		}
		if (httpAcceptor != null) {
			httpAcceptor.dispose();
		}
		if (wsebAcceptor != null) {
			wsebAcceptor.dispose();
		}
		if (tcpConnector != null) {
			tcpConnector.dispose();
		}
		if (httpConnector != null) {
			httpConnector.dispose();
		}
		if (wsebConnector != null) {
			wsebConnector.dispose();
		}
		if (wsConnector != null) {
			wsConnector.dispose();
		}
	}

    @Test
    public void connectorShouldReceiveMessageFromAcceptor() throws Exception {

        String location = "wse://localhost:8000/echo";
        Map<String, Object> addressOptions = Collections.emptyMap(); //Collections.<String, Object>singletonMap("http.transport", URI.create("pipe://internal"));
        ResourceAddress address = addressFactory.newResourceAddress(location, addressOptions);
        final CountDownLatch acceptSessionClosed = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter() {

            @Override
            public void sessionOpened(final IoSession session) throws Exception {
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
            public void sessionClosed(IoSession session) throws Exception {
                acceptSessionClosed.countDown();
            }

        };
        wsebAcceptor.bind(address, acceptHandler, null);

        final CountDownLatch messageReceived = new CountDownLatch(1);
        IoHandler connectHandler = new IoHandlerAdapter() {

            @Override
            public void sessionOpened(IoSession session) throws Exception {
                //session.write(wrap("Hello, world".getBytes()));
            }

            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                messageReceived.countDown();
            }

        };

        ConnectFuture connectFuture = wsebConnector.connect(address, connectHandler, null);
        if (!connectFuture.await(NETWORK_OPERATION_WAIT_SECS * 1000L)) {
            fail(String.format("Connection did not complete within %d seconds", NETWORK_OPERATION_WAIT_SECS));
        }
        final WsebSession session = (WsebSession)connectFuture.getSession();
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

		String location = "wse://localhost:8000/echo";
		Map<String, Object> addressOptions = Collections.emptyMap(); //Collections.<String, Object>singletonMap("http.transport", URI.create("pipe://internal"));
		ResourceAddress address = addressFactory.newResourceAddress(location, addressOptions);
		final CountDownLatch acceptSessionClosed = new CountDownLatch(1);
		IoHandler acceptHandler = new IoHandlerAdapter() {
			@Override
			public void messageReceived(IoSession session, Object message)
					throws Exception {
				// echo message
				IoBuffer buf = (IoBuffer) message;
				IoSessionEx sessionEx = (IoSessionEx) session;
				System.out.println("Acceptor: received message: " + Utils.asString(buf.buf()));
				IoBufferAllocatorEx<?> allocator = sessionEx.getBufferAllocator();
                session.write(allocator.wrap(asByteBuffer("Reply from acceptor")));
			}

			@Override
            public void sessionClosed(IoSession session) throws Exception {
			    acceptSessionClosed.countDown();
            }
		};
		wsebAcceptor.bind(address, acceptHandler, null);

		final CountDownLatch echoReceived = new CountDownLatch(1);
		IoHandler connectHandler = new IoHandlerAdapter() {

			@Override
			public void sessionOpened(IoSession session) throws Exception {
				//session.write(wrap("Hello, world".getBytes()));
			}

			@Override
			public void messageReceived(IoSession session, Object message) throws Exception {
			    echoReceived.countDown();
			}

		};

		ConnectFuture connectFuture = wsebConnector.connect(address, connectHandler, null);
		if (!connectFuture.await(NETWORK_OPERATION_WAIT_SECS * 1000L)) {
		    fail(String.format("Connection did not complete within %d seconds", NETWORK_OPERATION_WAIT_SECS));
		}
		final WsebSession session = (WsebSession)connectFuture.getSession();
		session.write(new WsebBufferAllocator(SimpleBufferAllocator.BUFFER_ALLOCATOR).wrap(Utils.asByteBuffer("Message from connector")));
		waitForLatch(echoReceived, NETWORK_OPERATION_WAIT_SECS, TimeUnit.SECONDS,
		        "echo not received");
		session.close(true);
        // TODO: fix bridge acceptor to propagate sessionClosed to the child session instead of
        //       of calling childSession.close, and uncomment the next statement.
//        waitForLatch(acceptSessionClosed, NETWORK_OPERATION_WAIT_SECS, TimeUnit.SECONDS,
//                "sessionClosed did not fire on the acceptor");
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
