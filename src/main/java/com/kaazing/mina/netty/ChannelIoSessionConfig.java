/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import org.jboss.netty.channel.ChannelConfig;

import com.kaazing.mina.core.session.AbstractIoSessionConfigEx;
import com.kaazing.mina.core.session.IoSessionConfigEx;

public class ChannelIoSessionConfig<T extends ChannelConfig> extends AbstractIoSessionConfigEx {

    protected final T channelConfig;

    public ChannelIoSessionConfig(T channelConfig, IoSessionConfigEx defaults) {
        this.channelConfig = channelConfig;
        doSetAll(defaults);
    }

    public ChannelIoSessionConfig(T channelConfig) {
        this.channelConfig = channelConfig;
    }

    protected void init(IoSessionConfigEx defaults) {
        doSetAll(defaults);
    }

    public void setOption(String name, Object value) {
        channelConfig.setOption(name, value);
    }

    @Override
    protected void doSetAll(IoSessionConfigEx config) {

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

}
