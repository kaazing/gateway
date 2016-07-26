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

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;
import org.kaazing.gateway.management.monitoring.configuration.MonitoringDataManager;
import org.kaazing.gateway.management.monitoring.configuration.MonitoringDataManagerInjector;

@Ignore("doesn't work with latest Agrona 0.4.12")
public class MonitoringDataManagerInjectorTest {

    private static final String AGRONA_ENABLED = "org.kaazing.gateway.management.AGRONA_ENABLED";

    @Test
    public void monitoringDataManagerInjectorAgronaEnabledUnset() {
        MonitoringDataManagerInjector injector = new MonitoringDataManagerInjectorImpl(new Properties());
        MonitoringDataManager monitoringDataManager = injector.makeMonitoringDataManager();
        monitoringDataManager.close();
        assertTrue(monitoringDataManager instanceof MonitoringDataManagerStub);
    }

    @Test
    public void monitoringDataManagerInjectorAgronaEnabledFalse() {
        Properties configuration = new Properties();
        configuration.setProperty(AGRONA_ENABLED, Boolean.toString(false));
        MonitoringDataManagerInjector injector = new MonitoringDataManagerInjectorImpl(configuration);
        MonitoringDataManager monitoringDataManager = injector.makeMonitoringDataManager();
        monitoringDataManager.close();
        assertTrue(monitoringDataManager instanceof MonitoringDataManagerStub);
    }

    @Test
    public void monitoringDataManagerInjectorAgronaEnabledTrue() {
        Properties configuration = new Properties();
        configuration.setProperty(AGRONA_ENABLED, Boolean.toString(true));
        MonitoringDataManagerInjector injector = new MonitoringDataManagerInjectorImpl(configuration);
        MonitoringDataManager monitoringDataManager = injector.makeMonitoringDataManager();
        monitoringDataManager.close();
        assertTrue(monitoringDataManager instanceof MMFMonitoringDataManager);
    }
}
