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
package org.kaazing.gateway.management.monitoring.entity.manager;

import org.kaazing.gateway.management.Utils.ManagementSessionType;
import org.kaazing.gateway.service.MonitoringEntityFactory;

/**
 * Interface for a ServiceCounterManager responsible with holding the service session counter data and
 * performing the needed operations on its respective counters. It also provides methods (from
 * MonitoringEntityFactory) for use by individual services to add service specific counters.
 *
 */
public interface ServiceCounterManager extends MonitoringEntityFactory {

    /**
     * Method incrementing the service session counters
     * @param managementSessionType - session type used to determine whether session is native or not
     */
    void incrementSessionCounters(ManagementSessionType managementSessionType);

    /**
     * Method decrementing the service session counters
     * @param managementSessionType - session type used to determine whether session is native or not
     */
    void decrementSessionCounters(ManagementSessionType managementSessionType);
}
