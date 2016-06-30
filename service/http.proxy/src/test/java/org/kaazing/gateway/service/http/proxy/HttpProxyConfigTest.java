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

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.config.parse.GatewayConfigParser;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.util.feature.EarlyAccessFeatures;
import org.kaazing.test.util.MethodExecutionTrace;

public class HttpProxyConfigTest {

    @Rule
    public final TestRule trace = new MethodExecutionTrace();

    @Test
    public void testHttpProxyConfig() throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL configUrl = classLoader.getResource("http-proxy-config.xml");
        if (configUrl == null) {
            throw new FileNotFoundException("File http-proxy-config.xml not found");
        }
        URI file = configUrl.toURI();

        GatewayConfigParser parser = new GatewayConfigParser();
        parser.parse(new File(file));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnknownSimpleProperties() throws Exception {
        // @formatter:off
        GatewayConfiguration configuration =
                new GatewayConfigurationBuilder()
                        .property(EarlyAccessFeatures.HTTP_PROXY_SERVICE.getPropertyName(), "true")
                        .service()
                            .name("simple")
                            .accept("http://localhost:8110")
                            .connect("http://localhost:8080")
                            .type("http.proxy")
                            .property("foo", "enabled")
                            .property("bar", "100")
                        .done()
                .done();
        // @formatter:on
        Gateway gateway = new Gateway();
        try {
            gateway.start(configuration);
        } finally {
            gateway.stop();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnknownNestedProperties() throws Exception {
        // @formatter:off
        GatewayConfiguration configuration =
                new GatewayConfigurationBuilder()
                        .property(EarlyAccessFeatures.HTTP_PROXY_SERVICE.getPropertyName(), "true")
                        .service()
                            .name("nested")
                            .accept("http://localhost:8110")
                            .connect("http://localhost:8080")
                            .type("http.proxy")
                            .nestedProperty("cookie-domain-mapping1")
                                .property("from", "a.b")
                                .property("to", "c.d")
                            .done()
                            .property("rewrite-cookie-path", "enabled")
                            .nestedProperty("cookie-path-mapping2")
                                .property("from", "/foo/")
                                .property("to", "/bar/")
                            .done()
                        .done()
                .done();
        // @formatter:on
        Gateway gateway = new Gateway();
        try {
            gateway.start(configuration);
        } finally {
            gateway.stop();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUseForwardedPropertyValues() throws Exception {
        // @formatter:off
        GatewayConfiguration configuration =
                new GatewayConfigurationBuilder()
                        .property(EarlyAccessFeatures.HTTP_PROXY_SERVICE.getPropertyName(), "true")
                        .service()
                            .name("useForwarded")
                            .accept("http://localhost:8110")
                            .connect("http://localhost:8080")
                            .type("http.proxy")
                            .property("use-forwarded", "delete")
                            .property("use-forwarded", "100")
                        .done()
                .done();
        // @formatter:on
        Gateway gateway = new Gateway();
        try {
            gateway.start(configuration);
        } finally {
            gateway.stop();
        }
    }

}
