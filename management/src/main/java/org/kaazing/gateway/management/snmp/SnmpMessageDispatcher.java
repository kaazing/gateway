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

package org.kaazing.gateway.management.snmp;

import java.io.IOException;
import org.kaazing.gateway.management.snmp.transport.ManagementAddress;
import org.kaazing.gateway.management.snmp.transport.ManagementTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.TransportMapping;
import org.snmp4j.smi.Address;

/**
 * The Kaazing version of SNMP message dispatcher code, so we can take the data and provide it over WebSocket.
 * <p/>
 * Kaazing's SNMP support is based on the SNMP4J open-source library under the Apache 2.0 license. To see the full text of the
 * license, please see the Kaazing third-party licenses file.
 */
public class SnmpMessageDispatcher extends MessageDispatcherImpl {
    private static final Logger logger = LoggerFactory.getLogger(SnmpMessageDispatcher.class);

    /**
     * Sends a message using the <code>TransportMapping</code> that has been assigned for the supplied address type.
     *
     * @param transport   the transport mapping to be used to send the message.
     * @param destAddress the transport address where to send the message. The <code>destAddress</code> must be compatible with
     *                    the supplied <code>transport</code>.
     * @param message     the SNMP message to send.
     * @throws IOException if an I/O error occured while sending the message or if there is no transport mapping defined for the
     *                     supplied address type.
     */
    @Override
    protected void sendMessage(TransportMapping transport, Address destAddress, byte[] message) throws IOException {
        // the Kaazing ManagementAdress object has a callback for sending the message on the IoSessionEx.
        if ((destAddress instanceof ManagementAddress) && (transport instanceof ManagementTransport)) {
            transport.sendMessage(destAddress, message);
        } else {
            // log an issue with getting a message for an unsupported address
            logger.warn(
                    "SNMP message received for unsupported transport/address combo - transport: " + transport + " address: " +
                            destAddress);
        }
    }
}
