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

package org.kaazing.gateway.management.snmp.mib;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import org.kaazing.gateway.management.Utils;
import org.kaazing.gateway.management.context.ManagementContext;
import org.kaazing.gateway.management.snmp.SummaryDataIntervalMO;
import org.kaazing.gateway.management.system.JvmManagementBean;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOAccess;
import org.snmp4j.agent.MOGroup;
import org.snmp4j.agent.MOServer;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOFactory;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Counter64;
import org.snmp4j.smi.Gauge32;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;

/**
 * MIB support for JVM data.
 * <p/>
 * Kaazing's SNMP support is based on the SNMP4J open-source library under the Apache 2.0 license. To see the full text of the
 * license, please see the Kaazing third-party licenses file.
 */
public class JVMManagementMIB implements MOGroup {
    public static final OID oidSunEnterprise =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42});
    public static final OID oidJMgmt =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145});
    public static final OID oidJvmStandard =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3});
    public static final OID oidJvmManagementMIB =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1});
    public static final OID oidJvmMIBObjects =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1});
    public static final OID oidJvmMIBNotifications =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 2});
    public static final OID oidJvmMIBConformance =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 3}); // FIMXE:  used?

    // The class-loading group
    public static final OID oidJvmClassLoading =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1, 1});
    // the number of classes currently loaded in the JVM
    public static final OID oidJvmClassesLoadedCount =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1, 1, 1, 0});
    // The total #classes loaded since the JVM started execution
    public static final OID oidJvmClassesTotalLoadedCount =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1, 1, 2, 0});
    // The total #class unloaded since the JVM started execution
    public static final OID oidJvmClassesUnloadedCount =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1, 1, 3, 0});
    // Enable/disable verbose output for the class-loading system.
    // 'verbose' - verbose output enabled
    // 'silent' - otherwise
    public static final OID oidJvmClassesVerboseLevel =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1, 1, 4, 0});

    // The JVM memory group root OID
    public static final OID oidJvmMemory =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1, 2});

    // The approx. number of objects that are pending finalization
    public static final OID oidJvmMemoryPendingFinalCount =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1, 2, 1, 0});

    // Enables/disabled verbose output for the memory system
    // 'verbose' - verbose output is enabled
    // 'silent'  - otherwise
    public static final OID oidJvmMemoryGCVerboseLevel =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1, 2, 2, 0});

    // This object makes it possible to remotely trigger the
    // Garbage Collector in the JVM.  Syntax is an enumeration
    // which defines :
    //  two state values (returned from a GET request):
    //     unsupported(1): remote GC invocation not supported by SNMP agent
    //     supported(2)  : remote GC invocation IS supported by SNMP agent
    //  One action value, provided in a SET to trigger the GC:
    //     start(3) : manager wishes to trigger a GC
    //  Two result values as responses to SET request:
    //     started(4) : GC was successfully started (not necessarily finished)
    //     failed(5)  : GC couldn't be triggered
    // NOTE: if unsupported, a SET will return that, not failed(5).
    public static final OID oidJvmMemoryGCCall =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1, 2, 3, 0});

    // Total memory (bytes) that the JVM initially requests from the OS
    // for memory mngt for heap memory pools
    public static final OID oidJvmMemoryHeapInitSize =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1, 2, 10, 0});
    // Total memory used (bytes) from heap memory pools
    public static final OID oidJvmMemoryHeapUsed =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1, 2, 11, 0});
    // Total memory (bytes) committed by heap memory pools
    public static final OID oidJvmMemoryHeapCommitted =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1, 2, 12, 0});
    // Total max size of memory (bytes) for all heap memory pools.
    public static final OID oidJvmMemoryHeapMaxSize =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1, 2, 13, 0});
    // Total memory (bytes) that JVM initially requests from OS
    // for NON-heap memory pools
    public static final OID oidJvmMemoryNonHeapInitSize =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1, 2, 20, 0});
    // Total memory used (bytes) from non-heap memory pools
    public static final OID oidJvmMemoryNonHeapUsed =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1, 2, 21, 0});
    // Total memory (bytes) committed by non-heap memory pools
    public static final OID oidJvmMemoryNonHeapCommitted =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1, 2, 22, 0});
    // Total max size of memory (bytes) for all non-heap memory pools.
    public static final OID oidJvmMemoryNonHeapMaxSize =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1, 2, 23, 0});

    public static final OID oidJvmMemoryManagerTable =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1, 2, 100});
    public static final OID oidJvmMemoryManagerEntry =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1, 2, 100, 1});
    // Index to uniquely identify a memory manager.
    public static final OID oidJvmMemoryManagerIndex =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1, 2, 100, 1, 1});
    // Name of the memory manager, as returned by getName();
    public static final OID oidJvmMemoryManagerName =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1, 2, 100, 1, 2});
    // Memory manager state - indicates whether this memory manager is
    // valid in the JVM.  A memory manager becomes invalid once the
    // JVM removes it from the memory system
    public static final OID oidJvmMemoryManagerState =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1, 2, 100, 1, 3});

    public static final OID oidJvmThreading =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1, 3});
    public static final OID oidJvmThreadingLiveThreads =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1, 3, 1, 0});
    public static final OID oidJvmThreadingPeakThreads =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1, 3, 3, 0});
    public static final OID oidJvmThreadingTotalCount =
            new OID(new int[]{1, 3, 6, 1, 4, 1, 42, 2, 145, 3, 163, 1, 1, 3, 4, 0});

    private static final int JVM_CLASSES_LOADED_OPER = 1;
    private static final int JVM_CLASSES_TOTAL_LOADED_OPER = 2;
    private static final int JVM_CLASSES_UNLOADED_OPER = 3;
    private static final int JVM_CLASSES_VERBOSE_LEVEL_OPER = 4;
    private static final int JVM_MEMORY_PENDING_FINAL_COUNT_OPER = 5;
    private static final int JVM_MEMORY_GC_VERBOSE_LEVEL_OPER = 6;
    private static final int JVM_MEMORY_GC_CALL_OPER = 7;
    private static final int JVM_MEMORY_HEAP_SIZE_INIT_OPER = 8;
    private static final int JVM_MEMORY_HEAP_USED_OPER = 9;
    private static final int JVM_MEMORY_HEAP_COMMITTED_OPER = 10;
    private static final int JVM_MEMORY_HEAP_MAX_SIZE_OPER = 11;
    private static final int JVM_MEMORY_NONHEAP_SIZE_INIT_OPER = 12;
    private static final int JVM_MEMORY_NONHEAP_USED_OPER = 13;
    private static final int JVM_MEMORY_NONHEAP_COMMITTED_OPER = 14;
    private static final int JVM_MEMORY_NONHEAP_MAX_SIZE_OPER = 15;
    private static final int JVM_THREAD_LIVE_OPER = 16;
    private static final int JVM_THREAD_PEAK_OPER = 17;
    private static final int JVM_THREAD_TOTAL_OPER = 18;

    private final ManagementContext managementContext;

    // class loading variables
    private final MOScalar jvmClassesLoadedCount;
    private final MOScalar jvmClassesTotalLoadedCount;
    private final MOScalar jvmClassesUnloadedCount;
    private final MOScalar jvmClassesVerboseLevel;

    // GC variables
    private final MOScalar jvmMemoryPendingFinalCount;
    private final MOScalar jvmMemoryGCVerboseLevel;
    private final MOScalar jvmMemoryGCCall;

    // heap memory variables
    private final MOScalar jvmMemoryHeapSizeInit;
    private final MOScalar jvmMemoryHeapUsed;
    private final MOScalar jvmMemoryHeapCommitted;
    private final MOScalar jvmMemoryHeapMaxSize;

    // non-heap memory variables
    private final MOScalar jvmMemoryNonHeapSizeInit;
    private final MOScalar jvmMemoryNonHeapUsed;
    private final MOScalar jvmMemoryNonHeapCommitted;
    private final MOScalar jvmMemoryNonHeapMaxSize;

    // thread variables
    private final MOScalar jvmThreadingLiveThreads;
    private final MOScalar jvmThreadingPeakThreads;
    private final MOScalar jvmThreadingTotalThreads;

    private SystemString summaryDataFields;

    private SystemString summaryData;

    private MOScalar summaryDataNotificationInterval;

    private MOScalar summaryDataGatherInterval;

    private JvmManagementBean bean;

    private static final int SUMMARY_DATA_FIELDS_OPER = 40;
    private static final int SUMMARY_DATA_OPER = 41;

    public JVMManagementMIB(ManagementContext managementContext, MOFactory factory) {
        this.managementContext = managementContext;

        try {
            // ---------------------
            // CLASSES LOADED VARIABLES
            // ---------------------
            ClassLoadingMXBean classLoadingBean = ManagementFactory.getClassLoadingMXBean();
            jvmClassesLoadedCount = new JVMClassLoadingLong(oidJvmClassesLoadedCount,
                    factory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new Counter64(classLoadingBean.getLoadedClassCount()),
                    JVM_CLASSES_LOADED_OPER);

            jvmClassesTotalLoadedCount = new JVMClassLoadingLong(oidJvmClassesTotalLoadedCount,
                    factory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new Counter64(classLoadingBean.getTotalLoadedClassCount()),
                    JVM_CLASSES_TOTAL_LOADED_OPER);

            jvmClassesUnloadedCount = new JVMClassLoadingLong(oidJvmClassesUnloadedCount,
                    factory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new Counter64(classLoadingBean.getUnloadedClassCount()),
                    JVM_CLASSES_UNLOADED_OPER);

            jvmClassesVerboseLevel = new JVMClassLoadingVerboseLevel(oidJvmClassesVerboseLevel,
                    factory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_WRITE),
                    new Integer32(classLoadingBean.isVerbose() ? 2 : 1));

            // ---------------------
            // GC VARIABLES
            // ---------------------
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

            jvmMemoryPendingFinalCount = new JVMMemoryScalar(oidJvmMemoryPendingFinalCount,
                    factory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new Gauge32(memoryBean.getObjectPendingFinalizationCount()),
                    JVM_MEMORY_PENDING_FINAL_COUNT_OPER); // FIXME:  use a constant for the operation

            jvmMemoryGCVerboseLevel = new JVMMemoryGCVerboseLevel(oidJvmMemoryGCVerboseLevel,
                    factory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_WRITE),
                    new Integer32(memoryBean.isVerbose() ? 2 : 1));

            jvmMemoryGCCall = new JVMMemoryGCCall(oidJvmMemoryGCCall,
                    factory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_WRITE),
                    new Integer32(2));

            // ---------------------
            // HEAP MEMORY VARIABLES
            // ---------------------
            MemoryUsage heapUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();

            // initial heap size...no need for dynamic value
            jvmMemoryHeapSizeInit = new JVMMemoryScalar(oidJvmMemoryHeapInitSize,
                    factory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new Counter64(heapUsage.getInit()),
                    JVM_MEMORY_HEAP_SIZE_INIT_OPER);

            // used heap
            jvmMemoryHeapUsed = new JVMMemoryScalar(oidJvmMemoryHeapUsed,
                    factory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new Counter64(heapUsage.getUsed()),
                    JVM_MEMORY_HEAP_USED_OPER);

            // used heap
            jvmMemoryHeapCommitted = new JVMMemoryScalar(oidJvmMemoryHeapCommitted,
                    factory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new Counter64(heapUsage.getCommitted()),
                    JVM_MEMORY_HEAP_COMMITTED_OPER);

            jvmMemoryHeapMaxSize = new JVMMemoryScalar(oidJvmMemoryHeapMaxSize,
                    factory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new Counter64(heapUsage.getMax()),
                    JVM_MEMORY_HEAP_MAX_SIZE_OPER);

            // ---------------------
            // NON-HEAP MEMORY VARIABLES
            // ---------------------

            MemoryUsage nonHeapUsage = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();

            // initial heap size...no need for dynamic value
            jvmMemoryNonHeapSizeInit = new JVMMemoryScalar(oidJvmMemoryNonHeapInitSize,
                    factory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new Counter64(nonHeapUsage.getInit()),
                    JVM_MEMORY_NONHEAP_SIZE_INIT_OPER);

            // used heap
            jvmMemoryNonHeapUsed = new JVMMemoryScalar(oidJvmMemoryNonHeapUsed,
                    factory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new Counter64(nonHeapUsage.getUsed()),
                    JVM_MEMORY_NONHEAP_USED_OPER);

            // used heap
            jvmMemoryNonHeapCommitted = new JVMMemoryScalar(oidJvmMemoryNonHeapCommitted,
                    factory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new Counter64(nonHeapUsage.getCommitted()),
                    JVM_MEMORY_NONHEAP_COMMITTED_OPER);

            jvmMemoryNonHeapMaxSize = new JVMMemoryScalar(oidJvmMemoryNonHeapMaxSize,
                    factory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new Counter64(nonHeapUsage.getMax()),
                    JVM_MEMORY_NONHEAP_MAX_SIZE_OPER);


            // ---------------------
            // THREAD VARIABLES
            // ---------------------

            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

            jvmThreadingLiveThreads = new JVMThreadingScalar(oidJvmThreadingLiveThreads,
                    factory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new Gauge32(threadBean.getThreadCount()),
                    JVM_THREAD_LIVE_OPER);

            jvmThreadingPeakThreads = new JVMThreadingScalar(oidJvmThreadingPeakThreads,
                    factory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new Gauge32(threadBean.getPeakThreadCount()),
                    JVM_THREAD_PEAK_OPER);

            // used heap
            jvmThreadingTotalThreads = new JVMThreadingScalar(oidJvmThreadingTotalCount,
                    factory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new Gauge32(threadBean.getTotalStartedThreadCount()),
                    JVM_THREAD_TOTAL_OPER);

            summaryDataFields = new SystemString(MIBConstants.oidJvmSummaryDataFields,
                    factory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new OctetString(),
                    SUMMARY_DATA_FIELDS_OPER);

            summaryData = new SystemString(MIBConstants.oidJvmSummaryData,
                    factory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_ONLY),
                    new OctetString(),
                    SUMMARY_DATA_OPER);

            summaryDataNotificationInterval = new SummaryDataIntervalMO(factory,
                    managementContext.getJvmSummaryDataNotificationInterval(),
                    MIBConstants.oidJvmSummaryDataNotificationInterval);

            summaryDataGatherInterval = new SummaryDataIntervalMO(factory,
                    managementContext.getJvmSummaryDataNotificationInterval(),
                    MIBConstants.oidJvmSummaryDataGatherInterval);

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void registerMOs(MOServer server, OctetString context) throws DuplicateRegistrationException {
        server.register(jvmClassesLoadedCount, context);
        server.register(jvmClassesTotalLoadedCount, context);
        server.register(jvmClassesUnloadedCount, context);
        server.register(jvmClassesVerboseLevel, context);

        server.register(jvmMemoryPendingFinalCount, context);
        server.register(jvmMemoryGCVerboseLevel, context);
        server.register(jvmMemoryGCCall, context);

        server.register(jvmMemoryHeapSizeInit, context);
        server.register(jvmMemoryHeapUsed, context);
        server.register(jvmMemoryHeapCommitted, context);
        server.register(jvmMemoryHeapMaxSize, context);

        server.register(jvmMemoryNonHeapSizeInit, context);
        server.register(jvmMemoryNonHeapUsed, context);
        server.register(jvmMemoryNonHeapCommitted, context);
        server.register(jvmMemoryNonHeapMaxSize, context);

        server.register(jvmThreadingLiveThreads, context);
        server.register(jvmThreadingPeakThreads, context);
        server.register(jvmThreadingTotalThreads, context);

        server.register(summaryDataFields, context);
        server.register(summaryData, context);
        server.register(summaryDataNotificationInterval, context);
        server.register(summaryDataGatherInterval, context);
    }

    @Override
    public void unregisterMOs(MOServer server, OctetString context) {
        server.unregister(jvmClassesLoadedCount, context);
        server.unregister(jvmClassesTotalLoadedCount, context);
        server.unregister(jvmClassesUnloadedCount, context);
        server.unregister(jvmClassesVerboseLevel, context);

        server.unregister(jvmMemoryPendingFinalCount, context);
        server.unregister(jvmMemoryGCVerboseLevel, context);
        server.unregister(jvmMemoryGCCall, context);

        server.unregister(jvmMemoryHeapSizeInit, context);
        server.unregister(jvmMemoryHeapUsed, context);
        server.unregister(jvmMemoryHeapCommitted, context);
        server.unregister(jvmMemoryHeapMaxSize, context);

        server.unregister(jvmMemoryNonHeapSizeInit, context);
        server.unregister(jvmMemoryNonHeapUsed, context);
        server.unregister(jvmMemoryNonHeapCommitted, context);
        server.unregister(jvmMemoryNonHeapMaxSize, context);

        server.unregister(jvmThreadingLiveThreads, context);
        server.unregister(jvmThreadingPeakThreads, context);
        server.unregister(jvmThreadingTotalThreads, context);

        server.unregister(summaryDataFields, context);
        server.unregister(summaryData, context);
        server.unregister(summaryDataNotificationInterval, context);
        server.unregister(summaryDataGatherInterval, context);
    }

    public void addJvmManagementBean(JvmManagementBean jvmManagementBean) {
        bean = jvmManagementBean;
    }

    class JVMClassLoadingLong extends MOScalar {
        private int operation;

        JVMClassLoadingLong(OID id, MOAccess access, Variable value, int operation) {
            super(id, access, value);
            this.operation = operation;
        }

        @Override
        public Variable getValue() {
            ClassLoadingMXBean bean = ManagementFactory.getClassLoadingMXBean();
            long classLoadingLong = 0;
            switch (operation) {
                case JVM_CLASSES_LOADED_OPER:
                    classLoadingLong = (long) bean.getLoadedClassCount();
                    break;
                case JVM_CLASSES_TOTAL_LOADED_OPER:
                    classLoadingLong = bean.getTotalLoadedClassCount();
                    break;
                case JVM_CLASSES_UNLOADED_OPER:
                    classLoadingLong = bean.getUnloadedClassCount();
                    break;
                default:
                    throw new RuntimeException(
                            "JMVClassLoadingLong incorrectly configured with unsupported operation: " + operation);
            }
            return new Counter64(classLoadingLong);
        }
    }

    abstract class JVMVerboseLevel extends MOScalar {

        JVMVerboseLevel(OID id, MOAccess access, Variable value) {
            super(id, access, value);
        }

        abstract boolean isVerbose();

        abstract void setVerbose(boolean verbose);

        @Override
        public Variable getValue() {
            if (isVerbose()) {
                return new Integer32(2);
            } else {
                return new Integer32(1);
            }
        }

        @Override
        public int setValue(Variable value) {
            if (value instanceof Integer32) {
                int verboseLevel = ((Integer32) value).getValue();
                setVerbose(verboseLevel == 2);
                return SnmpConstants.SNMP_ERROR_SUCCESS;
            }
            return SnmpConstants.SNMP_ERROR_BAD_VALUE;
        }
    }

    class JVMClassLoadingVerboseLevel extends JVMVerboseLevel {
        private ClassLoadingMXBean bean;

        JVMClassLoadingVerboseLevel(OID id, MOAccess access, Variable value) {
            super(id, access, value);
            this.bean = ManagementFactory.getClassLoadingMXBean();
        }

        boolean isVerbose() {
            return bean.isVerbose();
        }

        void setVerbose(boolean verbose) {
            bean.setVerbose(verbose);
        }
    }

    class JVMMemoryGCVerboseLevel extends JVMVerboseLevel {
        private MemoryMXBean bean;

        JVMMemoryGCVerboseLevel(OID id, MOAccess access, Variable value) {
            super(id, access, value);
            this.bean = ManagementFactory.getMemoryMXBean();
        }

        boolean isVerbose() {
            return bean.isVerbose();
        }

        void setVerbose(boolean verbose) {
            bean.setVerbose(verbose);
        }
    }

    class JVMMemoryGCCall extends MOScalar {
        JVMMemoryGCCall(OID id, MOAccess access, Variable value) {
            super(id, access, value);
        }

        @Override
        public int setValue(Variable value) {
            try {
                if (value instanceof Integer32) {
                    int gcCallValue = ((Integer32) value).getValue();
                    if (gcCallValue == 3) { // FIXME:  use a constant
                        MemoryMXBean bean = ManagementFactory.getMemoryMXBean();
                        bean.gc();
                        return SnmpConstants.SNMP_ERROR_SUCCESS;
                    } else {
                        return SnmpConstants.SNMP_ERROR_BAD_VALUE;
                    }
                }
                return SnmpConstants.SNMP_ERROR_BAD_VALUE;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    class JVMMemoryScalar extends MOScalar {
        private int operation;

        JVMMemoryScalar(OID id, MOAccess access, Variable value, int operation) {
            super(id, access, value);
            this.operation = operation;
        }

        @Override
        public Variable getValue() {
            MemoryMXBean bean = ManagementFactory.getMemoryMXBean();
            switch (operation) {
                case 1:
                    return new Gauge32(bean.getObjectPendingFinalizationCount());
                case JVM_MEMORY_HEAP_SIZE_INIT_OPER:
                    return new Counter64(bean.getHeapMemoryUsage().getInit());
                case JVM_MEMORY_HEAP_USED_OPER:
                    return new Counter64(bean.getHeapMemoryUsage().getUsed());
                case JVM_MEMORY_HEAP_COMMITTED_OPER:
                    return new Counter64(bean.getHeapMemoryUsage().getCommitted());
                case JVM_MEMORY_HEAP_MAX_SIZE_OPER:
                    return new Counter64(bean.getHeapMemoryUsage().getMax());
                case JVM_MEMORY_NONHEAP_SIZE_INIT_OPER:
                    return new Counter64(bean.getNonHeapMemoryUsage().getInit());
                case JVM_MEMORY_NONHEAP_USED_OPER:
                    return new Counter64(bean.getNonHeapMemoryUsage().getUsed());
                case JVM_MEMORY_NONHEAP_COMMITTED_OPER:
                    return new Counter64(bean.getNonHeapMemoryUsage().getCommitted());
                case JVM_MEMORY_NONHEAP_MAX_SIZE_OPER:
                    return new Counter64(bean.getNonHeapMemoryUsage().getMax());
                default:
                    throw new RuntimeException(
                            "JMVMemoryScalar incorrectly configured with unsupported operation: " + operation);
            }
        }
    }

    class JVMThreadingScalar extends MOScalar {
        private int operation;

        JVMThreadingScalar(OID id, MOAccess access, Variable value, int operation) {
            super(id, access, value);
            this.operation = operation;
        }

        @Override
        public Variable getValue() {
            ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            // FIXME:  used constants for the operation
            switch (operation) {
                case 1:
                    return new Integer32(bean.getThreadCount());
                case 2:
                    return new Integer32(bean.getPeakThreadCount());
                case 3:
                    return new Counter64(bean.getTotalStartedThreadCount());
                default:
                    throw new RuntimeException(
                            "JMVThreadingScalar incorrectly configured with unsupported operation: " + operation);
            }
        }
    }

    class SystemString extends MOScalar {
        private int operation;

        SystemString(OID id, MOAccess access, Variable value, int operation) {
            super(id, access, value);
            this.operation = operation;
        }

        @Override
        public Variable getValue() {
            String value = "";

            switch (operation) {
                case SUMMARY_DATA_FIELDS_OPER:
                    value = bean.getSummaryDataFields();
                    break;
                case SUMMARY_DATA_OPER:
                    value = bean.getSummaryData();
                    break;
                default:
                    throw new RuntimeException("SystemString incorrectly configured with unsupported operation: " + operation);
            }

            OctetString val = (OctetString) Utils.stringToVariable(value);
            return val;
        }
    }

}
