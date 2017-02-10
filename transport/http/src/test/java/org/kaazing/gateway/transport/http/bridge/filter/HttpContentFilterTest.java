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

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.junit.Test;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.buffer.SimpleBufferAllocator;

public class HttpContentFilterTest {

	@Test
	public void testWriteGzipped() throws Exception {
	    IoBufferAllocatorEx<?> allocator = SimpleBufferAllocator.BUFFER_ALLOCATOR;
		HttpGzipEncoder encoder = new HttpGzipEncoder();
        IoBufferEx buf = allocator.wrap(ByteBuffer.wrap(new byte[] { 0, 1, 2, 3 }));
		IoBufferEx actual = encoder.write(buf, allocator);
		IoBufferEx expected = allocator.wrap(ByteBuffer.wrap(new byte[] { 0, 0x4, 0, (byte)0xfb, (byte)0xff, 0, 1, 2, 3 }));
		
		assertEquals(actual, expected);
	}
	
	@Test
	public void testWriteGzippedDirectBuffer() throws Exception {
	    IoBufferAllocatorEx<?> allocator = SimpleBufferAllocator.BUFFER_ALLOCATOR;
		HttpGzipEncoder encoder = new HttpGzipEncoder();
		byte[] bytes = new byte[] { 0, 1, 2, 3 };
		ByteBuffer data = ByteBuffer.allocateDirect(bytes.length);
		data.put(bytes);
		data.flip();
        IoBufferEx buf = allocator.wrap(data);
		IoBufferEx actual = encoder.write(buf, allocator);
		IoBufferEx expected = allocator.wrap(ByteBuffer.wrap(new byte[] { 0, 0x4, 0, (byte)0xfb, (byte)0xff, 0, 1, 2, 3 }));
		
		assertEquals(actual, expected);
	}

}
