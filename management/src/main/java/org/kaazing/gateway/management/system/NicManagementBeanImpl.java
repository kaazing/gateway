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
 * Implementation of the management 'data' bean for a singe network interface card.
 * This just contains the data. Wrappers for different management protocols define
 * the use of those data.
 */

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NicManagementBeanImpl implements NicManagementBean {
	
	private final int id;

    private final String interfaceName;
	
	/**
     * Constructor.
     * 
     * @param id
     * @param interfaceName
     */
    public NicManagementBeanImpl(int id, String interfaceName) {
        this.id = id;
        this.interfaceName = interfaceName;
    }

    public static double roundTo(double val, int places) {
        long temp = (long) (val * Math.pow(10, places));
        return (double) temp / Math.pow(10, places);
    }

    public static final int ROUND_TO_PLACES = 4;

    // Keep a unique index number for each service instance, as we can use
    // it in SNMP for an OID, and it might be useful elsewhere if we decide
    // we want to use it in place of some map key or something.  The SNMP
    // support for sessions also depends on knowing this value.
//    private static final AtomicInteger maxServiceIndex = new AtomicInteger(1);

    // note: update will be called to set this before any request for data.
    private Long[] netInterfaceStats;

    private double rxBytesPerSecond;

    private double txBytesPerSecond;

    private long updateTimeMillis = -1;  // last time the update occurred

//    private static final Logger logger = LoggerFactory.getLogger(NicManagementBeanImpl.class);
    
    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return interfaceName;
    }

    @Override
    public long getRxBytes() {
        return netInterfaceStats[0];
    }

    @Override
    public double getRxBytesPerSecond() {
        return rxBytesPerSecond;
    }

    @Override
    public long getRxDropped() {
        return netInterfaceStats[1];
    }

    @Override
    public long getRxErrors() {
        return netInterfaceStats[2];
    }

    @Override
    public long getTxBytes() {
        return netInterfaceStats[3];
    }

    @Override
    public double getTxBytesPerSecond() {
        return txBytesPerSecond;
    }

    @Override
    public long getTxDropped() {
        return netInterfaceStats[4];
    }

    @Override
    public long getTxErrors() {
        return netInterfaceStats[5];
    }


    @Override
    public void update(Long[] netInterfaceStats, long updateTimeMillis) {
        if (this.updateTimeMillis > 0) {
            long deltaMillis = updateTimeMillis - this.updateTimeMillis;
            long deltaRxBytes = netInterfaceStats[0] - getRxBytes();
            rxBytesPerSecond = (deltaRxBytes * 1000.0d) / deltaMillis;
            long deltaTxBytes = netInterfaceStats[3] - getTxBytes();
            txBytesPerSecond = (deltaTxBytes * 1000.0d) / deltaMillis;
        }

        this.netInterfaceStats = netInterfaceStats;
        this.updateTimeMillis = updateTimeMillis;
    }

    @Override
    public String getSummaryData() {
        JSONObject jsonObj = new JSONObject();

        try {
            jsonObj.put(SUMMARY_DATA_FIELD_LIST[SUMMARY_DATA_RXBYTES_INDEX], getRxBytes());
            jsonObj.put(SUMMARY_DATA_FIELD_LIST[SUMMARY_DATA_RXBYTESPERSECOND_INDEX], roundTo(getRxBytesPerSecond(),
                    ROUND_TO_PLACES));
            jsonObj.put(SUMMARY_DATA_FIELD_LIST[SUMMARY_DATA_RXDROPPED_INDEX], getRxDropped());
            jsonObj.put(SUMMARY_DATA_FIELD_LIST[SUMMARY_DATA_RXERRORS_INDEX], getRxErrors());
            jsonObj.put(SUMMARY_DATA_FIELD_LIST[SUMMARY_DATA_TXBYTES_INDEX], getTxBytes());
            jsonObj.put(SUMMARY_DATA_FIELD_LIST[SUMMARY_DATA_TXBYTESPERSECOND_INDEX], roundTo(getTxBytesPerSecond(),
                    ROUND_TO_PLACES));
            jsonObj.put(SUMMARY_DATA_FIELD_LIST[SUMMARY_DATA_TXDROPPED_INDEX], getTxDropped());
            jsonObj.put(SUMMARY_DATA_FIELD_LIST[SUMMARY_DATA_TXERRORS_INDEX], getTxErrors());
        } catch (Exception ex) {
            // This is only for JSON exceptions, but there should be no way to
            // hit this.
        }

        return jsonObj.toString();
    }

    @Override
    public Object[] getSummaryDataValues() {

        Object[] vals = new Object[SUMMARY_DATA_FIELD_LIST.length];

        vals[SUMMARY_DATA_RXBYTES_INDEX] = getRxBytes();
        vals[SUMMARY_DATA_RXBYTESPERSECOND_INDEX] = roundTo(getRxBytesPerSecond(), ROUND_TO_PLACES);
        vals[SUMMARY_DATA_RXDROPPED_INDEX] = getRxDropped();
        vals[SUMMARY_DATA_RXERRORS_INDEX] = getRxErrors();
        vals[SUMMARY_DATA_TXBYTES_INDEX] = getTxBytes();
        vals[SUMMARY_DATA_TXBYTESPERSECOND_INDEX] = roundTo(getTxBytesPerSecond(), ROUND_TO_PLACES);
        vals[SUMMARY_DATA_TXDROPPED_INDEX] = getTxDropped();
        vals[SUMMARY_DATA_TXERRORS_INDEX] = getTxErrors();

        return vals;
    }
}
