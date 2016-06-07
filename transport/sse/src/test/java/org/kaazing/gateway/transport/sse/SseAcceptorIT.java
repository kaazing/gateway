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
package org.kaazing.gateway.transport.sse;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static org.jboss.netty.util.CharsetUtil.UTF_8;
import static org.jmock.lib.script.ScriptedAction.perform;
import static org.kaazing.gateway.resource.address.ResourceAddressFactory.newResourceAddressFactory;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.INJECTABLE_HEADERS;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.http.HttpAcceptor;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.buffer.SimpleBufferAllocator;

public class SseAcceptorIT {

    private SseAcceptor sseAcceptor;
    private ResourceAddress sseAddress;

    @Rule
    public K3poRule robot = new K3poRule();

    @Rule
    public JUnitRuleMockery mockery = new JUnitRuleMockery() { {
        setImposteriser(ClassImposteriser.INSTANCE);
        setThreadingPolicy(new Synchroniser());
    } };
    private NioSocketAcceptor tcpAcceptor;
    private HttpAcceptor httpAcceptor;

    @Before
    public void setupAcceptor() {
        Map<String, ?> config = Collections.emptyMap();
        TransportFactory transportFactory = TransportFactory.newTransportFactory(config);
        BridgeServiceFactory serviceFactory = new BridgeServiceFactory(transportFactory);
        ResourceAddressFactory addressFactory = newResourceAddressFactory();

        SchedulerProvider provider = new SchedulerProvider();

        SseAcceptor sseAcceptor = (SseAcceptor)transportFactory.getTransport("sse").getAcceptor();
        sseAcceptor.setBridgeServiceFactory(serviceFactory);
        sseAcceptor.setResourceAddressFactory(addressFactory);
        sseAcceptor.setSchedulerProvider(provider);

        HttpAcceptor httpAcceptor = (HttpAcceptor)transportFactory.getTransport("http").getAcceptor();
        httpAcceptor.setBridgeServiceFactory(serviceFactory);
        httpAcceptor.setResourceAddressFactory(addressFactory);
        httpAcceptor.setSchedulerProvider(provider);

        NioSocketAcceptor tcpAcceptor = (NioSocketAcceptor)transportFactory.getTransport("tcp").getAcceptor();
        tcpAcceptor.setSchedulerProvider(provider);
        tcpAcceptor.setResourceAddressFactory(addressFactory);
        tcpAcceptor.setBridgeServiceFactory(serviceFactory);

        String location = "sse://localhost:8000/path";
        Map<String, Object> options = new HashMap<>();
        options.put(format("http.%s", INJECTABLE_HEADERS.name()), emptySet());
        ResourceAddress sseAddress = addressFactory.newResourceAddress(location, options);

        this.tcpAcceptor = tcpAcceptor;
        this.httpAcceptor = httpAcceptor;
        this.sseAcceptor = sseAcceptor;

        this.sseAddress = sseAddress;
    }

    @After
    public void disposeAcceptor() {
        if (sseAcceptor != null) {
            sseAcceptor.dispose();
        }

        if (httpAcceptor != null) {
            httpAcceptor.dispose();
        }

        if (tcpAcceptor != null) {
            tcpAcceptor.dispose();
        }
    }

    @Specification("should.receive.location.and.message.event")
    @Test(timeout=5000)
    public void shouldReceiveLocationAndMessageEvent() throws Exception {

        final IoHandler handler = mockery.mock(IoHandler.class);
        @SuppressWarnings("unchecked")
        final IoFutureListener<IoFuture> writeListener = mockery.mock(IoFutureListener.class);

        mockery.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSession.class)));
                oneOf(handler).sessionOpened(with(any(IoSession.class)));
                final SimpleBufferAllocator allocator = SimpleBufferAllocator.BUFFER_ALLOCATOR;
                will(perform("$0.write(buf).addListener(listener);" 
                           + "$0.close(false);" 
                           + " return;")
                           .where("buf", allocator.wrap(ByteBuffer.wrap("Hello, world".getBytes(UTF_8))))
                           .where("listener", writeListener));
                // we no longer fire messageSent for performance reasons
                never(handler).messageSent(with(any(IoSession.class)), with(hasRemaining(12)));
                oneOf(writeListener).operationComplete(with(any(IoFuture.class)));
                oneOf(handler).sessionClosed(with(any(IoSession.class)));
            }


            public Matcher<IoBuffer> hasRemaining(final int remaining) {
                return new BaseMatcher<IoBuffer>() {

                    @Override
                    public boolean matches(Object item) {
                        IoBuffer buf = (IoBuffer) item;
                        return (buf.remaining() == remaining);
                    }

                    @Override
                    public void describeTo(Description description) {
                        description.appendText(format("buffer has %d remaining bytes", remaining));
                    }
                };
            }
        });

        // bind the address behavior
        sseAcceptor.bind(sseAddress, handler, null);

        robot.finish();
    }
}
