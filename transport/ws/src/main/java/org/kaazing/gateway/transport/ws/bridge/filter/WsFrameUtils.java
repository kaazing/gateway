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
package org.kaazing.gateway.transport.ws.bridge.filter;

import java.nio.ByteBuffer;

/**
 * A utility class for managing operations on WebSocket frames.
 */
public class WsFrameUtils {

    /**
     * Masks source buffer into destination buffer.
     *
     * @param src   the buffer containing readable bytes to be masked
     * @param dst   the buffer where masked bytes are written
     * @param mask  the mask to apply
     */
    public static void xor(ByteBuffer src, ByteBuffer dst, int mask) {
    	int remainder = src.remaining() % 4;
    	int remaining = src.remaining() - remainder;
    	int end = remaining + src.position();

    	// xor a 32bit word at a time as long as possible
    	while (src.position() < end) {
    		int masked = src.getInt() ^ mask;
    		dst.putInt(masked);
    	}

    	// xor the remaining 3, 2, or 1 bytes
    	byte b;
    	switch (remainder) {
    	case 3:
    		b = (byte) (src.get() ^ ((mask >> 24) & 0xff));
    		dst.put(b);
    		b = (byte) (src.get() ^ ((mask >> 16) & 0xff));
    		dst.put(b);
    		b = (byte) (src.get() ^ ((mask >> 8) & 0xff));
    		dst.put(b);
    		break;
    	case 2:
    		b = (byte) (src.get() ^ ((mask >> 24) & 0xff));
    		dst.put(b);
    		b = (byte) (src.get() ^ ((mask >> 16) & 0xff));
    		dst.put(b);
    		break;
    	case 1:
    		b = (byte) (src.get() ^ (mask >> 24));
    		dst.put(b);
    		break;
    	case 0:
		default:
			break;
    	}
    }
}
