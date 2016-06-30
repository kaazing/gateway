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
import java.util.Arrays;
import org.kaazing.gateway.server.windowsservice.ExtendedAdvapi32.SERVICE_STATUS;
import org.kaazing.gateway.server.windowsservice.ExtendedAdvapi32.SERVICE_STATUS_HANDLE;

/**
 * Part of JNA-based Windows Service program, based on the example I found in http://enigma2eureka.blogspot
 * .com/2011/05/writing-windows-service-in-java.html
 * and my own C++-based service work from earlier in 3.2 and 3.3.
 */
class SimpleServiceMain implements SERVICE_MAIN_FUNCTION {
    private final ISimpleService simpleService;
    private final SimpleServiceControlHandler handler;
    private SERVICE_STATUS_HANDLE serviceStatusHandle;

    /**
     * Constructor.
     *
     * @param simpleService
     * @param handler
     */
    public SimpleServiceMain(ISimpleService simpleService,
                             SimpleServiceControlHandler handler) {
        this.simpleService = simpleService;
        this.handler = handler;
    }

    @Override
    public void ServiceMain(int argc, Pointer argv) {
        if (argc < 1 || argv == null) {
            // Missing the service name.
            return;
        }

        try {
            String[] args = argv.getStringArray(0, argc, true);
            String serviceName = args[0];
            String[] startParameters = Arrays.copyOfRange(args, 1, args.length);
            serviceStatusHandle =
                    ExtendedAdvapi32.INSTANCE.RegisterServiceCtrlHandlerEx(serviceName, handler, null);
            setServiceStatus(ExtendedAdvapi32.SERVICE_RUNNING,
                    ExtendedAdvapi32.SERVICE_ACCEPT_STOP |
                            ExtendedAdvapi32.SERVICE_ACCEPT_SHUTDOWN);
            simpleService.run(startParameters);
        } finally {
            setServiceStatus(ExtendedAdvapi32.SERVICE_STOPPED, 0);
        }
    }

    private void setServiceStatus(int currentState, int controlsAccepted) {
        SERVICE_STATUS serviceStatus = new SERVICE_STATUS();
        serviceStatus.currentState = currentState;
        serviceStatus.controlsAccepted = controlsAccepted;
        ExtendedAdvapi32.INSTANCE.SetServiceStatus(serviceStatusHandle, serviceStatus);
    }
}
