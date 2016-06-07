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
package org.kaazing.gateway.server.test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsEqual;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.kaazing.gateway.transport.TypedAttributeKey;
import static java.lang.String.format;

public class Expectations extends org.jmock.Expectations {

    private Map<String, Object> variables = new HashMap<>();

    public Matcher<AttributeKey> attributeKeyMatching(String regex) {
        return new AttributeKeyMatching(regex);
    }

    public Matcher<ByteBuffer> byteBufferMatching(String regex) {
        return new ByteBufferMatching(regex);
    }

    public Matcher<Long> integerBetween(final long min, final long max) {
        return new BaseMatcher<Long>() {
            @Override
            public boolean matches(Object arg0) {
                long value = (Long) arg0;
                return value <= max && value >= min;
            }

            @Override
            public void describeTo(Description arg0) {
                arg0.appendText("integer between or equal to " + min + " and " + max);
            }
        };
    }

    public Matcher<String> stringMatching(final String regex) {
        return new BaseMatcher<String>() {
            @Override
            public boolean matches(Object arg0) {
                String value = (String) arg0;
                return value.matches(regex);
            }

            @Override
            public void describeTo(Description arg0) {
                arg0.appendText("string matches regular expression " + regex);
            }
        };
    }

    public Matcher<List<String>> stringListMatching(final String... contents) {
        return new BaseMatcher<List<String>>() {
            @Override
            public boolean matches(Object arg0) {
                List<String> value = (List<String>) arg0;
                return value.equals(Arrays.asList(contents));
            }

            @Override
            public void describeTo(Description arg0) {
                StringBuilder b = new StringBuilder();
                if (contents != null) {
                    b.append("[");
                    for (String e : contents) {
                        b.append(e);
                        b.append(',');
                    }
                    b.replace(b.length() - 1, b.length(), "]");
                }
                arg0.appendText("list of string matches contents " + b.toString());
            }
        };
    }


    public Matcher<TypedAttributeKey<?>> typedAttributeKeyMatching(String regex) {
        return new TypedAttributeKeyMatching(regex);
    }

    public <T> Matcher<T> variable(String variableName, Class<T> clazz) {
        return new VariableMatcher<>(variableName, clazz, this);
    }

    public Matcher<WriteRequest> hasMessage(Object message) {
        return new HasMessage(message);
    }

    public Matcher<WriteRequest> hasMessage(Matcher<Object> message) {
        return new HasMessage(message);
    }

    public Matcher<IoBuffer> hasRemaining(final int remaining) {
        return new BaseMatcher<IoBuffer>() {

            @Override
            public boolean matches(Object item) {
                IoBuffer buf = (IoBuffer) item;
                return (buf.remaining() == remaining);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(format("buffer has %d remaining bytes", remaining));
            }
        };
    }

    public Matcher<WriteRequest> writeRequestWithMessage(final Object message) {
        return new BaseMatcher<WriteRequest>() {
            @Override
            public boolean matches(Object arg0) {
                WriteRequest request = (WriteRequest) arg0;
                return message.equals(request.getMessage());
            }

            @Override
            public void describeTo(Description arg0) {
                arg0.appendText("write request containing a message equal to " + message);
            }
        };
    }

    public CustomAction setSessionClosed(IoSession session) {
        return new SetIoSessionClosed(session);
    }

    public CustomAction readBytes(final byte[] srcBytes) {
        return new ReadBytes("read bytes", srcBytes);
    }

    public Object lookup(String variableName) {
        return variables.get(variableName);
    }

    public <T> T lookup(String variableName, Class<T> clazz) {
        return clazz.cast(variables.get(variableName));
    }

    public Action saveParameter(final String variableName, final int parameterIndex) {
        return new CustomAction("save parameter") {

            @Override
            public Object invoke(Invocation invocation) throws Throwable {
                variables.put(variableName, invocation.getParameter(parameterIndex));
                return null;
            }
        };
    }

    public Action saveParameter(final Object[] parameterStorage, final int parameterIndex) {
        return new CustomAction("save parameter") {

            @Override
            public Object invoke(Invocation invocation) throws Throwable {
                parameterStorage[0] = invocation.getParameter(parameterIndex);
                return null;
            }
        };
    }

    public Action returnVariable(final String variableName) {
        return new CustomAction("return variable") {

            @Override
            public Object invoke(Invocation invocation) throws Throwable {
                return variables.get(variableName);
            }
        };
    }

    private static final class HasMessage extends BaseMatcher<WriteRequest> {

        private final Matcher<Object> message;

        private HasMessage(Object message) {
            this(new IsEqual<>(message));
        }

        private HasMessage(Matcher<Object> message) {
            this.message = message;
        }

        @Override
        public boolean matches(Object arg) {
            return (arg instanceof WriteRequest) &&
                    (message.matches(((WriteRequest) arg).getMessage()));
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("has message ").appendValue(message);
        }

    }

}

