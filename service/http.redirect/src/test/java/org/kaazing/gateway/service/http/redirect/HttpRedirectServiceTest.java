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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.context.resolve.DefaultServiceProperties;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.ServiceFactory;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.util.feature.EarlyAccessFeatures;
import org.kaazing.test.util.MethodExecutionTrace;
import org.slf4j.Logger;

public class HttpRedirectServiceTest {
    @Rule
    public TestRule testExecutionTrace = new MethodExecutionTrace();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    Mockery context = new Mockery();
    @Test
    public void testCreateService() throws Exception {
        HttpRedirectService service = (HttpRedirectService) ServiceFactory.newServiceFactory().newService("http.redirect");
        Assert.assertNotNull("Failed to create HttpRedirectService", service);
    }

    @Test
    public void shouldFailWhenEarlyAccessNotEnabled() throws Exception {
        thrown.expect(UnsupportedOperationException.class);
        thrown.expectMessage("Feature \"http.redirect\" (HTTP Redirect Service) not enabled");
        Gateway gateway = new Gateway();
        // @formatter:off
        GatewayConfiguration configuration =
                new GatewayConfigurationBuilder()
                        .property(EarlyAccessFeatures.HTTP_REDIRECT.getPropertyName(), "false")
                        .service()
                            .accept("http://localhost:8080/")
                            .name("foo")
                            .type("http.redirect")
                            .property("location", "https://www.google.com")
                            .property("status-code", "301")
                            .property("cache-control", "no-store, no-cache, must-revalidate")
                        .done()
                    .done();
        // @formatter:on
        try {
            gateway.start(configuration);
        } finally {
            gateway.stop();
        }
    }

    @Test
    public void shouldFailWithNonRedirectStatusCode() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Wrong value for status-code:404. Valid values are integers between 300 and 399");
        Gateway gateway = new Gateway();
        // @formatter:off
        GatewayConfiguration configuration =
                new GatewayConfigurationBuilder()
                        .property(EarlyAccessFeatures.HTTP_REDIRECT.getPropertyName(), "true")
                        .service()
                            .accept("http://localhost:8080")
                            .name("foo")
                            .type("http.redirect")
                            .property("location", "https://www.google.com")
                            .property("status-code", "404")
                            .property("cache-control", "no-store, no-cache, must-revalidate")
                        .done()
                    .done();
        // @formatter:on
        try {
            gateway.start(configuration);
        } finally {
            gateway.stop();
        }
    }
    @Test
    public void shouldFailWithNullLocation() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Missing required property: location");
        Gateway gateway = new Gateway();
        // @formatter:off
        GatewayConfiguration configuration =
                new GatewayConfigurationBuilder()
                        .property(EarlyAccessFeatures.HTTP_REDIRECT.getPropertyName(), "true")
                        .service()
                            .accept("http://localhost:8080/")
                            .name("foo")
                            .type("http.redirect")
                            .property("location", null)
                            .property("status-code", "301")
                            .property("cache-control", "no-store, no-cache, must-revalidate")
                        .done()
                    .done();
        // @formatter:on
        try {
            gateway.start(configuration);
        } finally {
            gateway.stop();
        }
    }

    @Test
    public void shouldPassWithDefaultConnectPath() throws Exception {
        Gateway gateway = new Gateway();
        // @formatter:off
        GatewayConfiguration configuration =
                new GatewayConfigurationBuilder()
                        .property(EarlyAccessFeatures.HTTP_REDIRECT.getPropertyName(), "true")
                        .service()
                            .accept("http://localhost:8080/")
                            .name("foo")
                            .type("http.redirect")
                            .property("location", "https://www.google.com")
                            .property("status-code", "301")
                            .property("cache-control", "no-store, no-cache, must-revalidate")
                        .done()
                    .done();
        // @formatter:on
        try {
            gateway.start(configuration);
        } finally {
            gateway.stop();
        }
    }

    @Test
    public void shouldUse303AsDefaultStatusCode() throws Exception {
        HttpRedirectService service = createRedirectService();

        DefaultServiceProperties sp = new DefaultServiceProperties();
        sp.put("location", "https://www.google.com");
        sp.put("cache-control", "no-store, no-cache, must-revalidate");

        ServiceContext serviceContext = createServiceContext(sp);
        service.init(serviceContext);
        HttpRedirectServiceHandler h = service.getHandler();
        assertEquals(HttpStatus.REDIRECT_MULTIPLE_CHOICES, h.getStatusCode());
    }

    @Test
    public void testServiceHandlerToString() throws Exception {
        HttpRedirectService service = createRedirectService();

        DefaultServiceProperties sp = new DefaultServiceProperties();
        sp.put("location", "https://www.google.com");
        sp.put("status-code", "305");
        sp.put("cache-control", "no-store, no-cache, must-revalidate");

        ServiceContext serviceContext = createServiceContext(sp);
        service.init(serviceContext);
        HttpRedirectServiceHandler h = service.getHandler();
        assertEquals(
                "HttpRedirectServiceHandler [location='https://www.google.com', cache-control='no-store, no-cache, must-revalidate', status-code=305]",
                h.toString());
    }
    private ServiceContext createServiceContext(DefaultServiceProperties sp) {
        ServiceContext serviceContext = context.mock(ServiceContext.class);
        Logger logger = context.mock(Logger.class);
        Collection<String> accepts = new ArrayList<>();
        accepts.add("http://localhost:8080");
        // expectations
        context.checking(new Expectations() {
            {
                oneOf(serviceContext).getLogger();
                will(returnValue(logger));
                oneOf(serviceContext).getAccepts();
                will(returnValue(accepts));
                oneOf(serviceContext).getProperties();
                will(returnValue(sp));
            }
        });
        return serviceContext;
    }

    @Test
    public void shouldFailWithNonHttpAccept() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(
                "The accept URI ws://localhost:8080/ for service foo needs to start either with http: or with https:");
        Gateway gateway = new Gateway();
        // @formatter:off
        GatewayConfiguration configuration =
                new GatewayConfigurationBuilder()
                        .property(EarlyAccessFeatures.HTTP_REDIRECT.getPropertyName(), "true")
                        .service()
                            .accept("ws://localhost:8080/")
                            .name("foo")
                            .type("http.redirect")
                            .property("location", "http://localhost:8080/")
                            .property("status-code", "301")
                            .property("cache-control", "no-store, no-cache, must-revalidate")
                        .done()
                    .done();
        // @formatter:on
        try {
            gateway.start(configuration);
        } finally {
            gateway.stop();
        }
    }
    private HttpRedirectService createRedirectService() {
        Properties properties = new Properties();
        properties.setProperty("feature.http.redirect", "True");
        HttpRedirectService service = new HttpRedirectService();
        service.setConfiguration(properties);
        return service;
    }

}