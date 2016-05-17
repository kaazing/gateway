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

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import org.hyperic.sigar.SigarException;
import org.json.JSONException;
import org.json.JSONObject;
import org.kaazing.gateway.management.gateway.GatewayManagementBean;

/**
 * Implementation of the management 'data' bean for a gateway's JVM summary data (if the caller wants individual JVM items, they
 * can access them through the JVM MIB if using SNMP or JConsole, though we might want to change this). This just contains the
 * data. Wrappers for different management protocols define the use of those data.
 */
public class JvmManagementBeanImpl extends AbstractSystemManagementBean implements JvmManagementBean {

    private final GatewayManagementBean gatewayManagementBean;

    private long classesLoaded;
    private long totalClassesLoaded;
    private long totalClassesUnloaded;
    private long memHeapInitSize;
    private long memHeapUsed;
    private long memHeapCommitted;
    private long memHeapMaxSize;
    private long memNonHeapInitSize;
    private long memNonHeapUsed;
    private long memNonHeapCommitted;
    private long memNonHeapMaxSize;
    private long threadingLiveThreads;
    private long threadingPeakThreads;
    private long threadingTotalThreads;

    public JvmManagementBeanImpl(GatewayManagementBean gatewayManagementBean, int summaryDataLimit) {
        super(gatewayManagementBean.getManagementContext(),
                gatewayManagementBean.getManagementContext().getSystemSummaryDataNotificationInterval(),
                JvmManagementBean.SUMMARY_DATA_FIELD_LIST,
                gatewayManagementBean.getManagementContext().getJvmSummaryDataGatherInterval(),
                "JVM stats",
                summaryDataLimit,
                "SNMPJvmSummaryData");
        this.gatewayManagementBean = gatewayManagementBean;
    }

    @Override
    public void init() {
        // no-op
    }

    @Override
    public GatewayManagementBean getGatewayManagementBean() {
        return gatewayManagementBean;
    }

    /**
     * Do the type-specific gathering of stats, called from 'gatherStats' in AbstractSummaryDataProvider.
     */
    @Override
    public void doGatherStats(JSONObject jsonObj, long readTime) throws SigarException, JSONException {
        ClassLoadingMXBean classLoadingBean = ManagementFactory.getClassLoadingMXBean();
        classesLoaded = classLoadingBean.getLoadedClassCount();
        totalClassesLoaded = classLoadingBean.getTotalLoadedClassCount();
        totalClassesUnloaded = classLoadingBean.getUnloadedClassCount();

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage memoryUsage = memoryBean.getHeapMemoryUsage();
        memHeapInitSize = memoryUsage.getInit();
        memHeapUsed = memoryUsage.getUsed();
        memHeapCommitted = memoryUsage.getCommitted();
        memHeapMaxSize = memoryUsage.getMax();

        memoryUsage = memoryBean.getNonHeapMemoryUsage();
        memNonHeapInitSize = memoryUsage.getInit();
        memNonHeapUsed = memoryUsage.getUsed();
        memNonHeapCommitted = memoryUsage.getCommitted();
        memNonHeapMaxSize = memoryUsage.getMax();

        ThreadMXBean threadingBean = ManagementFactory.getThreadMXBean();
        threadingLiveThreads = threadingBean.getThreadCount();
        threadingPeakThreads = threadingBean.getPeakThreadCount();
        threadingTotalThreads = threadingBean.getTotalStartedThreadCount();


        Number[] vals = new Number[summaryDataFieldList.length];

        vals[SUMMARY_DATA_CLASSES_LOADED_INDEX] = classesLoaded;
        vals[SUMMARY_DATA_TOTAL_CLASSES_LOADED_INDEX] = totalClassesLoaded;
        vals[SUMMARY_DATA_TOTAL_CLASSES_UNLOADED_INDEX] = totalClassesUnloaded;
        vals[SUMMARY_DATA_MEM_HEAP_INIT_SIZE_INDEX] = memHeapInitSize;
        vals[SUMMARY_DATA_MEM_HEAP_USED_INDEX] = memHeapUsed;
        vals[SUMMARY_DATA_MEM_HEAP_COMMITTED_INDEX] = memHeapCommitted;
        vals[SUMMARY_DATA_MEM_HEAP_MAX_SIZE_INDEX] = memHeapMaxSize;
        vals[SUMMARY_DATA_MEM_NONHEAP_INIT_SIZE_INDEX] = memNonHeapInitSize;
        vals[SUMMARY_DATA_MEM_NONHEAP_USED_INDEX] = memNonHeapUsed;
        vals[SUMMARY_DATA_MEM_NONHEAP_COMMITTED_INDEX] = memNonHeapCommitted;
        vals[SUMMARY_DATA_MEM_NONHEAP_MAX_SIZE_INDEX] = memNonHeapMaxSize;
        vals[SUMMARY_DATA_THREADING_LIVE_THREADS_INDEX] = threadingLiveThreads;
        vals[SUMMARY_DATA_THREADING_PEAK_THREADS_INDEX] = threadingPeakThreads;
        vals[SUMMARY_DATA_THREADING_TOTAL_THREADS_INDEX] = threadingTotalThreads;

        jsonObj.put("jvmData", vals);
    }
}
