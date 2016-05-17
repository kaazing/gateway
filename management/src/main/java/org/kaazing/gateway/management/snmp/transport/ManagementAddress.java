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

import org.kaazing.mina.core.session.IoSessionEx;
import org.snmp4j.smi.Address;

public class ManagementAddress implements Address {

    private final IoSessionEx session;

    public ManagementAddress(IoSessionEx session) {
        this.session = session;
    }

    public IoSessionEx getSession() {
        return session;
    }

    @Override
    public boolean isValid() {
        return session.isConnected();
    }

    @Override
    public boolean parseAddress(String address) {
        return true;
    }

    @Override
    public void setValue(String address) {
        // no-op
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof ManagementAddress) {
            if (session.equals(((ManagementAddress) o).getSession())) {
                return 0;
            }
        }
        return -1;
    }
}
