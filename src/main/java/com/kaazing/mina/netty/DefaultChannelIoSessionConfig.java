/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.DefaultChannelConfig;

import com.kaazing.mina.core.session.AbstractIoSessionConfigEx;
import com.kaazing.mina.core.session.IoSessionConfigEx;

public class DefaultChannelIoSessionConfig extends ChannelIoSessionConfig<ChannelConfig> {
    private static final IoSessionConfigEx DEFAULT = new AbstractIoSessionConfigEx() {
        @Override
        protected void doSetAll(IoSessionConfigEx config) {
        }
    };

    public DefaultChannelIoSessionConfig() {
        this(new DefaultChannelConfig());
        // Push Mina default config settings into the channelConfig
        doSetAll(DEFAULT);
    }

    public DefaultChannelIoSessionConfig(ChannelConfig channelConfig) {
        super(channelConfig);
    }
}
