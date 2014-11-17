/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.management.gateway;

import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.management.ManagementStrategy;
import org.kaazing.gateway.management.Utils.ManagementSessionType;

/**
 * "Strategy" object to implement gateway-level management processing. This is only done on
 * non-management session requests. 
 * 
 * ALL REQUESTS WILL BE ON ONE OR ANOTHER OF THE IO THREADS, SO MUST NOT BLOCK!
 */
public interface ManagementGatewayStrategy extends ManagementStrategy {

    public void doSessionCreated(final GatewayManagementBean gatewayBean, 
                                 long sessionId, 
                                 ManagementSessionType managementSessionType) throws Exception;

    public void doSessionClosed(final GatewayManagementBean gatewayBean, 
                                long sessionId, 
                                ManagementSessionType managementSessionType) throws Exception;

    public void doMessageReceived(final GatewayManagementBean gatewayBean, 
                                  long sessionId, 
                                  long sessionBytesRead, 
                                  final Object message) throws Exception;

    public void doFilterWrite(final GatewayManagementBean gatewayBean, 
                              long sessionId,
                              long sessionBytesWritten, 
                              final WriteRequest writeRequest) throws Exception;

    public void doExceptionCaught(final GatewayManagementBean gatewayBean, 
                                  long sessionId, 
                                  final Throwable cause) throws Exception ;
}
