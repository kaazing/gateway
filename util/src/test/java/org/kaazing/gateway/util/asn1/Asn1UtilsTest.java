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
package org.kaazing.gateway.util.asn1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;

public class Asn1UtilsTest {

    // ----- BEGIN: INTEGER tests

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeIntegerWithNull() {
        Asn1Utils.decodeInteger(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeIntegerWithEmpty() {
        Asn1Utils.decodeInteger(ByteBuffer.allocate(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeIntegerWithoutLength() {
        Asn1Utils.decodeInteger(ByteBuffer.wrap(new byte[] { 2 }));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeIntegerWithoutData() {
        Asn1Utils.decodeInteger(ByteBuffer.wrap(new byte[] { 2, 1 }));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeIntegerWithWrongTag() {
        assertEquals(5, Asn1Utils.decodeInteger(ByteBuffer.wrap(new byte[] { 3, 1, 5 })));
    }

    @Test
    public void testDecodeSingleByteInteger() {
        assertEquals(5, Asn1Utils.decodeInteger(ByteBuffer.wrap(new byte[] { 2, 1, 5 })));
    }

    @Test
    public void testDecodeMultiByteInteger() {
        assertEquals(1285, Asn1Utils.decodeInteger(ByteBuffer.wrap(new byte[] { 2, 2, 5, 5 })));
    }

    @Test
    public void testEncodeIntegerZero() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[3]);
        buf.position(buf.position() + 3);
        int len = Asn1Utils.encodeInteger(0, buf);
        assertEquals(3, len);
        assertEquals(0, buf.position());
        assertEquals((byte) 2, buf.get(0));
        assertEquals((byte) 1, buf.get(1));
        assertEquals((byte) 0, buf.get(2));
    }

    @Test
    public void testEncodeInteger1Byte() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[3]);
        buf.position(buf.position() + 3);
        int len = Asn1Utils.encodeInteger(15, buf);
        assertEquals(3, len);
        assertEquals(0, buf.position());
        assertEquals((byte) 2, buf.get(0));
        assertEquals((byte) 1, buf.get(1));
        assertEquals((byte) 0xf, buf.get(2));
    }

    @Test
    public void testEncodeInteger2Byte() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[4]);
        buf.position(buf.position() + 4);
        int len = Asn1Utils.encodeInteger(512, buf);
        assertEquals(4, len);
        assertEquals(0, buf.position());
        assertEquals((byte) 2, buf.get(0));
        assertEquals((byte) 2, buf.get(1));
        assertEquals((byte) 2, buf.get(2));
        assertEquals((byte) 0, buf.get(3));
    }

    @Test
    public void testSizeOfInteger() {
        assertEquals(3, Asn1Utils.sizeOfInteger(15));
        assertEquals(4, Asn1Utils.sizeOfInteger(512));
        assertEquals(6, Asn1Utils.sizeOfInteger(1954663611));
    }

    // ----- END: INTEGER tests

    // ----- BEGIN: SEQUENCE tests

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeSequenceWithNull() {
        Asn1Utils.decodeSequence(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeSequenceWithEmpty() {
        Asn1Utils.decodeSequence(ByteBuffer.allocate(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeSequenceWithoutLength() {
        Asn1Utils.decodeSequence(ByteBuffer.wrap(new byte[] { 0x30 }));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeSequenceWithoutData() {
        Asn1Utils.decodeSequence(ByteBuffer.wrap(new byte[] { 0x30, 3 }));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeSequenceWithWrongTag() {
        Asn1Utils.decodeSequence(ByteBuffer.wrap(new byte[] { 0x31, 3, 2, 1, 5 }));
    }

    @Test
    public void testDecodeSequenceWithData() {
        Asn1Utils.decodeSequence(ByteBuffer.wrap(new byte[] { 0x30, 3, 2, 1, 5 }));
    }

    @Test
    public void testEncodeSequence() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[4]);
        buf.position(buf.position() + 2);
        int len = Asn1Utils.encodeSequence(2, buf);
        assertEquals(4, len);
        assertEquals(0, buf.position());
        assertEquals((byte) 0x30, buf.get(0));
        assertEquals((byte) 2, buf.get(1));
    }

    @Test
    public void testSizeOfSequence() {
        assertEquals(2, Asn1Utils.sizeOfSequence(0));
        assertEquals(3, Asn1Utils.sizeOfSequence(1));
        assertEquals(4, Asn1Utils.sizeOfSequence(2));
        assertEquals(5, Asn1Utils.sizeOfSequence(3));
    }

    // ----- END: SEQUENCE tests

    // ----- BEGIN: BIT STRING tests

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeBitStringWithNull() {
        Asn1Utils.decodeBitString(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeBitStringWithEmpty() {
        Asn1Utils.decodeBitString(ByteBuffer.allocate(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeBitStringWithoutLength() {
        Asn1Utils.decodeBitString(ByteBuffer.wrap(new byte[] { 3 }));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeBitStringWithoutPaddingAndData() {
        Asn1Utils.decodeBitString(ByteBuffer.wrap(new byte[] { 3, 5 }));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeBitStringWithoutData() {
        Asn1Utils.decodeBitString(ByteBuffer.wrap(new byte[] { 3, 5, 0 }));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeBitStringWithInvalidTag() {
        Asn1Utils.decodeBitString(ByteBuffer.wrap(new byte[] { 4, 5, 0, 0, (byte) 0x80, 0, 0 }));
    }

    @Test
    public void testDecodeBitStringWithData() {
        BitSet bits = Asn1Utils.decodeBitString(ByteBuffer.wrap(new byte[] { 3, 5, 0, 0, (byte) 0x80, 0, 0 }));
        for (int i = 0; i < bits.length(); i++) {
            if (i == 8) {
                assertTrue(bits.get(i));
            } else {
                assertFalse(bits.get(i));
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEncodeBitStringNull() {
        Asn1Utils.encodeBitString(null, 0, ByteBuffer.wrap(new byte[0]));
    }

    @Test
    public void testEncodeBitString8() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[4]);
        buf.position(buf.position() + 4);
        BitSet bits = new BitSet(8);
        bits.set(3);
        bits.set(5);
        int len = Asn1Utils.encodeBitString(bits, 8, buf);
        assertEquals(4, len);
        assertEquals(0, buf.position());
        assertEquals((byte) 3, buf.get(0));
        assertEquals((byte) 2, buf.get(1));
        assertEquals((byte) 0, buf.get(2));
        assertEquals((byte) 0x14, buf.get(3));
    }

    @Test
    public void testEncodeBitString32() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[7]);
        buf.position(buf.position() + 7);
        BitSet bits = new BitSet(32);
        bits.set(8);
        int len = Asn1Utils.encodeBitString(bits, 32, buf);
        assertEquals(7, len);
        assertEquals(0, buf.position());
        assertEquals((byte) 3, buf.get(0));
        assertEquals((byte) 5, buf.get(1));
        assertEquals((byte) 0, buf.get(2));
        assertEquals((byte) 0, buf.get(3));
        assertEquals((byte) 0x80, buf.get(4));
        assertEquals((byte) 0, buf.get(5));
        assertEquals((byte) 0, buf.get(6));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEncodeBitString32As8() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[7]);
        buf.position(buf.position() + 7);
        BitSet bits = new BitSet(32);
        bits.set(8);
        Asn1Utils.encodeBitString(bits, 8, buf);
    }

    @Test
    public void testSizeOfBitString() {
        BitSet bits = new BitSet(32);
        assertEquals(4, Asn1Utils.sizeOfBitString(bits, 7));
        assertEquals(4, Asn1Utils.sizeOfBitString(bits, 8));
        assertEquals(5, Asn1Utils.sizeOfBitString(bits, 9));
        assertEquals(7, Asn1Utils.sizeOfBitString(bits, 32));
    }

    // ----- END: BIT STRING tests

    // ----- BEGIN: IA5STRING tests

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeIA5StringWithNull() {
        Asn1Utils.decodeIA5String(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeIA5StringWithEmpty() {
        Asn1Utils.decodeIA5String(ByteBuffer.allocate(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeIA5StringWithoutLength() {
        Asn1Utils.decodeIA5String(ByteBuffer.wrap(new byte[] { 0x1b }));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeIA5StringWithoutData() {
        Asn1Utils.decodeIA5String(ByteBuffer.wrap(new byte[] { 0x1b, 4 }));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeIA5StringWithWrongTag() {
        Asn1Utils.decodeIA5String(ByteBuffer.wrap(new byte[] { 0x1c, 0 }));
    }

    @Test
    public void testDecodeIA5StringWithEmptyData() {
        String value = Asn1Utils.decodeIA5String(ByteBuffer.wrap(new byte[] { 0x1b, 0 }));
        assertEquals("", value);
    }

    @Test
    public void testDecodeIA5StringWithData() {
        String value = Asn1Utils.decodeIA5String(ByteBuffer.wrap(new byte[] { 0x1b, 4, 0x61, 0x72, 0x75, 0x6e }));
        assertEquals("arun", value);
    }

    @Test
    public void testEncodeIA5StringWithNull() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[2]);
        buf.position(buf.position() + 2);
        int len = Asn1Utils.encodeIA5String(null, buf);
        assertEquals(2, len);
        assertEquals(0, buf.position());
        assertEquals((byte) 0x1b, buf.get(0));
        assertEquals((byte) 0, buf.get(1));
    }

    @Test
    public void testEncodeIA5StringWithEmpty() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[2]);
        buf.position(buf.position() + 2);
        int len = Asn1Utils.encodeIA5String("", buf);
        assertEquals(2, len);
        assertEquals(0, buf.position());
        assertEquals((byte) 0x1b, buf.get(0));
        assertEquals((byte) 0, buf.get(1));
    }

    @Test
    public void testEncodeIA5StringWithValue() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[6]);
        buf.position(buf.position() + 6);
        int len = Asn1Utils.encodeIA5String("arun", buf);
        assertEquals(6, len);
        assertEquals(0, buf.position());
        assertEquals((byte) 0x1b, buf.get(0));
        assertEquals((byte) 4, buf.get(1));
        assertEquals((byte) 0x61, buf.get(2));
        assertEquals((byte) 0x72, buf.get(3));
        assertEquals((byte) 0x75, buf.get(4));
        assertEquals((byte) 0x6e, buf.get(5));
    }

    @Test
    public void testSizeOfIA5String() {
        assertEquals(2, Asn1Utils.sizeOfIA5String(null));
        assertEquals(2, Asn1Utils.sizeOfIA5String(""));
        assertEquals(6, Asn1Utils.sizeOfIA5String("arun"));
    }

    // ----- END: IA5STRING tests

    // ----- BEGIN: GeneralizedTime tests

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeGeneralizedTimeWithNull() {
        Asn1Utils.decodeGeneralizedTime(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeGeneralizedTimeWithEmpty() {
        Asn1Utils.decodeGeneralizedTime(ByteBuffer.allocate(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeGeneralizedTimeWithoutLength() {
        Asn1Utils.decodeGeneralizedTime(ByteBuffer.wrap(new byte[] { 0x18 }));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeGeneralizedTimeWithoutData() {
        Asn1Utils.decodeGeneralizedTime(ByteBuffer.wrap(new byte[] { 0x18, 0xf }));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeGeneralizedTimeWithWrongTag() {
        byte[] data = new byte[] { 0x19, 0xe, 0x32, 0x30, 0x31, 0x30, 0x30, 0x37, 0x31, 0x32, 0x32, 0x31, 0x34, 0x35, 0x32, 0x37 };
        Asn1Utils.decodeGeneralizedTime(ByteBuffer.wrap(data));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeGeneralizedTimeWithMalformedData() {
        byte[] data = new byte[] { 0x18, 0xc, 0x31, 0x30, 0x30, 0x37, 0x31, 0x32, 0x32, 0x31, 0x34, 0x35, 0x32, 0x37 };
        Asn1Utils.decodeGeneralizedTime(ByteBuffer.wrap(data));
    }

    @Test
    public void testDecodeGeneralizedTimeWithoutTimeZone() {
        // Test date: 2010 07 12 21 45 27
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, 2010);
        cal.set(Calendar.MONTH, Calendar.JULY);
        cal.set(Calendar.DAY_OF_MONTH, 12);
        cal.set(Calendar.HOUR_OF_DAY, 21);
        cal.set(Calendar.MINUTE, 45);
        cal.set(Calendar.SECOND, 27);
        Date expectedDate = cal.getTime();

        byte[] data = new byte[] { 0x18, 0xe, 0x32, 0x30, 0x31, 0x30, 0x30, 0x37, 0x31, 0x32, 0x32, 0x31, 0x34, 0x35, 0x32, 0x37 };
        Date actualDate = Asn1Utils.decodeGeneralizedTime(ByteBuffer.wrap(data));

        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDecodeGeneralizedTimeWithTimeZoneZ() {
        // Test date: 2010 07 12 21 45 27 Z
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, 2010);
        cal.set(Calendar.MONTH, Calendar.JULY);
        cal.set(Calendar.DAY_OF_MONTH, 12);
        cal.set(Calendar.HOUR_OF_DAY, 21);
        cal.set(Calendar.MINUTE, 45);
        cal.set(Calendar.SECOND, 27);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date expectedDate = cal.getTime();

        byte[] data = new byte[] { 0x18, 0xf, 0x32, 0x30, 0x31, 0x30, 0x30, 0x37, 0x31, 0x32, 0x32, 0x31, 0x34, 0x35, 0x32, 0x37,
                0x5a };
        Date actualDate = Asn1Utils.decodeGeneralizedTime(ByteBuffer.wrap(data));

        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDecodeGeneralizedTimeWithPosTimeZone() {
        // Test date: 2010 07 12 21 45 27 +0800
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, 2010);
        cal.set(Calendar.MONTH, Calendar.JULY);
        cal.set(Calendar.DAY_OF_MONTH, 12);
        cal.set(Calendar.HOUR_OF_DAY, 21);
        cal.set(Calendar.MINUTE, 45);
        cal.set(Calendar.SECOND, 27);
        cal.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        Date expectedDate = cal.getTime();

        byte[] data = new byte[] { 0x18, 0x13, 0x32, 0x30, 0x31, 0x30, 0x30, 0x37, 0x31, 0x32, 0x32, 0x31, 0x34, 0x35, 0x32,
                0x37, 0x2b, 0x30, 0x38, 0x30, 0x30 };
        Date actualDate = Asn1Utils.decodeGeneralizedTime(ByteBuffer.wrap(data));

        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDecodeGeneralizedTimeWithNegTimeZone() {
        // Test date: 2010 07 12 21 45 27 -0800
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, 2010);
        cal.set(Calendar.MONTH, Calendar.JULY);
        cal.set(Calendar.DAY_OF_MONTH, 12);
        cal.set(Calendar.HOUR_OF_DAY, 21);
        cal.set(Calendar.MINUTE, 45);
        cal.set(Calendar.SECOND, 27);
        cal.setTimeZone(TimeZone.getTimeZone("GMT-8"));
        Date expectedDate = cal.getTime();

        byte[] data = new byte[] { 0x18, 0x13, 0x32, 0x30, 0x31, 0x30, 0x30, 0x37, 0x31, 0x32, 0x32, 0x31, 0x34, 0x35, 0x32,
                0x37, 0x2d, 0x30, 0x38, 0x30, 0x30 };
        Date actualDate = Asn1Utils.decodeGeneralizedTime(ByteBuffer.wrap(data));

        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDecodeGeneralizedTimeWithoutTimeZoneWithFracSec() {
        // Test date: 2010 07 12 21 45 27.3
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, 2010);
        cal.set(Calendar.MONTH, Calendar.JULY);
        cal.set(Calendar.DAY_OF_MONTH, 12);
        cal.set(Calendar.HOUR_OF_DAY, 21);
        cal.set(Calendar.MINUTE, 45);
        cal.set(Calendar.SECOND, 27);
        cal.set(Calendar.MILLISECOND, 300);
        Date expectedDate = cal.getTime();

        byte[] data = new byte[] { 0x18, 0x10, 0x32, 0x30, 0x31, 0x30, 0x30, 0x37, 0x31, 0x32, 0x32, 0x31, 0x34, 0x35, 0x32,
                0x37, 0x2e, 0x33 };
        Date actualDate = Asn1Utils.decodeGeneralizedTime(ByteBuffer.wrap(data));

        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDecodeGeneralizedTimeWithoutTimeZoneWithFracSec2Byte() {
        // Test date: 2010 07 12 21 45 27.36
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, 2010);
        cal.set(Calendar.MONTH, Calendar.JULY);
        cal.set(Calendar.DAY_OF_MONTH, 12);
        cal.set(Calendar.HOUR_OF_DAY, 21);
        cal.set(Calendar.MINUTE, 45);
        cal.set(Calendar.SECOND, 27);
        cal.set(Calendar.MILLISECOND, 360);
        Date expectedDate = cal.getTime();

        byte[] data = new byte[] { 0x18, 0x11, 0x32, 0x30, 0x31, 0x30, 0x30, 0x37, 0x31, 0x32, 0x32, 0x31, 0x34, 0x35, 0x32,
                0x37, 0x2e, 0x33, 0x36 };
        Date actualDate = Asn1Utils.decodeGeneralizedTime(ByteBuffer.wrap(data));

        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDecodeGeneralizedTimeWithoutTimeZoneWithFracSec4Byte() {
        // Test date: 2010 07 12 21 45 27.3629
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, 2010);
        cal.set(Calendar.MONTH, Calendar.JULY);
        cal.set(Calendar.DAY_OF_MONTH, 12);
        cal.set(Calendar.HOUR_OF_DAY, 21);
        cal.set(Calendar.MINUTE, 45);
        cal.set(Calendar.SECOND, 27);
        cal.set(Calendar.MILLISECOND, 362);
        Date expectedDate = cal.getTime();

        byte[] data = new byte[] { 0x18, 0x13, 0x32, 0x30, 0x31, 0x30, 0x30, 0x37, 0x31, 0x32, 0x32, 0x31, 0x34, 0x35, 0x32,
                0x37, 0x2e, 0x33, 0x36, 0x32, 0x39 };
        Date actualDate = Asn1Utils.decodeGeneralizedTime(ByteBuffer.wrap(data));

        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDecodeGeneralizedTimeWithTimeZoneZWithFracSec() {
        // Test date: 2010 07 12 21 45 27.3 Z
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, 2010);
        cal.set(Calendar.MONTH, Calendar.JULY);
        cal.set(Calendar.DAY_OF_MONTH, 12);
        cal.set(Calendar.HOUR_OF_DAY, 21);
        cal.set(Calendar.MINUTE, 45);
        cal.set(Calendar.SECOND, 27);
        cal.set(Calendar.MILLISECOND, 300);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date expectedDate = cal.getTime();

        byte[] data = new byte[] { 0x18, 0x11, 0x32, 0x30, 0x31, 0x30, 0x30, 0x37, 0x31, 0x32, 0x32, 0x31, 0x34, 0x35, 0x32,
                0x37, 0x2e, 0x33, 0x5a };
        Date actualDate = Asn1Utils.decodeGeneralizedTime(ByteBuffer.wrap(data));

        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDecodeGeneralizedTimeWithPosTimeZoneWithFracSec() {
        // Test date: 2010 07 12 21 45 27.3 +0800
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, 2010);
        cal.set(Calendar.MONTH, Calendar.JULY);
        cal.set(Calendar.DAY_OF_MONTH, 12);
        cal.set(Calendar.HOUR_OF_DAY, 21);
        cal.set(Calendar.MINUTE, 45);
        cal.set(Calendar.SECOND, 27);
        cal.set(Calendar.MILLISECOND, 300);
        cal.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        Date expectedDate = cal.getTime();

        byte[] data = new byte[] { 0x18, 0x15, 0x32, 0x30, 0x31, 0x30, 0x30, 0x37, 0x31, 0x32, 0x32, 0x31, 0x34, 0x35, 0x32,
                0x37, 0x2e, 0x33, 0x2b, 0x30, 0x38, 0x30, 0x30 };
        Date actualDate = Asn1Utils.decodeGeneralizedTime(ByteBuffer.wrap(data));

        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDecodeGeneralizedTimeWithNegTimeZoneWithFracSec() {
        // Test date: 2010 07 12 21 45 27.3 -0800
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, 2010);
        cal.set(Calendar.MONTH, Calendar.JULY);
        cal.set(Calendar.DAY_OF_MONTH, 12);
        cal.set(Calendar.HOUR_OF_DAY, 21);
        cal.set(Calendar.MINUTE, 45);
        cal.set(Calendar.SECOND, 27);
        cal.set(Calendar.MILLISECOND, 300);
        cal.setTimeZone(TimeZone.getTimeZone("GMT-8"));
        Date expectedDate = cal.getTime();

        byte[] data = new byte[] { 0x18, 0x15, 0x32, 0x30, 0x31, 0x30, 0x30, 0x37, 0x31, 0x32, 0x32, 0x31, 0x34, 0x35, 0x32,
                0x37, 0x2e, 0x33, 0x2d, 0x30, 0x38, 0x30, 0x30 };
        Date actualDate = Asn1Utils.decodeGeneralizedTime(ByteBuffer.wrap(data));

        assertEquals(expectedDate, actualDate);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEncodeGeneralizedTimeWithNull() {
        Asn1Utils.encodeGeneralizedTime(null, null);
    }

    @Test
    public void testEncodeGeneralizedTime() {
        // Test date: 2010 07 12 21 45 27 UTC
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, 2010);
        cal.set(Calendar.MONTH, Calendar.JULY);
        cal.set(Calendar.DAY_OF_MONTH, 12);
        cal.set(Calendar.HOUR_OF_DAY, 21);
        cal.set(Calendar.MINUTE, 45);
        cal.set(Calendar.SECOND, 27);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date date = cal.getTime();

        int size = 17;
        ByteBuffer buf = ByteBuffer.wrap(new byte[size]);
        buf.position(buf.position() + size);
        int len = Asn1Utils.encodeGeneralizedTime(date, buf);

        assertEquals(size, len);
        assertEquals(0, buf.position());
        assertEquals((byte) 0x18, buf.get(0));
        assertEquals((byte) 0xf, buf.get(1));
        assertEquals((byte) 0x32, buf.get(2));
        assertEquals((byte) 0x30, buf.get(3));
        assertEquals((byte) 0x31, buf.get(4));
        assertEquals((byte) 0x30, buf.get(5));
        assertEquals((byte) 0x30, buf.get(6));
        assertEquals((byte) 0x37, buf.get(7));
        assertEquals((byte) 0x31, buf.get(8));
        assertEquals((byte) 0x32, buf.get(9));
        assertEquals((byte) 0x32, buf.get(10));
        assertEquals((byte) 0x31, buf.get(11));
        assertEquals((byte) 0x34, buf.get(12));
        assertEquals((byte) 0x35, buf.get(13));
        assertEquals((byte) 0x32, buf.get(14));
        assertEquals((byte) 0x37, buf.get(15));
        assertEquals((byte) 0x5a, buf.get(16));
    }

    @Test
    public void testSizeOfGeneralizedTime() {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, 2010);
        cal.set(Calendar.MONTH, Calendar.JULY);
        cal.set(Calendar.DAY_OF_MONTH, 12);
        cal.set(Calendar.HOUR_OF_DAY, 21);
        cal.set(Calendar.MINUTE, 45);
        cal.set(Calendar.SECOND, 27);
        cal.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        Date date = cal.getTime();
        assertEquals(17, Asn1Utils.sizeOfGeneralizedTime(date));
    }

    @Test
    public void testSizeOfGeneralizedTimeWithFracSet() {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, 2010);
        cal.set(Calendar.MONTH, Calendar.JULY);
        cal.set(Calendar.DAY_OF_MONTH, 12);
        cal.set(Calendar.HOUR_OF_DAY, 21);
        cal.set(Calendar.MINUTE, 45);
        cal.set(Calendar.SECOND, 27);
        cal.set(Calendar.MILLISECOND, 300);
        cal.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        Date date = cal.getTime();
        assertEquals(17, Asn1Utils.sizeOfGeneralizedTime(date));
    }

    // ----- END: GeneralizedTime tests

    // ----- BEGIN: OCTET STRING tests

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeOctetStringWithNull() {
        Asn1Utils.decodeOctetString(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeOctetStringWithEmpty() {
        Asn1Utils.decodeOctetString(ByteBuffer.allocate(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeOctetStringWithoutLength() {
        Asn1Utils.decodeOctetString(ByteBuffer.wrap(new byte[] { 4 }));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeOctetStringWithoutData() {
        Asn1Utils.decodeOctetString(ByteBuffer.wrap(new byte[] { 4, 4 }));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeOctetStringWithWrongTag() {
        Asn1Utils.decodeOctetString(ByteBuffer.wrap(new byte[] { 5, 0 }));
    }

    @Test
    public void testDecodeOctetStringWithEmptyData() {
        short[] value = Asn1Utils.decodeOctetString(ByteBuffer.wrap(new byte[] { 4, 0 }));
        assertEquals(0, value.length);
    }

    @Test
    public void testDecodeOctetStringWithData() {
        short[] value = Asn1Utils.decodeOctetString(ByteBuffer.wrap(new byte[] { 4, 4, (byte) 0xc0, (byte) 0xa8, 0, 0x0f }));
        assertEquals(4, value.length);
        assertEquals((short) 192, value[0]);
        assertEquals((short) 168, value[1]);
        assertEquals((short) 0, value[2]);
        assertEquals((short) 15, value[3]);
    }

    @Test
    public void testEncodeOctetStringNull() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[2]);
        buf.position(buf.position() + 2);
        int len = Asn1Utils.encodeOctetString(null, buf);
        assertEquals(2, len);
        assertEquals(0, buf.position());
        assertEquals((byte) 4, buf.get(0));
        assertEquals((byte) 0, buf.get(1));
    }

    @Test
    public void testEncodeOctetStringEmpty() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[2]);
        buf.position(buf.position() + 2);
        int len = Asn1Utils.encodeOctetString(new short[] {}, buf);
        assertEquals(2, len);
        assertEquals(0, buf.position());
        assertEquals((byte) 4, buf.get(0));
        assertEquals((byte) 0, buf.get(1));
    }

    @Test
    public void testEncodeOctetString() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[6]);
        buf.position(buf.position() + 6);
        int len = Asn1Utils.encodeOctetString(new short[] { 192, 168, 0, 15 }, buf);
        assertEquals(6, len);
        assertEquals(0, buf.position());
        assertEquals((byte) 4, buf.get(0));
        assertEquals((byte) 4, buf.get(1));
        assertEquals((byte) 0xc0, buf.get(2));
        assertEquals((byte) 0xa8, buf.get(3));
        assertEquals((byte) 0, buf.get(4));
        assertEquals((byte) 0x0f, buf.get(5));
    }

    @Test
    public void testSizeOfOctetString() {
        assertEquals(2, Asn1Utils.sizeOfOctetString(null));
        assertEquals(2, Asn1Utils.sizeOfOctetString(new short[] {}));
        assertEquals(6, Asn1Utils.sizeOfOctetString(new short[] { 192, 168, 0, 15 }));
    }

    // ----- END: OCTET STRING tests
}
