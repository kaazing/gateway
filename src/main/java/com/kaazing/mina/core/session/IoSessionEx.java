/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.session;

import static org.apache.mina.core.future.DefaultWriteFuture.newWrittenFuture;

import java.net.SocketAddress;
import java.util.concurrent.Executor;

import org.apache.mina.core.future.DefaultWriteFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

import com.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import com.kaazing.mina.core.service.IoServiceEx;

/**
 * Extended version of IoSession to add support for thread alignment.
*/
public interface IoSessionEx extends IoSession, IoAlignment {

    Thread CURRENT_THREAD = new Thread(new Runnable() {
        @Override
        public void run() {
            throw new IllegalStateException("not runnable");
        }

        @Override
        public String toString() {
            return "CURRENT_THREAD";
        }
    });

    Executor IMMEDIATE_EXECUTOR = new Executor() {
        @Override
        public void execute(Runnable command) {
            command.run();
        }

        @Override
        public String toString() {
            return "IMMEDIATE_EXECUTOR";
        }
    };

    Thread NO_THREAD = new Thread(new Runnable() {
        @Override
        public void run() {
            throw new IllegalStateException("not runnable");
        }

        @Override
        public String toString() {
            return "NO_THREAD";
        }
    });

    Executor NO_EXECUTOR = new Executor() {
        @Override
        public void execute(Runnable command) {
            throw new IllegalStateException("not executable");
        }

        @Override
        public String toString() {
            return "NO_EXECUTOR";
        }
    };

    WriteRequest REGISTERED_EVENT = new WriteRequest() {

        private final WriteFuture future = newWrittenFuture(null);

        @Override
        public WriteRequest getOriginalRequest() {
            return null;
        }

        @Override
        public Object getMessage() {
            return null;
        }

        @Override
        public WriteFuture getFuture() {
            return future;
        }

        @Override
        public SocketAddress getDestination() {
            return null;
        }
    };

    /**
     * Returns the I/O layer this session represents, with 0 at the base (eg. TCP) and incrementing up through higher layers.
     */
    int getIoLayer();

    /**
     * Returns the IO worker thread in which all filters on the filter chain for this session will be executed
     */
    Thread getIoThread();

    /**
     * Returns an Executor which can be used to execute tasks in the IO worker thread that owns this session.
     * This executor delegates to the executeInIoThread method of the corresponding Netty NioWorker, so when invoked
     * from that same thread, the Executor will immediately execute the task, and when invoked from a different thread,
     * the task will be queued for asynchronous (but quasi-immediate) execution in the worker thread.
     */
    Executor getIoExecutor();

    IoBufferAllocatorEx<?> getBufferAllocator();

    @Override
    IoSessionConfigEx getConfig();

    @Override
    IoServiceEx getService();

    @Override
    boolean isIoAligned();

    void setIoAlignment(Thread ioThread, Executor ioExecutor);

    boolean isIoRegistered();
}
