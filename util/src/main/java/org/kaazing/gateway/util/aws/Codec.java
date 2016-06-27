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
package org.kaazing.gateway.util.aws;

import java.nio.ByteBuffer;

public class Codec {
    protected Codec() {
    }

    // HEX

    private static final byte[] TO_HEX =
            new byte[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static String encodeHexString(byte[] data) {
        byte[] out = encodeHex(data);
        return new String(out);
    }

    public static byte[] encodeHex(byte[] data) {
        int len = data.length;
        byte[] out = new byte[len << 1];
        byte cur;
        int idx;
        for (int i = 0; i < len; i++) {
            cur = data[i];
            idx = i << 1;
            out[idx] = TO_HEX[(cur >> 4) & 0xF];
            out[idx + 1] = TO_HEX[cur & 0xF];
        }
        return out;
    }

    // Base64

    private static final byte[] TO_BASE64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes();
    private static final byte BASE64_PADDING_BYTE = (byte) '=';

    public static String base64Decode(String data) {
        if (data == null) {
            return null;
        }

        return decodeBase64(data).asCharBuffer().toString();
    }

    public static String base64Encode(byte[] data) {
        if (data == null) {
            return null;
        }

        return encodeBase64String(ByteBuffer.wrap(data));
    }

    public static String encodeBase64String(ByteBuffer in) {
        int length = in.remaining();
        ByteBuffer buf = ByteBuffer.allocate((int) Math.ceil(length / 3.0) * 4);
        for (int remaining = in.remaining(); remaining > 0;) {
            switch (remaining) {
            case 1: {
                short byte0 = in.get();
                remaining--;

                buf.put(TO_BASE64[(byte0 >> 2) & 0x3f]);
                buf.put(TO_BASE64[(byte0 << 4) & 0x30]);
                buf.put(BASE64_PADDING_BYTE);
                buf.put(BASE64_PADDING_BYTE);
                break;
            }
            case 2: {
                short byte0 = in.get();
                short byte1 = in.get();
                remaining -= 2;

                buf.put(TO_BASE64[(byte0 >> 2) & 0x3f]);
                buf.put(TO_BASE64[((byte0 << 4) & 0x30) | ((byte1 >> 4) & 0x0f)]);
                buf.put(TO_BASE64[(byte1 << 2) & 0x3c]);
                buf.put(BASE64_PADDING_BYTE);
                break;
            }
            default: {
                short byte0 = in.get();
                short byte1 = in.get();
                short byte2 = in.get();
                remaining -= 3;

                buf.put(TO_BASE64[(byte0 >> 2) & 0x3f]);
                buf.put(TO_BASE64[((byte0 << 4) & 0x30) | ((byte1 >> 4) & 0x0f)]);
                buf.put(TO_BASE64[((byte1 << 2) & 0x3c) | ((byte2 >> 6) & 0x03)]);
                buf.put(TO_BASE64[byte2 & 0x3f]);
                break;
            }
            }
        }

        buf.flip();
        buf = buf.slice();
        return new String(buf.array());
    }

    public static ByteBuffer decodeBase64(String input) {
        if (input == null) {
            return null;
        }
        ByteBuffer in = ByteBuffer.wrap(input.getBytes());
        int length = in.remaining();

        if (length % 4 != 0) {
            throw new IllegalArgumentException("base64");
        }

        ByteBuffer buf = ByteBuffer.allocate(length / 4 * 3);
        for (int remaining = in.remaining(); remaining > 0;) {
            byte char0 = in.get();
            byte char1 = in.get();
            byte char2 = in.get();
            byte char3 = in.get();
            remaining -= 4;

            byte index0 = mapped(char0);
            byte index1 = mapped(char1);
            byte index2 = mapped(char2);
            byte index3 = mapped(char3);

            buf.put((byte) (((index0 << 2) & 0xfc) | ((index1 >> 4) & 0x03)));
            if (char2 != BASE64_PADDING_BYTE) {
                buf.put((byte) (((index1 << 4) & 0xf0) | ((index2 >> 2) & 0x0f)));
                if (char3 != BASE64_PADDING_BYTE) {
                    buf.put((byte) (((index2 << 6) & 0xc0) | (index3 & 0x3f)));
                }
            }
        }
        buf.flip();
        return buf.slice();
    }

    private static byte mapped(int index) {
        switch (index) {
        case BASE64_PADDING_BYTE:
        case 'A':
            return 0;
        case 'B':
            return 1;
        case 'C':
            return 2;
        case 'D':
            return 3;
        case 'E':
            return 4;
        case 'F':
            return 5;
        case 'G':
            return 6;
        case 'H':
            return 7;
        case 'I':
            return 8;
        case 'J':
            return 9;
        case 'K':
            return 10;
        case 'L':
            return 11;
        case 'M':
            return 12;
        case 'N':
            return 13;
        case 'O':
            return 14;
        case 'P':
            return 15;
        case 'Q':
            return 16;
        case 'R':
            return 17;
        case 'S':
            return 18;
        case 'T':
            return 19;
        case 'U':
            return 20;
        case 'V':
            return 21;
        case 'W':
            return 22;
        case 'X':
            return 23;
        case 'Y':
            return 24;
        case 'Z':
            return 25;
        case 'a':
            return 26;
        case 'b':
            return 27;
        case 'c':
            return 28;
        case 'd':
            return 29;
        case 'e':
            return 30;
        case 'f':
            return 31;
        case 'g':
            return 32;
        case 'h':
            return 33;
        case 'i':
            return 34;
        case 'j':
            return 35;
        case 'k':
            return 36;
        case 'l':
            return 37;
        case 'm':
            return 38;
        case 'n':
            return 39;
        case 'o':
            return 40;
        case 'p':
            return 41;
        case 'q':
            return 42;
        case 'r':
            return 43;
        case 's':
            return 44;
        case 't':
            return 45;
        case 'u':
            return 46;
        case 'v':
            return 47;
        case 'w':
            return 48;
        case 'x':
            return 49;
        case 'y':
            return 50;
        case 'z':
            return 51;
        case '0':
            return 52;
        case '1':
            return 53;
        case '2':
            return 54;
        case '3':
            return 55;
        case '4':
            return 56;
        case '5':
            return 57;
        case '6':
            return 58;
        case '7':
            return 59;
        case '8':
            return 60;
        case '9':
            return 61;
        case '+':
            return 62;
        case '/':
            return 63;
        default:
            throw new IllegalArgumentException("Invalid base64 string");
        }
    }

}
