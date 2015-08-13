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

package org.kaazing.gateway.management.monitoring.writer.impl;

import java.io.File;
import java.nio.MappedByteBuffer;

import org.kaazing.gateway.management.monitoring.configuration.impl.MonitorFileDescriptor;
import org.kaazing.gateway.management.monitoring.entity.impl.AgronaMonitoringEntityFactory;
import org.kaazing.gateway.management.monitoring.writer.ServiceWriter;
import org.kaazing.gateway.service.MonitoringEntityFactory;

import uk.co.real_logic.agrona.concurrent.CountersManager;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public class MMFSeviceWriter implements ServiceWriter {

    private CountersManager countersManager;
    private MappedByteBuffer mappedMonitorFile;
    private UnsafeBuffer metaDataBuffer;
    private File monitoringDir;
    private int index;

    public MMFSeviceWriter(MappedByteBuffer mappedMonitorFile, UnsafeBuffer metaDataBuffer, File monitoringDir,
            int index) {
        this.mappedMonitorFile = mappedMonitorFile;
        this.metaDataBuffer = metaDataBuffer;
        this.monitoringDir = monitoringDir;
        this.index = index;
    }

    @Override
    public MonitoringEntityFactory writeCountersFactory() {
        createCountersManager();
        MonitoringEntityFactory factory = new AgronaMonitoringEntityFactory(countersManager, mappedMonitorFile,
                monitoringDir);
        return factory;
    }

    private void createCountersManager() {
        UnsafeBuffer counterLabelsBuffer = MonitorFileDescriptor.createServiceCounterLabelsBuffer(mappedMonitorFile,
                metaDataBuffer, index);
        UnsafeBuffer counterValuesBuffer = MonitorFileDescriptor.createServiceCounterValuesBuffer(mappedMonitorFile,
                metaDataBuffer, index);

        countersManager = new CountersManager(counterLabelsBuffer, counterValuesBuffer);
    }

}
