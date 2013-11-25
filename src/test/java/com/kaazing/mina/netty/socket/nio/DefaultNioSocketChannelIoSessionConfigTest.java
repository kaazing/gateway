/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket.nio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.Socket;

import org.apache.mina.transport.socket.DefaultSocketSessionConfigEx;
import org.apache.mina.transport.socket.nio.NioSocketAcceptorEx;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.ReceiveBufferSizePredictor;
import org.jboss.netty.channel.ReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.socket.DefaultSocketChannelConfig;
import org.jboss.netty.channel.socket.nio.NioSocketChannelConfig;
import org.junit.BeforeClass;
import org.junit.Test;

public class DefaultNioSocketChannelIoSessionConfigTest {
    private static final DefaultSocketSessionConfigEx DEFAULT = new DefaultSocketSessionConfigEx();

    @BeforeClass
    public static void initDefaultSocketSessionConfig() {
        DEFAULT.init(new NioSocketAcceptorEx());
    }

    @Test
    public void testNioSocketSetAll() throws Exception {
        DefaultNioSocketChannelIoSessionConfig config = new DefaultNioSocketChannelIoSessionConfig();
        config.init(new NioSocketChannelIoAcceptor(config));
        config.setReceiveBufferSize(2048);
        config.setSendBufferSize(2048);
        config.setSoLinger(120);
        config.setTrafficClass(1);
        config.setKeepAlive(true);
        config.setReuseAddress(true);
        config.setTcpNoDelay(true);

        NioSocketChannelIoSessionConfig acceptedConfig =
                new NioSocketChannelIoSessionConfig(new DefaultNioSocketChannelConfig(new Socket()));

        acceptedConfig.setAll(config);

        assertEquals(config.getBothIdleTimeInMillis(), acceptedConfig.getBothIdleTimeInMillis());
        assertEquals(config.getMaxReadBufferSize(), acceptedConfig.getMaxReadBufferSize());
        assertEquals(config.getMinReadBufferSize(), acceptedConfig.getMinReadBufferSize());
        assertEquals(config.getReadBufferSize(), acceptedConfig.getReadBufferSize());
        assertEquals(config.getReaderIdleTimeInMillis(), acceptedConfig.getReaderIdleTimeInMillis());
        assertEquals(config.getReceiveBufferSize(), acceptedConfig.getReceiveBufferSize());
        assertEquals(config.getSendBufferSize(), acceptedConfig.getSendBufferSize());
        assertEquals(config.getSoLinger(), acceptedConfig.getSoLinger());
        assertEquals(config.getThroughputCalculationIntervalInMillis(), acceptedConfig
                .getThroughputCalculationIntervalInMillis());
        assertEquals(config.getTrafficClass(), acceptedConfig.getTrafficClass());
        assertEquals(config.getWriterIdleTimeInMillis(), acceptedConfig.getWriterIdleTimeInMillis());
        assertEquals(config.getWriteTimeoutInMillis(), acceptedConfig.getWriteTimeoutInMillis());
        assertEquals(config.isKeepAlive(), acceptedConfig.isKeepAlive());
        assertEquals(config.isReuseAddress(), acceptedConfig.isReuseAddress());
        assertEquals(config.isTcpNoDelay(), acceptedConfig.isTcpNoDelay());
        assertEquals(config.isUseReadOperation(), acceptedConfig.isUseReadOperation());

    }

    @Test
    public void shouldUseFixedSizeReadBufferWithSameDefaultSizeAsMina() throws Exception {
        DefaultNioSocketChannelIoSessionConfig config = new DefaultNioSocketChannelIoSessionConfig();
        config.init(new NioSocketChannelIoAcceptor(config));
        NioSocketChannelConfig channelConfig = config.getChannelConfig();
        assertTrue("Should use fixed read buffer size",
                channelConfig.getReceiveBufferSizePredictorFactory() instanceof FixedReceiveBufferSizePredictorFactory);
        assertEquals("Read buffer size", DEFAULT.getReadBufferSize(),
                channelConfig.getReceiveBufferSizePredictorFactory().getPredictor().nextReceiveBufferSize());
    }

    @Test
    public void shouldUseSameDefaultValuesAsMina() throws Exception {
        DefaultNioSocketChannelIoSessionConfig config = new DefaultNioSocketChannelIoSessionConfig();
        config.init(new NioSocketChannelIoAcceptor(config));

        assertEquals(DEFAULT.getBothIdleTimeInMillis(), config.getBothIdleTimeInMillis());
        assertEquals(DEFAULT.getMaxReadBufferSize(), config.getMaxReadBufferSize());
        assertEquals(DEFAULT.getMinReadBufferSize(), config.getMinReadBufferSize());
        assertEquals(DEFAULT.getReadBufferSize(), config.getReadBufferSize());
        assertEquals(DEFAULT.getReaderIdleTimeInMillis(), config.getReaderIdleTimeInMillis());

        // Since the DefaultSocketSessionConfigEx that we bootstrap config does not have any saved settings
        // they do not get flushed through to the config. And get ... without a set first on our
        // DefaultNioSocketChannelIoSessionConfig
        // now throws an exception.

        // assertEquals(DEFAULT.getReceiveBufferSize(), config.getReceiveBufferSize());
        // IE (Windows) stalls on smaller buffers limited to a single packet, so 1460 is bad, but 1461 is good (using 2K here)
        // assertEquals(DEFAULT.getSendBufferSize() * 2, config.getSendBufferSize());
        // assertEquals(DEFAULT.getSoLinger(), config.getSoLinger());
        assertEquals(DEFAULT.getThroughputCalculationIntervalInMillis(), config.getThroughputCalculationIntervalInMillis());
        // assertEquals(DEFAULT.getTrafficClass(), config.getTrafficClass());
        assertEquals(DEFAULT.getWriterIdleTimeInMillis(), config.getWriterIdleTimeInMillis());
        assertEquals(DEFAULT.getWriteTimeoutInMillis(), config.getWriteTimeoutInMillis());
        // assertEquals(DEFAULT.isKeepAlive(), config.isKeepAlive());
        // assertEquals(DEFAULT.isOobInline(), config.isOobInline());
        // assertEquals(DEFAULT.isReuseAddress(), config.isReuseAddress());
        // assertEquals(DEFAULT.isTcpNoDelay(), config.isTcpNoDelay());
        assertEquals(DEFAULT.isUseReadOperation(), config.isUseReadOperation());
    }

    private static class DefaultNioSocketChannelConfig extends DefaultSocketChannelConfig
            implements NioSocketChannelConfig {

        public DefaultNioSocketChannelConfig(Socket socket) {
            super(socket);
        }

        @Override
        public int getWriteBufferHighWaterMark() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public void setWriteBufferHighWaterMark(int writeBufferHighWaterMark) {
            // TODO Auto-generated method stub

        }

        @Override
        public int getWriteBufferLowWaterMark() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public void setWriteBufferLowWaterMark(int writeBufferLowWaterMark) {
            // TODO Auto-generated method stub

        }

        @Override
        public int getWriteSpinCount() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public void setWriteSpinCount(int writeSpinCount) {
            // TODO Auto-generated method stub

        }

        @Override
        public ReceiveBufferSizePredictor getReceiveBufferSizePredictor() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setReceiveBufferSizePredictor(ReceiveBufferSizePredictor predictor) {
            // TODO Auto-generated method stub

        }

        @Override
        public ReceiveBufferSizePredictorFactory getReceiveBufferSizePredictorFactory() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setReceiveBufferSizePredictorFactory(ReceiveBufferSizePredictorFactory predictorFactory) {
            // TODO Auto-generated method stub

        }
    }

}
