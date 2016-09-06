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

import java.util.concurrent.Executor;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.DatagramChannelFactory;
import org.jboss.netty.channel.socket.InternetProtocolFamily;
import org.jboss.netty.util.ExternalResourceReleasable;

/**
 * A {@link DatagramChannelFactory} that creates a NIO-based connectionless
 * {@link DatagramChannel}. It utilizes the non-blocking I/O mode which
 * was introduced with NIO to serve many number of concurrent connections
 * efficiently.
 *
 * <h3>How threads work</h3>
 * <p>
 * There are two types of threads in a {@link NioServerDatagramChannelFactory};
 * one is boss thread and the other is worker thread.
 *
 * <h4>Boss threads</h4>
 * <p>
 * Each bound {@link NioDatagramChannel} has its own boss thread.
 * For example, if you opened two server ports such as 80 and 443, you will
 * have two boss threads.  A boss thread creates child sessions based on
 * the remote address of client.  Once a child connection is created
 * successfully, the boss thread passes the child {@link Channel} to one of
 * the worker threads that the {@link NioServerDatagramChannelFactory} manages.
 *
 * <h4>Worker threads</h4>
 * <p>
 * One {@link NioServerDatagramChannelFactory} can have one or more worker
 * threads.  A worker thread performs non-blocking read and write for one or
 * more {@link Channel}s in a non-blocking mode.
 */
public class NioServerDatagramChannelFactory implements DatagramChannelFactory {

    private final NioDatagramPipelineSink sink;
    private final NioChildDatagramPipelineSink childSink;
    private final BossPool<NioServerDatagramBoss> bossPool;
    private final WorkerPool<NioWorker> workerPool;
    private final InternetProtocolFamily family;
    private boolean releasePool;

    public NioServerDatagramChannelFactory(Executor bossExecutor, int bossCount, WorkerPool<NioWorker> workerPool) {
        bossPool = new NioDatagramBossPool(bossExecutor, bossCount, null);
        this.workerPool = workerPool;
        this.family = null;
        sink = new NioDatagramPipelineSink();
        childSink = new NioChildDatagramPipelineSink();
        releasePool = true;
    }

    public DatagramChannel newChannel(final ChannelPipeline pipeline) {
        return new NioDatagramChannel(this, pipeline, sink, bossPool.nextBoss(), family);
    }

    // mina.netty change -  adding this to create child datagram channels
    public NioChildDatagramChannel newChildChannel(Channel parent, final ChannelPipeline pipeline) {
        return new NioChildDatagramChannel(parent, this, pipeline, childSink, workerPool.nextWorker(), family);
    }

    public void shutdown() {
        workerPool.shutdown();
        bossPool.shutdown();
        if (releasePool) {
            releasePool();
        }
    }

    public void releaseExternalResources() {
        workerPool.shutdown();
        bossPool.shutdown();
        releasePool();
    }

    private void releasePool() {
        if (workerPool instanceof ExternalResourceReleasable) {
            ((ExternalResourceReleasable) workerPool).releaseExternalResources();
        }
        if (bossPool instanceof ExternalResourceReleasable) {
            ((ExternalResourceReleasable) bossPool).releaseExternalResources();
        }
    }
}
