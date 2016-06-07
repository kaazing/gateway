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
package org.kaazing.mina.core.buffer;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SimpleBufferTest extends AbstractIoBufferExTest<SimpleBufferAllocator, SimpleBufferAllocator.SimpleBuffer> {
    private static final SimpleBufferAllocator allocator = SimpleBufferAllocator.BUFFER_ALLOCATOR;

    @Parameters
    public static Collection<Object[]> createInputValues() {
        return AbstractIoBufferExTest.createInputValues(allocator);
    }

    public SimpleBufferTest(int flags, String bufDescription) {
        super(allocator, flags, bufDescription);
    }

}
