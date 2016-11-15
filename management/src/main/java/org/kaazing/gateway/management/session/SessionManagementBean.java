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
package org.kaazing.gateway.management.session;

import java.util.Map;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.management.ManagementBean;
import org.kaazing.gateway.management.service.ServiceManagementBean;
import org.kaazing.mina.core.session.IoSessionEx;

/**
 * Interface that defines the data and access methods that will be supported by all management protocols (e.g., JMX, SNMP) for a
 * given session.  Various lifecycle methods are also provided, to allow things like initialization and cleanup on start/stop.
 */
public interface SessionManagementBean extends ManagementBean {

    String[] SUMMARY_DATA_FIELD_LIST =
            new String[]{"readBytes", "readBytesThroughput",
                    "writtenBytes", "writtenBytesThroughput"};

    int SUMMARY_DATA_READ_BYTES_INDEX = 0;
    int SUMMARY_DATA_READ_BYTES_THPT_INDEX = 1;
    int SUMMARY_DATA_WRITTEN_BYTES_INDEX = 2;
    int SUMMARY_DATA_WRITTEN_BYTES_THPT_INDEX = 3;

    ServiceManagementBean getServiceManagementBean();

    IoSessionEx getSession();

    void close();

    void closeImmediately();

    long getId();

    long getReadBytes();

    double getReadBytesThroughput();

    long getWrittenBytes();

    double getWrittenBytesThroughput();

    String getUserPrincipals();  // for return via JSON

    Map<String, String> getUserPrincipalMap();  // for use in listener beans

    void setUserPrincipals(Map<String, String> userPrincipals);   // set from strategy

    void enableNotifications(boolean enableNotifications);

    boolean areNotificationsEnabled();

    long getCreateTime();

    String getRemoteAddress();

    String getSessionTypeName();  // for a specific session, NOT the service bind/connect address.

    String getSessionDirection();

    void incrementExceptionCount();

    /*
     * Various methods used by the management strategy objects
     */
    void doSessionCreated() throws Exception;

    void doSessionCreatedListeners();

    void doSessionClosed() throws Exception;

    void doSessionClosedListeners();

    void doMessageReceived(final Object message) throws Exception;

    void doMessageReceivedListeners(final Object message);

    void doFilterWrite(final WriteRequest writeRequest) throws Exception;

    void doFilterWriteListeners(final WriteRequest writeRequest);

    void doExceptionCaught(final Throwable cause) throws Exception;

    void doExceptionCaughtListeners(final Throwable cause);

    long getLastRoundTripLatency();

    long getLastRoundTripLatencyTimestamp();

}
