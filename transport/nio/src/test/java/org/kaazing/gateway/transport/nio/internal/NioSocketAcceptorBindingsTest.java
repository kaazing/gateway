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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.resource.address.ResourceAddress.ALTERNATE;
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static org.kaazing.gateway.resource.address.ResourceAddressFactory.newResourceAddressFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.transport.Bindings;
import org.kaazing.gateway.transport.Bindings.Binding;
import org.kaazing.gateway.transport.BridgeSessionInitializerAdapter;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.future.DefaultUnbindFuture;
import org.kaazing.mina.core.future.UnbindFuture;

public class NioSocketAcceptorBindingsTest {
    private static final int NUMBER_DISTINCT_ADDRESSES = 10;

    private NioSocketAcceptor tcpAcceptor;
    private ResourceAddressFactory addressFactory;
    private SchedulerProvider schedulerProvider;

    private Bindings<?> tcpBindings;

    @Before
    public void before() throws Exception {
        tcpAcceptor = new NioSocketAcceptor(new Properties());
        addressFactory = newResourceAddressFactory();
        tcpAcceptor.setResourceAddressFactory(addressFactory);
        schedulerProvider = new SchedulerProvider();
        tcpAcceptor.setSchedulerProvider(schedulerProvider);
        tcpBindings = tcpAcceptor.getBindings();
    }

    @After
    public void tearDown() throws Exception {
        tcpAcceptor.dispose();
        schedulerProvider.shutdownNow();
    }

    @Test
    public void bindingMapShouldInitiallyBeEmpty() throws Exception {
        assertTrue(tcpBindings.isEmpty());
    }

    @Test
    public void referenceCountingWorksForOneOrMoreBindsWithDistinctAddresses() throws Exception {

        Map<String,Object> opts = new HashMap<>();
        IoHandlerAdapter handler = new IoHandlerAdapter();
        BridgeSessionInitializerAdapter<IoFuture> initializer = new BridgeSessionInitializerAdapter<>();
        int[] bindPorts = new int[NUMBER_DISTINCT_ADDRESSES];
        String[] bindURIs = new String[NUMBER_DISTINCT_ADDRESSES];
        ResourceAddress[] bindAddresses = new ResourceAddress[NUMBER_DISTINCT_ADDRESSES];

        for(int i = 0; i < NUMBER_DISTINCT_ADDRESSES; i++) {
            bindPorts[i] = findFreePort();
            bindURIs[i] = format("tcp://127.0.0.1:%d", bindPorts[i]);
            bindAddresses[i] = addressFactory.newResourceAddress(bindURIs[i], opts);
        }


        int bindCount = 0;

        // bind distinct addresses 5 times
        do {

            for(int i = 0; i < NUMBER_DISTINCT_ADDRESSES; i++) {
                tcpAcceptor.bind(bindAddresses[i], handler, initializer);
            }

            ++bindCount;

            for (int i = 0; i < NUMBER_DISTINCT_ADDRESSES; i++) {
                for (ResourceAddress alternate = bindAddresses[i]; alternate != null; alternate = alternate.getOption(ALTERNATE)) {
                    Binding tcpBinding = tcpBindings.getBinding(alternate);
                    assertEquals(bindCount, tcpBinding.referenceCount());
                }
            }
        } while (bindCount <= 5);

        // unbind distinct addresses 5 times
        UnbindFuture unbindFuture = DefaultUnbindFuture.succeededFuture();
        do {
            
            for (int i = 0; i < NUMBER_DISTINCT_ADDRESSES; i++) {
                for (ResourceAddress alternate = bindAddresses[i]; alternate != null; alternate = alternate.getOption(ALTERNATE)) {
                    Binding tcpBinding = tcpBindings.getBinding(alternate);
                    assertEquals(bindCount, tcpBinding.referenceCount());
                }
            }

            for (int i = 0; i < NUMBER_DISTINCT_ADDRESSES; i++) {
                unbindFuture = DefaultUnbindFuture.combineFutures(unbindFuture, tcpAcceptor.unbind(bindAddresses[i]));
                unbindFuture.awaitUninterruptibly();
            }

            --bindCount;
        } while (bindCount > 0);

        // verify no bindings present
        for (int i = 0; i < NUMBER_DISTINCT_ADDRESSES; i++) {
            for (ResourceAddress alternate = bindAddresses[i]; alternate != null; alternate = alternate.getOption(ALTERNATE)) {
                Binding tcpBinding = tcpBindings.getBinding(alternate);
                assertNull(tcpBinding);
            }
        }

        assertTrue(tcpBindings.isEmpty());
    }


    @Test
    public void referenceCountingWorksForOneOrMoreBinds() throws Exception {

        int bindPort = findFreePort();
        String bindURI = format("tcp://localhost:%d", bindPort);
        Map<String,Object> opts = new HashMap<>();
        IoHandlerAdapter handler = new IoHandlerAdapter();
        ResourceAddress bindAddress = addressFactory.newResourceAddress(bindURI, opts);
        BridgeSessionInitializerAdapter<IoFuture> initializer = new BridgeSessionInitializerAdapter<>();

        int bindCount = 0;

        // bind 5 times
        do {
            tcpAcceptor.bind(bindAddress, handler, initializer);
            assertFalse("Failed to bind any addresses.", tcpBindings.isEmpty());
            ++bindCount;
            for (ResourceAddress alternate = bindAddress; alternate != null; alternate = alternate.getOption(ALTERNATE)) {
                Binding tcpBinding = tcpBindings.getBinding(alternate);
                assertEquals(bindCount, tcpBinding.referenceCount());
            }
        } while (bindCount <= 5);

        // unbind 5 times
        UnbindFuture unbindFuture = DefaultUnbindFuture.succeededFuture();
        do {
            for (ResourceAddress alternate = bindAddress; alternate != null; alternate = alternate.getOption(ALTERNATE)) {
                Binding tcpBinding = tcpBindings.getBinding(alternate);
                assertEquals(bindCount, tcpBinding.referenceCount());
            }
            unbindFuture = DefaultUnbindFuture.combineFutures(unbindFuture, tcpAcceptor.unbind(bindAddress));
            --bindCount;
            unbindFuture.awaitUninterruptibly();
        } while (bindCount > 0);

        // no bindings present
        for (ResourceAddress alternate = bindAddress; alternate != null; alternate = alternate.getOption(ALTERNATE)) {
            Binding tcpBinding = tcpBindings.getBinding(alternate);
            assertNull(tcpBinding);
        }

        assertTrue(tcpBindings.isEmpty());
    }

    @Test // if we are strict this should fail
    public void bindFollowedByTwoUnbindsSucceeds() throws Exception {

        int bindPort = findFreePort();
        String bindURI = format("tcp://localhost:%d", bindPort);
        Map<String,Object> opts = new HashMap<>();
        IoHandlerAdapter handler = new IoHandlerAdapter();
        ResourceAddress bindAddress = addressFactory.newResourceAddress(bindURI, opts);
        BridgeSessionInitializerAdapter<IoFuture> initializer = new BridgeSessionInitializerAdapter<>();

        // bind #1
        tcpAcceptor.bind(bindAddress, handler, initializer);
        assertFalse("Failed to bind any addresses.", tcpBindings.isEmpty());
        for (ResourceAddress alternate = bindAddress; alternate != null; alternate = alternate.getOption(ALTERNATE)) {
            Binding tcpBinding = tcpBindings.getBinding(alternate);
            assertEquals(1, tcpBinding.referenceCount());
        }

        // unbind #1
        UnbindFuture unbindFuture = DefaultUnbindFuture.succeededFuture();
        unbindFuture = DefaultUnbindFuture.combineFutures(unbindFuture, tcpAcceptor.unbind(bindAddress));
        unbindFuture.awaitUninterruptibly();
        for (ResourceAddress alternate = bindAddress; alternate != null; alternate = alternate.getOption(ALTERNATE)) {
            Binding tcpBinding = tcpBindings.getBinding(alternate);
            if (tcpBinding != null) {
                assertEquals(0, tcpBinding.referenceCount());
            }
        }

        assertTrue(tcpBindings.isEmpty());

        // unbind #2
        DefaultUnbindFuture.combineFutures(unbindFuture, tcpAcceptor.unbind(bindAddress));
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

    @Test
    public void referenceCountingWorksForOneOrMoreBindsWithDistinctNextProtocols() throws Exception {

        IoHandlerAdapter handler = new IoHandlerAdapter();
        BridgeSessionInitializerAdapter<IoFuture> initializer = new BridgeSessionInitializerAdapter<>();
        String[] bindURIs = new String[NUMBER_DISTINCT_ADDRESSES];
        ResourceAddress[] bindAddresses = new ResourceAddress[NUMBER_DISTINCT_ADDRESSES];

        int bindPort = findFreePort();
        for(int i = 0; i < NUMBER_DISTINCT_ADDRESSES; i++) {
            bindURIs[i] = format("tcp://127.0.0.1:%d", bindPort);
            ResourceOptions options = ResourceOptions.FACTORY.newResourceOptions();
            options.setOption(NEXT_PROTOCOL, format("starts-with-ascii-%s", (65 + i)));
            bindAddresses[i] = addressFactory.newResourceAddress(bindURIs[i], options);
        }


        int bindCount = 0;

        // bind distinct addresses 5 times
        do {

            for(int i = 0; i < NUMBER_DISTINCT_ADDRESSES; i++) {
                tcpAcceptor.bind(bindAddresses[i], handler, initializer);
            }

            ++bindCount;

            for (int i = 0; i < NUMBER_DISTINCT_ADDRESSES; i++) {
                for (ResourceAddress alternate = bindAddresses[i]; alternate != null; alternate = alternate.getOption(ALTERNATE)) {
                    Binding tcpBinding = tcpBindings.getBinding(alternate);
                    assertEquals(bindCount, tcpBinding.referenceCount());
                }
            }
        } while (bindCount <= 5);

        // unbind distinct addresses 5 times
        UnbindFuture unbindFuture = DefaultUnbindFuture.succeededFuture();
        do {
            
            for (int i = 0; i < NUMBER_DISTINCT_ADDRESSES; i++) {
                for (ResourceAddress alternate = bindAddresses[i]; alternate != null; alternate = alternate.getOption(ALTERNATE)) {
                    Binding tcpBinding = tcpBindings.getBinding(alternate);
                    assertEquals(bindCount, tcpBinding.referenceCount());
                }
            }

            for (int i = 0; i < NUMBER_DISTINCT_ADDRESSES; i++) {
                unbindFuture = DefaultUnbindFuture.combineFutures(unbindFuture, tcpAcceptor.unbind(bindAddresses[i]));
                unbindFuture.awaitUninterruptibly();
            }

            --bindCount;
        } while (bindCount > 0);

        // verify no bindings present
        for (int i = 0; i < NUMBER_DISTINCT_ADDRESSES; i++) {
            for (ResourceAddress alternate = bindAddresses[i]; alternate != null; alternate = alternate.getOption(ALTERNATE)) {
                Binding tcpBinding = tcpBindings.getBinding(alternate);
                assertNull(tcpBinding);
            }
        }

        assertTrue(tcpBindings.isEmpty());
    }

    /*
    public static final class TestProtocolDispatcherFactorySpi extends ProtocolDispatcherFactorySpi {

        private final Map<String, ProtocolDispatcher> dispatchers;

        public TestProtocolDispatcherFactorySpi() {
            Map<String, ProtocolDispatcher> dispatchers = new HashMap<String, ProtocolDispatcher>();
            for (int i=0; i < 128; i++) {
                final String protocolName = format("starts-with-ascii-%s", i);
                final Collection<byte[]> discriminators = singleton(new byte[] { (byte) i });
                ProtocolDispatcher dispatcher = new ProtocolDispatcher() {
                    
                    @Override
                    public String getProtocolName() {
                        return protocolName;
                    }
                    
                    @Override
                    public Collection<byte[]> getDiscriminators() {
                        return discriminators;
                    }

                    @Override
                    public int compareTo(ProtocolDispatcher pd) {
                        return protocolDispatchComparator.compare(this, pd);
                    }
                };
                dispatchers.put(protocolName, dispatcher);
            }
            this.dispatchers = dispatchers;
        }

        @Override
        public Collection<String> getProtocolNames() {
            return dispatchers.keySet();
        }

        @Override
        public ProtocolDispatcher getProtocolDispatcher(String protocolName) {
            return dispatchers.get(protocolName);
        }
    }
    */
}
