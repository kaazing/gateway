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
package org.kaazing.gateway.management.service;

import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.management.Utils.ManagementSessionType;

/**
 * "Strategy" object to implement management processing. This is only done on non-management session requests.
 * <p/>
 * ALL REQUESTS WILL BE ON ONE OR ANOTHER OF THE IO THREADS, SO MUST NOT BLOCK!
 */
public class CollectOnlyManagementServiceStrategy implements ManagementServiceStrategy {

    public CollectOnlyManagementServiceStrategy() {
    }

    @Override
    public void doSessionCreated(final ServiceManagementBean serviceBean,
                                 final long sessionId,
                                 final ManagementSessionType managementSessionType) throws Exception {
        serviceBean.doSessionCreated(sessionId, managementSessionType);
    }

    @Override
    public void doSessionClosed(final ServiceManagementBean serviceBean,
                                final long sessionId,
                                final ManagementSessionType managementSessionType) throws Exception {
        serviceBean.doSessionClosed(sessionId, managementSessionType);
    }

    @Override
    public void doMessageReceived(final ServiceManagementBean serviceBean,
                                  final long sessionId,
                                  long sessionReadBytes,
                                  final Object message) throws Exception {
        serviceBean.doMessageReceived(sessionId, sessionReadBytes, message);
    }

    @Override
    public void doFilterWrite(final ServiceManagementBean serviceBean,
                              final long sessionId,
                              long sessionWrittenBytes,
                              final WriteRequest writeRequest) throws Exception {
        serviceBean.doFilterWrite(sessionId, sessionWrittenBytes, writeRequest);
    }

    @Override
    public void doExceptionCaught(final ServiceManagementBean serviceBean,
                                  final long sessionId,
                                  final Throwable cause) throws Exception {
        serviceBean.doExceptionCaught(sessionId, cause);
    }

    public String toString() {
        return "COLLECT_ONLY_SERVICE_STRATEGY";
    }
}
