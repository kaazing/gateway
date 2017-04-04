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
package org.kaazing.gateway.transport.udp.specification;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.resource.address.udp.UdpResourceAddress;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.ResolutionTestUtils;

/**
 * RFC-768
 */
@RunWith(Parameterized.class)
public class UdpConnectorIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/udp/rfc768");

    private UdpConnectorRule connector = new UdpConnectorRule();

    private static String networkInterface = ResolutionTestUtils.getLoopbackInterface();

    @Rule
    public TestRule chain = createRuleChain(connector, k3po);

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {     
                {"udp://127.0.0.1:8080"}//, {"udp://[@" + networkInterface + "]:8080"}
           });
    }

    @Parameter
    public String uri;

    private void connectTo8080(IoHandlerAdapter<IoSessionEx> handler) throws Exception {
        ConnectFuture connectFuture = connector.connect(uri, handler, null);
        connectFuture.await(1, SECONDS);
        assertTrue(connectFuture.isConnected());
    }

    private WriteFuture writeStringMessageToSession(String message, IoSession session) {
        ByteBuffer data = ByteBuffer.wrap(message.getBytes());
        IoBufferAllocatorEx<?> allocator = ((IoSessionEx) session).getBufferAllocator();
        return session.write(allocator.wrap(data));
    }

    @Test
    @Specification("establish.connection/server")
    public void establishConnection() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        k3po.start();
        k3po.awaitBarrier("BOUND");
        connectTo8080(new IoHandlerAdapter<IoSessionEx>(){
            @Override
            protected void doSessionOpened(IoSessionEx session) {
                writeStringMessageToSession("client data", session);
                latch.countDown();
            }
        });
        k3po.finish();
        assertTrue(latch.await(2, SECONDS));
    }

    @Test
    @Specification("server.sent.data/server")
    public void serverSentData() throws Exception {
        k3po.start();
        k3po.awaitBarrier("BOUND");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> data = new AtomicReference<>();
        connectTo8080(new IoHandlerAdapter<IoSessionEx>() {

            @Override
            protected void doSessionOpened(IoSessionEx session) {
                writeStringMessageToSession("client data", session);
            }

            @Override
            protected void doMessageReceived(IoSessionEx session, Object message) {
                data.set(new String(((IoBuffer) message).array()));
                latch.countDown();
            }
        });

        k3po.finish();
        latch.await(2, TimeUnit.SECONDS);   // since k3po may finish early
        assertEquals("server data", data.get());
    }

    @Test
    @Specification("client.sent.data/server")
    public void clientSentData() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        k3po.start();
        k3po.awaitBarrier("BOUND");
        WriteFuture[] futures = new WriteFuture[1];
        connectTo8080(new IoHandlerAdapter<IoSessionEx>(){
            @Override
            protected void doSessionOpened(IoSessionEx session) {
                WriteFuture future = writeStringMessageToSession("client data", session);
                futures[0] = future;
                latch.countDown();
            }
        });

        k3po.finish();
        latch.await(5, TimeUnit.SECONDS);   // since k3po may finish early
        futures[0].await(2, SECONDS);
        assertTrue(futures[0].isWritten());
    }

    @Test
    @Specification("echo.data/server")
    public void bidirectionalData() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);

        k3po.start();
        k3po.awaitBarrier("BOUND");
        connectTo8080(new IoHandlerAdapter<IoSessionEx>(){
            private boolean first = true;

            @Override
            protected void doSessionOpened(IoSessionEx session) {
                writeStringMessageToSession("client data 1", session);
            }

            @Override
            protected void doMessageReceived(IoSessionEx session, Object message) {
                String decoded = new String(((IoBuffer) message).array());
                if (first) {
                    assertEquals("server data 1", decoded);
                    writeStringMessageToSession("client data 2", session);
                    first = false;
                } else {
                    assertEquals("server data 2", decoded);
                }
                latch.countDown();
            }
        });

        k3po.finish();
        assertTrue(latch.await(2, SECONDS));
    }


    @Test
    @Specification("additions/align.content/server")
    public void alignData() throws Exception {
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        ResourceOptions resourceOptions = ResourceOptions.FACTORY.newResourceOptions();
        resourceOptions.setOption(UdpResourceAddress.PADDING_ALIGNMENT, 4);
        ResourceAddress connectAddress = addressFactory.newResourceAddress("udp://127.0.0.1:8080", resourceOptions);
        AtomicBoolean bytesAligned = new AtomicBoolean(true);
        CountDownLatch messagesReceived = new CountDownLatch(2);
        ConnectFuture connectFuture = connector.connect(connectAddress, new IoHandlerAdapter<IoSessionEx>(){
            @Override
            protected void doSessionOpened(IoSessionEx session) {
                writeStringMessageToSession("client data", session);
            }

            @Override
            protected void doMessageReceived(IoSessionEx session, Object message) {
                if (bytesAligned.get()) {
                    bytesAligned.set((((IoBuffer) message).remaining() % 4 == 0));
                }
                messagesReceived.countDown();
            }
        }, null);
        connectFuture.await(1, SECONDS);
        assertTrue(connectFuture.isConnected());
        k3po.finish();
        messagesReceived.await(2, SECONDS);
        assertTrue(bytesAligned.get());
    }

//    @Test
//    @Specification("client.close/server")
//    @Ignore("no idle timeout on client side. so there isn't way to trigger closed event other than session.close()")
//    public void clientClose() throws Exception {
//        CountDownLatch closed = new CountDownLatch(1);
//
//        k3po.start();
//        k3po.awaitBarrier("BOUND");
//        connectTo8080(new IoHandlerAdapter<IoSessionEx>(){
//            @Override
//            protected void doSessionOpened(IoSessionEx session) {
//                writeStringMessageToSession("client data", session);
//            }
//            @Override
//            protected void doSessionClosed(IoSessionEx session) {
//                closed.countDown();
//            }
//        });
//        k3po.finish();
//
//        closed.await(5,  SECONDS);
//    }

    @Test
    @Specification("client.close/server")
    public void clientCloseFuture() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        CloseFuture[] futures = new CloseFuture[1];

        k3po.start();
        k3po.awaitBarrier("BOUND");
        connectTo8080(new IoHandlerAdapter<IoSessionEx>(){
            @Override
            protected void doSessionOpened(IoSessionEx session) {
                writeStringMessageToSession("client data", session);
                CloseFuture future = session.close(true);
                futures[0] = future;
                latch.countDown();
            }
            @Override
            protected void doSessionClosed(IoSessionEx session) {
                latch.countDown();
            }
        });
        k3po.finish();

        latch.await(5,  SECONDS);
        futures[0].await(2, SECONDS);
        assertTrue(futures[0].isClosed());
    }

    @Test
    @Specification("concurrent.connections/server")
    public void concurrentConnections() throws Exception {
        CountDownLatch latch = new CountDownLatch(6);

        class ConcurrentHandler extends IoHandlerAdapter<IoSessionEx> {
            private boolean first = true;

            @Override
            protected void doSessionOpened(IoSessionEx session) {
                writeStringMessageToSession("Hello", session);
            }

            @Override
            protected void doMessageReceived(IoSessionEx session, Object message) {
                String decoded = new String(((IoBuffer) message).array());
                if (first) {
                    assertEquals("Hello", decoded);
                    writeStringMessageToSession("Goodbye", session);
                    first = false;
                } else {
                    assertEquals("Goodbye", decoded);
                }
                latch.countDown();
            }
        };

        k3po.start();
        k3po.awaitBarrier("BOUND");

        connectTo8080(new ConcurrentHandler());
        connectTo8080(new ConcurrentHandler());
        connectTo8080(new ConcurrentHandler());

        k3po.finish();
        assertTrue(latch.await(2, SECONDS));
    }

}
