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
package org.kaazing.gateway.management.system;


/**
 * Interface for data for a single NIC. As the individual beans do not support things like summary intervals and change
 * notifications (those are done at the NIC List level), this is NOT an extension of ManagementBean, even though it is defining
 * several of the same methods.
 */
public interface NicManagementBean {

    String[] SUMMARY_DATA_FIELD_LIST =
            new String[]{"rxBytes", "rxBytesPerSecond", "rxDropped", "rxErrors", "txBytes", "txBytesPerSecond", "txDropped",
                    "txErrors"};
    int SUMMARY_DATA_RXBYTES_INDEX = 0;
    int SUMMARY_DATA_RXBYTESPERSECOND_INDEX = 1;
    int SUMMARY_DATA_RXDROPPED_INDEX = 2;
    int SUMMARY_DATA_RXERRORS_INDEX = 3;
    int SUMMARY_DATA_TXBYTES_INDEX = 4;
    int SUMMARY_DATA_TXBYTESPERSECOND_INDEX = 5;
    int SUMMARY_DATA_TXDROPPED_INDEX = 6;
    int SUMMARY_DATA_TXERRORS_INDEX = 7;

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    int getId();

    /**
     * Return the NIC's interface name.
     * 
     * @return
     */
    String getName();

    /**
     * Return the total number of bytes read since the system started
     * 
     * @return
     */
    long getRxBytes();

    /**
     * Return the computed read rate since the last update of management data.
     * 
     * @return
     */
    double getRxBytesPerSecond();

    /**
     * Return the total number of read dropped packets(?).
     * 
     * @return
     */
    long getRxDropped();

    /**
     * Return the total number of transmit errors.
     * 
     * @return
     */
    long getRxErrors();

    /**
     * Return the total number of bytes transmitted since the system started
     * 
     * @return
     */
    long getTxBytes();

    /**
     * Return the computed transmit rate since the last update of management data.
     * 
     * @return
     */
    double getTxBytesPerSecond();

    /**
     * Return the total number of transmit dropped packets(?).
     * 
     * @return
     */
    long getTxDropped();

    /**
     * Return the total number of transmit errors.
     * 
     * @return
     */
    long getTxErrors();

    /**
     * Retrieve the summary data as a JSON string (used by JMX and the individual SNMP row).
     * 
     * @return
     */
    String getSummaryData();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    Object[] getSummaryDataValues();

    /**
     * TODO Add method documentation
     * 
     * @param netInterfaceStats
     * @param updateTimeMillis
     */
    void update(Long[] netInterfaceStats, long updateTimeMillis);
}
