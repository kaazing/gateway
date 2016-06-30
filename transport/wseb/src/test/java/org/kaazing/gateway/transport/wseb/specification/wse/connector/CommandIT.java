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
package org.kaazing.gateway.transport.wseb.specification.wse.connector;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kaazing.test.util.ITUtil.timeoutRule;

import java.util.Properties;

import org.apache.mina.core.service.IoHandler;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.gateway.transport.wseb.test.WsebConnectorRule;
import org.kaazing.gateway.util.InternalSystemProperty;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MethodExecutionTrace;

public class CommandIT {

    private K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/wse/command");

    private final WsebConnectorRule connector;

    {
        Properties configuration = new Properties();
        configuration.setProperty(InternalSystemProperty.WS_CLOSE_TIMEOUT.getPropertyName(), "1s");
        connector = new WsebConnectorRule(configuration);
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
    // contextRule after k3po so we don't choke on exceptionCaught happening when k3po closes connections
    public TestRule chain = RuleChain.outerRule(trace).around(connector).around(k3po).around(contextRule)
            .around(timeoutRule);

    @Test
    @Specification("server.send.nop/response")
    public void shouldReceiveNop() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });
        connector.connect("ws://localhost:8080/path?query", null, handler);
        k3po.finish();
    }

    @Test
    @Specification("server.send.reconnect/response")
    public void shouldReceiveReconnect() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });
        connector.connect("ws://localhost:8080/path?query", null, handler);
        k3po.finish();
    }

    @Test
    @Specification("server.send.close/response")
    public void shouldReceiveClose() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });
        connector.connect("ws://localhost:8080/path?query", null, handler);
        k3po.finish();
    }

}
