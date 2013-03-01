/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import static org.jboss.netty.channel.Channels.pipeline;

import java.net.SocketAddress;
import java.util.concurrent.Executor;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.DefaultConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;

import com.kaazing.mina.core.service.AbstractIoConnector;
import com.kaazing.mina.core.session.IoSessionConfigEx;

public abstract class ChannelIoConnector<C extends IoSessionConfigEx, F extends ChannelFactory, A extends SocketAddress> extends AbstractIoConnector implements ChannelIoService {

	private final F channelFactory;
	private ChannelPipelineFactory pipelineFactory;
	private final ChannelGroup channelGroup;

	public ChannelIoConnector(C sessionConfig, F channelFactory) {
		super(sessionConfig, new Executor() {
			@Override
			public void execute(Runnable command) {
			}
		});

		this.channelFactory = channelFactory;
		this.channelGroup = new DefaultChannelGroup();
	}
	
	public void setPipelineFactory(ChannelPipelineFactory pipelineFactory) {
		this.pipelineFactory = pipelineFactory;
	}

	@Override
	protected ConnectFuture connect0(SocketAddress remoteAddress,
			SocketAddress localAddress,
			final IoSessionInitializer<? extends ConnectFuture> sessionInitializer) {

		final ConnectFuture connectFuture = new DefaultConnectFuture();
		
		ClientBootstrap bootstrap = new ClientBootstrap(new ChannelFactory() {

			@Override
			public Channel newChannel(ChannelPipeline pipeline) {
				Channel newChannel = channelFactory.newChannel(pipeline);
				ChannelConfig newChannelConfig = newChannel.getConfig();
				newChannelConfig.setConnectTimeoutMillis((int)getConnectTimeoutMillis());
				return newChannel;
			}

			@Override
			public void releaseExternalResources() {
				channelFactory.releaseExternalResources();
			}
			
		});

		// support custom channel handlers before bridge
		ChannelPipeline newPipeline;
		if (pipelineFactory != null) {
			try {
				newPipeline = pipelineFactory.getPipeline();
			} catch (Exception e) {
				connectFuture.setException(e);
				return connectFuture;
			}
		}
		else {
			newPipeline = pipeline();
		}
		
		
		newPipeline.addLast("mina-bridge", new IoConnectorChannelHandler(this, connectFuture, sessionInitializer));
		bootstrap.setPipeline(newPipeline);
		ChannelFuture channelFuture = bootstrap.connect(remoteAddress, localAddress);
		channelFuture.addListener(new ChannelFutureListener() {
			
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (!future.isSuccess()) {
					connectFuture.setException(future.getCause());
				} else {
					channelGroup.add(future.getChannel());
				}
			}
		});
		
		return connectFuture;
	}

	@Override
	protected IoFuture dispose0() throws Exception {
		channelGroup.close();
		channelFactory.releaseExternalResources();
		return null;
	}

	@Override
	public void initializeSession(ChannelIoSession session, IoFuture future, IoSessionInitializer<?> sessionInitializer) {
		initSession(session, future, sessionInitializer);
	}

	@Override
	@SuppressWarnings("unchecked")
	public C getSessionConfig() {
		return (C)super.getSessionConfig();
	}

	@Override
	@SuppressWarnings("unchecked")
	public A getDefaultRemoteAddress() {
		return (A)super.getDefaultRemoteAddress();
	}

}
