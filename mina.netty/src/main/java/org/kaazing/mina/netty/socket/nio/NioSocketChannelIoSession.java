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
package org.kaazing.mina.netty.socket.nio;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;

import java.util.concurrent.Executor;

import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.socket.nio.AbstractNioWorker;
import org.jboss.netty.channel.socket.nio.NioSocketChannel;
import org.jboss.netty.channel.socket.nio.NioSocketChannelConfig;
import org.jboss.netty.channel.socket.nio.NioWorker;

import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.netty.ChannelIoService;
import org.kaazing.mina.netty.ChannelIoSession;

/**
 * This session is always used in conjunction with an NioSocketChannel, which necessarily has an associated worker.
 * It forces all operations of the session to be done in the worker thread (using worker.executeIntoThread if a call
 * is made in another thread).
 */
public class NioSocketChannelIoSession extends ChannelIoSession<NioSocketChannelConfig> {

    public NioSocketChannelIoSession(ChannelIoService service, IoProcessorEx<ChannelIoSession<? extends ChannelConfig>>
        processor, NioSocketChannel channel) {
        super(service, processor, channel, new NioSocketChannelIoSessionConfig(channel.getConfig()), currentThread(),
                asExecutor(channel.getWorker()));
    }

    @Override
    protected void setIoAlignment0(Thread ioThread, Executor ioExecutor) {
        NioSocketChannel channel = (NioSocketChannel) getChannel();
        if (ioExecutor == NO_EXECUTOR) {
            channel.setWorker(null);
        }
        else if (isClosedReceived()) {
            // Process the closed event now that realignment is complete
            // We must not register the channel with the worker since it is closed
            getProcessor().remove(this);
        }
        else {
            AbstractNioWorker newWorker = ((WorkerExecutor) ioExecutor).worker;
            channel.setWorker(newWorker);
        }
    }

    private static Executor asExecutor(NioWorker worker) {
        WorkerExecutor executor = (WorkerExecutor) CURRENT_WORKER.get();
        if (executor == null) {
            assert isInIoThread(worker) : "Session created from non-I/O thread";
            executor = new WorkerExecutor(worker);
            CURRENT_WORKER.set(executor);
        }
        assert executor.worker == worker : "Worker does not match I/O thread";
        return executor;
    }

    private static boolean isInIoThread(NioWorker worker) {
        final Thread[] ioThread = new Thread[]{null};
        worker.executeInIoThread(new Runnable() {
            @Override
            public void run() {
                ioThread[0] = currentThread();
            }
        });
        boolean aligned = ioThread[0] == currentThread();
        assert aligned : format("Current thread %s does not match I/O thread %s", currentThread(), ioThread[0]);
        return aligned;
    }

}
