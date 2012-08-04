/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import static java.util.concurrent.TimeUnit.SECONDS;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ChannelBufType;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.channel.ServerChannel;

import java.net.SocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.AbstractIoAcceptor;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.core.session.IoSessionInitializer;

public abstract class ChannelIoAcceptor<E extends EventLoop, S extends IoSessionConfig, P extends ServerChannel, C extends Channel, A extends SocketAddress> extends AbstractIoAcceptor implements ChannelIoService {

	private final ServerBootstrap bootstrap;
	private final E parentEventLoop;
	private final ConcurrentMap<SocketAddress, Channel> boundChannels;
	
	public ChannelIoAcceptor(ChannelBufType bufType, S sessionConfig, E parentEventLoop, E childEventLoop) {
		super(sessionConfig, new Executor() {
			@Override
			public void execute(Runnable command) {
			}
		});
		
		this.bootstrap = new ServerBootstrap()
			.eventLoop(parentEventLoop, childEventLoop)
			.childHandler(new IoAcceptorChildChannelInitializer<C>(this, bufType));
		
		this.parentEventLoop = parentEventLoop;
		this.boundChannels = new ConcurrentHashMap<SocketAddress, Channel>();
	}

	protected abstract P newServerChannel(E parentEventLoop);
	
	@Override
	public void initializeSession(ChannelIoSession session, IoFuture future, IoSessionInitializer<?> sessionInitializer) {
		initSession(session, future, sessionInitializer);
	}
	
	protected ServerBootstrap getBootstrap() {
		return bootstrap;
	}
	
	@Override
	protected Set<SocketAddress> bindInternal(
			List<? extends SocketAddress> localAddresses) throws Exception {

		for (SocketAddress localAddress : localAddresses) {
			bootstrap.channel(newServerChannel(parentEventLoop));
			bootstrap.localAddress(localAddress);
			// must sync to maintain equivalence with Mina semantics
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
		bootstrap.shutdown();
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
