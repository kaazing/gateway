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
package org.kaazing.gateway.util.scheduler;

import static org.kaazing.gateway.util.InternalSystemProperty.BACKGROUND_TASK_THREADS;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class SchedulerProvider {

    private final List<ManagedScheduledExecutorService> schedulers = new ArrayList<>(10);
    private final ManagedScheduledExecutorService sharedScheduler;

    public SchedulerProvider() {
        this(new Properties());
    }

    public SchedulerProvider(Properties configuration) {
        int corePoolSize = BACKGROUND_TASK_THREADS.getIntProperty(configuration);
        sharedScheduler = new ManagedScheduledExecutorService(corePoolSize, "gtwy_bg_tasks", true);
    }


    /*
     * @param owner    Object which will be using the scheduler (and shutting it down when the object is disposed)
     *
     * @param purpose  short description of the purpose of the scheduler, will be set as the thread name
     *                 if a dedicated thread is used
     *
     * @param needDedicatedThread set this to true to guarantee the scheduler will have its own dedicated thread.
     *                            This is appropriate if the tasks that will be scheduled may take significant time,
     *                            e.g. sending periodic keep alive messages to all sessions
     * @return
     */
    public synchronized ScheduledExecutorService getScheduler(final String purpose, boolean needDedicatedThread) {
        return needDedicatedThread ? new ManagedScheduledExecutorService(1, purpose, false) : sharedScheduler;
    }

    public synchronized void shutdownNow() {
        for (ManagedScheduledExecutorService scheduler : schedulers) {
            scheduler.shutdownImmediate();
        }

        schedulers.clear();
        sharedScheduler.shutdownImmediate();
    }

    /**
     * This class represents a scheduler which may be shared. If it is, attempts to shut it down using the standard
     * ScheduledExecutorService methods shutdown and shutdownNow will be noops.
     *
     */
    private class ManagedScheduledExecutorService extends ScheduledThreadPoolExecutor {
        private final boolean shared;

        ManagedScheduledExecutorService(int corePoolSize, final String purpose, boolean shared) {
            super(corePoolSize, new ThreadFactory() {
                final AtomicInteger poolNumber = new AtomicInteger(1);
                final String namePrefix = purpose + "-";

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, namePrefix + poolNumber.getAndIncrement());
                }
            });
            this.shared = shared;
            schedulers.add(this);
        }

        @Override
        public void shutdown() {
            if (!shared) {
                schedulers.remove(this);
                super.shutdown();
            }
        }

        @Override
        public List<Runnable> shutdownNow() {
            if (!shared) {
                schedulers.remove(this);
                return super.shutdownNow();
            }

            return null;
        }

        void shutdownImmediate() {
            super.shutdownNow();
        }
    }
}

