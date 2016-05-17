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

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.W32Errors;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.win32.W32APIOptions;

/**
 * Based on the blog post http://enigma2eureka.blogspot.com/2011/05/writing-windows-service-in-java.html, with adjustments for
 * calls Kaazing makes beyond those described in the article.
 */
public interface ExtendedAdvapi32 extends Advapi32 {
    ExtendedAdvapi32 INSTANCE =
            (ExtendedAdvapi32) Native.loadLibrary("Advapi32",
                    ExtendedAdvapi32.class,
                    W32APIOptions.UNICODE_OPTIONS);

    class SERVICE_TABLE_ENTRY extends Structure {
        public String serviceName;
        public SERVICE_MAIN_FUNCTION serviceProc;
    }

    boolean StartServiceCtrlDispatcher(SERVICE_TABLE_ENTRY[] lpServiceTable);

    interface HandlerEx extends StdCallCallback {
        int serviceControlHandler(int serviceControlCode,
                                  int eventType,
                                  Pointer eventData,
                                  Pointer context);
    }

    class SERVICE_STATUS_HANDLE extends HANDLE {
        public SERVICE_STATUS_HANDLE() {
        }

        public SERVICE_STATUS_HANDLE(Pointer p) {
            super(p);
        }
    }

    SERVICE_STATUS_HANDLE RegisterServiceCtrlHandlerEx(String serviceName,
                                                       HandlerEx handler,
                                                       Object context);

    class SERVICE_STATUS extends Structure {
        public int serviceType = SERVICE_WIN32_OWN_PROCESS;
        public int currentState;
        public int controlsAccepted;
        public int win32ExitCode = W32Errors.NO_ERROR;
        public int serviceSpecificExitCode;
        public int checkPoint;
        public int waitHint;
    }

    boolean SetServiceStatus(SERVICE_STATUS_HANDLE serviceStatusHandle,
                             SERVICE_STATUS serviceStatus);

    // Codes used/returned by the SERVICE_STATUS structure.
    int SERVICE_WIN32_OWN_PROCESS = 0x00000010;

    // declare the Windows current-state codes (ServiceStatus.dwCurrentState)
    int SERVICE_CONTINUE_PENDING = 0x00000005;
    int SERVICE_PAUSE_PENDING = 0x00000006;
    int SERVICE_PAUSED = 0x00000007;
    int SERVICE_RUNNING = 0x00000004;
    int SERVICE_START_PENDNG = 0x00000002;  // the service is starting
    int SERVICE_STOP_PENDING = 0x00000003;
    int SERVICE_STOPPED = 0x00000001;   // is it really same as SERVICE_ACCEPT_STOP?

    // declare the controls accepted (dwControlsAccepted)
    int SERVICE_ACCEPT_NETBINDCHNAGE = 0x00000010;
    int SERVICE_ACCEPT_PARAMCHANGE = 0x00000008;
    int SERVICE_ACCEPT_PAUSE_CONTINUE = 0x00000002;  // can be paused and continued
    int SERVICE_ACCEPT_PRESHUTDOWN = 0x00000100;
    int SERVICE_ACCEPT_SHUTDOWN = 0x00000004;  // is it really same as SERVICE_RUNNING?
    int SERVICE_ACCEPT_STOP = 0x00000001;

    int SERVICE_CONTROL_SHUTDOWN = 0x00000005;  // Code sent by the system when SERVICE_ACCEPT_SHUTDOWN is set.)
    int SERVICE_CONTROL_STOP = 0x00000001;  // Code to send to the service when it has SERVICE_ACCEPT_STOP.

    // Must return NO_ERROR for this, not ERROR_CALL_NOT_IMPLEMENTED
    int SERVICE_CONTROL_INTERROGATE = 0x00000004;

}
