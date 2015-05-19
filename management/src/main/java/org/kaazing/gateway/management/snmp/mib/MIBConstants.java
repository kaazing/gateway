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

import org.snmp4j.smi.OID;

/**
 * Constants referenced in other parts of the SNMP management service.
 * <p/>
 * Kaazing's SNMP support is based on the SNMP4J open-source library under the Apache 2.0 license. To see the full text of the
 * license, please see the Kaazing third-party licenses file.
 */
public class MIBConstants {

    protected MIBConstants() {
    }

    public static int KAAZING_ENTERPRISE_ID = 29197;

    // OID hierarchy
    //     1.3.6.1.4.1 --- iso.identified_organization.dod.internet.private.enterprise
    //                .29197 --- Kaazing Enterprise ID
    //                      .5 --- GatewayConfiguration root
    //                        .1 --- Cluster
    //                        .2 --- Network table
    //                        .3 --- Security
    //                          .9 --- Realm table
    //                        .4 --- Service table
    //                        .5 --- Service Defaults table???? TODO
    //                        .6 --- Resources table???? TODO

    // Gateway config root
    public static final OID oidGatewayConfiguration =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 1});

    public static final OID oidClusterConfig =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 1, 1});
    public static final OID oidClusterName =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 1, 1, 1, 0});
    public static final OID oidClusterAccepts =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 1, 1, 2, 0});
    public static final OID oidClusterConnects =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 1, 1, 3, 0});
    public static final OID oidClusterConnectOptions =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 1, 1, 4, 0});

    public static final OID oidNetworkConfig =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 1, 2});
    public static final OID oidNetworkConfigAddressMappings =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 1, 2, 1, 0});

    public static final OID oidSecurityConfig =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 1, 3});
    public static final OID oidSecurityKeystoreType =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 1, 3, 1, 0});
    public static final OID oidSecurityKeystoreCertificateInfo =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 1, 3, 2, 0});
    public static final OID oidSecurityTruststoreType =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 1, 3, 3, 0});
    public static final OID oidSecurityTruststoreCertificateInfo =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 1, 3, 4, 0});

    public static final OID oidRealmConfig =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 1, 3, 5});

    public static final int REALM_COLUMN_COUNT = 10;
    // columns
    public static final int realmConfigName = 1;
    public static final int realmConfigDescription = 2;
    public static final int realmConfigUserPrincipalClasses = 3;
    public static final int realmConfigHttpChallengeScheme = 4;
    public static final int realmConfigHttpHeaders = 5;
    public static final int realmConfigQueryParams = 6;
    public static final int realmConfigCookieNames = 7;
    public static final int realmConfigAuthorizationMode = 8;
    public static final int realmConfigSessionTimeout = 9;
    public static final int realmConfigLoginModules = 10;

    // indicies (subtract 1 from column ordinal)
    public static final int realmConfigNameIndex = 0;
    public static final int realmConfigDescriptionIndex = 1;
    public static final int realmConfigUserPrincipalClassesIndex = 2;
    public static final int realmConfigHttpChallengeSchemeIndex = 3;
    public static final int realmConfigHttpHeadersIndex = 4;
    public static final int realmConfigQueryParamsIndex = 5;
    public static final int realmConfigCookieNamesIndex = 6;
    public static final int realmConfigAuthorizationModeIndex = 7;
    public static final int realmConfigSessionTimeoutIndex = 8;
    public static final int realmConfigLoginModulesIndex = 9;

    public static final OID oidServiceConfig =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 1, 4});

    public static final int SERVICE_CONFIG_COLUMN_COUNT = 13;

    // columns
    public static final int serviceConfigType = 1; // type
    public static final int serviceConfigName = 2; // name
    public static final int serviceConfigDescription = 3; // description
    public static final int serviceConfigAccepts = 4; // accepts
    public static final int serviceConfigAcceptOptions = 5; // accept options
    public static final int serviceConfigBalances = 6; // balances
    public static final int serviceConfigConnects = 7; // connects
    public static final int serviceConfigConnectOptions = 8; // connect options
    public static final int serviceConfigCrossSiteConstraints = 9; // cross-site-constraints
    public static final int serviceConfigProperties = 10; // properties
    public static final int serviceConfigRequiredRoles = 11; // required roles
    public static final int serviceConfigRealm = 12; // realm
    public static final int serviceConfigMimeMappings = 13; // mime mappings

    // indicies (subtract 1 from column ordinal)
    public static final int serviceConfigTypeIndex = 0; // type
    public static final int serviceConfigNameIndex = 1; // name
    public static final int serviceConfigDescriptionIndex = 2; // description
    public static final int serviceConfigAcceptsIndex = 3; // accepts
    public static final int serviceConfigAcceptOptionsIndex = 4; // accept options
    public static final int serviceConfigBalancesIndex = 5; // balances
    public static final int serviceConfigConnectsIndex = 6; // connects
    public static final int serviceConfigConnectOptionsIndex = 7; // connect options
    public static final int serviceConfigCrossSiteConstraintsIndex = 8; // cross-site-constraints
    public static final int serviceConfigPropertiesIndex = 9; // properties
    public static final int serviceConfigRequiredRolesIndex = 10; // required roles
    public static final int serviceConfigRealmIndex = 11; // realm
    public static final int serviceConfigMimeMappingsIndex = 12; // mime mappings

    public static final OID oidServiceDefaults =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 1, 5});
    public static final OID oidServiceDefaultsAcceptOptions =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 1, 5, 1, 0});
    public static final OID oidServiceDefaultsMimeMappings =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 1, 5, 2, 0});
    // TODO: add connect-options to service-defaults
//    public static final OID oidServiceDefaultsConnectOptions =
//            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 1, 5, 3, 0});

    public static final OID oidVersionInfo =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 1, 6});
    public static final OID oidVersionInfoProductTitle =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 1, 6, 1, 0});
    public static final OID oidVersionInfoProductBuild =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 1, 6, 2, 0});
    public static final OID oidVersionInfoProductEdition =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 1, 6, 3, 0});

    // OID hierarchy
    //     1.3.6.1.4.1 --- iso.identified_organization.dod.internet.private.enterprise
    //                .29197 --- the real Kaazing Enterprise ID
    //                      .2 --- Gateway table
    //                        .1 --- gateway entry
    // Tables
    public static final OID oidGatewayEntry =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 2, 1});

    public static final int GATEWAY_COLUMN_COUNT = 14;

    // Column sub-identifier definitions for gatewayEntry:
    public static final int colGatewayIndex = 1; // index
    public static final int colGatewayId = 2; // id
    public static final int colTotalCurrentSessions = 3; // the total number of open sessions across all services
    public static final int colTotalBytesReceived = 4; // the total number of bytes received across all services
    public static final int colTotalBytesSent = 5; // the total number of bytes sent across all services
    public static final int colUptime = 6;
    public static final int colStartTime = 7;
    public static final int colInstanceKey = 8;  // random, one per gateway instance.
    public static final int colGatewaySummaryData = 9;
    public static final int colClusterMembers = 10;
    public static final int colBalancerMap = 11;
    public static final int colManagementServiceMap = 12;
    public static final int colLatestUpdateableVersion = 13;
    public static final int colForceUpdateVersionCheck = 14;

    // index sub-identifier definitions for gatewayEntry (subtract 1 from column identifier):
    public static final int indexGatewayIndex = 0; // index
    public static final int indexGatewayId = 1; // id
    public static final int indexTotalCurrentSessions = 2; // the total number of open sessions across all services
    public static final int indexTotalBytesReceived = 3; // the total number of bytes received across all services
    public static final int indexTotalBytesSent = 4; // the total number of bytes sent across all services
    public static final int indexUptime = 5;
    public static final int indexStartTime = 6;
    public static final int indexInstanceKey = 7;
    public static final int indexGatewaySummaryData = 8;
    public static final int indexClusterMembers = 9;
    public static final int indexBalancerMap = 10;
    public static final int indexManagementServiceMap = 11;
    public static final int indexLatestUpdateableVersion = 12;
    public static final int indexForceUpdateVersionCheck = 13;

    public static final OID oidGatewaySummaryDataFields =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 2, 2, 0});
    public static final OID oidGatewaySummaryDataNotificationInterval =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 2, 3, 0});
    public static final OID oidGatewaySummaryDataEvent =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 2, 4});
    public static final OID oidClusterMembershipEvent =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 2, 5});
    public static final OID oidClusterMembershipEventType =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 2, 6, 0});
    public static final OID oidClusterMembershipEventInstanceKey =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 2, 7, 0});
    public static final OID oidClusterManagementServiceEvent =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 2, 8});
    public static final OID oidClusterManagementServiceEventType =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 2, 9, 0});
    public static final OID oidClusterManagementServiceEventURIs =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 2, 10, 0});
    public static final OID oidClusterBalancerMapEvent =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 2, 11});
    public static final OID oidClusterBalancerMapEventType =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 2, 12, 0});
    public static final OID oidClusterBalancerMapEventBalancerURI =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 2, 13, 0});
    public static final OID oidClusterBalancerMapEventBalanceeURIs =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 2, 14, 0});

    // OID hierarchy
    //     1.3.6.1.4.1 --- iso.identified_organization.dod.internet.private.enterprise
    //                .29197 --- Kaazing Enterprise ID
    //                      .3 --- Service table
    //                        .1 --- service entry

    // Tables
    public static final OID oidServiceEntry =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 3, 1});

    public static final int STATE_RUNNING = 1;
    public static final int STATE_STOPPED = 2;
    public static final int STATE_STOP_REQUESTED = 3;
    public static final int STATE_RESTART_REQUESTED = 4;
    public static final int STATE_START_REQUESTED = 5;

    public static final int SERVICE_COLUMN_COUNT = 25;

    // Column sub-identifer definitions for serviceEntry:
    public static final int colServiceIndex = 1;  // service index
    public static final int colServiceState = 2; // state [running, stopped, stop requested, restart requested, start requested]
    public static final int colServiceConnected = 3;
            // boolean value (true == yes) whether or not the service can reach the connect
    public static final int colServiceBytesReceivedCount = 4;  // total bytes received count
    public static final int colServiceBytesSentCount = 5;  // total bytes sent count
    public static final int colServiceCurrentSessionCount = 6;  // number of current sessions
    public static final int colServiceCurrentNativeSessionCount = 7;  // number of current native websocket sessions
    public static final int colServiceCurrentEmulatedSessionCount = 8;  // number of current emulated websocket sessions
    public static final int colServiceTotalSessionCount = 9;  // number of cumulative sessions
    public static final int colServiceTotalNativeSessionCount = 10;  // number of cumulative native websocket sessions
    public static final int colServiceTotalEmulatedSessionCount = 11;  // number of cumulative native websocket sessions
    public static final int colServiceTotalExceptionCount = 12; // total count of exceptions on the service
    public static final int colServiceLatestException = 13; // latest exception to occur for a session in the service
    public static final int colServiceLatestExceptionTime = 14; // time of latest exception to occur for a session in the service
    public static final int colServiceLastSuccessfulConnectTime = 15;  // timestamp of the last successful connection
    public static final int colServiceLastFailedConnectTime = 16;  // timestamp of the last failed connection
    public static final int colServiceLastHeartbeatPingResult = 17; // boolean value (true == success) of last heartbeat ping
    public static final int colServiceLastHeartbeatPingTimestamp = 18; // timestamp of last heartbeat ping
    public static final int colServiceHeartbeatPingCount = 19; // number of times the heartbeat has pinged the connect
    public static final int colServiceHeartbeatPingSuccessesCount = 20;
            // number of times the heartbeat has successfully pinged the connect
    public static final int colServiceHeartbeatPingFailuresCount = 21;
            // number of times the heartbeat has failed to ping the connect
    public static final int colServiceHeartbeatRunning = 22;
            // boolean value (true == yes) whether or not the heartbeat is running
    public static final int colServiceEnableNotifications = 23;
            // whether or not notifications are enabled for the service (1==yes, 0==no)
    public static final int colServiceLoggedInSessions = 24; // logged in sessions
    public static final int colServiceSummaryData = 25;

    // index sub-identifer definitions for serviceEntry:
    public static final int indexServiceIndex = 0;  // service index
    public static final int indexServiceState = 1;
            // state [running, stopped, stop requested, restart requested, start requested]
    public static final int indexServiceConnected = 2;
            // boolean value (true == yes) whether or not the service can reach the connect
    public static final int indexServiceBytesReceivedCount = 3;  // total bytes received count
    public static final int indexServiceBytesSentCount = 4;  // total bytes sent count
    public static final int indexServiceCurrentSessionCount = 5;  // number of current sessions
    public static final int indexServiceCurrentNativeSessionCount = 6;  // number of current native websocket sessions
    public static final int indexServiceCurrentEmulatedSessionCount = 7;  // number of current emulated websocket sessions
    public static final int indexServiceTotalSessionCount = 8;  // number of cumulative sessions
    public static final int indexServiceTotalNativeSessionCount = 9;  // number of cumulative native websocket sessions
    public static final int indexServiceTotalEmulatedSessionCount = 10;  // number of cumulative native websocket sessions
    public static final int indexServiceTotalExceptionCount = 11; // total count of exceptions on the service
    public static final int indexServiceLatestException = 12; // latest exception to occur for a session in the service
    public static final int indexServiceLatestExceptionTime = 13;
            // time of latest exception to occur for a session in the service
    public static final int indexServiceLastSuccessfulConnectTime = 14;  // timestamp of the last successful connection
    public static final int indexServiceLastFailedConnectTime = 15;  // timestamp of the last failed connection
    public static final int indexServiceLastHeartbeatPingResult = 16; // boolean value (true == success) of last heartbeat ping
    public static final int indexServiceLastHeartbeatPingTimestamp = 17; // timestamp of last heartbeat ping
    public static final int indexServiceHeartbeatPingCount = 18; // number of times the heartbeat has pinged the connect
    public static final int indexServiceHeartbeatPingSuccessesCount = 19;
            // number of times the heartbeat has successfully pinged the connect
    public static final int indexServiceHeartbeatPingFailuresCount = 20;
            // number of times the heartbeat has failed to ping the connect
    public static final int indexServiceHeartbeatRunning = 21;
            // boolean value (true == yes) whether or not the heartbeat is running
    public static final int indexServiceEnableNotifications = 22;
            // whether or not notifications are enabled for the service (1==yes, 0==no)
    public static final int indexServiceLoggedInSessions = 23; // logged in sessions
    public static final int indexServiceSummaryData = 24;

    public static final OID oidServiceSummaryDataFields =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 3, 2, 0});
    public static final OID oidServiceSummaryDataNotificationInterval =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 3, 3, 0});
    public static final OID oidServiceSummaryDataNotification =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 3, 4});
    public static final OID oidServiceConnectionNotification =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 3, 5});
    public static final OID oidServiceDisconnectionNotification =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 3, 6});

    // OID hierarchy
    //     1.3.6.1.4.1 --- iso.identified_organization.dod.internet.private.enterprise
    //                .29197 --- the real Kaazing Enterprise ID
    //                      .4 --- Session table
    //                        .1 --- session entry
    // Tables

    public static final OID oidSessionEntry =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 4, 1});

    // Index OID definitions
    public static final OID oidSessionEntryIndex1 =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 4, 1, 1});

    public static final int SESSION_COLUMN_COUNT = 14;

    // Column sub-identifier definitions for sessionEntry:
    public static final int colSessionIndex = 1; // index
    public static final int colSessionId = 2; // id
    public static final int colSessionReadBytes = 3; // read bytes
    public static final int colSessionReadBytesThroughput = 4; // read bytes throughput
    public static final int colSessionWrittenBytes = 5; // written bytes
    public static final int colSessionWrittenBytesThroughput = 6; // written bytes throughput
    public static final int colSessionCloseSession = 7; // open (value of 1 means open, value of 0 sets to closed)
    public static final int colSessionEnableNotifications = 8; // open (value of 1 means open, value of 0 sets to closed)
    public static final int colSessionCreateTime = 9; // timestamp of when the session was created
    public static final int colSessionRemoteAddress = 10; // address from where session originated
    public static final int colSessionPrincipals = 11; // principals from the user login for the session
    public static final int colSessionSessionTypeName = 12; // label string for the type of session (wsnative, etc.)
    public static final int colSessionSessionDirection = 13; // 'accept', 'connect', ???
    public static final int colSessionSummaryData = 14;

    // index sub-identifier definitions for sessionEntry:
    public static final int indexSessionIndex = 0; // index
    public static final int indexSessionId = 1; // id
    public static final int indexSessionReadBytes = 2; // read bytes
    public static final int indexSessionReadBytesThroughput = 3; // read bytes throughput
    public static final int indexSessionWrittenBytes = 4; // written bytes
    public static final int indexSessionWrittenBytesThroughput = 5; // written bytes throughput
    public static final int indexSessionCloseSession = 6; // open (value of 1 means open, value of 0 sets to closed)
    public static final int indexSessionEnableNotifications = 7; // open (value of 1 means open, value of 0 sets to closed)
    public static final int indexSessionCreateTime = 8; // timestamp of when the session was created
    public static final int indexSessionRemoteAddress = 9; // address from where session originated
    public static final int indexSessionPrincipals = 10; // principals from the user login for the session
    public static final int indexSessionSessionTypeName = 11; // label string for the type of connection (wsnative, etc.)
    public static final int indexSessionSessionDirection = 12; // 'accept', 'connect', ???
    public static final int indexSessionSummaryData = 13;

    public static final OID oidSessionSummaryDataFields =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 4, 2, 0});
    public static final OID oidSessionSummaryDataNotificationInterval =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 4, 3, 0});
    public static final OID oidSessionSummaryDataNotification =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 4, 4});
    public static final OID oidSessionMessageReceivedNotification =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 4, 5});
    public static final OID oidSessionFilterWriteNotification =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 4, 6});


    // OID hierarchy
    //     1.3.6.1.4.1 --- iso.identified_organization.dod.internet.private.enterprise
    //                .29197 --- the real Kaazing Enterprise ID
    //                      .5 --- System (no tables, no index needed)

    public static final OID oidSystemEntry =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 5});

    public static final OID oidSystemOsName =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 5, 1, 0});
    public static final OID oidSystemUptimeSeconds =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 5, 2, 0});
    public static final OID oidSystemTotalFreeMemory =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 5, 3, 0});
    public static final OID oidSystemTotalUsedMemory =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 5, 4, 0});
    public static final OID oidSystemTotalMemory =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 5, 5, 0});
    public static final OID oidSystemTotalFreeSwap =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 5, 6, 0});
    public static final OID oidSystemTotalUsedSwap =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 5, 7, 0});
    public static final OID oidSystemTotalSwap =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 5, 8, 0});
    public static final OID oidSystemCpuPercentage =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 5, 9, 0});


    public static final OID oidSystemSummaryDataFields =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 5, 30, 0});
    public static final OID oidSystemSummaryData =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 5, 31, 0});
    public static final OID oidSystemSummaryDataNotificationInterval =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 5, 32, 0});
    public static final OID oidSystemSummaryDataNotification =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 5, 33});
    public static final OID oidSystemSummaryDataGatherInterval =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 5, 34, 0});

    // Because we don't want to mess with the JVM MIB's OIDs, we'll declare
    // OIDs for the JVM summary information in the system MIB
    public static final OID oidJvmSummaryDataFields =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 5, 40, 0});
    public static final OID oidJvmSummaryData =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 5, 41, 0});
    public static final OID oidJvmSummaryDataNotificationInterval =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 5, 42, 0});
    public static final OID oidJvmSummaryDataNotification =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 5, 43});
    public static final OID oidJvmSummaryDataGatherInterval =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 5, 44, 0});


    // OID hierarchy
    //     1.3.6.1.4.1 --- iso.identified_organization.dod.internet.private.enterprise
    //                .29197 --- the real Kaazing Enterprise ID
    //                      .6 --- CPU table
    //                        .1 --- cpu entry
    //                           .1 -- entry index
    //                           .2 --- id, ...
    //                        .2 --- number of CPUs
    //                        .3 --- stringified JSON array of summary field names
    //                        .4 --- summaryData (across all CPUs at same time)
    //                        .5 --- summary data notification interval (for 6.4, not 6.1.12)
    //                        .6 --- summary data notification
    //                        .7 --- summary data gather interval
    // Tables

    public static final OID oidCpuListEntry =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 6, 1});
    public static final OID oidCpuListNumCpus =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 6, 2, 0});
    public static final OID oidCpuListSummaryDataFields =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 6, 3, 0});
    public static final OID oidCpuListSummaryData =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 6, 4, 0});
    public static final OID oidCpuListSummaryDataNotificationInterval =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 6, 5, 0});
    public static final OID oidCpuListSummaryDataNotification =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 6, 6});
    public static final OID oidCpuListSummaryDataGatherInterval =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 6, 7, 0});

    // Index OID definitions
    public static final OID oidCpuListEntryIndex1 =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 6, 1, 1});

    public static final int CPU_COLUMN_COUNT = 12;

    // Column sub-identifier definitions for sessionEntry:
    public static final int colCpuIndex = 1; // index
    public static final int colCpuId = 2; // id
    public static final int colCpuCombined = 3; // combined
    public static final int colCpuIdle = 4; // idle
    public static final int colCpuIrq = 5; // Irq
    public static final int colCpuNice = 6; // nice
    public static final int colCpuSoftIrq = 7; // softIrq
    public static final int colCpuStolen = 8; // stolen
    public static final int colCpuSys = 9; // sys
    public static final int colCpuUser = 10; // user
    public static final int colCpuWait = 11; // wait
    public static final int colCpuSummaryData = 12; // summaryData

    // index sub-identifier definitions for sessionEntry:
    public static final int indexCpuIndex = 0;
    public static final int indexCpuId = 1;
    public static final int indexCpuCombined = 2;
    public static final int indexCpuIdle = 3;
    public static final int indexCpuIrq = 4;
    public static final int indexCpuNice = 5;
    public static final int indexCpuSoftIrq = 6;
    public static final int indexCpuStolen = 7;
    public static final int indexCpuSys = 8;
    public static final int indexCpuUser = 9;
    public static final int indexCpuWait = 10;
    public static final int indexCpuSummaryData = 11;

    // OID hierarchy
    //     1.3.6.1.4.1 --- iso.identified_organization.dod.internet.private.enterprise
    //                .29197 --- the real Kaazing Enterprise ID
    //                      .7 --- NIC table
    //                        .1 --- NIC entry
    //                           .1 -- entry index
    //                           .2 --- id, ...
    //                        .2 --- stringified JSON array of NIC interface names
    //                        .3 --- stringified JSON array of summary field names
    //                        .4 --- summaryData (across all NICs at same time)
    //                        .5 --- summary data notification interval (for 7.4, not 7.1.12)
    //                        .6 --- summary data notification
    //                        .7 --- summary data gather interval

    public static final OID oidNicListEntry =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 7, 1});
    public static final OID oidNicListNetInterfaceNames =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 7, 2, 0});
    public static final OID oidNicListSummaryDataFields =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 7, 3, 0});
    public static final OID oidNicListSummaryData =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 7, 4, 0});
    public static final OID oidNicListSummaryDataNotificationInterval =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 7, 5, 0});
    public static final OID oidNicListSummaryDataNotification =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 7, 6});
    public static final OID oidNicListSummaryDataGatherInterval =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 7, 7, 0});

    // Index OID definitions
    public static final OID oidNicListEntryIndex1 =
            new OID(new int[]{1, 3, 6, 1, 4, 1, KAAZING_ENTERPRISE_ID, 7, 1, 1});

    public static final int NIC_COLUMN_COUNT = 12;

    // Column sub-identifier definitions for sessionEntry:
    public static final int colNicIndex = 1; // index
    public static final int colNicId = 2; // id
    public static final int colNicName = 3;
    public static final int colNicRxBytes = 4;
    public static final int colNicRxBytesPerSecond = 5;
    public static final int colNicRxDropped = 6;
    public static final int colNicRxErrors = 7;
    public static final int colNicTxBytes = 8;
    public static final int colNicTxBytesPerSecond = 9;
    public static final int colNicTxDropped = 10;
    public static final int colNicTxErrors = 11;
    public static final int colNicSummaryData = 12;

    // index sub-identifier definitions for sessionEntry:
    public static final int indexNicIndex = 0;
    public static final int indexNicId = 1;
    public static final int indexNicName = 2;
    public static final int indexNicRxBytes = 3;
    public static final int indexNicRxBytesPerSecond = 4;
    public static final int indexNicRxDropped = 5;
    public static final int indexNicRxErrors = 6;
    public static final int indexNicTxBytes = 7;
    public static final int indexNicTxBytesPerSecond = 8;
    public static final int indexNicTxDropped = 9;
    public static final int indexNicTxErrors = 10;
    public static final int indexNicSummaryData = 11;

}
