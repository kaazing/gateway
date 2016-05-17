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
import com.sun.jna.platform.win32.W32Errors;
import org.kaazing.gateway.server.windowsservice.ExtendedAdvapi32.HandlerEx;

/**
 * Part of JNA-based Windows Service program, based on the example I found in http://enigma2eureka.blogspot
 * .com/2011/05/writing-windows-service-in-java.html
 * and my own C++-based service work from earlier in 3.2 and 3.3.
 */
class SimpleServiceControlHandler implements HandlerEx {
    private final ISimpleService service;

    /**
     * Constructor.
     *
     * @param service
     */
    public SimpleServiceControlHandler(ISimpleService service) {
        this.service = service;
    }

    /**
     * @see org.kaazing.gateway.server.windowsservice.ExtendedAdvapi32.
     *      HandlerEx#serviceControlHandler(int, int, com.sun.jna.Pointer, com.sun.jna.Pointer)
     */
    @Override
    public int serviceControlHandler(int serviceControlCode,
                                     int eventType,
                                     Pointer eventData,
                                     Pointer context) {
        switch (serviceControlCode) {
            case ExtendedAdvapi32.SERVICE_CONTROL_INTERROGATE:
                return W32Errors.NO_ERROR;
            case ExtendedAdvapi32.SERVICE_CONTROL_STOP:
                service.stop();
                return W32Errors.NO_ERROR;
            default:
                return W32Errors.ERROR_CALL_NOT_IMPLEMENTED;
        }
    }

}
