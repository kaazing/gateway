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

import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.resource.address.ResourceAddressFactory.newResourceAddressFactory;
import static org.kaazing.gateway.util.InternalSystemProperty.GATEWAY_IDENTIFIER;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.nio.internal.socket.NioSocketAcceptor;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.test.util.MemoryAppender;
import org.kaazing.test.util.MethodExecutionTrace;

public class NamedNioWorkerThreadsTest {

    @Rule
    public TestRule testExecutionTrace = new MethodExecutionTrace();

    @Rule
    public TestRule timeout = new DisableOnDebug(new Timeout(10, TimeUnit.SECONDS));


    @Test
    public void shouldLogIOThreadsWithGatewayIdentifier() throws Exception {
        final SchedulerProvider schedulerProvider = new SchedulerProvider();
        final ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        final String connectURIString = "tcp://127.0.0.1:8000";
        final Map<String, Object> acceptOptions = new HashMap<>();
        final ResourceAddress bindAddress =
                addressFactory.newResourceAddress(
                        connectURIString,
                        acceptOptions);

        final CountDownLatch sessionOpened = new CountDownLatch(1);
        final IoHandler ioHandler = new IoHandlerAdapter() {
            @Override
            protected void doSessionOpened(IoSession session) throws Exception {
                super.doSessionOpened(session);
                sessionOpened.countDown();
            }
        };

        Properties configuration = new Properties();
        configuration.setProperty(GATEWAY_IDENTIFIER.getPropertyName(), "gw1");
        NioSocketAcceptor acceptor = new NioSocketAcceptor(configuration);
        acceptor.setSchedulerProvider(schedulerProvider);
        acceptor.setResourceAddressFactory(newResourceAddressFactory());
        acceptor.bind(bindAddress, ioHandler, null);

        InetSocketAddress remoteAddress = new InetSocketAddress("localhost", 8000);
        Socket socket = new Socket();
        socket.connect(remoteAddress);
        assertTrue(sessionOpened.await(5, TimeUnit.SECONDS));
        socket.close();

        acceptor.unbind(bindAddress);
        schedulerProvider.shutdownNow();
        acceptor.dispose();

        // TODO make sure messages were logged
        MemoryAppender.assertLogMessages(
                Arrays.asList("\\[gw1:New I/O worker #\\d+\\]"),
                Arrays.asList("\\[New I/O worker #\\d+\\]"),
                null,
                null,
                null,
                false,
                true);
    }
}
