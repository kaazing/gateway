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
package org.kaazing.gateway.service.http.balancer;

import java.io.Serializable;
import java.util.Collection;
import java.util.TreeSet;

import org.junit.Test;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MultiMap;

public class MultimapTest {
    
//    Class to help understand HazelCast inconsistent hashing. Switching from HastSet to TreeSet resolves issue

//    Hazelcast doc (see below), the last sentence is what gave me idea to try a TreeSet to resolve issue.
//    
//     When you store a key and value in a distributed Map, Hazelcast serializes the key and value, and stores 
//    the byte array version of them in local ConcurrentHashMaps. These ConcurrentHashMaps use equals and hashCode
//    methods of byte array version of your key. It does not take into account the actual equals and hashCode 
//     implementations of your objects. So it is important that you choose your keys in a proper way.
//
//    Implementing equals and hashCode is not enough, it is also important that the object is always serialized 
//    into the same byte array. All primitive types  like String, Long, Integer, etc. are good candidates for keys
//    to be used in Hazelcast. An unsorted Set is an example of a very bad candidate because Java Serialization may
//    serialize the same unsorted set in two different byte arrays.

    @Test
    public void TestMultiMap() {
        Config config = new Config();
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        HazelcastInstance instance = Hazelcast.newHazelcastInstance(config);
        MultiMap<Integer, Order> multiMap = instance.getMultiMap("blah");
        multiMap.put(1, new Order(1, "foo"));
        multiMap.put(1, new Order(2, "bar"));
        multiMap.put(1, new Order(3, "zoo"));

        multiMap.put(2, new Order(4, "foo"));
        multiMap.put(2, new Order(5, "bar"));
        multiMap.put(2, new Order(6, "zoo"));

        Collection<Order> items = multiMap.get(1);
        for (Order o : items) {
            multiMap.remove(1, o);
         }
        assert(3 == multiMap.size());
    }

    public static class Order implements Serializable {
        private static final long serialVersionUID = 1L;
        private int id;
        private TreeSet<String> set;

        public Order(int id, String s) {
            this.id = id;
            set = new TreeSet<>();
            set.add(s);
        }

    }
}


