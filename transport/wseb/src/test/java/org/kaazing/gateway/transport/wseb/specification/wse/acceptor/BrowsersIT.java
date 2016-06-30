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

public class BrowsersIT {

    private K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/wse/browsers");

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
            init(configuration, "log4j-trace.properties");
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, k3po);

    @Test
    @Specification("client.reconnect.downstream/request")
    public void serverShouldSendReconnectFrameAfterDetectingNewDownstreamRequestFromClient()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification("client.send.kb.parameter.in.downstream.request/request")
    public void serverShouldSendReconnectFrameAfterRequestedClientBufferSizeIsExceeded()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification("client.request.padded.response/request")
    public void serverShouldSendPaddingInDownstream() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification("client.send.ksn.parameter/request")
    public void serverShouldReadRequestSequenceNumberFromQueryParameter() throws Exception {
        k3po.finish();
    }
}
