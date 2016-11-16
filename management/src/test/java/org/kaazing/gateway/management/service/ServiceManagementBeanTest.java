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
package org.kaazing.gateway.management.service;

import static org.junit.Assert.assertTrue;

import java.security.Principal;

import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.kaazing.gateway.management.SummaryManagementInterval;
import org.kaazing.gateway.management.context.ManagementContext;
import org.kaazing.gateway.management.gateway.GatewayManagementBean;
import org.kaazing.gateway.security.RealmContext;
import org.kaazing.gateway.service.Service;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.transport.test.Expectations;
import org.slf4j.Logger;

public class ServiceManagementBeanTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    @Test
    public void createShouldSucceedWithValidUserPrincipalClass() throws Exception {
        final ServiceContext serviceContext = context.mock(ServiceContext.class);
        final GatewayManagementBean gateway = context.mock(GatewayManagementBean.class);
        final ManagementContext managementContext = context.mock(ManagementContext.class);
        final SummaryManagementInterval interval = context.mock(SummaryManagementInterval.class, "interval");
        final Logger logger = context.mock(Logger.class);
        final Service service = context.mock(Service.class);
        final RealmContext realmContext = context.mock(RealmContext.class);

        context.checking(new Expectations() {{
            allowing(gateway).getManagementContext(); will(returnValue(managementContext));
            oneOf(managementContext).getServiceSummaryDataNotificationInterval();  will(returnValue(interval));
            allowing(serviceContext).getLogger(); will(returnValue(logger));
            oneOf(serviceContext).getService(); will(returnValue(service));
            oneOf(serviceContext).getServiceRealm(); will(returnValue(realmContext));
            oneOf(realmContext).getUserPrincipalClasses(); will(returnValue( new String[]{ValidPrincipal.class.getName()}));
        }});
        new  ServiceManagementBean.DefaultServiceManagementBean(gateway, serviceContext);
    }

    @Test(expected=IllegalArgumentException.class)
    public void createShouldLogErrorAndFailWhenCantLoadUserPrincipalClass() throws Exception {
        final ServiceContext serviceContext = context.mock(ServiceContext.class);
        final GatewayManagementBean gateway = context.mock(GatewayManagementBean.class);
        final ManagementContext managementContext = context.mock(ManagementContext.class);
        final SummaryManagementInterval interval = context.mock(SummaryManagementInterval.class, "interval");
        final Logger logger = context.mock(Logger.class);
        final Service service = context.mock(Service.class);
        final RealmContext realmContext = context.mock(RealmContext.class);

        context.checking(new Expectations() {{
            allowing(gateway).getManagementContext(); will(returnValue(managementContext));
            oneOf(managementContext).getServiceSummaryDataNotificationInterval();  will(returnValue(interval));
            allowing(serviceContext).getLogger(); will(returnValue(logger));
            oneOf(serviceContext).getService(); will(returnValue(service));
            oneOf(serviceContext).getServiceRealm(); will(returnValue(realmContext));
            oneOf(realmContext).getUserPrincipalClasses(); will(returnValue(new String[]{"i.dont.Exist"}));
        }});

        try {
            new  ServiceManagementBean.DefaultServiceManagementBean(gateway, serviceContext);
        }
        catch(IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().matches(".*i.dont.Exist.*ClassNotFoundException.*"));
            throw e;
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void createShouldLogErrorAndFailWhenUserPrincipalClassIsNotAPrincipal() throws Exception {
        final ServiceContext serviceContext = context.mock(ServiceContext.class);
        final GatewayManagementBean gateway = context.mock(GatewayManagementBean.class);
        final ManagementContext managementContext = context.mock(ManagementContext.class);
        final SummaryManagementInterval interval = context.mock(SummaryManagementInterval.class, "interval");
        final Logger logger = context.mock(Logger.class);
        final Service service = context.mock(Service.class);
        final RealmContext realmContext = context.mock(RealmContext.class);

        context.checking(new Expectations() {{
            allowing(gateway).getManagementContext(); will(returnValue(managementContext));
            oneOf(managementContext).getServiceSummaryDataNotificationInterval();  will(returnValue(interval));
            allowing(serviceContext).getLogger(); will(returnValue(logger));
            oneOf(serviceContext).getService(); will(returnValue(service));
            oneOf(serviceContext).getServiceRealm(); will(returnValue(realmContext));
            oneOf(realmContext).getUserPrincipalClasses(); will(returnValue( new String[]{NotAPrincipal.class.getName()}));
        }});
        try {
            new  ServiceManagementBean.DefaultServiceManagementBean(gateway, serviceContext);
        }
        catch(IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().matches(".*NotAPrincipal is not of type Principal.*"));
            throw e;
        }
    }

    public class ValidPrincipal implements Principal {

        @Override
        public String getName() {
            return "valid";
        }

    }

    public class NotAPrincipal {

    }

}
