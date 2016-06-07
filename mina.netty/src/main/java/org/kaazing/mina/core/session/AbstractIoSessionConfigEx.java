/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kaazing.mina.core.session;

import org.apache.mina.core.session.AbstractIoSessionConfig;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSessionConfig;

/**
 * Extended version of AbstractIoSessionConfig to add support for millisecond precise idle timeouts.
*/
public abstract class AbstractIoSessionConfigEx extends AbstractIoSessionConfig implements IoSessionConfigEx  {

    private static final ChangeListener DEFAULT_CHANGE_LISTENER = new ChangeListener() {
        @Override
        public void idleTimeInMillisChanged(IdleStatus status,
                long idleTimeMillis) {
        }
    };

    private volatile long idleTimeMillisForRead;
    private volatile long idleTimeMillisForWrite;
    private volatile long idleTimeMillisForBoth;

    private volatile ChangeListener listener = DEFAULT_CHANGE_LISTENER;

    @Override
    protected final void doSetAll(IoSessionConfig config) {

        setIdleTimeInMillis(IdleStatus.BOTH_IDLE, config.getBothIdleTimeInMillis());
        setIdleTimeInMillis(IdleStatus.READER_IDLE, config.getReaderIdleTimeInMillis());
        setIdleTimeInMillis(IdleStatus.WRITER_IDLE, config.getWriterIdleTimeInMillis());

        doSetAll((IoSessionConfigEx) config);
    }

    protected abstract void doSetAll(IoSessionConfigEx config);

    @Override
    public void setChangeListener(ChangeListener listener) {

        ChangeListener oldListener = this.listener;
        ChangeListener newListener = listener != null ? listener : DEFAULT_CHANGE_LISTENER;

        // deactivate notifications
        oldListener.idleTimeInMillisChanged(IdleStatus.BOTH_IDLE, 0L);
        oldListener.idleTimeInMillisChanged(IdleStatus.READER_IDLE, 0L);
        oldListener.idleTimeInMillisChanged(IdleStatus.WRITER_IDLE, 0L);

        this.listener = newListener;

        // activate notifications
        newListener.idleTimeInMillisChanged(IdleStatus.BOTH_IDLE, idleTimeMillisForBoth);
        newListener.idleTimeInMillisChanged(IdleStatus.READER_IDLE, idleTimeMillisForRead);
        newListener.idleTimeInMillisChanged(IdleStatus.WRITER_IDLE, idleTimeMillisForWrite);
    }

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
            long idleTimeMillisForBoth = this.idleTimeMillisForBoth;
            if (idleTimeMillis != idleTimeMillisForBoth) {
                this.idleTimeMillisForBoth = idleTimeMillis;
                listener.idleTimeInMillisChanged(status, idleTimeMillis);
            }
        } else if (status == IdleStatus.READER_IDLE) {
            long idleTimeMillisForRead = this.idleTimeMillisForRead;
            if (idleTimeMillis != idleTimeMillisForRead) {
                this.idleTimeMillisForRead = idleTimeMillis;
                listener.idleTimeInMillisChanged(status, idleTimeMillis);
            }
        } else if (status == IdleStatus.WRITER_IDLE) {
            long idleTimeMillisForWrite = this.idleTimeMillisForWrite;
            if (idleTimeMillis != idleTimeMillisForWrite) {
                this.idleTimeMillisForWrite = idleTimeMillis;
                listener.idleTimeInMillisChanged(status, idleTimeMillis);
            }
        } else {
            throw new IllegalArgumentException("Unknown idle status: " + status);
        }

    }

}
