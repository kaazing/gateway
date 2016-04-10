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

import static org.kaazing.gateway.util.InternalSystemProperty.WSE_SPECIFICATION;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class DownstreamIT {

    private K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/wse/downstream");

    private GatewayRule gateway = new GatewayRule() {
        {
         // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .property(WSE_SPECIFICATION.getPropertyName(), "true")
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
    public TestRule chain = createRuleChain(gateway, k3po);

    // Client test only
    @Specification("response.header.content.type.has.unexpected.value/downstream.response")
    void shouldCloseConnectionWhenBinaryDownstreamResponseContentTypeHasUnexpectedValue()
            throws Exception {
        k3po.finish();
    }

    // Client test only
    @Specification("response.status.code.not.200/downstream.response")
    void shouldCloseConnectionWhenBinaryDownstreamResponseStatusCodeNot200()
            throws Exception {
        k3po.finish();
    }

    // Client test only
    @Specification("server.send.frame.after.reconnect/downstream.response")
    void shouldCloseConnectionWhenBinaryDownstreamResponseContainsFrameAfterReconnectFrame()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification("request.header.origin/downstream.request")
    public void shouldConnectWithDownstreamRequestOriginHeaderSet()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification("request.method.post/downstream.request")
    public void serverShouldTolerateDownstreamRequestMethodPost()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification("request.method.post.with.body/downstream.request")
    public void serverShouldTolerateDownstreamRequestMethodPostWithBody()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification("request.method.not.get.or.post/downstream.request")
    public void shouldRespondWithBadRequestWhenDownstreamRequestMethodNotGetOrPost() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification("request.out.of.order/downstream.request")
    public void shouldCloseConnectionWhenBinaryDownstreamRequestIsOutOfOrder() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification("subsequent.request.out.of.order/request")
    public void shouldCloseConnectionWhenSubsequentBinaryDownstreamRequestIsOutOfOrder() throws Exception {
        k3po.finish();
    }

    // Client test only
    @Specification("text.encoding/response.header.content.type.has.unexpected.value/downstream.response")
    void shouldCloseConnectionWhenTextDownstreamResponseContentTypeHasUnexpectedValue()
            throws Exception {
        k3po.finish();
    }

    // Client test only
    @Specification("text.escaped.encoding/response.header.content.type.has.unexpected.value/downstream.response")
    void shouldCloseConnectionWhenEscapedTextDownstreamResponseContentTypeHasUnexpectedValue()
            throws Exception {
        k3po.finish();
    }

    // Client test only
    @Specification("binary.frames.only/binary.encoding/response.header.content.type.has.unexpected.value/downstream.request")
    void shouldCloseConnectionWhenBinaryFramesOnlyBinaryDownstreamResponseContentTypeHasUnexpectedValue()
            throws Exception {
        k3po.finish();
    }

    // Client test only
    @Specification("binary.frames.only/text.encoding/response.header.content.type.has.unexpected.value/downstream.request")
    void shouldCloseConnectionWhenBinaryFramesOnlyTextDownstreamResponseContentTypeHasUnexpectedValue()
            throws Exception {
        k3po.finish();
    }

    // Client test only
    @Specification("binary.frames.only/text.escaped.encoding/response.header.content.type.has.unexpected.value/downstream.request")
    void shouldCloseConnectionWhenBinaryFramesOnlyTextEscapedDownstreamResponseContentTypeHasUnexpectedValue()
            throws Exception {
        k3po.finish();
    }

}
