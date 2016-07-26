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
package org.kaazing.gateway.management.monitoring.configuration.impl;

import static org.junit.Assert.assertNotNull;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Ignore;
import org.junit.Test;
import org.kaazing.gateway.management.monitoring.entity.manager.impl.ServiceCounterManagerImpl;
import org.kaazing.gateway.management.monitoring.service.MonitoredService;
import org.kaazing.gateway.service.MonitoringEntityFactory;

@Ignore("doesn't work with the latest Agrona 0.4.12")
public class MMFMonitoringDataManagerTest {
    private static final String MONITORING_FILE = "monitor";

    @Test
    public void basicInitializeFlow() {
        MMFMonitoringDataManager monitoringDataManager = new MMFMonitoringDataManager(MONITORING_FILE);
        try {
            assertNotNull(monitoringDataManager);
            MonitoringEntityFactory monitoringEntityFactory = monitoringDataManager.initialize();
            try {
                assertNotNull(monitoringEntityFactory);
            }
            finally {
                monitoringEntityFactory.close();
            }
        }
        finally {
            monitoringDataManager.close();
        }
    }

    @Test
    public void addService() {
        Mockery context = new Mockery();
        MMFMonitoringDataManager monitoringDataManager = new MMFMonitoringDataManager(MONITORING_FILE);
        try {
            assertNotNull(monitoringDataManager);
            MonitoringEntityFactory monitoringEntityFactory = monitoringDataManager.initialize();
            try {
                assertNotNull(monitoringEntityFactory);
                MonitoredService monitoredService = context.mock(MonitoredService.class);
                context.checking(new Expectations() {{
                    oneOf(monitoredService).getServiceName();
                }});
                ServiceCounterManagerImpl serviceCounterManager = monitoringDataManager.addService(monitoredService);
                assertNotNull(serviceCounterManager);
            }
            finally {
                monitoringEntityFactory.close();
            }
        }
        finally {
            monitoringDataManager.close();
        }
    }

}
