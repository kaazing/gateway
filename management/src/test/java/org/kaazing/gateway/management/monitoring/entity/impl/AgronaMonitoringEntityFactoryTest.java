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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;
import org.kaazing.gateway.management.monitoring.configuration.impl.MMFMonitoringDataManager;
import org.kaazing.gateway.service.LongMonitoringCounter;
import org.kaazing.gateway.service.MonitoringEntityFactory;

import org.agrona.IoUtil;

@Ignore("doesn't work with latest Agrona 0.4.12")
public class AgronaMonitoringEntityFactoryTest {

    private static final String DEV_SHM = "/dev/shm/";
    private static final String LINUX = "Linux";
    private static final String OS_NAME = "os.name";
    private static final String MONITORING_FILE = "monitor";
    private static final String MONITORING_FILE_LOCATION = "/kaazing";

    @Test
    public void testAgronaLifecycle() {
        File monitoringDir;
        File monitoringFile;
        MMFMonitoringDataManager monitoringDataManager = new MMFMonitoringDataManager(MONITORING_FILE);
        try {
            MonitoringEntityFactory monitoringEntityFactory = monitoringDataManager.initialize();
            try {
                LongMonitoringCounter longMonitoringCounter = monitoringEntityFactory.makeLongMonitoringCounter("test");
    
                String osName = System.getProperty(OS_NAME);
                if (LINUX.equals(osName)) {
                    String monitoringDirName = DEV_SHM + IoUtil.tmpDirName() + MONITORING_FILE_LOCATION;
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
    
                assertNotNull(longMonitoringCounter);
            }
            finally {
                monitoringEntityFactory.close();
            }
        }
        finally {
            monitoringDataManager.close();
        }

        assertFalse(monitoringDir.exists());
        assertFalse(monitoringFile.exists());
    }
}
