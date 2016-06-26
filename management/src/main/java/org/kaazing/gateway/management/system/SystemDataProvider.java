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


public interface SystemDataProvider {

	/**
     * TODO Add method documentation
     * 
     * @return
     */
    int getNumberOfCpus();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    double getCombinedCpuPercentage();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    long getTotalFreeMemory();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    long getTotalUsedMemory();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    long getTotalMemory();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    long getTotalFreeSwap();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    long getTotalUsedSwap();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    long getTotalSwap();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    String[] getNetInterfaceNames();

    /**
     * TODO Add method documentation
     * 
     * @return
     */
    double getUptimeSeconds();

    /**
     * Return the NIC transmission values for the given NIC. The value is an array of 6 long values: 0 - rxBytes 1 - rxDropped 2
     * - rxErrors 3 - txBytes 4 - txDropped 5 - txError
     * <p/>
     * All the values are total bytes, values 0-100.
     *
     * @param netInterfaceName
     * @return
     */
    Long[] getNetInterfaceStats(String netInterfaceName);

    /**
     * Return the CPU percentage values for all the known CPUs. The value is an array of N values (1 per CPU) of Double[9]. The 9
     * values are the various CPU stats. They are organized as follows: 0 - combined 1 - idle 2 - irq 3 - nice 4 - softIrq 5 -
     * stolen 6 - sys 7 - user 8 - wait
     * <p/>
     * All the values are percentages, values 0-100.
     * 
     * @return
     */
    Double[][] getCpuPercentages();
}
