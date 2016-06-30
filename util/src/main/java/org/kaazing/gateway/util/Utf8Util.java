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

import static java.lang.Character.charCount;
import static java.lang.Character.codePointAt;
import static java.lang.String.format;

import java.io.IOException;
import java.nio.ByteBuffer;

// Copied from
// https://github.com/kaazing/netx/blob/develop/ws/src/main/java/org/kaazing/netx/ws/internal/util/Utf8Util.java
public final class Utf8Util {
    public static final int INVALID_UTF8 = -1;

    private static final String MSG_INVALID_CODEPOINT = "Invalid UTF-16 codepoint %c";

    private Utf8Util() {
    }

    public static int byteCountUTF8(char[] cbuf, int offset, int length) throws IOException {
        int count = 0;
        while (offset < length) {
            int codePoint = codePointAt(cbuf, offset);
            count += byteCountUTF8(codePoint);
            offset += charCount(codePoint);
        }
        return count;
    }

    public static int byteCountUTF8(int codePoint) throws IOException {
        if ((codePoint | 0x7f) == 0x7f) {
            return 1;
        }
        else if ((codePoint | 0x07ff) == 0x07ff) {
            return 2;
        }
        else if ((codePoint | 0xffff) == 0xffff) {
            return 3;
        }
        else if ((codePoint | 0x1fffff) == 0x1fffff) {
            return 4;
        }
        else {
            throw new IOException("Invalid UTF-8 code point. UTF-8 code point cannot span for more than 4 bytes.");
        }
    }

    public static int initialDecodeUTF8(int remainingWidth, int encodedByte) throws IOException {
        switch (remainingWidth) {
        case 0:
            return encodedByte & 0x7f;
        case 1:
            return encodedByte & 0x1f;
        case 2:
            return encodedByte & 0x0f;
        case 3:
            return encodedByte & 0x07;
        default:
            throw new IOException("Invalid UTF-8 byte sequence. UTF-8 char cannot span for more than 4 bytes.");
        }
    }

    public static int remainingDecodeUTF8(int decodedBytes, int remainingWidth, int encodedByte) throws IOException {
        switch (remainingWidth) {
        case 3:
        case 2:
        case 1:
            return (decodedBytes << 6) | (encodedByte & 0x3f);
        case 0:
            return decodedBytes;
        default:
            throw new IOException("Invalid UTF-8 byte sequence. UTF-8 char cannot span for more than 4 bytes.");
        }
    }

    public static int remainingBytesUTF8(int leadingByte) {
        if ((leadingByte & 0x80) == 0) {
            return 0;
        }

        for (byte i = 0; i < 7; i++) {
            int bitMask = 1 << (7 - i);

            if ((leadingByte & bitMask) != 0) {
                continue;
            }
            else {
                switch (i) {
                case 0:
                case 7:
                    throw new IllegalStateException(format("Invalid UTF-8 sequence leader byte: 0x%02x", leadingByte));
                default:
                    return i - 1;
                }
            }
        }

        throw new IllegalStateException(String.format("Invalid UTF-8 sequence leader byte: 0x%02x", leadingByte));
    }

    public static boolean validBytesUTF8(byte[] input) {
        for (int index = 0; index < input.length;) {
            byte leadingByte = input[index++];
            if ((leadingByte & 0xc0) == 0x80) {
                return false;
            }
            int remaining = remainingBytesUTF8(leadingByte);
            switch (remaining) {
            case 0:
                break;
            default:
                while (remaining-- > 0) {
                    if ((input[index++] & 0xc0) != 0x80) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public static int validateUTF8(ByteBuffer buffer, int offset, int length, ErrorHandler errorHandler) {
        for (int index = 0; index < length; index++) {
            byte leadingByte = buffer.get(offset + index);
            final int expectedLen;
            int codePoint;
            if ((leadingByte & 0x80) == 0) {
                continue;
            }
            if ((leadingByte & 0xff) > 0xf4) {
                errorHandler.handleError(format("Invalid leading byte: %x", leadingByte));
                return INVALID_UTF8;
            }
            if ((leadingByte & 0xE0) == 0xC0) {
                expectedLen = 2;
                codePoint = leadingByte & 0x1F;
                if (codePoint < 2) {
                    errorHandler.handleError(format("Overlong encoding: %x%x", leadingByte, buffer.get(offset + index + 1)));
                    return INVALID_UTF8;
                }
            } else if ((leadingByte & 0xF0) == 0xE0) {
                expectedLen = 3;
                codePoint = leadingByte & 0x0F;
            } else if ((leadingByte & 0xF8) == 0xF0) {
                expectedLen = 4;
                codePoint = leadingByte & 0x07;
            } else {
                errorHandler.handleError(format("Value exceeds Unicode limit: %x", leadingByte));
                return INVALID_UTF8;
            }
            int characterStartIndex = index;
            int remainingLen = expectedLen;
            while (--remainingLen > 0) {
                if (++index >= length) {
                    // incomplete character at end
                    return length - characterStartIndex;
                }
                byte nextByte = buffer.get(offset + index);
                if ((nextByte & 0xC0) != 0x80) {
                    errorHandler.handleError(format("Invalid continuation byte: %x", nextByte));
                    return INVALID_UTF8;
                }
                codePoint = (codePoint << 6) | (nextByte & 0x3F);
                if (codePoint > 0x10FFFF) { // maximum Unicode code point
                    return INVALID_UTF8;
                }
            }

            try {
                if (expectedLen > byteCountUTF8(codePoint)) {
                    errorHandler.handleError(format("Overlong encoding starting at byte %x postion %d", leadingByte,
                            characterStartIndex));
                    return INVALID_UTF8;
                }
            } catch (IOException e) {
                errorHandler.handleError(e.getMessage());
                return INVALID_UTF8;
            }
        }
        return 0;
    }

    public static boolean validBytesUTF8(ByteBuffer buf, int offset, int limit) {
        for (int index = offset; index < limit;) {
            byte leadingByte = buf.get(index++);
            if ((leadingByte & 0xc0) == 0x80) {
                return false;
            }
            int remaining = remainingBytesUTF8(leadingByte);
            switch (remaining) {
            case 0:
                break;
            default:
                while (remaining-- > 0) {
                    if ((buf.get(index++) & 0xc0) != 0x80) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Custom UTF-8 encoding. Generates UTF-8 byte sequence for the specified char[]. The UTF-8 byte sequence is
     * encoded in the specified ByteBuffer.
     *
     * @param srcBuf         the source char[] to be encoded as UTF-8 byte sequence
     * @param srcOffset      offset in the char[] from where the conversion to UTF-8 should begin
     * @param srcLength      the number of chars to be encoded as UTF-8 bytes
     * @param dest           the destination ByteBuffer
     * @param destOffset     offset in the ByteBuffer starting where the encoded UTF-8 bytes should be copied
     * @return the number of bytes encoded
     */
    public static int charstoUTF8Bytes(char[] srcBuf, int srcOffset, int srcLength, ByteBuffer dest, int destOffset) {
        int destMark = destOffset;

        for (int i = srcOffset; i < srcLength;) {
            char ch = srcBuf[i];

            if (ch < 0x0080) {
                dest.put(destOffset++, (byte) ch);
            }
            else if (ch < 0x0800) {
                dest.put(destOffset++, (byte) (0xc0 | (ch >> 6)));
                dest.put(destOffset++, (byte) (0x80 | ((ch >> 0) & 0x3f)));
            }
            else if (((ch >= 0x0800) && (ch <= 0xD7FF)) ||
                     ((ch >= 0xE000) && (ch <= 0xFFFF))) {
                dest.put(destOffset++, (byte) (0xe0 | (ch >> 12)));
                dest.put(destOffset++, (byte) (0x80 | ((ch >> 6) & 0x3F)));
                dest.put(destOffset++, (byte) (0x80 | ((ch >> 0) & 0x3F)));
            }
            else if ((ch >= Character.MIN_SURROGATE) && (ch <= Character.MAX_SURROGATE)) {  // Surrogate pair
                if (i == srcBuf.length) {
                    throw new IllegalStateException(format(MSG_INVALID_CODEPOINT, ch));
                }

                char ch1 = ch;
                char ch2 = srcBuf[++i];

                if (ch1 > Character.MAX_HIGH_SURROGATE) {
                    throw new IllegalStateException(format(MSG_INVALID_CODEPOINT, ch1));
                }

                int codePoint = Character.toCodePoint(ch1, ch2);
//                int codePoint = (((ch1 & 0x03FF) << 10) | (ch2 & 0x03FF)) + Character.MIN_SUPPLEMENTARY_CODE_POINT;

                dest.put(destOffset++, (byte) (0xf0 | (codePoint >> 18)));
                dest.put(destOffset++, (byte) (0x80 | ((codePoint >> 12) & 0x3F)));
                dest.put(destOffset++, (byte) (0x80 | ((codePoint >> 6) & 0x3F)));
                dest.put(destOffset++, (byte) (0x80 | ((codePoint >> 0) & 0x3F)));
            }
            else {
                throw new IllegalStateException(format(MSG_INVALID_CODEPOINT, ch));
            }

            i++;
        }

        return destOffset - destMark;
    }
}
