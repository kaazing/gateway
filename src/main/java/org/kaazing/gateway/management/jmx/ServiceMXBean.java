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

package org.kaazing.gateway.management.jmx;

import java.util.Map;

public interface ServiceMXBean {

    public long getNumberOfCumulativeSessions();

    public long getNumberOfCumulativeNativeSessions();
    
    public long getNumberOfCumulativeEmulatedSessions();

    public long getNumberOfCurrentSessions();
    
    public long getNumberOfCurrentNativeSessions();
    
    public long getNumberOfCurrentEmulatedSessions();

    public long getNumberOfExceptions();
    
    public String getLatestException();

    public void clearCumulativeSessionsCount();

    public long getTotalBytesReceivedCount();

    public long getTotalBytesSentCount();

    public Map<String, Map<String, String>> getLoggedInSessions();

    public Map<String, String> getUserPrincipals(Long sessionId);
    
    // -----------------------------------------------------------------
    // For proxy style services the following methods should return data
    // -----------------------------------------------------------------

    // FIXME: Refactor this into ProxyServiceMXBean so that they only show up on relevant services?
    public long getLastSuccessfulConnectTime();
    public long getLastFailedConnectTime();
    public boolean getLastHeartbeatPingResult();
    public long getLastHeartbeatPingTimestamp();
    public int getHeartbeatPingCount();
    public int getHeartbeatPingSuccessesCount();
    public int getHeartbeatPingFailuresCount();
    public boolean isServiceConnected();
    public boolean isHeartbeatRunning();

    // -----------------------------------------------------------------
    // end of proxy-style service data
    // -----------------------------------------------------------------

    // lifecycle methods
    public void start() throws Exception;
    public void stop() throws Exception;
    public void restart() throws Exception;
}
