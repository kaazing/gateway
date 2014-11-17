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

package org.apache.mina.transport.socket.nio;

import java.util.concurrent.Executor;

import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.buffer.SimpleBufferAllocator;
import org.kaazing.mina.core.session.IoSessionEx;

/**
 * An extended version of NioSession which implements IoSessionEx.
 */
public abstract class NioSessionEx extends NioSession implements IoSessionEx {

    private IoBufferEx incompleteSharedWriteBuffer;

    public final void setIncompleteSharedWriteBuffer(IoBufferEx incompleteSharedWriteBuffer) {
        this.incompleteSharedWriteBuffer = incompleteSharedWriteBuffer;
    }

    public final IoBufferEx getIncompleteSharedWriteBuffer() {
        return incompleteSharedWriteBuffer;
    }

    @Override
    public Thread getIoThread() {
        // immediate execution
        return CURRENT_THREAD;
    }

    @Override
    public Executor getIoExecutor() {
        // immediate execution
        return IMMEDIATE_EXECUTOR;
    }

    @Override
    public boolean isIoAligned() {
        return false;
    }

    @Override
    public IoBufferAllocatorEx<?> getBufferAllocator() {
        return SimpleBufferAllocator.BUFFER_ALLOCATOR;
    }

    @Override
    public void setIoAlignment(Thread ioThread, Executor ioExecutor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isIoRegistered() {
        return true;
    }

}
