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

package org.kaazing.gateway.management.monitoring.configuration.impl;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.kaazing.gateway.management.monitoring.entity.manager.impl.ServiceCounterManagerImpl;
import org.kaazing.gateway.management.monitoring.service.MonitoredService;
import org.kaazing.gateway.management.monitoring.service.impl.MonitoredServiceImpl;
import org.kaazing.gateway.security.CrossSiteConstraintContext;
import org.kaazing.gateway.server.context.resolve.DefaultAcceptOptionsContext;
import org.kaazing.gateway.server.context.resolve.DefaultConnectOptionsContext;
import org.kaazing.gateway.server.context.resolve.DefaultServiceContext;
import org.kaazing.gateway.server.context.resolve.DefaultServiceProperties;
import org.kaazing.gateway.service.MonitoringEntityFactory;

public class MMFMonitoringDataManagerTest {
    private static final String MONITORING_FILE = "monitor";

    @Test
    public void basicInitializeFlow() {
        MMFMonitoringDataManager monitoringDataManager = new MMFMonitoringDataManager(MONITORING_FILE);
        assertNotNull(monitoringDataManager);
        MonitoringEntityFactory monitoringEntityFactory = monitoringDataManager.initialize();
        assertNotNull(monitoringEntityFactory);
        monitoringEntityFactory.close();
        monitoringDataManager.close();
    }

    @Test
    public void addService() {
        MMFMonitoringDataManager monitoringDataManager = new MMFMonitoringDataManager(MONITORING_FILE);
        assertNotNull(monitoringDataManager);
        MonitoringEntityFactory monitoringEntityFactory = monitoringDataManager.initialize();
        assertNotNull(monitoringEntityFactory);
        MonitoredService monitoredService = new MonitoredServiceImpl(createDefaultServiceContext("serviceName"));
        ServiceCounterManagerImpl serviceCounterManager = monitoringDataManager.addService(monitoredService);
        assertNotNull(serviceCounterManager);
        monitoringEntityFactory.close();
        monitoringDataManager.close();
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
