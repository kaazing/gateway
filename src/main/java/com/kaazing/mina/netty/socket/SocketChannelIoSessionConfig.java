/**
 * Copyright (c) 2007-2014, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket;

import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.SocketSessionConfigEx;
import org.jboss.netty.channel.socket.SocketChannelConfig;

import com.kaazing.mina.core.session.IoSessionConfigEx;
import com.kaazing.mina.netty.ChannelIoSessionConfig;

public abstract class SocketChannelIoSessionConfig<T extends SocketChannelConfig>
                   extends ChannelIoSessionConfig<T> implements SocketSessionConfigEx {

    public SocketChannelIoSessionConfig(T channelConfig) {
        super(channelConfig);
    }

    @Override
    protected void doSetAll(IoSessionConfigEx config) {

        super.doSetAll(config);

        if (config instanceof SocketChannelIoSessionConfig) {
            SocketChannelIoSessionConfig<?> socketConfig = (SocketChannelIoSessionConfig<?>) config;

            if (socketConfig.isReceiveBufferSizeChanged()) {
                setReceiveBufferSize(socketConfig.getReceiveBufferSize());
            }
            if (socketConfig.isSendBufferSizeChanged()) {
                setSendBufferSize(socketConfig.getSendBufferSize());
            }
            if (socketConfig.isSoLingerChanged()) {
                setSoLinger(socketConfig.getSoLinger());
            }
            if (socketConfig.isTrafficClassChanged()) {
                setTrafficClass(socketConfig.getTrafficClass());
            }
            if (socketConfig.isKeepAliveChanged()) {
                setKeepAlive(socketConfig.isKeepAlive());
            }
            if (socketConfig.isOobInlineChanged()) {
                setOobInline(socketConfig.isOobInline());
            }
            if (socketConfig.isReuseAddressChanged()) {
                setReuseAddress(socketConfig.isReuseAddress());
            }
            if (socketConfig.isTcpNoDelayChanged()) {
                setTcpNoDelay(socketConfig.isTcpNoDelay());
            }
        } else if (config instanceof SocketSessionConfig) {
            SocketSessionConfig cfg = (SocketSessionConfig) config;
            setKeepAlive(cfg.isKeepAlive());
            setOobInline(cfg.isOobInline());
            setReceiveBufferSize(cfg.getReceiveBufferSize());
            setReuseAddress(cfg.isReuseAddress());
            setSendBufferSize(cfg.getSendBufferSize());
            setSoLinger(cfg.getSoLinger());
            setTcpNoDelay(cfg.isTcpNoDelay());
            cfg.getTrafficClass();
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
    public int getSoLinger() {
        return channelConfig.getSoLinger();
    }

    @Override
    public int getTrafficClass() {
        return channelConfig.getTrafficClass();
    }

    @Override
    public boolean isKeepAlive() {
        return channelConfig.isKeepAlive();
    }

    @Override
    public boolean isOobInline() {
        return false; // TODO
    }

    @Override
    public boolean isReuseAddress() {
        return channelConfig.isReuseAddress();
    }

    @Override
    public boolean isTcpNoDelay() {
        return channelConfig.isTcpNoDelay();
    }

    @Override
    public void setKeepAlive(boolean keepAlive) {
        channelConfig.setKeepAlive(keepAlive);
    }

    @Override
    public void setOobInline(boolean oobInline) {
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
    public void setSoLinger(int soLinger) {
        channelConfig.setSoLinger(soLinger);
    }

    @Override
    public void setTcpNoDelay(boolean tcpNoDelay) {
        channelConfig.setTcpNoDelay(tcpNoDelay);
    }

    @Override
    public void setTrafficClass(int trafficClass) {
        channelConfig.setTrafficClass(trafficClass);
    }

    /**
     * Returns <tt>true</tt> if and only if the <tt>keepAlive</tt> property has been changed by its setter method. The
     * system call related with the property is made only when this method returns <tt>true</tt>. By default, this
     * method always returns <tt>true</tt> to simplify implementation of subclasses, but overriding the default behavior
     * is always encouraged.
     */
    protected boolean isKeepAliveChanged() {
        return true;
    }

    /**
     * Returns <tt>true</tt> if and only if the <tt>oobInline</tt> property has been changed by its setter method. The
     * system call related with the property is made only when this method returns <tt>true</tt>. By default, this
     * method always returns <tt>true</tt> to simplify implementation of subclasses, but overriding the default behavior
     * is always encouraged.
     */
    protected boolean isOobInlineChanged() {
        return true;
    }

    /**
     * Returns <tt>true</tt> if and only if the <tt>receiveBufferSize</tt> property has been changed by its setter
     * method. The system call related with the property is made only when this method returns <tt>true</tt>. By
     * default, this method always returns <tt>true</tt> to simplify implementation of subclasses, but overriding the
     * default behavior is always encouraged.
     */
    protected boolean isReceiveBufferSizeChanged() {
        return true;
    }

    /**
     * Returns <tt>true</tt> if and only if the <tt>reuseAddress</tt> property has been changed by its setter method.
     * The system call related with the property is made only when this method returns <tt>true</tt>. By default, this
     * method always returns <tt>true</tt> to simplify implementation of subclasses, but overriding the default behavior
     * is always encouraged.
     */
    protected boolean isReuseAddressChanged() {
        return true;
    }

    /**
     * Returns <tt>true</tt> if and only if the <tt>sendBufferSize</tt> property has been changed by its setter method.
     * The system call related with the property is made only when this method returns <tt>true</tt>. By default, this
     * method always returns <tt>true</tt> to simplify implementation of subclasses, but overriding the default behavior
     * is always encouraged.
     */
    protected boolean isSendBufferSizeChanged() {
        return true;
    }

    /**
     * Returns <tt>true</tt> if and only if the <tt>soLinger</tt> property has been changed by its setter method. The
     * system call related with the property is made only when this method returns <tt>true</tt>. By default, this
     * method always returns <tt>true</tt> to simplify implementation of subclasses, but overriding the default behavior
     * is always encouraged.
     */
    protected boolean isSoLingerChanged() {
        return true;
    }

    /**
     * Returns <tt>true</tt> if and only if the <tt>tcpNoDelay</tt> property has been changed by its setter method. The
     * system call related with the property is made only when this method returns <tt>true</tt>. By default, this
     * method always returns <tt>true</tt> to simplify implementation of subclasses, but overriding the default behavior
     * is always encouraged.
     */
    protected boolean isTcpNoDelayChanged() {
        return true;
    }

    /**
     * Returns <tt>true</tt> if and only if the <tt>trafficClass</tt> property has been changed by its setter method.
     * The system call related with the property is made only when this method returns <tt>true</tt>. By default, this
     * method always returns <tt>true</tt> to simplify implementation of subclasses, but overriding the default behavior
     * is always encouraged.
     */
    protected boolean isTrafficClassChanged() {
        return true;
    }
}
