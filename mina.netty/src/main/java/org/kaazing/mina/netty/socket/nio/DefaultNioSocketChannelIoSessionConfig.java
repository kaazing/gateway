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

import java.net.Socket;

import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.ReceiveBufferSizePredictor;
import org.jboss.netty.channel.ReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.socket.DefaultSocketChannelConfig;
import org.jboss.netty.channel.socket.nio.NioSocketChannelConfig;

public class DefaultNioSocketChannelIoSessionConfig extends NioSocketChannelIoSessionConfig {

    private static final byte REUSEADDRESS_MASK = 1;
    private static final byte RECEIVEBUFFERSIZE_MASK = 2;
    private static final byte SENDBUFFERSIZE_MASK = 4;
    private static final byte TRAFFICCLASS_MASK = 8;
    private static final byte KEEPALIVE_MASK = 16;
    private static final byte OOBINLINE_MASK = 32;
    private static final byte SOLINGER_MASK = 64;
    private static final byte TCPNODELAY_MASK = (byte) 128;

    // bit mask to tell if a field has been set or not
    private byte fieldSetMask;

    private boolean reuseAddress;
    private int receiveBufferSize;
    private int sendBufferSize;
    private int trafficClass;
    private boolean keepAlive;
    private boolean oobInline;
    private int soLinger;
    private boolean tcpNoDelay;


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

    @Override
    public boolean isKeepAlive() {
        checkIsSet(KEEPALIVE_MASK);
        return keepAlive;
    }

    @Override
    public void setKeepAlive(boolean keepAlive) {
        setMask(KEEPALIVE_MASK);
        this.keepAlive = keepAlive;
    }

    @Override
    public boolean isOobInline() {
        checkIsSet(OOBINLINE_MASK);
        return oobInline;
    }

    @Override
    public void setOobInline(boolean oobInline) {
        setMask(OOBINLINE_MASK);
        this.oobInline = oobInline;
    }

    @Override
    public int getSoLinger() {
        checkIsSet(SOLINGER_MASK);
        return soLinger;
    }

    @Override
    public void setSoLinger(int soLinger) {
        setMask(SOLINGER_MASK);
        this.soLinger = soLinger;
    }

    @Override
    public boolean isTcpNoDelay() {
        checkIsSet(TCPNODELAY_MASK);
        return tcpNoDelay;
    }

    @Override
    public void setTcpNoDelay(boolean tcpNoDelay) {
        setMask(TCPNODELAY_MASK);
        this.tcpNoDelay = tcpNoDelay;
    }

    @Override
    protected boolean isKeepAliveChanged() {
        return isFieldSet(KEEPALIVE_MASK);
    }

    @Override
    protected boolean isOobInlineChanged() {
        return isFieldSet(OOBINLINE_MASK);
    }

    @Override
    protected boolean isReceiveBufferSizeChanged() {
        return isFieldSet(RECEIVEBUFFERSIZE_MASK);
    }

    @Override
    protected boolean isReuseAddressChanged() {
        return isFieldSet(REUSEADDRESS_MASK);
    }

    @Override
    protected boolean isSendBufferSizeChanged() {
        return isFieldSet(SENDBUFFERSIZE_MASK);
    }

    @Override
    protected boolean isSoLingerChanged() {
        return isFieldSet(SOLINGER_MASK);
    }

    @Override
    protected boolean isTcpNoDelayChanged() {
        return isFieldSet(TCPNODELAY_MASK);
    }

    @Override
    protected boolean isTrafficClassChanged() {
        return isFieldSet(TRAFFICCLASS_MASK);
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
