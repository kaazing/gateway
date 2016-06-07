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

import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.kaazing.gateway.transport.wseb.test.WsebConnectorRule;
import org.kaazing.gateway.util.InternalSystemProperty;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MethodExecutionTrace;

public class UpstreamIT {

    private K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/wse/upstream");

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

    // Server only test
    @Specification("request.method.not.post/upstream.response")
    void shouldCloseConnectionWhenUpstreamRequestMethodNotPost()
            throws Exception {
        k3po.finish();
    }

    // Not relevant to WsebConnector because it does upstreaming (so will not see the upstream
    // response status until the wseb session is closed). Case where server closes upstream
    // transport abruptly is covered in ClosingIT.
    @Specification("response.status.code.not.200/upstream.response")
    void shouldCloseConnectionWhenUpstreamStatusCodeNot200() throws Exception {
        k3po.finish();
    }

    // Not applicable to WsebConnector (because it does upstreaming)
    @Specification("client.send.overlapping.request/upstream.response")
    void shouldRejectParallelUpstreamRequest() throws Exception {
        k3po.finish();
    }

    // Server test only
    @Specification("request.out.of.order/upstream.response")
    void shouldRejectOutOfOrderUpstreamRequest() throws Exception {
        k3po.finish();
    }

    // Server test only
    @Specification("subsequent.request.out.of.order/request")
    void shouldCloseConnectionWhenSubsequentUpstreamRequestIsOutOfOrder() throws Exception {
        k3po.finish();
    }

    // Not applicable for WsebConnector (which does a single chunked upstream request)
    @Specification("client.send.multiple.requests/upstream.response")
    void shouldAllowMultipleSequentialUpstreamRequests() throws Exception {
        k3po.finish();
    }

    // Server test only
    @Specification("zero.content.length.request/upstream.response")
    public void shouldRejectZeroContentLengthUpstreamRequest() throws Exception {
        k3po.finish();
    }
}
