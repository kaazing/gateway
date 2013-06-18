/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.buffer;

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
