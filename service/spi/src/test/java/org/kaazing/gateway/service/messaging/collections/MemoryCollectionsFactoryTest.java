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

import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;

import org.junit.Before;
import org.junit.Test;

import com.hazelcast.core.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.SqlPredicate;

public class MemoryCollectionsFactoryTest {
    private static final String MAP_NAME = "TestMap";

    private MemoryCollectionsFactory factory;

    // In support of http://jira.kaazing.wan/browse/KG-5001, the
    // MemoryCollectionsFactory's IMap implementation needs to handle
    // querying for keys, values, entries using Predicates.

    private class TestObject {
        private final String value;

        public TestObject(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null ||
                !(o instanceof TestObject)) {
                return false; 
            }

            TestObject that = (TestObject) o;
            return that.hashCode() == hashCode();
        }

        @Override
        public String toString() {
            return String.format("{ value=%s }", value);
        }
    }

    @Before
    public void setUp()
        throws Exception {

        factory = new MemoryCollectionsFactory();
    }

    @Test
    public void shouldGetIMap()
        throws Exception {

        IMap<String, String> map = factory.getMap(MAP_NAME);
        Assert.assertTrue("Expected map, got null", map != null);
        Assert.assertTrue(String.format("Expected map name '%s', got '%s'",
            MAP_NAME, map.getName()), map.getName().equals(MAP_NAME));
    }

    @Test
    public void shouldGetIMapLocalKeySet()
        throws Exception {

        IMap<String, String> map = factory.getMap(MAP_NAME);

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
        Assert.assertTrue("Expected key set, got null", keys != null);
        Assert.assertTrue(String.format("Expected keys %s, got %s", expected, keys), keys.equals(expected));
    }

    @Test
    public void shouldGetIMapLocalKeySetByPredicate()
        throws Exception {

        IMap<String, TestObject> map = factory.getMap(MAP_NAME);
        map.addIndex("value", false);

        // Add three keys
        String k1 = "foo";
        String v1 = "1";
        map.put(k1, new TestObject(v1));

        String k2 = "bar";
        String v2 = "2";
        map.put(k2, new TestObject(v2));

        String k3 = "baz";
        String v3 = "3";
        map.put(k3, new TestObject(v3));

        Set<String> expected = new HashSet<>();
        expected.add(k1);
        expected.add(k2);

        Predicate query = new SqlPredicate("value = '1' OR value = '2'");
        Set<String> keys = map.localKeySet(query);
        Assert.assertTrue("Expected key set, got null", keys != null);
        Assert.assertTrue(String.format("Expected keys %s, got %s", expected, keys), keys.equals(expected));
    }

    @Test
    public void shouldGetIMapKeySetByPredicate()
        throws Exception {

        IMap<String, TestObject> map = factory.getMap(MAP_NAME);
        map.addIndex("value", false);

        // Add three keys
        String k1 = "foo";
        String v1 = "1";
        map.put(k1, new TestObject(v1));

        String k2 = "bar";
        String v2 = "2";
        map.put(k2, new TestObject(v2));

        String k3 = "baz";
        String v3 = "3";
        map.put(k3, new TestObject(v3));

        Set<String> expected = new HashSet<>();
        expected.add(k1);
        expected.add(k2);

        Predicate query = new SqlPredicate("value = '1' OR value = '2'");
        Set<String> keys = map.keySet(query);
        Assert.assertTrue("Expected key set, got null", keys != null);
        Assert.assertTrue(String.format("Expected keys %s, got %s", expected, keys), keys.equals(expected));
    }

    @Test
    public void shouldGetIMapValuesByPredicate()
        throws Exception {

        IMap<String, TestObject> map = factory.getMap(MAP_NAME);
        map.addIndex("value", false);

        // Add three keys
        String k1 = "foo";
        TestObject v1 = new TestObject("1");
        map.put(k1, v1);

        String k2 = "bar";
        TestObject v2 = new TestObject("2");
        map.put(k2, v2);

        String k3 = "baz";
        TestObject v3 = new TestObject("3");
        map.put(k3, v3);

        Collection<TestObject> expected = new HashSet<>();
        expected.add(v1);
        expected.add(v2);

        Predicate query = new SqlPredicate("value = '1' OR value = '2'");
        Collection<TestObject> values = map.values(query);
        Assert.assertTrue("Expected values, got null", values != null);
        Assert.assertTrue(String.format("Expected values %s, got %s", expected, values), values.equals(expected));
    }

    @Test
    public void shouldGetIMapEntrySetByPredicate()
        throws Exception {

        IMap<String, TestObject> map = factory.getMap(MAP_NAME);
        map.addIndex("value", false);

        // Add three keys
        String k1 = "foo";
        TestObject v1 = new TestObject("1");
        map.put(k1, v1);

        String k2 = "bar";
        TestObject v2 = new TestObject("2");
        map.put(k2, v2);

        String k3 = "baz";
        TestObject v3 = new TestObject("3");
        map.put(k3, v3);

        Set<Map.Entry<String, TestObject>> expected = map.entrySet();

        Predicate query = new SqlPredicate("value != '4'");
        Set<Map.Entry<String, TestObject>> entries = map.entrySet(query);
        Assert.assertTrue("Expected entries, got null", entries != null);
        Assert.assertTrue(String.format("Expected entries %s, got %s", expected, entries), entries.equals(expected));
    }

    @Test
    public void expiringMap() throws Exception {
        IMap<String, String> map = factory.getMap(MAP_NAME);

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
}
