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

package org.kaazing.gateway.server.context.resolve;

import com.hazelcast.core.IdGenerator;
import org.junit.Assert;
import org.junit.Test;
import org.kaazing.gateway.service.cluster.ClusterContext;

public class StandaloneClusterContextTest {

    // In support of http://jira.kaazing.wan/browse/KG-5001, the
    // StandaloneClusterContext class needs to return a Hazelcast
    // IdGenerator, rather than throwing an UnsupportedOperationException.

    @Test
    public void shouldGetIdGenerator()
            throws Exception {

        ClusterContext clusterContext = new StandaloneClusterContext();

        String name = "Standalone ID Generator";
        IdGenerator idGenerator = clusterContext.getIdGenerator(name);
        Assert.assertTrue("Expected ID generator, got null", idGenerator != null);

        Assert.assertTrue(String.format("Expected ID generator name '%s', got '%s'", name, idGenerator.getName()), idGenerator
                .getName().equals(name));

        long id = idGenerator.newId();
        long expected = Long.MIN_VALUE + 1;
        Assert.assertTrue(String.format("Expected next ID of %d, got %d", expected, id), id == expected);
    }
}
