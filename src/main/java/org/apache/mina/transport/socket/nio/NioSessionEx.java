/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package org.apache.mina.transport.socket.nio;

import java.util.concurrent.Executor;

import com.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import com.kaazing.mina.core.buffer.IoBufferEx;
import com.kaazing.mina.core.buffer.SimpleBufferAllocator;
import com.kaazing.mina.core.session.IoSessionEx;

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
}
