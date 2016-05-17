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

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.nio.InvalidMarkException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

public abstract class AbstractIoBufferExTest<A extends IoBufferAllocatorEx<B>, B extends IoBufferEx> {
    private static final byte[] DATA = "abcdefghijk".getBytes();
    private A allocator;
    private int flags;
    private String bufDescription;

    protected AbstractIoBufferExTest(A allocator, int flags, String bufDescription) {
        this.allocator = allocator;
        this.flags = flags;
        this.bufDescription = bufDescription + " buffer";
    }

    protected static Collection<Object[]> createInputValues(IoBufferAllocatorEx<?> allocator) {
    return Arrays.asList(new Object[][] {
            {IoBufferEx.FLAG_SHARED, "shared"},
            {IoBufferEx.FLAG_SHARED + IoBufferEx.FLAG_DIRECT, "shared, direct"},
            {IoBufferEx.FLAG_SHARED + IoBufferEx.FLAG_ZERO_COPY, "shared, zero-copy"},
            {IoBufferEx.FLAG_NONE, "unshared"},
            {IoBufferEx.FLAG_NONE + IoBufferEx.FLAG_DIRECT, "unshared, direct"},
            {IoBufferEx.FLAG_NONE + IoBufferEx.FLAG_ZERO_COPY, "unshared, zero-copy"}
        });
    }

    @Test
    public void asReadOnlyBufferShouldPropagateMark() throws Exception {
        IoBufferEx buf = allocator.wrap(ByteBuffer.wrap(DATA), flags);
        buf.position(3);
        buf.mark();
        IoBufferEx result = buf.asReadOnlyBuffer();
        assertEquals(bufDescription, 3, result.markValue());
    }

    @Test
    public void asReadOnlyBufferOfUnmarkedBufferShouldBeUnmarked() throws Exception {
        IoBufferEx buf = allocator.wrap(ByteBuffer.wrap(DATA), flags);
        IoBufferEx result = buf.asReadOnlyBuffer();
        assertEquals(bufDescription, -1, result.markValue());
    }

    @Test
    public void asUnsharedBufferShouldPropagateMark() throws Exception {
        IoBufferEx buf = allocator.wrap(ByteBuffer.wrap(DATA), flags);
        buf.position(3);
        buf.mark();
        IoBufferEx result = buf.asUnsharedBuffer();
        assertEquals(bufDescription, 3, result.markValue());
    }

    @Test
    public void asUnsharedBufferOfUnmarkedBufferShouldBeUnmarked() throws Exception {
        IoBufferEx buf = allocator.wrap(ByteBuffer.wrap(DATA), flags);
        IoBufferEx result = buf.asUnsharedBuffer();
        assertEquals(bufDescription, -1, result.markValue());
    }

    @Test
    public void duplicateShouldPropagateMark() throws Exception {
        IoBufferEx buf = allocator.wrap(ByteBuffer.wrap(DATA), flags);
        buf.position(3);
        buf.mark();
        IoBufferEx result = buf.duplicate();
        assertEquals(bufDescription, 3, result.markValue());
    }

    @Test
    public void duplicateOfUnmarkedBufferShouldBeUnmarked() throws Exception {
        IoBufferEx buf = allocator.wrap(ByteBuffer.wrap(DATA), flags);
        IoBufferEx result = buf.duplicate();
        assertEquals(bufDescription, -1, result.markValue());
    }

    @Test(expected = InvalidMarkException.class)
    public void markShouldNotSetMarkOnUnderlyingByteBuffer() throws Exception {
        IoBufferEx buf = allocator.wrap(ByteBuffer.wrap(DATA), flags);
        buf.position(7);
        buf.mark();
        buf.buf().reset();
    }

    @Test
    public void resetShouldResetPositionToMarkedPosition() {
        IoBufferEx buf = allocator.wrap(ByteBuffer.wrap(DATA), flags);
        buf.position(3);
        buf.mark();
        buf.position(7);
        buf.reset();
        assertEquals(bufDescription, 3, buf.position());
    }

    @Test(expected = InvalidMarkException.class)
    public void resetWithoutMarkShouldThrowException() {
        IoBufferEx buf = allocator.wrap(ByteBuffer.wrap(DATA), flags);
        buf.reset();
    }

    @Test
    public void sliceShouldNotPropagateMark() throws Exception {
        IoBufferEx buf = allocator.wrap(ByteBuffer.wrap(DATA), flags);
        buf.position(3);
        buf.mark();
        IoBufferEx result = buf.slice();
        assertEquals(bufDescription, -1, result.markValue());
    }

    @Test
    public void sliceOfUnmarkedBufferShouldBeUnmarked() throws Exception {
        IoBufferEx buf = allocator.wrap(ByteBuffer.wrap(DATA), flags);
        IoBufferEx result = buf.slice();
        assertEquals(bufDescription, -1, result.markValue());
    }

}
