/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket.nio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.mina.transport.socket.DefaultSocketSessionConfig;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.socket.nio.NioSocketChannelConfig;
import org.junit.Test;

public class DefaultNioSocketChannelIoSessionConfigTest {
    private static final SocketSessionConfig DEFAULT = new DefaultSocketSessionConfig();

    @Test
    public void shouldUseFixedSizeReadBufferWithSameDefaultSizeAsMina() throws Exception {
        DefaultNioSocketChannelIoSessionConfig config = new DefaultNioSocketChannelIoSessionConfig();
        NioSocketChannelConfig channelConfig = config.getChannelConfig();
        assertTrue("Should use fixed read buffer size",
                channelConfig.getReceiveBufferSizePredictorFactory() instanceof FixedReceiveBufferSizePredictorFactory);
        assertEquals("Read buffer size", DEFAULT.getReadBufferSize(),
                channelConfig.getReceiveBufferSizePredictorFactory().getPredictor().nextReceiveBufferSize());
    }

    @Test
    public void shouldUseSameDefaultValuesAsMina() throws Exception {
        DefaultNioSocketChannelIoSessionConfig config = new DefaultNioSocketChannelIoSessionConfig();
        assertEquals(DEFAULT.getBothIdleTimeInMillis(), config.getBothIdleTimeInMillis());
        assertEquals(DEFAULT.getMaxReadBufferSize(), config.getMaxReadBufferSize());
        assertEquals(DEFAULT.getMinReadBufferSize(), config.getMinReadBufferSize());
        assertEquals(DEFAULT.getReadBufferSize(), config.getReadBufferSize());
        assertEquals(DEFAULT.getReaderIdleTimeInMillis(), config.getReaderIdleTimeInMillis());
        assertEquals(DEFAULT.getReceiveBufferSize(), config.getReceiveBufferSize());
        //assertEquals(DEFAULT.getSendBufferSize(), config.getSendBufferSize());
        // IE (Windows) stalls on smaller buffers limited to a single packet, so 1460 is bad, but 1461 is good (using 2K here)
        assertEquals(DEFAULT.getSendBufferSize() * 2, config.getSendBufferSize());
        assertEquals(DEFAULT.getSoLinger(), config.getSoLinger());
        assertEquals(DEFAULT.getThroughputCalculationIntervalInMillis(), config.getThroughputCalculationIntervalInMillis());
        assertEquals(DEFAULT.getTrafficClass(), config.getTrafficClass());
        assertEquals(DEFAULT.getWriterIdleTimeInMillis(), config.getWriterIdleTimeInMillis());
        assertEquals(DEFAULT.getWriteTimeoutInMillis(), config.getWriteTimeoutInMillis());
        assertEquals(DEFAULT.isKeepAlive(), config.isKeepAlive());
        assertEquals(DEFAULT.isOobInline(), config.isOobInline());
        assertEquals(DEFAULT.isReuseAddress(), config.isReuseAddress());
        assertEquals(DEFAULT.isTcpNoDelay(), config.isTcpNoDelay());
        assertEquals(DEFAULT.isUseReadOperation(), config.isUseReadOperation());
    }

}
