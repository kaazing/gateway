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

package org.kaazing.gateway.management.monitoring.entity.mock;

import org.kaazing.gateway.management.monitoring.entity.LongMonitoringCounter;

public class LongMonitoringCounterMock implements LongMonitoringCounter {

    private long value;

    @Override
    public LongMonitoringCounter reset() {
        value = DEFAULT_VALUE;
        System.out.println("Resetting counter data");
        return this;
    }

    @Override
    public LongMonitoringCounter setValue(long value) {
        this.value = value;
        System.out.println("Setting counter data to " + value);
        return this;
    }

    @Override
    public LongMonitoringCounter increment() {
        value++;
        System.out.println("Incrementing counter data");
        return this;
    }

    @Override
    public LongMonitoringCounter incrementByValue(long value) {
        this.value += value;
        System.out.println("Incrementing counter data with " + value);
        return this;
    }

    @Override
    public LongMonitoringCounter decrement() {
        value--;
        System.out.println("Decrementing counter data");
        return this;
    }

    @Override
    public LongMonitoringCounter decrementByValue(long value) {
        this.value -= value;
        System.out.println("Decrementing counter data with " + value);
        return this;
    }

    @Override
    public long getValue() {
        return value;
    }
}
