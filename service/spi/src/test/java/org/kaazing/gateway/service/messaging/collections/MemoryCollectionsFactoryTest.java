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
package org.kaazing.gateway.service.messaging.collections;

import static java.lang.String.format;
import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.service.collections.MemoryCollectionsFactory;
import org.kaazing.gateway.util.AtomicCounter;

import com.hazelcast.core.IList;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;

public class MemoryCollectionsFactoryTest {
    private static final String OBJECT_NAME = "TestObject";
    private static final String ANOTHER_OBJECT_NAME = "anotherTestObject";

    private MemoryCollectionsFactory factory;

    @Before
    public void setUp() throws Exception {
        factory = new MemoryCollectionsFactory();
    }

    @Test
    public void shouldGetIMap() throws Exception {
        IMap<String, String> map = factory.getMap(OBJECT_NAME);

        assertTrue("Expected map, got null", map != null);
        assertTrue(format("Expected map name '%s', got '%s'", OBJECT_NAME, map.getName()), map.getName().equals(OBJECT_NAME));
    }

    @Test
    public void shouldGetIMapLocalKeySet() throws Exception {
        IMap<String, String> map = factory.getMap(OBJECT_NAME);

        // Add three keys
        String k1 = "foo";
        String v1 = "1";
        map.put(k1, v1);

        String k2 = "bar";
        String v2 = "2";
        map.put(k2, v2);

        String k3 = "baz";
        String v3 = "3";
        map.put(k3, v3);

        Set<String> expected = new HashSet<>();
        expected.add(k1);
        expected.add(k2);
        expected.add(k3);

        Set<String> keys = map.localKeySet();
        assertTrue("Expected key set, got null", keys != null);
        assertTrue(format("Expected keys %s, got %s", expected, keys), keys.equals(expected));
    }

    @Test
    public void shouldTestMapEntryExpiry() throws Exception {
        IMap<String, String> map = factory.getMap(OBJECT_NAME);

        map.putIfAbsent("one", "1", 1, MILLISECONDS);
        map.putIfAbsent("two", "2", 10000, MILLISECONDS);
        map.putIfAbsent("three", "3", 2, MILLISECONDS);

        sleep(5);

        assertNull(map.putIfAbsent("one", "10", 1, MILLISECONDS));
        assertEquals("2", map.putIfAbsent("two", "20", 1000, MILLISECONDS));
        assertNull(map.putIfAbsent("three", "30", 10000, MILLISECONDS));

        sleep(5);

        assertNull(map.get("one"));
        assertEquals("2", map.get("two"));
        assertEquals("30", map.get("three"));

        sleep(5);

        assertFalse(map.remove("one", "10"));
        assertTrue(map.remove("three", "30"));
        assertFalse(map.remove("three", "30"));
    }

    @Test
    public void shouldGetIList() throws Exception {
        IList<String> list = factory.getList(OBJECT_NAME);

        assertNotNull("Expected list, got null", list);
        assertEquals(format("Expected list name '%s', got '%s'", OBJECT_NAME, list.getName()), OBJECT_NAME, list.getName());
        assertEquals(format("Expected the same list instance for name \"%s\"", OBJECT_NAME), list, factory.getList(OBJECT_NAME));
        assertNotEquals(format("Expected different list instance for name \"%s\"", ANOTHER_OBJECT_NAME), list,
                factory.getList(ANOTHER_OBJECT_NAME));
    }

    @Test
    public void shouldGetILock() throws Exception {
        ILock lock = factory.getLock(OBJECT_NAME);

        assertNotNull("Expected a lock, got null", lock);
        assertEquals(format("Expected lock name '%s', got '%s'", OBJECT_NAME, lock.getName()), OBJECT_NAME, lock.getName());
        assertEquals(format("Expected the same lock instance for name \"%s\"", OBJECT_NAME), lock, factory.getLock(OBJECT_NAME));
        assertNotEquals(format("Expected different lock instance for name \"%s\"", ANOTHER_OBJECT_NAME), lock,
                factory.getLock(ANOTHER_OBJECT_NAME));
    }

    @Test
    public void shouldGetAtomicCounter() throws Exception {
        AtomicCounter atomicCounter = factory.getAtomicCounter(OBJECT_NAME);

        assertNotNull("Expected an atomic counter, got null", atomicCounter);
        assertEquals(format("Expected the same atomic counter instance for name \"%s\"", OBJECT_NAME), atomicCounter,
                factory.getAtomicCounter(OBJECT_NAME));
        assertNotEquals(format("Expected different atomic counter instance for name \"%s\"", ANOTHER_OBJECT_NAME), atomicCounter,
                factory.getAtomicCounter(ANOTHER_OBJECT_NAME));
    }

    @Test
    public void shouldCheckAtomicCounterOperations() throws Exception {
        AtomicCounter atomicCounter = factory.getAtomicCounter(OBJECT_NAME);

        assertEquals(0, atomicCounter.get());
        assertEquals(1, atomicCounter.incrementAndGet());
        assertEquals(0, atomicCounter.decrementAndGet());
        assertEquals(true, atomicCounter.compareAndSet(0, 1));
        assertEquals(false, atomicCounter.compareAndSet(0, 1));
        assertEquals(1, factory.getAtomicCounter(OBJECT_NAME).get());
    }

    @Test
    public void shouldGetSameITopicFromMultipleThreads() throws InterruptedException {
        ITopic<Object> topic = factory.getTopic(OBJECT_NAME);
        topic.getLocalTopicStats();
        Thread t = new Thread(() -> {
            ITopic<Object> otherTopic = factory.getTopic(OBJECT_NAME);
            assertEquals("Should receive same topic instace for the same name", topic, otherTopic);
        });
        t.start();
        t.join();
    }
}
