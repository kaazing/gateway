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
package org.kaazing.gateway.transport.ws.bridge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.junit.Test;
import org.kaazing.gateway.transport.BridgeCodecSession;
import org.kaazing.gateway.transport.bridge.CachingMessageEncoder;
import org.kaazing.gateway.transport.io.IoMessage;
import org.kaazing.gateway.transport.io.filter.IoMessageEncoder;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.buffer.SimpleBufferAllocator;


public class MessageTest {

	private static final Charset UTF_8 = Charset.forName("UTF-8");
	
	@Test
    public void encodedBuffersWithDifferentCacheKeysShouldNotCollide() throws Exception {
	    SimpleBufferAllocator allocator = SimpleBufferAllocator.BUFFER_ALLOCATOR;

	    BridgeCodecSession primary = new BridgeCodecSession("primary");
        ProtocolEncoderOutput primaryOutput = primary.getEncoderOutput();
        CachingMessageEncoder primaryMessageEncoder = primary.getMessageEncoder();

        BridgeCodecSession secondary = new BridgeCodecSession("secondary");
        ProtocolEncoderOutput secondaryOutput = secondary.getEncoderOutput();
        CachingMessageEncoder secondaryMessageEncoder = secondary.getMessageEncoder();
        
        ProtocolEncoder primaryEncoder = new IoMessageEncoder(primaryMessageEncoder, allocator);
        ProtocolEncoder primaryEncoder2 = new IoMessageEncoder(primaryMessageEncoder, allocator);
        
        ProtocolEncoder secondaryEncoder = new IoMessageEncoder(secondaryMessageEncoder, allocator);
        ProtocolEncoder secondaryEncoder2 = new IoMessageEncoder(secondaryMessageEncoder, allocator);

        IoBufferEx buf = allocator.wrap(ByteBuffer.wrap("abcd".getBytes(UTF_8)));
        IoMessage in = new IoMessage(buf);
        in.initCache();

        primaryEncoder.encode(primary, in, primaryOutput);
        primaryEncoder2.encode(primary, in, primaryOutput);
        secondaryEncoder.encode(secondary, in, secondaryOutput);
        secondaryEncoder2.encode(secondary, in, secondaryOutput);

        IoBuffer primaryOut = (IoBuffer)primary.getEncoderOutputQueue().poll();
        IoBuffer primaryOut2 = (IoBuffer)primary.getEncoderOutputQueue().poll();
        IoBuffer secondaryOut = (IoBuffer)secondary.getEncoderOutputQueue().poll();
        IoBuffer secondaryOut2 = (IoBuffer)secondary.getEncoderOutputQueue().poll();

        IoBufferEx expected = buf.asSharedBuffer();

        assertEquals(expected, primaryOut);
        assertEquals(expected, secondaryOut);
        assertSame(buf.array(), primaryOut.array());
        assertNotSame(buf.array(), secondaryOut.array());

        assertSame(primaryOut, primaryOut2);
        assertSame(secondaryOut, secondaryOut2);
        assertNotSame(primaryOut, secondaryOut);
	}

}
