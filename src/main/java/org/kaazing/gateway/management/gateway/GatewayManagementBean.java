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

package org.kaazing.gateway.management.gateway;

import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.management.ClusterManagementListener;
import org.kaazing.gateway.management.ManagementBean;
import org.kaazing.gateway.management.Utils.ManagementSessionType;
import org.kaazing.gateway.management.update.check.ManagementUpdateCheck;
import org.kaazing.gateway.service.cluster.ClusterContext;

/**
 * Interface that defines the data and access methods that will be supported by all management 
 * protocols (e.g., JMX, SNMP) for a single gateway instance.  Various lifecycle methods are also provided, 
 * to allow things like initialization and cleanup on start/stop.
 * NOTE: there are certain methods that must be publicly callable, but are not actually supposed
 * to be exposed to the users.
 */
public interface GatewayManagementBean extends ManagementBean {

    public static String[] SUMMARY_DATA_FIELD_LIST =
            new String[] {"totalCurrentSessions", 
                          "totalBytesReceived", 
                          "totalBytesSent",
                          "totalExceptions",
                          "latestUpdateableGatewayVersion"};

    public static int SUMMARY_DATA_TOTAL_CURRENT_SESSIONS_INDEX = 0;
    public static int SUMMARY_DATA_TOTAL_BYTES_RECEIVED_INDEX = 1;
    public static int SUMMARY_DATA_TOTAL_BYTES_SENT_INDEX = 2;
    public static int SUMMARY_DATA_TOTAL_EXCEPTIONS_INDEX = 3;
    public static int SUMMARY_DATA_LATEST_UPDATEABLE_GATEWAY_VERSION_INDEX = 4;
    
    public int getId();
    
    public String getHostAndPid();

    public String getProductTitle();
    public String getProductBuild();
    public String getProductEdition();

    public long getTotalCurrentSessions();
    public long getTotalBytesReceived();
    public long getTotalBytesSent();
    public long getTotalExceptions();

    public long getUptime();
    public long getStartTime();
    public String getInstanceKey();

    public void setClusterContext(ClusterContext clusterContext);

    public String getClusterMembers();
    public String getClusterBalancerMap();
    public String getManagementServiceMap();

    public void addClusterManagementListener(ClusterManagementListener listener);
    
    // Various methods needed by the strategy objects
    public void doSessionCreated(final long sessionId, final ManagementSessionType managementSessionType) throws Exception;
    public void doSessionCreatedListeners(final long sessionId, final ManagementSessionType managementSessionType);
    
    public void doSessionClosed(final long sessionId, final ManagementSessionType managementSessionType) throws Exception;      
    public void doSessionClosedListeners(final long sessionId, final ManagementSessionType managementSessionType);      
    
    public void doMessageReceived(final long sessionId, final long sessioReadBytes, final Object message) throws Exception;
    public void doMessageReceivedListeners(final long sessionId, final long sessioReadBytes, final Object message);

    public void doFilterWrite(final long sessionId, final long sessioWrittenBytes, final WriteRequest writeRequest) throws Exception;
    public void doFilterWriteListeners(final long sessionId, final long sessioWrittenBytes, final WriteRequest writeRequest);

    public void doExceptionCaught(final long sessionId, final Throwable cause) throws Exception;
    public void doExceptionCaughtListeners(final long sessionId, final Throwable cause);

    public String getAvailableUpdateVersion();

    /**
     * Forces a check an update check
     */
    public void forceUpdateVersionCheck();
    public ManagementUpdateCheck getUpdateCheck();
}
