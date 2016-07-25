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

import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.ServiceFactory;
import org.kaazing.gateway.service.ServiceProperties;
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
    
    @Test
    public void initializesCredentialGenerator() throws Exception {
        final ServiceContext serviceContext = mockery.mock(ServiceContext.class);
        final ServiceProperties properties = mockery.mock(ServiceProperties.class, "properties");
        final List<ServiceProperties> optionsList = mockery.mock(List.class, "optionsList");
        final ServiceProperties options = mockery.mock(ServiceProperties.class, "options");
        final List<ServiceProperties> urisList = mockery.mock(List.class, "urisList");
        final ServiceProperties uris = mockery.mock(ServiceProperties.class, "uris");
        
        mockery.checking(new Expectations() {
            {
                allowing(serviceContext).getProperties();
                will(returnValue(properties));
                allowing(properties).get(with("generate.credentials"));
                will(returnValue("class:org.kaazing.gateway.service.turn.rest.TestCredentialGenerator"));
                allowing(properties).getNested(with("options"));
                will(returnValue(optionsList));
                allowing(optionsList).get(with(0));
                will(returnValue(options));
                allowing(options).get(with("secret"));
                will(returnValue(new String()));
                allowing(options).get(with("symbol"));
                will(returnValue(new String()));
                allowing(options).getNested(with("uris"));
                will(returnValue(urisList));
                allowing(urisList).get(with(0));
                will(returnValue(uris));
                allowing(uris).get(with("uri"));
                will(returnValue(new String()));
            }
        });
        
        TurnRestService service = (TurnRestService)ServiceFactory.newServiceFactory().newService("turn.rest");
        service.init(serviceContext);
        mockery.assertIsSatisfied();
    }
}
