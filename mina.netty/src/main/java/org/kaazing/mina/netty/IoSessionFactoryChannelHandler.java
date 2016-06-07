/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kaazing.mina.netty;

import static org.kaazing.mina.netty.buffer.ByteBufferWrappingChannelBufferFactory.CHANNEL_BUFFER_FACTORY;
import static org.kaazing.mina.netty.buffer.ByteBufferWrappingChannelBufferFactory.OPTIMIZE_PERFORMANCE_CLIENT;
import static java.lang.String.format;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This creates the session then immediately replaces itself with IoSessionChannelHandler, in order to allow
 * the session member variable in IoSessionChannelHandler to be final, which is more efficient.
 */
public class IoSessionFactoryChannelHandler extends SimpleChannelHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(IoSessionFactoryChannelHandler.class);

    private final ChannelIoService service;
    private final ConnectFuture future;
    private final IoSessionInitializer<?> initializer;

    public IoSessionFactoryChannelHandler(ChannelIoService service, ConnectFuture future,
            IoSessionInitializer<?> initializer) {
        this.service = service;
        this.future = future;
        this.initializer = initializer;
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        Channel channel = e.getChannel();

        if (OPTIMIZE_PERFORMANCE_CLIENT) {
            channel.getConfig().setBufferFactory(CHANNEL_BUFFER_FACTORY);
        }

        ChannelIoSession<?> session = service.createSession(channel);
        String baseName = ctx.getName();
        String name = format("%s#session", baseName);
        ChannelHandler handler = new IoSessionChannelHandler(session, future, initializer,
                service.getSessionIdleTracker());
        ChannelPipeline pipeline = ctx.getPipeline();
        pipeline.addAfter(baseName, name, handler);
        ctx.sendUpstream(e);
        pipeline.remove(this);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        if (future != null) {
            future.setException(e.getCause());
        }
        else {
            LOGGER.error("Exception caught in IoSessionFactoryChannelHandler", e.getCause());
            ctx.sendUpstream(e);
        }
    }
}
