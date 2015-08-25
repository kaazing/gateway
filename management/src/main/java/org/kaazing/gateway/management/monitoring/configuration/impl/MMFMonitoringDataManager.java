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

import java.io.File;
import java.nio.MappedByteBuffer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;

import org.kaazing.gateway.management.monitoring.configuration.MonitorFileWriter;
import org.kaazing.gateway.management.monitoring.configuration.MonitoringDataManager;
import org.kaazing.gateway.management.monitoring.entity.manager.impl.ServiceCounterManagerImpl;
import org.kaazing.gateway.management.monitoring.service.MonitoredService;
import org.kaazing.gateway.service.MonitoringEntityFactory;
import org.kaazing.gateway.util.InternalSystemProperty;

import uk.co.real_logic.agrona.IoUtil;

/**
 * Agrona implementation for the monitoring entity factory builder.
 */
public class MMFMonitoringDataManager implements MonitoringDataManager {

    private static final String LINUX = "Linux";
    private static final String OS_NAME_SYSTEM_PROPERTY = "os.name";
    private static final String LINUX_DEV_SHM_DIRECTORY = "/dev/shm";
    private static final String MONITOR_DIR_NAME = "/kaazing";

    private MonitorFileWriter monitorFileWriter;
    private Properties configuration;
    private MappedByteBuffer mappedMonitorFile;
    private File monitoringDir;
    private Collection<MonitoredService> services = new HashSet<>();

    public MMFMonitoringDataManager(Properties configuration) {
        super();
        this.configuration = configuration;
        String gatewayId = InternalSystemProperty.GATEWAY_IDENTIFIER.getProperty(configuration);
        monitorFileWriter = new MonitorFileWriterImpl(gatewayId);
    }

    @Override
    public MonitoringEntityFactory initialize() {
        // create MMF
        createMonitoringFile();

        // create gateway monitoring entity factory
        MonitoringEntityFactory gwCountersFactory =
                monitorFileWriter.getGatewayMonitoringEntityFactory(mappedMonitorFile);

        return gwCountersFactory;
    }

    @Override
    public ServiceCounterManagerImpl addService(MonitoredService monitoredService) {
        MonitoringEntityFactory serviceCountersFactory = monitorFileWriter.getServiceMonitoringEntityFactory(mappedMonitorFile,
                monitoredService, services.size());

        services.add(monitoredService);
        return new ServiceCounterManagerImpl(serviceCountersFactory);
    }


    @Override
    public void close() {
        monitorFileWriter.close(monitoringDir, mappedMonitorFile);
    }

    /**
     * Method creating the monitoring MMF
     */
    private void createMonitoringFile() {
        String monitoringDirName = getMonitoringDirName();
        monitoringDir = new File(monitoringDirName);

        String fileName = InternalSystemProperty.GATEWAY_IDENTIFIER.getProperty(configuration);
        File monitoringFile = new File(monitoringDir, fileName);
        IoUtil.deleteIfExists(monitoringFile);

        int fileSize = monitorFileWriter.computeMonitorTotalFileLength();
        mappedMonitorFile = IoUtil.mapNewFile(monitoringFile, fileSize);

        monitorFileWriter.addMetadataToMonitoringFile(mappedMonitorFile);
    }

    /**
     * This method is used to compute the monitoring directory name which will be used by Agrona in order
     * to create a file in which to write the data in shared memory.
     *
     * The monitoring directory will be dependent of the operating system.
     *
     * For Linux we will use the OS implementation of the shared memory. So the directory will be created
     * in /dev/shm. For the other operating systems we will create a monitoring folder under the
     * gateway folder.
     *
     * @return the monitoring directory name
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
