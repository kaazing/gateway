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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.util.feature.EarlyAccessFeatures;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

import static org.kaazing.test.util.ITUtil.createRuleChain;

public class HttpProxyAuthenticationIT {

    private final K3poRule robot = new K3poRule();

    private final GatewayRule gateway = new GatewayRule() {{
        // @formatter:off
        GatewayConfiguration configuration =
                new GatewayConfigurationBuilder()
                    .property(EarlyAccessFeatures.HTTP_PROXY_SERVICE.getPropertyName(), "true")
                    .service()
                        .accept("http://localhost:8110")
                        .connect("http://localhost:8080")
                        .type("http.proxy")
                    .done()
                .done();
        // @formatter:on
        init(configuration);
    }};

    @Rule
    public TestRule chain = createRuleChain(gateway, robot);

    @Specification("auth/http.proxy.auth.forbidden.status.code")
    @Test
    public void shouldRespondWithForbiddenStatusCode() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("auth/http.proxy.auth.invalid.then.valid.credentials")
    public void shouldRespondWithUnauthorizedStatusCodeWithInvalidCredentials() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("auth/http.proxy.auth.missing.then.valid.credentials")
    public void shouldRespondWithUnauthorizedStatusCodeWithMissingCredentials() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("auth/http.proxy.auth.partial.then.valid.credentials")
    public void shouldRespondWithUnauthorizedStatusCodeWithPartialCredentials() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("auth/http.proxy.auth.authentication")
    public void shouldPassWithProxyAuthentication() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("auth/http.proxy.auth.header.invalid.user")
    public void shouldRespond401ToInvalidUser() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("auth/http.proxy.auth.header.invalid.proxy.user")
    public void secureProxyShouldSend407ToAnyUnAuthorizedRequest() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("auth/http.proxy.auth.status.multiple.requests.with.invalid.credentials")
    public void shouldRespondWithMultiple401sWithMultipleInvalidRequests() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("auth/http.proxy.auth.status.valid.credentials")
    public void shouldRespond200WithValidCredentials() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("auth/http.proxy.auth.status.challenge.with.proxy.authorization.header")
    public void proxyMustNotAlterAuthenticationHeader() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("auth/http.proxy.auth.status.challenge.with.proxy.authenticate.header")
    public void proxyMustNotModifyWWWAuthenticateHeader() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("auth/http.proxy.auth.authentication.digest")
    public void authenticationDigest() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("auth/http.proxy.auth.authentication.basic")
    public void authenticationBasic() throws Exception {
        robot.finish();
    }

}
