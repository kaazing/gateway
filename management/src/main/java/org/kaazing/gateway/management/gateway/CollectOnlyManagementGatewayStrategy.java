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
package org.kaazing.gateway.management.gateway;

import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.management.Utils.ManagementSessionType;

/**
 * "Strategy" object to implement management processing. This is only done on non-management session requests.
 */
public class CollectOnlyManagementGatewayStrategy implements ManagementGatewayStrategy {

    @Override
    public void doSessionCreated(GatewayManagementBean gatewayBean,
                                 long sessionId,
                                 ManagementSessionType managementSessionType) throws Exception {
        gatewayBean.doSessionCreated(sessionId, managementSessionType);
    }

    @Override
    public void doSessionClosed(GatewayManagementBean gatewayBean,
                                long sessionId,
                                ManagementSessionType managementSessionType) throws Exception {
        gatewayBean.doSessionClosed(sessionId, managementSessionType);
    }

    @Override
    public void doMessageReceived(GatewayManagementBean gatewayBean,
                                  long sessionId,
                                  long sessionBytesRead,
                                  Object message) throws Exception {
        gatewayBean.doMessageReceived(sessionId, sessionBytesRead, message);

    }

    @Override
    public void doFilterWrite(GatewayManagementBean gatewayBean,
                              long sessionId,
                              long sessionBytesWritten,
                              WriteRequest writeRequest) throws Exception {
        gatewayBean.doFilterWrite(sessionId, sessionBytesWritten, writeRequest);

    }

    @Override
    public void doExceptionCaught(GatewayManagementBean gatewayBean,
                                  long sessionId,
                                  Throwable cause) throws Exception {
        gatewayBean.doExceptionCaught(sessionId, cause);
    }

    public String toString() {
        return "COLLECT_ONLY_GATEWAY_STRATEGY";
    }

}
