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

import java.util.HashMap;
import java.util.Map;
import javax.management.ObjectName;
import org.kaazing.gateway.management.service.ServiceManagementBean;
import org.kaazing.gateway.transport.TypedAttributeKey;
import org.kaazing.gateway.util.Utils;

public class ServiceMXBeanImpl implements ServiceMXBean {
    /*
     * The management bean this MBean is wrapping.
     */
    private ServiceManagementBean serviceManagementBean;

    /*
     * The JMX-specific management service handler that created this bean,
     * just so we can fix loggedInSessions when requested.
     */
    private JmxManagementServiceHandler managementServiceHandler;

    /*
     * Storing the session's name only so we can retrieve it during shutdown,
     * when we need to have it to unregister it.
     */
    private final ObjectName objectName;

    /*
     * Various bundles (e.g. Stomp JMS and perhaps AMQP or another one) that need
     * to do authentication handling can potentially get in a situation where they
     * first having called 'sessionCreated', or b) Somehow call 'sessionClosed'
     * more than once.  We want to prevent both scenarios, so to do so we'll have
     * a flag that is set during sessionCreated, then cleared after the first time
     * through sessionClosed.
     */
    public static final TypedAttributeKey<Boolean> SESSION_CREATED_FLAG_KEY =
            new TypedAttributeKey<>(ServiceMXBeanImpl.class, "sessionCreatedFlag");

    /**
     * Constructor.
     *
     * @param managementServiceHandler The ONLY reason this is here is because (a) the only place that calls this method is in
     *                                 the JMXManagementProcessor, and (b) to return the list of loggedInSessions, we have to
     *                                 convert from a map with session IDs to a map with session mbean names, and that's where
     *                                 they are stored.
     * @param objectName
     * @param serviceManagementBean
     */
    public ServiceMXBeanImpl(JmxManagementServiceHandler managementServiceHandler, ObjectName objectName,
                             ServiceManagementBean serviceManagementBean) {
        super();
        this.managementServiceHandler = managementServiceHandler;
        this.objectName = objectName;
        this.serviceManagementBean = serviceManagementBean;
    }

    public ObjectName getObjectName() {
        return objectName;
    }

    public int getIndex() {
        return serviceManagementBean.getId();
    }

    @Override
    public long getNumberOfCumulativeSessions() {
        return serviceManagementBean.getCumulativeSessionCount();
    }

    @Override
    public long getNumberOfCumulativeNativeSessions() {
        return serviceManagementBean.getCumulativeNativeSessionCount();
    }

    @Override
    public long getNumberOfCumulativeEmulatedSessions() {
        return serviceManagementBean.getCumulativeEmulatedSessionCount();
    }

    @Override
    public long getNumberOfCurrentSessions() {
        return serviceManagementBean.getCurrentSessionCount();
    }

    @Override
    public long getNumberOfCurrentNativeSessions() {
        return serviceManagementBean.getCurrentNativeSessionCount();
    }

    @Override
    public long getNumberOfCurrentEmulatedSessions() {
        return serviceManagementBean.getCurrentEmulatedSessionCount();
    }

    @Override
    public long getNumberOfExceptions() {
        return serviceManagementBean.getExceptionCount();
    }

    @Override
    public String getLatestException() {
        return serviceManagementBean.getLatestException();
    }

    @Override
    public void clearCumulativeSessionsCount() {
        serviceManagementBean.clearCumulativeSessionsCount();
    }

    @Override
    public long getTotalBytesReceivedCount() {
        return serviceManagementBean.getTotalBytesReceivedCount();
    }

    @Override
    public long getTotalBytesSentCount() {
        return serviceManagementBean.getTotalBytesSentCount();
    }

    /**
     * Return a map of session mbean names to the user principals for those sessions. The serviceManagementBean stores them as
     * session ID to user principals, and we have to convert the session ID to mbean name here.  Gross, but it's the only way to
     * not have JMX-specific stuff in the ServiceManagementBean.
     */
    @Override
    public Map<String, Map<String, String>> getLoggedInSessions() {
        // First, get the map of session ID to user principals for that session.
        Map<Long, Map<String, String>> sessionPrincipalMap = serviceManagementBean.getLoggedInSessions();

        Map<String, Map<String, String>> result = new HashMap<>();

        for (Map.Entry<Long, Map<String, String>> entry : sessionPrincipalMap.entrySet()) {
            long sessionId = entry.getKey();
            Map<String, String> userPrincipals = entry.getValue();
            ObjectName sessionMBeanName = managementServiceHandler.getSessionMXBean(sessionId).getObjectName();
            result.put(sessionMBeanName.toString(), userPrincipals);
        }

        return result;
    }

    @Override
    public Map<String, String> getUserPrincipals(Long sessionId) {
        // First, get the map of session ID to user principals for that session.
        Map<Long, Map<String, String>> sessionPrincipalMap = serviceManagementBean.getLoggedInSessions();

        if (sessionPrincipalMap != null) {
            for (Map.Entry<Long, Map<String, String>> entry : sessionPrincipalMap.entrySet()) {
                if (entry.getKey() == sessionId.longValue()) {
                    return entry.getValue();
                }
            }
        }

        return null;
    }

    @Override
    public long getLastSuccessfulConnectTime() {
        return serviceManagementBean.getLastSuccessfulConnectTime();
    }

    @Override
    public long getLastFailedConnectTime() {
        return serviceManagementBean.getLastFailedConnectTime();
    }

    @Override
    public boolean getLastHeartbeatPingResult() {
        return serviceManagementBean.getLastHeartbeatPingResult();
    }

    @Override
    public long getLastHeartbeatPingTimestamp() {
        return serviceManagementBean.getLastHeartbeatPingTimestamp();
    }

    @Override
    public int getHeartbeatPingCount() {
        return serviceManagementBean.getHeartbeatPingCount();
    }

    @Override
    public int getHeartbeatPingSuccessesCount() {
        return serviceManagementBean.getHeartbeatPingSuccessesCount();
    }

    @Override
    public int getHeartbeatPingFailuresCount() {
        return serviceManagementBean.getHeartbeatPingFailuresCount();
    }

    @Override
    public boolean isServiceConnected() {
        return serviceManagementBean.isServiceConnected();
    }

    @Override
    public boolean isHeartbeatRunning() {
        return serviceManagementBean.isHeartbeatRunning();
    }

    // Some lifecycle methods for the service, called from
    // the management platform (e.g. JConsole.)
    @Override
    public void start() throws Exception {
        serviceManagementBean.start();
    }

    @Override
    public void stop() throws Exception {
        serviceManagementBean.stop();
    }

    @Override
    public void restart() throws Exception {
        serviceManagementBean.restart();
    }

    @Override
    public void closeSessions(String principalName, String principalClassName) throws Exception {
        if ((principalName == null) ||
            (principalName.trim().length() == 0) ||
            (principalClassName == null) ||
            (principalClassName.trim().length() == 0)) {
            return;
        }

        principalName = principalName.trim();
        principalClassName = principalClassName.trim();
        Class<?> principalClass = Utils.loadClass(principalClassName);

        Map<Long, Map<String, String>> sessionPrincipalMap = serviceManagementBean.getLoggedInSessions();

        for (Map.Entry<Long, Map<String, String>> entry : sessionPrincipalMap.entrySet()) {
            long sessionId = entry.getKey();
            Map<String, String> userPrincipals = entry.getValue();

            for (Map.Entry<String, String> principal : userPrincipals.entrySet()) {
                String key = principal.getKey();
                Class<?> userPrincipalClass = Utils.loadClass(principal.getValue());

                // Case sensitive for both name and class-name.
                if (key.equals(principalName) && (principalClass.isAssignableFrom(userPrincipalClass))) {
                    SessionMXBean sessionBean = managementServiceHandler.getSessionMXBean(sessionId);
                    sessionBean.close();
                    serviceManagementBean.removeSessionManagementBean(sessionId);
                    break;
                }
            }
        }
    }
}

