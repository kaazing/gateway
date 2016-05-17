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
package org.kaazing.gateway.management.snmp;

import org.kaazing.gateway.management.SummaryManagementInterval;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOFactory;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;

/**
 * Summary data interval, in seconds, as an SNMP object.
 * <p/>
 * Kaazing's SNMP support is based on the SNMP4J open-source library under the Apache 2.0 license. To see the full text of the
 * license, please see the Kaazing third-party licenses file.
 */
public class SummaryDataIntervalMO extends MOScalar {
    private SummaryManagementInterval interval;

    public SummaryDataIntervalMO(MOFactory moFactory, SummaryManagementInterval interval, OID intervalOID) {
        super(intervalOID,
                moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_WRITE),
                new Integer32(interval.getInterval()));

        this.interval = interval;
    }

    @Override
    public Variable getValue() {
        return new Integer32(interval.getInterval());
    }

    @Override
    public int setValue(Variable newInterval) {
        if (newInterval instanceof Integer32) {
            interval.setInterval(((Integer32) newInterval).getValue());
            return SnmpConstants.SNMP_ERROR_SUCCESS;
        }
        return SnmpConstants.SNMP_ERROR_BAD_VALUE;
    }
}
