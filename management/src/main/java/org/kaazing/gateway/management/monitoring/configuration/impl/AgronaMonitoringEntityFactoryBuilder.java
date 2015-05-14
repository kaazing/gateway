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

import org.kaazing.gateway.management.monitoring.configuration.MonitoringEntityFactoryBuilder;
import org.kaazing.gateway.management.monitoring.entity.factory.MonitoringEntityFactory;
import org.kaazing.gateway.management.monitoring.entity.impl.AgronaMonitoringEntityFactory;

import uk.co.real_logic.agrona.IoUtil;
import uk.co.real_logic.agrona.concurrent.CountersManager;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

/**
 * Agrona implementation for the monitoring entity factory builder.
 */
public class AgronaMonitoringEntityFactoryBuilder implements MonitoringEntityFactoryBuilder {

    private static final String OS_NAME_SYSTEM_PROPERTY = "os.name";
    private static final String LINUX_DEV_SHM_DIRECTORY = "/dev/shm";
    private static final String MONITOR_DIR_DESCRIPTION = "Monitoring directory";
    private static final String MONITOR_DIR_NAME = "/kaazing";
    private static final String MONITOR_FILE_NAME = "monitor";
    private static final int MONITOR_COUNTER_VALUES_BUFFER_LENGTH = 1024 * 1024;
    private static final int MONITOR_COUNTER_LABELS_BUFFER_LENGTH = 32 * MONITOR_COUNTER_VALUES_BUFFER_LENGTH;

    private CountersManager countersManager;

    @Override
    public MonitoringEntityFactory build() {
        String monitoringDirName = getMonitoringDirName();
        File monitoringDir = new File(monitoringDirName);

        File monitoringFile = new File(monitoringDir, MONITOR_FILE_NAME);
        IoUtil.deleteIfExists(monitoringFile);

        int fileSize = MonitorFileDescriptor.computeMonitorTotalFileLength(
                MONITOR_COUNTER_LABELS_BUFFER_LENGTH + MONITOR_COUNTER_VALUES_BUFFER_LENGTH);
        MappedByteBuffer mappedMonitorFile = IoUtil.mapNewFile(monitoringFile,
                fileSize);

        UnsafeBuffer metaDataBuffer = MonitorFileDescriptor.createMetaDataBuffer(mappedMonitorFile);
        MonitorFileDescriptor.fillMetaData(
                metaDataBuffer,
                MONITOR_COUNTER_LABELS_BUFFER_LENGTH,
                MONITOR_COUNTER_VALUES_BUFFER_LENGTH);

        UnsafeBuffer counterLabelsBuffer = MonitorFileDescriptor.createCounterLabelsBuffer(mappedMonitorFile, metaDataBuffer);
        UnsafeBuffer counterValuesBuffer = MonitorFileDescriptor.createCounterValuesBuffer(mappedMonitorFile, metaDataBuffer);

        countersManager = new CountersManager(counterLabelsBuffer, counterValuesBuffer);

        MonitoringEntityFactory factory = new AgronaMonitoringEntityFactory(countersManager, mappedMonitorFile, monitoringDir);

        return factory;
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
        } else {
            File monitoringDir = new File(MONITOR_DIR_NAME);
            IoUtil.ensureDirectoryExists(monitoringDir, MONITOR_DIR_DESCRIPTION);
        }

        return monitoringDirName;
    }

}
