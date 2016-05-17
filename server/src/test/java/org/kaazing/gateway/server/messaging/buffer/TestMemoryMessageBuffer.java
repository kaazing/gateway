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
package org.kaazing.gateway.server.messaging.buffer;

import org.junit.Test;
import org.kaazing.gateway.server.messaging.DefaultMessagingMessage;
import org.kaazing.gateway.service.messaging.buffer.MessageBuffer;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


// TODO: add youngest and oldest message id tests
// TODO: add test of messages and ids while in loop
// TODO: add case to verify addMessage result (compare frames)
public class TestMemoryMessageBuffer {

    @Test
    public void testBufferSmallData() throws Exception {
        MessageBuffer buffer = new MemoryMessageBuffer(10);
        for (int i = 0; i < 5; i++) {
            buffer.add(new DefaultMessagingMessage());
        }

        assertTrue(buffer.get(-1000) == null);
        assertTrue(buffer.get(-1) == null);
        assertTrue(buffer.get(0) == null);
        assertTrue(buffer.get(6) == null);
        assertTrue(buffer.get(10) == null);
        assertTrue(buffer.get(1000) == null);

        assertNotNull(buffer.get(1));
        assertTrue(buffer.get(1).getId() == 1);

        assertNotNull(buffer.get(3));
        assertTrue(buffer.get(3).getId() == 3);

        assertNotNull(buffer.get(5));
        assertTrue(buffer.get(5).getId() == 5);
    }

    @Test
    public void testBuffer() throws Exception {

        // test with 10 buffer and 5 items

        MessageBuffer buffer = new MemoryMessageBuffer(10);
        for (int i = 0; i < 20; i++) {
            buffer.add(new DefaultMessagingMessage());
        }

        assertTrue(buffer.get(-1000) == null);
        assertTrue(buffer.get(-1) == null);
        assertTrue(buffer.get(0) == null);
        assertTrue(buffer.get(5) == null);
        assertTrue(buffer.get(9) == null);
        assertTrue(buffer.get(10) == null);
        assertTrue(buffer.get(21) == null);
        assertTrue(buffer.get(1000) == null);

        assertNotNull(buffer.get(11));
        assertTrue(buffer.get(11).getId() == 11);

        assertNotNull(buffer.get(15));
        assertTrue(buffer.get(15).getId() == 15);

        assertNotNull(buffer.get(20));
        assertTrue(buffer.get(20).getId() == 20);
    }

    @Test
    public void testLargeBuffer() throws Exception {

        MessageBuffer buffer = new MemoryMessageBuffer(1000);
        for (int i = 0; i < 20000; i++) {
            buffer.add(new DefaultMessagingMessage());
        }

        assertTrue(buffer.get(-10000) == null);
        assertTrue(buffer.get(-1000) == null);
        assertTrue(buffer.get(-1) == null);
        assertTrue(buffer.get(200000) == null);
        assertTrue(buffer.get(0) == null);
        assertTrue(buffer.get(5) == null);
        assertTrue(buffer.get(9) == null);
        assertTrue(buffer.get(19000) == null);
        assertTrue(buffer.get(20001) == null);

        assertNotNull(buffer.get(19001));
        assertTrue(buffer.get(19001).getId() == 19001);

        assertNotNull(buffer.get(19500));
        assertTrue(buffer.get(19500).getId() == 19500);

        assertNotNull(buffer.get(20000));
        assertTrue(buffer.get(20000).getId() == 20000);
    }

    @Test
    public void testLastMessageId() throws Exception {

        MessageBuffer buffer = new MemoryMessageBuffer(1000);
        for (int i = 0; i < 20000; i++) {
            buffer.add(new DefaultMessagingMessage());
            int lastMessageId = buffer.getYoungestId();
            assertTrue(lastMessageId == i + 1);
        }

    }

}
