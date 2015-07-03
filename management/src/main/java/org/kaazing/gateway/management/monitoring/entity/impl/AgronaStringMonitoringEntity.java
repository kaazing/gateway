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

import org.kaazing.gateway.management.agrona.ex.AtomicStringEntity;
import org.kaazing.gateway.management.monitoring.entity.StringMonitoringEntity;

/**
 * Agrona specific String monitoring entity which uses AtomicStringEntity as the underlying implementation.
 */
public class AgronaStringMonitoringEntity implements StringMonitoringEntity {

    private AtomicStringEntity entity;

    AgronaStringMonitoringEntity(AtomicStringEntity entity) {
        this.entity = entity;
    }

    @Override
    public StringMonitoringEntity setValue(String value) {
        entity.set(value);
        return this;
    }

    @Override
    public String getValue() {
        return entity.get();
    }

    @Override
    public StringMonitoringEntity reset() {
        entity.set(DEFAULT_VALUE);
        return this;
    }

}
