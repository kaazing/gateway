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
package org.kaazing.gateway.management.monitoring.configuration;

import java.io.File;

import org.kaazing.gateway.management.monitoring.service.MonitoredService;
import org.kaazing.gateway.service.MonitoringEntityFactory;

import org.agrona.concurrent.UnsafeBuffer;

/**
 * MonitoringFileWriter interface responsible with writing data to monitoring files
 *
 */
public interface MonitorFileWriter {

    /**
     * Method returning gateway counter labels buffer
     * @param buffer
     * @return
     */
    UnsafeBuffer createGatewayCounterLabelsBuffer();

    /**
     * Method returning gateway counter values buffer
     * @return
     */
    UnsafeBuffer createGatewayCounterValuesBuffer();

    /**
     * Method creating service counter labels buffer for service index
     * @param index
     * @return
     */
    UnsafeBuffer createServiceCounterLabelsBuffer(int index);

    /**
     * Method returning service counter values buffer for service index
     * @param index
     * @return
     */
    UnsafeBuffer createServiceCounterValuesBuffer(int index);

    /**
     * Method returning gateway monitoring entity factory
     * @return
     */
    MonitoringEntityFactory getGatewayMonitoringEntityFactory();

    /**
     * Method returning service monitoring entity factory
     * @param monitoredService
     * @param index
     * @return
     */
    MonitoringEntityFactory getServiceMonitoringEntityFactory(MonitoredService monitoredService,
                                                              int index);

    /**
     * Method cleaning up monitoring file writer resources
     * @param monitoringDir
     */
    void close(File monitoringDir);

    /**
     * Method intializing monitoring file
     * @param monitoringFile
     */
    void initialize(File monitoringFile);

}
