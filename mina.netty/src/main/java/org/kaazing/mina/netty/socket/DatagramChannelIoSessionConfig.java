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
package org.kaazing.mina.netty.socket;

import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.DatagramSessionConfigEx;
import org.jboss.netty.channel.socket.DatagramChannelConfig;

import org.kaazing.mina.core.session.IoSessionConfigEx;
import org.kaazing.mina.netty.ChannelIoSessionConfig;

public abstract class DatagramChannelIoSessionConfig extends ChannelIoSessionConfig<DatagramChannelConfig>
    implements DatagramSessionConfigEx {

    public DatagramChannelIoSessionConfig(DatagramChannelConfig channelConfig, DatagramSessionConfigEx defaults) {
        super(channelConfig, defaults);
    }

    @Override
    protected final void doSetAll(IoSessionConfigEx config) {

        super.doSetAll(config);

        if (config instanceof DatagramSessionConfig) {
            DatagramSessionConfig datagramConfig = (DatagramSessionConfig) config;

            setReceiveBufferSize(datagramConfig.getReceiveBufferSize());
            setSendBufferSize(datagramConfig.getSendBufferSize());
            setTrafficClass(datagramConfig.getTrafficClass());
            setBroadcast(datagramConfig.isBroadcast());
            setCloseOnPortUnreachable(datagramConfig.isCloseOnPortUnreachable());
            setReuseAddress(datagramConfig.isReuseAddress());
        }
    }

    @Override
    public int getReceiveBufferSize() {
        return channelConfig.getReceiveBufferSize();
    }

    @Override
    public int getSendBufferSize() {
        return channelConfig.getSendBufferSize();
    }

    @Override
    public int getTrafficClass() {
        return channelConfig.getTrafficClass();
    }

    @Override
    public boolean isBroadcast() {
        return channelConfig.isBroadcast();
    }

    @Override
    public boolean isCloseOnPortUnreachable() {
        return true; // TODO
    }

    @Override
    public boolean isReuseAddress() {
        return channelConfig.isReuseAddress();
    }

    @Override
    public void setBroadcast(boolean broadcast) {
        channelConfig.setBroadcast(broadcast);
    }

    @Override
    public void setCloseOnPortUnreachable(boolean closeOnPortUnreachable) {
        // TODO
    }

    @Override
    public void setReceiveBufferSize(int receiveBufferSize) {
        channelConfig.setReceiveBufferSize(receiveBufferSize);
    }

    @Override
    public void setReuseAddress(boolean reuseAddress) {
        channelConfig.setReuseAddress(reuseAddress);
    }

    @Override
    public void setSendBufferSize(int sendBufferSize) {
        channelConfig.setSendBufferSize(sendBufferSize);
    }

    @Override
    public void setTrafficClass(int trafficClass) {
        channelConfig.setTrafficClass(trafficClass);
    }

}
