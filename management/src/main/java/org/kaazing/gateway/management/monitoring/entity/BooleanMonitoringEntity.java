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

package org.kaazing.gateway.management.monitoring.entity;

/**
 * This interface represents the abstraction layer for boolean monitoring entities.
 *
 * Monitoring entities represent values which indicate specific gateway relevant information, such as is session
 * connected, backend connection established, etc.
 *
 * This interface exposes the API for storing boolean metrics hiding the underlying implementation, which is
 * technology-dependent.
 *
 */
public interface BooleanMonitoringEntity {
    /**
     * Default value used by the reset method
     */
    boolean DEFAULT_VALUE = false;

    /**
     * Method setting a monitoring entity to a specific value
     * @param value - the value to which the entity is set
     * @return BooleanMonitoringEntity - the updated entity
     */
    BooleanMonitoringEntity setValue(boolean value);

    /**
     * Method returning entity value
     * @return boolean - the value stored in the entity
     */
    boolean getValue();

    /**
     * Method resetting a monitoring entity to its default value
     * @return BooleanMonitoringEntity - the updated entity
     */
    BooleanMonitoringEntity reset();
}
