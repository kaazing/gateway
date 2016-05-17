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
package org.kaazing.gateway.management.system;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.legacy.ClassImposteriser;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Test;
import org.kaazing.gateway.management.ManagementStrategyChangeListener;
import org.kaazing.gateway.management.SummaryManagementInterval;
import org.kaazing.gateway.management.context.ManagementContext;
import org.kaazing.gateway.management.gateway.GatewayManagementBean;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;

public class CpuListManagementBeanImplTest {

    private static final int SUMMARY_DATA_LIMIT = 3;
    private static final int SUMMARY_DATA_LIMIT_OVERHEAD = 2;

    @SuppressWarnings("unchecked")
    @Test
    public void testCpuSummaryDataLimit() throws JSONException {

        Mockery context = new Mockery();
        context.setImposteriser(ClassImposteriser.INSTANCE);
        final Counter counter = new Counter(0);
        final ManagementContext managementContext = context.mock(ManagementContext.class);
        final GatewayManagementBean gateway = context.mock(GatewayManagementBean.class);
        final SummaryManagementInterval interval = context.mock(SummaryManagementInterval.class);
        final SchedulerProvider schedulerProvider = context.mock(SchedulerProvider.class);
        final ScheduledExecutorService scheduledExecutorService = context.mock(ScheduledExecutorService.class);
        final ManagementSystemStrategy managementSystemStrategy = context.mock(ManagementSystemStrategy.class);

        context.checking(new Expectations() {{
            allowing(gateway).getManagementContext();
            will(returnValue(managementContext));
            oneOf(managementContext).getSystemSummaryDataNotificationInterval();
            oneOf(managementContext).getCpuListSummaryDataGatherInterval();
            will(returnValue(interval));
            allowing(interval).getInterval();
            oneOf(managementContext).addManagementStrategyChangeListener(with(any(ManagementStrategyChangeListener.class)));
            allowing(managementContext).getSystemDataProvider();
            allowing(managementContext).getManagementSystemStrategy();
            will(returnValue(managementSystemStrategy));

            allowing(managementContext).runManagementTask(with(any(Runnable.class)));
            will(new CustomAction("Run management task.") {
                @Override
                public Object invoke(Invocation invocation) throws Throwable {
                    Runnable runnable = (Runnable) invocation.getParameter(0);
                    runnable.run();
                    return null;
                }
            });

            oneOf(managementContext).getSchedulerProvider();
            will(returnValue(schedulerProvider));
            allowing(schedulerProvider).getScheduler(with(any(String.class)), with(any(Boolean.class)));
            will(returnValue(scheduledExecutorService));

            allowing(scheduledExecutorService).schedule((Callable<Boolean>) with(any(Callable.class)), with(any(Long.class)), with(any(TimeUnit.class)));
            allowing(scheduledExecutorService).schedule(with(any(Runnable.class)), with(any(Long.class)), with(any(TimeUnit.class)));
            will(new CustomAction("Call scheduled executor.") {
              @Override
              public Object invoke(Invocation invocation) throws Throwable {
                  if (counter.getCount() < SUMMARY_DATA_LIMIT + SUMMARY_DATA_LIMIT_OVERHEAD) {
                      Runnable runnable = (Runnable) invocation.getParameter(0);
                      counter.increment();
                      runnable.run();
                  }
                  return null;
              }
            });

            allowing(managementSystemStrategy).continueGatherStats(with(any(AbstractSystemManagementBean.class)));
            will(new CustomAction("Call continue gather stats.") {
                @Override
                public Object invoke(Invocation invocation) throws Throwable {
                    AbstractSystemManagementBean dataProvider = (AbstractSystemManagementBean) invocation.getParameter(0);
                    dataProvider.continueGatherStats();
                    return null;
                }
              });
        }});

        CpuListManagementBeanImpl cpuListManagementBeanImpl = new CpuListManagementBeanImpl(gateway, SUMMARY_DATA_LIMIT);
        cpuListManagementBeanImpl.gatherStats();
        String summaryData = cpuListManagementBeanImpl.getSummaryData();
        JSONArray jsonArray = new JSONArray(summaryData);

        assertTrue("The limit should not be exceeded by the json length", SUMMARY_DATA_LIMIT >= jsonArray.length());
    }

    private class Counter {

        private int count;

        public Counter(int count) {
            this.count = count;
        }

        public void increment() {
            count++;
        }

        public int getCount() {
            return count;
        }
    }
}
