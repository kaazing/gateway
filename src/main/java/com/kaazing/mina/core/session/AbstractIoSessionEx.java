/**
 * Copyright (c) 2007-2014, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.session;

import static java.lang.Thread.currentThread;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

import javax.security.auth.Subject;

import org.apache.mina.core.filterchain.IoFilterChain;

import com.kaazing.mina.core.filterchain.DefaultIoFilterChain;
import com.kaazing.mina.core.filterchain.DefaultIoFilterChainEx;
import com.kaazing.mina.core.service.IoProcessorEx;
import com.kaazing.mina.core.write.WriteRequestEx;

/**
 * This class extends the functionality of AbstractIoSession to add support for thread alignment: the guarantee
 * that all operations on the session's filter chain take place in the same IO worker thread.
*/
public abstract class AbstractIoSessionEx extends AbstractIoSession implements IoSessionEx {

    private final boolean ioAligned;
    private final int ioLayer;
    private final ThreadLocal<WriteRequestEx> ioWriteRequest;

    private final Runnable readSuspender;
    private final Runnable readResumer;

    // volatile to support thread alignment migration
    private volatile IoFilterChain filterChain;
    private volatile Thread ioThread;
    private volatile Executor ioExecutor;
    private volatile boolean ioRegistered;

    private Subject subject;
    private final List<SubjectChangeListener> subjectChangeListeneres;

    protected AbstractIoSessionEx(int ioLayer, Thread ioThread, Executor ioExecutor,
                                  ThreadLocal<WriteRequestEx> ioWriteRequest) {
        super();
        if (ioThread == null) {
            throw new NullPointerException("ioThread");
        }
        if (ioExecutor == null) {
            throw new NullPointerException("ioExecutor");
        }

        this.ioLayer = ioLayer;
        this.ioWriteRequest = ioWriteRequest;
        boolean ioAligned = ioExecutor != IMMEDIATE_EXECUTOR && ioThread != CURRENT_THREAD;
        this.ioAligned = ioAligned;

        // note: alignment is optional before 4.0
        this.ioThread = ioThread;
        this.ioExecutor = ioExecutor;
        boolean ioRegistered = ioExecutor != NO_EXECUTOR && ioThread != NO_THREAD;
        this.ioRegistered = ioRegistered;

        this.filterChain = ioAligned ? new DefaultIoFilterChainEx(this) : new DefaultIoFilterChain(this);

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

        subjectChangeListeneres = ioAligned ? new ArrayList<SubjectChangeListener>()
                : new CopyOnWriteArrayList<SubjectChangeListener>();
    }

    @Override
    public final int getIoLayer() {
        return ioLayer;
    }

    @Override
    public final boolean isIoAligned() {
        return ioAligned;
    }

    @Override
    public final boolean isIoRegistered() {
        return ioRegistered;
    }

    @Override
    public final void setIoAlignment(Thread ioThread, Executor ioExecutor) {
        if (!ioAligned) {
            throw new IllegalStateException("ioAligned = false");
        }
        if (ioThread == null) {
            throw new NullPointerException("ioThread");
        }
        if (ioExecutor == null) {
            throw new NullPointerException("ioExecutor");
        }

        boolean ioRegistered = ioExecutor != NO_EXECUTOR && ioThread != NO_THREAD;
        if (ioRegistered && currentThread() != ioThread) {
            throw new RuntimeException("Not called from I/O thread");
        }

        // migrate the filter chain to new thread alignment
        DefaultIoFilterChainEx filterChain = (DefaultIoFilterChainEx) this.filterChain;
        DefaultIoFilterChainEx newFilterChain = new DefaultIoFilterChainEx(filterChain, ioThread, ioExecutor);
        this.filterChain = newFilterChain;

        // update I/O registered after filter chain to avoid race condition
        // where session reports that it is I/O registered but filter chain is still not executable
        this.ioThread = ioThread;
        this.ioExecutor = ioExecutor;
        this.ioRegistered = ioRegistered;

        setIoAlignment0(ioThread, ioExecutor);

        // signal to codec filter that decoder output can flush
        if (ioRegistered) {
            newFilterChain.fireMessageSent(REGISTERED_EVENT);
        }
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

    protected void setIoAlignment0(Thread ioThread, Executor ioExecutor) {
        // override
    }

    @Override
    protected WriteRequestEx nextWriteRequest(Object message, SocketAddress remoteAddress) {
        if (ioAligned) {
            WriteRequestEx writeRequest = ioWriteRequest.get();
            writeRequest.reset(this, message, remoteAddress);
            return writeRequest;
        }

        return super.nextWriteRequest(message, remoteAddress);
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

    @Override
    public Subject getSubject() {
        return subject;
    }

    /**
     * Memorizes this the Subject representing the current logged on user and fires any
     * currently registered SubjectChangeListeners
     */
    protected void setSubject(Subject subject) {
        Subject currentSubject = this.subject;
        if (!(currentSubject == null && subject == null)) {
            this.subject = subject;
            if (currentThread() == ioThread) {
                notifySubjectChanged(subject);
            }
            else {
                final Subject changedSubject = subject;
                ioExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        notifySubjectChanged(changedSubject);
                    }
                });
            }
        }
    }

    private void notifySubjectChanged(Subject subject) {
        for (SubjectChangeListener listener : subjectChangeListeneres) {
            listener.subjectChanged(subject);
        }
    }

    @Override
    public void addSubjectChangeListener(SubjectChangeListener listener) {
        subjectChangeListeneres.add(listener);
    }

    @Override
    public void removeSubjectChangeListener(SubjectChangeListener listener) {
        subjectChangeListeneres.remove(listener);
    }
}
