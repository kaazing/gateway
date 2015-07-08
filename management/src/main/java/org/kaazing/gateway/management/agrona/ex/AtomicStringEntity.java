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

package org.kaazing.gateway.management.agrona.ex;

import java.nio.ByteOrder;

import uk.co.real_logic.agrona.concurrent.AtomicBuffer;

/**
 * Atomic String monitoring entity that is backed by an {@link AtomicBuffer} that can be read across threads and processes.
 */
public class AtomicStringEntity implements AutoCloseable {
    private final AtomicBuffer buffer;
    private final int id;
    private final StringsManager stringsManager;
    private final int offset;
    private static final ByteOrder NATIVE_BYTE_ORDER = ByteOrder.nativeOrder();
    private static final String DEFAULT_VALUE = "";

    AtomicStringEntity(final AtomicBuffer buffer, final int id, final StringsManager stringsManager) {
        this(buffer, id, DEFAULT_VALUE, stringsManager);
    }

    AtomicStringEntity(final AtomicBuffer buffer, final int id, final String value, final StringsManager stringsManager) {
        this.buffer = buffer;
        this.id = id;
        this.stringsManager = stringsManager;
        this.offset = StringsManager.getEntityOffset(id);
        buffer.putStringUtf8(offset, value, NATIVE_BYTE_ORDER);
    }

    /**
     * Set the value of the String monitoring entity.
     *
     * @param value to be set.
     */
    public void set(final String value) {
        buffer.putStringUtf8(offset, value, NATIVE_BYTE_ORDER);
    }

    /**
     * Set the value of the String monitoring entity with a length prefix.
     *
     * @param value to be set.
     * @param byteOrder for the length prefix.
     */
    public void set(final String value, final ByteOrder byteOrder) {
        buffer.putStringUtf8(offset, value, byteOrder);
    }

    /**
     * Get the latest value for the String monitoring entity.
     *
     * @return the latest value for the String monitoring entity.
     */
    public String get() {
        return buffer.getStringUtf8(offset, NATIVE_BYTE_ORDER);
    }

    /**
     * Free the counter slot for reuse.
     */
    @Override
    public void close() {
        stringsManager.free(id);
    }
}
