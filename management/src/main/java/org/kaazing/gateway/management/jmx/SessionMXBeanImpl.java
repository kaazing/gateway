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
import org.kaazing.gateway.management.session.SessionManagementBean;

/**
 * The ManagementProcessor-level wrapper object for a SessionDataBean.
 */
public class SessionMXBeanImpl implements SessionMXBean {
    /*
     * The management bean this MBean is wrapping.
     */
    private SessionManagementBean sessionManagementBean;

    /*
     * Storing the session's name only so we can retrieve it during shutdown,
     * when we need to have it to unregister it.
     */
    private final ObjectName objectName;

    public SessionMXBeanImpl(ObjectName objectName,
                             SessionManagementBean sessionManagementBean) {
        this.objectName = objectName;
        this.sessionManagementBean = sessionManagementBean;
    }

    @Override
    public void close() {
        sessionManagementBean.close();
    }

    @Override
    public void closeImmediately() {
        sessionManagementBean.closeImmediately();
    }

    @Override
    public ObjectName getObjectName() {
        return objectName;
    }

    @Override
    public long getId() {
        return sessionManagementBean.getId();
    }

    @Override
    public long getReadBytes() {
        return sessionManagementBean.getReadBytes();
    }

    @Override
    public double getReadBytesThroughput() {
        return sessionManagementBean.getReadBytesThroughput();
    }

    @Override
    public long getWrittenBytes() {
        return sessionManagementBean.getWrittenBytes();
    }

    @Override
    public double getWrittenBytesThroughput() {
        return sessionManagementBean.getWrittenBytesThroughput();
    }

    @Override
    public String getPrincipals() {
        return sessionManagementBean.getUserPrincipals();
    }

    @Override
    public long getCreateTime() {
        return sessionManagementBean.getCreateTime();
    }

    @Override
    public String getRemoteAddress() {
        return sessionManagementBean.getRemoteAddress();

    }

    @Override
    public String getSessionTypeName() {
        return sessionManagementBean.getSessionTypeName();
    }

    @Override
    public String getSessionDirection() {
        return sessionManagementBean.getSessionDirection();
    }

    @Override
    public long getLastRoundTripLatency() {
        return sessionManagementBean.getLastRoundTripLatency();
    }

    @Override
    public long getLastRoundTripLatencyTimestamp() {
        return sessionManagementBean.getLastRoundTripLatencyTimestamp();
    }
}
