/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ChannelBufType;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.MessageBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandlerContext;

import java.net.SocketAddress;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.DefaultIoFilterChain;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.AbstractIoSession;
import org.apache.mina.core.session.IoSessionConfig;


public class ChannelIoSession extends AbstractIoSession implements MutableIoSession {

    public static enum InterestOps { READ, WRITE };
    
	public static final Object FLUSH = new Object();

	private final ChannelIoService service;
	private final ChannelHandlerContext ctx;
	private final Channel channel;
	private final ChannelIoSessionConfig<?> config;
	private final ChannelIoProcessor processor;
	private final IoFilterChain filterChain;
	private final AtomicInteger filterCount;
	private final TransportMetadata transportMetadata;
    private final AtomicInteger readSuspendCount;
    private final EnumSet<InterestOps> interestOps;
    private final ChannelBufType bufferType;
    private final ByteBuf outboundByteBuffer;
    private final CompositeByteBuf compositeOutboundByteBuffer;
    private final MessageBuf<Object> outboundMessageBuffer;
    private final ByteBuf inboundByteBuffer;
    private final IoBuffer inboundIoBuffer;
    
    private IoHandler handler;
	
	public ChannelIoSession(ChannelIoService service, ChannelHandlerContext ctx) {
		this.service = service;
		this.ctx = ctx;
		this.channel = ctx.channel();
		this.config = new ChannelIoSessionConfig<ChannelConfig>(channel.config());
        this.config.setAll(service.getSessionConfig());
        this.handler = service.getHandler();
		this.processor = new ChannelIoProcessor();
		this.filterCount = new AtomicInteger();
		this.filterChain = new DefaultIoFilterChain(this) {

            @Override
            public synchronized void addFirst(String name, IoFilter filter) {
                super.addFirst(name, filter);
                filterCount.incrementAndGet();
            }

            @Override
            public synchronized void addLast(String name, IoFilter filter) {
                super.addLast(name, filter);
                filterCount.incrementAndGet();
            }

            @Override
            public synchronized void addBefore(String baseName, String name,
                    IoFilter filter) {
                super.addBefore(baseName, name, filter);
                filterCount.incrementAndGet();
            }

            @Override
            public synchronized void addAfter(String baseName, String name,
                    IoFilter filter) {
                super.addAfter(baseName, name, filter);
                filterCount.incrementAndGet();
            }

            @Override
            public synchronized IoFilter remove(String name) {
                IoFilter filter = super.remove(name);
                filterCount.decrementAndGet();
                return filter;
            }

            @Override
            public synchronized void remove(IoFilter filter) {
                super.remove(filter);
                filterCount.decrementAndGet();
            }

            @Override
            public synchronized IoFilter remove(
                    Class<? extends IoFilter> filterType) {
                IoFilter filter = super.remove(filterType);
                filterCount.decrementAndGet();
                return filter;
            }
		    
		};
		this.transportMetadata = service.getTransportMetadata();
        this.readSuspendCount = new AtomicInteger();
        this.interestOps = EnumSet.allOf(InterestOps.class);

        ChannelBufType bufferType = channel.metadata().bufferType();
        switch (bufferType) {
        case BYTE:
            ByteBuf inboundByteBuffer = ctx.inboundByteBuffer();
            ByteBuf outboundByteBuffer = channel.outboundByteBuffer();
            IoBuffer inboundIoBuffer = IoBuffer.wrap(inboundByteBuffer.nioBuffer(0, inboundByteBuffer.capacity())).flip();
            
            this.inboundByteBuffer = inboundByteBuffer;
            this.inboundIoBuffer = new ChannelIoBufferImpl(inboundIoBuffer, inboundByteBuffer);
            this.outboundByteBuffer = outboundByteBuffer;
            this.compositeOutboundByteBuffer = (outboundByteBuffer instanceof CompositeByteBuf) ? (CompositeByteBuf)outboundByteBuffer : null;
            this.outboundMessageBuffer = null;
            break;
        case MESSAGE:
            MessageBuf<Object> outboundMessageBuffer = channel.outboundMessageBuffer();

            this.inboundByteBuffer = null;
            this.inboundIoBuffer = null;
            this.outboundByteBuffer = null;
            this.compositeOutboundByteBuffer = null;
            this.outboundMessageBuffer = outboundMessageBuffer;
            break;
        default:
            throw new IllegalArgumentException("bufferType");
        }
        
        this.bufferType = bufferType;
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
	public void setHandler(IoHandler handler) {
	    this.handler = handler;
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

    @Override
    public WriteFuture write(Object message, SocketAddress remoteAddress) {
        if (filterCount.get() == 0 && message instanceof IoBuffer) {
            IoBuffer buf = (IoBuffer)message;
            ByteBuf byteBuf = (buf instanceof ChannelIoBuffer) ? ((ChannelIoBuffer)buf).byteBuf() : wrappedBuffer(buf.buf());
            switch (bufferType) {
            case BYTE:
                if (compositeOutboundByteBuffer != null) {
                    compositeOutboundByteBuffer.addComponent(byteBuf);
                    int writerIndex = compositeOutboundByteBuffer.writerIndex();
                    compositeOutboundByteBuffer.writerIndex(writerIndex + byteBuf.readableBytes());
                }
                else {
                    outboundByteBuffer.writeBytes(byteBuf);
                }
                break;
            case MESSAGE:
                outboundMessageBuffer.offer(byteBuf);
                break;
            }
            return new ChannelWriteFuture(this, ctx.flush());
        }
        return super.write(message, remoteAddress);
    }

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

    public void notifyInboundByteBufferUpdated() {
        IoBuffer inboundIoBuffer = this.inboundIoBuffer;
        ByteBuf inboundByteBuffer = this.inboundByteBuffer;

        // sync indexes before message delivery
        inboundIoBuffer.position(inboundByteBuffer.readerIndex());
        inboundIoBuffer.limit(inboundByteBuffer.writerIndex());
        
        if (filterCount.get() == 0) {
            try {
                handler.messageReceived(this, inboundIoBuffer);
            }
            catch (Exception e) {
                filterChain.fireExceptionCaught(e);
            }
        }
        else {
            filterChain.fireMessageReceived(inboundIoBuffer);
        }
        
        // assumes in-bound buffer can be recycled if reads are not suspended
        // if reads are suspended, then in-bound buffer will be recycled later,
        // when reads are resumed (see ChannelIoProcessor)
        if (ctx.isReadable()) {
            inboundByteBuffer.setIndex(0, 0);
        }
    }

    public void notifyInboundMessageBufferUpdated() {
        MessageBuf<ByteBuf> inMsg = ctx.inboundMessageBuffer();
        if (!inMsg.isEmpty()) {
            LinkedList<ByteBuf> inBufs = new LinkedList<ByteBuf>();
            inMsg.drainTo(inBufs);
            if (filterCount.get() == 0) {
                try {
                    for (ByteBuf inBuf : inBufs) {
                        IoBuffer ioBuf = ChannelIoBuffers.wrap(inBuf).ioBuf();
                        if (filterCount.get() == 0) {
                            handler.messageReceived(this, ioBuf);
                        }
                        else {
                            filterChain.fireMessageReceived(ioBuf);
                        }
                    }
                }
                catch (Exception e) {
                    filterChain.fireExceptionCaught(e);
                }
            }
            else {
                for (ByteBuf inBuf : inBufs) {
                    IoBuffer ioBuf = ChannelIoBuffers.wrap(inBuf).ioBuf();
                    filterChain.fireMessageReceived(ioBuf);
                }
            }
        }
    }
}
