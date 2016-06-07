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

import java.util.Properties;

import org.kaazing.gateway.management.monitoring.configuration.MonitoringDataManager;
import org.kaazing.gateway.management.monitoring.configuration.MonitoringDataManagerInjector;
import org.kaazing.gateway.util.InternalSystemProperty;

public class MonitoringDataManagerInjectorImpl implements MonitoringDataManagerInjector {

    /**
     * Configuration parameter
     */
    private Properties configuration;

    public MonitoringDataManagerInjectorImpl(Properties configuration) {
        this.configuration = configuration;
    }

    @Override
    public MonitoringDataManager makeMonitoringDataManager() {
        MonitoringDataManager monitoringManager;

        if (InternalSystemProperty.AGRONA_ENABLED.getBooleanProperty(configuration)) {
            String gatewayId = InternalSystemProperty.GATEWAY_IDENTIFIER.getProperty(configuration);
            monitoringManager = new MMFMonitoringDataManager(gatewayId);
        }
        else {
            monitoringManager = new MonitoringDataManagerStub();
        }
        monitoringManager.initialize();
        return monitoringManager;
    }

}
