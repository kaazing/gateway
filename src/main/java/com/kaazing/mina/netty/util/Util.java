/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.util;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

public class Util {

    public static void setHeadOutboundBuffer(ChannelHandlerContext ctx,
    		ByteBuf outboundBuffer) throws NoSuchMethodException,
    		IllegalAccessException, InvocationTargetException,
    		NoSuchFieldException {
    
        ChannelHandlerContext head = ctx.channel().unsafe().directOutboundContext();
    
    	Field outByteBufField = OUT_BYTE_BUF_FIELD.get();
    	if (outByteBufField == null) {
    		Class<?> headClass = head.getClass();
    		outByteBufField = headClass.getDeclaredField("outByteBuf");
    		outByteBufField.setAccessible(true);
    		OUT_BYTE_BUF_FIELD.compareAndSet(null, outByteBufField);
    	}
    	
    	outByteBufField.set(head, outboundBuffer);
    }


    private static final AtomicReference<Field> OUT_BYTE_BUF_FIELD = new AtomicReference<Field>();
}
