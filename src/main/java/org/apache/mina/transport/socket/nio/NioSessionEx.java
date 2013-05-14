/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package org.apache.mina.transport.socket.nio;

import java.util.concurrent.Executor;

import com.kaazing.mina.core.session.IoSessionEx;

/**
 * An extended version of NioSession which implements IoSessionEx.
 */
public abstract class NioSessionEx extends NioSession implements IoSessionEx {

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

}
