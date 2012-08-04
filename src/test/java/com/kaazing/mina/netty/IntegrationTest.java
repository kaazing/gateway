/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import static org.junit.Assert.assertTrue;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalEventLoop;
import io.netty.channel.local.LocalServerChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.filter.logging.LoggingFilter;
import org.junit.Test;

import com.kaazing.mina.netty.local.LocalChannelIoAcceptor;
import com.kaazing.mina.netty.local.LocalChannelIoConnector;

public class IntegrationTest {
	@Test
	public void testNettyLocal() throws Exception {

		LocalEventLoop eventLoop = new LocalEventLoop();
		LocalChannelIoAcceptor acceptor = new LocalChannelIoAcceptor(eventLoop) {

			@Override
			protected LocalServerChannel newServerChannel(
					LocalEventLoop parentEventLoop) {
				LocalServerChannel newServerChannel = super.newServerChannel(parentEventLoop);
				ChannelPipeline pipeline = newServerChannel.pipeline();
				pipeline.addLast(new LoggingHandler(LogLevel.INFO));
				return newServerChannel;
			}
			
		};
		DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();
		builder.addLast("logger", new LoggingFilter());
		acceptor.setFilterChainBuilder(builder);
		acceptor.setHandler(new IoHandlerAdapter() {
			@Override
			public void messageReceived(IoSession session, Object message)
					throws Exception {
				IoBuffer buf = (IoBuffer)message;
				session.write(buf.duplicate());
			}
		});
		
		acceptor.bind(new LocalAddress("8000"));

		LocalChannelIoConnector connector = new LocalChannelIoConnector(eventLoop) {
			@Override
			protected LocalChannel newChannel() {
				LocalChannel newChannel = super.newChannel();
				ChannelPipeline pipeline = newChannel.pipeline();
				pipeline.addLast(new LoggingHandler(LogLevel.INFO));
				return newChannel;
			}
		};
		connector.setFilterChainBuilder(builder);
		connector.setHandler(new IoHandlerAdapter());
		
		final AtomicBoolean sessionInitialized = new AtomicBoolean();
		ConnectFuture connectFuture = connector.connect(new LocalAddress("8000"), new IoSessionInitializer<ConnectFuture>() {
			@Override
			public void initializeSession(IoSession session, ConnectFuture future) {
				sessionInitialized.set(true);
			}
		});
		
		connectFuture.awaitUninterruptibly();
		assertTrue(sessionInitialized.get());
		IoSession session = connectFuture.getSession();
		session.suspendRead();
		session.resumeRead();
		session.write(IoBuffer.wrap(new byte[] { 0x00, 0x01, 0x02 })).awaitUninterruptibly();
		Thread.sleep(1000);
		session.close(true).awaitUninterruptibly();
		acceptor.unbind(new LocalAddress("8000"));
		
		connector.dispose();
		acceptor.dispose();
	}
}
