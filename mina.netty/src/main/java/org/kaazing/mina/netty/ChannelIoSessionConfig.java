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
package org.kaazing.mina.netty;

import org.jboss.netty.channel.ChannelConfig;

import org.kaazing.mina.core.session.AbstractIoSessionConfigEx;
import org.kaazing.mina.core.session.IoSessionConfigEx;

public class ChannelIoSessionConfig<T extends ChannelConfig> extends AbstractIoSessionConfigEx {

    // Push Mina default config settings into the channelConfig
    private static final IoSessionConfigEx DEFAULT = new AbstractIoSessionConfigEx() {
        @Override
        protected void doSetAll(final IoSessionConfigEx config) {
        }
    };

    protected final T channelConfig;

    public ChannelIoSessionConfig(T channelConfig, IoSessionConfigEx defaults) {
        this.channelConfig = channelConfig;
        doSetAll(defaults);
    }

    public ChannelIoSessionConfig(T channelConfig) {
        this.channelConfig = channelConfig;
        doSetAll(DEFAULT);
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
