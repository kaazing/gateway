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

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.kaazing.gateway.management.monitoring.configuration.MonitoringDataManager;
import org.kaazing.gateway.management.monitoring.configuration.impl.MMFMonitoringDataManager;
import org.kaazing.gateway.service.LongMonitoringCounter;
import org.kaazing.gateway.service.MonitoringEntityFactory;

/**
 * Unit test for AgronaLongMonitoringCounter
 */
@Ignore("doesn't work with latest Agrona 0.4.12")
public class AgronaLongMonitoringCounterTest {

    private MonitoringEntityFactory monitoringEntityFactory;
    private LongMonitoringCounter counter;
    private MonitoringDataManager monitoringDataManager;

    @Before
    public void before() {
        monitoringDataManager = new MMFMonitoringDataManager("test");
        monitoringEntityFactory = monitoringDataManager.initialize();
        counter = monitoringEntityFactory.makeLongMonitoringCounter("counter");
    }

    @After
    public void after() {
        monitoringEntityFactory.close();
        monitoringDataManager.close();
    }

    @Test
    public void reset() {
        counter.reset();
        assertEquals(counter.getValue(), 0);
    }

    @Test
    public void set() {
        counter.setValue(1);
        assertEquals(counter.getValue(), 1);
    }

    @Test
    public void increment() {
        counter.reset();
        counter.increment();
        assertEquals(counter.getValue(), 1);
    }

    @Test
    public void incrementByValue() {
        counter.reset();
        counter.incrementByValue(2);
        assertEquals(counter.getValue(), 2);
    }

    @Test
    public void decrement() {
        counter.reset();
        counter.decrement();
        assertEquals(counter.getValue(), -1);
    }

    @Test
    public void decrementByValue() {
        counter.reset();
        counter.decrementByValue(2);
        assertEquals(counter.getValue(), -2);
    }
}
