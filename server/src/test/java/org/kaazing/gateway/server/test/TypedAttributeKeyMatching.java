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
/**
 *
 */
package org.kaazing.gateway.server.test;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.kaazing.gateway.transport.TypedAttributeKey;

final class TypedAttributeKeyMatching extends BaseMatcher<TypedAttributeKey<?>> {

    private final String pattern;

    public TypedAttributeKeyMatching(String regex) {
        this.pattern = regex;
    }

    @Override
    public boolean matches(Object arg) {
        return (arg instanceof TypedAttributeKey) &&
                arg.toString().matches(pattern);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("matches regular expression ").appendValue(pattern);
    }

}
