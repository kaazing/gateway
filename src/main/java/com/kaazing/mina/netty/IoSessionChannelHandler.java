/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import static io.netty.buffer.Unpooled.directBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ChannelBuf;
import io.netty.buffer.ChannelBufType;
import io.netty.buffer.DefaultMessageBuf;
import io.netty.buffer.MessageBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.LinkedList;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.session.IoSessionInitializer;

public class IoSessionChannelHandler extends ChannelInboundHandlerAdapter {

	private final ChannelIoSession session;
	private final ChannelBufType bufType;
	private final IoFuture future;
	private final IoSessionInitializer<?> initializer;
	
	public IoSessionChannelHandler(ChannelIoSession session, ChannelBufType bufType) {
		this(session, bufType, null, null);
	}

	public IoSessionChannelHandler(ChannelIoSession session, ChannelBufType bufType, IoFuture future, IoSessionInitializer<?> initializer) {
		this.session = session;
		this.bufType = bufType;
		this.future = future;
		this.initializer = initializer;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		session.getService().initializeSession(session, future, initializer);
		session.getProcessor().add(session);
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		session.close(true);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		session.getFilterChain().fireExceptionCaught(cause);
	}

	@Override
	public ChannelBuf newInboundBuffer(ChannelHandlerContext ctx)
			throws Exception {
		switch (bufType) {
		case BYTE:
			return directBuffer();
		case MESSAGE:
			return new DefaultMessageBuf<ByteBuf>(new LinkedList<ByteBuf>());
		default:
			throw new IllegalStateException("Unrecognized channel buffer type: " + bufType);
		}
	}

	@Override
	public void inboundBufferUpdated(ChannelHandlerContext ctx)
			throws Exception {

		switch (bufType) {
		case BYTE:
			ByteBuf in = ctx.inboundByteBuffer();
			IoBuffer buf = asIoBuffer(in);
			in.skipBytes(in.readableBytes());
			
			session.getFilterChain().fireMessageReceived(buf);
			break;
		case MESSAGE:
			MessageBuf<ByteBuf> inMsg = ctx.inboundMessageBuffer();
			if (!inMsg.isEmpty()) {
				LinkedList<ByteBuf> inBufs = new LinkedList<ByteBuf>();
				inMsg.drainTo(inBufs);
				for (ByteBuf inBuf : inBufs) {
					IoBuffer ioBuf = asIoBuffer(inBuf);
					inBuf.skipBytes(inBuf.readableBytes());
					session.getFilterChain().fireMessageReceived(ioBuf);
					
				}
			}
			break;
		default:
			throw new IllegalStateException("Unrecognized channel buffer type: " + bufType);
		}
	}

	private static final IoBuffer asIoBuffer(ByteBuf byteBuf) {
		if (byteBuf.hasNioBuffer()) {
			return IoBuffer.wrap(byteBuf.nioBuffer());
		}
		else if (byteBuf.hasArray()) {
			byte[] byteArray = byteBuf.array();
			int offset = byteBuf.arrayOffset();
			int length = byteBuf.readableBytes();
			return IoBuffer.wrap(byteArray, offset, length);
		}
		else {
			throw new IllegalStateException("Unable to convert ByteBuf to IoBuffer");
		}
	}
}
