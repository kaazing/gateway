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

import java.util.concurrent.Executor;

/**
 * Receives downstream events from a {@link ChannelPipeline}.  It contains
 * an array of I/O workers.
 */
class NioChildDatagramPipelineSink extends AbstractNioChannelSink {

    private final WorkerPool<NioChildDatagramWorker> workerPool;

    NioChildDatagramPipelineSink(final WorkerPool<NioChildDatagramWorker> workerPool) {
        this.workerPool = workerPool;
    }

    /**
     * Handle downstream event.
     *
     * @param pipeline the {@link ChannelPipeline} that passes down the
     *                 downstream event.
     * @param e The downstream event.
     */
    public void eventSunk(final ChannelPipeline pipeline, final ChannelEvent e)
            throws Exception {

        final NioChildDatagramChannel channel = (NioChildDatagramChannel) e.getChannel();
        final ChannelFuture future = e.getFuture();
        if (e instanceof ChannelStateEvent) {
            final ChannelStateEvent stateEvent = (ChannelStateEvent) e;
            final ChannelState state = stateEvent.getState();
            final Object value = stateEvent.getValue();
            switch (state) {
                case OPEN:
                    if (Boolean.FALSE.equals(value)) {
                        channel.worker.close(channel, future);
                    }
                    break;
            }
        } else if (e instanceof MessageEvent) {
            // final MessageEvent event = (MessageEvent) e;
            // final boolean offered = parent.writeBufferQueue.offer(event);
            // assert offered;
            // parent.worker.writeFromUserCode(parent);
            //
            // In netty AbstractNioChannel#writeBufferQueue is thread-safe but not in mina.netty's copy.
            // Scheduling the write on parent channel thread since child channels and parent channel are
            // on different i/o threads
            NioDatagramChannel parent = (NioDatagramChannel) channel.getParent();

            Runnable writeTask = () -> {
                Object message = ((MessageEvent) e).getMessage();
                ChannelFuture parentFuture = parent.write(message, channel.getRemoteAddress());
                parentFuture.addListener(f -> {
                    if (f.isSuccess()) {
                        future.setSuccess();
                    } else {
                        future.setFailure(f.getCause());
                    }
                });
            };

            parent.getWorker().executeInIoThread(writeTask);
        }
    }

    NioChildDatagramWorker nextWorker() {
        return workerPool.nextWorker();
    }

}