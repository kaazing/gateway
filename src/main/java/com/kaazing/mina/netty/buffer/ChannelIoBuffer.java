/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.buffer;

import io.netty.buffer.ByteBuf;

import org.apache.mina.core.buffer.IoBuffer;

public interface ChannelIoBuffer {

    public ByteBuf byteBuf();
    
    public IoBuffer ioBuf();
}
