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
package org.kaazing.gateway.management.monitoring.configuration;

import org.kaazing.gateway.management.monitoring.service.MonitoredService;
import org.kaazing.gateway.service.MonitoringEntityFactory;

/**
 * This interface represents the abstraction layer for creating the monitoring entity factories {@link MonitoringEntityFactory},
 * with the specific underlying implementation, e.g. Agrona
 */
public interface MonitoringDataManager {

    /**
     * Method initializing data manager
     * @return the gw monitoring entity factory
     */
    MonitoringEntityFactory initialize();

    /**
     * Method for adding a monitored service
     * @param monitoredService
     * @return
     */
    MonitoringEntityFactory addService(MonitoredService monitoredService);

    /**
     * Method cleaning up resources in MonitoringDataManager
     */
    void close();
}
