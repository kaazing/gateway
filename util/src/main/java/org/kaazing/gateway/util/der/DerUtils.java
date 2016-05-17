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

import java.nio.ByteBuffer;

/**
 * Utilities for working with DER-encoded messages.
 */
public final class DerUtils {
    private DerUtils() {
    }

    /**
     * Decode octets and extract the length information from a DER-encoded message.
     *
     * @param buf
     *            the DER-encoded data buffer with position immediately after the DER-identifier octets
     * @return the length of the DER-encoded contents
     */
    public static int decodeLength(ByteBuffer buf) {
        if (buf == null || buf.remaining() == 0) {
            throw new IllegalArgumentException("Null or empty buffer");
        }

        int len = 0;

        byte first = buf.get();
        if (first >> 7 == 0) {
            // Bit 8 is zero: Short form
            len = first & 0x7f;
        } else {
            // Bit 8 is one: Long form
            // Bit 7-1 represents number of subsequent octets
            int numOctets = first & 0x7f;
            if (buf.remaining() < numOctets) {
                throw new IllegalArgumentException("Insufficient data to decode long-form DER length");
            }

            // Combine subsequent octets to compute length
            for (int i = 0; i < numOctets; i++) {
                len = (len << 8) + (0xff & buf.get());
            }
        }

        return len;
    }

    /**
     * Decodes ID and length octets from a DER-encoded message.
     *
     * @param buf
     *            the buffer with the DER-encoded message
     * @return the ID
     */
    public static DerId decodeIdAndLength(ByteBuffer buf) {
        DerId id = DerId.decode(buf);
        DerUtils.decodeLength(buf);
        return id;
    }

    /**
     * DER-encode content into a provided buffer.
     *
     * @param tagClass
     *            the tag class
     * @param encodingType
     *            the tag encoding type
     * @param tagNumber
     *            the tag number
     * @param contentLength
     *            the length of the content (presumed to be in the buffer)
     * @param buf
     *            the buffer with sufficient space before the current position to add the length and identifier octets
     * @return the length of the encoded data for the ID and length octets
     */
    public static int encodeIdAndLength(DerId.TagClass tagClass, DerId.EncodingType encodingType, int tagNumber,
            int contentLength, ByteBuffer buf) {
        int origPos = buf.position();
        int pos = buf.position();

        // Write out length octets
        if (contentLength < 0) {
            throw new IllegalArgumentException("Invalid content length " + contentLength);
        } else if (contentLength <= 0x7f) {
            // Definite form, single octet
            pos--;
            buf.put(pos, (byte) contentLength);
        } else {
            // Definite form, multiple octets
            int lenOctetCount = 0;
            while (contentLength != 0) {
                pos--;
                buf.put(pos, (byte) (contentLength & 0xff));
                contentLength >>>= 8;
                lenOctetCount++;
            }
            pos--;
            buf.put(pos, (byte) (0x80 | lenOctetCount));
        }

        // Write out identifier octets {
        if (tagNumber < 0) {
            throw new IllegalArgumentException("Invalid tag number " + tagNumber);
        } else {
            byte firstOctet = 0;

            // Handle tag class
            switch (tagClass) {
            case UNIVERSAL:
                break;
            case APPLICATION:
                firstOctet = 0x40;
                break;
            case CONTEXT_SPECIFIC:
                firstOctet = (byte) 0x80;
                break;
            case PRIVATE:
                firstOctet = (byte) 0xc0;
            }

            // Handle encoding type
            switch (encodingType) {
            case PRIMITIVE:
                break;
            case CONSTRUCTED:
                firstOctet |= 0x20;
            }

            // Handle tag number
            if (tagNumber <= 30) {
                firstOctet |= tagNumber;
                pos--;
                buf.put(pos, firstOctet);
            } else {
                boolean last = true;
                while (tagNumber != 0) {
                    byte octet = (byte) (tagNumber & 0x7f);
                    pos--;
                    if (last) {
                        buf.put(pos, octet);
                        last = false;
                    } else {
                        buf.put(pos, (byte) (octet | 0x80));
                    }
                    tagNumber >>>= 8;
                }
                firstOctet |= 0x1f;
                pos--;
                buf.put(pos, firstOctet);
            }
        }

        // skip (origPos - pos) bytes
        buf.position(pos);
        return origPos - pos;
    }

    /**
     * Computes the DER-encoded size of content with a specified tag number.
     *
     * @param tagNumber
     *            the DER tag number in the identifier
     * @param contentLength
     *            the length of the content in bytes
     * @return
     */
    public static int sizeOf(int tagNumber, int contentLength) {
        if (tagNumber < 0 || contentLength < 0) {
            throw new IllegalArgumentException("Invalid tagNumber/contentLength: " + tagNumber + ", " + contentLength);
        }

        int len = 0;

        // ID octets
        if (tagNumber <= 30) {
            len++; // single octet
        } else {
            len = len + 1 + (int) Math.ceil((1 + Integer.numberOfTrailingZeros(Integer.highestOneBit(tagNumber))) / 7.0d);
        }

        // Length octets (TODO: indefinite form)
        if (contentLength <= 0x7f) {
            len++; // definite form, single octet
        } else {
            len = len + 1 + (int) Math.ceil((1 + Integer.numberOfTrailingZeros(Integer.highestOneBit(contentLength))) / 8.0d);
        }

        // Content octets
        len += contentLength;

        return len;
    }
}
