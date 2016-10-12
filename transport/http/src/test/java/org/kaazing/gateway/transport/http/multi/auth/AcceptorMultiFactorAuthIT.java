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
package org.kaazing.gateway.transport.http.multi.auth;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.rules.RuleChain.outerRule;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.resource.address.http.HttpOriginSecurity;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.server.context.resolve.DefaultCrossSiteConstraintContext;
import org.kaazing.gateway.transport.http.HttpAcceptorRule;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

// TODO, work in progress
public class AcceptorMultiFactorAuthIT {
    private final K3poRule k3po = new K3poRule()
            .setScriptRoot("org/kaazing/specification/http/multi/auth");

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));
    
    private final HttpAcceptorRule acceptor = new HttpAcceptorRule();

    @Rule
    public final TestRule chain = outerRule(acceptor).outerRule(k3po).around(timeout);

    @Test
    @Specification({
        "request.with.secure.challenge.identity/server",
        })
    public void serverMayGiveSecChallengeIdentityHeaderWith401() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({"response.with.secure.challenge.identity/server"})
    public void clientShouldAttachSecChallengeIdentityToFollowingRequests() throws Exception {
        k3po.finish();
    }

    private static ResourceAddress httpAddress() {
        Map<String, DefaultCrossSiteConstraintContext> constraints = new HashMap<>();
        DefaultCrossSiteConstraintContext constraintContext =
                new DefaultCrossSiteConstraintContext("http://source.example.com:80", "GET,POST", null, null);
        constraints.put(constraintContext.getAllowOrigin(), constraintContext);
        HttpOriginSecurity httpOriginSecuirty = new HttpOriginSecurity(constraints);

        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        ResourceOptions options = ResourceOptions.FACTORY.newResourceOptions();
        options.setOption(HttpResourceAddress.ORIGIN_SECURITY, httpOriginSecuirty);
        return addressFactory.newResourceAddress("http://localhost:8000/path", options);
    }
}
