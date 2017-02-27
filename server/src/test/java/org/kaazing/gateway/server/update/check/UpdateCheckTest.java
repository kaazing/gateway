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
package org.kaazing.gateway.server.update.check;

import static java.util.concurrent.TimeUnit.DAYS;
import static org.jmock.lib.legacy.ClassImposteriser.INSTANCE;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
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
import org.kaazing.gateway.util.scheduler.SchedulerProvider;

public class UpdateCheckTest {

    Properties configuration = new Properties();
    private UpdateCheck updateCheck;
    private MockUpdateCheckListener listener;

    @Before
    public void setupService() {
        BasicConfigurator.configure();
        try {
            this.updateCheck = new UpdateCheck(configuration);
        } catch (RuntimeException e) {
            Assert.fail("There is a jar in ./src/test/resources/gateway.server-5.0.0.8.jar that needs to be on the class path, fix your test runner settings");
            throw e;
        }
        this.listener = new MockUpdateCheckListener();
    }

    @Test
    public void testListenersAreNotNotifiedWhenNoNewVersions() {
        updateCheck.addListener(listener);
        assertTrue("No notification when no new versions", listener.notifiedEvents.size() == 0);
    }

    @Test
    public void testListenersAreGivenTheService() {
        updateCheck.addListener(listener);
        assertTrue("Listener has the service", listener.updateCheck == updateCheck);
    }

    @Test
    public void testListenerNotifiedOnUpdate() {
        // add listener
        updateCheck.addListener(listener);
        // trigger event
        GatewayVersion latestVersion = new GatewayVersion(5, 0, 12);
        updateCheck.setLatestGatewayVersion(latestVersion);

        assertTrue("Listener notified of newest Version", listener.notifiedEvents.size() == 1);
        NotifiedNewVersionAvailableEvent notifiedEvent = listener.notifiedEvents.get(0);
        assertTrue(notifiedEvent.latestGatewayVersion.equals(new GatewayVersion(5, 0, 12)));
        assertTrue(notifiedEvent.currentVersion.equals(new GatewayVersion(5, 0, 0)));
    }

    @Test
    public void testListenerNotifiedOnJoining() {
        // trigger event
        GatewayVersion latestVersion = new GatewayVersion(5, 0, 12);
        updateCheck.setLatestGatewayVersion(latestVersion);

        // add listener
        updateCheck.addListener(listener);

        assertTrue("Listener notified of newest Version", listener.notifiedEvents.size() == 1);
        NotifiedNewVersionAvailableEvent notifiedEvent = listener.notifiedEvents.get(0);
        assertTrue(notifiedEvent.latestGatewayVersion.equals(new GatewayVersion(5, 0, 12)));
        assertTrue(notifiedEvent.currentVersion.equals(new GatewayVersion(5, 0, 0)));
    }

    @Test
    public void testListenerNotNotifiedOnLowerVersion() {
        // add listener
        updateCheck.addListener(listener);

        // trigger event for older VERSION
        GatewayVersion latestVersion = new GatewayVersion(-1, 0, 12);
        updateCheck.setLatestGatewayVersion(latestVersion);

        assertTrue("Listener notified of newest Version", listener.notifiedEvents.size() == 0);


        // trigger event for older RC
        latestVersion = new GatewayVersion(5, 0, 0, "RC001");
        updateCheck.setLatestGatewayVersion(latestVersion);

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

        context.checking(new Expectations() {
            {
                oneOf(scheduleProvider).getScheduler(with(equal("update_check")), with(false));
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

        updateCheck.setSchedulerProvider(scheduleProvider);
        updateCheck.start();

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

        context.checking(new Expectations() {
            {

                oneOf(scheduleProvider).getScheduler(with(equal("update_check")), with(false));
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

        updateCheck.setSchedulerProvider(scheduleProvider);
        updateCheck.start();


        context.assertIsSatisfied();
    }


    /**
     * Mock UpdateCheckListener that keeps track of all the events that it receives
     *
     */
    private class MockUpdateCheckListener implements UpdateCheckListener {

        private final List<NotifiedNewVersionAvailableEvent> notifiedEvents = new ArrayList<>();
        private UpdateCheck updateCheck;

        @Override
        public void newVersionAvailable(GatewayVersion currentVersion, GatewayVersion latestGatewayVersion) {
            getNotifiedOnPairs().add(new NotifiedNewVersionAvailableEvent(currentVersion, latestGatewayVersion));
        }

        public List<NotifiedNewVersionAvailableEvent> getNotifiedOnPairs() {
            return notifiedEvents;
        }

        @Override
        public void setUpdateCheck(UpdateCheck updateCheck) {
            this.updateCheck = updateCheck;
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
