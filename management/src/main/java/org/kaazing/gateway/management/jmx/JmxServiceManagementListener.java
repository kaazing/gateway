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

import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import org.apache.mina.integration.jmx.IoSessionMBean;
import org.kaazing.gateway.management.service.ServiceManagementBean;
import org.kaazing.gateway.management.service.ServiceManagementListener;
import org.kaazing.mina.core.session.DummySessionEx;

public final class JmxServiceManagementListener extends NotificationBroadcasterSupport implements ServiceManagementListener {

    /**
     * Executor, initially for use with ServiceMXBeanImpl for sendNotification(), to make notification handling asynchronous.
     */
    private static final Executor executor;

    static {
        // load an IoSessionMBean to force loading of various MINA internal
        // classes.  In a test, if somebody tries to cut a large number (e.g. 10000)
        // connections, then reconnect, there is blocking while the classes
        // load for the first IoSessionMBean.  By preloading, we avoid the block.
        @SuppressWarnings("unused")
        IoSessionMBean dummyBean = new IoSessionMBean(new DummySessionEx());
        executor = Executors.newSingleThreadExecutor();
    }

    // The following must all run OFF the IO threads

    private JmxManagementServiceHandler managementServiceHandler;

    public JmxServiceManagementListener(JmxManagementServiceHandler managementServiceHandler) {
        super(executor);
        this.managementServiceHandler = managementServiceHandler;
    }

    @Override
    public void doSessionCreated(final ServiceManagementBean serviceBean,
                                 long newCurrentSessionCount,
                                 long newTotalSessionCount) throws Exception {
        // The session bean will already have sent a notification that the session credentials
        // have been registered before this method is called.

        ServiceMXBean serviceMXBean = managementServiceHandler.getServiceMXBean(serviceBean.getId());

        // Send a notification for the changing of the session count, giving old and new values.
        long currentTime = System.currentTimeMillis();
        Notification n = new AttributeChangeNotification(serviceMXBean,
                managementServiceHandler.nextNotificationSequenceNumber(),
                currentTime,
                "Sessions Count changed",
                "Sessions Count",
                "long",
                newCurrentSessionCount - 1,
                newCurrentSessionCount);
        sendNotification(n);

        // Send a notification for the change in cumulative session count.
        n = new AttributeChangeNotification(serviceMXBean,
                managementServiceHandler.nextNotificationSequenceNumber(),
                currentTime,
                "Cumulative sessions count changed",
                "Cumulative Sessions Count",
                "long",
                newTotalSessionCount - 1,
                newTotalSessionCount);
        sendNotification(n);
    }

    @Override
    public void doSessionClosed(final ServiceManagementBean serviceBean,
                                long sessionId,
                                long newCurrentSessionCount) throws Exception {
        // The session bean will already have sent a notification that the session credentials
        // have been deregistered before this method is called.

        ServiceMXBean serviceMXBean = managementServiceHandler.getServiceMXBean(serviceBean.getId());

        // Send a notification for the changing of the session count, giving old and new values.
        Notification n = new AttributeChangeNotification(serviceMXBean,
                managementServiceHandler.nextNotificationSequenceNumber(),
                System.currentTimeMillis(),
                "Sessions Count changed",
                "Sessions Count",
                "long",
                newCurrentSessionCount + 1,
                newCurrentSessionCount);
        sendNotification(n);
    }

    @Override
    public void doMessageReceived(final ServiceManagementBean serviceBean,
                                  long sessionId,
                                  ByteBuffer message) throws Exception {
        // We could do something here if we wanted to.
    }

    @Override
    public void doFilterWrite(final ServiceManagementBean serviceBean,
                              long sessionId,
                              ByteBuffer writeMessage) throws Exception {
        // We could do something here if we wanted to.
    }

    @Override
    public void doExceptionCaught(final ServiceManagementBean serviceBean,
                                  long sessionId,
                                  String exceptionMessage) throws Exception {
        // We could do something here if we wanted to.
    }


    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        String[] types = new String[]{AttributeChangeNotification.ATTRIBUTE_CHANGE};
        String name = AttributeChangeNotification.class.getName();
        String description = "Number of sessions in Kaazing Enterprise Gateway has changed";
        MBeanNotificationInfo info = new MBeanNotificationInfo(types, name, description);

        return new MBeanNotificationInfo[]{info};
    }
}
