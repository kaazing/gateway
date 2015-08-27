/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.management.monitoring.service.impl;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.kaazing.gateway.management.monitoring.service.MonitoredService;
import org.kaazing.gateway.security.CrossSiteConstraintContext;
import org.kaazing.gateway.server.context.resolve.DefaultAcceptOptionsContext;
import org.kaazing.gateway.server.context.resolve.DefaultConnectOptionsContext;
import org.kaazing.gateway.server.context.resolve.DefaultServiceContext;
import org.kaazing.gateway.server.context.resolve.DefaultServiceProperties;
import org.kaazing.gateway.service.ServiceContext;

public class MonitoredServiceImplTest {

    @Test
    public void assertServiceName() {
        ServiceContext serviceContext = createDefaultServiceContext("serviceName");
        MonitoredService service = new MonitoredServiceImpl(serviceContext);
        assertEquals("serviceName", service.getServiceName());
    }

    @Test
    public void assertEqualsOverriden() {
        ServiceContext serviceContext1 = createDefaultServiceContext("serviceName1");
        ServiceContext serviceContext2 = createDefaultServiceContext("serviceName2");
        ServiceContext serviceContext3 = createDefaultServiceContext("serviceName1");
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
    private DefaultServiceContext createDefaultServiceContext(String serviceName) {
        return new DefaultServiceContext("type",
                serviceName,
                "serviceDescription",
                null,
                null,
                null,
                Collections.<URI>emptySet(),
                Collections.<URI>emptySet(),
                Collections.<URI>emptySet(),
                new DefaultServiceProperties(),
                new String[]{},
                Collections.<String, String>emptyMap(),
                Collections.<URI, Map<String, CrossSiteConstraintContext>>emptyMap(),
                null,
                new DefaultAcceptOptionsContext(),
                new DefaultConnectOptionsContext(),
                null,
                null,
                null,
                true,
                true,
                false,
                1,
                null,
                null);
    }
}
