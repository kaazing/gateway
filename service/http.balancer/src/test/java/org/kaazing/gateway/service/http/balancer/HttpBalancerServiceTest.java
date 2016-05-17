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

import org.apache.log4j.PropertyConfigurator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.service.ServiceFactory;
import org.kaazing.test.util.MethodExecutionTrace;

public class HttpBalancerServiceTest {
    @Rule
    public TestRule testExecutionTrace = new MethodExecutionTrace();

    @Before
    public void setup() {
        PropertyConfigurator.configure("src/test/resources/log4j-trace.properties");
    }

    @Test
    public void testCreateService() throws Exception {
        HttpBalancerService service = (HttpBalancerService)ServiceFactory.newServiceFactory().newService("balancer");
        Assert.assertNotNull("Failed to create HttpBalancerService", service);
    }
}
