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

import org.kaazing.gateway.management.ManagementStrategy;

/**
 * "Strategy" object to implement management processing. This particular one implements the 'gather' strategy (basically, "do
 * everything" or "do nothing").
 */
public interface ManagementSystemStrategy extends ManagementStrategy {

    // Do the task to gather stats now (i.e. run.execute()
    void gatherStats(AbstractSystemManagementBean dataProvider);

    // schedule the next round of gathering stats
    void continueGatherStats(AbstractSystemManagementBean dataProvider);

    // Cancel any pending task to gather stats
    void stopGatherStats(AbstractSystemManagementBean dataProvider);
}
