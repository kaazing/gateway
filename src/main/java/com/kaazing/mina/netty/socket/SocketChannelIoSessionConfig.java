package com.kaazing.mina.netty.socket;

import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.jboss.netty.channel.socket.SocketChannelConfig;

import com.kaazing.mina.netty.ChannelIoSessionConfig;

public class SocketChannelIoSessionConfig extends ChannelIoSessionConfig<SocketChannelConfig> implements SocketSessionConfig {

	public SocketChannelIoSessionConfig(SocketChannelConfig channelConfig) {
		super(channelConfig);
	}
	
	@Override
	protected final void doSetAll(IoSessionConfig config) {

		super.doSetAll(config);
		
		if (config instanceof SocketSessionConfig) {
			SocketSessionConfig socketConfig = (SocketSessionConfig)config;
			
			setReceiveBufferSize(socketConfig.getReceiveBufferSize());
			setSendBufferSize(socketConfig.getSendBufferSize());
			setSoLinger(socketConfig.getSoLinger());
			setTrafficClass(socketConfig.getTrafficClass());
			setKeepAlive(socketConfig.isKeepAlive());
			setOobInline(socketConfig.isOobInline());
			setReuseAddress(socketConfig.isReuseAddress());
			setTcpNoDelay(socketConfig.isTcpNoDelay());
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

}
