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
import org.jboss.netty.channel.socket.DatagramChannelConfig;
import org.jboss.netty.channel.socket.nio.NioDatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramWorker;

import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.netty.ChannelIoService;
import org.kaazing.mina.netty.ChannelIoSession;
import org.kaazing.mina.netty.socket.DefaultDatagramChannelIoSessionConfig;
import org.kaazing.mina.netty.util.threadlocal.VicariousThreadLocal;

/**
 * This session is always used in conjunction with an NioDatagramChannel, which necessarily has an associated worker.
 * It forces all operations of the session to be done in the worker thread (using worker.executeIntoThread if a call
 * is made in another thread).
 */
public class NioDatagramChannelIoSession extends ChannelIoSession<DatagramChannelConfig> {

    private static final ThreadLocal<WorkerExecutor> WORKER_EXECUTOR = new VicariousThreadLocal<>();

    public NioDatagramChannelIoSession(ChannelIoService service,
            IoProcessorEx<ChannelIoSession<? extends ChannelConfig>> processor, NioDatagramChannel channel) {
        super(service, processor, channel, new DefaultDatagramChannelIoSessionConfig(),
                currentThread(), asExecutor(channel.getWorker()));
    }

    private static Executor asExecutor(NioDatagramWorker worker) {
        assert isInIoThread(worker) : "Session created from non-I/O thread";
        WorkerExecutor executor = WORKER_EXECUTOR.get();
        if (executor == null) {
            executor = new WorkerExecutor(worker);
            WORKER_EXECUTOR.set(executor);
        }
        assert executor.worker == worker : "Worker does not match I/O thread";
        return executor;
    }

    private static boolean isInIoThread(NioDatagramWorker worker) {
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

    private static final class WorkerExecutor implements Executor {
        private final NioDatagramWorker worker;

        WorkerExecutor(NioDatagramWorker worker) {
            this.worker = worker;
        }

        @Override
        public void execute(Runnable command) {
            worker.executeInIoThread(command, /* alwaysAsync */ true);
        }
    }

}
