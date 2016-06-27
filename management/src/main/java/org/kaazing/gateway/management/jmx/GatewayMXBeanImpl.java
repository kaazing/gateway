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
package org.kaazing.gateway.management.jmx;

import javax.management.ObjectName;
import org.kaazing.gateway.management.gateway.GatewayManagementBean;

/**
 * Implementation of the Gateway-level data bean for JMX.
 */
public class GatewayMXBeanImpl implements GatewayMXBean {

    /*
     * Storing the gateway's object name only so we can retrieve it during shutdown,
     * when we need to have it to unregister it.
     */
    private final ObjectName objectName;

    private GatewayManagementBean gatewayManagementBean;

    public GatewayMXBeanImpl(ObjectName objectName, GatewayManagementBean gatewayManagementBean) {
        this.objectName = objectName;
        this.gatewayManagementBean = gatewayManagementBean;
    }

    @Override
    public ObjectName getObjectName() {
        return objectName;
    }

    @Override
    public int getIndex() {
        return gatewayManagementBean.getId();
    }

    @Override
    public String getId() {
        return gatewayManagementBean.getHostAndPid();
    }

    @Override
    public String getProductTitle() {
        return gatewayManagementBean.getProductTitle();
    }

    @Override
    public String getProductBuild() {
        return gatewayManagementBean.getProductBuild();
    }

    @Override
    public String getProductEdition() {
        return gatewayManagementBean.getProductEdition();
    }

    @Override
    public long getTotalCurrentSessions() {
        return gatewayManagementBean.getTotalCurrentSessions();
    }

    @Override
    public long getTotalBytesReceived() {
        return gatewayManagementBean.getTotalBytesReceived();
    }

    @Override
    public long getTotalBytesSent() {
        return gatewayManagementBean.getTotalBytesSent();
    }

    @Override
    public long getUptime() {
        return gatewayManagementBean.getUptime();
    }

    @Override
    public long getStartTime() {
        return gatewayManagementBean.getStartTime();
    }

    @Override
    public String getInstanceKey() {
        return gatewayManagementBean.getInstanceKey();
    }

    @Override
    public String getClusterMembers() {
        return gatewayManagementBean.getClusterMembers();
    }

    @Override
    public String getClusterBalancerMap() {
        return gatewayManagementBean.getClusterBalancerMap();
    }

    @Override
    public String getManagementServiceMap() {
        return gatewayManagementBean.getManagementServiceMap();
    }

    @Override
    public String getAvailableUpdateVersion() {
        return gatewayManagementBean.getAvailableUpdateVersion();
    }

    @Override
    public void forceUpdateVersionCheck() {
        gatewayManagementBean.forceUpdateVersionCheck();
    }
}
