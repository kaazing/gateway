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
import org.kaazing.gateway.management.system.CpuManagementBean;

/**
 * The ManagementProcessor-level wrapper object for a SessionDataBean.
 */
public class CpuMXBeanImpl implements CpuMXBean {

    /*
     * The management bean this MBean is wrapping.
     */
    private CpuManagementBean cpuManagementBean;

    /*
     * Storing the cpu's name only so we can retrieve it during shutdown,
     * when we need to have it to unregister it.
     */
    private final ObjectName objectName;

    public CpuMXBeanImpl(ObjectName objectName, CpuManagementBean cpuManagementBean) {
        this.objectName = objectName;
        this.cpuManagementBean = cpuManagementBean;
    }

    @Override
    public ObjectName getObjectName() {
        return objectName;
    }

    @Override
    public long getId() {
        return cpuManagementBean.getId();
    }

    @Override
    public double getCombined() {
        return cpuManagementBean.getCombined();
    }

    @Override
    public double getIdle() {
        return cpuManagementBean.getIdle();
    }

    @Override
    public double getIrq() {
        return cpuManagementBean.getIrq();
    }

    @Override
    public double getNice() {
        return cpuManagementBean.getNice();
    }

    @Override
    public double getSoftIrq() {
        return cpuManagementBean.getSoftIrq();
    }

    @Override
    public double getStolen() {
        return cpuManagementBean.getStolen();
    }

    @Override
    public double getSys() {
        return cpuManagementBean.getSys();
    }

    @Override
    public double getUser() {
        return cpuManagementBean.getUser();
    }

    @Override
    public double getWait() {
        return cpuManagementBean.getWait();
    }

    @Override
    public String getSummaryData() {
        return cpuManagementBean.getSummaryData();
    }
}
