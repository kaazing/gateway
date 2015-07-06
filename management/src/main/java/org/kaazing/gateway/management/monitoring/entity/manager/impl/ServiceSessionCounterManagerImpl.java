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

package org.kaazing.gateway.management.monitoring.entity.manager.impl;

import org.kaazing.gateway.management.Utils.ManagementSessionType;
import org.kaazing.gateway.management.monitoring.entity.LongMonitoringCounter;
import org.kaazing.gateway.management.monitoring.entity.factory.MonitoringEntityFactory;
import org.kaazing.gateway.management.monitoring.entity.manager.ServiceSessionCounterManager;

public class ServiceSessionCounterManagerImpl implements
        ServiceSessionCounterManager {

    private static final String SEPARATOR = "-";
    private static final String CURRENT_NUMBER_OF_SESSIONS = "-current-number-of-sessions";
    private static final String CURRENT_NUMBER_OF_NATIVE_SESSIONS = "-current-number-of-native-sessions";
    private static final String CURRENT_NUMBER_OF_EMULATED_SESSIONS = "-current-number-of-emulated-sessions";
    private static final String CUMULATIVE_NUMBER_OF_SESSIONS = "-cumulative-number-of-sessions";
    private static final String CUMULATIVE_NUMBER_OF_NATIVE_SESSIONS = "-cumulative-number-of-native-sessions";
    private static final String CUMULATIVE_NUMBER_OF_EMULATED_SESSIONS = "-cumulative-number-of-emulated-sessions";
    private LongMonitoringCounter numberOfSessionsCounter;
    private LongMonitoringCounter numberOfNativeSessionsCounter;
    private LongMonitoringCounter numberOfEmulatedSessionsCounter;
    private LongMonitoringCounter cumulativeSessionsCounter;
    private LongMonitoringCounter cumulativeNativeSessionsCounter;
    private LongMonitoringCounter cumulativeEmulatedSessionsCounter;

    private MonitoringEntityFactory monitoringEntityFactory;
    private String serviceName;
    private String gatewayId;

    public ServiceSessionCounterManagerImpl(MonitoringEntityFactory monitoringEntityFactory,
            String serviceName, String gatewayId) {
        this.monitoringEntityFactory = monitoringEntityFactory;
        this.serviceName = serviceName;
        this.gatewayId = gatewayId;
   }

    @Override
    public void initializeCounters() {
        numberOfSessionsCounter = monitoringEntityFactory.makeLongMonitoringCounter(gatewayId + SEPARATOR + serviceName
                + CURRENT_NUMBER_OF_SESSIONS);
        numberOfNativeSessionsCounter = monitoringEntityFactory.makeLongMonitoringCounter(gatewayId + SEPARATOR + serviceName
                + CURRENT_NUMBER_OF_NATIVE_SESSIONS);
        numberOfEmulatedSessionsCounter = monitoringEntityFactory.makeLongMonitoringCounter(gatewayId + SEPARATOR + serviceName
                + CURRENT_NUMBER_OF_EMULATED_SESSIONS);
        cumulativeSessionsCounter = monitoringEntityFactory.makeLongMonitoringCounter(gatewayId + SEPARATOR + serviceName
                + CUMULATIVE_NUMBER_OF_SESSIONS);
        cumulativeNativeSessionsCounter = monitoringEntityFactory.makeLongMonitoringCounter(gatewayId + SEPARATOR + serviceName
                + CUMULATIVE_NUMBER_OF_NATIVE_SESSIONS);
        cumulativeEmulatedSessionsCounter = monitoringEntityFactory.makeLongMonitoringCounter(gatewayId + SEPARATOR + serviceName
                + CUMULATIVE_NUMBER_OF_EMULATED_SESSIONS);
    }

    @Override
    public void incrementCounters(ManagementSessionType managementSessionType) {
        numberOfSessionsCounter = numberOfSessionsCounter.increment();
        cumulativeSessionsCounter = cumulativeSessionsCounter.increment();
        if (managementSessionType.equals(ManagementSessionType.NATIVE)) {
            numberOfNativeSessionsCounter = numberOfNativeSessionsCounter.increment();
            cumulativeNativeSessionsCounter = cumulativeNativeSessionsCounter.increment();
        }
        else if (managementSessionType.equals(ManagementSessionType.EMULATED)) {
            numberOfEmulatedSessionsCounter = numberOfEmulatedSessionsCounter.increment();
            cumulativeEmulatedSessionsCounter = cumulativeEmulatedSessionsCounter.increment();
        }
    }

    @Override
    public void decrementCounters(ManagementSessionType managementSessionType) {
        numberOfSessionsCounter = numberOfSessionsCounter.decrement();
        if (managementSessionType.equals(ManagementSessionType.NATIVE)) {
            numberOfNativeSessionsCounter = numberOfNativeSessionsCounter.decrement();
        }
        else if (managementSessionType.equals(ManagementSessionType.EMULATED)) {
            numberOfEmulatedSessionsCounter = numberOfEmulatedSessionsCounter.decrement();
        }
    }

    /**
     * Getter for the numberOfSessionsCounter
     * @return the numberOfSessionsCounter
     */
    public LongMonitoringCounter getNumberOfSessionsCounter() {
        return numberOfSessionsCounter;
    }

    /**
     * Getter for the numberOfNativeSessionsCounter
     * @return the numberOfNativeSessionsCounter
     */
    public LongMonitoringCounter getNumberOfNativeSessionsCounter() {
        return numberOfNativeSessionsCounter;
    }

    /**
     * Getter for the numberOfEmulatedSessionsCounter
     * @return the numberOfEmulatedSessionsCounter
     */
    public LongMonitoringCounter getNumberOfEmulatedSessionsCounter() {
        return numberOfEmulatedSessionsCounter;
    }

    /**
     * Getter for the cumulativeSessionsCounter
     * @return the cumulativeSessionsCounter
     */
    public LongMonitoringCounter getCumulativeSessionsCounter() {
        return cumulativeSessionsCounter;
    }

    /**
     * Getter for the cumulativeNativeSessionsCounter
     * @return the cumulativeNativeSessionsCounter
     */
    public LongMonitoringCounter getCumulativeNativeSessionsCounter() {
        return cumulativeNativeSessionsCounter;
    }

    /**
     * Getter for the cumulativeEmulatedSessionsCounter
     * @return the cumulativeEmulatedSessionsCounter
     */
    public LongMonitoringCounter getCumulativeEmulatedSessionsCounter() {
        return cumulativeEmulatedSessionsCounter;
    }

}
