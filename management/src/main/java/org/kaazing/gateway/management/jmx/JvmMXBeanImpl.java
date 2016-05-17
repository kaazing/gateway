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
import org.kaazing.gateway.management.system.JvmManagementBean;

/**
 * The ManagementProcessor-level wrapper object for a SystemManagementBean.
 */
public class JvmMXBeanImpl implements JvmMXBean {

    /*
     * The management bean this MBean is wrapping.
     */
    private JvmManagementBean jvmManagementBean;

    /*
     * Storing the session's name only so we can retrieve it during shutdown,
     * when we need to have it to unregister it.
     */
    private final ObjectName objectName;

    public JvmMXBeanImpl(ObjectName objectName, JvmManagementBean jvmManagementBean) {
        this.objectName = objectName;
        this.jvmManagementBean = jvmManagementBean;
    }

    @Override
    public ObjectName getObjectName() {
        return objectName;
    }

    @Override
    public String getSummaryDataFields() {
        return jvmManagementBean.getSummaryDataFields();
    }

    @Override
    public String getSummaryData() {
        return jvmManagementBean.getSummaryData();
    }

    @Override
    public int getSummaryDataGatherInterval() {
        return jvmManagementBean.getSummaryDataGatherInterval();
    }

    @Override
    public void setSummaryDataGatherInterval(int interval) {
        jvmManagementBean.setSummaryDataGatherInterval(interval);
    }
}
