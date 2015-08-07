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

import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.Test;
import org.kaazing.gateway.management.monitoring.configuration.MonitoringEntityFactoryInjector;
import org.kaazing.gateway.management.monitoring.entity.impl.AgronaMonitoringEntityFactory;
import org.kaazing.gateway.management.monitoring.entity.impl.DefaultMonitoringEntityFactoryStub;
import org.kaazing.gateway.service.MonitoringEntityFactory;

public class MonitoringEntityFactoryInjectorTest {

    private static final String AGRONA_ENABLED = "org.kaazing.gateway.management.AGRONA_ENABLED";

    @Test
    public void testMonitoringEntityFactoryInjectorAgronaEnabledUnset() {
        MonitoringEntityFactoryInjector injector = new MonitoringEntityFactoryInjectorImpl(new Properties());
        MonitoringEntityFactory monitoringEntityFactory = injector.makeMonitoringEntityFactory();
        monitoringEntityFactory.close();
        assertTrue(monitoringEntityFactory instanceof DefaultMonitoringEntityFactoryStub);
    }

    @Test
    public void testMonitoringEntityFactoryInjectorAgronaEnabledFalse() {
        Properties configuration = new Properties();
        configuration.setProperty(AGRONA_ENABLED, Boolean.toString(false));
        MonitoringEntityFactoryInjector injector = new MonitoringEntityFactoryInjectorImpl(configuration);
        MonitoringEntityFactory monitoringEntityFactory = injector.makeMonitoringEntityFactory();
        monitoringEntityFactory.close();
        assertTrue(monitoringEntityFactory instanceof DefaultMonitoringEntityFactoryStub);
    }

    @Test
    public void testMonitoringEntityFactoryInjectorAgronaEnabledTrue() {
        Properties configuration = new Properties();
        configuration.setProperty(AGRONA_ENABLED, Boolean.toString(true));
        MonitoringEntityFactoryInjector injector = new MonitoringEntityFactoryInjectorImpl(configuration);
        MonitoringEntityFactory monitoringEntityFactory = injector.makeMonitoringEntityFactory();
        monitoringEntityFactory.close();
        assertTrue(monitoringEntityFactory instanceof AgronaMonitoringEntityFactory);
    }
}
