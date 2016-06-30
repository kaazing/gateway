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

import java.util.ArrayList;
import java.util.List;
import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.Swap;
import org.hyperic.sigar.Uptime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class to provide system dynamic status information using the open-source SIGAR library to retrieve the system, CPU and NIC
 * data. SIGAR is provided under the Apache 2.0 license. See the Kaazing third-party licenses file for the complete text of the
 * SIGAR license.
 */
public class SigarSystemDataProvider implements SystemDataProvider {
    private static final Logger logger = LoggerFactory.getLogger(SigarSystemDataProvider.class);

    // The SIGAR instance. We know that this instance is valid.
    private Sigar sigar;

    // To avoid overload of exception messages when we somehow have an issue,
    // I'll show the error messages once, then suppress them (it's really more
    // of an issue with NICs). This flag is the suppressor.
    private boolean nicErrorShown;

    public SigarSystemDataProvider() {
        sigar = new Sigar();
    }

    @Override
    public int getNumberOfCpus() {
        try {
            CpuInfo[] cpuInfo = sigar.getCpuInfoList();
            return cpuInfo.length;
        } catch (Throwable t) {
            // we should never get here.
            return 1;
        }
    }

    @Override
    public double getCombinedCpuPercentage() {
        try {
            return sigar.getCpuPerc().getCombined();
        } catch (Throwable t) {
            // we should never get here.
            return 0.0;
        }
    }

    @Override
    public long getTotalFreeMemory() {
        try {
            Mem mem = sigar.getMem();
            return mem.getFree();
        } catch (Throwable t) {
            // we should never get here.
            return 0;
        }
    }

    @Override
    public long getTotalUsedMemory() {
        try {
            Mem mem = sigar.getMem();
            return mem.getUsed();
        } catch (Throwable t) {
            // we should never get here.
            return 0;
        }
    }

    @Override
    public long getTotalMemory() {
        try {
            Mem mem = sigar.getMem();
            return mem.getTotal();
        } catch (Throwable t) {
            // we should never get here.
            return 0;
        }
    }

    @Override
    public long getTotalFreeSwap() {
        try {
            Swap swap = sigar.getSwap();
            return swap.getFree();
        } catch (Throwable t) {
            // we should never get here.
            return 0;
        }
    }

    @Override
    public long getTotalUsedSwap() {
        try {
            Swap swap = sigar.getSwap();
            return swap.getUsed();
        } catch (Throwable t) {
            // we should never get here.
            return 0;
        }
    }

    @Override
    public long getTotalSwap() {
        try {
            Swap swap = sigar.getSwap();
            return swap.getTotal();
        } catch (Throwable t) {
            // we should never get here.
            return 0;
        }
    }

    /**
     * Return the list of valid NIC names.
     * <p/>
     * SIGAR does not know how to handle sub-interfaces (e.g. of form eth0:1, so we will explicitly exclude them. It's also
     * possible, due to a Mac bug in the native SIGAR library for Mac, that we'll get an exception on a particular interface
     * name, and we'll get the same one every time, so we'll test and exclude those interface names as well, but put out an error
     * about that, too.
     *
     * @return
     */
    @Override
    public String[] getNetInterfaceNames() {
        try {
            String[] names = sigar.getNetInterfaceList();
            boolean foundError = false;

            // SIGAR does not know how to handle sub-interfaces (e.g. of form eth0:1),
            // so explicitly exclude those from the list, if any.
            List<String> netInterfaceNames = new ArrayList<>();

            for (String nicName : names) {
                if (nicName.contains(":")) {
                    continue;
                }

                try {
                    sigar.getNetInterfaceStat(nicName);
                    netInterfaceNames.add(nicName);
                } catch (Exception ex) {
                    if (!nicErrorShown) {
                        logger.warn("Caught SIGAR exception trying to get NIC stats for interface '" +
                                nicName + "'. No data for that interface will be sent.");
                        foundError = true;
                    }
                }
            }

            if (foundError) {
                nicErrorShown = true;
            }

            String[] val = new String[netInterfaceNames.size()];
            for (int i = 0; i < netInterfaceNames.size(); i++) {
                val[i] = netInterfaceNames.get(i);
            }

            return val;
        } catch (Throwable t) {
            // we should never get here.
            return new String[0];
        }
    }

    @Override
    public double getUptimeSeconds() {
        try {
            Uptime uptime = sigar.getUptime();
            return uptime.getUptime();
        } catch (Throwable t) {
            // we should never get here.
            return 0.0;
        }
    }

    /**
     * Return the NIC transmission values for the given NIC. The value is an array of 6 long values: 0 - rxBytes 1 - rxDropped 2
     * - rxErrors 3 - txBytes 4 - txDropped 5 - txError
     * <p/>
     * All the values are total bytes, values 0-100.
     *
     * @param netInterfaceName
     */
    @Override
    public Long[] getNetInterfaceStats(String netInterfaceName) {
        // We should never actually get an error on this, as we already tried
        // the all the various names when creating the NIC name list.
        try {
            NetInterfaceStat nicStat = sigar.getNetInterfaceStat(netInterfaceName);
            return new Long[]{nicStat.getRxBytes(),
                    nicStat.getRxDropped(),
                    nicStat.getRxErrors(),
                    nicStat.getTxBytes(),
                    nicStat.getTxDropped(),
                    nicStat.getTxErrors()};
        } catch (Throwable t) {
            // we should never get here.
            return new Long[]{0L, 0L, 0L, 0L, 0L, 0L};
        }
    }

    /**
     * Return the CPU percentage values for all the known CPUs. The value is an array of N values (1 per CPU) of Double[9]. The 9
     * values are the various CPU stats. They are organized as follows: 0 - combined 1 - idle 2 - irq 3 - nice 4 - softIrq 5 -
     * stolen 6 - sys 7 - user 8 - wait
     * <p/>
     * All the values are percentages, values 0-100.
     */
    @Override
    public Double[][] getCpuPercentages() {

        try {
            CpuPerc[] cpuPercList = sigar.getCpuPercList();
            int numCpus = cpuPercList.length;
            Double[][] cpuData = new Double[numCpus][9];

            for (int i = 0; i < numCpus; i++) {
                CpuPerc cpuPerc = cpuPercList[i];
                cpuData[i][0] = cpuPerc.getCombined() * 100.0;
                cpuData[i][1] = cpuPerc.getIdle() * 100.0;
                cpuData[i][2] = cpuPerc.getIrq() * 100.0;
                cpuData[i][3] = cpuPerc.getNice() * 100.0;
                cpuData[i][4] = cpuPerc.getSoftIrq() * 100.0;
                cpuData[i][5] = cpuPerc.getStolen() * 100.0;
                cpuData[i][6] = cpuPerc.getSys() * 100.0;
                cpuData[i][7] = cpuPerc.getUser() * 100.0;
                cpuData[i][8] = cpuPerc.getWait() * 100.0;
            }

            return cpuData;
        } catch (Throwable t) {
            // we should never get here.
            // fake a single cpu
            Double[][] cpuData = new Double[1][9];
            cpuData[0] = new Double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
            return cpuData;
        }
    }
}
