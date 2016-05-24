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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

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
import org.kaazing.test.util.MemoryAppender;
import org.kaazing.test.util.MethodExecutionTrace;

public class DownstreamIT {
    private static final Set<String> EMPTY_STRING_SET = Collections.emptySet();

    private K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/wse/downstream");

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
    @Specification("response.header.content.type.has.unexpected.value/downstream.response")
    public void shouldCloseConnectionWhenBinaryDownstreamResponseContentTypeHasUnexpectedValue() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(IOException.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(countDown(closed));
            }
        });
        connector.connect("ws://localhost:8080/path?query", null, handler).getSession();
        k3po.finish();
        closed.await(4, SECONDS);
        MemoryAppender.assertMessagesLogged(Collections.singletonList(".*nexpected.*type.*"), EMPTY_STRING_SET, null, false);
    }

    @Test
    @Specification("response.status.code.not.200/downstream.response")
    public void shouldCloseConnectionWhenBinaryDownstreamResponseStatusCodeNot200() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(IOException.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(countDown(closed));
            }
        });
        connector.connect("ws://localhost:8080/path?query", null, handler);
        k3po.finish();
        closed.await(4, SECONDS);
        MemoryAppender.assertMessagesLogged(Collections.singletonList(".*nexpected.*status.*"), EMPTY_STRING_SET, null, false);
    }

    @Test
    @Specification("server.send.frame.after.reconnect/downstream.response")
    public void shouldCloseConnectionWhenBinaryDownstreamResponseContainsFrameAfterReconnectFrame()
            throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(IOException.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(countDown(closed));
            }
        });
        connector.connect("ws://localhost:8080/path?query", null, handler);
        k3po.finish();
        closed.await(4, SECONDS);
        MemoryAppender.assertMessagesLogged(Collections.singletonList(".*received.*after reconnect.*"), EMPTY_STRING_SET, null, false);
    }

    // Only relevant for browser clients
    @Specification("request.header.origin/downstream.response")
    void shouldConnectWithDownstreamRequestOriginHeaderSet()
            throws Exception {
        k3po.finish();
    }

    // Server only test. Spec compliant clients ALWAYS use GET.
    @Specification("request.method.post/downstream.response")
    void serverShouldTolerateDownstreamRequestMethodPost()
            throws Exception {
        k3po.finish();
    }

    // Server only test. Spec compliant clients ALWAYS use GET.
    @Specification("request.method.post.with.body/downstream.response")
    void serverShouldTolerateDownstreamRequestMethodPostWithBody()
            throws Exception {
        k3po.finish();
    }

    // Server only test. Spec compliant clients ALWAYS use GET.
    @Specification("request.method.not.get.or.post/downstream.response")
    void shouldRespondWithBadRequestWhenDownstreamRequestMethodNotGetOrPost()
            throws Exception {
        k3po.finish();
    }

    // Server only test. Spec compliant clients do not do this.
    @Specification("request.out.of.order/downstream.response")
    void shouldCloseConnectionWhenBinaryDownstreamRequestIsOutOfOrder() throws Exception {
        k3po.finish();
    }

    // Server only test. Spec compliant clients do not do this.
    @Specification("subsequent.request.out.of.order/response")
    void shouldCloseConnectionWhenSubsequentBinaryDownstreamRequestIsOutOfOrder() throws Exception {
        k3po.finish();
    }

    // Not relevant, WsebConnector always uses /cbm (mixed frames binary encoding)
    @Specification("text.encoding/response.header.content.type.has.unexpected.value/downstream.response")
    void shouldCloseConnectionWhenTextDownstreamResponseContentTypeHasUnexpectedValue()
            throws Exception {
        k3po.finish();
    }

    // Not relevant, WsebConnector always uses /cbm (mixed frames binary encoding)
    @Specification("text.escape.encoding/response.header.content.type.has.unexpected.value/downstream.response")
    void shouldCloseConnectionWhenEscapedTextDownstreamResponseContentTypeHasUnexpectedValue()
            throws Exception {
        k3po.finish();
    }

    // Not relevant, WsebConnector always uses /cbm (mixed frames binary encoding)
    @Specification("binary.frames.only/response.header.content.type.has.unexpected.value/downstream.response")
    void shouldCloseConnectionWhenBinaryFramesOnlyBinaryDownstreamResponseContentTypeHasUnexpectedValue()
            throws Exception {
        k3po.finish();
    }

    // Not relevant, WsebConnector always uses /cbm (mixed frames binary encoding)
    @Specification("binary.frames.only/text.encoding/response.header.content.type.has.unexpected.value/downstream.response")
    void shouldCloseConnectionWhenBinaryFramesOnlyTextDownstreamResponseContentTypeHasUnexpectedValue()
            throws Exception {
        k3po.finish();
    }

    // Not relevant, WsebConnector always uses /cbm (mixed frames binary encoding)
    @Specification("binary.frames.only/text.escaped.encoding/response.header.content.type.has.unexpected.value/downstream.response")
    void shouldCloseConnectionWhenBinaryFramesOnlyTextEscapedDownstreamResponseContentTypeHasUnexpectedValue()
            throws Exception {
        k3po.finish();
    }
}
