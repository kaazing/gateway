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
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import org.kaazing.gateway.management.session.SessionManagementBean;
import org.kaazing.gateway.management.session.SessionManagementListener;

public final class JmxSessionManagementListener extends NotificationBroadcasterSupport implements SessionManagementListener {
    private static final String SESSION_CREATED = "session.created";
    private static final String SESSION_CLOSED = "session.closed";

    private JmxManagementServiceHandler managementServiceHandler;

    public JmxSessionManagementListener(JmxManagementServiceHandler managementServiceHandler) {
        this.managementServiceHandler = managementServiceHandler;
    }

    // All of the following are expected to be OFF any session's IO thread.
    @Override
    public void doSessionCreated(SessionManagementBean sessionBean) throws Exception {
        SessionMXBean sessionMxBean = managementServiceHandler.getSessionMXBean(sessionBean.getId());

        Map<String, String> userPrincipals = sessionBean.getUserPrincipalMap();

        // Send a notification for the principals for the logged-in session.
        if (userPrincipals != null) {
            Map<String, Map<String, String>> userData = new HashMap<>();
            userData.put(sessionMxBean.getObjectName().toString(), userPrincipals);

            Notification n2 = new Notification(SESSION_CREATED,
                    sessionMxBean,
                    managementServiceHandler.nextNotificationSequenceNumber(),
                    System.currentTimeMillis(),
                    "Session Credentials Registered");
            n2.setUserData(userData);
            sendNotification(n2);
        }
    }

    @Override
    public void doSessionClosed(SessionManagementBean sessionBean) throws Exception {
        SessionMXBean sessionMxBean = managementServiceHandler.removeSessionMXBean(sessionBean);

        Map<String, String> userPrincipals = sessionBean.getUserPrincipalMap();

        // Send a notification for the principals for the no-longer-logged-in session.
        if (userPrincipals != null) {
            Map<String, Map<String, String>> userData = new HashMap<>();
            userData.put(sessionMxBean.getObjectName().toString(), userPrincipals);

            Notification n2 = new Notification(SESSION_CLOSED,
                    sessionMxBean,
                    managementServiceHandler.nextNotificationSequenceNumber(),
                    System.currentTimeMillis(),
                    "Session Credentials Deregistered");
            n2.setUserData(userData);
            sendNotification(n2);
        }
    }

    @Override
    public void doMessageReceived(SessionManagementBean sessionBean, Object message) throws Exception {
        // We could do something here if we wanted to.
    }

    @Override
    public void doFilterWrite(final SessionManagementBean sessionBean, final Object message, final Object originalMessage) throws
            Exception {
        // We could do something here if we wanted to.
    }

    @Override
    public void doExceptionCaught(SessionManagementBean sessionBean, Throwable cause) {
        // We could do something here if we wanted to.
    }

    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        // notification information about session->credentials mappings
        String[] sessionCredentialsTypes = new String[]{SESSION_CREATED, SESSION_CLOSED};
        String sessionCredentialsClassName = Notification.class.getName();
        String sessionCredentialsDescription = "The objectName to login name mapping for authenticated sessions";
        MBeanNotificationInfo sessionCredentialsInfo = new MBeanNotificationInfo(sessionCredentialsTypes,
                sessionCredentialsClassName,
                sessionCredentialsDescription);

        return new MBeanNotificationInfo[]{sessionCredentialsInfo};
    }

}
