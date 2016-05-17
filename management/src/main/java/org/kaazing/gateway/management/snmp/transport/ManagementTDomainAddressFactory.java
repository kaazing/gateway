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
package org.kaazing.gateway.management.snmp.transport;

import org.kaazing.gateway.server.context.resolve.DefaultServiceContext;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.agent.mo.snmp.TDomainAddressFactory;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

public class ManagementTDomainAddressFactory implements TDomainAddressFactory {
    private static final Logger logger = LoggerFactory.getLogger(ManagementTDomainAddressFactory.class);

    public static final OID KaazingTransportDomain = new OID("1.3.6.1.2.1.100.1.'kaazing'");

    private final DefaultServiceContext serviceContext;

    public ManagementTDomainAddressFactory(DefaultServiceContext serviceContext) {
        this.serviceContext = serviceContext;
    }

    @Override
    public Address createAddress(OID transportDomain, OctetString address) {
        String sessionIdString = new String(address.getValue());
        try {
            long sessionId = Long.parseLong(sessionIdString);
            IoSessionEx session = serviceContext.getActiveSession(sessionId);
            if (session != null) {
                return new ManagementAddress(session);
            } else {
                if (logger.isTraceEnabled()) {
                    logger.trace("Attempting to create a ManagementAddress for a non-existent session: " + sessionIdString);
                }
            }
        } catch (NumberFormatException ex) {
            logger.warn("Received an invalid address: " + sessionIdString + " for creating a ManagementAddress.");
        }
        return null;
    }

    @Override
    public OctetString getAddress(Address address) {
        if ((address == null) || !(address instanceof ManagementAddress)) {
            return null;
        }
        ManagementAddress managementAddress = (ManagementAddress) address;
        long sessionId = managementAddress.getSession().getId();
        return new OctetString(Long.toString(sessionId));
    }

    @Override
    public OID getTransportDomain(Address address) {
        if (address instanceof ManagementAddress) {
            return KaazingTransportDomain;
        }
        return null;
    }

    @Override
    public boolean isValidAddress(OID transportDomain, OctetString address) {
        if (KaazingTransportDomain.equals(transportDomain)) {
            String sessionIdString = address.toString();
            try {
                long sessionId = Long.parseLong(sessionIdString);
                if (serviceContext.getActiveSession(sessionId) != null) {
                    return true;
                }
            } catch (NumberFormatException ex) {
                // address didn't parse, fall through to returning false
            }
        }
        return false;
    }
}
