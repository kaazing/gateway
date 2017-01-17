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

import java.io.File;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;

import org.kaazing.gateway.management.monitoring.configuration.MonitorFileWriter;
import org.kaazing.gateway.management.monitoring.service.MonitoredService;
import org.kaazing.gateway.management.monitoring.writer.GatewayWriter;
import org.kaazing.gateway.management.monitoring.writer.ServiceWriter;
import org.kaazing.gateway.management.monitoring.writer.impl.MMFGatewayWriter;
import org.kaazing.gateway.management.monitoring.writer.impl.MMFServiceWriter;
import org.kaazing.gateway.service.MonitoringEntityFactory;

import org.agrona.BitUtil;
import org.agrona.IoUtil;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * Class responsible for storing/writing information in the appropriate the MMF format.
 *
 * File layout:
 * +-----------------------------------------------------------------------+
 * | File version | GW data offset | Service mappings offset | | GW ID | GW counters
 * lbl buffer offset | GW counters lbl buffer length | GW counters values buffer
 * offset | GW counters values buffer length | | Number of services | Service 1 name
 * | Service 1 offset | ... | Service 1 lbl buffer offset | Service 1 lbl buffer length |
 * | Service 1 values buffer offset | Service 1 values buffer length | | ... |
 * | GW counters labels buffer | | GW counters values buffer | | Service 1 labels buffer |
 * | Service 1 values buffer || ... |
 * +-----------------------------------------------------------------------+
 * Metadata length: NUMBER_OF_INTS_IN_HEADER * BitUtil.SIZE_OF_INT + SIZEOF_STRING +
 * servicesCount * (SIZEOF_STRING + NUMBER_OF_INTS_PER_SERVICE * BitUtil.SIZE_OF_INT)
 */
public final class MonitorFileWriterImpl implements MonitorFileWriter {
    private static final int OFFSETS_PER_SERVICE = 4;
    private static final int NUMBER_OF_INTS_PER_SERVICE = 5;
    private static final int NUMBER_OF_INTS_IN_HEADER = 8;
    private static final int SIZEOF_STRING = 128;
    private static final int SIZEOF_ENTRY = 128;
    private static final int ENTRIES_COUNT = 1024;
    private static final int MAX_SERVICE_COUNT = 100;

    private static final int MONITOR_VERSION = 1;
    private static final int MONITOR_VERSION_OFFSET = 0;
    private static final int GW_DATA_REFERENCE_OFFSET = MONITOR_VERSION_OFFSET + BitUtil.SIZE_OF_INT;
    private static final int SERVICE_DATA_REFERENCE_OFFSET = GW_DATA_REFERENCE_OFFSET + BitUtil.SIZE_OF_INT;

    private static final int GW_ID_OFFSET = SERVICE_DATA_REFERENCE_OFFSET + BitUtil.SIZE_OF_INT;
    private static final int GW_DATA_OFFSET = GW_ID_OFFSET;

    private static final int GATEWAY_COUNTER_VALUES_BUFFER_LENGTH = ENTRIES_COUNT * SIZEOF_ENTRY;
    private static final int GATEWAY_COUNTER_LABELS_BUFFER_LENGTH = 2 * GATEWAY_COUNTER_VALUES_BUFFER_LENGTH;
    private static final int SERVICE_COUNTER_VALUES_BUFFER_LENGTH = ENTRIES_COUNT * SIZEOF_ENTRY;
    private static final int SERVICE_COUNTER_LABELS_BUFFER_LENGTH = 2 * SERVICE_COUNTER_VALUES_BUFFER_LENGTH;

    private int gwCountersLblBuffersReferenceOffset;
    private int gwCountersLblBuffersLengthOffset;
    private int gwCountersValueBuffersReferenceOffset;
    private int gwCountersValueBuffersLengthOffset;
    private int noOfServicesOffset;
    private int serviceDataOffset;
    private int metadataLength;
    private int servicesCount;
    private int endOfMetadata;
    private int serviceRefSection;
    private UnsafeBuffer metaDataBuffer;
    private String gatewayId;
    private int prevServiceOffset;
    private String prevServiceName = "";
    private MappedByteBuffer mappedMonitorFile;

    /**
     * MonitorFileDescriptor constructor
     * @param services
     * @param gatewayId
     */
    public MonitorFileWriterImpl(String gatewayId) {
        this.gatewayId = gatewayId;
        setGatewayIdDependentOffsets(gatewayId);
        setServicesCount();
    }


    @Override
    public void initialize(File monitoringFile) {
        int fileSize = computeMonitorTotalFileLength();
        mappedMonitorFile = IoUtil.mapNewFile(monitoringFile, fileSize);
        metaDataBuffer = new UnsafeBuffer(mappedMonitorFile, 0, metadataLength + BitUtil.SIZE_OF_INT);
        fillMetaData();
    }

    @Override
    public UnsafeBuffer createGatewayCounterLabelsBuffer() {
        final int offset = endOfMetadata;
        final int length = metaDataBuffer.getInt(gwCountersLblBuffersLengthOffset);
        // Update offset in header section
        metaDataBuffer.putInt(gwCountersLblBuffersReferenceOffset, offset);

        return new UnsafeBuffer(mappedMonitorFile, offset, length);
    }

    @Override
    public UnsafeBuffer createGatewayCounterValuesBuffer() {
        final int offset = endOfMetadata
                + metaDataBuffer.getInt(gwCountersLblBuffersLengthOffset);
        final int length = metaDataBuffer.getInt(gwCountersValueBuffersLengthOffset);
        // Update offset in header section
        metaDataBuffer.putInt(gwCountersValueBuffersReferenceOffset, offset);

        return new UnsafeBuffer(mappedMonitorFile, offset, length);
    }

    @Override
    public UnsafeBuffer createServiceCounterLabelsBuffer(int index) {
        final int offset = endOfMetadata
                + metaDataBuffer.getInt(gwCountersLblBuffersLengthOffset)
                + metaDataBuffer.getInt(gwCountersValueBuffersLengthOffset) + index
                * (SERVICE_COUNTER_VALUES_BUFFER_LENGTH + SERVICE_COUNTER_LABELS_BUFFER_LENGTH);
        final int length = SERVICE_COUNTER_LABELS_BUFFER_LENGTH;

        // Update offset in header section
        metaDataBuffer.putInt(serviceRefSection + index * OFFSETS_PER_SERVICE * BitUtil.SIZE_OF_INT, offset);

        return new UnsafeBuffer(mappedMonitorFile, offset, length);
    }

    @Override
    public UnsafeBuffer createServiceCounterValuesBuffer(int index) {
        final int offset = endOfMetadata
                + metaDataBuffer.getInt(gwCountersLblBuffersLengthOffset)
                + metaDataBuffer.getInt(gwCountersValueBuffersLengthOffset) + index
                * (SERVICE_COUNTER_VALUES_BUFFER_LENGTH + SERVICE_COUNTER_LABELS_BUFFER_LENGTH)
                + SERVICE_COUNTER_LABELS_BUFFER_LENGTH;
        final int length = SERVICE_COUNTER_VALUES_BUFFER_LENGTH;

        // Update offset in header section
        metaDataBuffer.putInt(serviceRefSection + (index * OFFSETS_PER_SERVICE + 2) * BitUtil.SIZE_OF_INT, offset);

        return new UnsafeBuffer(mappedMonitorFile, offset, length);
    }

    @Override
    public MonitoringEntityFactory getGatewayMonitoringEntityFactory() {
        GatewayWriter gatewayWriter = new MMFGatewayWriter(this);
        return gatewayWriter.writeCountersFactory();
    }

    @Override
    public MonitoringEntityFactory getServiceMonitoringEntityFactory(
             MonitoredService monitoredService, int index) {
        fillServiceMetadata(monitoredService.getServiceName(), index);
        //create service writer
        ServiceWriter serviceWriter = new MMFServiceWriter(this, index);
        return serviceWriter.writeCountersFactory();
    }

    @Override
    public void close(File monitoringDir) {
        IoUtil.unmap(mappedMonitorFile);
        IoUtil.delete(monitoringDir, false);
    }

    /**
     * Method setting the number of services and metadata length
     */
    private void setServicesCount() {
        servicesCount = MAX_SERVICE_COUNT;
        metadataLength = NUMBER_OF_INTS_IN_HEADER * BitUtil.SIZE_OF_INT + SIZEOF_STRING + servicesCount * (SIZEOF_STRING +
                NUMBER_OF_INTS_PER_SERVICE * BitUtil.SIZE_OF_INT);
        endOfMetadata = BitUtil.align(metadataLength + BitUtil.SIZE_OF_INT, BitUtil.CACHE_LINE_LENGTH);
        serviceRefSection = endOfMetadata - servicesCount * OFFSETS_PER_SERVICE * BitUtil.SIZE_OF_INT;
    }

    /**
     * Fills the meta data in the specified buffer
     * @param monitorMetaDataBuffer - the meta data buffer
     */
    private void fillMetaData() {
        metaDataBuffer.putInt(MONITOR_VERSION_OFFSET, MONITOR_VERSION);
        metaDataBuffer.putInt(GW_DATA_REFERENCE_OFFSET, GW_DATA_OFFSET);
        metaDataBuffer.putInt(SERVICE_DATA_REFERENCE_OFFSET, serviceDataOffset);
        metaDataBuffer.putStringUtf8(GW_ID_OFFSET, gatewayId, ByteOrder.nativeOrder());
        metaDataBuffer.putInt(gwCountersLblBuffersReferenceOffset, 0);
        metaDataBuffer.putInt(gwCountersLblBuffersLengthOffset, GATEWAY_COUNTER_LABELS_BUFFER_LENGTH);
        metaDataBuffer.putInt(gwCountersValueBuffersReferenceOffset, 0);
        metaDataBuffer.putInt(gwCountersValueBuffersLengthOffset, GATEWAY_COUNTER_VALUES_BUFFER_LENGTH);
        metaDataBuffer.putInt(noOfServicesOffset, 0);
    }

    /**
     * Method adding services metadata
     * @param monitorMetaDataBuffer - the metadata buffer
     */
    private void fillServiceMetadata(final String serviceName, final int index) {
        final int servAreaOffset = noOfServicesOffset + BitUtil.SIZE_OF_INT;
        metaDataBuffer.putInt(noOfServicesOffset, metaDataBuffer.getInt(noOfServicesOffset) + 1);
        int serviceNameOffset = getServiceNameOffset(servAreaOffset);
        int serviceLocationOffset = serviceNameOffset + serviceName.length() + BitUtil.SIZE_OF_INT;
        metaDataBuffer.putStringUtf8(serviceNameOffset, serviceName, ByteOrder.nativeOrder());

        initializeServiceRefMetadata(serviceLocationOffset, index * OFFSETS_PER_SERVICE);
        prevServiceOffset = serviceNameOffset;
        prevServiceName = serviceName;
    }

    /**
     * Method returning serviceNameOffset
     * @param servAreaOffset
     * @return
     */
    private int getServiceNameOffset(final int servAreaOffset) {
        // if there are other services which have been written
        if (prevServiceOffset != 0) {
            return prevServiceOffset + prevServiceName.length() + BitUtil.SIZE_OF_INT + BitUtil.SIZE_OF_INT;
        }
        // else
        return servAreaOffset;
    }

    /**
     * Method initializing service ref metadata section data
     * @param serviceLocationOffset
     * @param serviceOffsetIndex
     */
    private void initializeServiceRefMetadata(int serviceLocationOffset, int serviceOffsetIndex) {
        metaDataBuffer.putInt(serviceLocationOffset, serviceRefSection + serviceOffsetIndex * BitUtil.SIZE_OF_INT);

        // service reference section
        metaDataBuffer.putInt(serviceRefSection + serviceOffsetIndex * BitUtil.SIZE_OF_INT, 0);
        metaDataBuffer.putInt(serviceRefSection + (serviceOffsetIndex + 1) * BitUtil.SIZE_OF_INT,
                SERVICE_COUNTER_LABELS_BUFFER_LENGTH);
        metaDataBuffer.putInt(serviceRefSection + (serviceOffsetIndex + 2) * BitUtil.SIZE_OF_INT, 0);
        metaDataBuffer.putInt(serviceRefSection + (serviceOffsetIndex + 3) * BitUtil.SIZE_OF_INT,
                SERVICE_COUNTER_VALUES_BUFFER_LENGTH);
    }

    /**
     * Method setting gatewayId dependent offsets
     * @param gatewayId
     */
    private void setGatewayIdDependentOffsets(String gatewayId) {
        gwCountersLblBuffersReferenceOffset = GW_ID_OFFSET + gatewayId.length() + BitUtil.SIZE_OF_INT;
        gwCountersLblBuffersLengthOffset = gwCountersLblBuffersReferenceOffset + BitUtil.SIZE_OF_INT;
        gwCountersValueBuffersReferenceOffset = gwCountersLblBuffersLengthOffset + BitUtil.SIZE_OF_INT;
        gwCountersValueBuffersLengthOffset = gwCountersValueBuffersReferenceOffset + BitUtil.SIZE_OF_INT;
        noOfServicesOffset = gwCountersValueBuffersLengthOffset + BitUtil.SIZE_OF_INT;
        serviceDataOffset = noOfServicesOffset;
    }

    /**
     * Computes the total length of the file used by Agrona
     * @return
     */
    private int computeMonitorTotalFileLength() {
        int totalLengthOfBuffers =
                GATEWAY_COUNTER_LABELS_BUFFER_LENGTH + GATEWAY_COUNTER_VALUES_BUFFER_LENGTH +
                MAX_SERVICE_COUNT * (SERVICE_COUNTER_VALUES_BUFFER_LENGTH + SERVICE_COUNTER_LABELS_BUFFER_LENGTH);
        return endOfMetadata + totalLengthOfBuffers;
    }
}
