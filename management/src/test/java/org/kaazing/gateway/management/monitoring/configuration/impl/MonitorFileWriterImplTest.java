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

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.kaazing.gateway.management.monitoring.configuration.MonitorFileWriter;
import org.kaazing.gateway.management.monitoring.service.MonitoredService;
import org.kaazing.gateway.management.monitoring.service.impl.MonitoredServiceImpl;
import org.kaazing.gateway.security.CrossSiteConstraintContext;
import org.kaazing.gateway.server.context.resolve.DefaultAcceptOptionsContext;
import org.kaazing.gateway.server.context.resolve.DefaultConnectOptionsContext;
import org.kaazing.gateway.server.context.resolve.DefaultServiceContext;
import org.kaazing.gateway.server.context.resolve.DefaultServiceProperties;
import org.kaazing.gateway.service.MonitoringEntityFactory;

import uk.co.real_logic.agrona.IoUtil;


public class MonitorFileWriterImplTest {
    private static final String LINUX = "Linux";
    private static final String OS_NAME_SYSTEM_PROPERTY = "os.name";
    private static final String LINUX_DEV_SHM_DIRECTORY = "/dev/shm";
    private static final String MONITOR_DIR_NAME = "/kaazing";
    private static final String MONITOR_FILE_NAME = "monitoring";

    @Test
    public void basicFlow() {
        String monitoringDirName = getMonitoringDirName();

        MonitorFileWriter monitorFileWriter = new MonitorFileWriterImpl(MONITOR_FILE_NAME);
        File monitoringFile = new File(monitoringDirName, MONITOR_FILE_NAME);
        monitorFileWriter.initialize(monitoringFile);
        MonitoringEntityFactory gatewayMonitoringEntityFactory = monitorFileWriter.getGatewayMonitoringEntityFactory();
        assertNotNull(gatewayMonitoringEntityFactory);
        MonitoredService monitoredService = new MonitoredServiceImpl(createDefaultServiceContext("serviceName"));
        MonitoringEntityFactory serviceMonitoringEntityFactory =
                monitorFileWriter.getServiceMonitoringEntityFactory(monitoredService , 0);
        assertNotNull(serviceMonitoringEntityFactory);
        assertNotNull(monitorFileWriter.createGatewayCounterValuesBuffer());
        assertNotNull(monitorFileWriter.createGatewayCounterLabelsBuffer());
        assertNotNull(monitorFileWriter.createServiceCounterValuesBuffer(0));
        assertNotNull(monitorFileWriter.createServiceCounterLabelsBuffer(0));
        monitorFileWriter.close(monitoringFile);
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

    /**
     * Method instantiating a new service context
     * @param serviceName 
     * @return
     */
    private DefaultServiceContext createDefaultServiceContext(String serviceName) {
        return new DefaultServiceContext("type",
                serviceName,
                "serviceDescription",
                null,
                null,
                null,
                Collections.<URI>emptySet(),
                Collections.<URI>emptySet(),
                Collections.<URI>emptySet(),
                new DefaultServiceProperties(),
                new String[]{},
                Collections.<String, String>emptyMap(),
                Collections.<URI, Map<String, CrossSiteConstraintContext>>emptyMap(),
                null,
                new DefaultAcceptOptionsContext(),
                new DefaultConnectOptionsContext(),
                null,
                null,
                null,
                true,
                true,
                false,
                1,
                null,
                null);
    }
}
