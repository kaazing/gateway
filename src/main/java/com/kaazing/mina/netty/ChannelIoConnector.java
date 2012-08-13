/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;
import static java.util.concurrent.TimeUnit.SECONDS;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ChannelBufType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;

import java.net.SocketAddress;
import java.util.concurrent.Executor;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.DefaultConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.AbstractIoConnector;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.core.session.IoSessionInitializer;

public abstract class ChannelIoConnector<E extends EventLoopGroup, S extends IoSessionConfig, C extends Channel, A extends SocketAddress> extends AbstractIoConnector implements ChannelIoService {

	private final E eventLoop;
	private final ChannelBufType bufType;
	
	public ChannelIoConnector(S sessionConfig, E eventLoop, ChannelBufType bufType) {
		super(sessionConfig, new Executor() {
			@Override
			public void execute(Runnable command) {
			}
		});
		
		this.eventLoop = eventLoop;
		this.bufType = bufType;
	}

	protected abstract C newChannel(E group);
	
	@Override
	protected ConnectFuture connect0(SocketAddress remoteAddress,
			SocketAddress localAddress,
			final IoSessionInitializer<? extends ConnectFuture> sessionInitializer) {

		final ConnectFuture connectFuture = new DefaultConnectFuture();
		
		C newChannel = newChannel(eventLoop);
		
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(eventLoop);
		bootstrap.handler(new IoConnectorChannelHandler(this, bufType, connectFuture, sessionInitializer));
		bootstrap.channel(newChannel);
		bootstrap.option(CONNECT_TIMEOUT_MILLIS, (int)getConnectTimeoutMillis());
		bootstrap.localAddress(localAddress);
		bootstrap.remoteAddress(remoteAddress);
		bootstrap.connect().addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (!future.isSuccess()) {
					connectFuture.setException(future.cause());
				}
			}
		});
		
		return connectFuture;
	}

	@Override
	protected IoFuture dispose0() throws Exception {
		if (!eventLoop.isShutdown()) {
			eventLoop.shutdown();
			eventLoop.awaitTermination(10, SECONDS);
		}
		return null;
	}

	@Override
	public void initializeSession(ChannelIoSession session, IoFuture future, IoSessionInitializer<?> sessionInitializer) {
		initSession(session, future, sessionInitializer);
	}

	@Override
	@SuppressWarnings("unchecked")
	public S getSessionConfig() {
		return (S)super.getSessionConfig();
	}

	@Override
	@SuppressWarnings("unchecked")
	public A getDefaultRemoteAddress() {
		return (A)super.getDefaultRemoteAddress();
	}

}
