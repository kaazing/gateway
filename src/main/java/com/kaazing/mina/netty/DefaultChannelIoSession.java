/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import static java.lang.Thread.currentThread;

import java.util.concurrent.Executor;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;

import com.kaazing.mina.core.filterchain.DefaultIoFilterChainEx.CallNextSessionIdleCommand;
import com.kaazing.mina.core.service.IoProcessorEx;
import com.kaazing.mina.netty.util.threadlocal.VicariousThreadLocal;

/**
 * This session will throw runtime exceptions if any operation is attempted on the session in a thread other than the
 * thread that created the session.
 */
public class DefaultChannelIoSession extends ChannelIoSession<ChannelConfig> {

    private static final ThreadLocal<Executor> CREATION_ALIGNED_EXECUTOR = new VicariousThreadLocal<Executor>() {

        @Override
        protected Executor initialValue() {
            return new CreationAlignedExecutor(currentThread());
        }

    };

    public DefaultChannelIoSession(ChannelIoService service, IoProcessorEx<ChannelIoSession<? extends ChannelConfig>> processor,
            Channel channel) {
          super(service, processor, channel, new DefaultChannelIoSessionConfig(), currentThread(),
                  CREATION_ALIGNED_EXECUTOR.get());
    }

    private static final class CreationAlignedExecutor implements Executor {
        private final Thread creationThread;

        public CreationAlignedExecutor(Thread creationThread) {
            this.creationThread = creationThread;
        }

        @Override
        public void execute(Runnable command) {
            // READ_IDLE commands are coming from the Timer thread, but still legitimate
            if (currentThread() != creationThread && !(CallNextSessionIdleCommand.class.equals(command.getClass()))) {
                throw new UnsupportedOperationException(
                        String.format("Current thread %s is different from session creator thread %s",
                                      currentThread(), creationThread));
            }
        }
    }

}
