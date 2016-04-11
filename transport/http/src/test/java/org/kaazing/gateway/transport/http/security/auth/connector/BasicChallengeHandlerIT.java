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
package org.kaazing.gateway.transport.http.security.auth.connector;

import static org.kaazing.gateway.transport.http.HttpMethod.GET;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.transport.http.HttpConnectSession;
import org.kaazing.gateway.transport.http.HttpConnectorRule;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.netx.http.auth.ChallengeHandler;

public class BasicChallengeHandlerIT {

    private final HttpConnectorRule connector = new HttpConnectorRule().setSchedulerProvider(new SchedulerProvider());
    private final K3poRule k3po = new K3poRule();

    @Rule
    public TestRule chain = createRuleChain(connector, k3po);

    private Mockery context;

    @Before
    public void initialize() {
        context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());
    }

    @Specification("application.challenge.and.accept")
    @Test
    public void basicChallengeAndAccept() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSession.class)));
                oneOf(handler).sessionOpened(with(any(IoSession.class)));
                oneOf(handler).sessionClosed(with(any(IoSession.class)));
            }
        });
        ArrayList<Class<? extends ChallengeHandler>> challengeHandlers = new ArrayList<>();
        challengeHandlers.add(TestChallengeHandler.class);
        Map<String, Object> connectOptions = new HashMap<>();
        connectOptions.put("http.challengeHandler", challengeHandlers);

        connector.connect("http://localhost:8080/resource", handler, new IoSessionInitializer<ConnectFuture>() {
            @Override
            public void initializeSession(IoSession session, ConnectFuture future) {
                HttpConnectSession connectSession = (HttpConnectSession) session;
                connectSession.setMethod(GET);
            }
        }, connectOptions);

        k3po.finish();
    }

}
