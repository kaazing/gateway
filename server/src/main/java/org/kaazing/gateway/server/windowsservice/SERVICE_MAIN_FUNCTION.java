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

import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary.StdCallCallback;

/**
 * Part of JNA-based Windows Service program, based on the example I found in http://enigma2eureka.blogspot
 * .com/2011/05/writing-windows-service-in-java.html
 * and my own C++-based service work from earlier in 3.2 and 3.3.
 */
public interface SERVICE_MAIN_FUNCTION extends StdCallCallback {
    /**
     * ServiceMain is the main method of the service.  It should return only once the service is stopped.
     *
     * @param dwArgc
     * @param argv   A pointer to an array (of length dwArgc) of pointers to strings.
     */
    void ServiceMain(int dwArgc, Pointer argv);

}
