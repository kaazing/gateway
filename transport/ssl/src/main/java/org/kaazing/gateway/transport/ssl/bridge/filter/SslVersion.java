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
package org.kaazing.gateway.transport.ssl.bridge.filter;

enum SslVersion {
    SSL_3_0(3, 0),
    TLS_1_0(3, 1),
    TLS_1_1(3, 2),
    TLS_1_2(3, 3);

    private final int major;
    private final int minor;

    SslVersion(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    public int major() {
        return major;
    }

    public int minor() {
        return minor;
    }

    public static SslVersion decode(int version) {
        int majorVersion = ((version >> 8) & 0xff);
        int minorVersion = ((version >> 0) & 0xff);

        switch (majorVersion) {
            case 3:
                switch (minorVersion) {
                    case 0:
                        return SSL_3_0;
                    case 1:
                        return TLS_1_0;
                    case 2:
                        return TLS_1_1;
                    case 3:
                        return TLS_1_2;
                    default:
                        throw new IllegalArgumentException("Unrecognized minor version: " + minorVersion);
                }
            default:
                throw new IllegalArgumentException("Unrecognized major version: " + majorVersion);
        }
    }
}
