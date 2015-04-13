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

package org.kaazing.gateway.management.snmp.mib;

import org.kaazing.gateway.management.Utils;
import org.kaazing.gateway.management.context.ManagementContext;
import org.kaazing.gateway.management.snmp.SummaryDataIntervalMO;
import org.kaazing.gateway.management.system.CpuListManagementBean;
import org.kaazing.gateway.management.system.CpuManagementBean;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOAccess;
import org.snmp4j.agent.MOGroup;
import org.snmp4j.agent.MOServer;
import org.snmp4j.agent.mo.DefaultMOMutableRow2PC;
import org.snmp4j.agent.mo.DefaultMOMutableTableModel;
import org.snmp4j.agent.mo.DefaultMOTable;
import org.snmp4j.agent.mo.DefaultMOTableRow;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOColumn;
import org.snmp4j.agent.mo.MOFactory;
import org.snmp4j.agent.mo.MOMutableColumn;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.agent.mo.MOTable;
import org.snmp4j.agent.mo.MOTableIndex;
import org.snmp4j.agent.mo.MOTableModel;
import org.snmp4j.agent.mo.MOTableRow;
import org.snmp4j.agent.mo.MOTableSubIndex;
import org.snmp4j.agent.mo.snmp.AgentCapabilityList;
import org.snmp4j.agent.request.SubRequest;
import org.snmp4j.event.CounterEvent;
import org.snmp4j.event.CounterListener;
import org.snmp4j.smi.Counter64;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.Variable;

/**
 * The support for the Kaazing 'CPU' table in the Kaazing MIB. There are two Java types involved: CpuListManagementBean (manages
 * whole list of CPU beans) and CpuManagementBean (data for a single CPU in the list of CPUs).
 * <p/>
 * Kaazing's SNMP support is based on the SNMP4J open-source library under the Apache 2.0 license. To see the full text of the
 * license, please see the Kaazing third-party licenses file.
 */
public class CpuManagementMIB implements MOGroup, CounterListener, AgentCapabilityList {
    private static final int NUM_CPUS_OPER = 1;
    private static final int SUMMARY_DATA_FIELDS_OPER = 2;
    private static final int SUMMARY_DATA_OPER = 3;

    private final ManagementContext managementContext;

    private DefaultMOTable sysOREntry;
    private DefaultMOMutableTableModel sysOREntryModel;

    private MOTableSubIndex[] entryIndexes;
    private MOTableIndex entryIndex;
    private MOTable cpuListEntry;
    private MOTableModel cpuEntryModel;

    private SystemInt numCpus;

    private SystemString summaryDataFields;

    private SystemString summaryData;

    private MOScalar summaryDataNotificationInterval;

    private MOScalar summaryDataGatherInterval;

    private CpuListManagementBean bean;

    public CpuManagementMIB(ManagementContext managementContext, MOFactory factory) {
        this.managementContext = managementContext;
        createMO(factory);
    }

    private void createMO(MOFactory moFactory) {
        // Index definition
        OID cpuEntryIndexOID = ((OID) MIBConstants.oidCpuListEntry.clone()).append(1);
        entryIndexes =
                new MOTableSubIndex[]{
                        moFactory.createSubIndex(cpuEntryIndexOID,
                                SMIConstants.SYNTAX_INTEGER, 1, 1),
                };

        entryIndex =
                moFactory.createIndex(entryIndexes, true);

        // Columns
        MOColumn[] entryColumns = new MOColumn[MIBConstants.CPU_COLUMN_COUNT];
        entryColumns[MIBConstants.indexCpuIndex] =
                new MOMutableColumn(MIBConstants.colCpuIndex,
                        SMIConstants.SYNTAX_INTEGER32,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        entryColumns[MIBConstants.indexCpuId] =
                new MOMutableColumn(MIBConstants.colCpuId,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        entryColumns[MIBConstants.indexCpuCombined] =
                new MOMutableColumn(MIBConstants.colCpuCombined,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        entryColumns[MIBConstants.indexCpuIdle] =
                new MOMutableColumn(MIBConstants.colCpuIdle,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        entryColumns[MIBConstants.indexCpuIrq] =
                new MOMutableColumn(MIBConstants.colCpuIrq,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        entryColumns[MIBConstants.indexCpuNice] =
                new MOMutableColumn(MIBConstants.colCpuNice,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        entryColumns[MIBConstants.indexCpuSoftIrq] =
                new MOMutableColumn(MIBConstants.colCpuSoftIrq,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        entryColumns[MIBConstants.indexCpuStolen] =
                new MOMutableColumn(MIBConstants.colCpuStolen,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        entryColumns[MIBConstants.indexCpuSys] =
                new MOMutableColumn(MIBConstants.colCpuSys,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        entryColumns[MIBConstants.indexCpuUser] =
                new MOMutableColumn(MIBConstants.colCpuUser,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        entryColumns[MIBConstants.indexCpuWait] =
                new MOMutableColumn(MIBConstants.colCpuWait,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        entryColumns[MIBConstants.indexCpuSummaryData] =
                new MOMutableColumn(MIBConstants.colCpuSummaryData,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // Table model
        cpuEntryModel = new CpuManagementTableModel();
        cpuListEntry = moFactory.createTable(MIBConstants.oidCpuListEntry,
                entryIndex,
                entryColumns,
                cpuEntryModel);

        numCpus = new SystemInt(MIBConstants.oidCpuListNumCpus,
                moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                new Integer32(),
                NUM_CPUS_OPER);

        summaryDataFields = new SystemString(MIBConstants.oidCpuListSummaryDataFields,
                moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                new OctetString(),
                SUMMARY_DATA_FIELDS_OPER);

        summaryData = new SystemString(MIBConstants.oidCpuListSummaryData,
                moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                new OctetString(),
                SUMMARY_DATA_OPER);

        summaryDataNotificationInterval = new SummaryDataIntervalMO(moFactory,
                managementContext.getCpuListSummaryDataNotificationInterval(),
                MIBConstants.oidCpuListSummaryDataNotificationInterval);

        summaryDataGatherInterval = new SummaryDataIntervalMO(moFactory,
                managementContext.getCpuListSummaryDataGatherInterval(),
                MIBConstants.oidCpuListSummaryDataGatherInterval);
    }

    @Override
    public void registerMOs(MOServer server, OctetString context) throws DuplicateRegistrationException {
        server.register(cpuListEntry, context);

        server.register(numCpus, context);
        server.register(summaryDataFields, context);
        server.register(summaryData, context);
        server.register(summaryDataNotificationInterval, context);
        server.register(summaryDataGatherInterval, context);
    }

    @Override
    public void unregisterMOs(MOServer server, OctetString context) {
        server.unregister(cpuListEntry, context);

        server.unregister(numCpus, context);
        server.unregister(summaryDataFields, context);
        server.unregister(summaryData, context);
        server.unregister(summaryDataNotificationInterval, context);
        server.unregister(summaryDataGatherInterval, context);
    }

    public void addCpuListManagementBean(CpuListManagementBean cpuListManagementBean) {
        bean = cpuListManagementBean;
    }

    @Override
    public void incrementCounter(CounterEvent event) {
        // FIXME: do we need this?
    }

    @Override
    public OID addSysOREntry(OID sysORID, OctetString sysORDescr) {
        OID index = new OID(new int[]{sysOREntryModel.getRowCount() + 1});
        Variable[] values = new Variable[sysOREntry.getColumnCount()];
        int n = 0;
        values[n++] = sysORID;
        values[n++] = sysORDescr;
        DefaultMOTableRow row = new DefaultMOTableRow(index, values);
        sysOREntry.addRow(row);
        return index;
    }

    @Override
    public MOTableRow removeSysOREntry(OID index) {
        return sysOREntry.removeRow(index);
    }

    public OID addCpuManagementBean(CpuManagementBean bean) {
        OID cpuIndexOID = new OID(new int[]{bean.getId()});
        cpuListEntry.addRow(new CpuEntryRow(cpuIndexOID, bean));

        return cpuIndexOID;
    }

    public void removeCpuManagementBean(OID oid) {
        cpuListEntry.removeRow(oid);
    }

    private class CpuManagementTableModel extends DefaultMOMutableTableModel {
    }

    private final class CpuEntryRow extends DefaultMOMutableRow2PC {
        private CpuManagementBean bean;

        private CpuEntryRow(OID index, CpuManagementBean bean) {
            super(index, null);
            this.bean = bean;
        }

        @Override
        public int size() {
            return MIBConstants.CPU_COLUMN_COUNT;
        }

        @Override
        public Variable getValue(int column) {
            Object scalarValue = null;
            try {
                switch (column) {
                    case MIBConstants.indexCpuIndex:
                        return new Integer32(getIndex().last());
                    case MIBConstants.indexCpuId:
                        scalarValue = bean.getId();
                        break;
                    case MIBConstants.indexCpuCombined:
                        scalarValue = bean.getCombined();
                        break;
                    case MIBConstants.indexCpuIdle:
                        scalarValue = bean.getIdle();
                        break;
                    case MIBConstants.indexCpuIrq:
                        scalarValue = bean.getIrq();
                        break;
                    case MIBConstants.indexCpuNice:
                        scalarValue = bean.getNice();
                        break;
                    case MIBConstants.indexCpuSoftIrq:
                        scalarValue = bean.getSoftIrq();
                        break;
                    case MIBConstants.indexCpuStolen:
                        scalarValue = bean.getStolen();
                        break;
                    case MIBConstants.indexCpuSys:
                        scalarValue = bean.getSys();
                        break;
                    case MIBConstants.indexCpuUser:
                        scalarValue = bean.getUser();
                        break;
                    case MIBConstants.indexCpuWait:
                        scalarValue = bean.getWait();
                        break;
                    case MIBConstants.indexCpuSummaryData:
                        return Utils.stringToVariable(bean.getSummaryData());
                    default:
                        return super.getValue(column);
                }
            } catch (Exception ex) {
                // XXX FIXME handle errors
            }

            if (scalarValue == null) {
                return new Null();
            }

            long counterValue = 0;
            if (scalarValue instanceof Double) {
                counterValue = (long) (((Double) scalarValue).doubleValue() * 1000);
            } else if (scalarValue instanceof Float) {
                counterValue = (long) (((Float) scalarValue).floatValue() * 1000);
            } else {
                counterValue = ((Number) scalarValue).longValue();
            }

            return new Counter64(counterValue);
        }

        @Override
        public void commit(SubRequest subRequest, MOTableRow changeSet, int column) {
            setValue(column, (Variable) subRequest.getVariableBinding().getVariable().clone());
            subRequest.completed();
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
            String value = "";

            switch (operation) {
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

    class SystemInt extends MOScalar {
        private int operation;

        SystemInt(OID id, MOAccess access, Variable value, int operation) {
            super(id, access, value);
            this.operation = operation;
        }

        @Override
        public Variable getValue() {
            int value = -1;

            switch (operation) {
                case NUM_CPUS_OPER:
                    value = bean.getNumCpus();
                    break;
                default:
                    throw new RuntimeException("SystemString incorrectly configured with unsupported operation: " + operation);
            }

            Integer32 val = new Integer32(value);
            return val;
        }
    }

}
