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

package org.kaazing.gateway.management.service;

import java.nio.ByteBuffer;


/**
 * Interface to be implemented by those objects that want to act as listeners
 * for service-level management events on particular ServiceManagementBean instances.
 * Presumably each implementer of this interface would be protocol-specific.
 */
public interface ServiceManagementListener {
    // Service-level event handlers, ultimately called from the service strategy object.
    // All of the following must be executed OFF a session's IO thread.
    public void doSessionCreated(final ServiceManagementBean serviceBean,
                                  final long newCurrentSessionCount, 
                                  final long newTotalSessionCount) throws Exception;

    public void doSessionClosed(final ServiceManagementBean serviceBean,
                                 final long sessionId,
                                 final long newCurrentSessionCount) throws Exception;

    public void doMessageReceived(final ServiceManagementBean serviceBean,
                                   final long sessionId, 
                                   final ByteBuffer message) throws Exception;

    public void doFilterWrite(final ServiceManagementBean serviceBean,
                               final long sessionId, 
                               final ByteBuffer writeMessage) throws Exception;

    public void doExceptionCaught(final ServiceManagementBean serviceBean,
                                   final long sessionId, 
                                   final String exceptionMessage) throws Exception;
}
