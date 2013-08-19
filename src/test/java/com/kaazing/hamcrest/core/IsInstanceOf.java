/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.hamcrest.core;

import static java.lang.String.format;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class IsInstanceOf<T> extends BaseMatcher<T> {

    private final Class<T> instanceType;

    public IsInstanceOf(Class<T> instanceType) {
        this.instanceType = instanceType;
    }

    @Override
    public boolean matches(Object object) {
        return instanceType.isInstance(object);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(format("instance of %s", instanceType.getName()));
    }

}
