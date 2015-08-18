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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;

import org.kaazing.gateway.server.Gateway;
import org.kaazing.gateway.service.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

/**
 * TODO: Testing is in progress for this item. Validate items' lengths Helper class used to create the initial
 * configuration for Agrona. Listing below the monitoring file structure:
 *
 * Metadata length: 8 * int + 1 * string + no_of_serv * (1 * string + 5 * int)
 * +-----------------------------------------------------------------------+ | File version | GW data offset | Service
 * mappings offset | | GW ID | GW counters lbl buffer offset | GW counters lbl buffer length | GW counters values buffer
 * offset | GW counters values buffer length | | Number of services | Service 1 name | Service 1 offset | ... | Service
 * 1 lbl buffer offset | Service 1 lbl buffer length | | Service 1 values buffer offset | Service 1 values buffer length
 * | | ... | | GW counters labels buffer | | GW counters values buffer | | Service 1 labels buffer | | Service 1 values
 * buffer || ... | +-----------------------------------------------------------------------+
 */
public final class MonitorFileDescriptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gateway.class);
    private static final int SIZEOF_STRING = 1024;

    private static final int MONITOR_VERSION = 1;
    private static final int MONITOR_VERSION_OFFSET = 0;
    private static final int META_DATA_OFFSET = MONITOR_VERSION_OFFSET + BitUtil.SIZE_OF_INT;
    private static final int GW_DATA_REFERENCE_OFFSET = MONITOR_VERSION_OFFSET + BitUtil.SIZE_OF_INT;
    private static final int SERVICE_DATA_REFERENCE_OFFSET = GW_DATA_REFERENCE_OFFSET + BitUtil.SIZE_OF_INT;

    private static final int GW_ID_OFFSET = SERVICE_DATA_REFERENCE_OFFSET + BitUtil.SIZE_OF_INT;
    private static final int GW_DATA_OFFSET = GW_ID_OFFSET;
    private static final int GW_COUNTERS_LBL_BUFFERS_REFERENCE_OFFSET = GW_ID_OFFSET + SIZEOF_STRING;
    private static final int GW_COUNTERS_LBL_BUFFERS_LENGTH_OFFSET = GW_COUNTERS_LBL_BUFFERS_REFERENCE_OFFSET
            + BitUtil.SIZE_OF_INT;
    private static final int GW_COUNTERS_VALUE_BUFFERS_REFERENCE_OFFSET = GW_COUNTERS_LBL_BUFFERS_LENGTH_OFFSET
            + BitUtil.SIZE_OF_INT;
    private static final int GW_COUNTERS_VALUE_BUFFERS_LENGTH_OFFSET = GW_COUNTERS_VALUE_BUFFERS_REFERENCE_OFFSET
            + BitUtil.SIZE_OF_INT;

    private static final int NO_OF_SERVICES_OFFSET = GW_COUNTERS_VALUE_BUFFERS_LENGTH_OFFSET + BitUtil.SIZE_OF_INT;
    private static final int SERVICE_DATA_OFFSET = NO_OF_SERVICES_OFFSET;

    private int metadataLength;
    private int servicesCount;
    private int endOfMetadata;
    private int serviceRefSection;

    private int serviceCounterLabelsBufferLength;
    private int serviceCounterValuesBufferLength;
    private int gatewayCounterLabelsBufferLength;
    private int gatewayCounterValuesBufferLength;

    private String gatewayId;

    private Collection<? extends ServiceContext> services;

    /**
     * TODO: Switch to an instantiable class with state
     * @param services
     * @param gatewayId
     * @param serviceCounterValuesBufferLength
     * @param serviceCounterLabelsBufferLength
     * @param gatewayCounterValuesBufferLength
     * @param gatewayCounterLabelsBufferLength
     */
    public MonitorFileDescriptor(int gatewayCounterLabelsBufferLength, int gatewayCounterValuesBufferLength,
            int serviceCounterLabelsBufferLength, int serviceCounterValuesBufferLength, String gatewayId,
            Collection<? extends ServiceContext> services) {
        this.gatewayCounterLabelsBufferLength = gatewayCounterLabelsBufferLength;
        this.gatewayCounterValuesBufferLength = gatewayCounterValuesBufferLength;
        this.serviceCounterLabelsBufferLength = serviceCounterLabelsBufferLength;
        this.serviceCounterValuesBufferLength = serviceCounterValuesBufferLength;
        this.gatewayId = gatewayId;
        this.services = services;
        setServicesCount(services.size());
    }

    /**
     * Computes the total length of the file used by Agrona
     * @param totalLengthOfBuffers - the total length of the buffers
     * @return
     */
    public int computeMonitorTotalFileLength(final int totalLengthOfBuffers) {
        return endOfMetadata + totalLengthOfBuffers;
    }

    /**
     * Computes the offset for the monitoring framework version
     * @param baseOffset - the base offset of the buffer
     * @return the monitor version offset
     */
    public static int monitorVersionOffset(final int baseOffset) {
        return baseOffset + MONITOR_VERSION_OFFSET;
    }

    /**
     * Computes the offset for the counter labels buffer
     * @param baseOffset - the base offset of the buffer
     * @return the counter labels buffer offset
     */
    public static int metadataRelativeOffset(final int baseOffset, int offset) {
        return baseOffset + META_DATA_OFFSET + offset;
    }

    /**
     * Computes the offset for the counter labels buffer
     * @param baseOffset - the base offset of the buffer
     * @return the counter labels buffer offset
     */
    public static int metadataItemOffset(int offset) {
        return metadataRelativeOffset(0, offset);
    }

    /**
     * Creates the meta data buffer
     * @param buffer - the underlying byte buffer
     * @return the meta data buffer
     */
    public UnsafeBuffer createMetaDataBuffer(final ByteBuffer buffer) {
        return new UnsafeBuffer(buffer, 0, metadataLength + BitUtil.SIZE_OF_INT);
    }

    /**
     * Fills the meta data in the specified buffer
     * @param monitorMetaDataBuffer - the meta data buffer
     * @param monitorLabelsBufferLength - the length of the counters labels buffer
     * @param monitorValuesBufferLength - the length of the counters values buffer
     */
    public void fillMetaData(final UnsafeBuffer monitorMetaDataBuffer) {
        monitorMetaDataBuffer.putInt(monitorVersionOffset(0), MONITOR_VERSION);
        monitorMetaDataBuffer.putInt(metadataItemOffset(GW_DATA_REFERENCE_OFFSET), metadataItemOffset(GW_DATA_OFFSET));
        monitorMetaDataBuffer.putInt(metadataItemOffset(SERVICE_DATA_REFERENCE_OFFSET),
                metadataItemOffset(SERVICE_DATA_OFFSET));
        monitorMetaDataBuffer.putStringUtf8(metadataItemOffset(GW_ID_OFFSET), gatewayId, ByteOrder.BIG_ENDIAN);
        monitorMetaDataBuffer.putInt(metadataItemOffset(GW_COUNTERS_LBL_BUFFERS_REFERENCE_OFFSET), 0);
        monitorMetaDataBuffer.putInt(metadataItemOffset(GW_COUNTERS_LBL_BUFFERS_LENGTH_OFFSET),
                gatewayCounterLabelsBufferLength);
        monitorMetaDataBuffer.putInt(metadataItemOffset(GW_COUNTERS_VALUE_BUFFERS_REFERENCE_OFFSET), 0);
        monitorMetaDataBuffer.putInt(metadataItemOffset(GW_COUNTERS_VALUE_BUFFERS_LENGTH_OFFSET),
                gatewayCounterValuesBufferLength);
        monitorMetaDataBuffer.putInt(metadataItemOffset(NO_OF_SERVICES_OFFSET), services.size());
        fillServicesMetadata(monitorMetaDataBuffer);
    }

    /**
     * Method adding services metadata
     * @param monitorMetaDataBuffer
     */
    private void fillServicesMetadata(final UnsafeBuffer monitorMetaDataBuffer) {
        int i = 0;
        int servOffset = metadataItemOffset(NO_OF_SERVICES_OFFSET) + BitUtil.SIZE_OF_INT;

//        LOGGER.info(services.size() + " items starting from " + serviceRefSection + " towards "
//                + endOfMetadata + ", " + BitUtil.SIZE_OF_INT + " per item");
//        LOGGER.info("Capacity: " + monitorMetaDataBuffer.capacity());

        for (ServiceContext service : services) {
            String serviceName = service.getServiceName();
            int serviceNameOffset = servOffset + i * (SIZEOF_STRING + BitUtil.SIZE_OF_INT);
            int serviceLocationOffset = servOffset + (i + 1) * SIZEOF_STRING + i * BitUtil.SIZE_OF_INT;
//            LOGGER.info("Service " + serviceName + " @ index " + i);
//            LOGGER.info("Write " + serviceName + " at " + serviceNameOffset);
//            LOGGER.info("Write " + 0 + " at " + serviceLocationOffset);
            monitorMetaDataBuffer.putStringUtf8(serviceNameOffset, serviceName, ByteOrder.BIG_ENDIAN);
            monitorMetaDataBuffer.putInt(serviceLocationOffset, 0); // TBD

            // service reference section
            monitorMetaDataBuffer.putInt(serviceRefSection + i * BitUtil.SIZE_OF_INT, 0);
            monitorMetaDataBuffer.putInt(serviceRefSection + (i + 1) * BitUtil.SIZE_OF_INT,
                      serviceCounterLabelsBufferLength);
            monitorMetaDataBuffer.putInt(serviceRefSection + (i + 2) * BitUtil.SIZE_OF_INT, 0);
            monitorMetaDataBuffer.putInt(serviceRefSection + (i + 3) * BitUtil.SIZE_OF_INT,
                      serviceCounterValuesBufferLength);
            i++;
        }
    }

    /**
     * Creates the gateway counter labels buffer
     * @param buffer - the underlying byte buffer
     * @param metaDataBuffer - the meta data buffer from which we compute the offset
     * @return the counter labels buffer
     */
    public UnsafeBuffer createGatewayCounterLabelsBuffer(final ByteBuffer buffer, final DirectBuffer metaDataBuffer) {
        final int offset = endOfMetadata;
        final int length = metaDataBuffer.getInt(metadataItemOffset(GW_COUNTERS_LBL_BUFFERS_LENGTH_OFFSET));
        // Update offset in header section
        buffer.putInt(metadataItemOffset(GW_COUNTERS_LBL_BUFFERS_REFERENCE_OFFSET), offset);

        return new UnsafeBuffer(buffer, offset, length);
    }

    /**
     * Creates the gateway counter values buffer
     * @param buffer - the underlying byte buffer
     * @param metaDataBuffer - the meta data buffer from which we compute the offset
     * @return the counter values buffer
     */
    public UnsafeBuffer createGatewayCounterValuesBuffer(final ByteBuffer buffer, final DirectBuffer metaDataBuffer) {
        final int offset = endOfMetadata
                + metaDataBuffer.getInt(metadataItemOffset(GW_COUNTERS_LBL_BUFFERS_LENGTH_OFFSET));
        final int length = metaDataBuffer.getInt(metadataItemOffset(GW_COUNTERS_VALUE_BUFFERS_LENGTH_OFFSET));
        // Update offset in header section
        buffer.putInt(metadataItemOffset(GW_COUNTERS_VALUE_BUFFERS_REFERENCE_OFFSET), offset);

        return new UnsafeBuffer(buffer, offset, length);
    }

    /**
     * Creates the counter labels buffer for service i
     * @param buffer - the underlying byte buffer
     * @param metaDataBuffer - the meta data buffer from which we compute the offset
     * @param index identifier
     * @return the counter labels buffer
     */
    public UnsafeBuffer createServiceCounterLabelsBuffer(final ByteBuffer buffer,
                                                         final DirectBuffer metaDataBuffer,
                                                         int index) {
        final int offset = endOfMetadata
                + metaDataBuffer.getInt(metadataItemOffset(GW_COUNTERS_LBL_BUFFERS_LENGTH_OFFSET))
                + metaDataBuffer.getInt(metadataItemOffset(GW_COUNTERS_VALUE_BUFFERS_LENGTH_OFFSET)) + index
                * (serviceCounterValuesBufferLength + serviceCounterLabelsBufferLength);
        final int length = serviceCounterLabelsBufferLength;
                //metaDataBuffer.getInt(serviceRefSection + (index + 1) * BitUtil.SIZE_OF_INT);
        // Update offset in header section
        buffer.putInt(serviceRefSection + index * BitUtil.SIZE_OF_INT, offset);

        return new UnsafeBuffer(buffer, offset, length);
    }

    /**
     * Creates the counter values buffer for service i
     * @param buffer - the underlying byte buffer
     * @param metaDataBuffer - the meta data buffer from which we compute the offset
     * @param service identifier
     * @return the counter values buffer
     */
    public UnsafeBuffer createServiceCounterValuesBuffer(final ByteBuffer buffer,
                                                         final DirectBuffer metaDataBuffer,
                                                         int index) {
        final int offset = endOfMetadata
                + metaDataBuffer.getInt(metadataItemOffset(GW_COUNTERS_LBL_BUFFERS_LENGTH_OFFSET))
                + metaDataBuffer.getInt(metadataItemOffset(GW_COUNTERS_VALUE_BUFFERS_LENGTH_OFFSET)) + index
                * (serviceCounterValuesBufferLength + serviceCounterLabelsBufferLength)
                + serviceCounterLabelsBufferLength;
        final int length = serviceCounterValuesBufferLength;
                //metaDataBuffer.getInt(serviceRefSection + (index + 3) * BitUtil.SIZE_OF_INT);
        // Update offset in header section
        buffer.putInt(serviceRefSection + (index + 2) * BitUtil.SIZE_OF_INT, offset);

        return new UnsafeBuffer(buffer, offset, length);
    }

    /**
     * Method setting the number of services and metadata length
     * @param size
     */
    private void setServicesCount(int count) {
        servicesCount = count;
        metadataLength = 8 * BitUtil.SIZE_OF_INT + SIZEOF_STRING + servicesCount
                * (SIZEOF_STRING + 5 * BitUtil.SIZE_OF_INT);
        endOfMetadata = BitUtil.align(metadataLength + BitUtil.SIZE_OF_INT, BitUtil.CACHE_LINE_LENGTH);
        serviceRefSection = endOfMetadata - count * 4 * BitUtil.SIZE_OF_INT;
    }

}
