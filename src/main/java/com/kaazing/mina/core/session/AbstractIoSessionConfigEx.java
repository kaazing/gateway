/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.session;

import org.apache.mina.core.session.AbstractIoSessionConfig;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSessionConfig;

/**
 * Extended version of AbstractIoSessionConfig to add support for millisecond precise idle timeouts.
*/   
public abstract class AbstractIoSessionConfigEx extends AbstractIoSessionConfig implements IoSessionConfigEx  {
    
    private volatile long idleTimeMillisForRead;
    private volatile long idleTimeMillisForWrite;
    private volatile long idleTimeMillisForBoth;

    @Override
    protected final void doSetAll(IoSessionConfig config) {
        doSetAll( (IoSessionConfigEx)config );
    }

    protected abstract void doSetAll(IoSessionConfigEx config);

    @Override
    public int getIdleTime(IdleStatus status) {
        return (int) getIdleTimeInMillis(status) / 1000;
    }
    
    @Override
    public long getIdleTimeInMillis(IdleStatus status) {
        if (status == IdleStatus.BOTH_IDLE) {
            return idleTimeMillisForBoth;
        }

        if (status == IdleStatus.READER_IDLE) {
            return idleTimeMillisForRead;
        }

        if (status == IdleStatus.WRITER_IDLE) {
            return idleTimeMillisForWrite;
        }

        throw new IllegalArgumentException("Unknown idle status: " + status);
    }
    
    @Override
    public void setIdleTime(IdleStatus status, int idleTime) {
        if (idleTime < 0) {
            throw new IllegalArgumentException("Illegal idle time: " + idleTime);
        }
        long idleTimeMillis = idleTime * 1000L;
        setIdleTimeInMillis(status, idleTimeMillis);
    }

    @Override
    public void setIdleTimeInMillis(IdleStatus status, long idleTimeMillis) {
        if (idleTimeMillis < 0) {
            throw new IllegalArgumentException("Illegal idle time: " + idleTimeMillis);
        }

        if (status == IdleStatus.BOTH_IDLE) {
            idleTimeMillisForBoth = idleTimeMillis;
        } else if (status == IdleStatus.READER_IDLE) {
            idleTimeMillisForRead = idleTimeMillis;
        } else if (status == IdleStatus.WRITER_IDLE) {
            idleTimeMillisForWrite = idleTimeMillis;
        } else {
            throw new IllegalArgumentException("Unknown idle status: " + status);
        }
    }
    
}
