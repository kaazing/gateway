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
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

import org.kaazing.mina.netty.config.InternalSystemProperty;


public class NioWorkerTest {

    @Test
    public void MaximumProcessTaskQueueNanos_shouldDefaultToZero() throws Exception {
        assertGetMaximumProcessTaskQueueNanosReturns(0);
    }

    @Test
    public void MaximumProcessTaskQueueNanos_shouldBeDerivedFromSystemProperty() throws Exception {
        System.setProperty(InternalSystemProperty.MAXIMUM_PROCESS_TASKS_TIME.getPropertyName(), "20");
        try {
            assertGetMaximumProcessTaskQueueNanosReturns(20 * 1000 * 1000);
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

        NioWorker worker = new NioWorker(executor);
        assertEquals(expectedValue, worker.getMaximumProcessTaskQueueTimeNanos());
        context.assertIsSatisfied();
    }

    @Test
    public void normalSelectTimeoutShouldDefaultToNettyDefaultValue() throws Exception {
        assertSelectTimeout(false, SelectorUtil.DEFAULT_SELECT_TIMEOUT);
    }

    @Test
    public void quickSelectTimeoutShouldDefaultToSelectNow() throws Exception {
        assertQuickSelectDoesSelectNow();
    }

    @Test
    public void quickSelectTimeoutShouldBeDerivedFromSystemProperty() throws Exception {
        System.setProperty(InternalSystemProperty.QUICK_SELECT_TIMEOUT.getPropertyName(), "2");
        try {
            assertSelectTimeout(true, 2);
        }
        finally {
            System.clearProperty(InternalSystemProperty.QUICK_SELECT_TIMEOUT.getPropertyName());
        }
    }

    private void assertSelectTimeout(boolean quickSelect, long expectedValue) throws Exception {
        Mockery context = new Mockery();
        context.setImposteriser(ClassImposteriser.INSTANCE);
        final Executor executor = context.mock(Executor.class);
        final Selector selector = context.mock(Selector.class);
        final long[] timeout = new long[1];

        context.checking(new Expectations() {
            {
                oneOf(executor).execute(with(any(Runnable.class)));
                oneOf(selector).select(with(any(Long.class)));
                will(new CustomAction("saveTimeout") {
                    @Override
                    public Object invoke(Invocation arg0) throws Throwable {
                        timeout[0] = (Long) arg0.getParameter(0);
                        return 0;
                    }
                });
            }
        });

        NioWorker worker = new NioWorker(executor);
        worker.select(selector, quickSelect);
        assertEquals(expectedValue, timeout[0]);
        context.assertIsSatisfied();
    }

    private void assertQuickSelectDoesSelectNow() throws Exception {
        Mockery context = new Mockery();
        context.setImposteriser(ClassImposteriser.INSTANCE);
        final Executor executor = context.mock(Executor.class);
        final Selector selector = context.mock(Selector.class);

        context.checking(new Expectations() {
            {
                oneOf(executor).execute(with(any(Runnable.class)));
                oneOf(selector).selectNow();
                will(returnValue(0));
            }
        });

        NioWorker worker = new NioWorker(executor);
        worker.select(selector, true);
        context.assertIsSatisfied();
    }
}
