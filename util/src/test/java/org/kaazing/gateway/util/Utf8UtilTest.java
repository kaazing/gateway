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

import static org.junit.Assert.assertEquals;
import static org.kaazing.gateway.util.Utf8Util.initialDecodeUTF8;
import static org.kaazing.gateway.util.Utf8Util.remainingBytesUTF8;
import static org.kaazing.gateway.util.Utf8Util.remainingDecodeUTF8;

import org.junit.Test;

// Copied from
// https://github.com/kaazing/netx/blob/develop/ws/src/test/java/org/kaazing/netx/ws/internal/util/Utf8UtilTest.java
public class Utf8UtilTest {

    @Test
    public void shouldDecode3ByteChar() throws Exception {

        byte[] bytes = new byte[] { (byte) 0xe8, (byte) 0xaf, (byte) 0x85 };

        int remainingBytes = remainingBytesUTF8(bytes[0]);
        assertEquals(2, remainingBytes);

        int bytesOffset = 0;
        int codePoint = initialDecodeUTF8(remainingBytes, bytes[bytesOffset++]);
        while (remainingBytes > 0) {
            switch (remainingBytes) {
            case 1:
                codePoint = remainingDecodeUTF8(codePoint, remainingBytes--, bytes[bytesOffset++]);
                assertEquals(0x8bc5, new String(new int[] { codePoint }, 0, 1).codePointAt(0));
                break;
            default:
                codePoint = remainingDecodeUTF8(codePoint, remainingBytes--, bytes[bytesOffset++]);
                break;
            }
        }
    }

    @Test
    public void shouldDecode4ByteChar() throws Exception {
        byte[] bytes = new byte[] { (byte) 0xf1, (byte) 0x88, (byte) 0xb5, (byte) 0x92 };

        int remainingBytes = remainingBytesUTF8(bytes[0]);
        assertEquals(3, remainingBytes);

        int bytesOffset = 0;
        int codePoint = initialDecodeUTF8(remainingBytes, bytes[bytesOffset++]);
        while (remainingBytes > 0) {
            switch (remainingBytes) {
            case 1:
                codePoint = remainingDecodeUTF8(codePoint, remainingBytes--, bytes[bytesOffset++]);
                assertEquals(0x48d52, new String(new int[] { codePoint }, 0, 1).codePointAt(0));
                break;
            default:
                codePoint = remainingDecodeUTF8(codePoint, remainingBytes--, bytes[bytesOffset++]);
                break;
            }
        }
    }
}