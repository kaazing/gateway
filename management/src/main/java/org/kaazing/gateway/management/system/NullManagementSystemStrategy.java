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
 * Do 'collect-only' management processing for those beans that run by 'gatherStats', i.e., the AbstractSummaryDataProviders
 * (CpuList, NicList, JVM, System).
 * <p/>
 * In 'collect-only' mode, for now we actually do NOTHING.
 */
public class NullManagementSystemStrategy implements ManagementSystemStrategy {

    public NullManagementSystemStrategy() {
    }

    @Override
    public void gatherStats(AbstractSystemManagementBean dataProvider) {
        // the null strategy for 'starting' is "stop any previous stat-gathering,
        // then do nothing else".
        dataProvider.stopGatherStats();
    }

    @Override
    public void continueGatherStats(AbstractSystemManagementBean dataProvider) {
        // the null strategy for 'continue' is "continue to do nothing".
        // Just in case, however, we'l tell our provider to stop.
        dataProvider.stopGatherStats();
    }

    @Override
    public void stopGatherStats(AbstractSystemManagementBean dataProvider) {
        // the null strategy for 'stop' is "we probably don't have to do
        // anything, but we'll tell our provider to stop anyway".
        dataProvider.stopGatherStats();
    }

    public String toString() {
        return "NULL_GATHER_STRATEGY";
    }
}

