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
package org.kaazing.gateway.util.der;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.junit.Test;

public class DerIdTest {
    @Test(expected = IllegalArgumentException.class)
    public void testNull() {
        DerId.decode(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmpty() {
        DerId.decode(ByteBuffer.allocate(0));
    }

    @Test
    public void testSingleByte1() {
        DerId id = DerId.decode(ByteBuffer.wrap(new byte[] { (byte) 0x02 }));
        assertEquals(DerId.TagClass.UNIVERSAL, id.getTagClass());
        assertEquals(DerId.EncodingType.PRIMITIVE, id.getEncodingType());
        assertEquals(2, id.getTagNumber());
    }

    @Test
    public void testSingleByte2() {
        DerId id = DerId.decode(ByteBuffer.wrap(new byte[] { (byte) 0x6a }));
        assertEquals(DerId.TagClass.APPLICATION, id.getTagClass());
        assertEquals(DerId.EncodingType.CONSTRUCTED, id.getEncodingType());
        assertEquals(10, id.getTagNumber());
    }

    @Test
    public void testSingleByte3() {
        DerId id = DerId.decode(ByteBuffer.wrap(new byte[] { (byte) 0xa1 }));
        assertEquals(DerId.TagClass.CONTEXT_SPECIFIC, id.getTagClass());
        assertEquals(DerId.EncodingType.CONSTRUCTED, id.getEncodingType());
        assertEquals(1, id.getTagNumber());
    }

    @Test
    public void testDoubleByte() {
        DerId id = DerId.decode(ByteBuffer.wrap(new byte[] { (byte) 0xdf, (byte) 0x28 }));
        assertEquals(DerId.TagClass.PRIVATE, id.getTagClass());
        assertEquals(DerId.EncodingType.PRIMITIVE, id.getEncodingType());
        assertEquals(40, id.getTagNumber());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDoubleByteWithInsufficientData() {
        DerId.decode(ByteBuffer.wrap(new byte[] { (byte) 0xdf }));
    }

    @Test
    public void testTripleByte() {
        DerId id = DerId.decode(ByteBuffer.wrap(new byte[] { (byte) 0x3f, (byte) 0xa8, (byte) 0x35 }));
        assertEquals(DerId.TagClass.UNIVERSAL, id.getTagClass());
        assertEquals(DerId.EncodingType.CONSTRUCTED, id.getEncodingType());
        assertEquals(5173, id.getTagNumber());
    }
}
