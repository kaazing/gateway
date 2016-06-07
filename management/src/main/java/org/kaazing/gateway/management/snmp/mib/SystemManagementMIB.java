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
package org.kaazing.gateway.management.snmp.mib;

import org.kaazing.gateway.management.Utils;
import org.kaazing.gateway.management.context.ManagementContext;
import org.kaazing.gateway.management.snmp.SummaryDataIntervalMO;
import org.kaazing.gateway.management.system.HostManagementBean;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOAccess;
import org.snmp4j.agent.MOGroup;
import org.snmp4j.agent.MOServer;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOFactory;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.smi.Counter64;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;

/**
 * MIB for returning system data (e.g., current load average, memory usage) from the machine the gateway is running on.
 * <p/>
 * Kaazing's SNMP support is based on the SNMP4J open-source library under the Apache 2.0 license. To see the full text of the
 * license, please see the Kaazing third-party licenses file.
 */
public class SystemManagementMIB implements MOGroup {

    private static final int OS_NAME_OPER = 1;
    private static final int UPTIME_SECONDS_OPER = 2;
    private static final int TOTAL_FREE_MEMORY_OPER = 3;
    private static final int TOTAL_USED_MEMORY_OPER = 4;
    private static final int TOTAL_MEMORY_OPER = 5;
    private static final int TOTAL_FREE_SWAP_OPER = 6;
    private static final int TOTAL_USED_SWAP_OPER = 7;
    private static final int TOTAL_SWAP_OPER = 8;
    private static final int CPU_PERCENTAGE_OPER = 9;

    private static final int SUMMARY_DATA_FIELDS_OPER = 30;
    private static final int SUMMARY_DATA_OPER = 31;

    private final ManagementContext managementContext;

    private SystemString osName;
    private SystemDouble uptimeSeconds;
    private SystemLong totalFreeMemory;
    private SystemLong totalUsedMemory;
    private SystemLong totalMemory;
    private SystemLong totalFreeSwap;
    private SystemLong totalUsedSwap;
    private SystemLong totalSwap;
    private SystemDouble cpuPercentage;

    private SystemString summaryDataFields;

    private SystemString summaryData;

    private MOScalar summaryDataNotificationInterval;

    private MOScalar summaryDataGatherInterval;

    private HostManagementBean bean;

    public SystemManagementMIB(ManagementContext managementContext, MOFactory factory) {
        this.managementContext = managementContext;
        createMO(factory);
    }

    private void createMO(MOFactory moFactory) {
        try {
            osName = new SystemString(MIBConstants.oidSystemOsName,
                    moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new OctetString(),
                    OS_NAME_OPER);

            uptimeSeconds = new SystemDouble(MIBConstants.oidSystemUptimeSeconds,
                    moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new Counter64(),
                    UPTIME_SECONDS_OPER);

            totalFreeMemory = new SystemLong(MIBConstants.oidSystemTotalFreeMemory,
                    moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new Counter64(),
                    TOTAL_FREE_MEMORY_OPER);

            totalUsedMemory = new SystemLong(MIBConstants.oidSystemTotalUsedMemory,
                    moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new Counter64(),
                    TOTAL_USED_MEMORY_OPER);

            totalMemory = new SystemLong(MIBConstants.oidSystemTotalMemory,
                    moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new Counter64(),
                    TOTAL_MEMORY_OPER);

            totalFreeSwap = new SystemLong(MIBConstants.oidSystemTotalFreeSwap,
                    moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new Counter64(),
                    TOTAL_FREE_SWAP_OPER);

            totalUsedSwap = new SystemLong(MIBConstants.oidSystemTotalUsedSwap,
                    moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new Counter64(),
                    TOTAL_USED_SWAP_OPER);

            totalSwap = new SystemLong(MIBConstants.oidSystemTotalSwap,
                    moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new Counter64(),
                    TOTAL_SWAP_OPER);

            cpuPercentage = new SystemDouble(MIBConstants.oidSystemCpuPercentage,
                    moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new Counter64(),
                    CPU_PERCENTAGE_OPER);


            summaryDataFields = new SystemString(MIBConstants.oidSystemSummaryDataFields,
                    moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new OctetString(),
                    SUMMARY_DATA_FIELDS_OPER);

            summaryData = new SystemString(MIBConstants.oidSystemSummaryData,
                    moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new OctetString(),
                    SUMMARY_DATA_OPER);

            summaryDataNotificationInterval = new SummaryDataIntervalMO(moFactory,
                    managementContext.getSystemSummaryDataNotificationInterval(),
                    MIBConstants.oidSystemSummaryDataNotificationInterval);

            summaryDataGatherInterval = new SummaryDataIntervalMO(moFactory,
                    managementContext.getSystemSummaryDataNotificationInterval(),
                    MIBConstants.oidSystemSummaryDataGatherInterval);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void registerMOs(MOServer server, OctetString context) throws DuplicateRegistrationException {
        server.register(osName, context);
        server.register(uptimeSeconds, context);
        server.register(totalFreeMemory, context);
        server.register(totalUsedMemory, context);
        server.register(totalMemory, context);
        server.register(totalFreeSwap, context);
        server.register(totalUsedSwap, context);
        server.register(totalSwap, context);
        server.register(cpuPercentage, context);

        server.register(summaryDataFields, context);
        server.register(summaryData, context);
        server.register(summaryDataNotificationInterval, context);
        server.register(summaryDataGatherInterval, context);
    }

    @Override
    public void unregisterMOs(MOServer server, OctetString context) {
        server.unregister(osName, context);
        server.unregister(uptimeSeconds, context);
        server.unregister(totalFreeMemory, context);
        server.unregister(totalUsedMemory, context);
        server.unregister(totalMemory, context);
        server.unregister(totalFreeSwap, context);
        server.unregister(totalUsedSwap, context);
        server.unregister(totalSwap, context);
        server.unregister(cpuPercentage, context);

        server.unregister(summaryDataFields, context);
        server.unregister(summaryData, context);
        server.unregister(summaryDataNotificationInterval, context);
        server.unregister(summaryDataGatherInterval, context);
    }

    public void addSystemManagementBean(HostManagementBean systemManagementBean) {
        bean = systemManagementBean;
    }

    class SystemLong extends MOScalar {
        private int operation;

        SystemLong(OID id, MOAccess access, Variable value, int operation) {
            super(id, access, value);
            this.operation = operation;
        }

        @Override
        public Variable getValue() {
            long value;

            switch (operation) {
                case TOTAL_FREE_MEMORY_OPER:
                    value = bean.getTotalFreeMemory();
                    break;
                case TOTAL_USED_MEMORY_OPER:
                    value = bean.getTotalUsedMemory();
                    break;
                case TOTAL_MEMORY_OPER:
                    value = bean.getTotalMemory();
                    break;
                case TOTAL_FREE_SWAP_OPER:
                    value = bean.getTotalFreeSwap();
                    break;
                case TOTAL_USED_SWAP_OPER:
                    value = bean.getTotalUsedSwap();
                    break;
                case TOTAL_SWAP_OPER:
                    value = bean.getTotalSwap();
                    break;
                default:
                    throw new RuntimeException("SystemLong incorrectly configured with unsupported operation: " + operation);
            }
            return new Counter64(value);
        }
    }

    class SystemDouble extends MOScalar {
        private int operation;

        SystemDouble(OID id, MOAccess access, Variable value, int operation) {
            super(id, access, value);
            this.operation = operation;
        }

        @Override
        public Variable getValue() {
            double value;

            switch (operation) {
                case UPTIME_SECONDS_OPER:
                    value = bean.getUptimeSeconds();
                    break;
                case CPU_PERCENTAGE_OPER:
                    value = bean.getCpuPercentage();
                    break;
                default:
                    throw new RuntimeException("SystemFloat incorrectly configured with unsupported operation: " + operation);
            }

            // multiply by 1000, return as a long, since we don't have doubles,
            // so we have precision of 3 digits past the decimal.
            return new Counter64(Math.round(value * 1000));
        }
    }

    class SystemString extends MOScalar {
        private int operation;

        SystemString(OID id, MOAccess access, Variable value, int operation) {
            super(id, access, value);
            this.operation = operation;
        }

        @Override
        public Variable getValue() {
            String value;

            switch (operation) {
                case OS_NAME_OPER:
                    value = bean.getOSName();
                    break;
                case SUMMARY_DATA_FIELDS_OPER:
                    value = bean.getSummaryDataFields();
                    break;
                case SUMMARY_DATA_OPER:
                    value = bean.getSummaryData();
                    break;
                default:
                    throw new RuntimeException("SystemString incorrectly configured with unsupported operation: " + operation);
            }

            OctetString val = (OctetString) Utils.stringToVariable(value);
            return val;
        }
    }

}
