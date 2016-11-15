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
 * The SystemDataProvider now always used because we no longer include SIGAR. Generally wejust do things like return empty
 * arrays of NIC names, only 1 CPU, 0 for various statistics values, etc. Later we might be able to return some actual reasonable
 * information if we can find another (non-SIGAR) source for the data.
 */
public class NonSigarSystemDataProvider implements SystemDataProvider {

    public NonSigarSystemDataProvider() {
    }

    @Override
    public int getNumberOfCpus() {
        return Runtime.getRuntime().availableProcessors();
    }

    @Override
    public double getCombinedCpuPercentage() {
        return 0.0;  // just return a bogus value;
    }

    @Override
    public long getTotalFreeMemory() {
        return 0;
    }

    @Override
    public long getTotalUsedMemory() {
        return 0;
    }

    @Override
    public long getTotalMemory() {
        return 0;
    }

    @Override
    public long getTotalFreeSwap() {
        return 0;
    }

    @Override
    public long getTotalUsedSwap() {
        return 0;
    }

    @Override
    public long getTotalSwap() {
        return 0;
    }

    @Override
    public String[] getNetInterfaceNames() {
        return new String[0];
    }

    @Override
    public double getUptimeSeconds() {
        return 0.0;
    }

    @Override
    public Long[] getNetInterfaceStats(String netInterfaceName) {
        return new Long[]{0L, 0L, 0L, 0L, 0L, 0L};
    }

    @Override
    public Double[][] getCpuPercentages() {
        // fake a single cpu
        Double[][] cpuData = new Double[1][9];
        cpuData[0] = new Double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        return cpuData;
    }
}
