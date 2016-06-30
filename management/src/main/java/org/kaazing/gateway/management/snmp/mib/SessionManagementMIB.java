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
import org.kaazing.gateway.management.service.ServiceManagementBean;
import org.kaazing.gateway.management.session.SessionManagementBean;
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
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.Variable;

/**
 * MIB support for Kaazing-session-level dynamic data.
 * <p/>
 * Kaazing's SNMP support is based on the SNMP4J open-source library under the Apache 2.0 license. To see the full text of the
 * license, please see the Kaazing third-party licenses file.
 */
public class SessionManagementMIB implements MOGroup, CounterListener, AgentCapabilityList {
    private final ManagementContext managementContext;

    // void close();
    // void closeImmediately();
    // long getId();
    // long getReadBytes();
    // double getReadBytesThroughput();
    // long getWrittenBytes();
    // double getWrittenBytesThroughput();

    private DefaultMOTable sysOREntry;
    private DefaultMOMutableTableModel sysOREntryModel;

    private MOTableSubIndex[] sessionEntryIndexes;
    private MOTableIndex sessionEntryIndex;
    private MOTable sessionEntry;
    private MOTableModel sessionEntryModel;
    private MOScalar summaryDataFields;
    private MOScalar summaryDataNotificationInterval;

    public SessionManagementMIB(ManagementContext managementContext, MOFactory factory) {
        this.managementContext = managementContext;
        createMO(factory);
    }

    private void createMO(MOFactory moFactory) {
        // Index definition
        OID serviceConfigEntryIndexOID = ((OID) MIBConstants.oidSessionEntry.clone()).append(1);
        sessionEntryIndexes =
                new MOTableSubIndex[]{
                        moFactory.createSubIndex(serviceConfigEntryIndexOID,
                                SMIConstants.SYNTAX_INTEGER, 1, 1),
                };

        sessionEntryIndex =
                moFactory.createIndex(sessionEntryIndexes, true);

        // Columns
        MOColumn[] sessionEntryColumns = new MOColumn[MIBConstants.SESSION_COLUMN_COUNT];
        sessionEntryColumns[MIBConstants.indexSessionIndex] =
                new MOMutableColumn(MIBConstants.colSessionIndex,
                        SMIConstants.SYNTAX_INTEGER32,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        sessionEntryColumns[MIBConstants.indexSessionId] =
                new MOMutableColumn(MIBConstants.colSessionId,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        sessionEntryColumns[MIBConstants.indexSessionReadBytes] =
                new MOMutableColumn(MIBConstants.colSessionReadBytes,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        sessionEntryColumns[MIBConstants.indexSessionReadBytesThroughput] =
                new MOMutableColumn(MIBConstants.colSessionReadBytesThroughput,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        sessionEntryColumns[MIBConstants.indexSessionWrittenBytes] =
                new MOMutableColumn(MIBConstants.colSessionWrittenBytes,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        sessionEntryColumns[MIBConstants.indexSessionWrittenBytesThroughput] =
                new MOMutableColumn(MIBConstants.colSessionWrittenBytesThroughput,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        sessionEntryColumns[MIBConstants.indexSessionCloseSession] =
                new MOMutableColumn(MIBConstants.colSessionCloseSession,
                        SMIConstants.SYNTAX_INTEGER,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_WRITE));
        sessionEntryColumns[MIBConstants.indexSessionEnableNotifications] =
                new MOMutableColumn(MIBConstants.colSessionEnableNotifications,
                        SMIConstants.SYNTAX_INTEGER,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_WRITE));
        sessionEntryColumns[MIBConstants.indexSessionCreateTime] =
                new MOMutableColumn(MIBConstants.colSessionCreateTime,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        sessionEntryColumns[MIBConstants.indexSessionRemoteAddress] =
                new MOMutableColumn(MIBConstants.colSessionRemoteAddress,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        sessionEntryColumns[MIBConstants.indexSessionPrincipals] =
                new MOMutableColumn(MIBConstants.colSessionPrincipals,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        sessionEntryColumns[MIBConstants.indexSessionSessionTypeName] =
                new MOMutableColumn(MIBConstants.colSessionSessionTypeName,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        sessionEntryColumns[MIBConstants.indexSessionSessionDirection] =
                new MOMutableColumn(MIBConstants.colSessionSessionDirection,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));
        sessionEntryColumns[MIBConstants.indexSessionSummaryData] =
                new MOMutableColumn(MIBConstants.colSessionSummaryData,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // Table model
        sessionEntryModel = new SessionManagementTableModel();
        sessionEntry = moFactory.createTable(MIBConstants.oidSessionEntry,
                sessionEntryIndex,
                sessionEntryColumns,
                sessionEntryModel);

        try {
            JSONArray jsonArray = new JSONArray(SessionManagementBean.SUMMARY_DATA_FIELD_LIST);
            summaryDataFields = new MOScalar(MIBConstants.oidSessionSummaryDataFields,
                    moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ),
                    new OctetString(jsonArray.toString()));
        } catch (JSONException ex) {
            // Should not be possible to get here, since the list of
            // strings is valid and constant.
        }

        summaryDataNotificationInterval = new SummaryDataIntervalMO(moFactory,
                managementContext.getSessionSummaryDataNotificationInterval(),
                MIBConstants.oidSessionSummaryDataNotificationInterval);
    }

    @Override
    public void registerMOs(MOServer server, OctetString context) throws DuplicateRegistrationException {
        server.register(sessionEntry, context);
        server.register(summaryDataFields, context);
        server.register(summaryDataNotificationInterval, context);
    }

    @Override
    public void unregisterMOs(MOServer server, OctetString context) {
        server.unregister(sessionEntry, context);
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

    public OID addSessionBean(SessionManagementBean bean) {
        ServiceManagementBean serviceBean = bean.getServiceManagementBean();
        GatewayManagementBean gatewayBean = serviceBean.getGatewayManagementBean();

        OID sessionIndexOID = new OID(new int[]{gatewayBean.getId(), serviceBean.getId(), (int) bean.getId()});
        sessionEntry.addRow(new SessionEntryRow(sessionIndexOID, bean));

        return sessionIndexOID;
    }

    public void removeSessionBean(OID oid) {
        sessionEntry.removeRow(oid);
    }

    private class SessionManagementTableModel extends DefaultMOMutableTableModel {
    }

    private final class SessionEntryRow extends DefaultMOMutableRow2PC {
        private SessionManagementBean bean;

        private SessionEntryRow(OID index, SessionManagementBean bean) {
            super(index, null);
            this.bean = bean;
        }

        @Override
        public int size() {
            return MIBConstants.SESSION_COLUMN_COUNT;
        }

        @Override
        public Variable getValue(int column) {
            Object scalarValue = null;
            try {
                switch (column) {
                    case MIBConstants.indexSessionIndex:
                        return new Integer32(getIndex().last());
                    case MIBConstants.indexSessionId:
                        scalarValue = bean.getId();
                        break;
                    case MIBConstants.indexSessionReadBytes:
                        scalarValue = bean.getReadBytes();
                        break;
                    case MIBConstants.indexSessionReadBytesThroughput:
                        scalarValue = bean.getReadBytesThroughput();
                        break;
                    case MIBConstants.indexSessionWrittenBytes:
                        scalarValue = bean.getWrittenBytes();
                        break;
                    case MIBConstants.indexSessionWrittenBytesThroughput:
                        scalarValue = bean.getWrittenBytesThroughput();
                        break;
                    case MIBConstants.indexSessionCloseSession:
                        return new Integer32(1);
                    case MIBConstants.indexSessionEnableNotifications:
                        return new Integer32(bean.areNotificationsEnabled() ? 1 : 0);
                    case MIBConstants.indexSessionCreateTime:
                        scalarValue = bean.getCreateTime();
                        break;
                    case MIBConstants.indexSessionRemoteAddress:
                        return Utils.stringToVariable(bean.getRemoteAddress());
                    case MIBConstants.indexSessionPrincipals:
                        return Utils.stringToVariable(bean.getUserPrincipals());
                    case MIBConstants.indexSessionSessionTypeName:
                        return Utils.stringToVariable(bean.getSessionTypeName());
                    case MIBConstants.indexSessionSessionDirection:
                        return Utils.stringToVariable(bean.getSessionDirection());
                    case MIBConstants.indexSessionSummaryData:
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

            long counterValue;
            if (scalarValue instanceof Double) {
                counterValue = (long) ((Double) scalarValue * 1000);
            } else if (scalarValue instanceof Float) {
                counterValue = (long) ((Float) scalarValue * 1000);
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

        @Override
        public void setValue(int column, Variable newValue) {
            switch (column) {
                case MIBConstants.indexSessionCloseSession:
                    if ((newValue instanceof Integer32) && (((Integer32) newValue).getValue() == 0)) {
                        bean.close();
                        removeSessionBean(getIndex());
                    }
                    break;
                case MIBConstants.indexSessionEnableNotifications:
                    if (newValue instanceof Integer32) {
                        bean.enableNotifications(((Integer32) newValue).getValue() == 1);
                    }
                    break;
//            case MIBConstants.indexSessionSummaryDataInterval:
//                if ((newValue instanceof Integer32) && (((Integer32)newValue).getValue() > 0)) {
//                    bean.setSummaryDataInterval(((Integer32)newValue).getValue());
//                }
//                break;
                default:
                    super.setValue(column, newValue);
                    break;
            }
        }
    }
}
