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
package org.kaazing.gateway.server.messaging.collections;

import org.kaazing.gateway.service.messaging.collections.CollectionsFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.map.listener.MapListener;


public class ClusterCollectionsFactory implements CollectionsFactory {


    private HazelcastInstance cluster;

    public ClusterCollectionsFactory(HazelcastInstance cluster) {
        this.cluster = cluster;
    }

    @Override
    public <K, V> IMap<K, V> getMap(String name) {
        return cluster.getMap(name);
    }

    @Override
    public <K, V> void addEntryListener(MapListener listener, String name) {
        IMap<K, V> map = cluster.getMap(name);
        if (map != null) {
            map.addEntryListener(listener, true);
        }
    }

}
