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
package org.kaazing.test.util;

import static java.lang.reflect.Array.getLength;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.Map;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public final class Assert {

    private Assert() {
        // utility only
    }

    public static <T extends Map<?, ?>> void assertEmpty(T map) {
        assertThat(map, new IsMapEmpty<>());
    }

    public static <T extends Collection<?>> void assertEmpty(T collection) {
        assertThat(collection, new IsCollectionEmpty<>());
    }

    public static void assertEmpty(Object array) {
        assertThat(array, new IsArrayEmpty());
    }

    private static final class IsMapEmpty<T extends Map<?, ?>> extends BaseMatcher<T> {
        @Override
        public void describeTo(Description description) {
            description.appendText("is empty");
        }

        @Override
        @SuppressWarnings("rawtypes")
        public boolean matches(Object that) {
            return (that != null) && ((Map) that).isEmpty();
        }
    }

    private static final class IsCollectionEmpty<T extends Collection<?>> extends BaseMatcher<T> {
        @Override
        public void describeTo(Description description) {
            description.appendText("is empty");
        }

        @Override
        @SuppressWarnings("rawtypes")
        public boolean matches(Object that) {
            return (that != null) && ((Collection) that).isEmpty();
        }
    }

    private static final class IsArrayEmpty extends BaseMatcher<Object> {
        @Override
        public void describeTo(Description description) {
            description.appendText("is empty");
        }

        @Override
        public boolean matches(Object that) {
            return (that != null) && (getLength(that) == 0);
        }
    }
}
