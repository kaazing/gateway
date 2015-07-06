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
import org.kaazing.gateway.management.monitoring.entity.factory.MonitoringEntityFactory;
import org.kaazing.gateway.management.monitoring.entity.manager.ServiceSessionCounterManager;

public class ServiceSessionCounterManagerImplTest {

    private static final String AGRONA_ENABLED = "org.kaazing.gateway.management.AGRONA_ENABLED";

    @Test
    public void testAgronaEnabledNativeCounters() {
        MonitoringEntityFactory monitoringEntityFactory = createMonitoringEntityFactory(true);
        ServiceSessionCounterManager serviceSessionCounterManager = new ServiceSessionCounterManagerImpl(monitoringEntityFactory, null, null);

        serviceSessionCounterManager.initializeCounters();
        assertCounters(serviceSessionCounterManager, 0, 0, 0, 0, 0, 0);

        serviceSessionCounterManager.incrementCounters(ManagementSessionType.NATIVE);
        assertCounters(serviceSessionCounterManager, 0, 1, 1, 0, 1, 1);

        serviceSessionCounterManager.decrementCounters(ManagementSessionType.NATIVE);
        assertCounters(serviceSessionCounterManager, 0, 1, 1, 0, 0, 0);

        monitoringEntityFactory.close();
    }

    @Test
    public void testAgronaEnabledEmulatedCounters() {
        MonitoringEntityFactory monitoringEntityFactory = createMonitoringEntityFactory(true);
        ServiceSessionCounterManager serviceSessionCounterManager = new ServiceSessionCounterManagerImpl(monitoringEntityFactory, null, null);

        serviceSessionCounterManager.initializeCounters();
        assertCounters(serviceSessionCounterManager, 0, 0, 0, 0, 0, 0);

        serviceSessionCounterManager.incrementCounters(ManagementSessionType.EMULATED);
        assertCounters(serviceSessionCounterManager, 1, 0, 1, 1, 0, 1);

        serviceSessionCounterManager.decrementCounters(ManagementSessionType.EMULATED);
        assertCounters(serviceSessionCounterManager, 1, 0, 1, 0, 0, 0);

        monitoringEntityFactory.close();
    }

    @Test
    public void testAgronaDisabledNativeCounters() {
        MonitoringEntityFactory monitoringEntityFactory = createMonitoringEntityFactory(false);
        ServiceSessionCounterManager serviceSessionCounterManager = new ServiceSessionCounterManagerImpl(monitoringEntityFactory, null, null);

        serviceSessionCounterManager.initializeCounters();
        serviceSessionCounterManager.incrementCounters(ManagementSessionType.NATIVE);
        serviceSessionCounterManager.decrementCounters(ManagementSessionType.NATIVE);
        monitoringEntityFactory.close();
    }

    @Test
    public void testAgronaDisabledEmulatedCounters() {
        MonitoringEntityFactory monitoringEntityFactory = createMonitoringEntityFactory(false);
        ServiceSessionCounterManager serviceSessionCounterManager = new ServiceSessionCounterManagerImpl(monitoringEntityFactory, null, null);

        serviceSessionCounterManager.initializeCounters();
        serviceSessionCounterManager.incrementCounters(ManagementSessionType.EMULATED);
        serviceSessionCounterManager.decrementCounters(ManagementSessionType.EMULATED);
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
    private void assertCounters(ServiceSessionCounterManager serviceSessionCounterManager, long... values) {
        ServiceSessionCounterManagerImpl serviceSessionCounterManagerImpl = (ServiceSessionCounterManagerImpl) serviceSessionCounterManager;
        assertEquals(values[0], serviceSessionCounterManagerImpl.getCumulativeEmulatedSessionsCounter().getValue());
        assertEquals(values[1], serviceSessionCounterManagerImpl.getCumulativeNativeSessionsCounter().getValue());
        assertEquals(values[2], serviceSessionCounterManagerImpl.getCumulativeSessionsCounter().getValue());
        assertEquals(values[3], serviceSessionCounterManagerImpl.getNumberOfEmulatedSessionsCounter().getValue());
        assertEquals(values[4], serviceSessionCounterManagerImpl.getNumberOfNativeSessionsCounter().getValue());
        assertEquals(values[5], serviceSessionCounterManagerImpl.getNumberOfSessionsCounter().getValue());
    }
}
