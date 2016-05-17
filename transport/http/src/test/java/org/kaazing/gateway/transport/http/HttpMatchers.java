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
package org.kaazing.gateway.transport.http;

import static java.lang.String.format;
import static org.jmock.Expectations.equal;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class HttpMatchers {

    public static <T extends HttpAcceptSession>  Matcher<T> hasMethod(HttpMethod method) {
        return new HasMethod<>(method);
    }

    public static <T extends HttpAcceptSession>  Matcher<T> hasMethod(Matcher<HttpMethod> matcher) {
        return new HasMethod<>(matcher);
    }

    public static <T extends HttpSession>  Matcher<T> hasReadHeader(String headerName, String headerValue) {
        return new HasReadHeader<>(headerName, headerValue);
    }

    public static <T extends HttpSession>  Matcher<T> hasReadHeader(String headerName, Matcher<String> headerValue) {
        return new HasReadHeader<>(headerName, headerValue);
    }

    private HttpMatchers() {
        // utility, no instances
    }

    private static final class HasMethod<T extends HttpAcceptSession> extends BaseMatcher<T> {
        private final Matcher<HttpMethod> matcher;

        private HasMethod(HttpMethod method) {
            this(equal(method));
        }

        private HasMethod(Matcher<HttpMethod> matcher) {
            this.matcher = matcher;
        }
        
        @Override
        public boolean matches(Object item) {
            HttpAcceptSession httpSession = (HttpAcceptSession) item;
            return (matcher.matches(httpSession.getMethod()));
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("has method ");
            matcher.describeTo(description);
        }
    }

    private static final class HasReadHeader<T extends HttpSession> extends BaseMatcher<T> {
        private final String headerName;
        private final Matcher<String> headerValue;

        private HasReadHeader(String headerName, String headerValue) {
            this(headerName, equal(headerValue));
        }

        private HasReadHeader(String headerName, Matcher<String> headerValue) {
            this.headerName = headerName;
            this.headerValue = headerValue;
        }
        
        @Override
        public boolean matches(Object item) {
            HttpSession httpSession = (HttpSession) item;
            return (headerValue.matches(httpSession.getReadHeader(headerName)));
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(format("has \"%s\" read header ", headerName));
            headerValue.describeTo(description);
        }
    }

}
