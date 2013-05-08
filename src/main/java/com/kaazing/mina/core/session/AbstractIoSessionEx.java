/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.session;

import static java.lang.Thread.currentThread;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.filterchain.IoFilterChain;
import com.kaazing.mina.core.filterchain.DefaultIoFilterChainEx;
import com.kaazing.mina.core.service.IoProcessorEx;

/**
 * This class extends the functionality of AbstractIoSession to add support for thread alignment: the guarantee
 * that all operations on the session's filter chain take place in the same IO worker thread.
*/
public abstract class AbstractIoSessionEx extends AbstractIoSession implements IoSessionEx {
    private final IoFilterChain filterChain;

    private final Thread ioThread;
    private final Executor ioExecutor;

    private final AtomicInteger readSuspendCount;
    private final Runnable readSuspender;
    private final Runnable readResumer;

    protected AbstractIoSessionEx(Thread ioThread, Executor ioExecutor) {
        super();
        if (ioThread == null) {
            throw new NullPointerException("ioThread");
        }
        if (ioExecutor == null) {
            throw new NullPointerException("ioExecutor");
        }
        this.ioThread = ioThread;
        this.ioExecutor = ioExecutor;
        this.filterChain = new DefaultIoFilterChainEx(this);
        this.readSuspendCount = new AtomicInteger();
        this.readSuspender = new Runnable() {
            @Override
            public void run() {
                AbstractIoSessionEx.super.suspendRead();
            }
        };
        this.readResumer = new Runnable() {
            @Override
            public void run() {
                AbstractIoSessionEx.super.resumeRead();
            }
        };
    }

    @Override
    public boolean isThreadSafe() {
        return true;
    }

    @Override
    public final IoFilterChain getFilterChain() {
        return filterChain;
    }

    @Override
    public Thread getIoThread() {
        return ioThread;
    }

    @Override
    public Executor getIoExecutor() {
        return ioExecutor;
    }

    @SuppressWarnings("rawtypes")
    public abstract IoProcessorEx getProcessor();

    @Override
    public void suspendRead() {
        // manage the readSuspendCount here atomically instead of superclass to minimize scheduling overhead
        if (readSuspendCount.getAndIncrement() == 0) {
            if (currentThread() == ioThread) {
                super.suspendRead();
            }
            else {
                ioExecutor.execute(readSuspender);
            }
        }
    }

    @Override
    public void resumeRead() {
        switch (readSuspendCount.decrementAndGet()) {
        case -1:
            throw new IllegalStateException("resumeRead not balanced by previous suspendRead");
        case 0:
            if (currentThread() == ioThread) {
                super.resumeRead();
            }
            else {
                ioExecutor.execute(readResumer);
            }
            break;
        }
    }

    @Override
    protected void doCloseOnFlush() {
        // Ensure getProcessor().flush() is executed in this session's IO thread.
        if (Thread.currentThread() == getIoThread()) {
            closeOnFlushTask.run();
        }
        else {
            getIoExecutor().execute(closeOnFlushTask);
        }
    }

    private Runnable closeOnFlushTask = new Runnable() {
        @SuppressWarnings("unchecked")
        @Override public void run() {
            getWriteRequestQueue().offer(AbstractIoSessionEx.this, CLOSE_REQUEST);
            getProcessor().flush(AbstractIoSessionEx.this);
        }
    };
}
