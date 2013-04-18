/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket;

import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.DefaultDatagramSessionConfig;
import org.jboss.netty.channel.socket.DatagramChannelConfig;

import com.kaazing.mina.core.session.IoSessionConfigEx;
import com.kaazing.mina.netty.ChannelIoSessionConfig;

public class DatagramChannelIoSessionConfig extends ChannelIoSessionConfig<DatagramChannelConfig>
    implements DatagramSessionConfig {

    private static final IoSessionConfigEx DEFAULT = new DefaultDatagramSessionConfigEx();

    public DatagramChannelIoSessionConfig(DatagramChannelConfig channelConfig) {
        super(channelConfig);
        // Push Mina default config settings into the channelConfig
        doSetAll(DEFAULT);
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

    private static class DefaultDatagramSessionConfigEx extends DefaultDatagramSessionConfig
        implements IoSessionConfigEx {

        @Override
        public void setIdleTimeInMillis(IdleStatus status, long idleTimeMillis) {
            throw new UnsupportedOperationException();
        }

    }

}
