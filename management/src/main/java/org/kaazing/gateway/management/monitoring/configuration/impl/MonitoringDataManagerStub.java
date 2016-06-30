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

import org.kaazing.gateway.management.monitoring.configuration.MonitoringDataManager;
import org.kaazing.gateway.management.monitoring.entity.impl.DefaultLongMonitoringCounterStub;
import org.kaazing.gateway.management.monitoring.entity.manager.impl.ServiceCounterManagerImpl;
import org.kaazing.gateway.management.monitoring.service.MonitoredService;
import org.kaazing.gateway.service.LongMonitoringCounter;
import org.kaazing.gateway.service.MonitoringEntityFactory;

public class MonitoringDataManagerStub implements MonitoringDataManager {

    private static final DefaultLongMonitoringCounterStub DEFAULT_LONG_MONITORING_COUNTER_STUB =
            new DefaultLongMonitoringCounterStub();
    private static final MonitoringEntityFactory MONITORING_ENTITY_FACTORY = new
            ServiceCounterManagerImpl(new MonitoringEntityFactory() {

        @Override
        public LongMonitoringCounter makeLongMonitoringCounter(String name) {
            return DEFAULT_LONG_MONITORING_COUNTER_STUB;
        }

        @Override
        public void close() {
        }
    });

    public MonitoringDataManagerStub() {
    }

    @Override
    public MonitoringEntityFactory initialize() {
        return MONITORING_ENTITY_FACTORY;
    }

    @Override
    public MonitoringEntityFactory addService(MonitoredService monitoredService) {
        return MONITORING_ENTITY_FACTORY;
    }

    @Override
    public void close() {
    }

}
