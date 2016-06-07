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
 * Interface that defines the data and access methods that will be supported by all management protocols (e.g., JMX, SNMP) for
 * retrieving System data.
 */
public interface HostManagementBean extends SystemManagementBean {

    String[] SUMMARY_DATA_FIELD_LIST =
            new String[]{"osName", "uptimeSeconds",
                    "totalFreeMemory", "totalUsedMemory", "totalMemory",
                    "totalFreeSwap", "totalUsedSwap", "totalSwap",
                    "cpuPercentage"};
    int SUMMARY_DATA_OS_NAME_INDEX = 0;
    int SUMMARY_DATA_UPTIME_SECONDS_INDEX = 1;
    int SUMMARY_DATA_TOTAL_FREE_MEMORY_INDEX = 2;
    int SUMMARY_DATA_TOTAL_USED_MEMORY_INDEX = 3;
    int SUMMARY_DATA_TOTAL_MEMORY_INDEX = 4;
    int SUMMARY_DATA_TOTAL_FREE_SWAP_INDEX = 5;
    int SUMMARY_DATA_TOTAL_USED_SWAP_INDEX = 6;
    int SUMMARY_DATA_TOTAL_SWAP_INDEX = 7;
    int SUMMARY_DATA_CPU_PERCENTAGE_INDEX = 8;

    String getOSName();

    double getUptimeSeconds();

    long getTotalFreeMemory();

    long getTotalUsedMemory();

    long getTotalMemory();

    long getTotalFreeSwap();

    long getTotalUsedSwap();

    long getTotalSwap();

    double getCpuPercentage();
}
