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

package org.kaazing.gateway.service.http.directory.cachecontrol;

import static org.junit.rules.RuleChain.outerRule;

import java.io.File;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

/**
 * RFC-7234, section 5.3 Expires
 */
public class ExpiresIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/http/rfc7234/expires");

    private final GatewayRule gateway = new GatewayRule() {
        {
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .webRootDirectory(new File("src/test/webapp"))
                        .service()
                            .accept("http://localhost:8000/")
                            .type("directory")
                            .property("directory", "/public")
                            .property("welcome-file", "index.html")
                            .nestedProperty("location")
                                .property("patterns", "**/*")
                                .property("cache-control", "max-age=100")
                            .done()
                        .done()
                    .done();
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = outerRule(k3po).around(gateway);

    @Ignore("https://github.com/kaazing/gateway/issues/383")
    @Test
    @Specification({"already.expired.conditional.request.304/request"})
    public void shouldReceiveOKWhenCacheResponseExpiredForUnconditionalRequest() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({"already.expired.unconditional.request.200/request"})
    public void shouldReceiveNotModifiedWhenCacheResponseExpiredForConditionalRequest() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({"fresh.response.from.cache/request"})
    public void shouldReceiveUnexpiredResponseFromCache() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({"ignored.when.multiple.expires.200/request"})
    public void shouldIgnoreMultipleExpiresHeaderInResponse() throws Exception {
        k3po.finish();
    }

    @Ignore("https://github.com/kaazing/gateway/issues/383")
    @Test
    @Specification({"invalid.date.conditional.request.304/request"})
    public void shouldReceiveNotModifiedWhenCachedResponseIsStaleDueToInvalidDateForConditionalRequest() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({"invalid.date.unconditional.request.200/request"})
    public void shouldReceiveOKWhenCachedResponseIsStaleDueToInvalidDateForUnconditionalRequest() throws Exception {
        k3po.finish();
    }

    @Ignore("https://github.com/kaazing/gateway/issues/383")
    @Test
    @Specification({"stale.response.conditional.request.304/request"})
    public void shouldReceiveNotModifiedWhenCachedResponseIsStaleForConditionalRequest() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({"stale.response.unconditional.request.200/request"})
    public void shouldReceiveOKWhenCachedResponseIsStaleForUnconditionalRequest() throws Exception {
        k3po.finish();
    }
}
