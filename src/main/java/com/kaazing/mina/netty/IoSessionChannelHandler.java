/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import static com.kaazing.mina.netty.util.Util.setHeadOutboundBuffer;
import static io.netty.buffer.Unpooled.directBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ChannelBuf;
import io.netty.buffer.ChannelBufType;
import io.netty.buffer.DefaultCompositeByteBuf;
import io.netty.buffer.DefaultMessageBuf;
import io.netty.buffer.MessageBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.LinkedList;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSessionInitializer;

import com.kaazing.mina.netty.buffer.ChannelIoBuffers;

public class IoSessionChannelHandler extends ChannelInboundHandlerAdapter {

    private final ChannelIoService service;
	private final ChannelBufType bufType;
	private final ConnectFuture connectFuture;
	private final IoSessionInitializer<?> initializer;

	private volatile ChannelIoSession session;
	
	public IoSessionChannelHandler(ChannelIoService service, ChannelBufType bufType) {
		this(service, bufType, null, null);
	}

	public IoSessionChannelHandler(ChannelIoService service, ChannelBufType bufType, ConnectFuture connectFuture, IoSessionInitializer<?> initializer) {
	    this.service = service;
		this.bufType = bufType;
		this.connectFuture = connectFuture;
		this.initializer = initializer;
	}

	@Override
    public void beforeAdd(ChannelHandlerContext ctx) throws Exception {
	    session = new ChannelIoSession(service, ctx);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        DefaultCompositeByteBuf outboundBuffer = new DefaultCompositeByteBuf(8192 / 4);
        setHeadOutboundBuffer(ctx, outboundBuffer);
    }

    @Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		session.getService().initializeSession(session, connectFuture, initializer);
		session.getProcessor().add(session);
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		session.close(true);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		// ChannelIoConnector handles the failure case with a ChannelFutureListener
		// ChannelIoAcceptor instantiates this handler with null connect future
		if (connectFuture == null || connectFuture.isConnected()) {
			session.getFilterChain().fireExceptionCaught(cause);
		}
	}

	@Override
	public ChannelBuf newInboundBuffer(ChannelHandlerContext ctx)
			throws Exception {
		switch (bufType) {
		case BYTE:
			// TODO: eliminate magic values
			return directBuffer(8192, 8192);
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
			IoBuffer buf = ChannelIoBuffers.wrap(in).ioBuf();
			session.getFilterChain().fireMessageReceived(buf);

			// assumes in-bound buffer can be recycled if reads are not suspended
			// if reads are suspended, then in-bound buffer will be recycled later,
			// when reads are resumed (see ChannelIoProcessor)
			// TODO: Ctrl-S to close slow client, then Ctrl-Q to resume after closed
			//       the flush causes framing bytes to jump across the proxy!
			if (ctx.isReadable()) {
			    in.setIndex(0, 0);
			}

			break;
		case MESSAGE:
			MessageBuf<ByteBuf> inMsg = ctx.inboundMessageBuffer();
			if (!inMsg.isEmpty()) {
				LinkedList<ByteBuf> inBufs = new LinkedList<ByteBuf>();
				inMsg.drainTo(inBufs);
				for (ByteBuf inBuf : inBufs) {
					IoBuffer ioBuf = ChannelIoBuffers.wrap(inBuf).ioBuf();
					session.getFilterChain().fireMessageReceived(ioBuf);
				}
			}
			break;
		default:
			throw new IllegalStateException("Unrecognized channel buffer type: " + bufType);
		}
	}
}
