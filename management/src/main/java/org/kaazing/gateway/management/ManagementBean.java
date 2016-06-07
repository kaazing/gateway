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
package org.kaazing.gateway.management;

import java.util.List;
import org.kaazing.gateway.management.context.ManagementContext;

/**
 * Interface that all management beans must support. Initiailly, all beans must support providing a string of "summary" data at
 * some interval, the changes to the bean since the end of the previous interval.
 */
public interface ManagementBean {

    /**
     * Return a stringified version of the summary data. The precise form of the data is implementation-dependent.
     */
    String getSummaryData();

    /**
     * Return a JSON-stringified version of the summary-data field list.
     *
     * @return
     */
    String getSummaryDataFields();

    /**
     * Return the list of listeners
     *
     * @return
     */
    List<SummaryDataListener> getSummaryDataListeners();

    /**
     * Add an object that will be called with the stringified summary data whenever the summary interval expires.
     */
    void addSummaryDataListener(SummaryDataListener listener);

    /**
     * Remove a summary-data listener.
     *
     * @param listener
     */
    void removeSummaryDataListener(SummaryDataListener listener);

    /**
     * Return the specific summary interval being used by this management bean.
     *
     * @return
     */
    SummaryManagementInterval getSummaryInterval();

    /**
     * Return true if the object is currently considered 'dirty' (i.e., it will have data to send out during the next summary
     * notification), else false.
     *
     * @return
     */
    boolean isDirty();

    /**
     * Return the ManagementContext that's driving management of this bean (this is primarily an optimization so all levels of
     * management beans have fast access to the management context when possible).
     */
    ManagementContext getManagementContext();

    /**
     * Run a task on a management-specific thread (i.e., NOT on the IO threads used to process messages).
     *
     * @param runnable
     */
    void runManagementTask(Runnable runnable);

}
