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
import org.kaazing.gateway.management.system.NicListManagementBean;
import org.kaazing.gateway.management.system.NicManagementBean;
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
 * MIB support for NIC-level data.
 * <p/>
 * Kaazing's SNMP support is based on the SNMP4J open-source library under the Apache 2.0 license. To see the full text of the
 * license, please see the Kaazing third-party licenses file.
 */
public class NicManagementMIB implements MOGroup, CounterListener, AgentCapabilityList {
    private static final int NET_INTERFACE_NAMES_OPER = 1;
    private static final int SUMMARY_DATA_FIELDS_OPER = 2;
    private static final int SUMMARY_DATA_OPER = 3;

    private final ManagementContext managementContext;

    private DefaultMOTable sysOREntry;
    private DefaultMOMutableTableModel sysOREntryModel;

    private MOTableSubIndex[] entryIndexes;
    private MOTableIndex entryIndex;
    private MOTable nicEntry;
    private MOTableModel nicEntryModel;

    private SystemString netInterfaceNames;

    private SystemString summaryDataFields;

    private SystemString summaryData;

    private MOScalar summaryDataNotificationInterval;

    private MOScalar summaryDataGatherInterval;

    private NicListManagementBean bean;

    public NicManagementMIB(ManagementContext managementContext, MOFactory factory) {
        this.managementContext = managementContext;
        createMO(factory);
    }

    private void createMO(MOFactory moFactory) {
        // Index definition
        OID nicEntryIndexOID = ((OID) MIBConstants.oidNicListEntry.clone()).append(1);
        entryIndexes =
                new MOTableSubIndex[]{
                        moFactory.createSubIndex(nicEntryIndexOID,
                                SMIConstants.SYNTAX_INTEGER, 1, 1),
                };

        entryIndex =
                moFactory.createIndex(entryIndexes, true);

        // Columns
        MOColumn[] entryColumns = new MOColumn[MIBConstants.NIC_COLUMN_COUNT];
        entryColumns[MIBConstants.indexNicIndex] =
                new MOMutableColumn(MIBConstants.colNicIndex,
                        SMIConstants.SYNTAX_INTEGER32,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        entryColumns[MIBConstants.indexNicId] =
                new MOMutableColumn(MIBConstants.colNicId,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        entryColumns[MIBConstants.indexNicName] =
                new MOMutableColumn(MIBConstants.colNicName,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        entryColumns[MIBConstants.indexNicRxBytes] =
                new MOMutableColumn(MIBConstants.colNicRxBytes,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        entryColumns[MIBConstants.indexNicRxBytesPerSecond] =
                new MOMutableColumn(MIBConstants.colNicRxBytesPerSecond,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        entryColumns[MIBConstants.indexNicRxDropped] =
                new MOMutableColumn(MIBConstants.colNicRxDropped,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        entryColumns[MIBConstants.indexNicRxErrors] =
                new MOMutableColumn(MIBConstants.colNicRxErrors,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        entryColumns[MIBConstants.indexNicTxBytes] =
                new MOMutableColumn(MIBConstants.colNicTxBytes,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        entryColumns[MIBConstants.indexNicTxBytesPerSecond] =
                new MOMutableColumn(MIBConstants.colNicTxBytesPerSecond,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        entryColumns[MIBConstants.indexNicTxDropped] =
                new MOMutableColumn(MIBConstants.colNicTxDropped,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        entryColumns[MIBConstants.indexNicTxErrors] =
                new MOMutableColumn(MIBConstants.colNicTxErrors,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        entryColumns[MIBConstants.indexNicSummaryData] =
                new MOMutableColumn(MIBConstants.colNicSummaryData,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // Table model
        nicEntryModel = new NicManagementTableModel();
        nicEntry = moFactory.createTable(MIBConstants.oidNicListEntry,
                entryIndex,
                entryColumns,
                nicEntryModel);

        netInterfaceNames = new SystemString(MIBConstants.oidNicListNetInterfaceNames,
                moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                new OctetString(),
                NET_INTERFACE_NAMES_OPER);

        summaryDataFields = new SystemString(MIBConstants.oidNicListSummaryDataFields,
                moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                new OctetString(),
                SUMMARY_DATA_FIELDS_OPER);

        summaryData = new SystemString(MIBConstants.oidNicListSummaryData,
                moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                new OctetString(),
                SUMMARY_DATA_OPER);

        summaryDataNotificationInterval = new SummaryDataIntervalMO(moFactory,
                managementContext.getNicListSummaryDataNotificationInterval(),
                MIBConstants.oidNicListSummaryDataNotificationInterval);

        summaryDataGatherInterval = new SummaryDataIntervalMO(moFactory,
                managementContext.getNicListSummaryDataNotificationInterval(),
                MIBConstants.oidNicListSummaryDataGatherInterval);
    }

    @Override
    public void registerMOs(MOServer server, OctetString context) throws DuplicateRegistrationException {
        server.register(nicEntry, context);

        server.register(netInterfaceNames, context);
        server.register(summaryDataFields, context);
        server.register(summaryData, context);
        server.register(summaryDataNotificationInterval, context);
        server.register(summaryDataGatherInterval, context);
    }

    @Override
    public void unregisterMOs(MOServer server, OctetString context) {
        server.unregister(nicEntry, context);

        server.unregister(netInterfaceNames, context);
        server.unregister(summaryDataFields, context);
        server.unregister(summaryData, context);
        server.unregister(summaryDataNotificationInterval, context);
        server.unregister(summaryDataGatherInterval, context);
    }

    public void addNicListManagementBean(NicListManagementBean nicListManagementBean) {
        bean = nicListManagementBean;
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

    public OID addNicManagementBean(NicManagementBean bean) {
        OID cpuIndexOID = new OID(new int[]{bean.getId()});
        nicEntry.addRow(new NicEntryRow(cpuIndexOID, bean));

        return cpuIndexOID;
    }

    public void removeNicManagementBean(OID oid) {
        nicEntry.removeRow(oid);
    }

    private class NicManagementTableModel extends DefaultMOMutableTableModel {
    }

    private final class NicEntryRow extends DefaultMOMutableRow2PC {
        private NicManagementBean bean;

        private NicEntryRow(OID index, NicManagementBean bean) {
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
                    case MIBConstants.indexNicIndex:
                        return new Integer32(getIndex().last());
                    case MIBConstants.indexNicId:
                        scalarValue = bean.getId();
                        break;
                    case MIBConstants.indexNicName:
                        return Utils.stringToVariable(bean.getName());
                    case MIBConstants.indexNicRxBytes:
                        scalarValue = bean.getRxBytes();
                        break;
                    case MIBConstants.indexNicRxBytesPerSecond:
                        scalarValue = bean.getRxBytesPerSecond();
                        break;
                    case MIBConstants.indexNicRxDropped:
                        scalarValue = bean.getRxDropped();
                        break;
                    case MIBConstants.indexNicRxErrors:
                        scalarValue = bean.getRxErrors();
                        break;
                    case MIBConstants.indexNicTxBytes:
                        scalarValue = bean.getTxBytes();
                        break;
                    case MIBConstants.indexNicTxBytesPerSecond:
                        scalarValue = bean.getTxBytesPerSecond();
                        break;
                    case MIBConstants.indexNicTxDropped:
                        scalarValue = bean.getTxDropped();
                        break;
                    case MIBConstants.indexNicTxErrors:
                        scalarValue = bean.getTxErrors();
                        break;
                    case MIBConstants.indexNicSummaryData:
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
                case NET_INTERFACE_NAMES_OPER:
                    value = bean.getNetInterfaceNames();
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
