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

import java.lang.reflect.Method;
import org.jmock.api.Invocation;
import org.jmock.api.Invokable;

final class SingleThreadedPolicy extends org.jmock.internal.SingleThreadedPolicy {

    private static final Method METHOD_OBJECT_FINALIZE;

    static {
        try {
            METHOD_OBJECT_FINALIZE = Object.class.getDeclaredMethod("finalize");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Invokable synchroniseAccessTo(final Invokable mockObject) {

        final Invokable invokable = super.synchroniseAccessTo(mockObject);
        return new Invokable() {

            @Override
            public Object invoke(Invocation invocation) throws Throwable {
                Method invokedMethod = invocation.getInvokedMethod();
                if (METHOD_OBJECT_FINALIZE.equals(invokedMethod)) {
                    return mockObject.invoke(invocation);
                }

                return invokable.invoke(invocation);
            }
        };
    }
}
