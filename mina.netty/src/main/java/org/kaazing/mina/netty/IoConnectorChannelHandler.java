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

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

public class IoConnectorChannelHandler extends SimpleChannelUpstreamHandler {

    private final ChannelIoConnector<?, ?, ?> connector;
    private final ConnectFuture connectFuture;
    private final IoSessionInitializer<?> sessionInitializer;

    public IoConnectorChannelHandler(ChannelIoConnector<?, ?, ?> connector, ConnectFuture connectFuture,
                                     IoSessionInitializer<?> sessionInitializer) {
        this.connector = connector;
        this.connectFuture = connectFuture;
        this.sessionInitializer = sessionInitializer;
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, final ChannelStateEvent e)
            throws Exception {

        Channel channel = e.getChannel();
        ChannelPipeline childPipeline = channel.getPipeline();

        IoSessionFactoryChannelHandler newHandler =
                new IoSessionFactoryChannelHandler(connector, connectFuture, sessionInitializer);
        childPipeline.replace(this, "factory", newHandler);

        ChannelHandlerContext childCtx = childPipeline.getContext(newHandler);
        newHandler.channelConnected(childCtx, e);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        connectFuture.setException(e.getCause());
    }

}
