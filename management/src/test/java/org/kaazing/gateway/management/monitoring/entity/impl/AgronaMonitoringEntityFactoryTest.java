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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Properties;

import org.junit.Test;
import org.kaazing.gateway.management.monitoring.configuration.impl.AgronaMonitoringEntityFactoryBuilder;
import org.kaazing.gateway.service.MonitoringEntityFactory;

import uk.co.real_logic.agrona.IoUtil;

public class AgronaMonitoringEntityFactoryTest {

    private static final String MONITORING_FILE = "monitor";
    private static final String MONITORING_FILE_LOCATION = "/kaazing";

    @Test
    public void testAgronaLifecycle() {
        AgronaMonitoringEntityFactoryBuilder builder = new AgronaMonitoringEntityFactoryBuilder(new Properties());
        MonitoringEntityFactory factory = builder.build();
        File monitoringDir;
        File monitoringFile;

        String osName = System.getProperty("os.name");
        if ("Linux".equals(osName)) {
            String monitoringDirName = "/dev/shm/" + IoUtil.tmpDirName() + MONITORING_FILE_LOCATION;
            monitoringDir = new File(monitoringDirName);
            assertTrue(monitoringDir.exists());
            monitoringFile = new File(monitoringDirName, MONITORING_FILE);
            assertTrue(monitoringFile.exists());
        } else {
            String monitoringDirName = IoUtil.tmpDirName() + MONITORING_FILE_LOCATION;
            monitoringDir = new File(monitoringDirName);
            assertTrue(monitoringDir.exists());
            monitoringFile = new File(monitoringDirName, MONITORING_FILE);
            assertTrue(monitoringFile.exists());
        }

        assertNotNull(factory.makeLongMonitoringCounter("test"));

        factory.close();

        assertFalse(monitoringDir.exists());
        assertFalse(monitoringFile.exists());
    }

}
