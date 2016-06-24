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
 * Implementation of the management 'data' bean for a single CPU/core. This just contains the
 * data. Wrappers for different management protocols define the use of those data.
 */

import org.json.JSONException;
import org.json.JSONObject;

/**
 * TODO Add class documentation
 */
public class CpuManagementBeanImpl implements CpuManagementBean {

	/**
     * Constructor.
     * @param id
     */
    public CpuManagementBeanImpl(int id) {
        this.id = id;
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
    private final int id;

    // note: update() will be called to set this before any request for data.
    // See SystemDataProvider.getCpuPercentages() for details.
    private Double[] cpuPercentages;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public double getCombined() {
        return cpuPercentages[0];
    }

    @Override
    public double getIdle() {
        return cpuPercentages[1];
    }

    @Override
    public double getIrq() {
        return cpuPercentages[2];
    }

    @Override
    public double getNice() {
        return cpuPercentages[3];
    }

    @Override
    public double getSoftIrq() {
        return cpuPercentages[4];
    }

    @Override
    public double getStolen() {
        return cpuPercentages[5];
    }

    @Override
    public double getSys() {
        return cpuPercentages[6];
    }

    @Override
    public double getUser() {
        return cpuPercentages[7];
    }

    @Override
    public double getWait() {
        return cpuPercentages[8];
    }

    @Override
    public void update(Double[] cpuPercentages) {
        this.cpuPercentages = cpuPercentages;
    }

    @Override
    public String getSummaryData() {
        JSONObject jsonObj = new JSONObject();

        try {
            jsonObj.put(SUMMARY_DATA_FIELD_LIST[SUMMARY_DATA_COMBINED_INDEX], roundTo(getCombined(), ROUND_TO_PLACES));
            jsonObj.put(SUMMARY_DATA_FIELD_LIST[SUMMARY_DATA_IDLE_INDEX], roundTo(getIdle(), ROUND_TO_PLACES));
            jsonObj.put(SUMMARY_DATA_FIELD_LIST[SUMMARY_DATA_IRQ_INDEX], roundTo(getIrq(), ROUND_TO_PLACES));
            jsonObj.put(SUMMARY_DATA_FIELD_LIST[SUMMARY_DATA_NICE_INDEX], roundTo(getNice(), ROUND_TO_PLACES));
            jsonObj.put(SUMMARY_DATA_FIELD_LIST[SUMMARY_DATA_SOFTIRQ_INDEX], roundTo(getSoftIrq(), ROUND_TO_PLACES));
            jsonObj.put(SUMMARY_DATA_FIELD_LIST[SUMMARY_DATA_STOLEN_INDEX], roundTo(getStolen(), ROUND_TO_PLACES));
            jsonObj.put(SUMMARY_DATA_FIELD_LIST[SUMMARY_DATA_SYS_INDEX], roundTo(getSys(), ROUND_TO_PLACES));
            jsonObj.put(SUMMARY_DATA_FIELD_LIST[SUMMARY_DATA_USER_INDEX], roundTo(getUser(), ROUND_TO_PLACES));
            jsonObj.put(SUMMARY_DATA_FIELD_LIST[SUMMARY_DATA_WAIT_INDEX], roundTo(getWait(), ROUND_TO_PLACES));
        } catch (JSONException ex) {
            // There should be no way to hit this, as we know all references above are valid.
        }

        return jsonObj.toString();
    }

    @Override
    public Number[] getSummaryDataValues() {

        Number[] vals = new Number[SUMMARY_DATA_FIELD_LIST.length];

        vals[SUMMARY_DATA_COMBINED_INDEX] = roundTo(getCombined(), ROUND_TO_PLACES);
        vals[SUMMARY_DATA_IDLE_INDEX] = roundTo(getIdle(), ROUND_TO_PLACES);
        vals[SUMMARY_DATA_IRQ_INDEX] = roundTo(getIrq(), ROUND_TO_PLACES);
        vals[SUMMARY_DATA_NICE_INDEX] = roundTo(getNice(), ROUND_TO_PLACES);
        vals[SUMMARY_DATA_SOFTIRQ_INDEX] = roundTo(getSoftIrq(), ROUND_TO_PLACES);
        vals[SUMMARY_DATA_STOLEN_INDEX] = roundTo(getStolen(), ROUND_TO_PLACES);
        vals[SUMMARY_DATA_SYS_INDEX] = roundTo(getSys(), ROUND_TO_PLACES);
        vals[SUMMARY_DATA_USER_INDEX] = roundTo(getUser(), ROUND_TO_PLACES);
        vals[SUMMARY_DATA_WAIT_INDEX] = roundTo(getWait(), ROUND_TO_PLACES);

        return vals;
    }
}
