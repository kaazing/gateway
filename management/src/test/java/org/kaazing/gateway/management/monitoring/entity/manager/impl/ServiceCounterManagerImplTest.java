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

package org.kaazing.gateway.management.monitoring.entity.manager.impl;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.kaazing.gateway.management.Utils.ManagementSessionType;
import org.kaazing.gateway.management.monitoring.configuration.MonitoringEntityFactoryInjector;
import org.kaazing.gateway.management.monitoring.configuration.impl.MonitoringEntityFactoryInjectorImpl;
import org.kaazing.gateway.management.monitoring.entity.manager.ServiceCounterManager;
import org.kaazing.gateway.service.MonitoringEntityFactory;

public class ServiceCounterManagerImplTest {

    private static final String AGRONA_ENABLED = "org.kaazing.gateway.management.AGRONA_ENABLED";

    @Test
    public void assertAgronaEnabledNativeCounters() {
        MonitoringEntityFactory monitoringEntityFactory = createMonitoringEntityFactory(true);
        ServiceCounterManager serviceCounterManager = new ServiceCounterManagerImpl(monitoringEntityFactory, null, null);

        serviceCounterManager.initializeSessionCounters();
        assertCounters(serviceCounterManager, 0, 0, 0, 0, 0, 0);

        serviceCounterManager.incrementSessionCounters(ManagementSessionType.NATIVE);
        assertCounters(serviceCounterManager, 0, 1, 1, 0, 1, 1);

        serviceCounterManager.decrementSessionCounters(ManagementSessionType.NATIVE);
        assertCounters(serviceCounterManager, 0, 1, 1, 0, 0, 0);

        monitoringEntityFactory.close();
    }

    @Test
    public void assertAgronaEnabledEmulatedCounters() {
        MonitoringEntityFactory monitoringEntityFactory = createMonitoringEntityFactory(true);
        ServiceCounterManager serviceCounterManager = new ServiceCounterManagerImpl(monitoringEntityFactory, null, null);

        serviceCounterManager.initializeSessionCounters();
        assertCounters(serviceCounterManager, 0, 0, 0, 0, 0, 0);

        serviceCounterManager.incrementSessionCounters(ManagementSessionType.EMULATED);
        assertCounters(serviceCounterManager, 1, 0, 1, 1, 0, 1);

        serviceCounterManager.decrementSessionCounters(ManagementSessionType.EMULATED);
        assertCounters(serviceCounterManager, 1, 0, 1, 0, 0, 0);

        monitoringEntityFactory.close();
    }

    @Test
    public void assertAgronaDisabledNativeCounters() {
        MonitoringEntityFactory monitoringEntityFactory = createMonitoringEntityFactory(false);
        ServiceCounterManager serviceCounterManager = new ServiceCounterManagerImpl(monitoringEntityFactory, null, null);

        serviceCounterManager.initializeSessionCounters();
        serviceCounterManager.incrementSessionCounters(ManagementSessionType.NATIVE);
        serviceCounterManager.decrementSessionCounters(ManagementSessionType.NATIVE);
        monitoringEntityFactory.close();
    }

    @Test
    public void assertAgronaDisabledEmulatedCounters() {
        MonitoringEntityFactory monitoringEntityFactory = createMonitoringEntityFactory(false);
        ServiceCounterManager serviceCounterManager = new ServiceCounterManagerImpl(monitoringEntityFactory, null, null);

        serviceCounterManager.initializeSessionCounters();
        serviceCounterManager.incrementSessionCounters(ManagementSessionType.EMULATED);
        serviceCounterManager.decrementSessionCounters(ManagementSessionType.EMULATED);
        monitoringEntityFactory.close();
    }

    /**
     * Helper method for creating a monitoring entity factory
     * @param agronaEnabled
     * @return
     */
    private MonitoringEntityFactory createMonitoringEntityFactory(boolean agronaEnabled) {
        Properties configuration = new Properties();
        configuration.setProperty(AGRONA_ENABLED, Boolean.toString(agronaEnabled));
        MonitoringEntityFactoryInjector injector = new MonitoringEntityFactoryInjectorImpl(configuration);
        MonitoringEntityFactory monitoringEntityFactory = injector.makeMonitoringEntityFactory();
        return monitoringEntityFactory;
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
