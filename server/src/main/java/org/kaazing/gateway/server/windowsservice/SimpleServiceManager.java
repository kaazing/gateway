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
package org.kaazing.gateway.server.windowsservice;

import org.kaazing.gateway.server.windowsservice.ExtendedAdvapi32.SERVICE_TABLE_ENTRY;

/**
 * Part of JNA-based Windows Service program, based on the example I found in http://enigma2eureka.blogspot
 * .com/2011/05/writing-windows-service-in-java.html
 * and my own C++-based service work from earlier in 3.2 and 3.3.
 */
public final class SimpleServiceManager {

    private SimpleServiceManager() {
    }

    public static void runSimpleService(ISimpleService service) {
        SimpleServiceControlHandler handler = new SimpleServiceControlHandler(service);
        SimpleServiceMain serviceMain = new SimpleServiceMain(service, handler);
        SERVICE_TABLE_ENTRY entry = new SERVICE_TABLE_ENTRY();
        entry.serviceName = "";  // Should we fix this?
        entry.serviceProc = serviceMain;
        SERVICE_TABLE_ENTRY[] serviceTable =
                (SERVICE_TABLE_ENTRY[]) entry.toArray(2);
        ExtendedAdvapi32.INSTANCE.StartServiceCtrlDispatcher(serviceTable);
    }
}
