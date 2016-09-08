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
/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.jboss.netty.channel.socket.nio;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.kaazing.mina.netty.channel.DefaultChannelFutureEx;

import java.net.SocketAddress;

/**
 * Receives downstream events from a {@link ChannelPipeline}.  It contains
 * an array of I/O workers.
 */
class NioChildDatagramPipelineSink extends AbstractNioChannelSink {

    /**
     * Handle downstream event.
     *
     * @param pipeline the {@link ChannelPipeline} that passes down the
     *                 downstream event.
     * @param e The downstream event.
     */
    public void eventSunk(final ChannelPipeline pipeline, final ChannelEvent e)
            throws Exception {

        final NioChildDatagramChannel childChannel = (NioChildDatagramChannel) e.getChannel();
        final ChannelFuture childFuture = e.getFuture();
        if (e instanceof ChannelStateEvent) {
            final ChannelStateEvent stateEvent = (ChannelStateEvent) e;
            final ChannelState state = stateEvent.getState();
            final Object value = stateEvent.getValue();
            switch (state) {
                case OPEN:
                    if (Boolean.FALSE.equals(value)) {
                        childChannel.worker.close(childChannel, childFuture);
                    }
                    break;
            }
        } else if (e instanceof MessageEvent) {
            // Making sure that child channel WriteFuture is fired on child channel's worker thread
            final MessageEvent childMessageEvent = (MessageEvent) e;
            ParentMessageEvent parentMessageEvent = new ParentMessageEvent(childMessageEvent);
            ChannelFuture parentFuture = parentMessageEvent.getFuture();
            parentFuture.addListener(f -> {
                childChannel.getWorker().executeInIoThread(() -> {
                    if (f.isSuccess()) {
                        childFuture.setSuccess();
                    } else {
                        childFuture.setFailure(f.getCause());
                    }
                });
            });

            // Write to parent channel
            NioDatagramChannel parentChannel = (NioDatagramChannel) childChannel.getParent();
            boolean offered = parentChannel.writeBufferQueue.offer(parentMessageEvent);
            assert offered;
            parentChannel.worker.writeFromUserCode(parentChannel);
        }
    }

    private static final class ParentMessageEvent implements MessageEvent {

        private final MessageEvent delegate;
        private final ChannelFuture parentFuture;

        ParentMessageEvent(MessageEvent delegate) {
            this.delegate = delegate;
            this.parentFuture = new DefaultChannelFutureEx();
        }

        @Override
        public Object getMessage() {
            return delegate.getMessage();
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return delegate.getRemoteAddress();
        }

        @Override
        public Channel getChannel() {
            return delegate.getChannel();
        }

        @Override
        public ChannelFuture getFuture() {
            return parentFuture;
        }
    }

}