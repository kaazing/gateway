package com.kaazing.mina.transport.netty;

import static org.jboss.netty.channel.Channels.pipeline;
import static org.jboss.netty.channel.Channels.pipelineFactory;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.DefaultSocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ServerChannelFactory;
import org.jboss.netty.channel.local.DefaultLocalClientChannelFactory;
import org.jboss.netty.channel.local.DefaultLocalServerChannelFactory;
import org.jboss.netty.channel.local.LocalAddress;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.ServerSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.junit.Test;

import com.kaazing.mina.netty.ChannelIoAcceptor;
import com.kaazing.mina.netty.ChannelIoConnector;
import com.kaazing.mina.netty.socket.SocketChannelIoAcceptor;
import com.kaazing.mina.netty.socket.SocketChannelIoConnector;

public class IT {

	@Test
	public void testMinaSocket() throws Exception {
		
		DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();
		builder.addLast("logger", new LoggingFilter());

		
		IoAcceptor acceptor = new NioSocketAcceptor();
		acceptor.setFilterChainBuilder(builder);
		acceptor.setHandler(new IoHandlerAdapter() {
			@Override
			public void messageReceived(IoSession session, Object message)
					throws Exception {
				IoBuffer buf = (IoBuffer)message;
				session.write(buf.duplicate());
			}
		});

		acceptor.bind(new InetSocketAddress(8000));

		IoConnector connector = new NioSocketConnector();
		connector.setFilterChainBuilder(builder);
		connector.setHandler(new IoHandlerAdapter());

		ConnectFuture connectFuture = connector.connect(new InetSocketAddress(8000));
		connectFuture.awaitUninterruptibly();
		IoSession session = connectFuture.getSession();
		session.write(IoBuffer.wrap(new byte[] { 0x00, 0x01, 0x02 })).awaitUninterruptibly();
		Thread.sleep(1000);
		session.close(false).awaitUninterruptibly();
		
		acceptor.unbind(new InetSocketAddress(8000));
		
		connector.dispose();
		acceptor.dispose();
	}
	
	@Test
	public void testNettySocket() throws Exception {
		
		ServerSocketChannelFactory serverChannelFactory = new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(), 
				Executors.newCachedThreadPool());
		
		IoAcceptor acceptor = new SocketChannelIoAcceptor(new DefaultSocketSessionConfig(), serverChannelFactory);
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
		
		acceptor.bind(new InetSocketAddress(8000));

		ClientSocketChannelFactory clientChannelFactory = new NioClientSocketChannelFactory(
				Executors.newCachedThreadPool(), 
				Executors.newCachedThreadPool());

		IoConnector connector = new SocketChannelIoConnector(new DefaultSocketSessionConfig(), clientChannelFactory);
		connector.setFilterChainBuilder(builder);
		connector.setHandler(new IoHandlerAdapter());
		
		final AtomicBoolean sessionInitialized = new AtomicBoolean();
		ConnectFuture connectFuture = connector.connect(new InetSocketAddress(8000), new IoSessionInitializer<ConnectFuture>() {
		
			@Override
			public void initializeSession(IoSession session, ConnectFuture future) {
				sessionInitialized.set(true);
			}
		});
		
		connectFuture.awaitUninterruptibly();
		assertTrue(sessionInitialized.get());
		IoSession session = connectFuture.getSession();
		session.write(IoBuffer.wrap(new byte[] { 0x00, 0x01, 0x02 })).awaitUninterruptibly();
		Thread.sleep(1000);
		session.close(true).awaitUninterruptibly();
		acceptor.unbind(new InetSocketAddress(8000));
		
		connector.dispose();
		acceptor.dispose();
	}

	@Test
	public void testNettyLocal() throws Exception {
		
		ServerChannelFactory serverChannelFactory = new DefaultLocalServerChannelFactory();
		
		ChannelIoAcceptor acceptor = new ChannelIoAcceptor(serverChannelFactory);
		DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();
		builder.addLast("logger", new LoggingFilter());
		acceptor.setPipelineFactory(pipelineFactory(pipeline(new LoggingHandler(InternalLogLevel.INFO))));
		acceptor.setFilterChainBuilder(builder);
		acceptor.setHandler(new IoHandlerAdapter() {
			@Override
			public void messageReceived(IoSession session, Object message)
					throws Exception {
				IoBuffer buf = (IoBuffer)message;
				session.write(buf.duplicate());
			}
		});
		
		acceptor.bind(new LocalAddress(8000));

		ChannelFactory clientChannelFactory = new DefaultLocalClientChannelFactory();

		ChannelIoConnector connector = new ChannelIoConnector(clientChannelFactory);
		connector.setPipelineFactory(pipelineFactory(pipeline(new LoggingHandler(InternalLogLevel.INFO))));
		connector.setFilterChainBuilder(builder);
		connector.setHandler(new IoHandlerAdapter());
		
		final AtomicBoolean sessionInitialized = new AtomicBoolean();
		ConnectFuture connectFuture = connector.connect(new LocalAddress(8000), new IoSessionInitializer<ConnectFuture>() {
		
			@Override
			public void initializeSession(IoSession session, ConnectFuture future) {
				sessionInitialized.set(true);
			}
		});
		
		connectFuture.awaitUninterruptibly();
		assertTrue(sessionInitialized.get());
		IoSession session = connectFuture.getSession();
		session.write(IoBuffer.wrap(new byte[] { 0x00, 0x01, 0x02 })).awaitUninterruptibly();
		Thread.sleep(1000);
		session.close(true).awaitUninterruptibly();
		acceptor.unbind(new LocalAddress(8000));
		
		connector.dispose();
		acceptor.dispose();
	}
}
