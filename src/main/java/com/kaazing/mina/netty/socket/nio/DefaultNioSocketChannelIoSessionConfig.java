/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket.nio;

import java.net.Socket;

import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.ReceiveBufferSizePredictor;
import org.jboss.netty.channel.ReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.socket.DefaultSocketChannelConfig;
import org.jboss.netty.channel.socket.nio.NioSocketChannelConfig;

public class DefaultNioSocketChannelIoSessionConfig extends NioSocketChannelIoSessionConfig {

    public DefaultNioSocketChannelIoSessionConfig() {
        super(new DefaultNioSocketChannelConfig(new Socket()));
        // We could make this conditional (if !ENABLE_BUFFER_SIZE) to allow users to turn on variable buffer size via
        // the System property (as we used to in pure Mina days), but we won't because it would break zero copy and
        // is known to break TrafficShapingFilter so we don't want anyone ever to use it.
        channelConfig.setReceiveBufferSizePredictorFactory(
            new FixedReceiveBufferSizePredictorFactory(getReadBufferSize()));
    }

    // for unit test
    NioSocketChannelConfig getChannelConfig() {
        return channelConfig;
    }

    /**
     * Unfortunately we cannot use Netty's DefaultNioSocketChannelConfig because it is not public :(
     * All we want to do is convey the default ReceiveBufferSizePredictorFactory
     */
    private static class DefaultNioSocketChannelConfig extends DefaultSocketChannelConfig
                                                       implements NioSocketChannelConfig {
        private ReceiveBufferSizePredictorFactory predictorFactory;

        public DefaultNioSocketChannelConfig(Socket socket) {
            super(socket);
        }

        @Override
        public int getWriteBufferHighWaterMark() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setWriteBufferHighWaterMark(int writeBufferHighWaterMark) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getWriteBufferLowWaterMark() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setWriteBufferLowWaterMark(int writeBufferLowWaterMark) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getWriteSpinCount() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setWriteSpinCount(int writeSpinCount) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReceiveBufferSizePredictor getReceiveBufferSizePredictor() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setReceiveBufferSizePredictor(ReceiveBufferSizePredictor predictor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReceiveBufferSizePredictorFactory getReceiveBufferSizePredictorFactory() {
            return predictorFactory;
        }

        @Override
        public void setReceiveBufferSizePredictorFactory(ReceiveBufferSizePredictorFactory predictorFactory) {
            this.predictorFactory = predictorFactory;
        }

    }

}
