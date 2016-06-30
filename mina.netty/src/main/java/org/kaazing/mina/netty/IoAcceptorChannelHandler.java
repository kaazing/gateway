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

import static java.lang.String.format;

import java.util.Map;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ChildChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;

public class IoAcceptorChannelHandler extends SimpleChannelUpstreamHandler {

    private final ChannelIoAcceptor<?, ?, ?> acceptor;
    private final ChannelGroup channelGroup;
    private final ChannelHandler bindHandler;
    private ChannelPipelineFactory pipelineFactory;

    public IoAcceptorChannelHandler(ChannelIoAcceptor<?, ?, ?> acceptor, ChannelGroup channelGroup,
            ChannelHandler bindHandler) {
        this.acceptor = acceptor;
        this.channelGroup = channelGroup;
        this.bindHandler = bindHandler;
    }

    public void setPipelineFactory(ChannelPipelineFactory pipelineFactory) {
        this.pipelineFactory = pipelineFactory;
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
        throws Exception {

        // add the bind handler to the pipeline
        String baseName = ctx.getName();
        String name = format("%s:bind", baseName);
        ctx.getPipeline().addAfter(baseName, name, bindHandler);

        // propagate the open event to bind handler
        super.channelOpen(ctx, e);
    }

    @Override
    public void childChannelOpen(ChannelHandlerContext ctx,
            ChildChannelStateEvent e) throws Exception {

        final Channel childChannel = e.getChildChannel();
        if (channelGroup != null) {
            channelGroup.add(childChannel);
        }

        ChannelPipeline childPipeline = childChannel.getPipeline();

        if (pipelineFactory != null) {
            ChannelPipeline newChildPipeline = pipelineFactory.getPipeline();
            for (Map.Entry<String, ChannelHandler> entry : newChildPipeline.toMap().entrySet()) {
                String key = entry.getKey();
                ChannelHandler handler = entry.getValue();
                childPipeline.addLast(key, handler);
            }
        }

        childPipeline.addLast("factory",
                new IoSessionFactoryChannelHandler(acceptor, null, acceptor.getIoSessionInitializer()));

        super.childChannelOpen(ctx, e);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        // this will cause the bind channel future to fail, without noisy logging
        ctx.sendUpstream(e);
    }
}
