/**
 * Copyright (c) 2007-2014, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.session;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

public class SubjectChangeListenerTest {

    @Test
    public void addedSubjectChangeListener() throws Exception {
        Mockery context = new Mockery();
        final SubjectChangeListener listener = context.mock(SubjectChangeListener.class);

        context.checking(new Expectations() { {
            oneOf(listener).subjectChanged(with(any(Subject.class)));
        } });

        DummySessionEx session = new DummySessionEx();
        session.addSubjectChangeListener(listener);
        session.setSubject(new Subject());
    }

    @Test
    public void removedSubjectChangeListener() throws Exception {
        Mockery context = new Mockery();
        final SubjectChangeListener listener1 = context.mock(SubjectChangeListener.class);
        final SubjectChangeListener listener2 = context.mock(SubjectChangeListener.class, "listener2");
        final SubjectChangeListener listener3 = context.mock(SubjectChangeListener.class, "listener3");
        final SubjectChangeListener listener4 = context.mock(SubjectChangeListener.class, "listener4");

        context.checking(new Expectations() { {
            exactly(2).of(listener1).subjectChanged(with(any(Subject.class)));
            exactly(1).of(listener2).subjectChanged(with(any(Subject.class)));
            exactly(1).of(listener3).subjectChanged(with(any(Subject.class)));
            never(listener4).subjectChanged(with(any(Subject.class)));
        } });

        DummySessionEx session = new DummySessionEx();
        session.addSubjectChangeListener(listener1);
        session.addSubjectChangeListener(listener2);
        session.setSubject(new Subject());
        session.removeSubjectChangeListener(listener2);
        session.addSubjectChangeListener(listener3);
        session.setSubject(new Subject());
        session.addSubjectChangeListener(listener4);
    }

}
