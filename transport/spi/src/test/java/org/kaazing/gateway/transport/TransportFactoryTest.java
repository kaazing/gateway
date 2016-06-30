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
package org.kaazing.gateway.transport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.mina.core.future.UnbindFuture;

public class TransportFactoryTest {
    private static final Map<String, BridgeAcceptor> acceptors = new HashMap<>();

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    @Before
    public void before() throws Exception {
        acceptors.clear();
    }

    @Test
    public void shouldInjectTransportResources() throws Exception {
        Map<String, Object> empty = Collections.emptyMap();
        TransportFactory transportFactory = TransportFactory.newTransportFactory(new TestClassLoader(
                TestTransportFactory1.class.getName(),
                TestTransportFactory2.class.getName()), empty);
        Map<String, Object> transportResources = transportFactory.injectResources(empty);
        assertEquals(4, transportResources.size());
        TestAcceptor1 acceptor1 = (TestAcceptor1) transportResources.get("t1.acceptor");
        TestConnector1 connector1 = (TestConnector1) transportResources.get("t1.connector");
        TestConnector2 connector2 = (TestConnector2) transportResources.get("t2.connector");
        TestAcceptor2 acceptor2 = (TestAcceptor2) transportResources.get("t2.acceptor");
        assertSame(acceptor1, connector1.getT1Acceptor());
        assertSame(connector2, connector1.getT2Connector());
        assertNull(acceptor2.getScheduler());
        assertNull(connector2.getScheduler());
        assertEquals(connector1, acceptor2.getT1Connector());
        Iterator<?> extensions = transportFactory.getTransport("t1").getExtensions().iterator();
        TestExtension1 extension = (TestExtension1) extensions.next();
        assertSame(acceptor1, extension.getT1Acceptor());
        assertNull(extension.getScheduler());
    }

    @Test
    public void shouldInjectTransportAndExternalResources() throws Exception {
        Map<String, Object> empty = Collections.emptyMap();
        TransportFactory transportFactory = TransportFactory.newTransportFactory(new TestClassLoader(
                TestTransportFactory1.class.getName(),
                TestTransportFactory2.class.getName()), empty);
        Map<String, Object> resources = new HashMap<>();
        Object scheduler = new Object();
        resources.put("scheduler", scheduler);

        Map<String, Object> transportResources = transportFactory.injectResources(Collections.unmodifiableMap(resources));
        assertEquals(5, transportResources.size());
        TestAcceptor1 acceptor1 = (TestAcceptor1) transportResources.get("t1.acceptor");
        TestConnector1 connector1 = (TestConnector1) transportResources.get("t1.connector");
        TestConnector2 connector2 = (TestConnector2) transportResources.get("t2.connector");
        TestAcceptor2 acceptor2 = (TestAcceptor2) transportResources.get("t2.acceptor");
        assertSame(acceptor1, connector1.getT1Acceptor());
        assertSame(connector2, connector1.getT2Connector());
        assertEquals(scheduler, acceptor2.getScheduler());
        assertEquals(scheduler, connector2.getScheduler());
        assertEquals(connector1, acceptor2.getT1Connector());
        Iterator<?> extensions = transportFactory.getTransport("t1").getExtensions().iterator();
        TestExtension1 extension = (TestExtension1) extensions.next();
        assertSame(acceptor1, extension.getT1Acceptor());
        assertSame(scheduler, extension.getScheduler());
    }



    @Test
    public void injectResourcesShouldTolerateNullAcceptorOrConnector() throws Exception {
        Map<String, Object> empty = Collections.emptyMap();
        TransportFactory transportFactory = TransportFactory.newTransportFactory(new TestClassLoader(
                TestTransportFactory3.class.getName()),
                empty);
        Map<String, Object> transportResources = transportFactory.injectResources(empty);
        assertEquals(2, transportResources.size());
    }

    /**
     * A classloader whose getResources("META-INF/services/org.kaazing.gateway.transport.TransportFactorySpi")
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
            if (name.equals("META-INF/services/" + TransportFactorySpi.class.getName())) {
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

    public static class TestTransportFactory1 extends TransportFactorySpi {
        @Override
        public String getTransportName() {
            return "t1";
        }

        @Override
        public Collection<String> getSchemeNames() {
            return Collections.singleton("t1");
        }

        @Override
        public Transport newTransport(Map<String, ?> configuration) {
            return new TestTransport1();
        }
    }

    public static class TestTransport1 extends Transport {
        private BridgeAcceptor acceptor = new TestAcceptor1();
        private BridgeConnector connector = new TestConnector1();
        private final Object extension = new TestExtension1();

        @Override
        public BridgeAcceptor getAcceptor() {
            return acceptor;
        }

        @Override
        public BridgeConnector getConnector() {
            return connector;
        }

        @Override
        public BridgeAcceptor getAcceptor(ResourceAddress address) {
            return getAcceptor();
        }

        @Override
        public BridgeConnector getConnector(ResourceAddress address) {
            return getConnector();
        }

        @Override
        public Collection<?> getExtensions() {
            return Collections.singletonList(extension);
        }
    }

    public static class TestAcceptor1 implements BridgeAcceptor {
        @Override
        public void dispose() {
        }

        @Override
        public void bind(ResourceAddress address,
                         IoHandler handler,
                         BridgeSessionInitializer<? extends IoFuture> initializer) {
        }

        @Override
        public UnbindFuture unbind(ResourceAddress address) {
            return null;
        }

        @Override
        public IoHandler getHandler(ResourceAddress address) {
            return null;
        }
    }

    public static class TestConnector1 implements BridgeConnector {
        private BridgeAcceptor t1Acceptor = null;
        private BridgeConnector t2Connector = null;

        @Override
        public void dispose() {
        }

        @Override
        public ConnectFuture connect(ResourceAddress address,
                                     IoHandler handler,
                                     IoSessionInitializer<? extends ConnectFuture> initializer) {
            return null;
        }

        @Override
        public void connectInit(ResourceAddress address) {
        }

        @Override
        public void connectDestroy(ResourceAddress address) {
        }

        @Resource(name="t1.acceptor")
        public void setT1Acceptor(BridgeAcceptor t1Acceptor) {
            this.t1Acceptor = t1Acceptor;
        }

        public BridgeAcceptor getT1Acceptor() {
            return t1Acceptor;
        }

        @Resource(name="t2.connector")
        public void setT2Connector(BridgeConnector t2Connector) {
            this.t2Connector = t2Connector;
        }

        public BridgeConnector getT2Connector() {
            return t2Connector;
        }
    }

    public static class TestExtension1 {
        private BridgeAcceptor t1Acceptor;
        private Object scheduler;

        @Resource(name="t1.acceptor")
        public void setT1Acceptor(BridgeAcceptor t1Acceptor) {
            this.t1Acceptor = t1Acceptor;
        }

        public BridgeAcceptor getT1Acceptor() {
            return t1Acceptor;
        }

        @Resource(name="scheduler")
        public void setScheduler(Object scheduler) {
            this.scheduler = scheduler;
        }

        public Object getScheduler() {
            return scheduler;
        }
    }

    public static class TestTransportFactory2 extends TestTransportFactory1 {
        @Override
        public String getTransportName() {
            return "t2";
        }

        @Override
        public Collection<String> getSchemeNames() {
            return Collections.singleton("t2");
        }

        @Override
        public Transport newTransport(Map<String, ?> configuration) {
            return new TestTransport2();
        }
    }

    public static class TestTransport2 extends TestTransport1 {
        private BridgeAcceptor acceptor = new TestAcceptor2();
        private BridgeConnector connector = new TestConnector2();

        @Override
        public BridgeAcceptor getAcceptor() {
            return acceptor;
        }

        @Override
        public BridgeConnector getConnector() {
            return connector;
        }
    }

    public static class TestAcceptor2 extends TestAcceptor1 {
        private BridgeConnector t1Connector = null;
        private Object scheduler;

        @Resource(name="t1.connector")
        public void setT1Connector(BridgeConnector t1Connector) {
            this.t1Connector = t1Connector;
        }

        public BridgeConnector getT1Connector() {
            return t1Connector;
        }

        @Resource(name="scheduler")
        public void setScheduler(Object scheduler) {
            this.scheduler = scheduler;
        }

        public Object getScheduler() {
            return scheduler;
        }
    }

    public static class TestConnector2 extends TestConnector1    {
        private Object scheduler;

        @Resource(name="scheduler")
        public void setScheduler(Object scheduler) {
            this.scheduler = scheduler;
        }

        public Object getScheduler() {
            return scheduler;
        }
    }

    public static class TestTransportFactory3 extends TransportFactorySpi {
        @Override
        public String getTransportName() {
            return "t3";
        }

        @Override
        public Collection<String> getSchemeNames() {
            return Collections.singleton("t3");
        }

        @Override
        public Transport newTransport(Map<String, ?> configuration) {
            return new TestTransport3();
        }

    }

    public static class TestTransport3 extends Transport {

        @Override
        public BridgeAcceptor getAcceptor() {
            return null;
        }

        @Override
        public BridgeConnector getConnector() {
            return null;
        }

        @Override
        public BridgeAcceptor getAcceptor(ResourceAddress address) {
            return getAcceptor();
        }

        @Override
        public BridgeConnector getConnector(ResourceAddress address) {
            return getConnector();
        }

    }

}
