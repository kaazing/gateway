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
package org.kaazing.mina.core.buffer;

import java.nio.ByteBuffer;

public abstract class AbstractIoBufferAllocatorEx<T extends AbstractIoBufferEx> implements IoBufferAllocatorEx<T> {

    @Override
    public final ByteBuffer allocate(int capacity) {
        return allocate(capacity, IoBufferEx.FLAG_NONE);
    }

    @Override
    public abstract ByteBuffer allocate(int capacity, int flags);

    public T wrap(byte[] array)
    {
        return wrap(ByteBuffer.wrap(array));
    }

    @Override
    public T wrap(ByteBuffer nioBuffer) {
        return wrap(nioBuffer, IoBufferEx.FLAG_NONE);
    }

    @Override
    public abstract T wrap(ByteBuffer nioBuffer, int flags);

    protected final ByteBuffer allocateNioBuffer0(int capacity, int flags) {
        boolean direct = (flags & IoBufferEx.FLAG_DIRECT) != IoBufferEx.FLAG_NONE;
        ByteBuffer nioBuffer;
        if (direct) {
            nioBuffer = ByteBuffer.allocateDirect(capacity);
        } else {
            nioBuffer = ByteBuffer.allocate(capacity);
        }
        return nioBuffer;
    }
}
