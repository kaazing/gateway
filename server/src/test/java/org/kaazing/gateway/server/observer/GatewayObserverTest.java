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
package org.kaazing.gateway.server.observer;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.server.GatewayObserver;
import org.kaazing.gateway.server.GatewayObserverApi;
import org.kaazing.gateway.server.context.GatewayContext;

public class GatewayObserverTest {

    private GatewayObserverApi observer;
    private GatewayContext mockGatewayContext;

    Mockery context;

    @Before
    public void createObserver() {
        context = new Mockery();
        mockGatewayContext = context.mock(GatewayContext.class);
        observer = GatewayObserver.newInstance();
    }

    @After
    public void resetObserverSpi() {
        CounterGatewayObserver.COUNT_INSTANCES = 0;
        CounterGatewayObserver.COUNT_INITING_SERVICE = 0;
        CounterGatewayObserver.configuration = null;
    }

    @Test
    public void shouldLoadCounterGatewayObserver() {
        assertEquals(1, CounterGatewayObserver.COUNT_INSTANCES);
        assertEquals(0, CounterGatewayObserver.COUNT_INITING_SERVICE);
        assertEquals(null, CounterGatewayObserver.configuration);
    }

    @Test
    public void shouldCallLoadedSpiMethodAndInjectResources() {
        observer.initingService(null);
        assertEquals(1, CounterGatewayObserver.COUNT_INITING_SERVICE);

        Properties configuration = new Properties();
        context.checking(new Expectations() {
            {
                Map<String, Object> injectables = new HashMap<>();
                injectables.put("configuration", configuration);
                oneOf(mockGatewayContext).getInjectables();
                will(returnValue(injectables));
            }
        });

        assertEquals(null, CounterGatewayObserver.configuration);
        observer.startingGateway(mockGatewayContext);
        assertEquals(configuration, CounterGatewayObserver.configuration);
    }

}
