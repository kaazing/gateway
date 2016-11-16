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
package org.kaazing.gateway.management;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.kaazing.gateway.management.context.ManagementContext;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;

/**
 * Superclass for all the beans that generate summary data on a timed interval and send that data out to summaryDataListeners,
 * which can then send it out.
 */
public abstract class AbstractManagementBean implements ManagementBean {
    protected final List<SummaryDataListener> summaryDataListeners;

    protected final ManagementContext managementContext;
    protected final SummaryManagementInterval summaryInterval;
    protected final String[] summaryDataFieldList;

    protected String summaryDataFields;

    // The following are established lazily at runtime, per KG-12946 && KG-13668.
    private SchedulerProvider schedulerProvider;
    private ScheduledExecutorService summaryDataScheduler;

    private final AtomicBoolean dirty = new AtomicBoolean(true);  // force initial send

    private ScheduledFuture<Boolean> summaryDataFuture;

    /**
     * Constructor.
     *
     * @param managementContext management context
     * @param summaryInterval summary interval
     * @param summaryDataFieldList list of summary data fields
     */
    public AbstractManagementBean(ManagementContext managementContext,
                                  SummaryManagementInterval summaryInterval,
                                  String[] summaryDataFieldList) {
        this.managementContext = managementContext;
        this.summaryInterval = summaryInterval;
        this.summaryDataFieldList = summaryDataFieldList;

        this.summaryDataListeners = new ArrayList<>();
        // DO NOT send initial summary data here--wait for the first onChange call.
    }

    // per bugs KG-12946 and KG-13668, we need to delay using schedulerProvider and
    // summaryDataScheduler until the gateway starts (i.e., not during init()). We'll
    // just lazily retrieve/create the two of them
    protected SchedulerProvider getSchedulerProvider() {
        if (schedulerProvider == null) {
            schedulerProvider = managementContext.getSchedulerProvider();
        }

        return schedulerProvider;
    }

    protected ScheduledExecutorService getSummaryDataScheduler() {
        if (summaryDataScheduler == null) {
            getSchedulerProvider();
            summaryDataScheduler = schedulerProvider.getScheduler("summaryData", false);
        }

        return summaryDataScheduler;
    }

    @Override
    public String getSummaryDataFields() {
        if (summaryDataFields == null) {
            this.summaryDataFields = Utils.makeJSONArrayString(summaryDataFieldList);
        }
        return summaryDataFields;
    }

    @Override
    public abstract String getSummaryData();

    @Override
    public List<SummaryDataListener> getSummaryDataListeners() {
        return summaryDataListeners;
    }

    @Override
    public void addSummaryDataListener(SummaryDataListener summaryDataListener) {
        summaryDataListeners.add(summaryDataListener);
    }

    @Override
    public void removeSummaryDataListener(SummaryDataListener summaryDataListener) {
        summaryDataListeners.remove(summaryDataListener);
    }

    @Override
    public SummaryManagementInterval getSummaryInterval() {
        return summaryInterval;
    }

    @Override
    public boolean isDirty() {
        return dirty.get();
    }

    @Override
    public ManagementContext getManagementContext() {
        return managementContext;
    }

    @Override
    public void runManagementTask(Runnable runnable) {
        managementContext.runManagementTask(runnable);
    }

    protected void markChanged() {
        setDirty();

        // depending on timing, summaryDataFuture may be null when we get here.
        // XXX We may want to restrict this to only when management is not
        // being 'collect-only' or 'pass-thru'.
        if (summaryDataFuture == null || summaryDataFuture.isDone()) {
            sendSummaryData();
        }
    }

    private void setDirty() {
        dirty.compareAndSet(false, true);
    }

    private boolean clearDirty() {
        boolean val = dirty.compareAndSet(true, false);
        return val;
    }

    /**
     * Send the summary data, returning whether anything actually needed to be sent.
     *
     * @return true iff any summary data needed to be sent
     */
    public boolean sendSummaryData() {
        if (clearDirty()) {
            //System.out.println("#### sendSummaryData for " + Utils.getClassName(this));

            // we had something that changed, so send the notification of summaryData
            String summaryData = getSummaryData();

            for (SummaryDataListener listener : summaryDataListeners) {
                listener.sendSummaryData(summaryData);
            }

            scheduleSummaryData();
            return true;
        } else {
            //System.out.println("#### sendSummaryData for " + Utils.getClassName(this) + ". Nothing to do.");

        }

        return false;
    }

    private void scheduleSummaryData() {
        summaryDataFuture = getSummaryDataScheduler().schedule(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return sendSummaryData();
            }
        }, summaryInterval.getInterval(), TimeUnit.MILLISECONDS);
    }
}
