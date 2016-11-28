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

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.management.NotificationBroadcasterSupport;
import org.kaazing.gateway.management.gateway.GatewayManagementBean;
import org.kaazing.gateway.management.gateway.GatewayManagementListener;

public final class JmxGatewayManagementListener extends NotificationBroadcasterSupport implements GatewayManagementListener {

    /**
     * Executor, initially for use with GatewayMXBeanImpl for sendNotification(), to make notification handling asynchronous.
     */
    private static final Executor executor;

    static {
        // load an IoSessionMBean to force loading of various MINA internal
        // classes.  In a test, if somebody tries to cut a large number (e.g. 10000)
        // connections, then reconnect, there is blocking while the classes
        // load for the first IoSessionMBean.  By preloading, we avoid the block.
        executor = Executors.newSingleThreadExecutor();
    }

    // The following must all run OFF the IO threads

    public JmxGatewayManagementListener(JmxManagementServiceHandler managementServiceHandler) {
        super(executor);
    }

    @Override
    public void doSessionCreated(final GatewayManagementBean gatewayBean,
                                 final long sessionId) throws Exception {
        // for the moment we don't sent gateway-level management notifications on sessionCreated.
    }

    @Override
    public void doSessionClosed(final GatewayManagementBean gatewayBean,
                                final long sessionId) throws Exception {
        // for the moment we don't sent gateway-level management notifications on sessionClosed
    }

    @Override
    public void doMessageReceived(final GatewayManagementBean gatewayBean,
                                  final long sessionId) throws Exception {
        // for the moment we don't sent gateway-level management notifications on messageReceived
    }

    @Override
    public void doFilterWrite(final GatewayManagementBean gatewayBean,
                              final long sessionId) throws Exception {
        // for the moment we don't sent gateway-level management notifications on filterWrite
    }

    @Override
    public void doExceptionCaught(final GatewayManagementBean gatewayBean,
                                  final long sessionId) throws Exception {
        // for the moment we don't sent gateway-level management notifications on exceptionCaught
    }
}
