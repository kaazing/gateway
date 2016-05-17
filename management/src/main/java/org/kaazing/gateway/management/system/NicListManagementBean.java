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
 * the list of NIC management beans. This is primarily here because we want to have a separate summary set of data for the list
 * of NIC data, not mix with CPU/core or more common system information.
 */
public interface NicListManagementBean extends SystemManagementBean {

    String getNetInterfaceNames();

    NicManagementBean[] getNicManagementBeans();
}
