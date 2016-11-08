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
package org.jboss.netty.channel.socket.nio;

import org.jboss.netty.util.ThreadNameDeterminer;

import java.util.concurrent.Executor;


/**
 * Holds {@link NioServerDatagramBoss} instances to use
 */
public class NioDatagramBossPool extends AbstractNioBossPool<NioServerDatagramBoss> {
    private final ThreadNameDeterminer determiner;

    /**
     * Create a new instance
     *
     * @param bossExecutor  the {@link Executor} to use for server the {@link NioServerDatagramBoss}
     * @param bossCount     the number of {@link NioServerDatagramBoss} instances this {@link NioDatagramBossPool} will hold
     * @param determiner    the {@link ThreadNameDeterminer} to use for name the threads. Use {@code null}
     *                      if you not want to set one explicit.
     */
    public NioDatagramBossPool(Executor bossExecutor, int bossCount, ThreadNameDeterminer determiner) {
        super(bossExecutor, bossCount, false);
        this.determiner = determiner;
        init();
    }

    /**
     * Create a new instance using no {@link ThreadNameDeterminer}
     *
     * @param bossExecutor  the {@link Executor} to use for server the {@link NioServerDatagramBoss}
     * @param bossCount     the number of {@link NioServerDatagramBoss} instances this {@link NioDatagramBossPool} will hold
     */
    public NioDatagramBossPool(Executor bossExecutor, int bossCount) {
        this(bossExecutor, bossCount, null);
    }

    @Override
    protected NioServerDatagramBoss newBoss(Executor executor) {
        return new NioServerDatagramBoss(executor);
    }
}
