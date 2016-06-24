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

import org.json.JSONArray;
import org.json.JSONException;
import org.kaazing.gateway.management.Utils;
import org.kaazing.gateway.management.context.ManagementContext;
import org.kaazing.gateway.management.gateway.GatewayManagementBean;
import org.kaazing.gateway.management.snmp.SummaryDataIntervalMO;
import org.snmp4j.agent.DuplicateRegistrationException;
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
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.Variable;

/**
 * MIB support for Gateway-level dynamic data.
 * <p/>
 * Kaazing's SNMP support is based on the SNMP4J open-source library under the Apache 2.0 license. To see the full text of the
 * license, please see the Kaazing third-party licenses file.
 */
public class GatewayManagementMIB implements MOGroup, CounterListener, AgentCapabilityList {

    private final ManagementContext managementContext;

    private DefaultMOTable sysOREntry;
    private DefaultMOMutableTableModel sysOREntryModel;

    private MOTableSubIndex[] gatewayEntryIndexes;
    private MOTableIndex gatewayEntryIndex;
    private MOTable gatewayEntry;
    private MOTableModel gatewayEntryModel;

    private MOScalar summaryDataFields;
    private MOScalar summaryDataNotificationInterval;

    public GatewayManagementMIB(ManagementContext managementContext, MOFactory factory) {
        this.managementContext = managementContext;
        createMO(factory);
    }

    private void createMO(MOFactory moFactory) {
        // Index definition
        OID gatewayConfigEntryIndexOID = ((OID) MIBConstants.oidGatewayEntry.clone()).append(1);
        gatewayEntryIndexes =
                new MOTableSubIndex[]{
                        moFactory.createSubIndex(gatewayConfigEntryIndexOID,
                                SMIConstants.SYNTAX_INTEGER, 1, 1),
                };

        gatewayEntryIndex =
                moFactory.createIndex(gatewayEntryIndexes, true);

        // Columns
        MOColumn[] gatewayEntryColumns = new MOColumn[MIBConstants.GATEWAY_COLUMN_COUNT];
        gatewayEntryColumns[MIBConstants.indexGatewayIndex] =
                new MOMutableColumn(MIBConstants.colGatewayIndex,
                        SMIConstants.SYNTAX_INTEGER32,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        gatewayEntryColumns[MIBConstants.indexGatewayId] =
                new MOMutableColumn(MIBConstants.colGatewayId,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        gatewayEntryColumns[MIBConstants.indexTotalCurrentSessions] =
                new MOMutableColumn(MIBConstants.colTotalCurrentSessions,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        gatewayEntryColumns[MIBConstants.indexTotalBytesReceived] =
                new MOMutableColumn(MIBConstants.colTotalBytesReceived,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        gatewayEntryColumns[MIBConstants.indexTotalBytesSent] =
                new MOMutableColumn(MIBConstants.colTotalBytesSent,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        gatewayEntryColumns[MIBConstants.indexUptime] =
                new MOMutableColumn(MIBConstants.colUptime,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        gatewayEntryColumns[MIBConstants.indexStartTime] =
                new MOMutableColumn(MIBConstants.colStartTime,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        gatewayEntryColumns[MIBConstants.indexInstanceKey] =
                new MOMutableColumn(MIBConstants.colInstanceKey,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        gatewayEntryColumns[MIBConstants.indexGatewaySummaryData] =
                new MOMutableColumn(MIBConstants.colGatewaySummaryData,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        gatewayEntryColumns[MIBConstants.indexClusterMembers] =
                new MOMutableColumn(MIBConstants.colClusterMembers,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        gatewayEntryColumns[MIBConstants.indexBalancerMap] =
                new MOMutableColumn(MIBConstants.colBalancerMap,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        gatewayEntryColumns[MIBConstants.indexManagementServiceMap] =
                new MOMutableColumn(MIBConstants.colManagementServiceMap,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        gatewayEntryColumns[MIBConstants.indexLatestUpdateableVersion] = new MOMutableColumn(
                MIBConstants.colLatestUpdateableVersion,
                SMIConstants.SYNTAX_OCTET_STRING,
                moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        gatewayEntryColumns[MIBConstants.indexForceUpdateVersionCheck] = new MOMutableColumn(
                MIBConstants.colForceUpdateVersionCheck,
                SMIConstants.SYNTAX_INTEGER,
                moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_WRITE));

        // Table model
        gatewayEntryModel = new GatewayMXBeanTableModel();
        gatewayEntry = moFactory.createTable(MIBConstants.oidGatewayEntry,
                gatewayEntryIndex,
                gatewayEntryColumns,
                gatewayEntryModel);

        try {
            JSONArray jsonArray = new JSONArray(GatewayManagementBean.SUMMARY_DATA_FIELD_LIST);
            summaryDataFields = new MOScalar(MIBConstants.oidGatewaySummaryDataFields,
                    moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ),
                    new OctetString(jsonArray.toString()));
        } catch (JSONException ex) {
            // Should not be possible to get here, since the list of
            // strings is valid and constant.
        }

        summaryDataNotificationInterval = new SummaryDataIntervalMO(moFactory,
                managementContext.getGatewaySummaryDataNotificationInterval(),
                MIBConstants.oidGatewaySummaryDataNotificationInterval);
    }

    @Override
    public void registerMOs(MOServer server, OctetString context) throws DuplicateRegistrationException {
        server.register(gatewayEntry, context);
        server.register(summaryDataFields, context);
        server.register(summaryDataNotificationInterval, context);
    }

    @Override
    public void unregisterMOs(MOServer server, OctetString context) {
        server.unregister(gatewayEntry, context);
        server.unregister(summaryDataFields, context);
        server.unregister(summaryDataNotificationInterval, context);
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

    public OID addGatewayBean(GatewayManagementBean bean) {
        // Note: as gateways are appearing and disappearing, we cannot just
        // have a gateway index as the current number of rows in the table,
        // because earlier gateway entries may be gone.
        OID gatewayIndexOID = new OID(new int[]{bean.getId()});
        gatewayEntry.addRow(new GatewayEntryRow(gatewayIndexOID, bean));

        return gatewayIndexOID;
    }

    public void removeGatewayBean(OID oid) {
        gatewayEntry.removeRow(oid);
    }

    private class GatewayMXBeanTableModel extends DefaultMOMutableTableModel {
    }

    private final class GatewayEntryRow extends DefaultMOMutableRow2PC {
        private GatewayManagementBean bean;

        private GatewayEntryRow(OID index, GatewayManagementBean bean) {
            super(index, null);
            this.bean = bean;
        }

        @Override
        public int size() {
            return MIBConstants.GATEWAY_COLUMN_COUNT;
        }

        @Override
        public Variable getValue(int column) {
            try {
                switch (column) {
                    case MIBConstants.indexGatewayIndex:
                        return new Integer32(getIndex().last());
                    case MIBConstants.indexGatewayId:
                        return Utils.stringToVariable(bean.getHostAndPid());
                    case MIBConstants.indexTotalCurrentSessions:
                        return new Counter64(bean.getTotalCurrentSessions());
                    case MIBConstants.indexTotalBytesReceived:
                        return new Counter64(bean.getTotalBytesReceived());
                    case MIBConstants.indexTotalBytesSent:
                        return new Counter64(bean.getTotalBytesSent());
                    case MIBConstants.indexUptime:
                        return new Counter64(bean.getUptime());
                    case MIBConstants.indexStartTime:
                        return new Counter64(bean.getStartTime());
                    case MIBConstants.indexInstanceKey:
                        return Utils.stringToVariable(bean.getInstanceKey());
                    case MIBConstants.indexGatewaySummaryData:
                        return new OctetString(bean.getSummaryData());
                    case MIBConstants.indexClusterMembers:
                        return new OctetString(bean.getClusterMembers());
                    case MIBConstants.indexBalancerMap:
                        return new OctetString(bean.getClusterBalancerMap());
                    case MIBConstants.indexManagementServiceMap:
                        return new OctetString(bean.getManagementServiceMap());
                    case MIBConstants.indexLatestUpdateableVersion:
                        return new OctetString(bean.getAvailableUpdateVersion());
                    default:
                        return super.getValue(column);
                }
            } catch (Exception ex) {
                // FIXME:  handle errors
                return new Integer32(-1);
            }
        }

        @Override
        public void commit(SubRequest subRequest, MOTableRow changeSet, int column) {
            setValue(column, (Variable) subRequest.getVariableBinding().getVariable().clone());
            subRequest.completed();
        }

        @Override
        public void setValue(int column, Variable newValue) {
        	if (column == MIBConstants.indexForceUpdateVersionCheck) {
        		if ((newValue instanceof Integer32) && (((Integer32) newValue).getValue() == 0)) {
                    bean.forceUpdateVersionCheck();
                }
        	} else {
        		super.setValue(column, newValue);
        	}
        }
    }
}
