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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.rules.RuleChain.outerRule;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_IDLE_TIMEOUT;

import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.service.IoHandler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.tcp.specification.TcpAcceptorRule;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.session.IoSessionEx;


public class TcpIdleTimeoutIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/tcp/rfc793");

    private TcpAcceptorRule acceptor =
            new TcpAcceptorRule().addConfigurationProperty(TCP_IDLE_TIMEOUT.getPropertyName(), "5");

    private TestRule timeout = new DisableOnDebug(new Timeout(15, SECONDS));

    @Rule
    public TestRule chain = outerRule(acceptor).around(k3po).around(timeout);

    @Test
    @Specification({"server.close/client"})
    public void serverCloseAfterOpenWithNoData() throws Exception {
        k3po.start();
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        acceptor.bind("tcp://127.0.0.1:8080", handler);
        k3po.notifyBarrier("BOUND");
        k3po.finish();
    }

    @Test
    @Specification({"additions/idle.timeout/does.not.close.when.data"})
    public void serverDoesNotCloseWithData() throws Exception {
        CountDownLatch writeAfterOpen = new CountDownLatch(1);
        CountDownLatch writeAfterRecv = new CountDownLatch(1);
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>() {
            @Override
            protected void doSessionOpened(IoSessionEx session) throws Exception {
                writeAfterOpen.countDown();
                super.doSessionOpened(session);
            }

            @Override
            protected void doMessageReceived(IoSessionEx session, Object message) throws Exception {
                writeAfterRecv.countDown();
                super.doMessageReceived(session, message);
            }
        };
        acceptor.bind("tcp://127.0.0.1:8080", handler);
        k3po.start();
        k3po.notifyBarrier("BOUND");
        writeAfterOpen.await();
        Thread.sleep(3000);
        k3po.notifyBarrier("SEND1");
        writeAfterRecv.await();
        Thread.sleep(3000);
        k3po.notifyBarrier("SEND2");
        writeAfterRecv.await();
        Thread.sleep(1000);
        k3po.notifyBarrier("CLOSE");
        k3po.finish();
    }

}