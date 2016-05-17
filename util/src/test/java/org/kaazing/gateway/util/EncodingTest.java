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
import static org.junit.Assert.assertFalse;

import java.nio.ByteBuffer;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.junit.Assert;
import org.junit.Test;

public class EncodingTest {

	// Helper to wrap buffer as readOnly to validate encoder/decoder methods
	// that do not have backing array (and use different code path currently)
    private ByteBuffer asReadOnlyIfNeeded(ByteBuffer byteBuffer, boolean withArray) {
        if (withArray) {
        	return byteBuffer;
        }
        else {
        	return byteBuffer.asReadOnlyBuffer();
        }
    }

    private void encodeUtf8(boolean withArray) {
        byte[] decodedWithOffset4 = new byte[] {0, 0, 0, 0, 0, 1, 2, 4, 8, 16, 32, 64, -128, -64, -1};
        ByteBuffer in = asReadOnlyIfNeeded(ByteBuffer.wrap(decodedWithOffset4, 4, decodedWithOffset4.length - 4), withArray);

        ByteBuffer out = Encoding.UTF8.encode(in);

        ByteBuffer expected = ByteBuffer.wrap(new byte[] {0, 1, 2, 4, 8, 16, 32, 64, -62, -128, -61, -128, -61, -65});
        assertEquals(expected, out);
    }

    @Test
    public void encodeUtf8() {
    	encodeUtf8(true);
    }

    @Test
    public void encodeUtf8NoArray() {
    	encodeUtf8(false);
    }

    private void decodeUtf8(boolean withArray) {
        // 00 01 02 04 08 10 20 40 C2 80 C3 80 C3 BF
        ByteBuffer in = asReadOnlyIfNeeded(ByteBuffer.wrap(new byte[] {0, 1, 2, 4, 8, 16, 32, 64, -62, -128, -61, -128, -61, -65}), withArray);

        // 00 01 02 04 08 10 20 40 80 C0 FF
        ByteBuffer expected = ByteBuffer.wrap(new byte[] {0, 1, 2, 4, 8, 16, 32, 64, -128, -64, -1});

        ByteBuffer out = Encoding.UTF8.decode(in);

        assertEquals(expected, out);
    }

    @Test
    public void decodeUtf8() {
    	decodeUtf8(true);
    }

    @Test
    public void decodeUtf8NoArray() {
    	decodeUtf8(false);
    }

    private void decodeUtf8Offset(boolean withArray) {
        byte[] data = new byte[] {8, -61, -65, 0, 1, 2, 4, 8, 16, 32, 64, -62, -128, -61, -128, -61, -65};
        ByteBuffer in = asReadOnlyIfNeeded(ByteBuffer.wrap(data, /*offset*/ 3, data.length - 3), withArray);

        ByteBuffer out = Encoding.UTF8.decode(in);

        ByteBuffer expected = ByteBuffer.wrap(new byte[] {0, 1, 2, 4, 8, 16, 32, 64, -128, -64, -1});
        assertEquals(expected, out);
    }

    @Test
    public void decodeUtf8Offset() {
    	decodeUtf8Offset(true);
    }

    @Test
    public void decodeUtf8OffsetNoArray() {
    	decodeUtf8Offset(false);
    }

    private void decodeUtf8Offset2(boolean withArray) {
        byte[] data = new byte[] {8, -61, -65, 0, 1, 2, 4, 8, 16, 32, 64, -62, -128, -61, -128, -61, -65};
        ByteBuffer in = asReadOnlyIfNeeded(ByteBuffer.wrap(data, /* offset */ 1, data.length - 1), withArray);

        ByteBuffer out = Encoding.UTF8.decode(in);

        ByteBuffer expected = ByteBuffer.wrap(new byte[] {-1, 0, 1, 2, 4, 8, 16, 32, 64, -128, -64, -1});
        assertEquals(expected, out);
    }

    @Test
    public void decodeUtf8Offset2() {
    	decodeUtf8Offset2(true);
    }

    @Test
    public void decodeUtf8Offset2NoArray() {
    	decodeUtf8Offset2(false);
    }

    private void encodeUtf8EscapeZeroAndNewline(boolean withArray) {
        byte[] bytes = new byte[] {0, 1, 2, 4, 8, 10, 13, 16, 32, 64, 127, -128, -64, -1};
        ByteBuffer in = asReadOnlyIfNeeded(ByteBuffer.wrap(bytes), withArray);

        ByteBuffer out = Encoding.UTF8_ESCAPE_ZERO_AND_NEWLINE.encode(in);

        ByteBuffer expected = ByteBuffer.wrap(new byte[] {0x7f, 0x30, 1, 2, 4, 8, 0x7f, 0x6e, 0x7f, 0x72, 16, 32, 64, 0x7f, 0x7f, -62, -128, -61, -128, -61, -65});
        assertEquals(expected, out);
    }

    @Test
    public void encodeUtf8EscapeZeroAndNewline() {
    	encodeUtf8EscapeZeroAndNewline(true);
    }

    @Test
    public void encodeUtf8EscapeZeroAndNewlineNoArray() {
    	encodeUtf8EscapeZeroAndNewline(false);
    }

    private void decodeUtf8EscapeZeroAndNewline(boolean withArray) {
    	byte[] bytes = new byte[] {0x7f, 0x30, 1, 2, 4, 8, 0x7f, 0x6e, 0x7f, 0x72, 16, 32, 64, 0x7f, 0x7f, -62, -128, -61, -128, -61, -65};
        ByteBuffer in = asReadOnlyIfNeeded(ByteBuffer.wrap(bytes), withArray);

        ByteBuffer out = Encoding.UTF8_ESCAPE_ZERO_AND_NEWLINE.decode(in);

        ByteBuffer expected = ByteBuffer.wrap(new byte[] {0, 1, 2, 4, 8, 10, 13, 16, 32, 64, 127, -128, -64, -1});
        assertEquals(expected, out);
    }

    @Test
    public void decodeUtf8EscapeZeroAndNewline() {
    	decodeUtf8EscapeZeroAndNewline(true);
    }

    @Test
    public void decodeUtf8EscapeZeroAndNewlineNoArray() {
    	decodeUtf8EscapeZeroAndNewline(false);
    }

    @Test
    public void decodeUtf8With256() {
        // UTF-8 decode should remove first 2 bit in Byte#2. please refer to http://en.wikipedia.org/wiki/UTF-8.
        byte[] bytes = new byte[] {(byte) 0xc4, (byte) 0x80, (byte) 0xc4, (byte) 0x80, (byte) 0xc2, (byte) 0x80};
        ByteBuffer in = ByteBuffer.wrap(bytes);

        ByteBuffer out = Encoding.UTF8.decode(in);

        ByteBuffer expected = ByteBuffer.wrap(new byte[] {0, 0, -128});
        assertEquals(expected, out);
    }

    private void encodeEscapeZeroAndNewline(boolean withArray) {
    	byte[] bytes = new byte[] {0, 1, 2, 4, 8, 10, 13, 16, 32, 64, 127, -128, -64, -1};
    	ByteBuffer buf = ByteBuffer.wrap(bytes);
    	ByteBuffer in = asReadOnlyIfNeeded(buf, withArray);

    	ByteBuffer out = Encoding.ESCAPE_ZERO_AND_NEWLINE.encode(in);

    	ByteBuffer expected = ByteBuffer.wrap(new byte[] {0x7f, 0x30, 1, 2, 4, 8, 0x7f, 0x6e, 0x7f, 0x72, 16, 32, 64, 0x7f, 0x7f, -128, -64, -1});
        assertEquals(expected, out);
    }

    @Test
    public void encodeEscapeZeroAndNewline() {
    	encodeEscapeZeroAndNewline(true);
    }

    @Test
    public void encodeEscapeZeroAndNewlineNoArray() {
    	encodeEscapeZeroAndNewline(false);
    }

    private void decodeEscapeZeroAndNewline(boolean withArray) {
    	byte[] bytes = new byte[] {0x7f, 0x30, 1, 2, 4, 8, 0x7f, 0x6e, 0x7f, 0x72, 16, 32, 64, 0x7f, 0x7f, -128, -64, -1};
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        ByteBuffer in = asReadOnlyIfNeeded(buf, withArray);

        ByteBuffer out = Encoding.ESCAPE_ZERO_AND_NEWLINE.decode(in);

        ByteBuffer expected = ByteBuffer.wrap(new byte[] {0, 1, 2, 4, 8, 10, 13, 16, 32, 64, 127, -128, -64, -1});
        assertEquals(expected, out);
    }

    @Test
    public void decodeEscapeZeroAndNewline() {
    	decodeEscapeZeroAndNewline(true);
    }

    @Test
    public void decodeEscapeZeroAndNewlineNoArray() {
    	decodeEscapeZeroAndNewline(false);
    }

	private void encodeBase64(boolean withArray) {
		byte[] bytes = new byte[] {0, 0, 0, 99, 3, 2, 127, 24, 0, 0, 0, 1, 0, 0, 120, 0, 42, 73, 68, 58, 106, 102, 97, 108, 108, 111, 119, 115, 45, 108, 97, 112, 116, 111, 112, 45, 53, 53, 55, 48, 51, 45, 49, 50, 49, 54, 57, 52, 52, 48, 51, 50, 56, 52, 52, 45, 48, 58, 48, 0, 42, 73, 68, 58, 106, 102, 97, 108, 108, 111, 119, 115, 45, 108, 97, 112, 116, 111, 112, 45, 53, 53, 55, 48, 51, 45, 49, 50, 49, 54, 57, 52, 52, 48, 51, 50, 56, 52, 52, 45, 49, 58, 48};
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        ByteBuffer in = asReadOnlyIfNeeded(buf, withArray);

		ByteBuffer out = Encoding.BASE64.encode(in);

		ByteBuffer expected = ByteBuffer.wrap(new byte[] {65, 65, 65, 65, 89, 119, 77, 67, 102, 120, 103, 65, 65, 65, 65, 66, 65, 65, 66, 52, 65, 67, 112, 74, 82, 68, 112, 113, 90, 109, 70, 115, 98, 71, 57, 51, 99, 121, 49, 115, 89, 88, 66, 48, 98, 51, 65, 116, 78, 84, 85, 51, 77, 68, 77, 116, 77, 84, 73, 120, 78, 106, 107, 48, 78, 68, 65, 122, 77, 106, 103, 48, 78, 67, 48, 119, 79, 106, 65, 65, 75, 107, 108, 69, 79, 109, 112, 109, 89, 87, 120, 115, 98, 51, 100, 122, 76, 87, 120, 104, 99, 72, 82, 118, 99, 67, 48, 49, 78, 84, 99, 119, 77, 121, 48, 120, 77, 106, 69, 50, 79, 84, 81, 48, 77, 68, 77, 121, 79, 68, 81, 48, 76, 84, 69, 54, 77, 65, 61, 61});
		assertEquals(expected, out);
	}

	@Test
	public void encodeBase64() {
		encodeBase64(true);
	}

	@Test
	public void encodeBase64NoArray() {
		encodeBase64(false);
	}

	private void decodeBase64(boolean withArray) {
		byte[] bytes = new byte[] {65, 65, 65, 65, 50, 81, 70, 66, 89, 51, 82, 112, 100, 109, 86, 78, 85, 81, 65, 65, 65, 65, 77, 66, 65, 65, 65, 65, 120, 119, 65, 65, 65, 65, 103, 65, 67, 85, 78, 104, 89, 50, 104, 108, 85, 50, 108, 54, 90, 81, 85, 65, 65, 65, 81, 65, 65, 65, 120, 68, 89, 87, 78, 111, 90, 85, 86, 117, 89, 87, 74, 115, 90, 87, 81, 66, 65, 81, 65, 83, 85, 50, 108, 54, 90, 86, 66, 121, 90, 87, 90, 112, 101, 69, 82, 112, 99, 50, 70, 105, 98, 71, 86, 107, 65, 81, 65, 65, 73, 69, 49, 104, 101, 69, 108, 117, 89, 87, 78, 48, 97, 88, 90, 112, 100, 72, 108, 69, 100, 88, 74, 104, 100, 71, 108, 118, 98, 107, 108, 117, 97, 88, 82, 104, 98, 69, 82, 108, 98, 71, 70, 53, 66, 103, 65, 65, 65, 65, 65, 65, 65, 67, 99, 81, 65, 66, 70, 85, 89, 51, 66, 79, 98, 48, 82, 108, 98, 71, 70, 53, 82, 87, 53, 104, 89, 109, 120, 108, 90, 65, 69, 66, 65, 66, 86, 78, 89, 88, 104, 74, 98, 109, 70, 106, 100, 71, 108, 50, 97, 88, 82, 53, 82, 72, 86, 121, 89, 88, 82, 112, 98, 50, 52, 71, 65, 65, 65, 65, 65, 65, 65, 65, 100, 84, 65, 65, 70, 70, 82, 112, 90, 50, 104, 48, 82, 87, 53, 106, 98, 50, 82, 112, 98, 109, 100, 70, 98, 109, 70, 105, 98, 71, 86, 107, 65, 81, 69, 65, 69, 86, 78, 48, 89, 87, 78, 114, 86, 72, 74, 104, 89, 50, 86, 70, 98, 109, 70, 105, 98, 71, 86, 107, 65, 81, 69, 61};
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        ByteBuffer in = asReadOnlyIfNeeded(buf, withArray);

		ByteBuffer out = Encoding.BASE64.decode(in);

		ByteBuffer expected = ByteBuffer.wrap(new byte[] {0, 0, 0, -39, 1, 65, 99, 116, 105, 118, 101, 77, 81, 0, 0, 0, 3, 1, 0, 0, 0, -57, 0, 0, 0, 8, 0, 9, 67, 97, 99, 104, 101, 83, 105, 122, 101, 5, 0, 0, 4, 0, 0, 12, 67, 97, 99, 104, 101, 69, 110, 97, 98, 108, 101, 100, 1, 1, 0, 18, 83, 105, 122, 101, 80, 114, 101, 102, 105, 120, 68, 105, 115, 97, 98, 108, 101, 100, 1, 0, 0, 32, 77, 97, 120, 73, 110, 97, 99, 116, 105, 118, 105, 116, 121, 68, 117, 114, 97, 116, 105, 111, 110, 73, 110, 105, 116, 97, 108, 68, 101, 108, 97, 121, 6, 0, 0, 0, 0, 0, 0, 39, 16, 0, 17, 84, 99, 112, 78, 111, 68, 101, 108, 97, 121, 69, 110, 97, 98, 108, 101, 100, 1, 1, 0, 21, 77, 97, 120, 73, 110, 97, 99, 116, 105, 118, 105, 116, 121, 68, 117, 114, 97, 116, 105, 111, 110, 6, 0, 0, 0, 0, 0, 0, 117, 48, 0, 20, 84, 105, 103, 104, 116, 69, 110, 99, 111, 100, 105, 110, 103, 69, 110, 97, 98, 108, 101, 100, 1, 1, 0, 17, 83, 116, 97, 99, 107, 84, 114, 97, 99, 101, 69, 110, 97, 98, 108, 101, 100, 1, 1});

		assertEquals(expected, out);
	}

	@Test
	public void decodeBase64() {
		decodeBase64(true);
	}

	@Test
	public void decodeBase64NoArray() {
		decodeBase64(false);
	}

    @Test
    public void fragmentsSplitWithin2ByteCharacterUTF8() throws Exception {
        fragmentedSequence("C2", "A9313131", Encoding.UTF8, true);
    }

    @Test
    public void fragmentsSplitWithin2ByteCharacterUTF8NoArray() throws Exception {
        fragmentedSequence("C2", "A9313131", Encoding.UTF8, false);
    }

    @Test
    public void fragmentsSplitWithin2ByteCharacterUTF8Escape() throws Exception {
        fragmentedSequence("C2", "A9313131", Encoding.UTF8_ESCAPE_ZERO_AND_NEWLINE, true);
    }

    @Test
    public void fragmentsSplitWithin2ByteCharacterUTF8EscapeNoArray() throws Exception {
        fragmentedSequence("C2", "A9313131", Encoding.UTF8_ESCAPE_ZERO_AND_NEWLINE, false);
    }

    // Our UTF8 decoding does not currently handle more than 2 byte characters, because the client never
    // uses more than 2 byte characters (because it only needs to encode byte values up to 255)
    // so we don't need to test 3 byte characters like E2A480 nor 4 byters like F0908080


    @Test
    public void fragmentsSplitWithin2ByteEscapedNewlineUTF8Escape() throws Exception {
        fragmentedSequence("7F", "6E303132", Encoding.UTF8_ESCAPE_ZERO_AND_NEWLINE, true);
    }

    @Test
    public void fragmentsSplitWithin2ByteEscapedNewlineUTF8EscapeNoArray() throws Exception {
        fragmentedSequence("7F", "6E303132", Encoding.UTF8_ESCAPE_ZERO_AND_NEWLINE, false);
    }

    @Test
    public void singleEndingWithASCIICharacterUTF8() throws Exception {
        // Present [013030C3BF30] as one fragment.
        decodeSinglePacket("013030C3BF30", "013030FF30", Encoding.UTF8, true);
    }

    @Test
    public void singleEndingWithASCIICharacterUTF8NoArray() throws Exception {
        // Present [013030C3BF30] as one fragment.
        decodeSinglePacket("013030C3BF30", "013030FF30", Encoding.UTF8, false);
    }

    @Test
    public void singleEndingWith2ByteCharacterUTF8() throws Exception {
        // Present [013030C3BF] as one fragment.
        decodeSinglePacket("013030C3BF", "013030FF", Encoding.UTF8, true);
    }

    @Test
    public void singleEndingWith2ByteCharacterUTF8NoArray() throws Exception {
        // Present [013030C3BF] as one fragment.
        decodeSinglePacket("013030C3BF", "013030FF", Encoding.UTF8, false);
    }

    // Test behavior where first packet ends with first byte of a two-byte sequence
    private void fragmentedSequence(final String fragment1Str, final String fragment2Str, Encoding encoding, boolean withArray) throws Exception {
        final ByteBuffer fragment1 = asReadOnlyIfNeeded(ByteBuffer.wrap(Utils.fromHex(fragment1Str)), withArray);
        final ByteBuffer fragment2 = asReadOnlyIfNeeded(ByteBuffer.wrap(Utils.fromHex(fragment2Str)), withArray);
        final Object[] remainingByteState = new Object[1];
        final byte remainingByte = fragment1.get(fragment1.remaining()-1);

        Mockery context = new Mockery();
        final DecodingState state = context.mock(DecodingState.class);
        context.checking(new Expectations() {
            {
                try {
                    oneOf(state).get();
                    will(returnValue(null));

                    oneOf(state).set(remainingByte);
                    will(saveParameter(remainingByteState, 0));

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            public Action saveParameter(final Object[] parameterStorage, final int parameterIndex) {
                return new CustomAction("save parameter") {

                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        parameterStorage[0] = invocation.getParameter(parameterIndex);
                        return null;
                    }
                };
            }
        });

        ByteBuffer result = encoding.decode(fragment1, state);
        assertFalse(result.hasRemaining());

        context.assertIsSatisfied();

        //context = new Mockery();
        //DecodingState state = context.mock(DecodingState.class);

        context.checking(new Expectations() {
            {
                try {
                    oneOf(state).get();
                    will(returnValue(remainingByteState[0]));

                    oneOf(state).set(null);

                    oneOf(state).get();
                    will(returnValue(null));

                    oneOf(state).set(null);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });


        result = encoding.decode(fragment2, state);

        ByteBuffer combined = asReadOnlyIfNeeded(ByteBuffer.wrap(Utils.fromHex(fragment1Str + fragment2Str)), withArray);
        ByteBuffer expected = encoding.decode(combined, state);
        assertEquals(expected, result);
        context.assertIsSatisfied();
    }



    private void decodeSinglePacket(String payload, String expectedResult, Encoding encoding, boolean withArray) throws Exception {
        Mockery context = new Mockery();
        final DecodingState state = context.mock(DecodingState.class);

        context.checking(new Expectations() {
            {
                try {
                    oneOf(state).get();
                    will(returnValue(null));

                    oneOf(state).set(null);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        byte[] byteArray = Utils.fromHex(payload);
        ByteBuffer buffer = asReadOnlyIfNeeded(ByteBuffer.wrap(byteArray), withArray);
        ByteBuffer result = encoding.decode(buffer, state);

        // equals does not check via array or check for readonly flag/direct buffers
        Assert.assertEquals(ByteBuffer.wrap(Utils.fromHex(expectedResult)), result);
        context.assertIsSatisfied();
    }

    public static void main(String... args) throws Exception {
		int count = 1000 * 1000;

		byte[] decoded = new byte[] {0, 0, 0, 99, 3, 2, 127, 24, 0, 0, 0, 1, 0, 0, 120, 0, 42, 73, 68, 58, 106, 102, 97, 108, 108, 111, 119, 115, 45, 108, 97, 112, 116, 111, 112, 45, 53, 53, 55, 48, 51, 45, 49, 50, 49, 54, 57, 52, 52, 48, 51, 50, 56, 52, 52, 45, 48, 58, 48, 0, 42, 73, 68, 58, 106, 102, 97, 108, 108, 111, 119, 115, 45, 108, 97, 112, 116, 111, 112, 45, 53, 53, 55, 48, 51, 45, 49, 50, 49, 54, 57, 52, 52, 48, 51, 50, 56, 52, 52, 45, 49, 58, 48};
		byte[] encoded = new byte[] {65, 65, 65, 65, 50, 81, 70, 66, 89, 51, 82, 112, 100, 109, 86, 78, 85, 81, 65, 65, 65, 65, 77, 66, 65, 65, 65, 65, 120, 119, 65, 65, 65, 65, 103, 65, 67, 85, 78, 104, 89, 50, 104, 108, 85, 50, 108, 54, 90, 81, 85, 65, 65, 65, 81, 65, 65, 65, 120, 68, 89, 87, 78, 111, 90, 85, 86, 117, 89, 87, 74, 115, 90, 87, 81, 66, 65, 81, 65, 83, 85, 50, 108, 54, 90, 86, 66, 121, 90, 87, 90, 112, 101, 69, 82, 112, 99, 50, 70, 105, 98, 71, 86, 107, 65, 81, 65, 65, 73, 69, 49, 104, 101, 69, 108, 117, 89, 87, 78, 48, 97, 88, 90, 112, 100, 72, 108, 69, 100, 88, 74, 104, 100, 71, 108, 118, 98, 107, 108, 117, 97, 88, 82, 104, 98, 69, 82, 108, 98, 71, 70, 53, 66, 103, 65, 65, 65, 65, 65, 65, 65, 67, 99, 81, 65, 66, 70, 85, 89, 51, 66, 79, 98, 48, 82, 108, 98, 71, 70, 53, 82, 87, 53, 104, 89, 109, 120, 108, 90, 65, 69, 66, 65, 66, 86, 78, 89, 88, 104, 74, 98, 109, 70, 106, 100, 71, 108, 50, 97, 88, 82, 53, 82, 72, 86, 121, 89, 88, 82, 112, 98, 50, 52, 71, 65, 65, 65, 65, 65, 65, 65, 65, 100, 84, 65, 65, 70, 70, 82, 112, 90, 50, 104, 48, 82, 87, 53, 106, 98, 50, 82, 112, 98, 109, 100, 70, 98, 109, 70, 105, 98, 71, 86, 107, 65, 81, 69, 65, 69, 86, 78, 48, 89, 87, 78, 114, 86, 72, 74, 104, 89, 50, 86, 70, 98, 109, 70, 105, 98, 71, 86, 107, 65, 81, 69, 61};

		{
            ByteBuffer decodedBuf = ByteBuffer.wrap(decoded);
            ByteBuffer encodedBuf = ByteBuffer.wrap(encoded);
			testEncodePerformance(decodedBuf, count);
			testDecodePerformance(encodedBuf, count);
		}

		// Repeat using direct buffers
		{
		    ByteBuffer decodedBuf = ByteBuffer.allocateDirect(decoded.length);
			decodedBuf.put(decoded);
			decodedBuf.flip();
			testEncodePerformance(decodedBuf, count);

            ByteBuffer encodedBuf = ByteBuffer.allocateDirect(encoded.length);
			encodedBuf.put(encoded);
			encodedBuf.flip();
			testDecodePerformance(encodedBuf, count);
		}
	}

	private static void testEncodePerformance(ByteBuffer in, int count) {
		long startAt = System.currentTimeMillis();
		for (int i=count; i > 0; i--) {
			Encoding.BASE64.encode(in.duplicate());
		}
		long endAt = System.currentTimeMillis();

		int encodedBytes = count * in.remaining();
		long encodedMillis = (endAt - startAt);
		float encodedMegabytesPerSecond = (encodedBytes / (1024.0f * 1024.0f)) / (encodedMillis / 1000.0f);
		System.out.println(String.format("Base64 encode performance: %.2f Mbytes/sec (in) %.2f Mbytes/sec (out)", encodedMegabytesPerSecond, encodedMegabytesPerSecond * 4.0f / 3.0f));
	}

	private static void testDecodePerformance(ByteBuffer in, int count) {
		long startAt = System.currentTimeMillis();
		for (int i=count; i > 0; i--) {
			Encoding.BASE64.decode(in.duplicate());
		}
		long endAt = System.currentTimeMillis();

		int decodedBytes = count * in.remaining();
		long decodedMillis = (endAt - startAt);
		float decodedMegabytesPerSecond = (decodedBytes / (1024.0f * 1024.0f)) / (decodedMillis / 1000.0f);
		System.out.println(String.format("Base64 decode performance: %.2f Mbytes/sec (in) %.2f Mbytes/sec (out)", decodedMegabytesPerSecond, decodedMegabytesPerSecond / 4.0f * 3.0f));
	}
}
