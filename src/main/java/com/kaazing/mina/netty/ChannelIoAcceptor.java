/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import static java.util.concurrent.TimeUnit.SECONDS;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ChannelBufType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;

import java.net.SocketAddress;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.AbstractIoAcceptor;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.core.session.IoSessionInitializer;

public abstract class ChannelIoAcceptor<E extends EventLoopGroup, S extends IoSessionConfig, P extends ServerChannel, C extends Channel, A extends SocketAddress> extends AbstractIoAcceptor implements ChannelIoService {

	private final E parentEventLoop;
	private final E childEventLoop;
	private final ChannelHandler childHandler;
	private final ConcurrentMap<SocketAddress, Channel> boundChannels;
	private final ChannelGroup channelGroup;

	private final Map<ChannelOption<?>, Object> parentOptions = new LinkedHashMap<ChannelOption<?>, Object>();
	
	public ChannelIoAcceptor(ChannelBufType bufType, S sessionConfig, E parentEventLoop, E childEventLoop) {
		super(sessionConfig, new Executor() {
			@Override
			public void execute(Runnable command) {
			}
		});
	
		this.channelGroup = new DefaultChannelGroup();	
		this.parentEventLoop = parentEventLoop;
		this.childEventLoop = childEventLoop;
		this.childHandler = new IoAcceptorChildChannelInitializer(this, bufType, channelGroup);
		this.boundChannels = new ConcurrentHashMap<SocketAddress, Channel>();
	}

	protected abstract P newServerChannel(E parentGroup, E childGroup);
	
	@Override
	public void initializeSession(ChannelIoSession session, IoFuture future, IoSessionInitializer<?> sessionInitializer) {
		initSession(session, future, sessionInitializer);
	}

	protected <T> void setBootstrapOption(ChannelOption<T> option, T newValue) {
		if (newValue == null) {
			parentOptions.remove(option);
		}
		else {
			parentOptions.put(option, newValue);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected <T> T getBootstrapOption(ChannelOption<T> option, T defaultValue) {
		T value = (T)parentOptions.get(option);
		if (value == null) {
			value = defaultValue;
		}
		return value;
	}
	
	@Override
	protected Set<SocketAddress> bindInternal(
			List<? extends SocketAddress> localAddresses) throws Exception {

		for (SocketAddress localAddress : localAddresses) {
			ServerBootstrap bootstrap = new ServerBootstrap()
				.group(parentEventLoop, childEventLoop)
				.childHandler(childHandler);
		
			bootstrap.channel(newServerChannel(parentEventLoop, childEventLoop));
			bootstrap.localAddress(localAddress);
			
			for (Map.Entry<ChannelOption<?>, Object> entry : parentOptions.entrySet()) {
				@SuppressWarnings("unchecked")
				ChannelOption<Object> option = (ChannelOption<Object>)entry.getKey();
				Object value = entry.getValue();
				bootstrap.option(option, value);
			}

			// must sync to maintain equivalence with MINA semantics
			Channel channel = bootstrap.bind().sync().channel();
			boundChannels.put(localAddress, channel);
		}
		
		Set<SocketAddress> newLocalAddresses = new HashSet<SocketAddress>();
		for (SocketAddress localAddress : localAddresses) {
			newLocalAddresses.add(localAddress);
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
			
			channel.close();
		}
		
	}

	@Override
	public ChannelIoSession newSession(SocketAddress remoteAddress,
			SocketAddress localAddress) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected IoFuture dispose0() throws Exception {
		channelGroup.close();

		if (!parentEventLoop.isShutdown()) {
			parentEventLoop.shutdown();
		}

		if (!childEventLoop.isShutdown()) {
			childEventLoop.shutdown();
		}
		
		if (!parentEventLoop.isShutdown()) {
			parentEventLoop.awaitTermination(5, SECONDS);
		}
		
		if (!childEventLoop.isShutdown()) {
			childEventLoop.awaitTermination(5, SECONDS);
		}
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public S getSessionConfig() {
		return (S)super.getSessionConfig();
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
