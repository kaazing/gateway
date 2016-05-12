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
package org.kaazing.gateway.transport.wseb;

import org.apache.mina.core.service.IoHandler;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.gateway.transport.wseb.test.WsebAcceptorRule;
import org.kaazing.gateway.util.InternalSystemProperty;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MethodExecutionTrace;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertNull;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.INACTIVITY_TIMEOUT;
import static org.kaazing.test.util.ITUtil.timeoutRule;

public class WsebIdleTimeoutExtensionIT {

    private final K3poRule k3po = new K3poRule();

    private final WsebAcceptorRule acceptor;

    {
        Properties configuration = new Properties();
        configuration.setProperty(InternalSystemProperty.WS_CLOSE_TIMEOUT.getPropertyName(), "2s");

        acceptor = new WsebAcceptorRule(configuration);
    }

    private final JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private final TestRule contextRule = ITUtil.toTestRule(context);
    private final TestRule trace = new MethodExecutionTrace();
    private final TestRule timeoutRule = timeoutRule(5, SECONDS);

    @Rule
    public final TestRule chain = RuleChain.outerRule(trace).around(acceptor).around(contextRule).
            around(k3po).around(timeoutRule);

    private final ResourceAddressFactory resourceAddressFactory = ResourceAddressFactory.newResourceAddressFactory();


    @Test
    @Specification("should.negotiate.idle.timeout.extension")
    public void shouldNegotiateIdleTimeoutExtension() throws Exception {

        Map<String, Object> options = new HashMap<>();
        options.put(INACTIVITY_TIMEOUT.name(), 10000L);
        ResourceAddress address = resourceAddressFactory.newResourceAddress("wse://localhost:8080/path", options);
        final AtomicReference<WsebSession> session = new  AtomicReference<>();

        acceptor.bind(address, mockHandler(session));
        k3po.finish();

        // IdleTimeoutFilter shouldn't be in transport session filter chain for wseb
        assertNull(session.get().getTransportSession().getFilterChain().get("x-kaazing-idle-timeout"));
    }

    private IoHandler mockHandler(AtomicReference<WsebSession> session) throws Exception {
        IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(WsebSession.class)));
                oneOf(handler).sessionOpened(with(any(WsebSession.class)));
                will(saveParameter(session, 0));
            }
        });
        return handler;
    }

}
