/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.junit.matchers;

import org.hamcrest.Matcher;

import com.kaazing.hamcrest.core.IsInstanceOf;

public class JUnitMatchers extends org.junit.matchers.JUnitMatchers {

    public static <T> Matcher<T> instanceOf(Class<T> type) {
        return new IsInstanceOf<T>(type);
    }

}
