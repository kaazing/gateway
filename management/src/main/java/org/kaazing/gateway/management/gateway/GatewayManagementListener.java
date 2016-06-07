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


/**
 * Interface to be implemented by those objects that want to act as listeners for gateway-level management events on particular
 * GateawyManagementBean instances. Presumably each implementer of this interface would be protocol-specific.
 */
public interface GatewayManagementListener {
    // Service-level event handlers, ultimately called from the service strategy object.
    // All of the following must be executed OFF a session's IO thread.
    void doSessionCreated(final GatewayManagementBean gatewayBean,
                                 final long sessionId) throws Exception;

    void doSessionClosed(final GatewayManagementBean gatewayBean,
                                final long sessionId) throws Exception;

    void doMessageReceived(final GatewayManagementBean gatewayBean,
                                  final long sessionId) throws Exception;

    void doFilterWrite(final GatewayManagementBean gatewayBean,
                              final long sessionId) throws Exception;

    void doExceptionCaught(final GatewayManagementBean gatewayBean,
                                  final long sessionId) throws Exception;
}
