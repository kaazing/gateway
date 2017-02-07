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
package org.kaazing.gateway.transport.ws;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.transport.ws.util.TestUtil.dispose;
import static org.kaazing.gateway.util.InternalSystemProperty.WS_ENABLED_TRANSPORTS;

import java.util.Collections;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.PropertyConfigurator;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.session.IoSession;
import org.jmock.Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.BridgeSessionInitializer;
import org.kaazing.gateway.transport.BridgeSessionInitializerAdapter;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.http.HttpAcceptor;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtensionFactory;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.future.UnbindFuture;
import org.kaazing.test.util.MethodExecutionTrace;

public class WsAcceptorTest {
    @Rule
    public final TestRule testExecutionTrace = new MethodExecutionTrace();

    private static final boolean DEBUG = false;

    private ResourceAddressFactory resourceAddressFactory;


    @After
    public void tearDown() throws Exception {
        // For reasons unknown, tcpAcceptor.unbind does not actually free up the bound port until dispose is called.
        // This causes the next test method to fail to bind.
        dispose(wsnAcceptor, wsebAcceptor, httpAcceptor, tcpAcceptor);
        schedulerProvider.shutdownNow();
    }

    @BeforeClass
    public static void init() throws Exception {
        if (DEBUG) {
            PropertyConfigurator.configure("src/test/resources/log4j-trace.properties");
        }
    }

    private static final String PROPERTY_NAME = WS_ENABLED_TRANSPORTS.getPropertyName();
    @After
    public void after() {
        System.clearProperty(PROPERTY_NAME);
    }

    private NioSocketAcceptor tcpAcceptor;
    private HttpAcceptor httpAcceptor;
    private WsebAcceptorMock wsebAcceptor = new WsebAcceptorMock();
    private WsnAcceptorMock wsnAcceptor = new WsnAcceptorMock();
    private WsAcceptor wsAcceptor;
    private SchedulerProvider schedulerProvider = new SchedulerProvider();

    @Before
    public void setup() {
        resourceAddressFactory = ResourceAddressFactory.newResourceAddressFactory();
        TransportFactory transportFactory = TransportFactory.newTransportFactory(Collections.EMPTY_MAP);
        BridgeServiceFactory bridgeServiceFactory = new BridgeServiceFactory(transportFactory);
        tcpAcceptor = (NioSocketAcceptor)transportFactory.getTransport("tcp").getAcceptor();
        httpAcceptor = (HttpAcceptor)transportFactory.getTransport("http").getAcceptor();
        wsAcceptor = (WsAcceptor)transportFactory.getTransport("ws").getAcceptor();
        schedulerProvider = new SchedulerProvider();


        tcpAcceptor.setResourceAddressFactory(resourceAddressFactory);
        tcpAcceptor.setSchedulerProvider(schedulerProvider);

        httpAcceptor = (HttpAcceptor)transportFactory.getTransport("http").getAcceptor();
        httpAcceptor.setBridgeServiceFactory(bridgeServiceFactory);
//        httpAcceptor.setServiceRegistry(new ServiceRegistry());
        httpAcceptor.setResourceAddressFactory(resourceAddressFactory);
        httpAcceptor.setSchedulerProvider(schedulerProvider);

        wsAcceptor.setWsnAcceptor(wsnAcceptor);
        wsAcceptor.setWsebAcceptor(wsebAcceptor);

        wsAcceptor.setConfiguration(new Properties());

    }

    @Test
    public void shouldBindAndUnbindOnWsAddress()
            throws Exception {

        String uri = "ws://localhost:9000/FOO";
        ResourceAddress address = resourceAddressFactory.newResourceAddress(uri);

        final IoHandlerAdapter acceptHandler = new IoHandlerAdapter() {
        };

        wsAcceptor.bind(address, acceptHandler, null);

        UnbindFuture unbindFuture = wsAcceptor.unbind(address);
        unbindFuture.await(3, SECONDS);

        assertTrue(unbindFuture.isDone());
        assertTrue(unbindFuture.isUnbound());
    }


    @Test
    public void testShouldBindAllTransportsByDefault() throws Exception {
        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());
        context.setImposteriser(ClassImposteriser.INSTANCE);
        performMockWsAcceptorBind(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testShouldThrowIllegalArgumentExceptionWhenInvalidTransportsSpecified() throws Exception {
        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());
        context.setImposteriser(ClassImposteriser.INSTANCE);
        performMockWsAcceptorBind("sse");
    }

    @Test
    public void testShouldBindWsnOnly() throws Exception {
        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());
        context.setImposteriser(ClassImposteriser.INSTANCE);
        performMockWsAcceptorBind("wsn");
    }

    @Test
    public void testShouldBindWsebOnly() throws Exception {
        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());
        context.setImposteriser(ClassImposteriser.INSTANCE);
        performMockWsAcceptorBind("wseb");
    }


    @Test
     public void testShouldBindWsnWsebOnly() throws Exception {
        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());
        context.setImposteriser(ClassImposteriser.INSTANCE);
        performMockWsAcceptorBind("wsn,wseb");
    }


    private void performMockWsAcceptorBind(String enabledTransportString) {

        Properties configuration = new Properties();
        if ( enabledTransportString != null ) {
            configuration.setProperty(PROPERTY_NAME, enabledTransportString);
        }

        // Make our own mocks since JMock class imposteriser does not call constructors,
        // and the AbstractBridgeAcceptor's final bind method relies on the constructor to set "started".
        // To work around this, and because we don't care about thread safety here,
        // we develop Acceptor mock classes whose constructors initialize the class correctly and set started correctly.

        final WsnAcceptorMock wsnAcceptor = new WsnAcceptorMock();

        final WsebAcceptorMock wsebAcceptor = new WsebAcceptorMock();

        // Set up an address to bind on

        String uri = "ws://localhost:9000/FOO";
        final ResourceAddress address = resourceAddressFactory.newResourceAddress(uri);
        final IoHandlerAdapter<IoSession> acceptHandler = new IoHandlerAdapter<IoSession>() {};
        final BridgeSessionInitializer<? extends IoFuture> initializer =
                new BridgeSessionInitializerAdapter<>();

        // Set up top level REAL WS acceptor
        final WsAcceptor wsAcceptor = new WsAcceptor(WebSocketExtensionFactory.newInstance());
        wsAcceptor.setConfiguration(configuration);

        final Set<WsAcceptor.EnabledWsTransport> ts = wsAcceptor.resolveEnabledTransports();


        // Set up the mock bridge service factory with the ws* acceptors
        wsAcceptor.setWsnAcceptor(wsnAcceptor);
        wsAcceptor.setWsebAcceptor(wsebAcceptor);

        // Bind the WS address and ensure the appropriate

        wsAcceptor.bind(address, acceptHandler, initializer);

        // Verify expectations for our mocks too...
        if ( ts.contains(WsAcceptor.EnabledWsTransport.wsn )) {
            assertTrue("Missing expected call to wsnAcceptor.bind().", wsnAcceptor.bindWasCalled());
        } else {
            assertTrue( "Unexpected call to wsnAcceptor.bind().", wsnAcceptor.bindWasNotCalled());
        }
        if ( ts.contains(WsAcceptor.EnabledWsTransport.wseb )) {
            assertTrue("Missing expected call to wsebAcceptor.bind().", wsebAcceptor.bindWasCalled());
        } else {
            assertTrue("Unexpected call to wsebAcceptor.bind().", wsebAcceptor.bindWasNotCalled());
        }
    }


    // see http://jira.kaazing.wan/browse/KG-5317
    @Test(expected = IllegalArgumentException.class)
    public void shouldNotBindOnNonWsAddress()
        throws Exception {

            String uri = "http://example.com/FOO";
            ResourceAddress address = resourceAddressFactory.newResourceAddress(uri);

        final IoHandlerAdapter<IoSession> acceptHandler = new IoHandlerAdapter<IoSession>() {
            };

            wsAcceptor.bind(address, acceptHandler, null);
    }
}
