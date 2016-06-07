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
package org.kaazing.gateway.management.filter;

import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.management.ManagementStrategy;
import org.kaazing.gateway.management.Utils.ManagementSessionType;
import org.kaazing.gateway.management.context.ManagementContext;
import org.kaazing.gateway.management.service.ServiceManagementBean;
import org.kaazing.mina.core.session.IoSessionEx;

/**
 * "Strategy" object to implement management processing. This is only done on non-management session requests.
 * <p/>
 * This highest level just does nothing.
 * <p/>
 * ALL REQUESTS WILL BE ON ONE OR ANOTHER OF THE IO THREADS, SO MUST NOT BLOCK!
 */
public interface ManagementFilterStrategy extends ManagementStrategy {

    void doSessionCreated(ManagementContext managementContext,
                                 ServiceManagementBean serviceBean,
                                 IoSessionEx session,
                                 ManagementSessionType managementSessionType) throws Exception;

    void doSessionClosed(ManagementContext managementContext,
                                ServiceManagementBean serviceBean,
                                long sessionId,
                                ManagementSessionType managementSessionType) throws Exception;

    void doMessageReceived(ManagementContext managementContext,
                                  ServiceManagementBean serviceBean,
                                  long sessionId,
                                  long sessionBytesRead,  // as of the message being received
                                  Object message) throws Exception;

    void doFilterWrite(ManagementContext managementContext,
                              ServiceManagementBean serviceBean,
                              long sessionId,
                              long sessionBytesRead,  // as of the message being sent
                              WriteRequest writeRequest) throws Exception;

    void doExceptionCaught(ManagementContext managementContext,
                                  ServiceManagementBean serviceBean,
                                  long sessionId,
                                  Throwable cause) throws Exception;
}
