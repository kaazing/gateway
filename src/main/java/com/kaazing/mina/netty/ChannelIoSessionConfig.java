/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import org.apache.mina.core.session.AbstractIoSessionConfig;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketSession.MillisecondIdletimeSessionConfig;

import org.jboss.netty.channel.ChannelConfig;

public class ChannelIoSessionConfig<T extends ChannelConfig> extends AbstractIoSessionConfig implements MillisecondIdletimeSessionConfig {

	protected final T channelConfig;
	
	public ChannelIoSessionConfig(T channelConfig) {
		this.channelConfig = channelConfig;
	}
	
	public void setOption(String name, Object value) {
		channelConfig.setOption(name, value);
	}

	@Override
	protected void doSetAll(IoSessionConfig config) {

		int minReadBufferSize = config.getMinReadBufferSize();
		int readBufferSize = config.getReadBufferSize();
		int maxReadBufferSize = config.getMaxReadBufferSize();
		
		int bothIdleTime = config.getBothIdleTime();
		int readerIdleTime = config.getReaderIdleTime();
		int writerIdleTime = config.getWriterIdleTime();
		int throughputCalculationInterval = config.getThroughputCalculationInterval();
		int writeTimeout = config.getWriteTimeout();
		boolean useReadOperation = config.isUseReadOperation();
		
		channelConfig.setOption("minReadBufferSize", minReadBufferSize);
		channelConfig.setOption("readBufferSize", readBufferSize);
		channelConfig.setOption("maxReadBufferSize", maxReadBufferSize);
		channelConfig.setOption("bothIdleTime", bothIdleTime);
		channelConfig.setOption("readerIdleTime", readerIdleTime);
		channelConfig.setOption("writerIdleTime", writerIdleTime);
		channelConfig.setOption("throughputCalculationInterval", throughputCalculationInterval);
		channelConfig.setOption("writeTimeout", writeTimeout);
		channelConfig.setOption("useReadOperation", useReadOperation);
	}
	
   /*
    ** Implementation of interface MillisecondIdletimeSessionConfig
    */
    
    private volatile long idleTimeMillisForRead;
    private volatile long idleTimeMillisForWrite;
    private volatile long idleTimeMillisForBoth;
    
    /**
     * {@inheritDoc}
     */
    public int getIdleTime(IdleStatus status) {
        return (int) getIdleTimeInMillis(status) / 1000;
    }
    
    /**
     * {@inheritDoc}
     */
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
    
    /**
     * {@inheritDoc}
     */
    public void setIdleTime(IdleStatus status, int idleTime) {
        if (idleTime < 0) {
            throw new IllegalArgumentException("Illegal idle time: " + idleTime);
        }
        long idleTimeMillis = idleTime * 1000L;
        setIdleTimeInMillis(status, idleTimeMillis);
    }
    
    /**
     * {@inheritDoc}
     */
    public void setIdleTimeInMillis(IdleStatus status, long idleTimeMillis) {
        if (idleTimeMillis < 0) {
            throw new IllegalArgumentException("Illegal idle time: " + idleTimeMillis);
        }
        
        // TODO: set as an option on channelConfig?

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
