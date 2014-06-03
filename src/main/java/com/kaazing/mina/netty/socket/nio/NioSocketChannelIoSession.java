/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket.nio;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;

import java.util.concurrent.Executor;

import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.socket.nio.NioSocketChannel;
import org.jboss.netty.channel.socket.nio.NioSocketChannelConfig;
import org.jboss.netty.channel.socket.nio.NioWorker;

import com.kaazing.mina.core.service.IoProcessorEx;
import com.kaazing.mina.netty.ChannelIoService;
import com.kaazing.mina.netty.ChannelIoSession;
import com.kaazing.mina.netty.util.threadlocal.VicariousThreadLocal;

/**
 * This session is always used in conjunction with an NioSocketChannel, which necessarily has an associated worker.
 * It forces all operations of the session to be done in the worker thread (using worker.executeIntoThread if a call
 * is made in another thread).
 */
public class NioSocketChannelIoSession extends ChannelIoSession<NioSocketChannelConfig> {

    // TODO: move to non-static on NioSocketChannelIoAcceptor / NioSocketChannelIoConnector
    private static final ThreadLocal<WorkerExecutor> WORKER_EXECUTOR = new VicariousThreadLocal<WorkerExecutor>();

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
        else {
            NioWorker newWorker = ((WorkerExecutor) ioExecutor).worker;
            channel.setWorker(newWorker);
        }
    }

    private static Executor asExecutor(NioWorker worker) {
        WorkerExecutor executor = WORKER_EXECUTOR.get();
        if (executor == null) {
            assert isInIoThread(worker) : "Session created from non-I/O thread";
            executor = new WorkerExecutor(worker);
            WORKER_EXECUTOR.set(executor);
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

    private static final class WorkerExecutor implements Executor {
        private final NioWorker worker;

        WorkerExecutor(NioWorker worker) {
            this.worker = worker;
        }

        @Override
        public void execute(Runnable command) {
            worker.executeInIoThread(command, /* alwaysAsync */ true);
        }
    }

}
