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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.service.ServiceFactory;
import org.kaazing.gateway.util.feature.EarlyAccessFeatures;
import org.kaazing.test.util.MethodExecutionTrace;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;

public class HttpProxyServiceTest {
    @Rule
    public TestRule testExecutionTrace = new MethodExecutionTrace();

    @Test
    public void testCreateService() throws Exception {
        HttpProxyService service = (HttpProxyService)ServiceFactory.newServiceFactory().newService("http.proxy");
        Assert.assertNotNull("Failed to create http.proxy service", service);
    }

    @Test
    public void shouldFailGatewayStartupAccepAndConnectDifferentEndingSlashes() throws Exception {
        Gateway gateway = new Gateway();
        // @formatter:off
        GatewayConfiguration configuration =
                new GatewayConfigurationBuilder()
                    .property(EarlyAccessFeatures.HTTP_PROXY_SERVICE.getPropertyName(), "true")
                    .service()
                        .accept("http://localhost:8080/a")
                        .connect("http://localhost:8081/")
                        .name("Proxy Service")
                        .type("http.proxy")
                        .connectOption("http.keepalive", "disabled")
                    .done()
                .done();
        // @formatter:on
        try {
            gateway.start(configuration);
            throw new AssertionError("Gateway should fail to start. IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue("Wrong error message", e.getMessage().contains(
                    "The accept URI is 'http://localhost:8080/a' and the connect URI is 'http://localhost:8081/'. "
                            + "One has a trailing slash and one doesn't. Both URIs either need to include a trailing slash or omit it."));
        } finally {
            gateway.stop();
        }
    }

    @Test
    public void shouldFailGatewayStartupWhenEarlyAccessFeaturePropertyNotSet() throws Exception {
        Gateway gateway = new Gateway();
        // @formatter:off
        GatewayConfiguration configuration =
                new GatewayConfigurationBuilder()
                    .service()
                        .accept("http://localhost:8080/a")
                        .connect("http://localhost:8081/")
                        .name("Proxy Service")
                        .type("http.proxy")
                        .connectOption("http.keepalive", "disabled")
                    .done()
                .done();
        // @formatter:on
        try {
            gateway.start(configuration);
            throw new AssertionError("Gateway should fail to start because early access feature not enabled.");
        } catch (UnsupportedOperationException e) {
            Assert.assertTrue("Wrong error message: " + e.getMessage(), e.getMessage().matches(
                    "Feature \"http.proxy\".*not enabled"));
        } finally {
            gateway.stop();
        }
    }

}
