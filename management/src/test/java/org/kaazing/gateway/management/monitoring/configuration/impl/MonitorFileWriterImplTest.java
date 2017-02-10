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
package org.kaazing.gateway.management.monitoring.configuration.impl;

import static org.junit.Assert.*;

import java.io.File;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Ignore;
import org.junit.Test;
import org.kaazing.gateway.management.monitoring.configuration.MonitorFileWriter;
import org.kaazing.gateway.management.monitoring.service.MonitoredService;
import org.kaazing.gateway.service.MonitoringEntityFactory;

import org.agrona.IoUtil;

@Ignore("doesn't work with latest Agrona 0.4.12")
public class MonitorFileWriterImplTest {
    private static final String LINUX = "Linux";
    private static final String OS_NAME_SYSTEM_PROPERTY = "os.name";
    private static final String LINUX_DEV_SHM_DIRECTORY = "/dev/shm";
    private static final String MONITOR_DIR_NAME = "/kaazing";
    private static final String MONITOR_FILE_NAME = "monitoring";

    @Test
    public void positiveFlow() {
        Mockery context = new Mockery();
        String monitoringDirName = getMonitoringDirName();

        MonitorFileWriter monitorFileWriter = new MonitorFileWriterImpl(MONITOR_FILE_NAME);
        File monitoringFile = new File(monitoringDirName, MONITOR_FILE_NAME);
        try {
            monitorFileWriter.initialize(monitoringFile);
            MonitoringEntityFactory gatewayMonitoringEntityFactory = monitorFileWriter.getGatewayMonitoringEntityFactory();
            assertNotNull(gatewayMonitoringEntityFactory);
            MonitoringEntityFactory serviceMonitoringEntityFactory = getServiceMonitoringEntityFactory(context,
                    monitorFileWriter);
            assertNotNull(serviceMonitoringEntityFactory);
            assertNotNull(monitorFileWriter.createGatewayCounterValuesBuffer());
            assertNotNull(monitorFileWriter.createGatewayCounterLabelsBuffer());
            assertNotNull(monitorFileWriter.createServiceCounterValuesBuffer(0));
            assertNotNull(monitorFileWriter.createServiceCounterLabelsBuffer(0));
        }
        finally {
            monitorFileWriter.close(monitoringFile);
        }
    }

    @Test
    public void servicesCounterValuesOverflow() {
        Mockery context = new Mockery();
        String monitoringDirName = getMonitoringDirName();

        MonitorFileWriter monitorFileWriter = new MonitorFileWriterImpl(MONITOR_FILE_NAME);
        File monitoringFile = new File(monitoringDirName, MONITOR_FILE_NAME);
        try {
            monitorFileWriter.initialize(monitoringFile);
            MonitoringEntityFactory serviceMonitoringEntityFactory = getServiceMonitoringEntityFactory(context,
                    monitorFileWriter);
            assertNotNull(serviceMonitoringEntityFactory);
            assertNotNull(monitorFileWriter.createServiceCounterValuesBuffer(100));
            // this should not be reached
            assertTrue(false);
        }
        catch (IndexOutOfBoundsException e) {
            assertTrue("Service counter values buffer overflow", true);
        }
        finally {
            monitorFileWriter.close(monitoringFile);
        }
    }

    @Test
    public void servicesCounterLabelsOverflow() {
        Mockery context = new Mockery();
        String monitoringDirName = getMonitoringDirName();

        MonitorFileWriter monitorFileWriter = new MonitorFileWriterImpl(MONITOR_FILE_NAME);
        File monitoringFile = new File(monitoringDirName, MONITOR_FILE_NAME);
        try {
            monitorFileWriter.initialize(monitoringFile);
            MonitoringEntityFactory serviceMonitoringEntityFactory = getServiceMonitoringEntityFactory(context,
                    monitorFileWriter);
            assertNotNull(serviceMonitoringEntityFactory);
            assertNotNull(monitorFileWriter.createServiceCounterLabelsBuffer(100));
            // this should not be reached
            assertTrue(false);
        }
        catch (IndexOutOfBoundsException e) {
            assertTrue("Service counter labels buffer overflow", true);
        }
        finally {
            monitorFileWriter.close(monitoringFile);
        }
    }

    /**
     * Method returning a service monitoring factory
     * @param context
     * @param monitorFileWriter
     * @return
     */
    private MonitoringEntityFactory getServiceMonitoringEntityFactory(Mockery context,
                                                                      MonitorFileWriter monitorFileWriter) {
        MonitoredService monitoredService = context.mock(MonitoredService.class);
        context.checking(new Expectations() {{
            oneOf(monitoredService).getServiceName();
        }});
        MonitoringEntityFactory serviceMonitoringEntityFactory =
                monitorFileWriter.getServiceMonitoringEntityFactory(monitoredService , 0);
        return serviceMonitoringEntityFactory;
    }

    /**
     * Method computing the monitoring directory name, which is OS-dependent
     * @return
     */
    private String getMonitoringDirName() {
        String monitoringDirName = IoUtil.tmpDirName() + MONITOR_DIR_NAME;

        if (LINUX.equalsIgnoreCase(System.getProperty(OS_NAME_SYSTEM_PROPERTY))) {
            final File devShmDir = new File(LINUX_DEV_SHM_DIRECTORY);

            if (devShmDir.exists()) {
                monitoringDirName = LINUX_DEV_SHM_DIRECTORY + monitoringDirName;
            }
        }
        return monitoringDirName;
    }
}
