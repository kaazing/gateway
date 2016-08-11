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
package org.kaazing.gateway.service.http.proxy;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.util.feature.EarlyAccessFeatures;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class HttpProxyChunkingIT {

    private final K3poRule robot = new K3poRule();

    private final GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .property(EarlyAccessFeatures.HTTP_PROXY_SERVICE.getPropertyName(), "true")
                        .service()
                            .accept("http://localhost:8110")
                            .connect("http://localhost:8080")
                            .type("http.proxy")
                            .connectOption("http.keepalive", "disabled")
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, robot);

    @Specification("http.proxy.payload.complete.chunked.encoding")
    @Test
    public void completeChunkedEncoding() throws Exception {
        robot.finish();
    }

    @Specification("http.proxy.gzip.chunked.encoding")
    @Test
    public void gzipChunkedEncoding() throws Exception {
        robot.finish();
    }

    @Specification("http.proxy.payload.image.encoding")
    @Test
    public void imageChunkedEncoding() throws Exception {
        robot.finish();
    }

    @Specification("http.proxy.payload.binary.encoding")
    @Test
    public void binaryDataChunkedEncoding() throws Exception {
        robot.finish();
    }

}
