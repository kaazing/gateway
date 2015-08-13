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


package org.kaazing.gateway.management.monitoring.configuration.impl;

import java.util.concurrent.ConcurrentHashMap;

import org.kaazing.gateway.management.monitoring.configuration.MonitoringDataManager;
import org.kaazing.gateway.management.monitoring.entity.impl.DefaultMonitoringEntityFactoryStub;
import org.kaazing.gateway.service.MonitoringEntityFactory;
import org.kaazing.gateway.service.ServiceContext;

public class DefaultMMFMonitoringDataManagerStub implements MonitoringDataManager {

    @Override
    public ConcurrentHashMap<ServiceContext, MonitoringEntityFactory> initialize() {
        ConcurrentHashMap<ServiceContext, MonitoringEntityFactory> chm = new ConcurrentHashMap<>();
        chm.put(null, new DefaultMonitoringEntityFactoryStub());
        return chm;
    }

    @Override
    public ConcurrentHashMap<ServiceContext, MonitoringEntityFactory> getMonitoringEntityFactories() {
        // TODO Auto-generated method stub
        return null;
    }

}
