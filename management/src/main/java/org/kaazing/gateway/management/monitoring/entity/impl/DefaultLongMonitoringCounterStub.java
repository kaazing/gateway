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

import org.kaazing.gateway.service.LongMonitoringCounter;

/**
 * Agrona specific monitoring counter which uses AtomicCounter as the underlying implementation.
 */
public class DefaultLongMonitoringCounterStub implements LongMonitoringCounter {

    @Override
    public LongMonitoringCounter increment() {
        return this;
    }

    @Override
    public LongMonitoringCounter incrementByValue(long value) {
        return this;
    }

    @Override
    public LongMonitoringCounter decrement() {
        return this;
    }

    @Override
    public LongMonitoringCounter decrementByValue(long value) {
        return this;
    }

    @Override
    public LongMonitoringCounter setValue(long value) {
        return this;
    }

    @Override
    public long getValue() {
        return 0;
    }

    @Override
    public LongMonitoringCounter reset() {
        return this;
    }

}
