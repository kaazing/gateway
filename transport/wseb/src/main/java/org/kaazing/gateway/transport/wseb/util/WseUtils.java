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
package org.kaazing.gateway.transport.wseb.util;

import java.net.URI;

public class WseUtils {

    public static final String HEADER_X_WEBSOCKET_PROTOCOL = "X-WebSocket-Protocol";

    /**
     * @param wsURI  WebSocket URI, e.g. ws://acme.com:8080/path"
     * @param downstreamOrUpstreamURI  e.g. http://acme.com:8080/path/;e/ub/AwlevBRAZsv5fwl1USiGawmxyyA4IUVz?query
     * @return
     */
    public static boolean pathPrefixMatches(URI wsURI, URI downstreamOrUpstreamURI) {
        String createPath = wsURI.getPath();
        String path = downstreamOrUpstreamURI.getPath();
        if (!path.startsWith(createPath)) {
            return false;
        }
        return true;
    }

}

