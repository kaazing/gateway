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
package org.kaazing.gateway.transport.nio.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.resource.address.ResourceAddressFactory.newResourceAddressFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import org.apache.mina.core.session.IoSession;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.nio.TcpExtension;
import org.kaazing.gateway.transport.nio.TcpExtensionFactorySpi;

public class TcpExtensionFactoryTest {
    private ResourceAddress address;

    @Before
    public void before() throws Exception {
        address = newResourceAddressFactory().newResourceAddress("tcp://localhost:8888");
    }

    @Test
    public void availableExtensionsShouldReportNoExtensionsWhenNoFactoriesAvailable() throws Exception {
        TcpExtensionFactory factory = TcpExtensionFactory.newInstance(new TestClassLoader());
        Collection<TcpExtensionFactorySpi> extensions = factory.availableExtensions();
        assertTrue(extensions.isEmpty());
    }

    @Test
    public void availableExtensionsShouldReportAllExtensions() throws Exception {
        TcpExtensionFactory factory = TcpExtensionFactory.newInstance(new TestClassLoader(
                TestExtensionFactory1.class.getName(),
                TestExtensionFactory2.class.getName(),
                NullExtensionFactory.class.getName()));
        Collection<TcpExtensionFactorySpi> extensions = factory.availableExtensions();
        assertEquals(3, extensions.size());
    }

    @Test
    public void bindShouldReportNoExtensionsWhenNoFactoriesAvailable() throws Exception {
        TcpExtensionFactory factory = TcpExtensionFactory.newInstance(new TestClassLoader());
        List<TcpExtension> extensions = factory.bind(address);
        assertTrue(extensions.isEmpty());
    }

    @Test
    public void bindShouldReportNoExtensionsWhenExtensionBindsReturnNull() throws Exception {
        TcpExtensionFactory factory = TcpExtensionFactory.newInstance(new TestClassLoader(
                NullExtensionFactory.class.getName()));
        List<TcpExtension> extensions = factory.bind(address);
        assertTrue(extensions.isEmpty());
    }

    @Test
    public void bindShouldReportExtensionsWhenExtensionBindsReturnsAValue() throws Exception {
        TcpExtensionFactory factory = TcpExtensionFactory.newInstance(new TestClassLoader(
                TestExtensionFactory1.class.getName()));
        List<TcpExtension> extensions = factory.bind(address);
        assertEquals(1, extensions.size());
        assertEquals(TestExtension1.class.getName(), extensions.get(0).getClass().getName());
    }

    @Test
    public void bindShouldReportExtensionsWhoseFactoryBindsReturnValues() throws Exception {
        TcpExtensionFactory factory = TcpExtensionFactory.newInstance(new TestClassLoader(
                TestExtensionFactory1.class.getName(),
                TestExtensionFactory2.class.getName(),
                NullExtensionFactory.class.getName()));
        List<TcpExtension> extensions = factory.bind(address);
        assertEquals(2, extensions.size());
        assertEquals(TestExtension1.class.getName(), extensions.get(0).getClass().getName());
        assertEquals(TestExtension2.class.getName(), extensions.get(1).getClass().getName());
    }

    /**
     * A classloader whose getResources("META-INF/services/org.kaazing.gateway.transport.nio.TcpExtensionFactorySpi")
     * method will return a URL whose contents will be the list of class names supplied in the constructor.
     * This avoids the need for test meta-info resources files to be available on the test class path.
     */
    static class TestClassLoader extends ClassLoader {
        URL url;

        TestClassLoader(String... factorySpiClassNames) throws MalformedURLException {
            url = factorySpiClassNames == null ? null :
                     new URL(null, "data:metainf", new TestURLStreamHandler(factorySpiClassNames));
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            if (name.equals("META-INF/services/" + TcpExtensionFactorySpi.class.getName())) {
                return new Enumeration<URL>() {
                    private boolean hasMore = true;

                    @Override
                    public boolean hasMoreElements() {
                        return hasMore;
                    }

                    @Override
                    public URL nextElement() {
                        if (hasMore) {
                            hasMore = false;
                            return url;
                        }
                        return null;
                    }
                };
            }
            else {
                return super.getResources(name);
            }
        }

    }

    private static class TestURLStreamHandler extends URLStreamHandler {
        private final byte[] contents;


        public TestURLStreamHandler(String[] factorySpiClassNames) {
            StringBuffer metaInfContent = new StringBuffer();
            for (String name : factorySpiClassNames) {
                metaInfContent.append(name);
                metaInfContent.append('\n');
            }
            contents = metaInfContent.toString().getBytes();
        }

        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            return new URLConnection(u) {

                @Override
                public void connect() throws IOException {
                }

                @Override
                public InputStream getInputStream() {
                    return new ByteArrayInputStream(contents);
                }

            };
        }

    }

    public static class NullExtensionFactory implements TcpExtensionFactorySpi {

        @Override
        public TcpExtension bind(ResourceAddress address) {
            return null;
        }

    }

    public static class TestExtensionFactory1 implements TcpExtensionFactorySpi {

        @Override
        public TcpExtension bind(ResourceAddress address) {
            return new TestExtension1();
        }

    }

    public static class TestExtension1 implements TcpExtension {

        @Override
        public void initializeSession(IoSession session) {
        }

    }

    public static class TestExtensionFactory2 implements TcpExtensionFactorySpi {

        @Override
        public TcpExtension bind(ResourceAddress address) {
            return new TestExtension2();
        }

    }

    public static class TestExtension2 implements TcpExtension {

        @Override
        public void initializeSession(IoSession session) {
        }

    }

}
