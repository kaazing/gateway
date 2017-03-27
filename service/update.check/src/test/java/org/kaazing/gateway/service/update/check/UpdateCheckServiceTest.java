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

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jmock.lib.legacy.ClassImposteriser.INSTANCE;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.apache.log4j.BasicConfigurator;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;

public class UpdateCheckServiceTest {

    private UpdateCheckService service;
    private MockUpdateCheckListener listener;

    @Before
    public void setupService() {
        BasicConfigurator.configure();
        try {
            this.service = new UpdateCheckService();
        } catch (RuntimeException e) {
            Assert.fail("There is a jar in ./src/test/resources/gateway.server-5.0.0.8.jar that needs to be on the class path, fix your test runner settings");
            throw e;
        }
        this.listener = new MockUpdateCheckListener();
    }

    @Test
    public void testListenersAreNotNotifiedWhenNoNewVersions() {
        service.addListener(listener);
        assertTrue("No notification when no new versions", listener.notifiedEvents.size() == 0);
    }

    @Test
    public void testListenersAreGivenTheService() {
        service.addListener(listener);
        assertTrue("Listener has the service", listener.service == service);
    }

    @Test
    public void testListenerNotifiedOnUpdate() {
        // add listener
        service.addListener(listener);
        // trigger event
        GatewayVersion latestVersion = new GatewayVersion(5, 0, 12);
        service.setLatestGatewayVersion(latestVersion);

        assertTrue("Listener notified of newest Version", listener.notifiedEvents.size() == 1);
        NotifiedNewVersionAvailableEvent notifiedEvent = listener.notifiedEvents.get(0);
        assertTrue(notifiedEvent.latestGatewayVersion.equals(new GatewayVersion(5, 0, 12)));
        assertTrue(notifiedEvent.currentVersion.equals(new GatewayVersion(5, 0, 0)));
    }

    @Test
    public void testListenerNotifiedOnJoining() {
        // trigger event
        GatewayVersion latestVersion = new GatewayVersion(5, 0, 12);
        service.setLatestGatewayVersion(latestVersion);

        // add listener
        service.addListener(listener);

        assertTrue("Listener notified of newest Version", listener.notifiedEvents.size() == 1);
        NotifiedNewVersionAvailableEvent notifiedEvent = listener.notifiedEvents.get(0);
        assertTrue(notifiedEvent.latestGatewayVersion.equals(new GatewayVersion(5, 0, 12)));
        assertTrue(notifiedEvent.currentVersion.equals(new GatewayVersion(5, 0, 0)));
    }

    @Test
    public void testListenerNotNotifiedOnLowerVersion() {
        // add listener
        service.addListener(listener);

        // trigger event for older VERSION
        GatewayVersion latestVersion = new GatewayVersion(-1, 0, 12);
        service.setLatestGatewayVersion(latestVersion);

        assertTrue("Listener notified of newest Version", listener.notifiedEvents.size() == 0);


        // trigger event for older RC
        latestVersion = new GatewayVersion(5, 0, 0, "RC001");
        service.setLatestGatewayVersion(latestVersion);

        assertTrue("Listener notified of newest Version", listener.notifiedEvents.size() == 0);
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
                        assertTrue("https://version.kaazing.org/KaazingWebSocketGateway/1.0/latest"
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

    @Test
    public void testTaskIsStartedAndStoppedThroughoutServiceLifeCycle() throws Exception {
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
                        assertTrue("https://version.kaazing.org/KaazingWebSocketGateway/1.0/latest"
                                .equals(webserviceUrl));
                        return scheduledFuture;
                    }
                });

                // quiesce
                oneOf(scheduledFuture).cancel(with(false));
                // stop
                oneOf(scheduledFuture).cancel(with(false));
                // destroy
                oneOf(scheduledFuture).cancel(with(true));
            }
        });

        service.setSchedulerProvider(scheduleProvider);
        service.init(serviceContext); // no ServiceContext is actually needed
        service.start();
        service.quiesce();
        service.stop();
        service.destroy();

        context.assertIsSatisfied();
    }

    @Test
    public void testForceCheckForUpdate() {
        Mockery context = new Mockery() {
            {
                setImposteriser(INSTANCE);
            }
        };
        final SchedulerProvider scheduleProvider = context.mock(SchedulerProvider.class);
        final ScheduledExecutorService executorService = context.mock(ScheduledExecutorService.class);

        context.checking(new Expectations() {
            {
                oneOf(scheduleProvider).getScheduler(with(equal("update_check_service")), with(false));
                will(returnValue(executorService));

                oneOf(executorService).schedule(with(any(UpdateCheckTask.class)), with(0L), with(SECONDS));
            }
        });

        service.setSchedulerProvider(scheduleProvider);
        service.checkForUpdate(listener);

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

}
