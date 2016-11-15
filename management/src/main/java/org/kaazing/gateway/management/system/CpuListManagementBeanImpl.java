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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kaazing.gateway.management.gateway.GatewayManagementBean;

/**
 * Implementation of the management 'data' bean for the whole list of CPUs/cores that make up a given core. We do this list
 * primarily so we can implement gather and notification intervals separate from the system and NIC and JVM management beans.
 */
public class CpuListManagementBeanImpl extends AbstractSystemManagementBean implements CpuListManagementBean {

    private final GatewayManagementBean gatewayManagementBean;

    // To avoid overload of exception messages when we somehow have an issue,
    // I'll show the error messages once, then suppress them (it's really more
    // of an issue with NICs). This flag is the suppressor.

    private CpuManagementBean[] cpuManagementBeans;

    public CpuListManagementBeanImpl(GatewayManagementBean gatewayManagementBean, int summaryDataLimit) {
        super(gatewayManagementBean.getManagementContext(),
                gatewayManagementBean.getManagementContext().getSystemSummaryDataNotificationInterval(),
                CpuManagementBean.SUMMARY_DATA_FIELD_LIST,
                gatewayManagementBean.getManagementContext().getCpuListSummaryDataGatherInterval(),
                "CPU list stats",
                summaryDataLimit,
                "SNMPCpuListSummaryData");
        this.gatewayManagementBean = gatewayManagementBean;

        // Retrieve basic information about the CPUs in the list.
        int numCpus = managementContext.getSystemDataProvider().getNumberOfCpus();

        cpuManagementBeans = new CpuManagementBean[numCpus];
        for (int i = 0; i < numCpus; i++) {
            cpuManagementBeans[i] = new CpuManagementBeanImpl(i);
        }
    }

    @Override
    public GatewayManagementBean getGatewayManagementBean() {
        return gatewayManagementBean;
    }

    @Override
    public CpuManagementBean[] getCpuManagementBeans() {
        return cpuManagementBeans;
    }

    @Override
    public int getNumCpus() {
        return managementContext.getSystemDataProvider().getNumberOfCpus();
    }

    /**
     * Do the type-specific gathering of stats, called from 'gatherStats' in AbstractSummaryDataProvider.
     */
    @Override
    public void doGatherStats(JSONObject jsonObj, long readTime) throws JSONException {
        // Get the CPU percentage. The value is actually given as a
        // real value (e.g. 0.04) instead of a percentage (4%) so
        // multiply by 100 so the value is really a percentage (4%).
        // Note that this is independent of any scaling we do when
        // sending over via SNMP/JMX as an integer to retain decimal
        // places.
        // Note: from examination of the C code from SIGAR, the value
        // 'combined' means "user + sys + nice + wait" from each cpuPerc.
        JSONArray cpuData = new JSONArray();

        Double[][] cpuPercentages = managementContext.getSystemDataProvider().getCpuPercentages();  // N * 9
        int numCpus = cpuPercentages.length;

        for (int i = 0; i < numCpus; i++) {
            CpuManagementBean cpuBean = cpuManagementBeans[i];
            cpuBean.update(cpuPercentages[i]);
            cpuData.put(cpuBean.getSummaryDataValues());
        }
        jsonObj.put("cpuData", cpuData);

        double total = 0.0;

        for (int i = 0; i < numCpus; i++) {
            total += cpuManagementBeans[i].getCombined();
        }

        double cpuPercentage = total / numCpus;
        jsonObj.put("cpuPercentage",
                CpuManagementBeanImpl.roundTo(cpuPercentage, CpuManagementBeanImpl.ROUND_TO_PLACES));
    }
}
