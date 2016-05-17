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
package org.kaazing.gateway.service;

/**
 * Factory for instantiating a specific monitoring entity.
 *
 * Monitoring entities represent values which indicate specific gateway relevant information, such as the number of open
 * sessions, the number of bytes sent/received, etc.
 *
 * This interface exposes the API for retrieving monitoring entities.
 */
public interface MonitoringEntityFactory {
    /**
     * Method returning a LongMonitoringCounter object
     * @param label - the name associated to the counter
     * @return - LongMonitoringCounter
     */
    LongMonitoringCounter makeLongMonitoringCounter(String name);

    /**
     * Cleans up the monitoring entities
     */
    void close();
}
