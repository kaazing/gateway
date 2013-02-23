/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.session;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.file.DefaultFileRegion;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.DefaultCloseFuture;
import org.apache.mina.core.future.DefaultReadFuture;
import org.apache.mina.core.future.DefaultWriteFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.AbstractIoService;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoEventType;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionAttributeMap;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteException;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;
import org.apache.mina.core.write.WriteTimeoutException;
import org.apache.mina.core.write.WriteToClosedSessionException;
import org.apache.mina.util.CircularQueue;
import org.apache.mina.util.ExceptionMonitor;


/**
 * This class extends the functionality of AbstractIoSession to add support for thread alignment: the guarantee
 * that all operations on the session's filter chain take place in the same IO worker thread.
*/   
public abstract class AbstractIoSessionEx extends AbstractIoSession implements IoSessionEx {
    
    private Thread owner; // TODO: how/when is this set?
    private Executor executor; // TODO: how/when is this set?
    
    protected AbstractIoSessionEx() {
        super();
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
    
    protected void setIoThread(Thread thread) {
        this.owner = thread;
    }
    
    
}
