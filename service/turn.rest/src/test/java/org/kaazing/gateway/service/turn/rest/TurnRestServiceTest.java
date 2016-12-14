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
package org.kaazing.gateway.service.turn.rest;

import org.jmock.Mockery;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.service.ServiceFactory;
import org.kaazing.gateway.service.turn.rest.internal.TurnRestService;
import org.kaazing.test.util.MethodExecutionTrace;

public class TurnRestServiceTest {
    Mockery mockery = new Mockery();
    
    @Rule
    public TestRule testExecutionTrace = new MethodExecutionTrace();

    @Test
    public void createService() throws Exception {
        TurnRestService service = (TurnRestService)ServiceFactory.newServiceFactory().newService("turn.rest");
        Assert.assertNotNull("Failed to create TURN REST Service", service);
    }
    
}
