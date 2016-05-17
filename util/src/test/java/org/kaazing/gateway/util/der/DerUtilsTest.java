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

public class DerUtilsTest {
    // ----- BEGIN: Decode length tests -----

    @Test(expected = IllegalArgumentException.class)
    public void testLengthNull() {
        DerUtils.decodeLength(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLengthEmpty() {
        DerUtils.decodeLength(ByteBuffer.allocate(0));
    }

    @Test
    public void testLengthShortForm() {
        int len = DerUtils.decodeLength(ByteBuffer.wrap(new byte[] { 0x26 }));
        assertEquals(38, len);
    }

    @Test
    public void testLengthLongForm() {
        int len = DerUtils.decodeLength(ByteBuffer.wrap(new byte[] { (byte) 0x81, (byte) 0xc9 }));
        assertEquals(201, len);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLengthLongFormInvalid() {
        DerUtils.decodeLength(ByteBuffer.wrap(new byte[] { (byte) 0x81 }));
    }

    // ----- END: Decode length tests -----

    // ----- BEGIN: Compute size tests -----

    @Test(expected = IllegalArgumentException.class)
    public void testSizeOfInvalidTagNumber() {
        DerUtils.sizeOf(-1, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSizeOfInvalidContentLength() {
        DerUtils.sizeOf(0, -1);
    }

    @Test
    public void testSizeOf() {
        assertEquals(2, DerUtils.sizeOf(29, 0));
        assertEquals(2, DerUtils.sizeOf(30, 0));
        assertEquals(3, DerUtils.sizeOf(31, 0));
        assertEquals(4, DerUtils.sizeOf(512, 0));
        assertEquals(5, DerUtils.sizeOf(65535, 0));
        assertEquals(129, DerUtils.sizeOf(29, 127));
        assertEquals(131, DerUtils.sizeOf(29, 128));
        assertEquals(260, DerUtils.sizeOf(29, 256));
        assertEquals(65539, DerUtils.sizeOf(29, 65535));
        assertEquals(65541, DerUtils.sizeOf(29, 65536));
    }

    // ----- END: Compute size tests -----

    // ----- BEGIN: Encode tests -----

    @Test(expected = IllegalArgumentException.class)
    public void testEncodeIllegalTagNumber() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[12]);
        buf.position(buf.position() + 2);
        DerUtils.encodeIdAndLength(DerId.TagClass.UNIVERSAL, DerId.EncodingType.PRIMITIVE, -1, 10, buf);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEncodeIllegalLength() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[12]);
        buf.position(buf.position() + 2);
        DerUtils.encodeIdAndLength(DerId.TagClass.UNIVERSAL, DerId.EncodingType.PRIMITIVE, 2, -10, buf);
    }

    @Test
    public void testEncode1() {
        // universal, primitive, tag <= 30, short length
        ByteBuffer buf = ByteBuffer.wrap(new byte[12]);
        buf.position(buf.position() + 2);
        int len = DerUtils.encodeIdAndLength(DerId.TagClass.UNIVERSAL, DerId.EncodingType.PRIMITIVE, 2, 10, buf);
        assertEquals(2, len);
        assertEquals(0, buf.position());
        assertEquals((byte) 2, buf.get(0));
        assertEquals((byte) 0xa, buf.get(1));
    }

    @Test
    public void testEncode2() {
        // application, primitive, tag <= 30, short length
        ByteBuffer buf = ByteBuffer.wrap(new byte[12]);
        buf.position(buf.position() + 2);
        int len = DerUtils.encodeIdAndLength(DerId.TagClass.APPLICATION, DerId.EncodingType.PRIMITIVE, 2, 10, buf);
        assertEquals(2, len);
        assertEquals(0, buf.position());
        assertEquals((byte) 0x42, buf.get(0));
        assertEquals((byte) 0xa, buf.get(1));
    }

    @Test
    public void testEncode3() {
        // context specific, constructed, tag <= 30, short length
        ByteBuffer buf = ByteBuffer.wrap(new byte[12]);
        buf.position(buf.position() + 2);
        int len = DerUtils.encodeIdAndLength(DerId.TagClass.CONTEXT_SPECIFIC, DerId.EncodingType.CONSTRUCTED, 2, 10, buf);
        assertEquals(2, len);
        assertEquals(0, buf.position());
        assertEquals((byte) 0xa2, buf.get(0));
        assertEquals((byte) 0xa, buf.get(1));
    }

    @Test
    public void testEncode4() {
        // private, constructed, tag <= 30, short length
        ByteBuffer buf = ByteBuffer.wrap(new byte[12]);
        buf.position(buf.position() + 2);
        int len = DerUtils.encodeIdAndLength(DerId.TagClass.PRIVATE, DerId.EncodingType.CONSTRUCTED, 2, 10, buf);
        assertEquals(2, len);
        assertEquals(0, buf.position());
        assertEquals((byte) 0xe2, buf.get(0));
        assertEquals((byte) 0xa, buf.get(1));
    }

    @Test
    public void testEncode5() {
        // universal, primitive, tag > 30 (2 byte), short length
        ByteBuffer buf = ByteBuffer.wrap(new byte[13]);
        buf.position(buf.position() + 3);
        int len = DerUtils.encodeIdAndLength(DerId.TagClass.UNIVERSAL, DerId.EncodingType.PRIMITIVE, 32, 10, buf);
        assertEquals(3, len);
        assertEquals(0, buf.position());
        assertEquals((byte) 0x1f, buf.get(0));
        assertEquals((byte) 0x20, buf.get(1));
        assertEquals((byte) 0xa, buf.get(2));
    }

    @Test
    public void testEncode6() {
        // application, primitive, tag > 30 (2 byte), short length
        ByteBuffer buf = ByteBuffer.wrap(new byte[13]);
        buf.position(buf.position() + 3);
        int len = DerUtils.encodeIdAndLength(DerId.TagClass.APPLICATION, DerId.EncodingType.PRIMITIVE, 32, 10, buf);
        assertEquals(3, len);
        assertEquals(0, buf.position());
        assertEquals((byte) 0x5f, buf.get(0));
        assertEquals((byte) 0x20, buf.get(1));
        assertEquals((byte) 0xa, buf.get(2));
    }

    @Test
    public void testEncode7() {
        // context specific, constructed, tag > 30 (2 byte), short length
        ByteBuffer buf = ByteBuffer.wrap(new byte[13]);
        buf.position(buf.position() + 3);
        int len = DerUtils.encodeIdAndLength(DerId.TagClass.CONTEXT_SPECIFIC, DerId.EncodingType.CONSTRUCTED, 32, 10, buf);
        assertEquals(3, len);
        assertEquals(0, buf.position());
        assertEquals((byte) 0xbf, buf.get(0));
        assertEquals((byte) 0x20, buf.get(1));
        assertEquals((byte) 0xa, buf.get(2));
    }

    @Test
    public void testEncode8() {
        // private, constructed, tag > 30 (2 byte), short length
        ByteBuffer buf = ByteBuffer.wrap(new byte[13]);
        buf.position(buf.position() + 3);
        int len = DerUtils.encodeIdAndLength(DerId.TagClass.PRIVATE, DerId.EncodingType.CONSTRUCTED, 32, 10, buf);
        assertEquals(3, len);
        assertEquals(0, buf.position());
        assertEquals((byte) 0xff, buf.get(0));
        assertEquals((byte) 0x20, buf.get(1));
        assertEquals((byte) 0xa, buf.get(2));
    }

    @Test
    public void testEncode9() {
        // context specific, constructed, tag > 30 (3 byte), short length
        ByteBuffer buf = ByteBuffer.wrap(new byte[14]);
        buf.position(buf.position() + 4);
        int len = DerUtils.encodeIdAndLength(DerId.TagClass.CONTEXT_SPECIFIC, DerId.EncodingType.CONSTRUCTED, 53280, 10, buf);
        assertEquals(4, len);
        assertEquals(0, buf.position());
        assertEquals((byte) 0xbf, buf.get(0));
        assertEquals((byte) 0xd0, buf.get(1));
        assertEquals((byte) 0x20, buf.get(2));
        assertEquals((byte) 0xa, buf.get(3));
    }

    @Test
    public void testEncode10() {
        // context specific, constructed, tag > 30 (3 byte), long length (201)
        ByteBuffer buf = ByteBuffer.wrap(new byte[206]);
        buf.position(buf.position() + 5);
        int len = DerUtils.encodeIdAndLength(DerId.TagClass.CONTEXT_SPECIFIC, DerId.EncodingType.CONSTRUCTED, 53280, 201, buf);
        assertEquals(5, len);
        assertEquals(0, buf.position());
        assertEquals((byte) 0xbf, buf.get(0));
        assertEquals((byte) 0xd0, buf.get(1));
        assertEquals((byte) 0x20, buf.get(2));
        assertEquals((byte) 0x81, buf.get(3));
        assertEquals((byte) 0xc9, buf.get(4));
    }

    // ----- END: Encode tests -----

}
