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
package org.kaazing.gateway.server.util.collection;

import java.util.Queue;
import org.junit.Test;
import static org.junit.Assert.*;

public class FixedArrayQueueTest {

    @Test
    public void addAndPollShouldReplayInOrder() {
        Queue<Integer> queue = new FixedArrayQueue<>(5);
        for (int i = 0; i < 5; i++) {
            queue.add(i);
        }
        for (int i = 0; i < 5; i++) {
            assertEquals(new Integer(i), queue.poll());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void addShouldThrowExceptionWhenFull() {
        Queue<Integer> queue = new FixedArrayQueue<>(2);
        assertTrue(queue.offer(1));
        assertTrue(queue.offer(1));
        queue.add(2);
    }

    @Test
    public void offerAndPeekShouldReplayInOrder() {
        Queue<Integer> queue = new FixedArrayQueue<>(5);
        for (int i = 0; i < 5; i++) {
            assertTrue(queue.offer(i));
        }
        for (int i = 0; i < 5; i++) {
            assertEquals(new Integer(i), queue.peek());
            assertEquals(new Integer(i), queue.poll());
        }
    }

    @Test
    public void offerShouldReturnFalseWhenFull() {
        Queue<Integer> queue = new FixedArrayQueue<>(1);
        assertTrue(queue.offer(1));
        assertFalse(queue.offer(2));
    }

    @Test
    public void peekOnEmptyQueueShouldReturnNull() {
        Queue<Integer> queue = new FixedArrayQueue<>(5);
        assertNull(queue.peek());
    }

    @Test
    public void pollOnEmptyQueueShouldReturnNull() {
        Queue<Integer> queue = new FixedArrayQueue<>(5);
        assertNull(queue.peek());
    }

}
