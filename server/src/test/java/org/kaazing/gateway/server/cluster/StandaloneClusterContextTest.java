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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.kaazing.gateway.server.context.resolve.StandaloneClusterContext;

public class StandaloneClusterContextTest {
    
  
    CountDownLatch entryAdded, entryRemoved, entryUpdated, entryEvicted;

    @Test
    public void testIMapImplEntryListenerSupportPut() {
        StandaloneClusterContext context = new StandaloneClusterContext();
        IMap<String, String> imap = context.getCollectionsFactory().getMap("test1");
        String value1 = "value1";
        EntryListenerImpl<String, String> listener1 = new EntryListenerImpl<>();
        EntryListenerImpl<String, String> listener2 = new EntryListenerImpl<>();
        imap.addEntryListener(listener1, true);
        imap.addEntryListener(listener2, true);
        entryAdded = new CountDownLatch(1);
        imap.put("test1", value1);
        try {
            entryAdded.await(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        assertEquals(1, listener1.addedCount);
        assertEquals(0, listener1.udpatedCount);
        assertEquals(0, listener1.removedCount);
        assertEquals(1, listener2.addedCount);
        assertEquals(0, listener2.udpatedCount);
        assertEquals(0, listener2.removedCount);
        assertEquals(value1, imap.get("test1"));
    }

    @Test
    public void testIMapImplEntryListenerSupportRemove() {
        StandaloneClusterContext context = new StandaloneClusterContext();
        IMap<String, String> imap = context.getCollectionsFactory().getMap("test2");
        String value1 = "value1";
        EntryListenerImpl<String, String> listener1 = new EntryListenerImpl<>();
        EntryListenerImpl<String, String> listener2 = new EntryListenerImpl<>();
        imap.addEntryListener(listener1, true);
        imap.addEntryListener(listener2, true);
        entryAdded = new CountDownLatch(2);
        imap.put("test1", value1); 
        try {
            entryAdded.await(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        assertEquals(value1, imap.get("test1"));
        
        entryRemoved = new CountDownLatch(2);
        imap.remove("test1");
        try {
            entryRemoved.await(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        assertEquals(1, listener1.addedCount);
        assertEquals(0, listener1.udpatedCount);
        assertEquals(1, listener1.removedCount);
        assertEquals(1, listener2.addedCount);
        assertEquals(0, listener2.udpatedCount);
        assertEquals(1, listener2.removedCount);
    }

    @Test
    public void testIMapImplRemoveWithValue() {
        StandaloneClusterContext context = new StandaloneClusterContext();
        IMap<String, String> imap = context.getCollectionsFactory().getMap("test3");
        String value1 = "value1";
        String value2 = "value2";
        EntryListenerImpl<String, String> listener1 = new EntryListenerImpl<>();
        imap.addEntryListener(listener1, true);
      
        entryAdded = new CountDownLatch(1);
        imap.put("test1", value1);
        try {
            entryAdded.await(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        assertEquals(1, listener1.addedCount);
        assertEquals(0, listener1.udpatedCount);
        assertEquals(value1, imap.get("test1"));
        
        entryUpdated = new CountDownLatch(1);
        imap.put("test1", value2);
        try {
            entryUpdated.await(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        assertEquals(1, listener1.addedCount);
        assertEquals(1, listener1.udpatedCount);
        assertEquals(value2, imap.get("test1"));
        
        entryRemoved = new CountDownLatch(1);
        imap.remove("test1", value2);
        try {
            entryRemoved.await(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        assertEquals(1, listener1.removedCount);
        assertEquals(false, imap.remove("test1", value1));
    }

    @Test
    public void testIMapImplEvict() {
        StandaloneClusterContext context = new StandaloneClusterContext();
        IMap<String, String> imap = context.getCollectionsFactory().getMap("test4");
        String value1 = "value1";
        EntryListenerImpl<String, String> listener1 = new EntryListenerImpl<>();
        imap.addEntryListener(listener1, true);
        
        entryAdded = new CountDownLatch(1);
        imap.put("test1", value1);
        try {
            entryAdded.await(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        assertEquals(value1, imap.get("test1"));
        assertEquals(1, listener1.addedCount);
        assertEquals(0, listener1.evictedCount);
        
        entryEvicted = new CountDownLatch(1);
        imap.evict("test1");
        try {
            entryEvicted.await(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        assertEquals(1, listener1.addedCount);
        assertEquals(1, listener1.evictedCount);
    }

    @Test
    public void testIMapImplEntryListenerSupportReplace() {
        StandaloneClusterContext context = new StandaloneClusterContext();
        IMap<String, String> imap = context.getCollectionsFactory().getMap("test5");
        String value1 = "value1";
        EntryListenerImpl<String, String> listener1 = new EntryListenerImpl<>();
        imap.addEntryListener(listener1, true);
        
        entryAdded = new CountDownLatch(1);
        imap.put("test1", value1);
        try {
            entryAdded.await(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        assertEquals(value1, imap.get("test1"));
        
        entryUpdated = new CountDownLatch(1);
        imap.replace("test1", "value2");
        try {
            entryUpdated.await(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        assertEquals(1, listener1.addedCount);
        assertEquals(1, listener1.udpatedCount);
        assertEquals(0, listener1.removedCount);
        assertTrue(imap.get("test1").equals("value2"));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testIMapImplEntryListenerSupportValue() {
        StandaloneClusterContext context = new StandaloneClusterContext();
        IMap<String, String> imap = context.getCollectionsFactory().getMap("test6");
        final String value1 = "value1";
        final String value2 = "value2";
        imap.addEntryListener(new EntryListener<String, String>() {

            @Override
            public void entryAdded(EntryEvent<String, String> event) {
                assertEquals(value1, event.getValue());
            }

            @Override
            public void entryEvicted(EntryEvent<String, String> event) {
            }

            @Override
            public void entryRemoved(EntryEvent<String, String> event) {
                assertEquals(null, event.getValue());
            }

            @Override
            public void entryUpdated(EntryEvent<String, String> event) {
                assertEquals(value2, event.getValue());
            }

            @Override
            public void mapCleared(MapEvent paramMapEvent) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void mapEvicted(MapEvent paramMapEvent) {
                // TODO Auto-generated method stub
                
            }
        }, true);

        imap.put("test1", value1);
        imap.put("test1", value2);
        imap.remove("test1");

    }

    @SuppressWarnings("deprecation")
    @Test
    public void testIMapImplEntryListenerNoSupportValue() {
        StandaloneClusterContext context = new StandaloneClusterContext();
        IMap<String, String> imap = context.getCollectionsFactory().getMap("test7");
        final String value1 = "value1";
        final String value2 = "value2";
        imap.addEntryListener(new EntryListener<String, String>() {

            @Override
            public void entryAdded(EntryEvent<String, String> event) {
                assertEquals(null, event.getValue());
            }

            @Override
            public void entryEvicted(EntryEvent<String, String> event) {
            }

            @Override
            public void entryRemoved(EntryEvent<String, String> event) {
                assertEquals(null, event.getValue());
            }

            @Override
            public void entryUpdated(EntryEvent<String, String> event) {
                assertEquals(null, event.getValue());
            }

            @Override
            public void mapCleared(MapEvent paramMapEvent) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void mapEvicted(MapEvent paramMapEvent) {
                // TODO Auto-generated method stub
                
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
            entryAdded.countDown();
        }

        @Override
        public void entryEvicted(EntryEvent<K, V> event) {
            entryEvicted.countDown();
            evictedCount++;
        }

        @Override
        public void entryRemoved(EntryEvent<K, V> event) {
            removedCount++;
            entryRemoved.countDown();

        }

        @Override
        public void entryUpdated(EntryEvent<K, V> event) {
            udpatedCount++;
            entryUpdated.countDown();

        }

        @Override
        public void mapCleared(MapEvent paramMapEvent) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void mapEvicted(MapEvent paramMapEvent) {
            // TODO Auto-generated method stub
            
        }
    }
}
