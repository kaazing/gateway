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

import java.nio.channels.Selector;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.DatagramChannelFactory;
import org.jboss.netty.channel.socket.InternetProtocolFamily;
import org.jboss.netty.channel.socket.Worker;
import org.jboss.netty.channel.socket.oio.OioDatagramChannelFactory;
import org.jboss.netty.util.ExternalResourceReleasable;

/**
 * A {@link DatagramChannelFactory} that creates a NIO-based connectionless
 * {@link DatagramChannel}. It utilizes the non-blocking I/O mode which
 * was introduced with NIO to serve many number of concurrent connections
 * efficiently.
 *
 * <h3>How threads work</h3>
 * <p>
 * There is only one thread type in a {@link NioDatagramChannelFactory};
 * worker threads.
 *
 * <h4>Worker threads</h4>
 * <p>
 * One {@link NioDatagramChannelFactory} can have one or more worker
 * threads.  A worker thread performs non-blocking read and write for one or
 * more {@link DatagramChannel}s in a non-blocking mode.
 *
 * <h3>Life cycle of threads and graceful shutdown</h3>
 * <p>
 * All worker threads are acquired from the {@link Executor} which was specified
 * when a {@link NioDatagramChannelFactory} was created.  Therefore, you should
 * make sure the specified {@link Executor} is able to lend the sufficient
 * number of threads.  It is the best bet to specify
 * {@linkplain Executors#newCachedThreadPool() a cached thread pool}.
 * <p>
 * All worker threads are acquired lazily, and then released when there's
 * nothing left to process.  All the related resources such as {@link Selector}
 * are also released when the worker threads are released.  Therefore, to shut
 * down a service gracefully, you should do the following:
 *
 * <ol>
 * <li>close all channels created by the factory usually using
 *     {@link ChannelGroup#close()}, and</li>
 * <li>call {@link #releaseExternalResources()}.</li>
 * </ol>
 *
 * Please make sure not to shut down the executor until all channels are
 * closed.  Otherwise, you will end up with a {@link RejectedExecutionException}
 * and the related resources might not be released properly.
 *
 * <h3>Limitation</h3>
 * <p>
 * Multicast is not supported.  Please use {@link OioDatagramChannelFactory}
 * instead.
 *
 * @apiviz.landmark
 */
public class NioDatagramChannelFactory implements DatagramChannelFactory {

    private final NioDatagramPipelineSink sink;
    private final NioChildDatagramPipelineSink childSink;
    private final WorkerPool<NioDatagramWorker> workerPool;
    private final WorkerPool<NioWorker> childPool;
    private final InternetProtocolFamily family;
    private boolean releasePool;

    /**
     * Create a new {@link NioDatagramChannelFactory} with a {@link Executors#newCachedThreadPool()}
     * and without preferred {@link InternetProtocolFamily}.  Please note that the {@link InternetProtocolFamily}
     * of the channel will be platform (and possibly configuration) dependent and therefore
     * unspecified.  Use {@link #NioDatagramChannelFactory(InternetProtocolFamily)} if unsure.
     *
     * See {@link #NioDatagramChannelFactory(Executor)}
     */
    public NioDatagramChannelFactory() {
        this((InternetProtocolFamily) null);
    }

    /**
     * Create a new {@link NioDatagramChannelFactory} with a {@link Executors#newCachedThreadPool()}.
     *
     * See {@link #NioDatagramChannelFactory(Executor)}
     */
    public NioDatagramChannelFactory(InternetProtocolFamily family) {
        workerPool = new NioDatagramWorkerPool(Executors.newCachedThreadPool(), SelectorUtil.DEFAULT_IO_THREADS);
        childPool = new NioWorkerPool(Executors.newCachedThreadPool(), SelectorUtil.DEFAULT_IO_THREADS);
        this.family = family;
        sink = new NioDatagramPipelineSink(workerPool);
        childSink = new NioChildDatagramPipelineSink(childPool);
        releasePool = true;
    }

    public DatagramChannel newChannel(final ChannelPipeline pipeline) {
        return new NioDatagramChannel(this, pipeline, sink, sink.nextWorker(), family);
    }

    // mina.netty change -  adding this to create child datagram channels
    public NioChildDatagramChannel newChildChannel(Channel parent, final ChannelPipeline pipeline) {
        return new NioChildDatagramChannel(parent, this, pipeline, childSink, childSink.nextWorker(), family);
    }

    public void shutdown() {
        workerPool.shutdown();
        childPool.shutdown();
        if (releasePool) {
            releasePool();
        }
    }

    public void releaseExternalResources() {
        workerPool.shutdown();
        childPool.shutdown();
        releasePool();
    }

    private void releasePool() {
        if (workerPool instanceof ExternalResourceReleasable) {
            ((ExternalResourceReleasable) workerPool).releaseExternalResources();
        }
        if (childPool instanceof ExternalResourceReleasable) {
            ((ExternalResourceReleasable) childPool).releaseExternalResources();
        }
    }
}