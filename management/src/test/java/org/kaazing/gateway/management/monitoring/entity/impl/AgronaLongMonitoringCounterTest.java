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

package org.kaazing.gateway.management.monitoring.entity.impl;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.management.monitoring.configuration.impl.AgronaMonitoringEntityFactoryBuilder;
import org.kaazing.gateway.service.LongMonitoringCounter;
import org.kaazing.gateway.service.MonitoringEntityFactory;

/**
 * Unit test for AgronaLongMonitoringCounter
 */
public class AgronaLongMonitoringCounterTest {

    private MonitoringEntityFactory factory;
    private LongMonitoringCounter counter;

    @Before
    public void before() {
        AgronaMonitoringEntityFactoryBuilder builder = new AgronaMonitoringEntityFactoryBuilder(new Properties());
        factory = builder.build();
        counter = factory.makeLongMonitoringCounter("counter");
    }

    @After
    public void after() {
        factory.close();
    }

    @Test
    public void testReset() {
        counter.reset();
        assertEquals(counter.getValue(), 0);
    }

    @Test
    public void testSet() {
        counter.setValue(1);
        assertEquals(counter.getValue(), 1);
    }

    @Test
    public void testIncrement() {
        counter.reset();
        counter.increment();
        assertEquals(counter.getValue(), 1);
    }

    @Test
    public void testIncrementByValue() {
        counter.reset();
        counter.incrementByValue(2);
        assertEquals(counter.getValue(), 2);
    }

    @Test
    public void testDecrement() {
        counter.reset();
        counter.decrement();
        assertEquals(counter.getValue(), -1);
    }

    @Test
    public void testDecrementByValue() {
        counter.reset();
        counter.decrementByValue(2);
        assertEquals(counter.getValue(), -2);
    }
}
