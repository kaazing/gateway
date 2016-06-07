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
package org.kaazing.gateway.transport.http.security.auth.challenge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;
public class HttpChallengeFactoriesTest {
    @Test
    public void testGetDefault() throws Exception {
        HttpChallengeFactory factory = HttpChallengeFactories.create();
        assertNotNull(factory);
        assertTrue(factory instanceof DispatchHttpChallengeFactory);
        DispatchHttpChallengeFactory dispatchHttpChallengeFactory = (DispatchHttpChallengeFactory) factory;
        final Map<String,HttpChallengeFactory> factoryMap = DispatchHttpChallengeFactory.challengeFactoriesByAuthScheme;
        assertEquals(3, factoryMap.size());
        assertTrue(factoryMap.containsKey("Basic"));
        assertTrue(factoryMap.containsKey("Negotiate"));
    }
}

