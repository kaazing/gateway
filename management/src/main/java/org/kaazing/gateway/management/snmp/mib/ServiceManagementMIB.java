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

import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.kaazing.gateway.management.Utils;
import org.kaazing.gateway.management.context.ManagementContext;
import org.kaazing.gateway.management.gateway.GatewayManagementBean;
import org.kaazing.gateway.management.service.ServiceManagementBean;
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
 * MIB support for Service-level dynamic data.
 * <p/>
 * Kaazing's SNMP support is based on the SNMP4J open-source library under the Apache 2.0 license. To see the full text of the
 * license, please see the Kaazing third-party licenses file.
 */
public class ServiceManagementMIB implements MOGroup, CounterListener, AgentCapabilityList {

    private static final int STATE_STOPPED = 0;
    private static final int STATE_RUNNING = 1;
    private static final int STATE_STOP_REQUESTED = 2;
    private static final int STATE_START_REQUESTED = 3;
    private static final int STATE_RESTART_REQUESTED = 4;

    private final ManagementContext managementContext;

    // long getNumberOfSessionsWithExceptions(); TODO
    // void clearCumulativeSessionsCount(); TODO

    private DefaultMOTable sysOREntry;
    private DefaultMOMutableTableModel sysOREntryModel;

    private MOTableSubIndex[] serviceEntryIndexes;
    private MOTableIndex serviceEntryIndex;
    private MOTable serviceEntry;
    private MOTableModel serviceEntryModel;

    private MOScalar summaryDataFields;
    private MOScalar summaryDataNotificationInterval;

    public ServiceManagementMIB(ManagementContext managementContext, MOFactory factory) {
        this.managementContext = managementContext;
        createMO(factory);
    }

    private void createMO(MOFactory moFactory) {
        // Index definition
        OID serviceConfigEntryIndexOID = ((OID) MIBConstants.oidServiceEntry.clone()).append(1);
        serviceEntryIndexes =
                new MOTableSubIndex[]{
                        moFactory.createSubIndex(serviceConfigEntryIndexOID,
                                SMIConstants.SYNTAX_INTEGER, 1, 1),
                };

        serviceEntryIndex =
                moFactory.createIndex(serviceEntryIndexes, true);

        // Columns
        MOColumn[] serviceEntryColumns = new MOColumn[MIBConstants.SERVICE_COLUMN_COUNT];
        serviceEntryColumns[MIBConstants.indexServiceIndex] =
                new MOMutableColumn(MIBConstants.colServiceIndex,
                        SMIConstants.SYNTAX_INTEGER,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // state [running, stopped, stop requested, restart requested, start requested]
        serviceEntryColumns[MIBConstants.indexServiceState] =
                new MOMutableColumn(MIBConstants.colServiceState,
                        SMIConstants.SYNTAX_INTEGER,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_WRITE));

        // boolean value (true == yes) whether or not the service can reach the connect (FIXME:  currently string as there is
        // no boolean in SNMP)
        serviceEntryColumns[MIBConstants.indexServiceConnected] =
                new MOMutableColumn(MIBConstants.colServiceConnected,
                        SMIConstants.SYNTAX_INTEGER32,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        serviceEntryColumns[MIBConstants.indexServiceBytesReceivedCount] =
                new MOMutableColumn(MIBConstants.colServiceBytesReceivedCount,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        serviceEntryColumns[MIBConstants.indexServiceBytesSentCount] =
                new MOMutableColumn(MIBConstants.colServiceBytesSentCount,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        serviceEntryColumns[MIBConstants.indexServiceCurrentSessionCount] =
                new MOMutableColumn(MIBConstants.colServiceCurrentSessionCount,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // number of current native websocket sessions
        serviceEntryColumns[MIBConstants.indexServiceCurrentNativeSessionCount] =
                new MOMutableColumn(MIBConstants.colServiceCurrentNativeSessionCount,
                        SMIConstants.SYNTAX_INTEGER,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_WRITE));

        // number of current emulated websocket sessions
        serviceEntryColumns[MIBConstants.indexServiceCurrentEmulatedSessionCount] =
                new MOMutableColumn(MIBConstants.colServiceCurrentEmulatedSessionCount,
                        SMIConstants.SYNTAX_INTEGER,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_WRITE));

        serviceEntryColumns[MIBConstants.indexServiceTotalSessionCount] =
                new MOMutableColumn(MIBConstants.colServiceTotalSessionCount,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // number of cumulative native websocket sessions
        serviceEntryColumns[MIBConstants.indexServiceTotalNativeSessionCount] =
                new MOMutableColumn(MIBConstants.colServiceTotalNativeSessionCount,
                        SMIConstants.SYNTAX_INTEGER,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_WRITE));

        // number of cumulative emulated websocket sessions
        serviceEntryColumns[MIBConstants.indexServiceTotalEmulatedSessionCount] =
                new MOMutableColumn(MIBConstants.colServiceTotalEmulatedSessionCount,
                        SMIConstants.SYNTAX_INTEGER,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_WRITE));

        // number of cumulative emulated websocket sessions
        serviceEntryColumns[MIBConstants.indexServiceTotalExceptionCount] =
                new MOMutableColumn(MIBConstants.colServiceTotalExceptionCount,
                        SMIConstants.SYNTAX_INTEGER,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_WRITE));

        // latest exception to occur for a session in the service
        serviceEntryColumns[MIBConstants.indexServiceLatestException] =
                new MOMutableColumn(MIBConstants.colServiceLatestException,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // time of latest exception, sent over as MS (long)
        serviceEntryColumns[MIBConstants.indexServiceLatestExceptionTime] =
                new MOMutableColumn(MIBConstants.colServiceLatestExceptionTime,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // timestamp of the last successful connection
        serviceEntryColumns[MIBConstants.indexServiceLastSuccessfulConnectTime] =
                new MOMutableColumn(MIBConstants.colServiceLastSuccessfulConnectTime,
                        SMIConstants.SYNTAX_COUNTER64, // FIXME: date string instead of timestamp?
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // timestamp of the last failed connection
        serviceEntryColumns[MIBConstants.indexServiceLastFailedConnectTime] =
                new MOMutableColumn(MIBConstants.colServiceLastFailedConnectTime,
                        SMIConstants.SYNTAX_COUNTER64, // FIXME: date string instead of timestamp?
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // boolean value (true == success) of last heartbeat ping (FIXME:  currently string as there is no boolean in SNMP)
        serviceEntryColumns[MIBConstants.indexServiceLastHeartbeatPingResult] =
                new MOMutableColumn(MIBConstants.colServiceLastHeartbeatPingResult,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // timestamp of last heartbeat ping
        serviceEntryColumns[MIBConstants.indexServiceLastHeartbeatPingTimestamp] =
                new MOMutableColumn(MIBConstants.colServiceLastHeartbeatPingTimestamp,
                        SMIConstants.SYNTAX_COUNTER64,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // number of times the heartbeat has pinged the connect
        serviceEntryColumns[MIBConstants.indexServiceHeartbeatPingCount] =
                new MOMutableColumn(MIBConstants.colServiceHeartbeatPingCount,
                        SMIConstants.SYNTAX_INTEGER32,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // number of times the heartbeat has successfully pinged the connect
        serviceEntryColumns[MIBConstants.indexServiceHeartbeatPingSuccessesCount] =
                new MOMutableColumn(MIBConstants.colServiceHeartbeatPingSuccessesCount,
                        SMIConstants.SYNTAX_INTEGER32,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // number of times the heartbeat has failed to pinged the connect
        serviceEntryColumns[MIBConstants.indexServiceHeartbeatPingFailuresCount] =
                new MOMutableColumn(MIBConstants.colServiceHeartbeatPingFailuresCount,
                        SMIConstants.SYNTAX_INTEGER32,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // boolean value (true == yes) whether or not the heartbeat is running (FIXME:  currently string as there is no
        // boolean in SNMP)
        serviceEntryColumns[MIBConstants.indexServiceHeartbeatRunning] =
                new MOMutableColumn(MIBConstants.colServiceHeartbeatRunning,
                        SMIConstants.SYNTAX_INTEGER32,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // whether or not notifications are enabled for the service (1==yes, 0==no)
        serviceEntryColumns[MIBConstants.indexServiceEnableNotifications] =
                new MOMutableColumn(MIBConstants.colServiceEnableNotifications,
                        SMIConstants.SYNTAX_INTEGER32,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_WRITE));

        // logged in sessions
        serviceEntryColumns[MIBConstants.indexServiceLoggedInSessions] =
                new MOMutableColumn(MIBConstants.colServiceLoggedInSessions,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        serviceEntryColumns[MIBConstants.indexServiceSummaryData] =
                new MOMutableColumn(MIBConstants.colServiceSummaryData,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // Table model
        serviceEntryModel = new ServiceBeanTableModel();
        serviceEntry = moFactory.createTable(MIBConstants.oidServiceEntry,
                serviceEntryIndex,
                serviceEntryColumns,
                serviceEntryModel);

        try {
            JSONArray jsonArray = new JSONArray(ServiceManagementBean.SUMMARY_DATA_FIELD_LIST);
            summaryDataFields = new MOScalar(MIBConstants.oidServiceSummaryDataFields,
                    moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ),
                    new OctetString(jsonArray.toString()));
        } catch (JSONException ex) {
            // Should not be possible to get here, since the list of
            // strings is valid and constant.
        }

        summaryDataNotificationInterval = new SummaryDataIntervalMO(moFactory,
                managementContext.getServiceSummaryDataNotificationInterval(),
                MIBConstants.oidServiceSummaryDataNotificationInterval);
    }

    @Override
    public void registerMOs(MOServer server, OctetString context) throws DuplicateRegistrationException {
        server.register(serviceEntry, context);
        server.register(summaryDataFields, context);
        server.register(summaryDataNotificationInterval, context);
    }

    @Override
    public void unregisterMOs(MOServer server, OctetString context) {
        server.unregister(serviceEntry, context);
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

    public OID addServiceBean(ServiceManagementBean bean) {
        // Note: if services are appearing and disappearing, we cannot just
        // have a service index as the current number of rows in the table,
        // because earlier service entries may be gone.
        GatewayManagementBean gatewayBean = bean.getGatewayManagementBean();
        OID serviceIndexOID = new OID(new int[]{gatewayBean.getId(), bean.getId()});
        serviceEntry.addRow(new ServiceEntryRow(serviceIndexOID, bean));

        return serviceIndexOID;
    }

    public void removeServiceBean(OID oid) {
        serviceEntry.removeRow(oid);
    }

    private class ServiceBeanTableModel extends DefaultMOMutableTableModel {
    }

    private final class ServiceEntryRow extends DefaultMOMutableRow2PC {
        private ServiceManagementBean bean;
        private int status;

        private ServiceEntryRow(OID index, ServiceManagementBean bean) {
            super(index, null);
            this.bean = bean;
            status = STATE_RUNNING;
        }

        @Override
        public int size() {
            return MIBConstants.SERVICE_COLUMN_COUNT;
        }

        @Override
        public Variable getValue(int column) {
            long scalarValue = 0;

            try {
                switch (column) {
                    case MIBConstants.indexServiceIndex:
                        return new Integer32(getIndex().last());
                    case MIBConstants.indexServiceState:
                        return new Integer32(status);
                    case MIBConstants.indexServiceConnected:
                        return new Integer32(bean.isServiceConnected() ? 1 : 0);
                    case MIBConstants.indexServiceBytesReceivedCount:
                        scalarValue = bean.getTotalBytesReceivedCount();
                        break;
                    case MIBConstants.indexServiceBytesSentCount:
                        scalarValue = bean.getTotalBytesSentCount();
                        break;
                    case MIBConstants.indexServiceCurrentSessionCount:
                        scalarValue = bean.getCurrentSessionCount();
                        break;
                    case MIBConstants.indexServiceCurrentNativeSessionCount:
                        scalarValue = bean.getCurrentNativeSessionCount();
                        break;
                    case MIBConstants.indexServiceCurrentEmulatedSessionCount:
                        scalarValue = bean.getCurrentEmulatedSessionCount();
                        break;
                    case MIBConstants.indexServiceTotalSessionCount:
                        scalarValue = bean.getCumulativeSessionCount();
                        break;
                    case MIBConstants.indexServiceTotalNativeSessionCount:
                        scalarValue = bean.getCumulativeNativeSessionCount();
                        break;
                    case MIBConstants.indexServiceTotalEmulatedSessionCount:
                        scalarValue = bean.getCumulativeEmulatedSessionCount();
                        break;
                    case MIBConstants.indexServiceTotalExceptionCount:
                        scalarValue = bean.getExceptionCount();
                        break;
                    case MIBConstants.indexServiceLatestException:
                        return Utils.stringToVariable(bean.getLatestException());
                    case MIBConstants.indexServiceLatestExceptionTime:
                        scalarValue = bean.getLatestExceptionTime();
                        break;
                    case MIBConstants.indexServiceLastSuccessfulConnectTime:
                        scalarValue = bean.getLastSuccessfulConnectTime();
                        break;
                    case MIBConstants.indexServiceLastFailedConnectTime:
                        scalarValue = bean.getLastFailedConnectTime();
                        break;
                    case MIBConstants.indexServiceLastHeartbeatPingResult:
                        return bean.getLastHeartbeatPingResult()
                                ? new OctetString("Successfully pinged service connects")
                                : new OctetString("Failed to ping service connects");
                    case MIBConstants.indexServiceLastHeartbeatPingTimestamp:
                        scalarValue = bean.getLastHeartbeatPingTimestamp();
                        break;
                    case MIBConstants.indexServiceHeartbeatPingCount:
                        return new Integer32(bean.getHeartbeatPingCount());
                    case MIBConstants.indexServiceHeartbeatPingSuccessesCount:
                        return new Integer32(bean.getHeartbeatPingSuccessesCount());
                    case MIBConstants.indexServiceHeartbeatPingFailuresCount:
                        return new Integer32(bean.getHeartbeatPingFailuresCount());
                    case MIBConstants.indexServiceHeartbeatRunning:
                        return new Integer32(bean.isHeartbeatRunning() ? 1 : 0);
                    case MIBConstants.indexServiceEnableNotifications:
                        return new Integer32(bean.areNotificationsEnabled() ? 1 : 0);
                    case MIBConstants.indexServiceLoggedInSessions:
                        return Utils.stringToVariable(getLoggedInSessionsData());
                    case MIBConstants.indexServiceSummaryData:
                        return Utils.stringToVariable(bean.getSummaryData());
                    default:
                        return super.getValue(column);
                }
            } catch (Exception ex) {
                // FIXME:  handle errors
            }
            return new Counter64(scalarValue);
        }

        @Override
        public void commit(SubRequest subRequest, MOTableRow changeSet, int column) {
            setValue(column, (Variable) subRequest.getVariableBinding().getVariable().clone());
            subRequest.completed();
        }

        @Override
        public void setValue(int column, Variable newValue) {
            switch (column) {
                case MIBConstants.indexServiceState:
                    if (newValue instanceof Integer32) {
                        int newState = ((Integer32) newValue).getValue();

                        // FIXME:  We need a real state machine here where we listen for some
                        //         kind of event when the service stops/starts/quiesces/etc.
                        //         The value of the STATUS column should be updated by this state
                        //         machine and the next phase should be validated based on current
                        //         state.
                        try {
                            switch (newState) {
                                case STATE_START_REQUESTED:
                                    bean.start();
                                    status = STATE_RUNNING;
                                    break;
                                case STATE_STOP_REQUESTED:
                                    bean.stop();
                                    status = STATE_STOPPED;
                                    break;
                                case STATE_RESTART_REQUESTED:
                                    bean.restart();
                                    status = STATE_RUNNING;
                                    break;
                            }
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                    break;
                case MIBConstants.indexServiceEnableNotifications:
                    if (newValue instanceof Integer32) {
                        bean.enableNotifications(((Integer32) newValue).getValue() == 1);
                    }
                    break;
//            case idxSummaryDataInterval:
//                if ((newValue instanceof Integer32) && (((Integer32)newValue).getValue() > 0)) {
//                    bean.setSummaryDataInterval(((Integer32)newValue).getValue());
//                }
//                break;
                default:
                    super.setValue(column, newValue);
                    break;
            }
        }

        private String getLoggedInSessionsData() {
            Map<Long, Map<String, String>> loggedInSessionMap = bean.getLoggedInSessions();

            StringBuffer sb = new StringBuffer();
            sb.append("{ ");
            for (Long sessionId : loggedInSessionMap.keySet()) {
                sb.append(sessionId.toString());
                sb.append(" : [ ");
                Map<String, String> principalMap = loggedInSessionMap.get(sessionId);
                for (String principalName : principalMap.keySet()) {
                    String principalClass = principalMap.get(principalName);
                    sb.append(principalName + " : " + principalClass + ", ");
                }

                // delete trailing space, then trailing comma
                sb.deleteCharAt(sb.length() - 1);
                sb.deleteCharAt(sb.length() - 1);

                // close principal map
                sb.append(" ], ");
            }

            if (sb.length() > 2) {
                // delete trailing space, then trailing comma
                sb.deleteCharAt(sb.length() - 1);
                sb.deleteCharAt(sb.length() - 1);
            }

            // close session map
            sb.append(" }");

            return sb.toString();
        }
    }
}
