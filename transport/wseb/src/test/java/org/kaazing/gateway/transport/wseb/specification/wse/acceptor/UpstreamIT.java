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
package org.kaazing.gateway.transport.wseb.specification.wse.acceptor;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kaazing.gateway.util.InternalSystemProperty.WSE_SPECIFICATION;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.util.InternalSystemProperty;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.MethodExecutionTrace;

public class UpstreamIT {

    private TestRule trace = new MethodExecutionTrace();
    private K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/wse/upstream");
    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    private GatewayRule gateway = new GatewayRule() {
        {
         // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .property(WSE_SPECIFICATION.getPropertyName(), "true")
                        .property(InternalSystemProperty.WS_CLOSE_TIMEOUT.getPropertyName(), "2s")
                        .service()
                            .accept("wse://localhost:8080/path")
                            .type("echo")
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(gateway).around(k3po).around(timeout);

    @Test
    @Specification("request.method.not.post/upstream.request")
    public void shouldCloseConnectionWhenUpstreamRequestMethodNotPost()
            throws Exception {
        k3po.finish();
    }

    // Client test only
    @Specification("response.status.code.not.200/upstream.request")
    void shouldCloseConnectionWhenUpstreamStatusCodeNot200()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification("client.send.overlapping.request/upstream.request")
    public void shouldRejectParallelUpstreamRequest() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification("request.out.of.order/upstream.request")
    public void shouldRejectOutOfOrderUpstreamRequest() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification("subsequent.request.out.of.order/request")
    public void shouldCloseConnectionWhenSubsequentUpstreamRequestIsOutOfOrder() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification("client.send.multiple.requests/upstream.request")
    public void shouldAllowMultipleSequentialUpstreamRequests() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification("zero.content.length.request/upstream.request")
    public void shouldRejectZeroContentLengthUpstreamRequest() throws Exception {
        k3po.finish();
    }
}
