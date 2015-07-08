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

import static uk.co.real_logic.agrona.BitUtil.SIZE_OF_INT;

import java.nio.ByteOrder;
import java.util.Deque;
import java.util.LinkedList;
import java.util.function.BiConsumer;

import uk.co.real_logic.agrona.concurrent.AtomicBuffer;

/**
 * Manages the allocation and freeing of String monitoring entities.
 */
public class StringsManager {

    public static final int LABEL_SIZE = 1024;
    public static final int STRING_ENTITY_SIZE = 1024;
    public static final int UNREGISTERED_STRING_SIZE = -1;

    private static final ByteOrder NATIVE_BYTE_ORDER = ByteOrder.nativeOrder();

    private final AtomicBuffer labelsBuffer;
    private final AtomicBuffer valuesBuffer;
    private final Deque<Integer> freeList = new LinkedList<Integer>();

    private int idHighWaterMark = -1;

    /**
     * Create a new String monitoring entity buffer manager over two buffers.
     *
     * @param labelsBuffer containing the human readable labels for the monitoring entities.
     * @param metricsBuffer containing the values of the String monitoring entities themselves.
     */
    public StringsManager(final AtomicBuffer labelsBuffer, final AtomicBuffer valuesBuffer) {
        this.labelsBuffer = labelsBuffer;
        this.valuesBuffer = valuesBuffer;
        valuesBuffer.verifyAlignment();
    }

    /**
     * Allocate a new String monitoring entity with a given label.
     *
     * @param label to describe the entity.
     * @return the id allocated for the entity.
     */
    public int allocate(final String label) {
        final int id = getId();
        final int labelsOffset = labelOffset(id);
        if ((getEntityOffset(id) + STRING_ENTITY_SIZE) > valuesBuffer.capacity()) {
            throw new IllegalArgumentException("Unable to allocated String entity, values buffer is full");
        }

        if ((labelsOffset + LABEL_SIZE) > labelsBuffer.capacity()) {
            throw new IllegalArgumentException("Unable to allocate counter, labels buffer is full");
        }

        labelsBuffer.putStringUtf8(labelsOffset, label, NATIVE_BYTE_ORDER, LABEL_SIZE - SIZE_OF_INT);

        return id;
    }

    /**
     * Create a new String monitoring entity with a given label and value.
     *
     * @param label to describe the entity.
     * @param value of the entity.
     * @return the newly created String monitoring entity.
     */
    public AtomicStringEntity newStringEntity(final String label, final String value) {
        return new AtomicStringEntity(valuesBuffer, allocate(label), value, this);
    }

    /**
     * Free the String monitoring entity identified by id.
     *
     * @param id the entity to be freed
     */
    public void free(final int id) {
        labelsBuffer.putInt(labelOffset(id), UNREGISTERED_STRING_SIZE);
        valuesBuffer.putInt(getEntityOffset(id), UNREGISTERED_STRING_SIZE);
        freeList.push(id);
    }

    /**
     * The offset in the values buffer for a given id.
     *
     * @param id for which the offset should be provided.
     * @return the offset in the values buffer.
     */
    public static int getEntityOffset(int id) {
        return id * STRING_ENTITY_SIZE;
    }

    /**
     * Iterate over all labels in the label buffer.
     *
     * @param consumer function to be called for each label.
     */
    public void forEach(final BiConsumer<Integer, String> consumer) {
        int labelsOffset = 0;
        int size;
        int id = 0;

        while ((size = labelsBuffer.getInt(labelsOffset)) != 0) {
            if (size != UNREGISTERED_STRING_SIZE) {
                final String label = labelsBuffer.getStringUtf8(labelsOffset, NATIVE_BYTE_ORDER);
                consumer.accept(id, label);
            }

            labelsOffset += LABEL_SIZE;
            id++;
        }
    }

    /**
     * Set an {@link AtomicStringEntity} value based on id.
     *
     * @param id to be set.
     * @param value to be set for the entity.
     */
    public void setEntityValue(final int id, final String value) {
        valuesBuffer.putStringUtf8(getEntityOffset(id), value, NATIVE_BYTE_ORDER);
    }

    private int labelOffset(final int id) {
        return id * LABEL_SIZE;
    }

    private int getId() {
        if (freeList.isEmpty()) {
            return ++idHighWaterMark;
        }
        return freeList.pop();
    }
}
