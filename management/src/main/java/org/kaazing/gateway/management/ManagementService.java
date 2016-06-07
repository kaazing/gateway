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

import org.kaazing.gateway.service.Service;

/**
 * A marker interface so that we can distinguish service instances that are being used for management from those that are being
 * used 'normally' as gateway services (e.g. echo, broadcast, etc.)  As of 3.3, there is only one ManagementService,
 * SnmpManagementService.  There may be more in future releases, if we have other protocols to support that actually require
 * service accepts and connects (JMX does not.)
 */
public interface ManagementService extends Service {
   String MANAGEMENT_SERVICE_MAP_NAME = "managementServiceMap";

    void init();
}
