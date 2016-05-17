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

import static java.lang.Thread.currentThread;

import java.util.concurrent.Executor;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;

import org.kaazing.mina.core.filterchain.DefaultIoFilterChainEx.CallNextSessionIdleCommand;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.netty.util.threadlocal.VicariousThreadLocal;

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
