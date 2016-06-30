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
package org.kaazing.gateway.transport.http.connector.specification.rfc7230;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.transport.http.HttpConnectSession;
import org.kaazing.gateway.transport.http.HttpConnectorRule;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.session.IoSessionEx;

import java.util.concurrent.CountDownLatch;

@org.junit.Ignore("The head response test passes even without fix")
public class MessageFormatIT {

    private final HttpConnectorRule connector = new HttpConnectorRule();
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/http/rfc7230/message.format");

    @Rule
    public TestRule chain = createRuleChain(connector, k3po);

    private Mockery context;

    @Before
    public void initialize() {
        context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());
    }

    @Test
    @Specification({
            "head.response.must.not.have.content.though.may.have.content.length/response" })
    public void headResponseMustNotHaveContentThoughMayHaveContentLength() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                allowing(handler).messageReceived(with(any(IoSessionEx.class)), with(any(Object.class)));
                allowing(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializer());
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();

        k3po.finish();
    }

    private static class ConnectSessionInitializer implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.HEAD);
        }
    }

}
