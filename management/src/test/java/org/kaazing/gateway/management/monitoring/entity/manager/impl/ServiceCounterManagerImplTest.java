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
package org.kaazing.gateway.management.monitoring.entity.manager.impl;
import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;
import org.kaazing.gateway.management.Utils.ManagementSessionType;
import org.kaazing.gateway.management.monitoring.configuration.MonitoringDataManager;
import org.kaazing.gateway.management.monitoring.configuration.impl.MMFMonitoringDataManager;
import org.kaazing.gateway.management.monitoring.entity.manager.ServiceCounterManager;
import org.kaazing.gateway.service.MonitoringEntityFactory;

@Ignore("doesn't work with latest Agrona 0.4.12")
public class ServiceCounterManagerImplTest {

    @Test
    public void assertAgronaEnabledNativeCounters() {
        MonitoringDataManager monitoringDataManager = new MMFMonitoringDataManager("test");
        try {
            MonitoringEntityFactory monitoringEntityFactory = monitoringDataManager.initialize();
            try {
                ServiceCounterManager serviceCounterManager = new ServiceCounterManagerImpl(monitoringEntityFactory);

                assertCounters(serviceCounterManager, 0, 0, 0, 0, 0, 0);

                serviceCounterManager.incrementSessionCounters(ManagementSessionType.NATIVE);
                assertCounters(serviceCounterManager, 0, 1, 1, 0, 1, 1);

                serviceCounterManager.decrementSessionCounters(ManagementSessionType.NATIVE);
                assertCounters(serviceCounterManager, 0, 1, 1, 0, 0, 0);
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
    public void assertAgronaEnabledEmulatedCounters() {
        MonitoringDataManager monitoringDataManager = new MMFMonitoringDataManager("test");
        try {
            MonitoringEntityFactory monitoringEntityFactory = monitoringDataManager.initialize();
            try {
                ServiceCounterManager serviceCounterManager = new ServiceCounterManagerImpl(monitoringEntityFactory);

                assertCounters(serviceCounterManager, 0, 0, 0, 0, 0, 0);

                serviceCounterManager.incrementSessionCounters(ManagementSessionType.EMULATED);
                assertCounters(serviceCounterManager, 1, 0, 1, 1, 0, 1);

                serviceCounterManager.decrementSessionCounters(ManagementSessionType.EMULATED);
                assertCounters(serviceCounterManager, 1, 0, 1, 0, 0, 0);
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
    public void assertAgronaDisabledNativeCounters() {
        MonitoringDataManager monitoringDataManager = new MMFMonitoringDataManager("test");
        try {
            MonitoringEntityFactory monitoringEntityFactory = monitoringDataManager.initialize();
            try {
                ServiceCounterManager serviceCounterManager = new ServiceCounterManagerImpl(monitoringEntityFactory);
    
                serviceCounterManager.incrementSessionCounters(ManagementSessionType.NATIVE);
                serviceCounterManager.decrementSessionCounters(ManagementSessionType.NATIVE);
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
    public void assertAgronaDisabledEmulatedCounters() {
        MonitoringDataManager monitoringDataManager = new MMFMonitoringDataManager("test");
        try {
            MonitoringEntityFactory monitoringEntityFactory = monitoringDataManager.initialize();
            try {
                ServiceCounterManager serviceCounterManager = new ServiceCounterManagerImpl(monitoringEntityFactory);

                serviceCounterManager.incrementSessionCounters(ManagementSessionType.EMULATED);
                serviceCounterManager.decrementSessionCounters(ManagementSessionType.EMULATED);
            }
            finally {
                monitoringEntityFactory.close();
            }
        }
        finally {
            monitoringDataManager.close();
        }
    }

    /**
     * Method asserting counter values to specified valuea array
     * @param serviceSessionCounterManager 
     * @param values
     */
    private void assertCounters(ServiceCounterManager serviceSessionCounterManager, long... values) {
        ServiceCounterManagerImpl serviceCounterManagerImpl = (ServiceCounterManagerImpl) serviceSessionCounterManager;
        assertEquals(values[0], serviceCounterManagerImpl.cumulativeEmulatedSessionsCounter().getValue());
        assertEquals(values[1], serviceCounterManagerImpl.cumulativeNativeSessionsCounter().getValue());
        assertEquals(values[2], serviceCounterManagerImpl.cumulativeSessionsCounter().getValue());
        assertEquals(values[3], serviceCounterManagerImpl.numberOfEmulatedSessionsCounter().getValue());
        assertEquals(values[4], serviceCounterManagerImpl.numberOfNativeSessionsCounter().getValue());
        assertEquals(values[5], serviceCounterManagerImpl.numberOfSessionsCounter().getValue());
    }
}
