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
package org.kaazing.mina.netty.socket.nio;

import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.ReceiveBufferSizePredictor;
import org.jboss.netty.channel.ReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.socket.DefaultDatagramChannelConfig;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelConfig;

import java.net.DatagramSocket;
import java.net.SocketException;

public class DefaultNioDatagramChannelIoSessionConfig extends NioDatagramChannelIoSessionConfig {

    private static final byte REUSEADDRESS_MASK = 1;
    private static final byte RECEIVEBUFFERSIZE_MASK = 2;
    private static final byte SENDBUFFERSIZE_MASK = 4;
    private static final byte TRAFFICCLASS_MASK = 8;

    // bit mask to tell if a field has been set or not
    private byte fieldSetMask;

    private boolean reuseAddress;
    private int receiveBufferSize;
    private int sendBufferSize;
    private int trafficClass;

    public DefaultNioDatagramChannelIoSessionConfig() {
        super(new DefaultNioDatagramChannelConfig(newDatagramSocket()));
        // We could make this conditional (if !ENABLE_BUFFER_SIZE) to allow users to turn on variable buffer size via
        // the System property (as we used to in pure Mina days), but we won't because it would break zero copy and
        // is known to break TrafficShapingFilter so we don't want anyone ever to use it.
        channelConfig.setReceiveBufferSizePredictorFactory(
            new FixedReceiveBufferSizePredictorFactory(getReadBufferSize()));
    }

    private static DatagramSocket newDatagramSocket() {
        DatagramSocket result;
        try {
            result = new DatagramSocket();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return result;
    }


    // for unit test
    NioDatagramChannelConfig getChannelConfig() {
        return channelConfig;
    }



    @Override
    public boolean isReuseAddress() {
        checkIsSet(REUSEADDRESS_MASK);
        return reuseAddress;
    }

    @Override
    public void setReuseAddress(boolean reuseAddress) {
        setMask(REUSEADDRESS_MASK);
        this.reuseAddress = reuseAddress;
    }

    @Override
    public int getReceiveBufferSize() {
        checkIsSet(RECEIVEBUFFERSIZE_MASK);
        return receiveBufferSize;
    }

    @Override
    public void setReceiveBufferSize(int receiveBufferSize) {
        setMask(RECEIVEBUFFERSIZE_MASK);
        this.receiveBufferSize = receiveBufferSize;
    }

    @Override
    public int getSendBufferSize() {
        checkIsSet(SENDBUFFERSIZE_MASK);
        return sendBufferSize;
    }

    @Override
    public void setSendBufferSize(int sendBufferSize) {
        setMask(SENDBUFFERSIZE_MASK);
        this.sendBufferSize = sendBufferSize;
    }

    @Override
    public int getTrafficClass() {
        checkIsSet(TRAFFICCLASS_MASK);
        return trafficClass;
    }

    @Override
    public void setTrafficClass(int trafficClass) {
        setMask(TRAFFICCLASS_MASK);
        this.trafficClass = trafficClass;
    }

    private boolean isFieldSet(byte mask) {
        return (fieldSetMask & mask) != 0;
    }

    private void checkIsSet(byte mask) {
        if (!isFieldSet(mask)) {
            throw new IllegalStateException("field not modified");
        }
    }

    private void setMask(byte mask) {
        fieldSetMask |= mask;
    }

    /**
     * Unfortunately we cannot use Netty's DefaultNioDatagramChannelConfig because it is not public :(
     * All we want to do is convey the default ReceiveBufferSizePredictorFactory
     */
    private static class DefaultNioDatagramChannelConfig extends DefaultDatagramChannelConfig
                                                       implements NioDatagramChannelConfig {
        private ReceiveBufferSizePredictorFactory predictorFactory;

        DefaultNioDatagramChannelConfig(DatagramSocket socket) {
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
