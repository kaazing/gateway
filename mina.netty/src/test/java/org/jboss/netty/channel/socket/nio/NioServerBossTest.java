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

import static org.junit.Assert.assertEquals;

import java.nio.channels.Selector;
import java.util.concurrent.Executor;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

import org.kaazing.mina.netty.config.InternalSystemProperty;


public class NioServerBossTest {

    @Test
    public void MaximumProcessTaskQueueNanos_shouldDefaultToZero() throws Exception {
        assertGetMaximumProcessTaskQueueNanosReturns(0);
    }

    @Test
    public void MaximumProcessTaskQueueNanos_shouldAlwaysReportZero() throws Exception {
        System.setProperty(InternalSystemProperty.MAXIMUM_PROCESS_TASKS_TIME.getPropertyName(), "20");
        try {
            assertGetMaximumProcessTaskQueueNanosReturns(0);
        }
        finally {
            System.clearProperty(InternalSystemProperty.MAXIMUM_PROCESS_TASKS_TIME.getPropertyName());
        }
    }

    private void assertGetMaximumProcessTaskQueueNanosReturns(long expectedValue) throws Exception {
        Mockery context = new Mockery();
        final Executor executor = context.mock(Executor.class);

        context.checking(new Expectations() {
            {
                oneOf(executor).execute(with(any(Runnable.class)));
            }
        });

        NioServerBoss boss = new NioServerBoss(executor);
        assertEquals(expectedValue, boss.getMaximumProcessTaskQueueTimeNanos());
        context.assertIsSatisfied();
    }

    @Test
    public void normalSelectShouldBeBlocking() throws Exception {
        assertSelectIsBlocking(false);
    }

    @Test
    public void quickSelectShouldBeBlocking() throws Exception {
        assertSelectIsBlocking(true);
    }

    @Test
    public void quickSelectShouldBeBlockingEvenWithSystemPropertySet() throws Exception {
        System.setProperty(InternalSystemProperty.QUICK_SELECT_TIMEOUT.getPropertyName(), "2");
        try {
            assertSelectIsBlocking(true);
        }
        finally {
            System.clearProperty(InternalSystemProperty.QUICK_SELECT_TIMEOUT.getPropertyName());
        }
    }

    private void assertSelectIsBlocking(boolean quickSelect) throws Exception {
        Mockery context = new Mockery();
        context.setImposteriser(ClassImposteriser.INSTANCE);
        final Executor executor = context.mock(Executor.class);
        final Selector selector = context.mock(Selector.class);

        context.checking(new Expectations() {
            {
                oneOf(executor).execute(with(any(Runnable.class)));
                oneOf(selector).select();
                will(returnValue(0));
            }
        });

        NioServerBoss boss = new NioServerBoss(executor);
        boss.select(selector, quickSelect);
        context.assertIsSatisfied();
    }

}
