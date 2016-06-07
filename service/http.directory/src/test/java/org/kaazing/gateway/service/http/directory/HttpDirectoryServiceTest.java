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

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.ServiceFactory;
import org.kaazing.gateway.service.ServiceProperties;
import org.kaazing.test.util.MethodExecutionTrace;

public class HttpDirectoryServiceTest {
    @Rule
    public TestRule testExecutionTrace = new MethodExecutionTrace();

    @Test
    public void testCreateService() throws Exception {
        HttpDirectoryService service = (HttpDirectoryService)ServiceFactory.newServiceFactory().newService("directory");
        Assert.assertNotNull("Failed to create HttpDirectoryService", service);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoDirectoryProperty() throws Exception {
        final Mockery mockery = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final ServiceContext serviceContext = mockery.mock(ServiceContext.class);
        mockery.checking(new Expectations() {
            {
                allowing(serviceContext).getWebDirectory();
                will(returnValue(mockery.mock(File.class)));
                allowing(serviceContext).getProperties();
                will(returnValue(new TestServiceProperties()));
                allowing(serviceContext).getAccepts();
                will(returnValue(Collections.<URI> emptyList()));
            }
        });

        HttpDirectoryService service = (HttpDirectoryService)ServiceFactory.newServiceFactory().newService("directory");
        service.init(serviceContext);
        mockery.assertIsSatisfied();
    }

    @Test
    public void testOnlyDirectoryProperty() throws Exception {
        final Mockery mockery = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final ServiceContext serviceContext = mockery.mock(ServiceContext.class);
        final File webDir = new File("." + File.pathSeparator);
        final TestServiceProperties properties = new TestServiceProperties();
        properties.put("directory", "/");
        mockery.checking(new Expectations() {
            {
                allowing(serviceContext).getWebDirectory();
                will(returnValue(webDir));
                allowing(serviceContext).getProperties();
                will(returnValue(properties));
                allowing(serviceContext).getAccepts();
                will(returnValue(Collections.<URI> emptyList()));
            }
        });

        HttpDirectoryService service = (HttpDirectoryService)ServiceFactory.newServiceFactory().newService("directory");
        service.init(serviceContext);
        mockery.assertIsSatisfied();
    }

    @Test
    public void testEmptyErrorPageDirectoryProperty() throws Exception {
        final Mockery mockery = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final ServiceContext serviceContext = mockery.mock(ServiceContext.class);
        final File webDir = new File("." + File.pathSeparator);
        final TestServiceProperties properties = new TestServiceProperties();
        properties.put("directory", "/");
        properties.put("error-page-directory", "");
        mockery.checking(new Expectations() {
            {
                allowing(serviceContext).getWebDirectory();
                will(returnValue(webDir));
                allowing(serviceContext).getProperties();
                will(returnValue(properties));
                allowing(serviceContext).getAccepts();
                will(returnValue(Collections.<URI> emptyList()));
            }
        });

        HttpDirectoryService service = (HttpDirectoryService)ServiceFactory.newServiceFactory().newService("directory");
        service.init(serviceContext);
        mockery.assertIsSatisfied();
    }

    @Test
    public void testIndexesProperty() throws Exception {
        final Mockery mockery = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final ServiceContext serviceContext = mockery.mock(ServiceContext.class);
        final File webDir = new File("." + File.pathSeparator);
        final TestServiceProperties properties = new TestServiceProperties();
        properties.put("directory", "/");
        properties.put("options", "indexes");
        mockery.checking(new Expectations() {
            {
                allowing(serviceContext).getWebDirectory();
                will(returnValue(webDir));
                allowing(serviceContext).getProperties();
                will(returnValue(properties));
                allowing(serviceContext).getAccepts();
                will(returnValue(Collections.<URI> emptyList()));
            }
        });

        HttpDirectoryService service = (HttpDirectoryService)ServiceFactory.newServiceFactory().newService("directory");
        service.init(serviceContext);
        mockery.assertIsSatisfied();
    }

    private class TestServiceProperties implements ServiceProperties {
        private Map<String, List<ServiceProperties>> nestedProperties = new HashMap<>();
        private Map<String, String> properties = new HashMap<>();

        @Override
        public String get(String name) {
            return properties.get(name);
        }

        @Override
        public List<ServiceProperties> getNested(String name) {
            return nestedProperties.get(name);
        }

        @Override
        public List<ServiceProperties> getNested(String name, boolean create) {
            List<ServiceProperties> nestedPropertyList = nestedProperties.get(name);
            if (create && (nestedPropertyList == null)) {
                nestedPropertyList = new ArrayList<>();
                nestedProperties.put(name, nestedPropertyList);
            }
            return nestedPropertyList;
        }

        @Override
        public Iterable<String> simplePropertyNames() {
            return properties.keySet();
        }

        @Override
        public Iterable<String> nestedPropertyNames() {
            return nestedProperties.keySet();
        }

        @Override
        public boolean isEmpty() {
            return (properties.isEmpty() && nestedProperties.isEmpty());
        }

        @Override
        public void put(String name, String value) {
            properties.put(name, value);
        }
    }
}
