/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandlerContext;

import java.net.SocketAddress;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.filterchain.DefaultIoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.AbstractIoSession;
import org.apache.mina.core.session.IoSessionConfig;

public class ChannelIoSession extends AbstractIoSession {

	public static final Object FLUSH = new Object();

	private final ChannelIoService service;
	private final ChannelHandlerContext ctx;
	private final Channel channel;
	private final ChannelIoSessionConfig<?> config;
	private final IoHandler handler;
	private final ChannelIoProcessor processor;
	private final IoFilterChain filterChain;
	private final TransportMetadata transportMetadata;
    private final AtomicInteger readSuspendCount;
	
	public ChannelIoSession(ChannelIoService service, ChannelHandlerContext ctx) {
		this.service = service;
		this.ctx = ctx;
		this.channel = ctx.channel();
		this.config = new ChannelIoSessionConfig<ChannelConfig>(channel.config());
        this.config.setAll(service.getSessionConfig());
        this.handler = service.getHandler();
		this.processor = new ChannelIoProcessor();
		this.filterChain = new DefaultIoFilterChain(this);
		this.transportMetadata = service.getTransportMetadata();
        this.readSuspendCount = new AtomicInteger();
	}

	public ChannelIoService getService() {
		return service;
	}
	
	public ChannelHandlerContext getChannelHandlerContext() {
		return ctx;
	}
	
	public Channel getChannel() {
		return channel;
	}
	
	@Override
	public IoHandler getHandler() {
		return handler;
	}

	@Override
	public IoSessionConfig getConfig() {
		return config;
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
		return channel.localAddress();
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return channel.remoteAddress();
	}

	@Override
	public TransportMetadata getTransportMetadata() {
		return transportMetadata;
	}

	public static enum InterestOps { READ, WRITE };
	
	private final EnumSet<InterestOps> interestOps = EnumSet.allOf(InterestOps.class);

    public Set<InterestOps> getInterestOps() {
        return interestOps;
    }
    
    public Set<InterestOps> updateInterestOps() {
        if (isReadSuspended()) {
            interestOps.remove(InterestOps.READ);
        }
        else {
            interestOps.add(InterestOps.READ);
        }
        if (isWriteSuspended()) {
            interestOps.remove(InterestOps.WRITE);
        }
        else {
            interestOps.add(InterestOps.WRITE);
        }
        return interestOps;
    }
    
    public AtomicInteger getReadSuspendCount() {
        return readSuspendCount;
    }
}
