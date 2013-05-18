/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package org.apache.mina.transport.socket.nio;

import static org.apache.mina.core.buffer.IoBuffer.setAllocator;

import java.util.concurrent.Executor;

import com.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import com.kaazing.mina.core.buffer.IoBufferEx;
import com.kaazing.mina.core.session.IoSessionEx;

/**
 * An extended version of NioSession which implements IoSessionEx.
 */
public abstract class NioSessionEx extends NioSession implements IoSessionEx {

    static {
        // MINA has static scope buffer allocator used for socket reads
        // so we need to override default allocator when NioSessionEx is used
        setAllocator(BUFFER_ALLOCATOR.asBufferAllocator());
    }

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
        return BUFFER_ALLOCATOR;
    }
}
