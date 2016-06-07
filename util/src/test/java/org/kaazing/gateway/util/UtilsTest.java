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
package org.kaazing.gateway.util;

import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * NOTE: tests for Utils.parseTimeInterval are in separate unit test class ParseTimeIntervalTest
 */
public class UtilsTest {

    public static int TEST_INT = 23;
    public static class TestInnerClass {
        public static int INT_FIELD = 11;
    }
    private static final String NON_ASCII_UTF8_STRING = "\u6C34"; // CJK UNIFIED IDEOGRAPH-6C34 (water)



    @Test
    public void asByteArrayShouldSupportDirectBufferWithNoArray() throws Exception {
        byte[] data = new byte[]{ (byte)1, (byte)2, (byte)3 };
        ByteBuffer buf = ByteBuffer.allocateDirect(data.length);
        buf.put(data);
        buf.flip();
        buf.get();
        byte[] result = Utils.asByteArray(buf);
        assertEquals(2, result[0]);
        assertEquals(3, result[1]);
    }

    @Test
    public void asByteArrayShouldUseUnderlyingArrayWhenPossible() throws Exception {
        byte[] data = new byte[]{ (byte)1, (byte)2, (byte)3 };
        ByteBuffer buf = ByteBuffer.wrap(data);
        byte[] result = Utils.asByteArray(buf);
        assertSame(buf.array(), result);
    }

    @Test
    public void asByteArrayShouldNotUseUnderlyingArrayIfCapacityGTLimit() throws Exception {
        byte[] data = new byte[]{ (byte)1, (byte)2, (byte)3 };
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.limit(2);
        byte[] result = Utils.asByteArray(buf);
        assertNotSame(buf.array(), result);
        assertEquals(2, result.length);
        assertEquals(1, result[0]);
        assertEquals(2, result[1]);
    }

    @Test
    public void asByteArrayShouldWorkWhenOffset() throws Exception {
        byte[] data = new byte[]{ (byte)1, (byte)2, (byte)3 };
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.get();
        byte[] result = Utils.asByteArray(buf);
        assertEquals(2, result[0]);
        assertEquals(3, result[1]);
    }

    @Test
    public void asStringShouldSupportDirectBufferWithNoArrayAndUseUTF8() throws Exception {
        byte[] utf8Bytes = NON_ASCII_UTF8_STRING.getBytes(Utils.UTF_8);
        ByteBuffer in = ByteBuffer.allocateDirect(utf8Bytes.length);
        in.put(utf8Bytes);
        in.flip();
        String result = Utils.asString(in);
        assertEquals(NON_ASCII_UTF8_STRING.charAt(0), result.charAt(0));
    }

    @Test
    public void getIntShouldWorkBigEndianIndex0() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.order(BIG_ENDIAN);
        buf.putInt(12345678);
        buf.flip();
        int result = Utils.getInt(buf, 0);
        int expected = buf.getInt();
        assertEquals(expected, result);
    }

    @Test
    public void getIntShouldWorkLittleEndianIndex0() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.order(LITTLE_ENDIAN);
        buf.putInt(12345678);
        buf.flip();
        int result = Utils.getInt(buf, 0);
        int expected = buf.getInt();
        assertEquals(expected, result);
    }

    @Test
    public void getIntShouldWorkBigEndianIndexNonZero() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.order(BIG_ENDIAN);
        buf.putInt(12345678);
        buf.putInt(87654321);
        buf.flip();
        int result = Utils.getInt(buf, 4);
        assertEquals(87654321, result);
    }

    @Test
    public void getIntShouldWorkLittleEndianIndexNonZero() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.order(LITTLE_ENDIAN);
        buf.putInt(12345678);
        buf.putInt(87654321);
        buf.flip();
        int result = Utils.getInt(buf, 4);
        assertEquals(87654321, result);
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void getIntShouldFailIfLimitTooLowBigEndian() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(3);
        buf.order(BIG_ENDIAN);
        Utils.getInt(buf, 0);
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void getIntShouldFailIfLimitTooLowLittleEndianIndex() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(3);
        buf.order(LITTLE_ENDIAN);
        Utils.getInt(buf, 0);
    }

    @Test
    public void getLongShouldWorkBigEndianIndex0() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.order(BIG_ENDIAN);
        buf.putLong(1234567890123456789L);
        buf.flip();
        long result = Utils.getLong(buf, 0);
        long expected = buf.getLong();
        assertEquals(expected, result);
    }

    @Test
    public void getLongShouldWorkLittleEndianIndex0() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.order(LITTLE_ENDIAN);
        buf.putLong(1234567890123456789L);
        buf.flip();
        long result = Utils.getLong(buf, 0);
        long expected = buf.getLong();
        assertEquals(expected, result);
    }

    @Test
    public void getLongShouldWorkBigEndianIndexNonZero() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.order(BIG_ENDIAN);
        buf.putLong(1234567890123456789L);
        buf.putLong(876543210987654321L);
        buf.flip();
        long result = Utils.getLong(buf, 8);
        assertEquals(876543210987654321L, result);
    }

    @Test
    public void getLongShouldWorkLittleEndianIndexNonZero() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.order(LITTLE_ENDIAN);
        buf.putLong(1234567890123456789L);
        buf.putLong(876543210987654321L);
        buf.flip();
        long result = Utils.getLong(buf, 8);
        assertEquals(876543210987654321L, result);
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void getLongShouldFailIfLimitTooLowBigEndian() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.order(BIG_ENDIAN);
        Utils.getLong(buf, 1);
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void getLongShouldFailIfLimitTooLowLittleEndianIndex() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.order(LITTLE_ENDIAN);
        Utils.getLong(buf, 1);
    }

    @Test
    public void getShortShouldWorkBigEndianIndex0() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.order(BIG_ENDIAN);
        buf.putShort((short)12345);
        buf.flip();
        short result = Utils.getShort(buf, 0);
        short expected = buf.getShort();
        assertEquals(expected, result);
    }

    @Test
    public void getShortShouldWorkLittleEndianIndex0() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.order(LITTLE_ENDIAN);
        buf.putShort((short)12345);
        buf.flip();
        short result = Utils.getShort(buf, 0);
        short expected = buf.getShort();
        assertEquals(expected, result);
    }

    @Test
    public void getShortShouldWorkBigEndianIndexNonZero() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.order(BIG_ENDIAN);
        buf.putShort((short)12345);
        buf.putShort((short)32101);
        buf.flip();
        short result = Utils.getShort(buf, 2);
        assertEquals(32101, result);
    }

    @Test
    public void getShortShouldWorkLittleEndianIndexNonZero() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.order(LITTLE_ENDIAN);
        buf.putShort((short)12345);
        buf.putShort((short)32101);
        buf.flip();
        short result = Utils.getShort(buf, 2);
        assertEquals(32101, result);
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void getShortShouldFailIfLimitTooLowBigEndian() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(3);
        buf.order(BIG_ENDIAN);
        Utils.getShort(buf, 2);
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void getShortShouldFailIfLimitTooLowLittleEndianIndex() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(3);
        buf.order(LITTLE_ENDIAN);
        Utils.getShort(buf, 2);
    }
    
    @Test
    public void initCapsShouldReturnEmptyString() {
        String result = Utils.initCaps("");
        assertEquals("", result);
    }
    
    @Test
    public void initCapsShouldUppercaseMultipleCharacterString() {
        String result = Utils.initCaps("abc");
        assertEquals("Abc", result);
    }
    
    @Test
    public void initCapsShouldUppercaseMCharacterString() {
        String result = Utils.initCaps("a");
        assertEquals("A", result);
    }

    @Test
    public void loadStaticIntValidOuterClass() throws Exception {
        int result = Utils.loadStaticInt(this.getClass().getName() + ".TEST_INT");
        Assert.assertEquals(TEST_INT, result);
    }

    @Test
    public void loadStaticIntValidInnerClass() throws Exception {
        int result = Utils.loadStaticInt(TestInnerClass.class.getName() + ".INT_FIELD");
        Assert.assertEquals(TestInnerClass.INT_FIELD, result);
    }

    @Test(expected = ClassNotFoundException.class)
    public void loadStaticIntBadNameLeadingDot() throws Exception {
        Utils.loadStaticInt(".NonexClass.INT_FIELD");
    }

    @Test(expected = IllegalArgumentException.class)
    public void loadStaticIntBadNameTrailingDot() throws Exception {
        Utils.loadStaticInt("NonexistentClass.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void loadStaticIntBadNameNoDot() throws Exception {
        Utils.loadStaticInt("InvalidClassName");
    }

    @Test(expected = ClassNotFoundException.class)
    public void loadStaticIntInvalidClassName() throws Exception {
        Utils.loadStaticInt("%Invalid@ClassName.INT_FIELD");
    }

    @Test(expected = NoSuchFieldException.class)
    public void loadStaticIntFieldNotFound() throws Exception {
        Utils.loadStaticInt(TestInnerClass.class.getName() + ".nonexistentField");
    }

    @Test(expected = ClassNotFoundException.class)
    public void loadStaticIntNotFound() throws Exception {
        Utils.loadStaticInt("an.invalid.package.AClass.INT_FIELD");
    }

    @Test
    public void loadClassValidInnerClass() throws Exception {
        int result = Utils.loadStaticInt(TestInnerClass.class.getName() + ".INT_FIELD");
        Assert.assertEquals(TestInnerClass.INT_FIELD, result);
    }

    @Test(expected = ClassNotFoundException.class)
    public void loadClassBadType() throws Exception {
        Utils.loadClass(TestInnerClass.class.getName() + ".INT_FIELD");
    }

    @Test
    public void bytes() {
        int result = Utils.parseDataSize("12345");
	    assertEquals(12345, result);
    }

    @Test
    public void kilobytesLC() {
        int result = Utils.parseDataSize("12345k");
        assertEquals(12345 * 1024, result);
    }

    @Test
    public void kilobytesUC() {
        int result = Utils.parseDataSize("12345K");
        assertEquals(12345 * 1024, result);
    }

    @Test
    public void megabytesLC() {
        int result = Utils.parseDataSize("1m");
        assertEquals(1 * 1024 * 1024, result);
    }

    @Test
    public void megabytesUC() {
        int result = Utils.parseDataSize("0M");
        assertEquals(0, result);
    }

    @Test (expected=NumberFormatException.class)
    public void negativeEmpty() {
        Utils.parseDataSize("");
    }

    @Test (expected=NumberFormatException.class)
    public void negativeLetterOnly() {
        Utils.parseDataSize("k");
    }

    @Test (expected=NumberFormatException.class)
    public void negativeWrongLetter() {
        Utils.parseDataSize("400J");
    }

    @Test (expected=NumberFormatException.class)
    public void negativeTwoLetters() {
        Utils.parseDataSize("400KB");
    }

    @Test
    public void noUnitsDataRate() {
        long result = Utils.parseDataRate("12345");
        assertEquals(12345, result);
    }

    @Test
    public void bytesDataRate() {
        long result = Utils.parseDataRate("12345B/s");
        assertEquals(12345, result);
    }

    @Test
    public void kilobytesDataRate() {
        long result = Utils.parseDataRate("12345kB/s");
        assertEquals(12345 * 1000, result);
    }

    @Test
    public void kibibytesDataRate() {
        long result = Utils.parseDataRate("12345KiB/s");
        assertEquals(12345 * 1024, result);
    }

    @Test
    public void megabytesDataRate() {
        long result = Utils.parseDataRate("1MB/s");
        assertEquals(1 * 1000 * 1000, result);
    }

    @Test
    public void mibibytesDataRate() {
        long result = Utils.parseDataRate("100MiB/s");
        assertEquals(100 * 1024 * 1024, result);
    }

    @Test (expected=NumberFormatException.class)
    public void negativeEmptyDataRate() {
        Utils.parseDataRate("");
    }

    @Test (expected=NumberFormatException.class)
    public void negativeZeroDataRate() {
        Utils.parseDataRate("0");
    }

    @Test (expected=NumberFormatException.class)
    public void negativeZeroDataRateWithUnit() {
        Utils.parseDataRate("0MiB/s");
    }

    @Test (expected=NumberFormatException.class)
    public void negativeWithSpacesDataRate() {
        Utils.parseDataRate("1000 MiB/s");
    }

    @Test (expected=NumberFormatException.class)
    public void negativeUnitOnlyDataRate() {
        Utils.parseDataRate("kB/s");
    }

    @Test (expected=NumberFormatException.class)
    public void negativeWrongLetterDataRate() {
        Utils.parseDataRate("400GB/s"); // should be kB/s
    }

    @Test (expected=NumberFormatException.class)
    public void negativeWrongCaseDataRate() {
        Utils.parseDataRate("400KB/s"); // should be kB/s
    }

    @Test (expected=NumberFormatException.class)
    public void negativeOverflowDataRate() {
        String maxLong = Long.toString(Long.MAX_VALUE/100);
        Utils.parseDataRate(maxLong + "kB/s");
    }

    @Test
    public void testGetHostString() throws Exception {

        InetSocketAddress unresolvedAddress = InetSocketAddress.createUnresolved("example.com", 5555);
        String host = Utils.getHostStringWithoutNameLookup(unresolvedAddress);
        assertEquals("example.com", host);

        InetSocketAddress resolvedAddress = new InetSocketAddress("localhost", 5555);
        assertFalse(resolvedAddress.isUnresolved());
        host = Utils.getHostStringWithoutNameLookup(resolvedAddress);
        assertEquals("127.0.0.1", host);
    }

    @Test
    public void validPositiveInteger() {
        long result = Utils.parsePositiveInteger("bushells", "123", 0);
        assertEquals(123, result);
    }

    @Test @Ignore // jdk 6 rejects value with + sign, jdk 7 allows it
    public void validPositiveIntegerWithPlusSign() {
        long result = Utils.parsePositiveInteger("bushells", "+123", 0);
        assertEquals(123, result);
    }

    @Test
    public void validMaxPositiveInteger() {
        long result = Utils.parsePositiveInteger("bushells",
                Long.toString(Long.MAX_VALUE), 0);
        assertEquals(Long.MAX_VALUE, result);
    }

    @Test
    public void defaultPositiveInteger() {
        long result = Utils.parsePositiveInteger("bushells", null, 0);
        assertEquals(0, result);
    }

    @Test (expected=IllegalArgumentException.class)
    public void negativeDecimalPositiveInteger() {
        Utils.parsePositiveInteger("bushells", "1.23", 0);
    }

    @Test (expected=IllegalArgumentException.class)
    public void negativeEmptyPositiveInteger() {
        Utils.parsePositiveInteger("bushells", "", 0);
    }

    @Test (expected=IllegalArgumentException.class)
    public void negativeLeadingSpacePositiveInteger() {
        Utils.parsePositiveInteger("bushells", " 123", 0);
    }

    @Test (expected=IllegalArgumentException.class)
    public void negativeTrailingSpacePositiveInteger() {
        Utils.parsePositiveInteger("cubits", "123 ", 0);
    }

    @Test (expected=IllegalArgumentException.class)
    public void negativePositiveIntegerWithNegativeValue() {
        Utils.parsePositiveInteger("system property \"long.stupid.name\"", "-12", 0);
    }

    @Test (expected=IllegalArgumentException.class)
    public void negativePositiveIntegerWithZeroValue() {
        Utils.parsePositiveInteger("system property \"long.stupid.name\"",
                "0", 0);
    }

    @Test
    public void validTrueBoolean() {
        boolean result = Utils.parseBoolean("bushells", "true", false);
        assertEquals(true, result);
    }

    @Test
    public void validTrueMixedCaseBoolean() {
        boolean result = Utils.parseBoolean("bushells", "TRue", false);
        assertEquals(true, result);
    }

    @Test
    public void validFalseBoolean() {
        boolean result = Utils.parseBoolean("bushells", "false", false);
        assertEquals(false, result);
    }

    @Test
    public void validFalseMixedCaseBoolean() {
        boolean result = Utils.parseBoolean("bushells", "FALSe", false);
        assertEquals(false, result);
    }

    @Test (expected=IllegalArgumentException.class)
    public void negativeEmptyBoolean() {
        Utils.parseBoolean("system property \"long.stupid.name\"",
                "trye", true);
    }

    @Test (expected=IllegalArgumentException.class)
    public void negativeTypoBoolean() {
        Utils.parseBoolean("system property \"long.stupid.name\"",
                "trye", true);
    }

    @Test (expected=IllegalArgumentException.class)
    public void negativeCompletelyWrongBoolean() {
        Utils.parseBoolean("system property \"long.stupid.name\"",
                "1234", true);
    }


}
