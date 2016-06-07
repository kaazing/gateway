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
package org.apache.mina.core.buffer;

import java.nio.ByteBuffer;

/**
 * Allocates {@link IoBuffer}s and manages them.  Please implement this
 * interface if you need more advanced memory management scheme.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface IoBufferAllocator {
    /**
     * Returns the buffer which is capable of the specified size.
     *
     * @param capacity the capacity of the buffer
     * @param direct <tt>true</tt> to get a direct buffer,
     *               <tt>false</tt> to get a heap buffer.
     */
    IoBuffer allocate(int capacity, boolean direct);

    /**
     * Returns the NIO buffer which is capable of the specified size.
     *
     * @param capacity the capacity of the buffer
     * @param direct <tt>true</tt> to get a direct buffer,
     *               <tt>false</tt> to get a heap buffer.
     */
    ByteBuffer allocateNioBuffer(int capacity, boolean direct);

    /**
     * Wraps the specified NIO {@link ByteBuffer} into MINA buffer.
     */
    IoBuffer wrap(ByteBuffer nioBuffer);

    /**
     * Dispose of this allocator.
     */
    void dispose();
}
