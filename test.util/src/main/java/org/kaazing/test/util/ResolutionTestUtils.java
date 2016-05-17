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
package org.kaazing.test.util;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public final class ResolutionTestUtils {

    private ResolutionTestUtils() {
        // not to be instantiated
    }

    public static String getLoopbackInterface() {
        String networkInterface = "";
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface resolvedNetworkInterface = interfaces.nextElement();
                if (resolvedNetworkInterface.isLoopback()) {
                    networkInterface = resolvedNetworkInterface.getDisplayName();
                    break;
                }
            }
            if (networkInterface.equals("")) {
                throw new RuntimeException("No loopback interfaces could be found");
            }
        } catch (SocketException socketEx) {
            throw new RuntimeException("No interfaces could be found");
        }
        return networkInterface;
    }

}
