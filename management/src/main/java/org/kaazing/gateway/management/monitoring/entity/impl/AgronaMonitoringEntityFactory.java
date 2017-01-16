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
package org.kaazing.gateway.management.monitoring.entity.impl;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.kaazing.gateway.service.LongMonitoringCounter;
import org.kaazing.gateway.service.MonitoringEntityFactory;

import org.agrona.concurrent.status.AtomicCounter;
import org.agrona.concurrent.status.CountersManager;

/**
 * MonitoringEntityFactory which provides Agrona specific monitoring entities
 */
public class AgronaMonitoringEntityFactory implements MonitoringEntityFactory {

    private CountersManager countersManager;

    // These are needed for the cleanup work that needs to be done in the close method.
    private List<AtomicCounter> counters = new CopyOnWriteArrayList<>();

    public AgronaMonitoringEntityFactory(CountersManager countersManager) {
        this.countersManager = countersManager;
    }

    @Override
    public LongMonitoringCounter makeLongMonitoringCounter(String name) {
        // We create the new AtomicCounter using the CountersManager and we also add it to the list of counters
        // in order to close them when needed.
        AtomicCounter counter = countersManager.newCounter(name);
        counters.add(counter);

        LongMonitoringCounter longMonitoringCounter = new AgronaLongMonitoringCounter(counter);

        return longMonitoringCounter;
    }

    @Override
    public void close() {
        // We close the counters, the String monitoring entities and the we also need to unmap the file and delete the
        // monitoring directory.
        for (AtomicCounter atomicCounter : counters) {
            atomicCounter.close();
        }
    }

}
