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
import org.kaazing.gateway.management.system.NicManagementBean;

/**
 * The ManagementProcessor-level wrapper object for a NicManagementBean.
 */
public class NicMXBeanImpl implements NicMXBean {

    /*
     * The management bean this MBean is wrapping.
     */
    private NicManagementBean nicManagementBean;

    /*
     * Storing the object name only so we can retrieve it during shutdown,
     * when we need to have it to unregister it.
     */
    private final ObjectName objectName;

    public NicMXBeanImpl(ObjectName objectName, NicManagementBean nicManagementBean) {
        this.objectName = objectName;
        this.nicManagementBean = nicManagementBean;
    }

    @Override
    public ObjectName getObjectName() {
        return objectName;
    }

    @Override
    public long getId() {
        return nicManagementBean.getId();
    }

    @Override
    public String getName() {
        return nicManagementBean.getName();
    }

    @Override
    public long getRxBytes() {
        return nicManagementBean.getRxBytes();
    }

    @Override
    public double getRxBytesPerSecond() {
        return nicManagementBean.getRxBytesPerSecond();
    }

    @Override
    public long getRxDropped() {
        return nicManagementBean.getRxDropped();
    }

    @Override
    public long getRxErrors() {
        return nicManagementBean.getRxErrors();
    }

    @Override
    public long getTxBytes() {
        return nicManagementBean.getTxBytes();
    }

    @Override
    public double getTxBytesPerSecond() {
        return nicManagementBean.getTxBytesPerSecond();
    }

    @Override
    public long getTxDropped() {
        return nicManagementBean.getTxDropped();
    }

    @Override
    public long getTxErrors() {
        return nicManagementBean.getTxErrors();
    }

    @Override
    public String getSummaryData() {
        return nicManagementBean.getSummaryData();
    }
}
