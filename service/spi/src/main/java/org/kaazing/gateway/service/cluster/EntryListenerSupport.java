/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.service.cluster;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;

public class EntryListenerSupport<K, V> implements EntryListener<K, V>  {
    private List<EntryListenerEntry<K,V>> listenerEntries;

    public EntryListenerSupport() {
    	listenerEntries = new CopyOnWriteArrayList<>();
    }
    
    public void addEntryListener(EntryListener<K, V> listener, K key, boolean includeValue) {
        listenerEntries.add(new EntryListenerEntry<>(listener, key, includeValue));
    }

    public void removeEntryListener(EntryListener<K, V> listener, Object key) {
        EntryListenerEntry<K,V> entry = getListenerEntry(listener, key);
        if (entry != null)
            listenerEntries.remove(entry);
    }

    public void addEntryListener(EntryListener<K, V> listener, boolean includeValue) {
        listenerEntries.add(new EntryListenerEntry<>(listener, includeValue));
    }

    public void removeEntryListener(EntryListener<K, V> listener) {
        listenerEntries.remove(listener);
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

    private void entryMethod(EntryEvent<K, V> event, Method method) {
    	//TODO MEMBER
        EntryEvent<K,V> copyNoValue = new EntryEvent<>(event.getName(), null, event.getEventType().hashCode(), event.getKey(), null);
        for (EntryListenerEntry<K, V> listenerEntry : listenerEntries) {
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

    private EntryListenerEntry<K, V> getListenerEntry(EntryListener<K, V> listener, Object key) {
        for (EntryListenerEntry<K, V> entry : listenerEntries) {
            if (entry.listener.equals(listener) && ((entry.key == null && key == null) || (entry.key != null && key != null && entry.key.equals(key)))) {
                return entry;
            }
        }
        return null;
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
