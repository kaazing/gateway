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

import org.hyperic.sigar.SigarException;
import org.json.JSONException;
import org.json.JSONObject;
import org.kaazing.gateway.management.gateway.GatewayManagementBean;

/**
 * Implementation of the management 'data' bean for a system (i.e. host computer). This just contains the data. Wrappers for
 * different management protocols define the use of those data.
 */
public class HostManagementBeanImpl extends AbstractSystemManagementBean implements HostManagementBean {

    private final GatewayManagementBean gatewayManagementBean;

    // To avoid overload of exception messages when we somehow have an issue,
    // I'll show the error messages once, then suppress them (it's really more
    // of an issue with NICs). This flag is the suppressor.

    private String osName = "Unknown";
    private double uptimeSeconds;
    private long totalFreeMemory;
    private long totalUsedMemory;
    private long totalMemory;
    private long totalFreeSwap;
    private long totalUsedSwap;
    private long totalSwap;
    private double cpuPercentage;

    public HostManagementBeanImpl(GatewayManagementBean gatewayManagementBean, int summaryDataLimit) {
        super(gatewayManagementBean.getManagementContext(),
                gatewayManagementBean.getManagementContext().getSystemSummaryDataNotificationInterval(),
                HostManagementBean.SUMMARY_DATA_FIELD_LIST,
                gatewayManagementBean.getManagementContext().getSystemSummaryDataGatherInterval(),
                "system stats",
                summaryDataLimit,
                "SNMPHostSummaryData");
        this.gatewayManagementBean = gatewayManagementBean;
    }

    @Override
    public GatewayManagementBean getGatewayManagementBean() {
        return gatewayManagementBean;
    }

    @Override
    public String getOSName() {
        return osName;
    }

    @Override
    public double getUptimeSeconds() {
        return uptimeSeconds;
    }

    @Override
    public long getTotalFreeMemory() {
        return totalFreeMemory;
    }

    @Override
    public long getTotalUsedMemory() {
        return totalUsedMemory;
    }

    @Override
    public long getTotalMemory() {
        return totalMemory;
    }

    @Override
    public long getTotalFreeSwap() {
        return totalFreeSwap;
    }

    @Override
    public long getTotalUsedSwap() {
        return totalUsedSwap;
    }

    @Override
    public long getTotalSwap() {
        return totalSwap;
    }

    @Override
    public double getCpuPercentage() {
        return cpuPercentage;
    }

    /**
     * Do the type-specific gathering of stats, called from 'gatherStats' in AbstractSummaryDataProvider.
     */
    @Override
    public void doGatherStats(JSONObject jsonObj, long readTime) throws SigarException, JSONException {
        SystemDataProvider systemDataProvider = managementContext.getSystemDataProvider();

        uptimeSeconds = systemDataProvider.getUptimeSeconds();

        totalFreeMemory = systemDataProvider.getTotalFreeMemory();
        totalUsedMemory = systemDataProvider.getTotalUsedMemory();
        totalMemory = systemDataProvider.getTotalMemory();

        totalFreeSwap = systemDataProvider.getTotalFreeSwap();
        totalUsedSwap = systemDataProvider.getTotalUsedSwap();
        totalSwap = systemDataProvider.getTotalSwap();

        // Get the 'quickie' version of the CPU percentage combined over all CPUs
        cpuPercentage = systemDataProvider.getCombinedCpuPercentage();

        Object[] vals = new Object[summaryDataFieldList.length];

        vals[SUMMARY_DATA_OS_NAME_INDEX] = osName;
        vals[SUMMARY_DATA_UPTIME_SECONDS_INDEX] = uptimeSeconds;
        vals[SUMMARY_DATA_TOTAL_FREE_MEMORY_INDEX] = totalFreeMemory;
        vals[SUMMARY_DATA_TOTAL_USED_MEMORY_INDEX] = totalUsedMemory;
        vals[SUMMARY_DATA_TOTAL_MEMORY_INDEX] = totalMemory;
        vals[SUMMARY_DATA_TOTAL_FREE_SWAP_INDEX] = totalFreeSwap;
        vals[SUMMARY_DATA_TOTAL_USED_SWAP_INDEX] = totalUsedSwap;
        vals[SUMMARY_DATA_TOTAL_SWAP_INDEX] = totalSwap;
        vals[SUMMARY_DATA_CPU_PERCENTAGE_INDEX] = cpuPercentage;

        jsonObj.put("systemData", vals);
    }
}
