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
package org.kaazing.gateway.service.update.check;

import org.apache.log4j.BasicConfigurator;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kaazing.gateway.server.impl.VersionUtils;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.DAYS;
import static org.jmock.lib.legacy.ClassImposteriser.INSTANCE;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class DetectProductTest {

    private UpdateCheckService service;
    private MockUpdateCheckListener listener;


    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "Kaazing Gateway", "Community.Gateway", "5.0.0", "", "https://version.kaazing.org/KaazingGateway/1.0/latest" },
                { "Kaazing Gateway", "Enterprise.Gateway", "5.0.0", "", "https://version.kaazing.com/KaazingGateway/1.0/latest" }
        });
    }

    private String productEdition;
    private String productTitle;
    private String productVersion;
    private String productDependencies;
    private String expectedUrl;

    public DetectProductTest(String productTitle, String productEdition, String productVersion, String productDependencies, String expectedUrl) {
        this.productTitle = productTitle;
        this.productEdition = productEdition;
        this.productVersion = productVersion;
        this.productDependencies = productDependencies;
        this.expectedUrl = expectedUrl;
    }

    @Before
    public void setupService() {
        setupMockProduct();
        BasicConfigurator.configure();
        this.service = new UpdateCheckService();
        this.listener = new MockUpdateCheckListener();
    }

    private void setupMockProduct() {
        VersionUtils.PRODUCT_EDITION=this.productEdition;
        VersionUtils.PRODUCT_TITLE=this.productTitle;
        VersionUtils.PRODUCT_VERSION=this.productVersion;
        VersionUtils.PRODUCT_DEPENDENCIES=this.productDependencies;
    }

    @After
    public void cleanupMockProduct() {
        VersionUtils.PRODUCT_EDITION=null;
        VersionUtils.PRODUCT_TITLE=null;
        VersionUtils.PRODUCT_VERSION=null;
        VersionUtils.PRODUCT_DEPENDENCIES=null;
    }

    /**
     * Mock UpdateCheckListener that keeps track of all the events that it receives
     *
     */
    private class MockUpdateCheckListener implements UpdateCheckListener {

        private final List<NotifiedNewVersionAvailableEvent> notifiedEvents = new ArrayList<>();
        private UpdateCheckService service;

        @Override
        public void newVersionAvailable(GatewayVersion currentVersion, GatewayVersion latestGatewayVersion) {
            getNotifiedOnPairs().add(new NotifiedNewVersionAvailableEvent(currentVersion, latestGatewayVersion));
        }

        public List<NotifiedNewVersionAvailableEvent> getNotifiedOnPairs() {
            return notifiedEvents;
        }

        @Override
        public void setUpdateCheckService(UpdateCheckService service) {
            this.service = service;
        }
    }

    /**
     * Notified UpdateCheckListener Event
     *
     */
    private class NotifiedNewVersionAvailableEvent {

        private final GatewayVersion currentVersion;
        private final GatewayVersion latestGatewayVersion;

        public NotifiedNewVersionAvailableEvent(GatewayVersion currentVersion, GatewayVersion latestGatewayVersion) {
            this.currentVersion = currentVersion;
            this.latestGatewayVersion = latestGatewayVersion;
        }
    }



    @Test
    public void testTaskIsStartedWithProperConfiguration() throws Exception {
        Mockery context = new Mockery() {
            {
                setImposteriser(INSTANCE);
            }
        };
        final SchedulerProvider scheduleProvider = context.mock(SchedulerProvider.class);
        final ScheduledExecutorService executorService = context.mock(ScheduledExecutorService.class);
        final ScheduledFuture<?> scheduledFuture = context.mock(ScheduledFuture.class);
        final ServiceContext serviceContext = context.mock(ServiceContext.class);

        context.checking(new Expectations() {
            {
                oneOf(serviceContext).getServiceSpecificObjects();
                will(returnValue(new HashMap<>()));
                oneOf(scheduleProvider).getScheduler(with(equal("update_check_service")), with(false));
                will(returnValue(executorService));

                oneOf(executorService).scheduleAtFixedRate(with(any(UpdateCheckTask.class)), with(0L), with(7L),
                        with(DAYS));
                will(new CustomAction("usedForParameters") {

                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        UpdateCheckTask task = (UpdateCheckTask) invocation.getParameter(0);
                        String webserviceUrl = task.getVersionServiceUrl();
                        assertTrue(expectedUrl
                                .equals(webserviceUrl));
                        return scheduledFuture;
                    }
                });
            }
        });

        service.setSchedulerProvider(scheduleProvider);
        service.init(serviceContext);
        service.start();

        context.assertIsSatisfied();
    }

}
