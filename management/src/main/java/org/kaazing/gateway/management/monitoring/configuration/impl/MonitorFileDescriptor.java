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
import uk.co.real_logic.agrona.BitUtil;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

/**
 * Helper class used to create the initial configuration for Agrona
 */
public final class MonitorFileDescriptor {

    private static final int MONITOR_VERSION = 1;
    private static final int MONITOR_VERSION_OFFSET = 0;
    private static final int META_DATA_OFFSET = MONITOR_VERSION_OFFSET + BitUtil.SIZE_OF_INT;

    private static final int COUNTER_LABELS_BUFFER_LENGTH_OFFSET = 0;
    private static final int COUNTER_VALUES_BUFFER_LENGTH_OFFSET = COUNTER_LABELS_BUFFER_LENGTH_OFFSET + BitUtil.SIZE_OF_INT;

    private static final int STRING_LABELS_BUFFER_LENGTH_OFFSET = COUNTER_VALUES_BUFFER_LENGTH_OFFSET + BitUtil.SIZE_OF_INT;
    private static final int STRING_VALUES_BUFFER_LENGTH_OFFSET = STRING_LABELS_BUFFER_LENGTH_OFFSET + BitUtil.SIZE_OF_INT;

    private static final int META_DATA_LENGTH = STRING_VALUES_BUFFER_LENGTH_OFFSET + BitUtil.SIZE_OF_INT;

    private static final int END_OF_METADATA = BitUtil.align(META_DATA_LENGTH + BitUtil.SIZE_OF_INT, BitUtil.CACHE_LINE_LENGTH);

    private MonitorFileDescriptor() {
    }

    /**
     * Computes the total length of the file used by Agrona
     * @param totalLengthOfBuffers - the total length of the buffers
     * @return
     */
    public static int computeMonitorTotalFileLength(final int totalLengthOfBuffers) {
        return END_OF_METADATA + totalLengthOfBuffers;
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
    public static int counterLabelsBufferLengthOffset(final int baseOffset) {
        return baseOffset + META_DATA_OFFSET + COUNTER_LABELS_BUFFER_LENGTH_OFFSET;
    }

    /**
     * Computes the offset for the counter values buffer
     * @param baseOffset - the base offset of the buffer
     * @return the counter values buffer offset
     */
    public static int counterValuesBufferLengthOffset(final int baseOffset) {
        return baseOffset + META_DATA_OFFSET + COUNTER_VALUES_BUFFER_LENGTH_OFFSET;
    }

    /**
     * Computes the offset for the String monitoring entity labels buffer
     * @param baseOffset - the base offset of the buffer
     * @return the String monitoring entity labels buffer offset
     */
    public static int stringLabelsBufferLengthOffset(final int baseOffset) {
        return baseOffset + META_DATA_OFFSET + STRING_LABELS_BUFFER_LENGTH_OFFSET;
    }

    /**
     * Computes the offset for the String monitoring entity values buffer
     * @param baseOffset - the base offset of the buffer
     * @return the String monitoring entity values buffer offset
     */
    public static int stringValuesBufferLengthOffset(final int baseOffset) {
        return baseOffset + META_DATA_OFFSET + STRING_VALUES_BUFFER_LENGTH_OFFSET;
    }

    /**
     * Creates the meta data buffer
     * @param buffer - the underlying byte buffer
     * @return the meta data buffer
     */
    public static UnsafeBuffer createMetaDataBuffer(final ByteBuffer buffer) {
        return new UnsafeBuffer(buffer, 0, META_DATA_LENGTH + BitUtil.SIZE_OF_INT);
    }

    /**
     * Fills the meta data in the specified buffer
     * @param monitorMetaDataBuffer - the meta data buffer
     * @param monitorLabelsBufferLength - the length of the counters labels buffer
     * @param monitorValuesBufferLength - the length of the counters values buffer
     * @param stringLabelsBufferLength - the length of the strings labels buffer
     * @param stringValuesBufferLength - the length of the strings values buffer
     */
    public static void fillMetaData(final UnsafeBuffer monitorMetaDataBuffer, final int monitorLabelsBufferLength,
        final int monitorValuesBufferLength, final int stringLabelsBufferLength, final int stringValuesBufferLength) {
        monitorMetaDataBuffer.putInt(monitorVersionOffset(0), MONITOR_VERSION);
        monitorMetaDataBuffer.putInt(counterLabelsBufferLengthOffset(0), monitorLabelsBufferLength);
        monitorMetaDataBuffer.putInt(counterValuesBufferLengthOffset(0), monitorValuesBufferLength);
        monitorMetaDataBuffer.putInt(stringLabelsBufferLengthOffset(0), stringLabelsBufferLength);
        monitorMetaDataBuffer.putInt(stringValuesBufferLengthOffset(0), stringValuesBufferLength);
    }

    /**
     * Creates the counter labels buffer
     * @param buffer - the underlying byte buffer
     * @param metaDataBuffer - the meta data buffer from which we compute the offset
     * @return the counter labels buffer
     */
    public static UnsafeBuffer createCounterLabelsBuffer(final ByteBuffer buffer, final DirectBuffer metaDataBuffer) {
        final int offset = END_OF_METADATA;
        final int length = metaDataBuffer.getInt(counterLabelsBufferLengthOffset(0));

        return new UnsafeBuffer(buffer, offset, length);
    }

    /**
     * Creates the counter values buffer
     * @param buffer - the underlying byte buffer
     * @param metaDataBuffer - the meta data buffer from which we compute the offset
     * @return the counter values buffer
     */
    public static UnsafeBuffer createCounterValuesBuffer(final ByteBuffer buffer, final DirectBuffer metaDataBuffer) {
        final int offset = END_OF_METADATA + metaDataBuffer.getInt(counterLabelsBufferLengthOffset(0));
        final int length = metaDataBuffer.getInt(counterValuesBufferLengthOffset(0));

        return new UnsafeBuffer(buffer, offset, length);
    }

    /**
     * Creates the String monitoring entity labels buffer
     * @param buffer - the underlying byte buffer
     * @param metaDataBuffer - the meta data buffer from which we compute the offset
     * @return the String monitoring entity labels buffer
     */
    public static UnsafeBuffer createStringLabelsBuffer(final ByteBuffer buffer, final DirectBuffer metaDataBuffer) {
        final int offset =
                END_OF_METADATA + metaDataBuffer.getInt(counterLabelsBufferLengthOffset(0))
                        + metaDataBuffer.getInt(counterValuesBufferLengthOffset(0));
        final int length = metaDataBuffer.getInt(stringLabelsBufferLengthOffset(0));

        return new UnsafeBuffer(buffer, offset, length);
    }

    /**
     * Creates the String monitoring entity values buffer
     * @param buffer - the underlying byte buffer
     * @param metaDataBuffer - the meta data buffer from which we compute the offset
     * @return the String monitoring entity values buffer
     */
    public static UnsafeBuffer createStringValuesBuffer(final ByteBuffer buffer, final DirectBuffer metaDataBuffer) {
        final int offset =
                END_OF_METADATA + metaDataBuffer.getInt(counterLabelsBufferLengthOffset(0))
                        + metaDataBuffer.getInt(counterValuesBufferLengthOffset(0))
                        + metaDataBuffer.getInt(stringLabelsBufferLengthOffset(0));
        final int length = metaDataBuffer.getInt(stringValuesBufferLengthOffset(0));

        return new UnsafeBuffer(buffer, offset, length);
    }

}
