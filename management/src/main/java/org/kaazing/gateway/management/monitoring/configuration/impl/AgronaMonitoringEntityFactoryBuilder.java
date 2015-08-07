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
import java.util.Properties;

import org.kaazing.gateway.management.monitoring.configuration.MonitoringEntityFactoryBuilder;
import org.kaazing.gateway.management.monitoring.entity.impl.AgronaMonitoringEntityFactory;
import org.kaazing.gateway.service.MonitoringEntityFactory;
import org.kaazing.gateway.util.InternalSystemProperty;

import uk.co.real_logic.agrona.IoUtil;
import uk.co.real_logic.agrona.concurrent.CountersManager;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

/**
 * Agrona implementation for the monitoring entity factory builder.
 */
public class AgronaMonitoringEntityFactoryBuilder implements MonitoringEntityFactoryBuilder {

    private static final String OS_NAME_SYSTEM_PROPERTY = "os.name";
    private static final String LINUX_DEV_SHM_DIRECTORY = "/dev/shm";
    private static final String MONITOR_DIR_NAME = "/kaazing";
    private static final String MONITOR_FILE_NAME = "monitor";
    private static final int MONITOR_COUNTER_VALUES_BUFFER_LENGTH = 1024 * 1024;
    private static final int MONITOR_COUNTER_LABELS_BUFFER_LENGTH = 32 * MONITOR_COUNTER_VALUES_BUFFER_LENGTH;

    private CountersManager countersManager;
    private Properties configuration;
    private UnsafeBuffer metaDataBuffer;

    private MappedByteBuffer mappedMonitorFile;
    private File monitoringDir;

    public AgronaMonitoringEntityFactoryBuilder(Properties configuration) {
        super();
        this.configuration = configuration;
    }

    @Override
    public MonitoringEntityFactory build() {
        createMonitoringFile();

        createCountersManager();

        MonitoringEntityFactory factory =
                new AgronaMonitoringEntityFactory(countersManager, mappedMonitorFile, monitoringDir);

        return factory;
    }

    private void createMonitoringFile() {
        String monitoringDirName = getMonitoringDirName();
        monitoringDir = new File(monitoringDirName);

        String fileName = InternalSystemProperty.GATEWAY_IDENTIFIER.getProperty(configuration);
        if (fileName.equals("")) {
            fileName = MONITOR_FILE_NAME;
        }
        File monitoringFile = new File(monitoringDir, fileName);
        IoUtil.deleteIfExists(monitoringFile);

        int totalLengthOfBuffers =
                MONITOR_COUNTER_LABELS_BUFFER_LENGTH + MONITOR_COUNTER_VALUES_BUFFER_LENGTH;
        int fileSize = MonitorFileDescriptor.computeMonitorTotalFileLength(totalLengthOfBuffers);
        mappedMonitorFile = IoUtil.mapNewFile(monitoringFile, fileSize);

        metaDataBuffer = addMetadataToAgronaFile(mappedMonitorFile);
    }

    private void createCountersManager() {
        UnsafeBuffer counterLabelsBuffer = MonitorFileDescriptor.createCounterLabelsBuffer(mappedMonitorFile, metaDataBuffer);
        UnsafeBuffer counterValuesBuffer = MonitorFileDescriptor.createCounterValuesBuffer(mappedMonitorFile, metaDataBuffer);

        countersManager = new CountersManager(counterLabelsBuffer, counterValuesBuffer);
    }

    private UnsafeBuffer addMetadataToAgronaFile(MappedByteBuffer mappedMonitorFile) {
        UnsafeBuffer metaDataBuffer = MonitorFileDescriptor.createMetaDataBuffer(mappedMonitorFile);
        MonitorFileDescriptor.fillMetaData(metaDataBuffer, MONITOR_COUNTER_LABELS_BUFFER_LENGTH,
                MONITOR_COUNTER_VALUES_BUFFER_LENGTH);
        return metaDataBuffer;
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

        if ("Linux".equalsIgnoreCase(System.getProperty(OS_NAME_SYSTEM_PROPERTY))) {
            final File devShmDir = new File(LINUX_DEV_SHM_DIRECTORY);

            if (devShmDir.exists()) {
                monitoringDirName = LINUX_DEV_SHM_DIRECTORY + monitoringDirName;
            }
        }

        return monitoringDirName;
    }

}
