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

import java.nio.ByteBuffer;

public enum Encoding {

    TEXT {
        @Override
        public ByteBuffer encode(ByteBuffer buf) {
            return buf;
        }

        @Override
        public ByteBuffer decode(ByteBuffer buf, DecodingState state) {
            return buf;
        }
    },
    BINARY {
        @Override
        public ByteBuffer encode(ByteBuffer buf) {
            return buf;
        }

        @Override
        public ByteBuffer decode(ByteBuffer buf, DecodingState state) {
            return buf;
        }
    },
    BASE64 {
        @Override
        public ByteBuffer encode(ByteBuffer decoded) {
            if (decoded.hasArray()) {
                return encodeWithHeap(decoded);
            }
            else {
                return encodeWithDirect(decoded);
            }
        }

        private ByteBuffer encodeWithDirect(ByteBuffer decoded) {
            final int decodedSize = decoded.remaining();
            final int effectiveDecodedSize = (int) Math.ceil(decodedSize / 3.0f) * 3;
            final int decodedFragmentSize = (3 - (effectiveDecodedSize - decodedSize)) % 3;

            final int encodedArraySize = effectiveDecodedSize / 3 * 4;
            final byte[] encodedArray = new byte[encodedArraySize];
            int encodedArrayPosition = 0;

            int decodedArrayPosition = decoded.position();
            final int decodedArrayLimit = decoded.limit() - decodedFragmentSize;

            while (decodedArrayPosition < decodedArrayLimit) {
                int byte0 = decoded.get(decodedArrayPosition++) & 0xff;
                int byte1 = decoded.get(decodedArrayPosition++) & 0xff;
                int byte2 = decoded.get(decodedArrayPosition++) & 0xff;

                encodedArray[encodedArrayPosition++] = INDEXED[(byte0 >> 2) & 0x3f];
                encodedArray[encodedArrayPosition++] = INDEXED[((byte0 << 4) & 0x30)
                        | ((byte1 >> 4) & 0x0f)];
                encodedArray[encodedArrayPosition++] = INDEXED[((byte1 << 2) & 0x3c)
                        | ((byte2 >> 6) & 0x03)];
                encodedArray[encodedArrayPosition++] = INDEXED[byte2 & 0x3f];
            }

            switch (decodedFragmentSize) {
            case 0:
                break;
            case 1: {
                int byte0 = decoded.get(decodedArrayPosition++) & 0xff;

                encodedArray[encodedArrayPosition++] = INDEXED[(byte0 >> 2) & 0x3f];
                encodedArray[encodedArrayPosition++] = INDEXED[(byte0 << 4) & 0x30];
                encodedArray[encodedArrayPosition++] = PADDING_BYTE;
                encodedArray[encodedArrayPosition++] = PADDING_BYTE;
                break;
            }
            case 2: {
                int byte0 = decoded.get(decodedArrayPosition++) & 0xff;
                int byte1 = decoded.get(decodedArrayPosition++) & 0xff;

                encodedArray[encodedArrayPosition++] = INDEXED[(byte0 >> 2) & 0x3f];
                encodedArray[encodedArrayPosition++] = INDEXED[((byte0 << 4) & 0x30)
                        | ((byte1 >> 4) & 0x0f)];
                encodedArray[encodedArrayPosition++] = INDEXED[(byte1 << 2) & 0x3c];
                encodedArray[encodedArrayPosition++] = PADDING_BYTE;
                break;
            }
            default: {
                throw new IllegalArgumentException("Invalid base64 encoding");
            }
            }

            return ByteBuffer.wrap(encodedArray);
        }

        private ByteBuffer encodeWithHeap(ByteBuffer decoded) {
            final int decodedSize = decoded.remaining();
            final int effectiveDecodedSize = (int) Math.ceil(decodedSize / 3.0f) * 3;
            final int decodedFragmentSize = (3 - (effectiveDecodedSize - decodedSize)) % 3;

            final int encodedArraySize = effectiveDecodedSize / 3 * 4;
            final byte[] encodedArray = new byte[encodedArraySize];
            int encodedArrayPosition = 0;

            final byte[] decodedArray = decoded.array();
            final int decodedArrayOffset = decoded.arrayOffset();
            int decodedArrayPosition = decodedArrayOffset + decoded.position();
            final int decodedArrayLimit = decodedArrayOffset + decoded.limit()
                    - decodedFragmentSize;

            while (decodedArrayPosition < decodedArrayLimit) {
                int byte0 = decodedArray[decodedArrayPosition++] & 0xff;
                int byte1 = decodedArray[decodedArrayPosition++] & 0xff;
                int byte2 = decodedArray[decodedArrayPosition++] & 0xff;

                encodedArray[encodedArrayPosition++] = INDEXED[(byte0 >> 2) & 0x3f];
                encodedArray[encodedArrayPosition++] = INDEXED[((byte0 << 4) & 0x30)
                        | ((byte1 >> 4) & 0x0f)];
                encodedArray[encodedArrayPosition++] = INDEXED[((byte1 << 2) & 0x3c)
                        | ((byte2 >> 6) & 0x03)];
                encodedArray[encodedArrayPosition++] = INDEXED[byte2 & 0x3f];
            }

            switch (decodedFragmentSize) {
            case 0:
                break;
            case 1: {
                int byte0 = decodedArray[decodedArrayPosition++] & 0xff;

                encodedArray[encodedArrayPosition++] = INDEXED[(byte0 >> 2) & 0x3f];
                encodedArray[encodedArrayPosition++] = INDEXED[(byte0 << 4) & 0x30];
                encodedArray[encodedArrayPosition++] = PADDING_BYTE;
                encodedArray[encodedArrayPosition++] = PADDING_BYTE;
                break;
            }
            case 2: {
                int byte0 = decodedArray[decodedArrayPosition++] & 0xff;
                int byte1 = decodedArray[decodedArrayPosition++] & 0xff;

                encodedArray[encodedArrayPosition++] = INDEXED[(byte0 >> 2) & 0x3f];
                encodedArray[encodedArrayPosition++] = INDEXED[((byte0 << 4) & 0x30)
                        | ((byte1 >> 4) & 0x0f)];
                encodedArray[encodedArrayPosition++] = INDEXED[(byte1 << 2) & 0x3c];
                encodedArray[encodedArrayPosition++] = PADDING_BYTE;
                break;
            }
            default: {
                throw new IllegalArgumentException("Invalid base64 encoding");
            }
            }

            return ByteBuffer.wrap(encodedArray);
        }

        @Override
        public ByteBuffer decode(ByteBuffer encoded, DecodingState state) {
            if (encoded.hasArray()) {
                return decodeWithHeap(encoded, state);
            }
            else {
                return decodeWithDirect(encoded, state);
            }
        }

        private ByteBuffer decodeWithDirect(ByteBuffer encoded, DecodingState state) {
            final int length = encoded.remaining();

            if (length % 4 != 0) {
                throw new IllegalArgumentException("base64");
            }

            final byte[] decodedArray = new byte[length / 4 * 3];
            int decodedArrayOffset = 0;

            final int encodedArrayLimit = encoded.limit();
            for (int i = encoded.position(); i < encodedArrayLimit;) {
                // This would be faster reading an int (4 bytes) at a time
                int char0 = encoded.get(i++);
                int char1 = encoded.get(i++);
                int char2 = encoded.get(i++);
                int char3 = encoded.get(i++);

                int byte0 = mapped(char0);
                int byte1 = mapped(char1);
                int byte2 = mapped(char2);
                int byte3 = mapped(char3);

                decodedArray[decodedArrayOffset++] = (byte) (((byte0 << 2) & 0xfc) | ((byte1 >> 4) & 0x03));
                if (char2 != PADDING_BYTE) {
                    decodedArray[decodedArrayOffset++] = (byte) (((byte1 << 4) & 0xf0) | ((byte2 >> 2) & 0x0f));
                    if (char3 != PADDING_BYTE) {
                        decodedArray[decodedArrayOffset++] = (byte) (((byte2 << 6) & 0xc0) | (byte3 & 0x3f));
                    }
                }
            }
            return ByteBuffer.wrap(decodedArray, 0, decodedArrayOffset);
        }

        private ByteBuffer decodeWithHeap(ByteBuffer encoded, DecodingState state) {
            int length = encoded.remaining();

            if (length % 4 != 0) {
                throw new IllegalArgumentException("base64");
            }

            byte[] decodedArray = new byte[length / 4 * 3];
            int decodedArrayOffset = 0;

            byte[] encodedArray = encoded.array();
            int encodedArrayOffset = encoded.arrayOffset();
            int encodedArrayLimit = encodedArrayOffset + encoded.limit();
            for (int i = encodedArrayOffset + encoded.position(); i < encodedArrayLimit;) {
                int char0 = encodedArray[i++];
                int char1 = encodedArray[i++];
                int char2 = encodedArray[i++];
                int char3 = encodedArray[i++];

                int byte0 = mapped(char0);
                int byte1 = mapped(char1);
                int byte2 = mapped(char2);
                int byte3 = mapped(char3);

                decodedArray[decodedArrayOffset++] = (byte) (((byte0 << 2) & 0xfc) | ((byte1 >> 4) & 0x03));
                if (char2 != PADDING_BYTE) {
                    decodedArray[decodedArrayOffset++] = (byte) (((byte1 << 4) & 0xf0) | ((byte2 >> 2) & 0x0f));
                    if (char3 != PADDING_BYTE) {
                        decodedArray[decodedArrayOffset++] = (byte) (((byte2 << 6) & 0xc0) | (byte3 & 0x3f));
                    }
                }
            }
            return ByteBuffer.wrap(decodedArray, 0, decodedArrayOffset);
        }
    },
    UTF8 {
        @Override
        public ByteBuffer encode(ByteBuffer decoded) {
            return encodeBinaryAsText(decoded, true, false);
        }

        @Override
        public ByteBuffer decode(ByteBuffer encoded, DecodingState state) {
            return decodeTextAsBinary(encoded, state, true, false);
        }
    },
    UTF8_ESCAPE_ZERO_AND_NEWLINE {
        @Override
        public ByteBuffer encode(ByteBuffer decoded) {
            return encodeBinaryAsText(decoded, true, true);
        }

        @Override
        public ByteBuffer decode(ByteBuffer encoded, DecodingState state) {
            return decodeTextAsBinary(encoded, state, true, true);
        }
    },
    ESCAPE_ZERO_AND_NEWLINE {
        @Override
        public ByteBuffer encode(ByteBuffer decoded) {
            return encodeBinaryAsText(decoded, false, true);
        }

        @Override
        public ByteBuffer decode(ByteBuffer encoded, DecodingState state) {
            return decodeTextAsBinary(encoded, state, false, true);
        }
    };

    public abstract ByteBuffer encode(ByteBuffer buf);

    public abstract ByteBuffer decode(ByteBuffer buf, DecodingState state);

    /**
     * WARNING: this method will throw a runtime exception if there is incomplete data at the end of buf
     * (e.g. first byte of a two byte UTF8 character or escaped byte). For that use decode(IoBuffer, DecodingState).
     * @throws UnsupportedOperationException - if the last byte is part of an incomplete sequence
     */
    public ByteBuffer decode(ByteBuffer buf) {
        return decode(buf, DecodingState.NONE);
    }

    private static final byte[] INDEXED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
            .getBytes();
    private static final int PADDING_BYTE = (int) '=';

    private static int mapped(int ch) {
        if ((ch & 0x40) != 0) {
            if ((ch & 0x20) != 0) {
                // a(01100001)-z(01111010) -> 26-51
                assert ch >= 'a';
                assert ch <= 'z';
                return ch - 71;
            } else {
                // A(01000001)-Z(01011010) -> 0-25
                assert ch >= 'A';
                assert ch <= 'Z';
                return ch - 65;
            }
        } else if ((ch & 0x20) != 0) {
            if ((ch & 0x10) != 0) {
                if ((ch & 0x08) != 0 && (ch & 0x04) != 0) {
                    // =(00111101) -> 0
                    assert ch == '=';
                    return 0;
                }
                else {
                    // 0(00110000)-9(00111001) -> 52-61
                    assert ch >= '0';
                    assert ch <= '9';
                    return ch + 4;
                }
            } else {
                if ((ch & 0x04) != 0) {
                    // /(00101111) -> 63
                    assert ch == '/';
                    return 63;
                } else {
                    // +(00101011) -> 62
                    assert ch == '+';
                    return 62;
                }
            }
        } else {
            throw new IllegalArgumentException("Invalid BASE64 string");
        }
    }

    private static ByteBuffer encodeBinaryAsText(ByteBuffer decoded, boolean encodeAsUTF8, boolean escapeZeroAndNewline) {
        if (decoded.hasArray()) {
            return encodeBinaryAsTextWithHeap(decoded, encodeAsUTF8, escapeZeroAndNewline);
        }
        else {
            return encodeBinaryAsTextWithDirect(decoded, encodeAsUTF8, escapeZeroAndNewline);
        }
    }

    private static ByteBuffer encodeBinaryAsTextWithDirect(ByteBuffer decoded,
                                                           boolean encodeAsUTF8,
                                                           boolean escapeZeroAndNewline) {
        int decodedPosition = decoded.position();
        final int decodedInitialPosition = decodedPosition;
        final int decodedLimit = decoded.limit();

        byte[] encodedArray = null;
        int encodedArrayInsertionCount = 0;
        for (; decodedPosition < decodedLimit; decodedPosition++) {
            byte decodedValue = decoded.get(decodedPosition);

            if (escapeZeroAndNewline) {
                switch (decodedValue) {
                case 0x00:
                    // null byte, requires multi-byte escaped representation
                    encodedArray = supplyEncodedArray(decoded, decodedPosition, decodedLimit, encodedArray);

                    // 0x00 -> 0x7f 0x30 ('0')
                    encodedArrayInsertionCount = updateEncodedArray(decodedPosition, encodedArray,
                            encodedArrayInsertionCount, (byte) 0x7f, (byte) 0x30);

                    // advance to next byte
                    continue;
                case 0x0a:
                    // newline byte, requires multi-byte escaped representation
                    encodedArray = supplyEncodedArray(decoded, decodedPosition, decodedLimit, encodedArray);

                    // 0x0a -> 0x7f 0x6e ('n')
                    encodedArrayInsertionCount = updateEncodedArray(decodedPosition, encodedArray, encodedArrayInsertionCount,
                            (byte) 0x7f, (byte) 0x6e);

                    // advance to next byte
                    continue;
                case 0x0d:
                    // carriage return byte, requires multi-byte escaped representation
                    encodedArray = supplyEncodedArray(decoded, decodedPosition, decodedLimit, encodedArray);

                    // 0x0d -> 0x7f 0x72 ('r')
                    encodedArrayInsertionCount = updateEncodedArray(decodedPosition, encodedArray, encodedArrayInsertionCount,
                            (byte) 0x7f, (byte) 0x72);

                    // advance to next byte
                    continue;
                case 0x7f:
                    // escape prefix byte, requires multi-byte escaped representation
                    encodedArray = supplyEncodedArray(decoded, decodedPosition, decodedLimit, encodedArray);

                    // 0x7f -> 0x7f 0x7f
                    encodedArrayInsertionCount = updateEncodedArray(decodedPosition, encodedArray, encodedArrayInsertionCount,
                            (byte) 0x7f, (byte) 0x7f);

                    // advance to next byte
                    continue;
                }
            }

            if (encodeAsUTF8 && (decodedValue & 0x80) != 0) {
                // high bit set, requires multi-byte representation in UTF8
                encodedArray = supplyEncodedArray(decoded, decodedPosition, decodedLimit, encodedArray);

                byte encodedByte0 = (byte) ((((decodedValue & 0xff) >> 6) & 0x03) | 0xc0);
                byte encodedByte1 = (byte) (decodedValue & 0xbf);
                encodedArrayInsertionCount = updateEncodedArray(decodedPosition, encodedArray, encodedArrayInsertionCount,
                        encodedByte0, encodedByte1);
            }
            else if (encodedArray != null) {
                updateEncodedArray(decodedPosition, encodedArray, encodedArrayInsertionCount, decodedValue);
            }
        }

        return (encodedArray != null) ? ByteBuffer.wrap(encodedArray, decodedInitialPosition,
                                                        decoded.remaining() + encodedArrayInsertionCount) : decoded;
    }

    private static ByteBuffer encodeBinaryAsTextWithHeap(ByteBuffer decoded,
                                                         boolean encodeAsUTF8,
                                                         boolean escapeZeroAndNewline) {
        byte[] decodedArray = decoded.array();
        final int decodedArrayOffset = decoded.arrayOffset();
        int decodedArrayPosition = decodedArrayOffset + decoded.position();
        final int decodedArrayInitialPosition = decodedArrayPosition;
        final int decodedArrayLimit = decodedArrayOffset + decoded.limit();

        byte[] encodedArray = null;
        int encodedArrayInsertionCount = 0;
        for (; decodedArrayPosition < decodedArrayLimit; decodedArrayPosition++) {
            byte decodedValue = decodedArray[decodedArrayPosition];

            if (escapeZeroAndNewline) {
                switch (decodedValue) {
                case 0x00:
                    // null byte, requires multi-byte escaped representation
                    encodedArray = supplyEncodedArray(decodedArray, decodedArrayOffset, decodedArrayPosition,
                            decodedArrayLimit, encodedArray);

                    // 0x00 -> 0x7f 0x30 ('0')
                    encodedArrayInsertionCount = updateEncodedArray(decodedArrayPosition, encodedArray,
                            encodedArrayInsertionCount, (byte) 0x7f, (byte) 0x30);

                    // advance to next byte
                    continue;
                case 0x0a:
                    // newline byte, requires multi-byte escaped representation
                    encodedArray = supplyEncodedArray(decodedArray, decodedArrayOffset, decodedArrayPosition,
                            decodedArrayLimit, encodedArray);

                    // 0x0a -> 0x7f 0x6e ('n')
                    encodedArrayInsertionCount = updateEncodedArray(decodedArrayPosition, encodedArray,
                            encodedArrayInsertionCount, (byte) 0x7f, (byte) 0x6e);

                    // advance to next byte
                    continue;
                case 0x0d:
                    // carriage return byte, requires multi-byte escaped representation
                    encodedArray = supplyEncodedArray(decodedArray, decodedArrayOffset, decodedArrayPosition,
                            decodedArrayLimit, encodedArray);

                    // 0x0d -> 0x7f 0x72 ('r')
                    encodedArrayInsertionCount = updateEncodedArray(decodedArrayPosition, encodedArray,
                            encodedArrayInsertionCount, (byte) 0x7f, (byte) 0x72);

                    // advance to next byte
                    continue;
                case 0x7f:
                    // escape prefix byte, requires multi-byte escaped representation
                    encodedArray = supplyEncodedArray(decodedArray, decodedArrayOffset, decodedArrayPosition,
                            decodedArrayLimit, encodedArray);

                    // 0x7f -> 0x7f 0x7f
                    encodedArrayInsertionCount = updateEncodedArray(decodedArrayPosition, encodedArray,
                            encodedArrayInsertionCount, (byte) 0x7f, (byte) 0x7f);

                    // advance to next byte
                    continue;
                }
            }

            if (encodeAsUTF8 && (decodedValue & 0x80) != 0) {
                // high bit set, requires multi-byte representation in UTF8
                encodedArray = supplyEncodedArray(decodedArray, decodedArrayOffset, decodedArrayPosition,
                        decodedArrayLimit, encodedArray);

                byte encodedByte0 = (byte) ((((decodedValue & 0xff) >> 6) & 0x03) | 0xc0);
                byte encodedByte1 = (byte) (decodedValue & 0xbf);
                encodedArrayInsertionCount = updateEncodedArray(decodedArrayPosition, encodedArray,
                        encodedArrayInsertionCount, encodedByte0, encodedByte1);
            }
            else if (encodedArray != null) {
                updateEncodedArray(decodedArrayPosition, encodedArray, encodedArrayInsertionCount, decodedValue);
            }
        }

        return (encodedArray != null) ? ByteBuffer.wrap(encodedArray, decodedArrayInitialPosition,
                                                        decoded.remaining() + encodedArrayInsertionCount) : decoded;
    }

    private static byte[] supplyEncodedArray(byte[] decodedArray, int decodedArrayOffset, int decodedArrayPosition,
                                             int decodedArrayLimit, byte[] encodedArray) {
        if (encodedArray == null) {
            int decodedPosition = decodedArrayPosition - decodedArrayOffset;
            int decodedRemaining = decodedArrayLimit - decodedArrayPosition;
            encodedArray = new byte[decodedPosition + decodedRemaining * 2];
            System.arraycopy(decodedArray, decodedArrayOffset, encodedArray, 0, decodedPosition);
        }
        return encodedArray;
    }

    private static byte[] supplyEncodedArray(ByteBuffer decoded, int decodedPosition, int decodedLimit, byte[] encodedArray) {
        if (encodedArray == null) {
            int decodedRemaining = decodedLimit - decodedPosition;
            encodedArray = new byte[decodedPosition + decodedRemaining * 2];

            int originalPosition = decoded.position();
            decoded.position(0);
            decoded.get(encodedArray, 0, decodedPosition);
            decoded.position(originalPosition); // decoded.get() moves the position forward - bring it back
        }
        return encodedArray;
    }

    private static int updateEncodedArray(int decodedArrayPosition, byte[] encodedArray, int encodedArrayInsertionCount,
                                          byte byte0, byte byte1) {
        updateEncodedArray(decodedArrayPosition, encodedArray, encodedArrayInsertionCount, byte0);
        encodedArrayInsertionCount++;
        updateEncodedArray(decodedArrayPosition, encodedArray, encodedArrayInsertionCount, byte1);
        return encodedArrayInsertionCount;
    }

    private static void updateEncodedArray(int decodedArrayPosition, byte[] encodedArray, int encodedArrayInsertionCount,
                                           byte encodedByte) {
        encodedArray[decodedArrayPosition + encodedArrayInsertionCount] = encodedByte;
    }

    private static ByteBuffer decodeTextAsBinary(ByteBuffer encoded, DecodingState state, boolean decodeAsUTF8,
                                                       boolean unescapeZeroAndNewline) {
        if (encoded.hasArray()) {
            return decodeTextAsBinaryWithHeap(encoded, state, decodeAsUTF8, unescapeZeroAndNewline);
        }
        else {
            return decodeTextAsBinaryWithDirect(encoded, state, decodeAsUTF8, unescapeZeroAndNewline);
        }
    }

    private static ByteBuffer decodeTextAsBinaryWithHeap(ByteBuffer encoded, DecodingState state,
                                                               boolean decodeAsUTF8, boolean unescapeZeroAndNewline) {
        byte[] encodedArray = encoded.array();
        // Maintain offset, position and limit for the portion that interests us (from position() to limit())
        int encodedArrayOffset = encoded.arrayOffset() + encoded.position();
        int encodedArrayPosition = encodedArrayOffset;
        final int encodedArrayLimit = encoded.arrayOffset() + encoded.limit();

        byte[] decodedArray = null;
        int encodedArrayInsertionCount = 0;
        Byte previousRemainingByte = (Byte) state.get(); // first byte of a byte pair left over from end of last packet, or null
        if (previousRemainingByte != null) {
            decodedArray = new byte[encoded.remaining()];
            // pretend the extra byte from the last packet is just before the first byte of this packet
            encodedArrayPosition--;
            encodedArrayOffset--;
        }
        Byte remainingByte = null;

        for (; encodedArrayPosition < encodedArrayLimit; encodedArrayPosition++) {
            byte encodedByte1 = encodedArrayPosition == -1 ? previousRemainingByte : encodedArray[encodedArrayPosition];
            if (decodeAsUTF8 && (encodedByte1 & 0x80) != 0) {
                // high bit set, consumes multi-byte representation in UTF8
                decodedArray = supplyDecodedArray(encodedArray, encodedArrayOffset, encodedArrayPosition,
                        encodedArrayLimit, decodedArray);
                if (++encodedArrayPosition >= encodedArrayLimit) {
                    remainingByte = encodedByte1;
                    encodedArrayInsertionCount++;
                    break;
                }
                byte encodedByte2 = encodedArray[encodedArrayPosition];
                byte decodedByte = (byte) ((encodedByte1 << 6) | (encodedByte2 & 0x3f));
                encodedArrayInsertionCount = updateDecodedArray(encodedArrayOffset, encodedArrayPosition,
                        decodedArray, encodedArrayInsertionCount, decodedByte);
            }
            else {
                if (unescapeZeroAndNewline && encodedByte1 == 0x7f) {
                    decodedArray = supplyDecodedArray(encodedArray, encodedArrayOffset, encodedArrayPosition,
                            encodedArrayLimit, decodedArray);

                    if (++encodedArrayPosition >= encodedArrayLimit) {
                        remainingByte = encodedByte1;
                        encodedArrayInsertionCount++;
                        break;
                    }
                    byte encodedByte2 = encodedArray[encodedArrayPosition];
                    switch (encodedByte2) {
                    case 0x30:
                        // '0'
                        encodedArrayInsertionCount = updateDecodedArray(encodedArrayOffset, encodedArrayPosition,
                                decodedArray, encodedArrayInsertionCount, (byte) 0x00);
                        break;
                    case 0x6e:
                        // 'n'
                        encodedArrayInsertionCount = updateDecodedArray(encodedArrayOffset, encodedArrayPosition,
                                decodedArray, encodedArrayInsertionCount, (byte) 0x0a);
                        break;
                    case 0x72:
                        // 'r'
                        encodedArrayInsertionCount = updateDecodedArray(encodedArrayOffset, encodedArrayPosition,
                                decodedArray, encodedArrayInsertionCount, (byte) 0x0d);
                        break;
                    default:
                        // tolerate 0x7f [not 0 or n] -> [not 0 or n]
                        encodedArrayInsertionCount = updateDecodedArray(encodedArrayOffset, encodedArrayPosition,
                                decodedArray, encodedArrayInsertionCount, encodedByte2);
                        break;
                    }
                }
                else if (decodedArray != null) {
                    decodedArray[encodedArrayPosition - encodedArrayInsertionCount - encodedArrayOffset] = encodedByte1;
                }
            }
        }
        state.set(remainingByte);
        int decodedLength = encoded.remaining() + (previousRemainingByte != null ? 1 : 0) - encodedArrayInsertionCount;
        return (decodedArray != null) ? ByteBuffer.wrap(decodedArray, 0, decodedLength) : encoded;
    }

    private static ByteBuffer decodeTextAsBinaryWithDirect(ByteBuffer encoded, DecodingState state,
                                                                 boolean decodeAsUTF8, boolean unescapeZeroAndNewline) {
        // Maintain offset, position and limit for the portion that interests us (from position() to limit())
        int encodedArrayOffset = encoded.position();
        int encodedArrayPosition = encodedArrayOffset;
        final int encodedArrayLimit = encoded.limit();

        byte[] decodedArray = null;
        int encodedArrayInsertionCount = 0;
        Byte previousRemainingByte = (Byte) state.get(); // first byte of a byte pair left over from end of last packet, or null
        if (previousRemainingByte != null) {
            decodedArray = new byte[encoded.remaining()];
            // pretend the extra byte from the last packet is just before the first byte of this packet
            encodedArrayPosition--;
            encodedArrayOffset--;
        }
        Byte remainingByte = null;

        for (; encodedArrayPosition < encodedArrayLimit; encodedArrayPosition++) {
            byte encodedByte1 = encodedArrayPosition == -1 ? previousRemainingByte : encoded.get(encodedArrayPosition);
            if (decodeAsUTF8 && (encodedByte1 & 0x80) != 0) {
                // high bit set, consumes multi-byte representation in UTF8
                decodedArray = supplyDecodedArray(encoded, encodedArrayOffset, encodedArrayPosition,
                        encodedArrayLimit, decodedArray);
                if (++encodedArrayPosition >= encodedArrayLimit) {
                    remainingByte = encodedByte1;
                    encodedArrayInsertionCount++;
                    break;
                }
                byte encodedByte2 = encoded.get(encodedArrayPosition);
                byte decodedByte = (byte) ((encodedByte1 << 6) | (encodedByte2 & 0x3f));
                encodedArrayInsertionCount = updateDecodedArray(encodedArrayOffset, encodedArrayPosition, decodedArray,
                        encodedArrayInsertionCount, decodedByte);
            }
            else {
                if (unescapeZeroAndNewline && encodedByte1 == 0x7f) {
                    decodedArray = supplyDecodedArray(encoded, encodedArrayOffset, encodedArrayPosition,
                            encodedArrayLimit, decodedArray);

                    if (++encodedArrayPosition >= encodedArrayLimit) {
                        remainingByte = encodedByte1;
                        encodedArrayInsertionCount++;
                        break;
                    }
                    byte encodedByte2 = encoded.get(encodedArrayPosition);
                    switch (encodedByte2) {
                    case 0x30:
                        // '0'
                        encodedArrayInsertionCount = updateDecodedArray(encodedArrayOffset, encodedArrayPosition,
                                decodedArray, encodedArrayInsertionCount, (byte) 0x00);
                        break;
                    case 0x6e:
                        // 'n'
                        encodedArrayInsertionCount = updateDecodedArray(encodedArrayOffset, encodedArrayPosition,
                                decodedArray, encodedArrayInsertionCount, (byte) 0x0a);
                        break;
                    case 0x72:
                        // 'r'
                        encodedArrayInsertionCount = updateDecodedArray(encodedArrayOffset, encodedArrayPosition,
                                decodedArray, encodedArrayInsertionCount, (byte) 0x0d);
                        break;
                    default:
                        // tolerate 0x7f [not 0 or n] -> [not 0 or n]
                        encodedArrayInsertionCount = updateDecodedArray(encodedArrayOffset, encodedArrayPosition,
                                decodedArray, encodedArrayInsertionCount, encodedByte2);
                        break;
                    }
                }
                else if (decodedArray != null) {
                    decodedArray[encodedArrayPosition - encodedArrayInsertionCount - encodedArrayOffset] = encodedByte1;
                }
            }
        }
        state.set(remainingByte);
        int decodedLength = encoded.remaining() + (previousRemainingByte != null ? 1 : 0) - encodedArrayInsertionCount;
        return (decodedArray != null) ? ByteBuffer.wrap(decodedArray, 0, decodedLength) : encoded;
    }

    private static int updateDecodedArray(int encodedArrayOffset, int encodedArrayPosition, byte[] decodedArray,
                                          int encodedArrayInsertionCount, byte decodedByte) {
        encodedArrayInsertionCount++;
        decodedArray[encodedArrayPosition - encodedArrayInsertionCount - encodedArrayOffset] = decodedByte;
        return encodedArrayInsertionCount;
    }

    private static byte[] supplyDecodedArray(byte[] encodedArray, int encodedArrayOffset, int encodedArrayPosition,
                                             int encodedArrayLimit, byte[] decodedArray) {
        if (decodedArray == null) {
            int encodedPosition = encodedArrayPosition - encodedArrayOffset;
            int encodedRemaining = encodedArrayLimit - encodedArrayPosition;
            decodedArray = new byte[encodedPosition + encodedRemaining];
            System.arraycopy(encodedArray, encodedArrayOffset, decodedArray, 0, encodedPosition);
        }
        return decodedArray;
    }

    private static byte[] supplyDecodedArray(ByteBuffer encoded, int encodedArrayOffset, int encodedArrayPosition,
                                             int encodedArrayLimit, byte[] decodedArray) {
        if (decodedArray == null) {
            int encodedPosition = encodedArrayPosition - encodedArrayOffset;
            int encodedRemaining = encodedArrayLimit - encodedArrayPosition;
            decodedArray = new byte[encodedPosition + encodedRemaining];
            encoded.get(decodedArray, 0, encodedPosition);
            // back it up - get() on previous line consumes bytes from encoded
            encoded.position(encoded.position() - encodedPosition);
        }
        return decodedArray;
    }
}
