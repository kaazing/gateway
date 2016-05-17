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
package org.kaazing.gateway.transport.wseb.filter;

import static org.kaazing.gateway.transport.bridge.CachingMessageEncoder.IO_MESSAGE_ENCODER;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.kaazing.gateway.transport.bridge.CachingMessageEncoder;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.WsPingMessage;
import org.kaazing.gateway.transport.ws.WsPongMessage;
import org.kaazing.gateway.transport.ws.util.WsUtils;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.netty.util.threadlocal.VicariousThreadLocal;

public class WsebFrameEscapeZeroAndNewLineEncoder extends WsebFrameEncoder {

    static protected final int cacheSize = 1024;  //cache size when read direct ByteBuffer
    static final byte BINARY_TYPE_BYTE = (byte) 0x80;
    static final byte SPECIFIED_LENGTH_TEXT_TYPE_BYTE = (byte) 0x81;

    private static final byte[] EMPTY_PING_BYTES = new byte[]{(byte)0x89, (byte)0x7f, (byte)0x30}; //escaped
    private static final byte[] EMPTY_PONG_BYTES = new byte[]{(byte)0x8A, (byte)0x7f, (byte)0x30}; //escaped

    public WsebFrameEscapeZeroAndNewLineEncoder(IoBufferAllocatorEx<?> allocator) {
        this(IO_MESSAGE_ENCODER, allocator);
    }

    public WsebFrameEscapeZeroAndNewLineEncoder(CachingMessageEncoder cachingEncoder, IoBufferAllocatorEx<?> allocator) {
        super(cachingEncoder, allocator);
    }


    protected final ThreadLocal<byte[]> readCacheRef = new VicariousThreadLocal<byte[]>() {

        @Override
        protected byte[] initialValue() {
            return new byte[cacheSize];
        }
    };

    private final ThreadLocal<byte[]> writeCacheRef = new VicariousThreadLocal<byte[]>() {

        @Override
        protected byte[] initialValue() {
            return new byte[cacheSize * 2];
        }
    };

    private IoBufferEx escapeZeroAndNewLine(IoBufferAllocatorEx<?> allocator, int flags, IoBufferEx decoded, ByteBuffer prefix) {

        if (decoded.hasArray()) {
            return escapeZeroAndNewLineWithHeap(allocator, flags, decoded, prefix);
        }
        else {
            return escapeZeroAndNewLineWithDirect(allocator, flags, decoded, prefix);
        }
    }

    private IoBufferEx escapeZeroAndNewLineWithDirect(IoBufferAllocatorEx<?> allocator, int flags, IoBufferEx decoded, ByteBuffer prefix) {

        byte[] readCache = readCacheRef.get();
        byte[] writeCache = writeCacheRef.get();

        final int decodedInitialPosition = decoded.position();
        final int decodedLimit = decoded.limit();

        ByteBuffer encodedBuf = null;

        int[] encodedArrayInsertionCount = new int[1];
        ByteBuffer emptyArray = ByteBuffer.allocate(0); //pass an empty array as prefix

        while(decoded.hasRemaining()) {
            int bytesToRead = decoded.remaining();
            if (bytesToRead > cacheSize) {
                bytesToRead = cacheSize;
            }
            // read data from decoded buffer
            decoded.get(readCache, 0, bytesToRead);
            // escape encode
            encodedArrayInsertionCount[0] = 0;
            doEscapeZeroAndNewline(readCache, 0, 0, bytesToRead, writeCache, encodedArrayInsertionCount, emptyArray);
            // write data to encodedBuffer
            if (encodedBuf == null && encodedArrayInsertionCount[0] > 0) {
                int currentPosition = decoded.position();
                //first escaped char found, allocate decodedBuf now, maximam size is (read so far + insertedCount + 2 * remaining data )
                int totalSize = prefix.remaining() + currentPosition - decodedInitialPosition + encodedArrayInsertionCount[0]; //read so far + insertedCount
                totalSize +=  2 * decoded.remaining();

                encodedBuf = ByteBuffer.allocateDirect(totalSize);
                encodedBuf.put(prefix);
                //copy data has been read from previous loops
                if (currentPosition > cacheSize) {
                    //set position and limit to previously read range
                    decoded.position(decodedInitialPosition);
                    decoded.limit(currentPosition - bytesToRead);
                    encodedBuf.put(decoded.buf());
                    //move position and limit back
                    decoded.limit(decodedLimit);
                    decoded.position(currentPosition);
                }
            }
            // now put this block if we have found escape char
            if (encodedBuf != null) {
                encodedBuf.put(writeCache, 0, bytesToRead + encodedArrayInsertionCount[0]);
            }
            // move to next block
        }

        if (encodedBuf != null) {
            encodedBuf.flip();
            return allocator.wrap(encodedBuf);
        }

        //no escape char found, return prefix + decodedBuf
        decoded.position(decodedInitialPosition);
        decoded.limit(decodedLimit);
          encodedBuf = ByteBuffer.allocateDirect(prefix.remaining() + decoded.remaining());
          encodedBuf.put(prefix);
           encodedBuf.put(decoded.buf());
           encodedBuf.flip();

        return allocator.wrap(encodedBuf);
    }

    private IoBufferEx escapeZeroAndNewLineWithHeap(IoBufferAllocatorEx<?> allocator, int flags, IoBufferEx decoded, ByteBuffer prefix) {

        byte[] decodedArray = decoded.array();
        final int decodedArrayOffset = decoded.arrayOffset();
        int decodedArrayPosition = decodedArrayOffset + decoded.position();
        final int decodedArrayInitialPosition = decodedArrayPosition;
        final int decodedArrayLimit = decodedArrayOffset + decoded.limit();

        int[] encodedArrayInsertionCount = new int[1];
        byte[] encodedArray = null;
        encodedArray = doEscapeZeroAndNewline(decodedArray, decodedArrayInitialPosition, decodedArrayInitialPosition, decodedArrayLimit, encodedArray, encodedArrayInsertionCount, prefix);

        if (encodedArray != null) {
            return allocator.wrap(ByteBuffer.wrap(encodedArray, decodedArrayInitialPosition, prefix.remaining() + decoded.remaining() + encodedArrayInsertionCount[0]));
        }
        else {
            encodedArray = Arrays.copyOf(prefix.array(), prefix.remaining() + decoded.remaining());
            System.arraycopy(decodedArray, decodedArrayInitialPosition, encodedArray, prefix.remaining(), decoded.remaining());
               return allocator.wrap(ByteBuffer.wrap(encodedArray));
        }

    }

    // quick calculate WsebFrame prefix bytes
    protected ByteBuffer calculatePrefixBytes(int payloadLength, byte opCode) {


        int totalOffset = 2 + WsUtils.calculateEncodedLengthSize(payloadLength);

        ByteBuffer binary = ByteBuffer.allocate(totalOffset);
        binary.put(opCode);
        WsUtils.encodeLength(binary, payloadLength);

        //the only possible escaped byte in prefix bytes is the last byte

        switch(binary.get(binary.position() - 1)) {
            case 0x00:
                binary.put(binary.position() - 1,(byte) 0x7f);
                binary.put((byte) 0x30);
            break;
            case 0x0a:
                binary.put(binary.position() - 1,(byte) 0x7f);
                binary.put((byte) 0x6e);
            break;
            case 0x0d:
                binary.put(binary.position() - 1,(byte) 0x7f);
                binary.put((byte) 0x72);
            break;
            case 0x7f:
                binary.put(binary.position() - 1,(byte) 0x7f);
                binary.put((byte) 0x7f);
            break;
        }
        binary.flip();
        return binary;

    }

    // insert escape characters into encodedArray, return number of inserted bytes
    private byte[] doEscapeZeroAndNewline(byte[] decodedArray,int decodedArrayOffset, int decodedArrayPosition,int decodedArrayLimit, byte[] encodedArray, int[] encodedArrayInsertionCount, ByteBuffer prefix) {
        int prefixLength = prefix.remaining();
        for (; decodedArrayPosition < decodedArrayLimit; decodedArrayPosition++) {
            byte decodedValue = decodedArray[decodedArrayPosition];

            switch (decodedValue) {
            case 0x00:
                // null byte, requires multi-byte escaped representation
                encodedArray = supplyEncodedArray(decodedArray,decodedArrayOffset, decodedArrayPosition, decodedArrayLimit, encodedArray, encodedArrayInsertionCount, prefix);

                // 0x00 -> 0x7f 0x30 ('0')
                encodedArray[decodedArrayPosition + encodedArrayInsertionCount[0] + prefixLength] = 0x7f;
                encodedArrayInsertionCount[0]++;
                encodedArray[decodedArrayPosition + encodedArrayInsertionCount[0] + prefixLength] = 0x30;

                // advance to next byte
                continue;
            case 0x0a:
                // newline byte, requires multi-byte escaped representation
                encodedArray = supplyEncodedArray(decodedArray, decodedArrayOffset, decodedArrayPosition, decodedArrayLimit, encodedArray, encodedArrayInsertionCount, prefix);

                // 0x0a -> 0x7f 0x6e ('n')
                encodedArray[decodedArrayPosition + encodedArrayInsertionCount[0] + prefixLength] = 0x7f;
                encodedArrayInsertionCount[0]++;
                encodedArray[decodedArrayPosition + encodedArrayInsertionCount[0] + prefixLength] = 0x6e;

                // advance to next byte
                continue;
            case 0x0d:
                // carriage return byte, requires multi-byte escaped
                // representation
                encodedArray = supplyEncodedArray(decodedArray, decodedArrayOffset, decodedArrayPosition, decodedArrayLimit, encodedArray, encodedArrayInsertionCount, prefix);

                // 0x0d -> 0x7f 0x72 ('r')
                encodedArray[decodedArrayPosition + encodedArrayInsertionCount[0] + prefixLength] = 0x7f;
                encodedArrayInsertionCount[0]++;
                encodedArray[decodedArrayPosition + encodedArrayInsertionCount[0] + prefixLength] = 0x72;

                // advance to next byte
                continue;
            case 0x7f:
                // escape prefix byte, requires multi-byte escaped
                // representation
                encodedArray = supplyEncodedArray(decodedArray, decodedArrayOffset, decodedArrayPosition, decodedArrayLimit, encodedArray, encodedArrayInsertionCount, prefix);

                // 0x7f -> 0x7f 0x7f
                encodedArray[decodedArrayPosition + encodedArrayInsertionCount[0] + prefixLength] = 0x7f;
                encodedArrayInsertionCount[0]++;
                encodedArray[decodedArrayPosition + encodedArrayInsertionCount[0] + prefixLength] = 0x7f;

                // advance to next byte
                continue;
            default:
                if (encodedArray != null) {
                    // not escaping character, copy this byte to encodedValue
                    encodedArray[decodedArrayPosition + encodedArrayInsertionCount[0] + prefixLength] = decodedValue;
                }

                continue;
            }

        }
        return encodedArray;

    }

    private static byte[] supplyEncodedArray(byte[] decodedArray, int decodedArrayOffset, int decodedArrayPosition, int decodedArrayLimit, byte[] encodedArray, int[] encodedArrayInsertionCount, ByteBuffer prefix) {
        if (encodedArray == null) {
            int decodedRemaining = decodedArrayLimit - decodedArrayPosition;
            encodedArray = new byte[decodedArrayPosition + decodedRemaining * 2 + prefix.remaining()];
        }
        if (encodedArrayInsertionCount[0] == 0) { //first escaped character encounter, copy read bytes until first escape byte
            System.arraycopy(prefix.array(), 0, encodedArray, decodedArrayOffset, prefix.remaining()); //copy prefix
            System.arraycopy(decodedArray, decodedArrayOffset, encodedArray, decodedArrayOffset + prefix.remaining(), decodedArrayPosition - decodedArrayOffset); //copy previous
        }
        return encodedArray;
    }

    private IoBufferEx doEncode(IoBufferAllocatorEx<?> allocator, int flags, IoBufferEx ioBuf, byte opCode) {
        ByteBuffer prefix = calculatePrefixBytes(ioBuf.buf().remaining(), opCode);
        return escapeZeroAndNewLine(allocator, flags, ioBuf, prefix);
    }

    @Override
    protected IoBufferEx doTextEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message) {
        return doEncode(allocator, flags, message.getBytes(), SPECIFIED_LENGTH_TEXT_TYPE_BYTE);
    }

    @Override
    protected IoBufferEx doBinaryEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message) {
        return doEncode(allocator, flags, message.getBytes(), BINARY_TYPE_BYTE);
    }

    @Override
    protected IoBufferEx doPingEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message) {
        WsPingMessage ping = (WsPingMessage)message;
        assert ping.getBytes().remaining() == 0 : "PING with payload not supported";
        ByteBuffer text = allocator.allocate(EMPTY_PING_BYTES.length, flags);
        int offset = text.position();
        text.put(EMPTY_PING_BYTES);
        text.flip();
        text.position(offset);
        return allocator.wrap(text, flags);
    }
    @Override
    protected IoBufferEx doPongEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message) {
        WsPongMessage ping = (WsPongMessage)message;
        assert ping.getBytes().remaining() == 0 : "PONG with payload not supported";
        ByteBuffer text = allocator.allocate(EMPTY_PONG_BYTES.length, flags);
        int offset = text.position();
        text.put(EMPTY_PONG_BYTES);
        text.flip();
        text.position(offset);
        return allocator.wrap(text, flags);
    }

}
