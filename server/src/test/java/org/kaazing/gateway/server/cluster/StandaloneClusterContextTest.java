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
package org.kaazing.gateway.server.cluster;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MapEvent;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryRemovedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;

import org.junit.Test;
import org.kaazing.gateway.server.context.resolve.StandaloneClusterContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StandaloneClusterContextTest {

    @Test
    public void testIMapImplEntryListenerSupportPut() {
        StandaloneClusterContext context = new StandaloneClusterContext();
        IMap<String, String> imap = context.getCollectionsFactory().getMap("test");
        String value1 = "value1";
        EntryListenerImpl<String, String> listener1 = new EntryListenerImpl<>();
        EntryListenerImpl<String, String> listener2 = new EntryListenerImpl<>();
        imap.addEntryListener(listener1, true);
        imap.addEntryListener(listener2, true);
        imap.put("test1", value1);
        assertEquals(listener1.addedCount, 1);
        assertEquals(listener1.udpatedCount, 0);
        assertEquals(listener1.removedCount, 0);
        assertEquals(listener2.addedCount, 1);
        assertEquals(listener2.udpatedCount, 0);
        assertEquals(listener2.removedCount, 0);
        assertEquals(imap.get("test1"), value1);
    }

    @Test
    public void testIMapImplEntryListenerSupportRemove() {
        StandaloneClusterContext context = new StandaloneClusterContext();
        IMap<String, String> imap = context.getCollectionsFactory().getMap("test");
        String value1 = "value1";
        EntryListenerImpl<String, String> listener1 = new EntryListenerImpl<>();
        EntryListenerImpl<String, String> listener2 = new EntryListenerImpl<>();
        imap.addEntryListener(listener1, true);
        imap.addEntryListener(listener2, true);
        imap.put("test1", value1);
        assertEquals(imap.get("test1"), value1);
        imap.remove("test1");
        assertEquals(listener1.addedCount, 1);
        assertEquals(listener1.udpatedCount, 0);
        assertEquals(listener1.removedCount, 1);
        assertEquals(listener2.addedCount, 1);
        assertEquals(listener2.udpatedCount, 0);
        assertEquals(listener2.removedCount, 1);
    }

    @Test
    public void testIMapImplRemoveWithValue() {
        StandaloneClusterContext context = new StandaloneClusterContext();
        IMap<String, String> imap = context.getCollectionsFactory().getMap("test");
        String value1 = "value1";
        String value2 = "value2";
        EntryListenerImpl<String, String> listener1 = new EntryListenerImpl<>();
        imap.addEntryListener(listener1, true);
        imap.put("test1", value1);
        assertEquals(listener1.addedCount, 1);
        assertEquals(listener1.udpatedCount, 0);
        assertEquals(imap.get("test1"), value1);
        imap.put("test1", value2);
        assertEquals(listener1.addedCount, 1);
        assertEquals(listener1.udpatedCount, 1);
        assertEquals(imap.get("test1"), value2);
        imap.remove("test1", value2);
        assertEquals(listener1.removedCount, 1);
        assertEquals(false, imap.remove("test1", value1));
    }

    @Test
    public void testIMapImplEvict() {
        StandaloneClusterContext context = new StandaloneClusterContext();
        IMap<String, String> imap = context.getCollectionsFactory().getMap("test");
        String value1 = "value1";
        EntryListenerImpl<String, String> listener1 = new EntryListenerImpl<>();
        imap.addEntryListener(listener1, true);
        imap.put("test1", value1);
        assertEquals(imap.get("test1"), value1);
        assertEquals(listener1.addedCount, 1);
        assertEquals(listener1.evictedCount, 0);
        imap.evict("test1");
        assertEquals(listener1.addedCount, 1);
        assertEquals(listener1.evictedCount, 1);
    }

    @Test
    public void testIMapImplEntryListenerSupportReplace() {
        StandaloneClusterContext context = new StandaloneClusterContext();
        IMap<String, String> imap = context.getCollectionsFactory().getMap("test");
        String value1 = "value1";
        EntryListenerImpl<String, String> listener1 = new EntryListenerImpl<>();
        imap.addEntryListener(listener1, true);
        imap.put("test1", value1);
        assertEquals(imap.get("test1"), value1);
        imap.replace("test1", "value2");
        assertEquals(listener1.addedCount, 1);
        assertEquals(listener1.udpatedCount, 1);
        assertEquals(listener1.removedCount, 0);
        assertTrue(imap.get("test1").equals("value2"));
    }

    @Test
    public void testIMapImplEntryListenerSupportValue() {
        StandaloneClusterContext context = new StandaloneClusterContext();
        IMap<String, String> imap = context.getCollectionsFactory().getMap("test");
        final String value1 = "value1";
        final String value2 = "value2";
        imap.addEntryListener(new EntryAddedListener<String, String>() {
            @Override
            public void entryAdded(EntryEvent<String, String> event) {
                assertEquals(event.getValue(), value1);
            }
        }, true);

        imap.addEntryListener(new EntryRemovedListener<String, String>() {
            @Override
            public void entryRemoved(EntryEvent<String, String> event) {
                assertEquals(event.getValue(), value2);
            }
        }, true);

        imap.addEntryListener(new EntryUpdatedListener<String, String>() {
            @Override
            public void entryUpdated(EntryEvent<String, String> event) {
                assertEquals(event.getValue(), value2);
            }
        }, true);

        imap.put("test1", value1);
        imap.put("test1", value2);
        imap.remove("test1");
    }

    @Test
    public void testIMapImplEntryListenerNoSupportValue() {
        StandaloneClusterContext context = new StandaloneClusterContext();
        IMap<String, String> imap = context.getCollectionsFactory().getMap("test");
        final String value1 = "value1";
        final String value2 = "value2";
        imap.addEntryListener(new EntryAddedListener<String, String>() {
            @Override
            public void entryAdded(EntryEvent<String, String> event) {
                assertEquals(event.getValue(), null);
            }
        }, false);

        imap.addEntryListener(new EntryRemovedListener<String, String>() {
            @Override
            public void entryRemoved(EntryEvent<String, String> event) {
                assertEquals(event.getValue(), null);
            }
        }, false);

        imap.addEntryListener(new EntryUpdatedListener<String, String>() {
            @Override
            public void entryUpdated(EntryEvent<String, String> event) {
                assertEquals(event.getValue(), null);
            }
        }, false);

        imap.put("test1", value1);
        imap.put("test1", value2);
        imap.remove("test1");
    }

    class EntryListenerImpl<K, V> implements EntryListener<K, V> {

        private int addedCount;
        private int removedCount;
        private int udpatedCount;
        private int evictedCount;

        @Override
        public void entryAdded(EntryEvent<K, V> event) {
            addedCount++;
        }

        @Override
        public void entryEvicted(EntryEvent<K, V> event) {
            evictedCount++;
        }

        @Override
        public void entryRemoved(EntryEvent<K, V> event) {
            removedCount++;

        }

        @Override
        public void entryUpdated(EntryEvent<K, V> event) {
            udpatedCount++;
        }

        @Override
        public void mapCleared(MapEvent event) {
            removedCount += event.getNumberOfEntriesAffected();
        }

        @Override
        public void mapEvicted(MapEvent event) {
            evictedCount += event.getNumberOfEntriesAffected();
        }
    }
}
