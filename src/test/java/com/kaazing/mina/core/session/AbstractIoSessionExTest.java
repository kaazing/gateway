/**
 * Copyright (c) 2007-2014, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.session;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.SocketAddress;
import java.security.Principal;

import javax.security.auth.Subject;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.TransportMetadata;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

import com.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import com.kaazing.mina.core.service.IoProcessorEx;
import com.kaazing.mina.core.service.IoServiceEx;
import com.kaazing.mina.core.write.DefaultWriteRequestEx.ShareableWriteRequest;

public class AbstractIoSessionExTest {

    @Test
    public void addedSubjectChangeListenerShouldBeFired() throws Exception {
        Mockery context = new Mockery();
        final SubjectChangeListener listener = context.mock(SubjectChangeListener.class);

        context.checking(new Expectations() { {
            oneOf(listener).subjectChanged(with(any(Subject.class)));
        } });

        TestAbstractIoSessionEx session = new TestAbstractIoSessionEx();
        session.addSubjectChangeListener(listener);
        session.setSubject(new Subject());

        context.assertIsSatisfied();
    }

    @Test
    public void removedSubjectChangeListenerShouldNotBeFired() throws Exception {
        Mockery context = new Mockery();
        final SubjectChangeListener listener1 = context.mock(SubjectChangeListener.class, "listener1");
        final SubjectChangeListener listener2 = context.mock(SubjectChangeListener.class, "listener2");
        final SubjectChangeListener listener3 = context.mock(SubjectChangeListener.class, "listener3");
        final SubjectChangeListener listener4 = context.mock(SubjectChangeListener.class, "listener4");

        context.checking(new Expectations() { {
            oneOf(listener1).subjectChanged(with(any(Subject.class)));
            oneOf(listener2).subjectChanged(with(any(Subject.class)));
            oneOf(listener1).subjectChanged(null);
            oneOf(listener3).subjectChanged(null);

            never(listener4).subjectChanged(with(any(Subject.class)));
        } });

        TestAbstractIoSessionEx session = new TestAbstractIoSessionEx();
        session.addSubjectChangeListener(listener1);
        session.addSubjectChangeListener(listener2);
        session.setSubject(new Subject());
        session.removeSubjectChangeListener(listener2);
        session.addSubjectChangeListener(listener3);
        session.setSubject(null);
        session.addSubjectChangeListener(listener4);

        context.assertIsSatisfied();
    }

    @Test
    public void setSubjectNullWhenAlreadyNullShouldNotFireListeners() throws Exception {
        Mockery context = new Mockery();
        final SubjectChangeListener listener = context.mock(SubjectChangeListener.class);

        TestAbstractIoSessionEx session = new TestAbstractIoSessionEx();
        session.addSubjectChangeListener(listener);
        session.setSubject(null);
        session.setSubject(null);

        context.assertIsSatisfied();
    }

    @Test
    public void setSubjectNullWhenNotNullShouldFireListeners() throws Exception {
        Mockery context = new Mockery();
        final SubjectChangeListener listener = context.mock(SubjectChangeListener.class);

        context.checking(new Expectations() { {
            oneOf(listener).subjectChanged(with(any(Subject.class)));
            oneOf(listener).subjectChanged(null);
        } });

        TestAbstractIoSessionEx session = new TestAbstractIoSessionEx();
        session.addSubjectChangeListener(listener);
        session.setSubject(new Subject());
        session.setSubject(null);

        context.assertIsSatisfied();
    }

    @Test
    public void setSubjectDifferentFromPreviousShouldFireListeners() throws Exception {
        Mockery context = new Mockery();
        final SubjectChangeListener listener = context.mock(SubjectChangeListener.class);
        final Subject subject1 = new Subject();
        final Subject subject2 = new Subject();
        subject2.getPrincipals().add(new Principal() {
            @Override
            public String getName() {
                // TODO Auto-generated method stub
                return null;
            }
        });

        context.checking(new Expectations() { {
            oneOf(listener).subjectChanged(subject1);
            oneOf(listener).subjectChanged(subject2);
        } });

        TestAbstractIoSessionEx session = new TestAbstractIoSessionEx();
        session.addSubjectChangeListener(listener);
        session.setSubject(subject1);
        assertFalse(subject2.equals(subject1));
        session.setSubject(subject2);

        context.assertIsSatisfied();
    }

    @Test
    public void setSubjectEqualToPreviousShouldFireListeners() throws Exception {
        Mockery context = new Mockery();
        final SubjectChangeListener listener = context.mock(SubjectChangeListener.class);

        final Subject subject1 = new Subject();
        final Subject subject2 = new Subject();
        context.checking(new Expectations() { {
            oneOf(listener).subjectChanged(with(subject1));
            oneOf(listener).subjectChanged(with(subject2));
        } });

        TestAbstractIoSessionEx session = new TestAbstractIoSessionEx();
        session.addSubjectChangeListener(listener);
        session.setSubject(subject1);
        assertTrue(subject2.equals(subject1));
        session.setSubject(subject2);

        context.assertIsSatisfied();
    }

    static class TestAbstractIoSessionEx extends AbstractIoSessionEx {

        protected TestAbstractIoSessionEx() {
            super(0, CURRENT_THREAD, IMMEDIATE_EXECUTOR, new ShareableWriteRequest());
        }

        @Override
        public IoBufferAllocatorEx<?> getBufferAllocator() {
            return null;
        }

        @Override
        public IoSessionConfigEx getConfig() {
            return null;
        }

        @Override
        public IoServiceEx getService() {
            return null;
        }

        @Override
        public IoHandler getHandler() {
            return null;
        }

        @Override
        public TransportMetadata getTransportMetadata() {
            return null;
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return null;
        }

        @Override
        public SocketAddress getLocalAddress() {
            return null;
        }

        @Override
        public IoProcessorEx<?> getProcessor() {
            return null;
        }

    };

}
