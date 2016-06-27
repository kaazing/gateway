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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kaazing.gateway.management.gateway.GatewayManagementBean;

/**
 * Implementation of the management 'data' bean for the list of NICs for the system running a given gateway. This just contains
 * the data. Wrappers for different management protocols define the use of those data.
 */
public class NicListManagementBeanImpl extends AbstractSystemManagementBean implements NicListManagementBean {

    private final GatewayManagementBean gatewayManagementBean;

    private String[] netInterfaceNames;

    private NicManagementBean[] nicManagementBeans;

    public NicListManagementBeanImpl(GatewayManagementBean gatewayManagementBean, int summaryDataLimit) {
        super(gatewayManagementBean.getManagementContext(),
                gatewayManagementBean.getManagementContext().getSystemSummaryDataNotificationInterval(),
                NicManagementBean.SUMMARY_DATA_FIELD_LIST,
                gatewayManagementBean.getManagementContext().getNicListSummaryDataGatherInterval(),
                "NIC list stats",
                summaryDataLimit,
                "SNMPNicListSummaryData");
        this.gatewayManagementBean = gatewayManagementBean;

        // Retrieve basic information about the NICs in the list.
        netInterfaceNames = managementContext.getSystemDataProvider().getNetInterfaceNames();

        nicManagementBeans = new NicManagementBean[netInterfaceNames.length];
        for (int i = 0; i < netInterfaceNames.length; i++) {
            nicManagementBeans[i] = new NicManagementBeanImpl(i, netInterfaceNames[i]);
        }
    }

    @Override
    public GatewayManagementBean getGatewayManagementBean() {
        return gatewayManagementBean;
    }

    @Override
    public NicManagementBean[] getNicManagementBeans() {
        return nicManagementBeans;
    }

    @Override
    public String getNetInterfaceNames() {
        JSONArray jsonArray = null;

        try {
            jsonArray = new JSONArray(netInterfaceNames);
        } catch (JSONException e) {
            // this will not do anything
        }

        return jsonArray.toString();
    }

    /**
     * Do the type-specific gathering of stats, called from 'gatherStats' in AbstractSummaryDataProvider.
     */
    @Override
    public void doGatherStats(JSONObject jsonObj, long readTime) throws SigarException, JSONException {
        // Get the NIC information.
        JSONArray nicData = new JSONArray();

        for (int i = 0; i < netInterfaceNames.length; i++) {
            String nicName;

            nicName = netInterfaceNames[i];

            Long[] netInterfaceStats = managementContext.getSystemDataProvider().getNetInterfaceStats(nicName);
            NicManagementBean nicBean = nicManagementBeans[i];
            nicBean.update(netInterfaceStats, readTime);
            nicData.put(nicBean.getSummaryDataValues());
        }

        jsonObj.put("nicData", nicData);
    }
}
