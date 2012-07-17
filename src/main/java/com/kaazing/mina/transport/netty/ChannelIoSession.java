package com.kaazing.mina.transport.netty;

import java.net.SocketAddress;

import org.apache.mina.core.filterchain.DefaultIoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.AbstractIoSession;
import org.jboss.netty.channel.Channel;

public class ChannelIoSession extends AbstractIoSession {

	private final ChannelIoProcessor processor;
	private final Channel channel;
	private final IoFilterChain filterChain;
	private final TransportMetadata transportMetadata;
	
	public ChannelIoSession(ChannelIoService service, Channel channel) {
		super(service);
		
		this.config = new ChannelIoSessionConfig(channel.getConfig());
        this.config.setAll(service.getSessionConfig());
		this.channel = channel;
		this.processor = new ChannelIoProcessor();
		this.filterChain = new DefaultIoFilterChain(this);
		this.transportMetadata = service.getTransportMetadata();
	}

	public Channel getChannel() {
		return channel;
	}
	
    public ChannelIoService getService() {
        return (ChannelIoService)super.getService();
    }
    
	@Override
	public IoProcessor<ChannelIoSession> getProcessor() {
		return processor;
	}

	@Override
	public IoFilterChain getFilterChain() {
		return filterChain;
	}

	@Override
	public SocketAddress getLocalAddress() {
		return channel.getLocalAddress();
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return channel.getRemoteAddress();
	}

	@Override
	public TransportMetadata getTransportMetadata() {
		return transportMetadata;
	}

}
