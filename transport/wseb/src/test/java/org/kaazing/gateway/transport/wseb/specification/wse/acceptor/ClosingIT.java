/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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
package org.kaazing.gateway.transport.wseb.specification.wse.acceptor;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.wseb.test.WsebAcceptorRule;
import org.kaazing.gateway.util.InternalSystemProperty;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class ClosingIT {

    private K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/wse/closing");

    private WsebAcceptorRule acceptor;

    {
        Properties configuration = new Properties();
        configuration.setProperty(InternalSystemProperty.WS_CLOSE_TIMEOUT.getPropertyName(), "2s");
        acceptor = new WsebAcceptorRule(configuration);
    }

    @Rule
    public TestRule chain = createRuleChain(acceptor, k3po);

    @Test
    @Specification("client.send.close/request")
    public void shouldEchoClientCloseFrame() throws Exception {
        acceptor.bind("wse://localhost:8080/path", new IoHandlerAdapter<IoSession>());
        k3po.finish();
    }

    // This test is only applicable for clients
    //@Specification({
    //    "client.send.close.no.reply.from.server/request",
    //    "client.send.close.no.reply.from.server/response" })
    //public void clientShouldCloseIfServerDoesNotEchoCloseFrame() throws Exception {
    //    k3po.finish();
    //}

    @Test
    @Specification("server.send.close/request")
    public void shouldPerformServerInitiatedClose() throws Exception {
        acceptor.bind("wse://localhost:8080/path", new IoHandlerAdapter<IoSession>() {
            @Override
            protected void doSessionOpened(IoSession session) throws Exception {
                session.close(false);
            }

        });
        k3po.finish();
    }

    @Test
    @Specification("server.send.close.no.reply.from.client/request")
    public void serverShouldCloseIfClientDoesNotEchoCloseFrame() throws Exception {
        final AtomicLong timeToClose = new AtomicLong(0);
        CountDownLatch closed = new CountDownLatch(1);
        acceptor.bind("wse://localhost:8080/path", new IoHandlerAdapter<IoSession>() {
            @Override
            protected void doSessionOpened(IoSession session) throws Exception {
                final long start = currentTimeMillis();
                session.close(false).addListener(new IoFutureListener<IoFuture>() {

                    @Override
                    public void operationComplete(IoFuture future) {
                        timeToClose.set(currentTimeMillis() - start);
                        closed.countDown();
                    }
                });
            }

        });
        k3po.finish();
        assertTrue("wsebSession was not closed after 4 seconds", closed.await(4, SECONDS));
        // Timing is not exact but should be close
        assertTrue(format("Time taken for ws close handshake timeout %d should be close to 2000 millisecs", timeToClose.get()),
                timeToClose.get() > 1500 && timeToClose.get() < 4000);
    }

    // This test is only applicable for clients
    // @Specification("server.send.data.after.close/request")
    //public void shouldIgnoreDataFromServerAfterCloseFrame() throws Exception {
    //    k3po.finish();
    //}

    // This test is oonly applicable for clients
    // @Specification("server.send.data.after.reconnect/request")
    // public void shouldIgnoreDataFromServerAfterReconnectFrame()
    //        throws Exception {
    //    k3po.finish();
    //}
}
