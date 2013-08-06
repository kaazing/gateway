/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.session;

import static java.lang.Thread.currentThread;

import java.util.concurrent.Executor;

import org.apache.mina.core.filterchain.IoFilterChain;

import com.kaazing.mina.core.filterchain.DefaultIoFilterChain;
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
    private final boolean ioAligned;

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

        // note: alignment is optional before 4.0
        boolean ioAligned = ioExecutor != IMMEDIATE_EXECUTOR && ioThread != CURRENT_THREAD;
        this.filterChain = ioAligned ? new DefaultIoFilterChainEx(this) : new DefaultIoFilterChain(this);
        this.ioAligned = ioAligned;

        this.readSuspender = new Runnable() {
            @Override
            public void run() {
                suspendRead1();
            }
        };
        this.readResumer = new Runnable() {
            @Override
            public void run() {
                resumeRead1();
            }
        };
    }

    @Override
    public final boolean isIoAligned() {
        return ioAligned;
    }

    @Override
    public final IoFilterChain getFilterChain() {
        return filterChain;
    }

    @Override
    public final Thread getIoThread() {
        return ioThread;
    }

    @Override
    public final Executor getIoExecutor() {
        return ioExecutor;
    }

    @SuppressWarnings("rawtypes")
    public abstract IoProcessorEx getProcessor();

    @Override
    protected final void suspendRead0() {
        if (currentThread() == ioThread) {
            readSuspender.run();
        }
        else {
            ioExecutor.execute(readSuspender);
        }
    }

    protected void suspendRead1() {
        super.suspendRead0();
    }

    @Override
    protected final void resumeRead0() {
        if (currentThread() == ioThread) {
            readResumer.run();
        }
        else {
            ioExecutor.execute(readResumer);
        }
    }

    protected void resumeRead1() {
        super.resumeRead0();
    }

    @Override
    protected void doCloseOnFlush() {
        // Ensure getProcessor().flush() is executed in this session's IO thread.
        if (currentThread() == ioThread) {
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
