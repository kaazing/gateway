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
package org.kaazing.gateway.management.monitoring.service.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.management.monitoring.service.MonitoredService;
import org.kaazing.gateway.service.ServiceContext;

public class MonitoredServiceImplTest {

    private static final String SERVICE_NAME1 = "serviceName1";
    private static final String SERVICE_NAME2 = "serviceName2";
    private static final String SERVICE_NAME3 = "serviceName3";
    private Mockery context;

    @Before
    public void before() {
        context = new Mockery();
    }

    @Test
    public void assertServiceName() {
        ServiceContext serviceContext = createMockedDefaultServiceContext("serviceName");
        context.checking(new Expectations() {{
            oneOf(serviceContext).getServiceName().equals("serviceName");
        }});
        assertNotNull(new MonitoredServiceImpl(serviceContext));
    }

    @Test
    public void assertEqualsOverriden() {
        ServiceContext serviceContext1 = createMockedDefaultServiceContext(SERVICE_NAME1);
        ServiceContext serviceContext2 = createMockedDefaultServiceContext(SERVICE_NAME2);
        ServiceContext serviceContext3 = createMockedDefaultServiceContext(SERVICE_NAME3);
        context.checking(new Expectations() {{
            oneOf(serviceContext1).getServiceName().equals(SERVICE_NAME1);will(returnValue(SERVICE_NAME1));
            oneOf(serviceContext2).getServiceName().equals(SERVICE_NAME2);will(returnValue(SERVICE_NAME2));
            oneOf(serviceContext3).getServiceName().equals(SERVICE_NAME1);will(returnValue(SERVICE_NAME1));
        }});
        MonitoredService service1 = new MonitoredServiceImpl(serviceContext1);
        MonitoredService service2 = new MonitoredServiceImpl(serviceContext2);
        MonitoredService service3 = new MonitoredServiceImpl(serviceContext3);
        assertTrue(service1.equals(service1));
        assertTrue(service1.equals(service3));
        assertTrue(!service1.equals(service2));
    }

    /**
     * Method instantiating a new service context
     * @param serviceName 
     * @return
     */
    private ServiceContext createMockedDefaultServiceContext(String serviceName) {
        return context.mock(ServiceContext.class, serviceName);
    }

}
