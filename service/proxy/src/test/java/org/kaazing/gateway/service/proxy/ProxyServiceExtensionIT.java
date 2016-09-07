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
package org.kaazing.gateway.service.proxy;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.netx.URLConnectionHelper;
import org.kaazing.test.util.ITUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import static java.util.stream.Collectors.joining;

public class ProxyServiceExtensionIT {
    private static ClassLoader classLoader;

    private final K3poRule k3po = new K3poRule();

    private final GatewayRule gateway = new GatewayRule() {
        {
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                .service()
                    .accept("tcp://localhost:8001")
                    .connect("tcp://localhost:8002")
                    .type("proxy")
                    .crossOrigin()
                        .allowOrigin("*")
                    .done()
                .done()
            .done();

            init(configuration);
        }
    };

    @Rule
    public TestRule chain = ITUtil.createRuleChain(gateway, k3po);

    @BeforeClass
    public static void before() throws Exception {
        classLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new TestClassLoader(TestExtension.class.getName()));
    }

    @AfterClass
    public static void after() {
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    @Specification("shouldInjectBytesBeforeForwardingMessages")
    @Test
    public void shouldInjectBytesBeforeForwardingMessages() throws Exception {
        k3po.finish();
    }

    /**
     * A classloader whose getResources("META-INF/services/org.kaazing.gateway.service.proxy.ProxyServiceExtensionSpi")
     * method will return a URL whose contents will be the list of class names supplied in the constructor.
     * This avoids the need for test meta-info resources files to be available on the test class path.
     */
    private static class TestClassLoader extends ClassLoader {
        private final List<URL> urls;

        TestClassLoader(String... factorySpiClassNames) throws IOException {
            URLConnectionHelper helper = URLConnectionHelper.newInstance();
            String contents = Arrays.stream(factorySpiClassNames).collect(joining("\n"));
            URI uri = URI.create("data:," + contents);
            URL url = helper.toURL(uri);
            urls = Collections.singletonList(url);
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            if (name.equals("META-INF/services/" + ProxyServiceExtensionSpi.class.getName())) {
                return Collections.enumeration(urls);
            }
            return super.getResources(name);
        }

    }

}
