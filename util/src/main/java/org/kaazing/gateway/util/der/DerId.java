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
 * Representation of the identifier in a DER-encoded message.
 */
public class DerId {
    /**
     * Tag classes in a DER identifier.
     */
    public enum TagClass {
        UNIVERSAL, APPLICATION, CONTEXT_SPECIFIC, PRIVATE
    }

    /**
     * Encoding types in a DER identifier.
     */
    public enum EncodingType {
        PRIMITIVE, CONSTRUCTED
    }

    private TagClass tagClass;
    private EncodingType encodingType;
    private int tagNumber;

    DerId() {
    }

    /**
     * Gets the tag class.
     *
     * @return the tag class
     */
    public TagClass getTagClass() {
        return tagClass;
    }

    /**
     * Gets the encoding type.
     *
     * @return the encoding type
     */
    public EncodingType getEncodingType() {
        return encodingType;
    }

    /**
     * Gets the tag number.
     *
     * @return the tag number
     */
    public int getTagNumber() {
        return tagNumber;
    }

    @Override
    public String toString() {
        return "DerId [tagClass=" + getTagClass() +
                ", encodingType=" + getEncodingType() +
                ", tagNumber=" + getTagNumber() + "]";
    }

    /**
     * Decodes identifier octets from a DER-encoded message.
     *
     * @param buf
     *            the buffer containing the DER-encoded message
     * @return the DER identifier
     */
    public static DerId decode(ByteBuffer buf) {
        if (buf == null || buf.remaining() == 0) {
            throw new IllegalArgumentException("Null or empty buffer");
        }

        DerId id = new DerId();

        byte first = buf.get(); // first octet

        // Bits 8 and 7 represent the tag class
        id.tagClass = TagClass.values()[(first & 0xc0) >> 6];

        // Bit 6 represents the encoding type
        id.encodingType = EncodingType.values()[(first & 0x20) >> 5];

        // Bits 5 through 1 represent the tag number if tag number is less than 30.
        // Bits 5 through 1 are set to 1 if the tag number is greater than 30.
        int tagNum = first & 0x1f;
        if (tagNum == 0x1F) {
            // Tag number greater than 30
            if (buf.remaining() == 0) {
                throw new IllegalArgumentException("Insufficient data to decode tag number greater than 30");
            }
            tagNum = 0;
            while (buf.hasRemaining()) {
                byte octet = buf.get();
                tagNum = (tagNum << 7) + (octet & 0x7f);
                if ((octet >> 7) == 0) {
                    break;
                }
            }
        }
        id.tagNumber = tagNum;

        return id;
    }

    /**
     * Tests whether this DER identifier matches the specified tag class, encoding type and tag number.
     *
     * @param tagClass
     *            the tag class to match against
     * @param encodingType
     *            the encoding type to match against
     * @param tagNumber
     *            the tag number to match against
     * @return whether there's a match
     */
    public boolean matches(TagClass tagClass, EncodingType encodingType, int tagNumber) {
        return this.tagClass == tagClass && this.encodingType == encodingType && this.tagNumber == tagNumber;
    }

    /**
     * Tests whether this DER identifier matches the specified tag class and tag number.
     * (Encoding type could be either one of the encoding types).
     *
     * @param tagClass
     *            the tag class to match against
     * @param tagNumber
     *            the tag number to match against
     * @return whether there's a match
     */
    public boolean matches(TagClass tagClass, int tagNumber) {
        return this.tagClass == tagClass && this.tagNumber == tagNumber;
    }
}
