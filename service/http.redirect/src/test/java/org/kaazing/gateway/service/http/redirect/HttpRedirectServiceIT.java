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
package org.kaazing.gateway.service.http.redirect;

import static org.kaazing.gateway.util.feature.EarlyAccessFeatures.HTTP_REDIRECT;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class HttpRedirectServiceIT {

    private static final String REDIRECT_SERVICE_ACCEPT = "http://localhost:8000/";

    private final K3poRule robot = new K3poRule().setScriptRoot("org/kaazing/specification/http/rfc7231/redirection");

    private final GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                .property(HTTP_REDIRECT.getPropertyName(), "true")
                        .service()
                            .accept(REDIRECT_SERVICE_ACCEPT)
                            .type("http.redirect")
                            .property("location", "https://www.google.com")
                            .property("status-code", "301")
                            .property("cache-control", "no-store, no-cache, must-revalidate")
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, robot);

    @Test
    @Specification({"secure.location/request"})
    public void shouldAcceptSecureLocation() throws Exception {
        robot.finish();
    }

}
