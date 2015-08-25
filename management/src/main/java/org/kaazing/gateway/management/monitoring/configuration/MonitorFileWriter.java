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

package org.kaazing.gateway.management.monitoring.configuration;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

import org.kaazing.gateway.management.monitoring.service.MonitoredService;
import org.kaazing.gateway.service.MonitoringEntityFactory;

import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

/**
 * MonitoringFileWriter interface responsible with writing data to monitoring files
 *
 */
public interface MonitorFileWriter {

    /**
     * Method computing monitoring file total length
     * @return
     */
    int computeMonitorTotalFileLength();

    /**
     * Method returning gateway counter labels buffer
     * @param buffer
     * @return
     */
    UnsafeBuffer createGatewayCounterLabelsBuffer(ByteBuffer buffer);

    /**
     * Method returning gateway counter values buffer
     * @param buffer
     * @return
     */
    UnsafeBuffer createGatewayCounterValuesBuffer(ByteBuffer buffer);

    /**
     * Method creating service counter labels buffer for service index
     * @param buffer
     * @param index
     * @return
     */
    UnsafeBuffer createServiceCounterLabelsBuffer(ByteBuffer buffer, int index);

    /**
     * Method returning service counter values buffer for service index
     * @param buffer
     * @param index
     * @return
     */
    UnsafeBuffer createServiceCounterValuesBuffer(ByteBuffer buffer, int index);

    /**
     * Method returning gateway monitoring entity factory
     * @param mappedMonitorFile
     * @return
     */
    MonitoringEntityFactory getGatewayMonitoringEntityFactory(MappedByteBuffer mappedMonitorFile);

    /**
     * Method returning service monitoring entity factory
     * @param mappedMonitorFile
     * @param monitoredService
     * @param index
     * @return
     */
    MonitoringEntityFactory getServiceMonitoringEntityFactory(MappedByteBuffer mappedMonitorFile,
                                                              MonitoredService monitoredService,
                                                              int index);

    /**
     * Method adding metadata to monitor file
     * @param mappedMonitorFile
     * @return
     */
    UnsafeBuffer addMetadataToMonitoringFile(MappedByteBuffer mappedMonitorFile);

    /**
     * Method cleaning up monitoring file writer resources
     * @param monitoringDir
     * @param mappedMonitorFile
     */
    void close(File monitoringDir, MappedByteBuffer mappedMonitorFile);

}
