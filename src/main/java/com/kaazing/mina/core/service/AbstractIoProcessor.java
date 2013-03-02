/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.service;

import static com.kaazing.mina.core.util.Util.verifyInIoThread;

import org.apache.mina.core.service.IoProcessor;

import com.kaazing.mina.core.session.IoSessionEx;

/**
 * All of our IoProcessor implementations (in mina.netty and gateway.server) must extend this class, which guarantees
 * all session related methods will be executed on the session's I/O thread (aka thread alignment). If any such
 * method is called in a thread other than the session's I/O thread, it will be scheduled for execution in the I/O thread,
 * otherwise it will be executed immediately. This is similar to what is done in DefaultIoFilterChainEx. The objective
 * is to ensure that all pipeline processing for a given IoSession is done in the same thread.
 */
public abstract class AbstractIoProcessor<T extends IoSessionEx> implements IoProcessor<T>  {

    // By definition, dispose() (and isDisposed/ing) must be callable from a non-IO thread, as must processor constructors,
    // since in principle the I/O threads don't even exist until the processor is created and started

    @Override
    public final void add(final T session) {
    	verifyInIoThread(session, session.getIoThread());
        add0(session);
    }
    protected abstract void add0(T session);

    @Override
    public final void flush(final T session) {
    	verifyInIoThread(session, session.getIoThread());
        flush0(session);
    }
    protected abstract void flush0(T session);

    @Override
    public final void remove(final T session) {
    	verifyInIoThread(session, session.getIoThread());
        remove0(session);
    }
    protected abstract void remove0(T session);
    
    @Override
    public final void updateTrafficControl(final T session) {
    	verifyInIoThread(session, session.getIoThread());
        updateTrafficControl0(session);
    }
    protected abstract void updateTrafficControl0(T session);

}
