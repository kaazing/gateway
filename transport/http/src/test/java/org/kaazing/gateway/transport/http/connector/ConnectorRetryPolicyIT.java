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
package org.kaazing.gateway.transport.http.connector;

import static org.kaazing.gateway.transport.http.HttpMethod.GET;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jmock.Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.http.HttpConnectorRetryPolicy;
import org.kaazing.gateway.transport.http.HttpConnectSession;
import org.kaazing.gateway.transport.http.HttpConnectorRule;
import org.kaazing.gateway.transport.http.HttpSession;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class ConnectorRetryPolicyIT {

    private final HttpConnectorRule connector = new HttpConnectorRule();
    private final K3poRule k3po = new K3poRule();

    @Rule
    public TestRule chain = createRuleChain(connector, k3po);

    private Mockery context;

    @Before
    public void initialize() {
        context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());
    }

    @Specification("basic.challenge.and.accept")
    @Test
    public void basicChallengeAndAccept() throws Exception {
        final IoHandler handler = new IoHandler() {

            @Override
            public void sessionOpened(IoSession arg0) throws Exception {
                System.out.println("sessionOpened");
                HttpSession httpSession = (HttpSession) arg0;
                HttpStatus status = httpSession.getStatus();
                System.out.println("status: " + status);
            }

            @Override
            public void sessionIdle(IoSession arg0, IdleStatus arg1) throws Exception {
                System.out.println("sessionIdle");
            }

            @Override
            public void sessionCreated(IoSession arg0) throws Exception {
                System.out.println("sessionCreated");
                HttpSession httpSession = (HttpSession) arg0;
                HttpStatus status = httpSession.getStatus();
                System.out.println("status: " + status);
            }

            @Override
            public void sessionClosed(IoSession arg0) throws Exception {
                System.out.println("sessionClosed");
                HttpSession httpSession = (HttpSession) arg0;
                HttpStatus status = httpSession.getStatus();
                System.out.println("status: " + status);
            }

            @Override
            public void messageSent(IoSession arg0, Object arg1) throws Exception {
                System.out.println("messageSent");
            }

            @Override
            public void messageReceived(IoSession arg0, Object arg1) throws Exception {
                System.out.println("messageReceived");
            }

            @Override
            public void exceptionCaught(IoSession arg0, Throwable arg1) throws Exception {
                arg1.printStackTrace();
                System.out.println("exceptionCaught");
            }
        };

        // Create challengeHandlers and add to connect options
        ArrayList<Class<? extends HttpConnectorRetryPolicy>> retryPolicies = new ArrayList<>();
        retryPolicies.add(SampleChallengeRetryPolicy.class);
        Map<String, Object> connectOptions = new HashMap<>();
        connectOptions.put("http.retryPolicyClasses", retryPolicies);

        connector.connect("http://localhost:8085/resource", handler, new IoSessionInitializer<ConnectFuture>() {
            @Override
            public void initializeSession(IoSession session, ConnectFuture future) {
                HttpConnectSession connectSession = (HttpConnectSession) session;
                connectSession.setMethod(GET);
            }
        }, connectOptions);

        // assertTrue(latch.await(10, SECONDS));
        k3po.finish();
        context.assertIsSatisfied();
    }

}
