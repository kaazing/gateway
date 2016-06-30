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
package org.kaazing.gateway.transport.wseb.specification.wse.acceptor;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kaazing.test.util.ITUtil.timeoutRule;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.service.IoHandler;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.gateway.transport.wseb.test.WsebAcceptorRule;
import org.kaazing.gateway.util.InternalSystemProperty;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MethodExecutionTrace;

public class ProxiesIT {

    private K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/wse/proxies");

    private WsebAcceptorRule acceptor;

    {
        Properties configuration = new Properties();
        configuration.setProperty(InternalSystemProperty.WS_CLOSE_TIMEOUT.getPropertyName(), "2s");
        acceptor = new WsebAcceptorRule(configuration);
    }

    private JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private TestRule contextRule = ITUtil.toTestRule(context);
    private final TestRule trace = new MethodExecutionTrace();
    private final TestRule timeoutRule = timeoutRule(5, SECONDS);

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(acceptor).around(contextRule).
             around(k3po).around(timeoutRule);

    @Test
    @Specification("client.send.overlapping.downstream.request/request")
    public void shouldFlushAndCloseDownstreamUponReceivingOverlappingLongpollingRequest() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                // Exception is from abrupt close of second downstream when k3po execution terminates
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(IOException.class)));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });

        acceptor.bind("wse://localhost:8080/path", handler);
        k3po.finish();
    }

    @Test
    @Specification("server.send.data.on.longpolling.request/request")
    public void shouldReceiveDataFromServerOnLongpollingRequest() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final AtomicReference<IoSessionEx> session = new AtomicReference<>();

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                will(saveParameter(session, 0));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });

        acceptor.bind("wse://localhost:8080/path", handler);
        k3po.start();
        k3po.awaitBarrier("CREATED");
        session.get().write(session.get().getBufferAllocator().wrap(ByteBuffer.wrap("data1".getBytes())));
        k3po.awaitBarrier("FIRST_DOWNSTREAM_RESPONSE_COMPLETE");
        session.get().write(session.get().getBufferAllocator().wrap(ByteBuffer.wrap("data2".getBytes())));
        k3po.finish();
    }

    @Test
    @Ignore("tickets#321 Secure redirect with wse downstream does not work")
    @Specification("server.send.secure.downstream.redirect/request")
    public void shouldReceiveSecureRedirectFromServerOnProxyModeRequest() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });

        acceptor.bind("wse://localhost:8080/path", handler);
        k3po.finish();
    }

    @Test
    @Specification("client.request.heartbeat.interval/request")
    public void shouldSendHeartbeatToClient() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });

        acceptor.bind("wse://localhost:8080/path", handler);
        k3po.finish();
    }
}
