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

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kaazing.gateway.util.der.DerId;
import org.kaazing.gateway.util.der.DerUtils;

/**
 * Utilities for working with ASN.1 types. See X.680-0207.
 */
public final class Asn1Utils {
    private static final int ASN1_INTEGER_TAG_NUM = 2;
    private static final int ASN1_BIT_STRING_TAG_NUM = 3;
    private static final int ASN1_OCTET_STRING_TAG_NUM = 4;
    private static final int ASN1_SEQUENCE_TAG_NUM = 16;
    private static final int ASN1_GENERALIZED_TIME_TAG_NUM = 24;
    private static final int ASN1_IA5STRING_TAG_NUM = 27;

    // Generalized time format is yyyyMMddHHmmss[.[s]+][Z|[+-]zzzz]
    private static final String GENERALIZED_TIME_REGEX =
            "(\\d{4})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\.\\d+)?(Z|[+-]\\d{4})?";
    private static final Pattern GENERALIZED_TIME_PATTERN = Pattern.compile(GENERALIZED_TIME_REGEX);
    private static final String GENERALIZED_TIME_FORMAT = "yyyyMMddHHmmss'Z'"; // always use UTC

    private static final int[] BIT_STRING_MASK = { 128, 64, 32, 16, 8, 4, 2, 1 };

    /**
     * Decode an ASN.1 BIT STRING.
     *
     * @param buf
     *            the buffer containing the DER-encoded BIT STRING.
     * @return the bits in the BIT STRING
     */
    public static BitSet decodeBitString(ByteBuffer buf) {
        DerId id = DerId.decode(buf);
        if (!id.matches(DerId.TagClass.UNIVERSAL, DerId.EncodingType.PRIMITIVE, ASN1_BIT_STRING_TAG_NUM)
                && !id.matches(DerId.TagClass.UNIVERSAL, DerId.EncodingType.CONSTRUCTED, ASN1_BIT_STRING_TAG_NUM)) {
            throw new IllegalArgumentException("Expected BIT STRING identifier, received " + id);
        }
        int len = DerUtils.decodeLength(buf);
        if (buf.remaining() < len) {
            throw new IllegalArgumentException("Insufficient content for BIT STRING");
        }

        // If primitive encoding, then the initial octet is just used for padding
        if (id.getEncodingType() == DerId.EncodingType.PRIMITIVE) {
            buf.get();
            len--;
        }

        int nbits = len * 8;
        BitSet bits = new BitSet(nbits);
        for (int i = 0; i < len; i++) {
            short next = (short) (0xff & buf.get());
            int bitIndex = (i + 1) * 8 - 1;
            while (next != 0) {
                if ((next & 1) == 1) {
                    bits.set(bitIndex);
                }
                bitIndex--;
                next >>>= 1;
            }
        }
        return bits;
    }

    /**
     * Decode an ASN.1 GeneralizedTime.
     *
     * @param buf
     *            the DER-encoded GeneralizedTime
     * @return the data and time
     */
    public static Date decodeGeneralizedTime(ByteBuffer buf) {
        // GeneralizedTime ::= [UNIVERSAL 24] IMPLICIT VisibleString
        DerId id = DerId.decode(buf);
        if (!id.matches(DerId.TagClass.UNIVERSAL, DerId.EncodingType.PRIMITIVE, ASN1_GENERALIZED_TIME_TAG_NUM)) {
            throw new IllegalArgumentException("Expected GeneralizedTime identifier, received " + id);
        }
        int len = DerUtils.decodeLength(buf);
        if (buf.remaining() < len) {
            throw new IllegalArgumentException("Insufficient content for GeneralizedTime");
        }

        Date date;

        byte[] dst = new byte[len];
        buf.get(dst);
        String iso8601DateString = new String(dst);
        Matcher matcher = GENERALIZED_TIME_PATTERN.matcher(iso8601DateString);
        if (matcher.matches()) {
            Calendar cal = Calendar.getInstance();
            cal.clear();

            // Process yyyyMMddHHmmss
            cal.set(Calendar.YEAR, Integer.parseInt(matcher.group(1)));
            cal.set(Calendar.MONTH, Integer.parseInt(matcher.group(2)) - 1); // Calendar.MONTH is zero based
            cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(matcher.group(3)));
            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(matcher.group(4)));
            cal.set(Calendar.MINUTE, Integer.parseInt(matcher.group(5)));
            cal.set(Calendar.SECOND, Integer.parseInt(matcher.group(6)));

            // Process fractional seconds, if any
            String fracSecStr = matcher.group(7);
            if (fracSecStr != null) {
                cal.set(Calendar.MILLISECOND, (int) (Float.parseFloat(fracSecStr) * 1000));
            }

            // Process time zone, if any
            String tzStr = matcher.group(8);
            if (tzStr != null) {
                cal.setTimeZone(TimeZone.getTimeZone("Z".equals(tzStr) ? "GMT" : "GMT" + tzStr));
            }

            date = cal.getTime();
        } else {
            throw new IllegalArgumentException("Malformed GeneralizedTime " + iso8601DateString);
        }
        return date;
    }

    /**
     * Decode an ASN.1 IA5String.
     *
     * @param buf
     *            the DER-encoded IA5String
     * @return the string
     */
    public static String decodeIA5String(ByteBuffer buf) {
        DerId id = DerId.decode(buf);
        if (!id.matches(DerId.TagClass.UNIVERSAL, DerId.EncodingType.PRIMITIVE, ASN1_IA5STRING_TAG_NUM)) {
            throw new IllegalArgumentException("Expected IA5String identifier, received " + id);
        }
        int len = DerUtils.decodeLength(buf);
        if (buf.remaining() < len) {
            throw new IllegalArgumentException("Insufficient content for IA5String");
        }
        byte[] dst = new byte[len];
        buf.get(dst);
        return new String(dst);
    }

    /**
     * Decode an ASN.1 INTEGER.
     *
     * @param buf
     *            the buffer containing the DER-encoded INTEGER
     * @return the value of the INTEGER
     */
    public static int decodeInteger(ByteBuffer buf) {
        DerId id = DerId.decode(buf);
        if (!id.matches(DerId.TagClass.UNIVERSAL, DerId.EncodingType.PRIMITIVE, ASN1_INTEGER_TAG_NUM)) {
            throw new IllegalArgumentException("Expected INTEGER identifier, received " + id);
        }
        int len = DerUtils.decodeLength(buf);
        if (buf.remaining() < len) {
            throw new IllegalArgumentException("Insufficient content for INTEGER");
        }
        int value = 0;
        for (int i = 0; i < len; i++) {
            value = (value << 8) + (0xff & buf.get());
        }
        return value;
    }

    /**
     * Decode an ASN.1 OCTET STRING.
     *
     * @param buf
     *            the DER-encoded OCTET STRING
     * @return the octets
     */
    public static short[] decodeOctetString(ByteBuffer buf) {
        DerId id = DerId.decode(buf);
        if (!id.matches(DerId.TagClass.UNIVERSAL, ASN1_OCTET_STRING_TAG_NUM)) {
            throw new IllegalArgumentException("Expected OCTET STRING identifier, received " + id);
        }
        int len = DerUtils.decodeLength(buf);
        if (buf.remaining() < len) {
            throw new IllegalArgumentException("Insufficient content for OCTET STRING");
        }
        short[] dst = new short[len];
        for (int i = 0; i < len; i++) {
            dst[i] = (short) (0xff & buf.get());
        }
        return dst;
    }

    /**
     * Decode an ASN.1 SEQUENCE by reading the identifier and length octets. The remaining data in the buffer is the SEQUENCE.
     *
     * @param buf
     *            the DER-encoded SEQUENCE
     * @return the length of the SEQUENCE
     */
    public static int decodeSequence(ByteBuffer buf) {
        DerId id = DerId.decode(buf);
        if (!id.matches(DerId.TagClass.UNIVERSAL, DerId.EncodingType.CONSTRUCTED, ASN1_SEQUENCE_TAG_NUM)) {
            throw new IllegalArgumentException("Expected SEQUENCE identifier, received " + id);
        }
        int len = DerUtils.decodeLength(buf);
        if (buf.remaining() < len) {
            throw new IllegalArgumentException("Insufficient content for SEQUENCE");
        }
        return len;
    }

    /**
     * Encode an ASN.1 BIT STRING.
     *
     * @param value
     *            the value to be encoded
     * @param nbits
     *            the number of bits in the bit string
     * @param buf
     *            the buffer with space to the left of current position where the value will be encoded
     * @return the length of the encoded data
     */
    public static int encodeBitString(BitSet value, int nbits, ByteBuffer buf) {
        if (value == null || nbits < value.length()) {
            throw new IllegalArgumentException();
        }

        int pos = buf.position();
        int contentLength = (int) Math.ceil(nbits / 8.0d);
        for (int i = contentLength; i > 0; i--) {
            byte octet = 0;
            for (int j = (i - 1) * 8; j < i * 8; j++) {
                if (value.get(j)) {
                    octet |= BIT_STRING_MASK[j % 8];
                }
            }
            pos--;
            buf.put(pos, octet);
        }

        // Write out padding byte (primitive encoding)
        pos--;
        buf.put(pos, (byte) 0);
        contentLength++;

        buf.position(buf.position() - contentLength);
        int headerLength = DerUtils.encodeIdAndLength(DerId.TagClass.UNIVERSAL, DerId.EncodingType.PRIMITIVE,
                ASN1_BIT_STRING_TAG_NUM, contentLength, buf);
        return headerLength + contentLength;
    }

    /**
     * Encode an ASN.1 GeneralizedTime.
     *
     * @param date
     *            the date value
     * @param buf
     *            the buffer with space to the left of current position where the value will be encoded
     * @return the length of the encoded data
     */
    public static int encodeGeneralizedTime(Date date, ByteBuffer buf) {
        if (date == null) {
            throw new IllegalArgumentException();
        }

        int pos = buf.position();
        SimpleDateFormat format = new SimpleDateFormat(GENERALIZED_TIME_FORMAT);
        format.setTimeZone(TimeZone.getTimeZone("GMT")); // always use UTC
        String value = format.format(date);
        byte[] data = value.getBytes();
        for (int i = data.length - 1; i >= 0; i--) {
            pos--;
            buf.put(pos, data[i]);
        }
        buf.position(buf.position() - data.length);
        int headerLength = DerUtils.encodeIdAndLength(DerId.TagClass.UNIVERSAL, DerId.EncodingType.PRIMITIVE,
                ASN1_GENERALIZED_TIME_TAG_NUM, data.length, buf);
        return headerLength + data.length;
    }

    /**
     * Encode an ASN.1 IA5String.
     *
     * @param value
     *            the value to be encoded
     * @param buf
     *            the buffer with space to the left of current position where the value will be encoded
     * @return the length of the encoded data
     */
    public static int encodeIA5String(String value, ByteBuffer buf) {
        int pos = buf.position();
        byte[] data = (value == null) ? new byte[0] : value.getBytes();
        for (int i = data.length - 1; i >= 0; i--) {
            pos--;
            buf.put(pos, data[i]);
        }
        buf.position(buf.position() - data.length);
        int headerLength = DerUtils.encodeIdAndLength(DerId.TagClass.UNIVERSAL, DerId.EncodingType.PRIMITIVE,
                ASN1_IA5STRING_TAG_NUM, data.length, buf);
        return headerLength + data.length;
    }

    /**
     * Encode an ASN.1 INTEGER.
     *
     * @param value
     *            the value to be encoded
     * @param buf
     *            the buffer with space to the left of current position where the value will be encoded
     * @return the length of the encoded data
     */
    public static int encodeInteger(int value, ByteBuffer buf) {
        int pos = buf.position();
        int contentLength = 0;
        do {
            pos--;
            buf.put(pos, (byte) (value & 0xff));
            value >>>= 8;
            contentLength++;
        } while (value != 0);
        buf.position(buf.position() - contentLength);
        int headerLen = DerUtils.encodeIdAndLength(DerId.TagClass.UNIVERSAL, DerId.EncodingType.PRIMITIVE, ASN1_INTEGER_TAG_NUM,
                contentLength, buf);
        return headerLen + contentLength;
    }

    /**
     * Encode an ASN.1 OCTET STRING.
     *
     * @param octets
     *            the octets
     * @param buf
     *            the buffer with space to the left of current position where the value will be encoded
     * @return the length of the encoded data
     */
    public static int encodeOctetString(short[] octets, ByteBuffer buf) {
        if (octets == null) {
            octets = new short[0];
        }
        int pos = buf.position();
        for (int i = octets.length - 1; i >= 0; i--) {
            pos--;
            buf.put(pos, (byte) octets[i]);
        }
        buf.position(buf.position() - octets.length);
        int headerLength = DerUtils.encodeIdAndLength(DerId.TagClass.UNIVERSAL, DerId.EncodingType.PRIMITIVE,
                ASN1_OCTET_STRING_TAG_NUM, octets.length, buf);
        return headerLength + octets.length;
    }

    /**
     * Encode an ASN.1 SEQUENCE.
     *
     * @param contentLength
     *            the length of the SEQUENCE
     * @param buf
     *            the buffer with space to the left of current position where the value will be encoded
     * @return the length of the encoded data
     */
    public static int encodeSequence(int contentLength, ByteBuffer buf) {
        int headerLength = DerUtils.encodeIdAndLength(DerId.TagClass.UNIVERSAL, DerId.EncodingType.CONSTRUCTED,
                ASN1_SEQUENCE_TAG_NUM, contentLength, buf);
        return headerLength + contentLength;
    }

    /**
     * Size of an ASN.1 BIT STRING.
     *
     * @param value
     *            the BIT STRING value
     * @param nbits
     *            the number of bits in the bit string
     * @return the size of the encoded data
     */
    public static int sizeOfBitString(BitSet value, int nbits) {
        return DerUtils.sizeOf(ASN1_BIT_STRING_TAG_NUM, (int) Math.ceil(nbits / 8.0d) + 1); // +1 for padding
    }

    /**
     * Size of an ASN.1 GeneralizedTime.
     *
     * @param date
     *            the date and time
     * @return the size of the encoded data
     */
    public static int sizeOfGeneralizedTime(Date date) {
        return DerUtils.sizeOf(ASN1_GENERALIZED_TIME_TAG_NUM, GENERALIZED_TIME_FORMAT.length() - 2); // -2 for quotes
    }

    /**
     * Size of an ASN.1 IA5String.
     *
     * @param value
     *            the string
     * @return the size of the encoded data
     */
    public static int sizeOfIA5String(String value) {
        return DerUtils.sizeOf(ASN1_IA5STRING_TAG_NUM, (value == null) ? 0 : value.getBytes().length);
    }

    /**
     * Size of an ASN.1 INTEGER.
     *
     * @param value
     *            the integer value
     * @return the size of the encoded data
     */
    public static int sizeOfInteger(int value) {
        int contentLength = 0;
        do {
            value >>>= 8;
            contentLength++;
        } while (value != 0);
        return DerUtils.sizeOf(ASN1_INTEGER_TAG_NUM, contentLength);
    }

    /**
     * Size of an ASN.1 OCTET STRING.
     *
     * @param octets
     *            the octets
     * @return the size of the encoded data
     */
    public static int sizeOfOctetString(short[] octets) {
        return DerUtils.sizeOf(ASN1_OCTET_STRING_TAG_NUM, (octets == null) ? 0 : octets.length);
    }

    /**
     * Size of an ASN.1 SEQUENCE.
     *
     * @param length
     *            the length of the SEQUENCE data
     * @return the size of the encoded data
     */
    public static int sizeOfSequence(int length) {
        return DerUtils.sizeOf(ASN1_SEQUENCE_TAG_NUM, length);
    }

    private Asn1Utils() {
    }

}
