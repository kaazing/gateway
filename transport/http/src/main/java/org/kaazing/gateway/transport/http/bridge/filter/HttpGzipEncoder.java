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
package org.kaazing.gateway.transport.http.bridge.filter;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;

public class HttpGzipEncoder implements HttpContentWriter {
	
    @Override
    public IoBufferEx write(IoBufferEx source, IoBufferAllocatorEx<?> allocator) {
        return writeGzipped(source, allocator);
    }
    
    public static final int GZIP_PREFIX_SIZE = 5;

    /*
     * Gzipped Transfer-Encoding (trivial passthrough)
     */
	private static IoBufferEx writeGzipped(IoBufferEx sourceIoBuf, IoBufferAllocatorEx<?> allocator) {
        ByteBuffer source = sourceIoBuf.buf();
        int position = source.position();
        int remaining = source.remaining();
        if (remaining == 0) {
        	return sourceIoBuf;
        }
        // TODO: consider ability to share
        if (source.hasArray() && source.array() == SSE_4K_PADDING_BYTES) {
            return allocator.wrap(ByteBuffer.wrap(GZIPPED_SSE_4K_PADDING_BYTES));
        }
        if (source.hasArray() && source.array() == WSEB_4K_PADDING_BYTES) {
            return allocator.wrap(ByteBuffer.wrap(GZIPPED_WSEB_4K_PADDING_BYTES));
        }
        
        // We don't implement Gzip - instead simply prepend gzip preamble
        if (position >= GZIP_PREFIX_SIZE) {
            // Note: duplicate first to support parallel encoding (atomic race condition)
            ByteBuffer newSource = source.duplicate();

            // Rewind 5 bytes
            newSource.position(position - GZIP_PREFIX_SIZE);
            
            int start = newSource.position();
            
            // [0] + [little endian u16 length] + [1s complement of the u16 length]
            newSource.put((byte)0);

            // little endian length
            byte loByte = (byte)(remaining & 0xFF);
            byte hiByte = (byte)((remaining>>8) & 0xFF);
            newSource.put(loByte);
            newSource.put(hiByte);
            
            // 1's complement of the little endian length
            newSource.put((byte)~loByte);
            newSource.put((byte)~hiByte);

            // Rewind back to starting position
            newSource.position(start);
            
            return allocator.wrap(newSource, sourceIoBuf.flags());
        }
        else {
            ByteBuffer newSource = allocator.allocate(GZIP_PREFIX_SIZE + remaining);
            int offset = newSource.position();
            // [0] + [little endian u16 length] + [1s complement of the u16 length]
            newSource.put((byte)0);

            // little endian length
            byte loByte = (byte)(remaining & 0xFF);
            byte hiByte = (byte)((remaining >> 8) & 0xFF);
            newSource.put(loByte);
            newSource.put(hiByte);
            
            // 1's complement of the little endian length
            newSource.put((byte)~loByte);
            newSource.put((byte)~hiByte);

            // (KG-8125) if shared, duplicate to ensure we don't affect other threads
            if (sourceIoBuf.isShared()) {
                newSource.put(source.duplicate());
            }
            else {
                int sourcePos = source.position();
                newSource.put(source);
                source.position(sourcePos);
            }
            newSource.flip();
            newSource.position(offset);

            return allocator.wrap(newSource, sourceIoBuf.flags());
        }
    }
	
    /*
     * GZip Content-Encoding
     */
    
    // gzip start of stream header
    public static final byte[] GZIP_START_OF_FRAME_BYTES = new byte[] { 31, (byte)139, 8, 0, (byte)240, 106, 64, 78, 2, (byte)255 };

    /* SSE Padding (for Android) */
    private static final byte[] GZIPPED_SSE_4K_PADDING_BYTES = new byte[/*31*/] {
        (byte)236, (byte)192, 65, 9, 0, 0, 8, 3,
        (byte)192, (byte)191, 96, 7, 115, (byte)216, 63, (byte)216,
        122, (byte)140, (byte)187, 63, 0, 0, 0, (byte)160,
        (byte)218, 78, 0, 0, 0, (byte)255, (byte)255
    };
    
    public static final byte[] SSE_4K_PADDING_BYTES;
    static {
        /* Assemble SSE_COMMENT_4K_PADDING_BYTES */
        int len = 4096;
        byte[] bytes = new byte[len]; 
        bytes[0] = (byte)0x3A;                    // colon
        Arrays.fill(bytes, 1, len-2, (byte)0x20); // spaces...
        bytes[len-2] = (byte)0x0A;                // linefeed
        bytes[len-1] = (byte)0x0A;                // linefeed
        
        SSE_4K_PADDING_BYTES = bytes; // compressed form is GZIPPED_SSE_4K_PADDING_BYTES
    }

    private static final byte[] GZIPPED_WSEB_4K_PADDING_BYTES = new byte[] {
        (byte)236, (byte)192, 1, 13, 0, 0, 8, 2,
        (byte)176, (byte)217, (byte)200, (byte)254, (byte)229, (byte)160, 7, (byte)251,
        (byte)239, 1, 0, 0, (byte)128, 113, 41, 0,
        0, 0, (byte)255, (byte)255
    };

    /* WSEB Padding (for Android) */
	public static final byte[] WSEB_4K_PADDING_BYTES;
	static {
	    /* Assemble WSEB_COMMENT_4K_PADDING_BYTES */
	    int len = 4096;
	    byte[] bytes = new byte[len]; 
	    bytes[0] = (byte)0x01;                    // command frame type
	    Arrays.fill(bytes, 1, len-1, (byte)0x30); // zeroes ('0')...
	    bytes[len-1] = (byte)0xFF;                // text terminator

	    WSEB_4K_PADDING_BYTES = bytes;  // compressed form is GZIPPED_WSEB_4K_PADDING_BYTES
	}
}