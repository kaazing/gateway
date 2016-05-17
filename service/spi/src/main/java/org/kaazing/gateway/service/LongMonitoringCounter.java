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
 * This interface represents the abstraction layer for long monitoring counters.
 *
 * Monitoring counters represent values which indicate specific gateway relevant information, such as the number of open
 * sessions, the number of bytes sent/received, etc.
 *
 * This interface exposes the API for storing long metrics hiding the underlying implementation, which is
 * technology-dependent.
 *
 */
public interface LongMonitoringCounter {
    /**
     * Default value used by the reset method
     */
    long DEFAULT_VALUE = 0;

    /**
     * Method incrementing a monitoring counter by one unit
     * @return LongMonitoringCounter - the updated counter
     */
    LongMonitoringCounter increment();

    /**
     * Method incrementing a monitoring counter by a specific value
     * @param value - the value by which the increment is performed
     * @return LongMonitoringCounter - the updated counter
     */
    LongMonitoringCounter incrementByValue(long value);

    /**
     * Method decrementing a monitoring counter by one unit
     * @return LongMonitoringCounter - the updated counter
     */
    LongMonitoringCounter decrement();

    /**
     * Method decrementing a monitoring counter by a specific value
     * @param value - the value by which the increment is performed
     * @return LongMonitoringCounter - the updated counter
     */
    LongMonitoringCounter decrementByValue(long value);

    /**
     * Method setting a monitoring counter to a specific value
     * @param value - the value to which the counter is set
     * @return LongMonitoringCounter - the updated counter
     */
    LongMonitoringCounter setValue(long value);

    /**
     * Method returning entity value
     * @return long - the value stored in the counter
     */
    long getValue();

    /**
     * Method resetting a monitoring entity to its default value
     * @return LongMonitoringCounter - the updated counter
     */
    LongMonitoringCounter reset();
}
