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
package org.kaazing.mina.core.service;

import static java.lang.Thread.currentThread;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

import org.kaazing.mina.core.session.IoSessionEx;

public class AbstractIoProcessorTest {
    private static final Thread TEST_THREAD = new Thread();

    @Test
    public void add_shouldExecuteImmediatelyIfInIoThread() throws Exception {
        Mockery context = new Mockery();
        final IoSessionEx session = context.mock(IoSessionEx.class);

        context.checking(new Expectations() { {
            oneOf(session).isIoAligned(); will(returnValue(true));
            oneOf(session).getIoThread(); will(returnValue(currentThread()));
        } });

        AbstractIoProcessor<IoSessionEx> processor = new TestIoProcessor<>();
        processor.add(session);
        context.assertIsSatisfied();
        assertEquals("add0", ((TestIoProcessor<?>) processor).called);
    }

    @Test(expected = RuntimeException.class)
    public void add_shouldThrowExceptionIfNotInIoThread() throws Exception {
        Mockery context = new Mockery();
        final IoSessionEx session = context.mock(IoSessionEx.class);

        context.checking(new Expectations() { {
            oneOf(session).isIoAligned(); will(returnValue(true));
            oneOf(session).getIoThread(); will(returnValue(TEST_THREAD));
        } });

        AbstractIoProcessor<IoSessionEx> processor = new TestIoProcessor<>();
        processor.add(session);
        context.assertIsSatisfied();
        assertNull(((TestIoProcessor<?>) processor).called);
    }

    @Test
    public void flush_shouldExecuteImmediatelyIfInIoThread() throws Exception {
        Mockery context = new Mockery();
        final IoSessionEx session = context.mock(IoSessionEx.class);

        context.checking(new Expectations() { {
            oneOf(session).isIoAligned(); will(returnValue(true));
            oneOf(session).getIoThread(); will(returnValue(currentThread()));
        } });

        AbstractIoProcessor<IoSessionEx> processor = new TestIoProcessor<>();
        processor.flush(session);
        context.assertIsSatisfied();
        assertEquals("flush0", ((TestIoProcessor<?>) processor).called);
    }

    @Test(expected = RuntimeException.class)
    public void flush_shouldThrowExceptionIfNotInIoThread() throws Exception {
        Mockery context = new Mockery();
        final IoSessionEx session = context.mock(IoSessionEx.class);

        context.checking(new Expectations() { {
            oneOf(session).isIoAligned(); will(returnValue(true));
            oneOf(session).getIoThread(); will(returnValue(TEST_THREAD));
        } });

        AbstractIoProcessor<IoSessionEx> processor = new TestIoProcessor<>();
        processor.flush(session);
        context.assertIsSatisfied();
        assertNull(((TestIoProcessor<?>) processor).called);
    }

    @Test
    public void remove_shouldExecuteImmediatelyIfInIoThread() throws Exception {
        Mockery context = new Mockery();
        final IoSessionEx session = context.mock(IoSessionEx.class);

        context.checking(new Expectations() { {
            oneOf(session).isIoAligned(); will(returnValue(true));
            oneOf(session).getIoThread(); will(returnValue(currentThread()));
        } });

        AbstractIoProcessor<IoSessionEx> processor = new TestIoProcessor<>();
        processor.remove(session);
        context.assertIsSatisfied();
        assertEquals("remove0", ((TestIoProcessor<?>) processor).called);
    }

    @Test(expected = RuntimeException.class)
    public void remove_shouldThrowExceptionIfNotInIoThread() throws Exception {
        Mockery context = new Mockery();
        final IoSessionEx session = context.mock(IoSessionEx.class);

        context.checking(new Expectations() { {
            oneOf(session).isIoAligned(); will(returnValue(true));
            oneOf(session).getIoThread(); will(returnValue(TEST_THREAD));
        } });

        AbstractIoProcessor<IoSessionEx> processor = new TestIoProcessor<>();
        processor.remove(session);
        context.assertIsSatisfied();
        assertNull(((TestIoProcessor<?>) processor).called);
    }

    @Test
    public void updateTrafficControl_shouldExecuteImmediatelyIfInIoThread() throws Exception {
        Mockery context = new Mockery();
        final IoSessionEx session = context.mock(IoSessionEx.class);

        context.checking(new Expectations() { {
            oneOf(session).isIoAligned(); will(returnValue(true));
            oneOf(session).getIoThread(); will(returnValue(currentThread()));
        } });

        AbstractIoProcessor<IoSessionEx> processor = new TestIoProcessor<>();
        processor.updateTrafficControl(session);
        context.assertIsSatisfied();
        assertEquals("updateTrafficControl0", ((TestIoProcessor<?>) processor).called);
    }

    @Test(expected = RuntimeException.class)
    public void updateTrafficControl_shouldThrowExceptionIfNotInIoThread() throws Exception {
        Mockery context = new Mockery();
        final IoSessionEx session = context.mock(IoSessionEx.class);

        context.checking(new Expectations() { {
            oneOf(session).isIoAligned(); will(returnValue(true));
            oneOf(session).getIoThread(); will(returnValue(TEST_THREAD));
        } });

        AbstractIoProcessor<IoSessionEx> processor = new TestIoProcessor<>();
        processor.updateTrafficControl(session);
        context.assertIsSatisfied();
        assertNull(((TestIoProcessor<?>) processor).called);
    }

    private static class TestIoProcessor<T extends IoSessionEx> extends AbstractIoProcessor<IoSessionEx> {
        String called;

        @Override
        public boolean isDisposing() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isDisposed() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void dispose() {
            // TODO Auto-generated method stub
        }

        @Override
        protected void add0(IoSessionEx session) {
            called = "add0";
        }

        @Override
        protected void flush0(IoSessionEx session) {
            called = "flush0";
        }

        @Override
        protected void updateTrafficControl0(IoSessionEx session) {
            called = "updateTrafficControl0";
        }

        @Override
        protected void remove0(IoSessionEx session) {
            called = "remove0";
        }

    }

}
