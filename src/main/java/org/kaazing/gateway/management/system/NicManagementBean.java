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

package org.kaazing.gateway.management.system;


/**
 * Interface for data for a single NIC. As the individual beans do not
 * support things like summary intervals and change notifications (those
 * are done at the NIC List level), this is NOT an extension of ManagementBean,
 * even though it is defining several of the same methods.
 */
public interface NicManagementBean {

    public static String[] SUMMARY_DATA_FIELD_LIST = 
            new String[] {"rxBytes", "rxBytesPerSecond", "rxDropped", "rxErrors", "txBytes", "txBytesPerSecond", "txDropped", "txErrors"};
    public static int SUMMARY_DATA_RXBYTES_INDEX = 0;
    public static int SUMMARY_DATA_RXBYTESPERSECOND_INDEX = 1;
    public static int SUMMARY_DATA_RXDROPPED_INDEX = 2;
    public static int SUMMARY_DATA_RXERRORS_INDEX = 3;
    public static int SUMMARY_DATA_TXBYTES_INDEX = 4;
    public static int SUMMARY_DATA_TXBYTESPERSECOND_INDEX = 5;
    public static int SUMMARY_DATA_TXDROPPED_INDEX = 6;
    public static int SUMMARY_DATA_TXERRORS_INDEX = 7;    
    
    public int getId();

    /**
     * Return the NIC's interface name.
     */
    public String getName();
    
    /**
     * Return the total number of bytes read since the system started
     */
    public long getRxBytes();
    
    /**
     * Return the computed read rate since the last update of management data.
     */
    public double getRxBytesPerSecond();
    
    /**
     * Return the total number of read dropped packets(?).
     */
    public long getRxDropped();
    
    /**
     * Return the total number of transmit errors.
     */
    public long getRxErrors();
    
    /**
     * Return the total number of bytes transmitted since the system started
     */
    public long getTxBytes();
    
    /**
     * Return the computed transmit rate since the last update of management data.
     */
    public double getTxBytesPerSecond();
    
    /**
     * Return the total number of transmit dropped packets(?).
     */
    public long getTxDropped();
    
    /**
     * Return the total number of transmit errors.
     */
    public long getTxErrors();
        
    /**
     * Retrieve the summary data as a JSON string (used by JMX and the individual
     * SNMP row).
     */
    public String getSummaryData();
        
    public Object[] getSummaryDataValues();

    public void update(Long[] netInterfaceStats, long updateTimeMillis);
 }
