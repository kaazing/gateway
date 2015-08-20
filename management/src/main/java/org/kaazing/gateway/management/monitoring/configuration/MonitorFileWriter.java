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

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

/**
 * MonitoringFileWriter interface responsible with writing data to monitoring files
 *
 */
public interface MonitorFileWriter {

    int computeMonitorTotalFileLength();

    UnsafeBuffer createGatewayCounterLabelsBuffer(ByteBuffer buffer, DirectBuffer metaDataBuffer);

    UnsafeBuffer createGatewayCounterValuesBuffer(ByteBuffer buffer, DirectBuffer metaDataBuffer);

    UnsafeBuffer createServiceCounterLabelsBuffer(ByteBuffer buffer, DirectBuffer metaDataBuffer, int index);

    UnsafeBuffer createServiceCounterValuesBuffer(ByteBuffer buffer, DirectBuffer metaDataBuffer, int index);

    MonitoringEntityFactory getGwMonitoringEntityFactory(MappedByteBuffer mappedMonitorFile, File monitoringDir);

    MonitoringEntityFactory getServiceMonitoringEntityFactory(MappedByteBuffer mappedMonitorFile,
                                                              File monitoringDir,
                                                              MonitoredService monitoredService,
                                                              int index);

    UnsafeBuffer addMetadataToAgronaFile(MappedByteBuffer mappedMonitorFile);

}
