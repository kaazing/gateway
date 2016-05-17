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
import org.kaazing.gateway.management.system.HostManagementBean;

/**
 * The ManagementProcessor-level wrapper object for a SystemManagementBean.
 */
public class HostMXBeanImpl implements HostMXBean {

    /*
     * The management bean this MBean is wrapping.
     */
    private HostManagementBean hostManagementBean;

    /*
     * Storing the session's name only so we can retrieve it during shutdown,
     * when we need to have it to unregister it.
     */
    private final ObjectName objectName;

    public HostMXBeanImpl(ObjectName objectName, HostManagementBean systemManagementBean) {
        this.objectName = objectName;
        this.hostManagementBean = systemManagementBean;
    }

    @Override
    public ObjectName getObjectName() {
        return objectName;
    }

    @Override
    public String getOSName() {
        return hostManagementBean.getOSName();
    }

    @Override
    public double getUptimeSeconds() {
        return hostManagementBean.getUptimeSeconds();
    }

    @Override
    public long getTotalFreeMemory() {
        return hostManagementBean.getTotalFreeMemory();
    }

    @Override
    public long getTotalUsedMemory() {
        return hostManagementBean.getTotalUsedMemory();
    }

    @Override
    public long getTotalMemory() {
        return hostManagementBean.getTotalMemory();
    }

    @Override
    public long getTotalFreeSwap() {
        return hostManagementBean.getTotalFreeSwap();
    }

    @Override
    public long getTotalUsedSwap() {
        return hostManagementBean.getTotalUsedSwap();
    }

    @Override
    public long getTotalSwap() {
        return hostManagementBean.getTotalSwap();
    }

    @Override
    public double getCpuPercentage() {
        return hostManagementBean.getCpuPercentage();
    }

    @Override
    public String getSummaryDataFields() {
        return hostManagementBean.getSummaryDataFields();
    }

    @Override
    public String getSummaryData() {
        return hostManagementBean.getSummaryData();
    }

    @Override
    public int getSummaryDataGatherInterval() {
        return hostManagementBean.getSummaryDataGatherInterval();
    }

    @Override
    public void setSummaryDataGatherInterval(int interval) {
        hostManagementBean.setSummaryDataGatherInterval(interval);
    }
}
