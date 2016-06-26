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
public interface CpuManagementBean {

    String[] SUMMARY_DATA_FIELD_LIST =
            new String[]{"combined", "idle", "irq", "nice", "softIrq", "stolen", "sys", "user", "wait"};
    int SUMMARY_DATA_COMBINED_INDEX = 0;
    int SUMMARY_DATA_IDLE_INDEX = 1;
    int SUMMARY_DATA_IRQ_INDEX = 2;
    int SUMMARY_DATA_NICE_INDEX = 3;
    int SUMMARY_DATA_SOFTIRQ_INDEX = 4;
    int SUMMARY_DATA_STOLEN_INDEX = 5;
    int SUMMARY_DATA_SYS_INDEX = 6;
    int SUMMARY_DATA_USER_INDEX = 7;
    int SUMMARY_DATA_WAIT_INDEX = 8;

    /**
     * @return
     */
    int getId();

    /**
     * @return the combined CPU percentage, as a value 0-100 (so 5% would be 5, not 0.05).
     */
    double getCombined();

    /**
     * @return the idle CPU percentage, as a value 0-100 (so 5% would be 5, not 0.05).
     */
    double getIdle();

    /**
     * @return the IRQ CPU percentage, as a value 0-100 (so 5% would be 5, not 0.05).
     */
    double getIrq();

    /**
     * @return the 'nice' CPU percentage, as a value 0-100 (so 5% would be 5, not 0.05).
     */
    double getNice();

    /**
     * @return the soft IRQ CPU percentage, as a value 0-100 (so 5% would be 5, not 0.05).
     */
    double getSoftIrq();

    /**
     * @return the ' CPU percentage, as a value 0-100 (so 5% would be 5, not 0.05).
     */
    double getStolen();

    /**
     * @return the combined CPU percentage, as a value 0-100 (so 5% would be 5, not 0.05).
     */
    double getSys();

    /**
     * @return the combined CPU percentage, as a value 0-100 (so 5% would be 5, not 0.05).
     */
    double getUser();

    /**
     * @return the combined CPU percentage, as a value 0-100 (so 5% would be 5, not 0.05).
     */
    double getWait();

    /**
     * @return Retrieve the summary data as a JSON string (used by JMX and the individual SNMP entry row).
     */
    String getSummaryData();

    /**
     * @return
     */
    Number[] getSummaryDataValues();

    /**
     * @param cpuPercentages
     */
    void update(Double[] cpuPercentages);
}
