/**
 * Copyright (c) 2007-2015, Kaazing Corporation. All rights reserved.
 */

package org.kaazing.gateway.transport.wseb;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.wseb.test.WsebAcceptorRule;
import org.kaazing.mina.core.buffer.SimpleBufferAllocator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.mina.core.buffer.IoBuffer;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MethodExecutionTrace;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import static org.kaazing.gateway.util.Utils.asByteBuffer;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.rules.RuleChain.outerRule;

public class WsebCloseIT {

    private final K3poRule robot = new K3poRule();

    private final WsebAcceptorRule acceptorRule = new WsebAcceptorRule();

    @Rule
    public final TestRule chain = ITUtil.createRuleChain(acceptorRule, robot, 30, SECONDS);

    private static class Backend implements Runnable {
        private final WsebSession session;
        private volatile boolean stop;

        Backend(WsebSession session) {
            this.session = session;
        }

        void stop() {
            stop = true;
        }

        public void run() {
            while(!stop) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("STOPPING WSEB session from backend thread");
            session.close(true);
        }
    }

    private static long getScheduledWriteBytes(IoSession client) {
        IoSession session = client;
        while (session instanceof BridgeSession) {
            IoSession parent = ((BridgeSession)session).getParent();
            if (parent == null) { // parent can occasionally be null (e.g. on a WsebSession from Flash client)
                break;
            }
            session = parent;
        }
        return session.getScheduledWriteBytes();
    }

    @Test
    @Specification("wse.session.close.immediately")
    @Ignore("Need a robot feature to read data (without matching)")
    public void testCloseImmediately() throws Exception {
        final ReadBarrier barrier = new ReadBarrier();
        new Thread(barrier, "barrier").start();

        acceptorRule.bind("wse://localhost:8000/echo", new IoHandlerAdapter<WsebSession>() {
            protected void doExceptionCaught(WsebSession session, Throwable cause) throws Exception {
                cause.printStackTrace();
            }

            protected void doMessageReceived(WsebSession session, Object message) throws Exception {
                Backend backend = new Backend(session);
                new Thread(backend, "Backend").start();

                IoBuffer buf = SimpleBufferAllocator.BUFFER_ALLOCATOR.wrap(asByteBuffer("Hello, WebSocket1"));
                session.write(buf);
                buf = SimpleBufferAllocator.BUFFER_ALLOCATOR.wrap(asByteBuffer("Hello, WebSocket2"));
                session.write(buf);

                int packets = 500000;
                for(int i=0; i < packets; i++) {
                    buf = data();
                    session.write(buf);
                    long bytes = getScheduledWriteBytes(session);
                    //System.out.println("Scheduled bytes = " + bytes);

                    if (bytes > 15000) {
                        barrier.wakeup();
                        backend.stop();
                        break;
                    }
                }
            }

            protected void doSessionClosed(WsebSession session) throws Exception {
                System.out.println("WsebCloseIT#doSessionClosed");
            }

            protected void doSessionCreated(WsebSession session) throws Exception {
                System.out.println("WsebCloseIT#doSessionCreated");
            }

            protected void doSessionOpened(WsebSession session) throws Exception {
                System.out.println("WsebCloseIT#doSessionOpened");
            }
        });
        robot.finish();
    }

    private static IoBuffer data() {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i < 30000; i++) {
            sb.append('1');
        }
        String str = sb.toString();
        return SimpleBufferAllocator.BUFFER_ALLOCATOR.wrap(asByteBuffer(str));
    }

    private static class ReadBarrier implements Runnable {

        private final CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void run() {
            ServerSocket listen = null;
            try {
                listen = new ServerSocket();
                listen.setReuseAddress(true);
                listen.bind(new InetSocketAddress("localhost", 61234));
                Socket socket = listen.accept();
                latch.await();
                socket.getOutputStream().write(("WakeUp").getBytes());
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (listen != null) {
                    try {
                        listen.close();
                    } catch (IOException ioe) {
                        // no-op
                    }
                }
            }
        }

        void wakeup() {
            latch.countDown();
        }
    }

}
