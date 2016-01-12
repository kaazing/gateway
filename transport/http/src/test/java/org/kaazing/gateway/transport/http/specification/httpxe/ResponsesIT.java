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

package org.kaazing.gateway.transport.http.specification.httpxe;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpAcceptorRule;
import org.kaazing.gateway.transport.http.HttpHeaders;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.session.IoSessionEx;

import java.net.URI;
import java.util.concurrent.CountDownLatch;

/**
 * Defines how httpxe will deal with http methods.
 */
public class ResponsesIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/httpxe/responses");

    private final HttpAcceptorRule acceptor = new HttpAcceptorRule();

    private Mockery context;

    @Before
    public void initialize() {
        context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        context.setThreadingPolicy(new Synchroniser());
    }

    @Rule
    public final TestRule chain = createRuleChain(acceptor, k3po);

    @Test
    @Specification({
        "wrapped.101.response.in.200/request"
    })
    public void shouldPassWithWrapped101ResponseIn200() throws Exception {
        test();
    }

    @Test
    @Specification({
        "wrapped.201.response.in.200/request"
    })
    public void shouldPassWithWrapped201ResponseIn200() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {
            @Override
            protected void doSessionCreated(HttpAcceptSession session) throws Exception {
                System.out.println("JITU session created " + session);
            }
            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                System.out.println("JITU session opened " + session);
                session.setStatus(HttpStatus.SUCCESS_CREATED);
                session.setWriteHeader(HttpHeaders.HEADER_CONTENT_TYPE, "text/html");
                session.close(false);
                latch.countDown();

            }
        };

        acceptor.bind(address(), acceptHandler);
        k3po.finish();

        latch.await(4, SECONDS);

        context.assertIsSatisfied();
    }

    @Test
    @Specification({
        "wrapped.302.response.in.200/request"
    })
    public void shouldPassWithWrapped302ResponseIn200() throws Exception {
        test();
    }

    @Test
    @Specification({
        "wrapped.400.response.in.200/request"
    })
    public void shouldPassWithWrapped400ResponseIn200() throws Exception {
        test();
    }

    @Test
    @Specification({
        "wrapped.501.response.in.200/request"
    })
    public void shouldPassWithWrapped501ResponseIn200() throws Exception {
        test();
    }

    @Test
    @Specification({
        "connection.header.not.enveloped.in.response.body/request"
    })
    public void shouldPassWhenConnectionHeaderInHeaderNotBody() throws Exception {
        test();
    }

    private void test() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = context.mock(IoHandler.class);
        context.checking(new Expectations() {
            {
                oneOf(acceptHandler).sessionCreated(with(any(IoSession.class)));
                will(new CustomAction("Foo") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        System.out.println("JITU ******* session is created");
                        return null;
                    }
                });
                oneOf(acceptHandler).sessionOpened(with(any(IoSession.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });
        acceptor.bind(address(), acceptHandler);
        k3po.finish();

        latch.await(4, SECONDS);

        context.assertIsSatisfied();
    }

    // Returns http address with httpxe address as its alernate
    private ResourceAddress address() {
        String address = "httpxe://localhost:8000/path";

        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        ResourceAddress httpxeAddress = addressFactory.newResourceAddress(URI.create(address));

        String httpAddressStr = address.replace("httpxe", "http");
        ResourceOptions options = ResourceOptions.FACTORY.newResourceOptions();
        options.setOption(ResourceAddress.ALTERNATE, httpxeAddress);
        return addressFactory.newResourceAddress(URI.create(httpAddressStr), options);
    }

}
