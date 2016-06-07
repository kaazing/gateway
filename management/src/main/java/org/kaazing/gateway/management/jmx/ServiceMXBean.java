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
package org.kaazing.gateway.management.jmx;

import java.util.Map;

public interface ServiceMXBean {

    long getNumberOfCumulativeSessions();

    long getNumberOfCumulativeNativeSessions();

    long getNumberOfCumulativeEmulatedSessions();

    long getNumberOfCurrentSessions();

    long getNumberOfCurrentNativeSessions();

    long getNumberOfCurrentEmulatedSessions();

    long getNumberOfExceptions();

    String getLatestException();

    void clearCumulativeSessionsCount();

    long getTotalBytesReceivedCount();

    long getTotalBytesSentCount();

    Map<String, Map<String, String>> getLoggedInSessions();

    Map<String, String> getUserPrincipals(Long sessionId);

    // -----------------------------------------------------------------
    // For proxy style services the following methods should return data
    // -----------------------------------------------------------------

    // FIXME: Refactor this into ProxyServiceMXBean so that they only show up on relevant services?
    long getLastSuccessfulConnectTime();

    long getLastFailedConnectTime();

    boolean getLastHeartbeatPingResult();

    long getLastHeartbeatPingTimestamp();

    int getHeartbeatPingCount();

    int getHeartbeatPingSuccessesCount();

    int getHeartbeatPingFailuresCount();

    boolean isServiceConnected();

    boolean isHeartbeatRunning();

    // -----------------------------------------------------------------
    // end of proxy-style service data
    // -----------------------------------------------------------------

    // lifecycle methods
    void start() throws Exception;
    void stop() throws Exception;
    void restart() throws Exception;
    void closeSessions(String prinicpalName, String prinicpalClassName) throws Exception;
}
