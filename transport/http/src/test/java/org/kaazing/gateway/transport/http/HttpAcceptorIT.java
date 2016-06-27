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
package org.kaazing.gateway.transport.http;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.core.AllOf.allOf;
import static org.jmock.lib.script.ScriptedAction.perform;
import static org.kaazing.gateway.resource.address.ResourceAddressFactory.newResourceAddressFactory;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.INJECTABLE_HEADERS;
import static org.kaazing.gateway.transport.http.HttpMatchers.hasMethod;
import static org.kaazing.gateway.transport.http.HttpMatchers.hasReadHeader;
import static org.kaazing.gateway.transport.http.HttpMethod.POST;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.util.Collections;
import java.util.Map;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class HttpAcceptorIT {

    private HttpAcceptor httpAcceptor;
    private ResourceAddress httpAddress;

    private K3poRule robot = new K3poRule();

    @Rule
    public TestRule chain = createRuleChain(robot, 10, SECONDS);

    @Rule
    public JUnitRuleMockery mockery = new JUnitRuleMockery() { {
        setImposteriser(ClassImposteriser.INSTANCE);
        setThreadingPolicy(new Synchroniser());
    } };
    private NioSocketAcceptor tcpAcceptor;

    @Before
    public void setupAcceptor() {
        ResourceAddressFactory addressFactory = newResourceAddressFactory();
        Map<String, ?> config = Collections.emptyMap();
        TransportFactory transportFactory = TransportFactory.newTransportFactory(config);
        BridgeServiceFactory serviceFactory = new BridgeServiceFactory(transportFactory);

        HttpAcceptor httpAcceptor = (HttpAcceptor)transportFactory.getTransport("http").getAcceptor();
        httpAcceptor.setBridgeServiceFactory(serviceFactory);
        httpAcceptor.setResourceAddressFactory(addressFactory);

        SchedulerProvider provider = new SchedulerProvider();
        httpAcceptor.setSchedulerProvider(provider);

        NioSocketAcceptor tcpAcceptor = (NioSocketAcceptor)transportFactory.getTransport("tcp").getAcceptor();
        tcpAcceptor.setSchedulerProvider(provider);
        tcpAcceptor.setResourceAddressFactory(addressFactory);
        tcpAcceptor.setBridgeServiceFactory(serviceFactory);

        String location = "http://localhost:8000/path";
        ResourceOptions options = ResourceOptions.FACTORY.newResourceOptions();
        options.setOption(INJECTABLE_HEADERS, Collections.emptySet());
        ResourceAddress httpAddress = addressFactory.newResourceAddress(location, options);

        this.tcpAcceptor = tcpAcceptor;
        this.httpAcceptor = httpAcceptor;

        this.httpAddress = httpAddress;
    }

    @After
    public void disposeAcceptor() {
        if (httpAcceptor != null) {
            httpAcceptor.dispose();
        }

        if (tcpAcceptor != null) {
            tcpAcceptor.dispose();
        }
    }

    @Specification("should.receive.echoed.post.body.in.response.body")
    @Test
    public void shouldEchoHttpPostBodyinHttpResponseBody() throws Exception {

        final IoHandler handler = mockery.mock(IoHandler.class);
        @SuppressWarnings("unchecked")
        final IoFutureListener<IoFuture> writeListener = mockery.mock(IoFutureListener.class);

        mockery.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSession.class)));

                oneOf(handler).sessionOpened(with(allOf(hasMethod(POST),
                                                        hasReadHeader("Content-Type", "text/plain;charset=UTF-8"))));
                will(perform("$0.setWriteHeader(\"Content-Type\", \"text/plain;charset=UTF-8\")"));

                oneOf(handler).messageReceived(with(any(HttpAcceptSession.class)), with(hasRemaining(12)));
                // @formatter:off
                will(perform("$0.suspendWrite();" +
                             "$0.write($1.duplicate()).addListener(writeListener);" +
                             "$0.shutdownWrite();" +
                             "$0.resumeWrite();" +
                             "$0.close(false);" +
                             "return;").where("writeListener", writeListener));
                // @formatter:on

                // we no longer fire messageSent for performance reasons
                never(handler).messageSent(with(any(IoSession.class)), with(hasRemaining(12)));
                oneOf(writeListener).operationComplete(with(any(IoFuture.class)));

                oneOf(handler).sessionClosed(with(any(IoSession.class)));
            }
        });

        // bind the address behavior
        httpAcceptor.bind(httpAddress, handler, null);

        robot.finish();
    }
}
