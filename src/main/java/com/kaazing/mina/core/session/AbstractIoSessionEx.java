/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.session;

import java.util.concurrent.Executor;

import org.apache.mina.core.filterchain.IoFilterChain;

import com.kaazing.mina.core.filterchain.DefaultIoFilterChainEx;


/**
 * This class extends the functionality of AbstractIoSession to add support for thread alignment: the guarantee
 * that all operations on the session's filter chain take place in the same IO worker thread.
*/   
public abstract class AbstractIoSessionEx extends AbstractIoSession implements IoSessionEx {
    private final IoFilterChain filterChain;
    
    private final Thread owner;
    private Executor executor; // TODO: how/when is this set?
    
    protected AbstractIoSessionEx() {
        super();
        this.owner = Thread.currentThread();
        this.filterChain = new DefaultIoFilterChainEx(this);
    }
    
    @Override
    public final IoFilterChain getFilterChain() {
        return filterChain;
    }

    @Override
    public Thread getIoThread() {
        return owner;
    }
    
    @Override
    public Executor getIoExecutor() {
        return executor;
    }
    
    protected void setIoExecutor(Executor executor) {
        this.executor = executor;
    }
    
//    protected void setIoThread(Thread thread) {
//        this.owner = thread;
//    }

    
}
