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

import com.hazelcast.core.HazelcastInstance;
import java.io.Serializable;
import org.kaazing.gateway.service.messaging.buffer.MessageBufferFactory;

/**
 * TODO Add class documentation
 */
public class ClusterMemoryMessageBufferFactory implements MessageBufferFactory, Serializable {

    private static final long serialVersionUID = 1L;
    private HazelcastInstance cluster;

    public ClusterMemoryMessageBufferFactory(HazelcastInstance cluster) {
        this.cluster = cluster;
    }

    @Override
    public ClusterMemoryMessageBuffer createMessageBuffer(int capacity) {
        return new ClusterMemoryMessageBuffer(cluster, capacity);
    }

}
