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

import javax.management.ObjectName;

public interface HostMXBean {

    ObjectName getObjectName();

    String getOSName();

    double getUptimeSeconds();

    long getTotalFreeMemory();

    long getTotalUsedMemory();

    long getTotalMemory();

    long getTotalFreeSwap();

    long getTotalUsedSwap();

    long getTotalSwap();

    double getCpuPercentage();


    String getSummaryDataFields();

    String getSummaryData();

    int getSummaryDataGatherInterval();

    void setSummaryDataGatherInterval(int interval);
}
