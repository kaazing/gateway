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
package org.kaazing.gateway.management.monitoring.writer.impl;

import org.kaazing.gateway.management.monitoring.configuration.MonitorFileWriter;
import org.kaazing.gateway.management.monitoring.entity.impl.AgronaMonitoringEntityFactory;
import org.kaazing.gateway.management.monitoring.writer.ServiceWriter;
import org.kaazing.gateway.service.MonitoringEntityFactory;

import org.agrona.concurrent.status.CountersManager;
import org.agrona.concurrent.UnsafeBuffer;

public class MMFServiceWriter implements ServiceWriter {

    private CountersManager countersManager;
    private int index;
    private MonitorFileWriter monitorFileWriter;

    public MMFServiceWriter(MonitorFileWriter monitorFile, int index) {
        this.monitorFileWriter = monitorFile;
        this.index = index;
    }

    @Override
    public MonitoringEntityFactory writeCountersFactory() {
        createCountersManager();
        MonitoringEntityFactory factory = new AgronaMonitoringEntityFactory(countersManager);
        return factory;
    }

    /**
     * Helper method instantiating a counters manager
     */
    private void createCountersManager() {
        UnsafeBuffer counterLabelsBuffer = monitorFileWriter.createServiceCounterLabelsBuffer(index);
        UnsafeBuffer counterValuesBuffer = monitorFileWriter.createServiceCounterValuesBuffer(index);

        countersManager = new CountersManager(counterLabelsBuffer, counterValuesBuffer);
    }

}
