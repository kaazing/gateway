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
public interface JvmManagementBean extends SystemManagementBean {
    String[] SUMMARY_DATA_FIELD_LIST =
            new String[]{"classesLoaded", "totalClassesLoaded", "totalClassesUnloaded",
                    "memHeapInitSize", "memHeapUsed", "memHeapCommitted", "memHeapMaxSize",
                    "memNonHeapInitSize", "memNonHeapUsed", "memNonHeapCommitted", "memNonHeapMaxSize",
                    "threadingLiveThreads", "threadingPeakThreads", "threadingTotalThreads"};

    int SUMMARY_DATA_CLASSES_LOADED_INDEX = 0;
    int SUMMARY_DATA_TOTAL_CLASSES_LOADED_INDEX = 1;
    int SUMMARY_DATA_TOTAL_CLASSES_UNLOADED_INDEX = 2;
    int SUMMARY_DATA_MEM_HEAP_INIT_SIZE_INDEX = 3;
    int SUMMARY_DATA_MEM_HEAP_USED_INDEX = 4;
    int SUMMARY_DATA_MEM_HEAP_COMMITTED_INDEX = 5;
    int SUMMARY_DATA_MEM_HEAP_MAX_SIZE_INDEX = 6;
    int SUMMARY_DATA_MEM_NONHEAP_INIT_SIZE_INDEX = 7;
    int SUMMARY_DATA_MEM_NONHEAP_USED_INDEX = 8;
    int SUMMARY_DATA_MEM_NONHEAP_COMMITTED_INDEX = 9;
    int SUMMARY_DATA_MEM_NONHEAP_MAX_SIZE_INDEX = 10;
    int SUMMARY_DATA_THREADING_LIVE_THREADS_INDEX = 11;
    int SUMMARY_DATA_THREADING_PEAK_THREADS_INDEX = 12;
    int SUMMARY_DATA_THREADING_TOTAL_THREADS_INDEX = 13;

    void init();
}
