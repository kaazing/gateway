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
package org.kaazing.gateway.service.cluster;

import static java.lang.String.format;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.MapEvent;

public class EntryListenerSupport<K, V> implements EntryListener<K, V> {
    private static final String NULL_NOT_ALLOWED_MESSAGE = "%s cannot be null";
    private Map<String, EntryListenerEntry<K,V>> listenerEntries;

    public EntryListenerSupport() {
        listenerEntries = new ConcurrentHashMap<>();
    }
    
    public String addEntryListener(EntryListener<K, V> listener, K key, boolean includeValue) {
        if (listener == null) {
            throw new NullPointerException(format(NULL_NOT_ALLOWED_MESSAGE, "listener"));
        }

        if (key == null) {
            throw new NullPointerException(format(NULL_NOT_ALLOWED_MESSAGE, "key"));
        }

        String registrationId = UUID.randomUUID().toString();
        listenerEntries.put(registrationId, new EntryListenerEntry<>(listener, key, includeValue));
        return registrationId;
    }

    public boolean removeEntryListener(String id) {
       if (listenerEntries.remove(id) != null) {
           return true;
       }

       return false;
    }

    public String addEntryListener(EntryListener<K, V> listener, boolean includeValue) {
        if (listener == null) {
            throw new NullPointerException(format(NULL_NOT_ALLOWED_MESSAGE, "listener"));
        }

        String registrationId = UUID.randomUUID().toString();
        listenerEntries.put(registrationId, new EntryListenerEntry<>(listener, includeValue));

        return registrationId;
    }

    public void removeAllListeners() {
        listenerEntries.clear();
    }

    @Override
    public void entryAdded(EntryEvent<K, V> event) {
        entryMethod(event, Method.ADDED);
    }

    @Override
    public void entryEvicted(EntryEvent<K, V> event) {
        entryMethod(event, Method.EVICTED);
    }

    @Override
    public void entryRemoved(EntryEvent<K, V> event) {
        entryMethod(event, Method.REMOVED);
    }

    @Override
    public void entryUpdated(EntryEvent<K, V> event) {
        entryMethod(event, Method.UPDATED);
    }

    @Override
    public void mapCleared(MapEvent event) {
        // TODO check if support is needed
    }

    @Override
    public void mapEvicted(MapEvent event) {
        // TODO check if support is needed
    }

    private void entryMethod(EntryEvent<K, V> event, Method method) {
        //TODO MEMBER
        EntryEvent<K,V> copyNoValue = new EntryEvent<>(event.getName(), null, event.getEventType().hashCode(), event.getKey(), null);
        for (EntryListenerEntry<K, V> listenerEntry : listenerEntries.values()) {
            if (listenerEntry.key == null || (event.getKey() != null && listenerEntry.key.equals(event.getKey()))) {
                switch (method) {
                case ADDED:
                    listenerEntry.listener.entryAdded(listenerEntry.includeValue ? event : copyNoValue);
                    break;
                case REMOVED:
                    listenerEntry.listener.entryRemoved(listenerEntry.includeValue ? event : copyNoValue);
                    break;
                case UPDATED:
                    listenerEntry.listener.entryUpdated(listenerEntry.includeValue ? event : copyNoValue);
                    break;
                case EVICTED:
                    listenerEntry.listener.entryEvicted(listenerEntry.includeValue ? event : copyNoValue);
                    break;
                }
            }
        }
    }

    private enum Method {
        ADDED, REMOVED, UPDATED, EVICTED,
    }

    private static class EntryListenerEntry<K, V> {
        private EntryListener<K, V> listener;
        private Object key;
        private boolean includeValue;

        public EntryListenerEntry(EntryListener<K, V> listener, boolean includeValue) {
            this(listener, null, includeValue);
        }

        public EntryListenerEntry(EntryListener<K, V> listener, K key, boolean includeValue) {
            this.listener = listener;
            this.key = key;
            this.includeValue = includeValue;
        }
    }

}
