package com.kaazing.mina.netty;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.AbstractIoAcceptor;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipelineFactory;

public abstract class ChannelIoAcceptor<C extends IoSessionConfig, F extends ChannelFactory, A extends SocketAddress> extends AbstractIoAcceptor implements ChannelIoService {

	private final ServerBootstrap bootstrap;
	private final Map<SocketAddress, Channel> boundChannels;
	private final IoAcceptorChannelHandler parentHandler;
	
	public ChannelIoAcceptor(C sessionConfig, F channelFactory, IoAcceptorChannelHandlerFactory factory) {
		super(sessionConfig, new Executor() {
			@Override
			public void execute(Runnable command) {
			}
		});
		
		parentHandler = factory.createHandler(this);
		
		bootstrap = new ServerBootstrap(channelFactory);
		bootstrap.setParentHandler(parentHandler);
		
		boundChannels = Collections.synchronizedMap(new HashMap<SocketAddress, Channel>());
	}

	public void setPipelineFactory(ChannelPipelineFactory pipelineFactory) {
		parentHandler.setPipelineFactory(pipelineFactory);
	}
	
	@Override
	public void initializeSession(ChannelIoSession session, IoFuture future, IoSessionInitializer<?> sessionInitializer) {
		initSession(session, future, sessionInitializer);
	}
	
	@SuppressWarnings("unchecked")
	protected F getChannelFactory() {
		return (F)bootstrap.getFactory();
	}
	
	@Override
	protected Set<SocketAddress> bindInternal(
			List<? extends SocketAddress> localAddresses) throws Exception {

		for (SocketAddress localAddress : localAddresses) {
			bootstrap.bind(localAddress);
		}
		
		Set<SocketAddress> newLocalAddresses = new HashSet<SocketAddress>();
		for (Channel channel : boundChannels.values()) {
			newLocalAddresses.add(channel.getLocalAddress());
		}
		
		return newLocalAddresses;
	}

	@Override
	protected void unbind0(List<? extends SocketAddress> localAddresses)
			throws Exception {

		for (SocketAddress localAddress : localAddresses) {
			Channel channel = boundChannels.remove(localAddress);
			
			if (channel == null) {
				continue;
			}
			
			channel.unbind();
		}
		
	}

	@Override
	public ChannelIoSession newSession(SocketAddress remoteAddress,
			SocketAddress localAddress) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected IoFuture dispose0() throws Exception {
		bootstrap.releaseExternalResources();
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public C getSessionConfig() {
		return (C)super.getSessionConfig();
	}

	@Override
	@SuppressWarnings("unchecked")
	public A getDefaultLocalAddress() {
		return (A)super.getDefaultLocalAddress();
	}

	@Override
	@SuppressWarnings("unchecked")
	public A getLocalAddress() {
		return (A)super.getLocalAddress();
	}
}
