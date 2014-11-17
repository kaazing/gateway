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

package org.kaazing.gateway.management.session;

import java.util.Map;

import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.management.ManagementBean;
import org.kaazing.gateway.management.service.ServiceManagementBean;
import org.kaazing.mina.core.session.IoSessionEx;

/**
 * Interface that defines the data and access methods that will be supported by all management 
 * protocols (e.g., JMX, SNMP) for a given session.  Various lifecycle methods are also provided, 
 * to allow things like initialization and cleanup on start/stop.
 */
public interface SessionManagementBean extends ManagementBean {

    public static String[] SUMMARY_DATA_FIELD_LIST = 
            new String[] {"readBytes", "readBytesThroughput", 
                          "writtenBytes", "writtenBytesThroughput"};

    public static int SUMMARY_DATA_READ_BYTES_INDEX = 0;
    public static int SUMMARY_DATA_READ_BYTES_THPT_INDEX = 1;
    public static int SUMMARY_DATA_WRITTEN_BYTES_INDEX = 2;
    public static int SUMMARY_DATA_WRITTEN_BYTES_THPT_INDEX = 3;

    public ServiceManagementBean getServiceManagementBean();
                
    public IoSessionEx getSession();
    
    public void close();

    public void closeImmediately();
    
    public long getId();

    public long getReadBytes();
    
    public double getReadBytesThroughput();
    
    public long getWrittenBytes();

    public double getWrittenBytesThroughput();

    public String getUserPrincipals();  // for return via JSON
    
    public Map<String, String> getUserPrincipalMap();  // for use in listener beans

    public void setUserPrincipals(Map<String, String> userPrincipals);   // set from strategy

    public void enableNotifications(boolean enableNotifications);
    public boolean areNotificationsEnabled();

    public long getCreateTime();
    public String getRemoteAddress();
    
    public String getSessionTypeName();  // for a specific session, NOT the service bind/connect address.

    public String getSessionDirection();
    
    public void incrementExceptionCount();
    
    /*
     * Various methods used by the management strategy objects
     */
    public void doSessionCreated() throws Exception;
    public void doSessionCreatedListeners();
    
    public void doSessionClosed() throws Exception;
    public void doSessionClosedListeners();
    
    public void doMessageReceived(final Object message) throws Exception;
    public void doMessageReceivedListeners(final Object message);

    public void doFilterWrite(final WriteRequest writeRequest) throws Exception;
    public void doFilterWriteListeners(final WriteRequest writeRequest);

    public void doExceptionCaught(final Throwable cause) throws Exception;
    public void doExceptionCaughtListeners(final Throwable cause);
}
