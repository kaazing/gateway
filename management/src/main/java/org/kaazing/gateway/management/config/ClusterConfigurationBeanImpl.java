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
package org.kaazing.gateway.management.config;

import java.util.ArrayList;
import java.util.List;
import org.kaazing.gateway.management.gateway.GatewayManagementBean;
import org.kaazing.gateway.service.cluster.ClusterConnectOptionsContext;
import org.kaazing.gateway.service.cluster.ClusterContext;
import org.kaazing.gateway.service.cluster.MemberId;

public class ClusterConfigurationBeanImpl implements ClusterConfigurationBean {

    private final GatewayManagementBean gatewayBean;
    //    private final long startTime;
    private final ClusterContext clusterContext;

    public ClusterConfigurationBeanImpl(ClusterContext clusterContext, GatewayManagementBean gatewayBean) {
        this.clusterContext = clusterContext;
        this.gatewayBean = gatewayBean;
//        this.startTime = System.currentTimeMillis();
    }

    @Override
    public GatewayManagementBean getGatewayManagementBean() {
        return gatewayBean;
    }

    @Override
    public int getId() {
        return 1; // hardcoded for now...
    }

    @Override
    public String getName() {
        return clusterContext.getClusterName();
    }

    @Override
    public List<String> getAccepts() {
        List<MemberId> accepts = clusterContext.getAccepts();

        List<String> acceptList = new ArrayList<>();
        if ((accepts != null) && !accepts.isEmpty()) {
            for (MemberId member : accepts) {
                acceptList.add(member.getId());
            }
        }
        return acceptList;
    }

    @Override
    public List<String> getConnects() {
        List<MemberId> connects = clusterContext.getConnects();

        List<String> connectList = new ArrayList<>();
        if ((connects != null) && !connects.isEmpty()) {
            for (MemberId member : connects) {
                connectList.add(member.getId());
            }
        }
        return connectList;
    }

    @Override
    public String getConnectOptions() {
        ClusterConnectOptionsContext connectOptions = clusterContext.getConnectOptions();
        if (connectOptions != null) {
            return connectOptions.getAwsAccessKeyId();
        }
        return "";
    }
}
