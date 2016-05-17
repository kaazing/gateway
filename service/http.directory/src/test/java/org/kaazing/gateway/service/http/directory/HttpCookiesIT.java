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
package org.kaazing.gateway.service.http.directory;

import static org.junit.rules.RuleChain.outerRule;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class HttpCookiesIT {

    private final GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .webRootDirectory(new File("src/test/webapp"))
                        .service()
                            .accept("http://localhost:8000/directory")
                            .type("directory")
                            .property("directory", "/public")
                            .property("welcome-file", "index.html")
                        .done()
                .done();
            // @formatter:on
            init(configuration);
        }
    };
    
    private final TestRule timeout = new DisableOnDebug(new Timeout(10, TimeUnit.SECONDS));

    private final K3poRule robot = new K3poRule();
    
    @Rule
    public TestRule chain = outerRule(robot).around(gateway).around(timeout);

    @Test
    @Specification("get.cookies.should.return.none")
    public void getCookiesShouldReturnNone() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("set.cookies.should.return.set.cookie.headers")
    public void setCookiesShouldReturnSetCookieHeaders() throws Exception {
        robot.finish();
    }

}
