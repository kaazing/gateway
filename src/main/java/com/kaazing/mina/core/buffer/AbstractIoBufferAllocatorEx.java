/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package com.kaazing.mina.core.buffer;

import java.nio.ByteBuffer;

import org.apache.mina.core.buffer.IoBufferAllocator;

public abstract class AbstractIoBufferAllocatorEx<T extends AbstractIoBufferEx> implements IoBufferAllocatorEx<T> {

    private IoBufferAllocator allocator;

    public final IoBufferAllocator asBufferAllocator() {
        if (allocator == null) {
            allocator = new IoBufferAllocator() {

                @Override
                public T wrap(ByteBuffer nioBuffer) {
                    return AbstractIoBufferAllocatorEx.this.wrap(nioBuffer, false);
                }

                @Override
                public ByteBuffer allocateNioBuffer(int capacity, boolean direct) {
                    return AbstractIoBufferAllocatorEx.this.allocateNioBuffer(capacity, direct);
                }

                @Override
                public T allocate(int capacity, boolean direct) {
                    ByteBuffer nioBuffer = AbstractIoBufferAllocatorEx.this.allocateNioBuffer(capacity, direct);
                    return AbstractIoBufferAllocatorEx.this.wrap(nioBuffer, /* shared */ false);
                }

                @Override
                public void dispose() {
                }
            };
        }
        return allocator;
    }

    @Override
    public final T allocate(int capacity, boolean shared) {
        ByteBuffer nioBuffer = allocateNioBuffer(capacity);
        return wrap(nioBuffer, shared);
    }

    @Override
    public final T duplicate(IoBufferEx buf) {
        return wrap(buf.buf(), buf.isShared());
    }

    public abstract ByteBuffer allocateNioBuffer(int capacity);

    public abstract T wrap(ByteBuffer nioBuffer, boolean shared);

    protected final ByteBuffer allocateNioBuffer(int capacity, boolean direct) {
        ByteBuffer nioBuffer;
        if (direct) {
            nioBuffer = ByteBuffer.allocateDirect(capacity);
        } else {
            nioBuffer = ByteBuffer.allocate(capacity);
        }
        return nioBuffer;
    }
}
