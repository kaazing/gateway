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
package org.kaazing.gateway.transport.http.bridge;

import java.util.Comparator;

public class HttpHeaderNameComparator implements Comparator<String> {

    public static final HttpHeaderNameComparator INSTANCE = new HttpHeaderNameComparator();

    @Override
    public int compare(String o1, String o2) {
        // Note: this is reverse ordering to deal with
        // explicit ordering of WebSocket handshake headers
        return -o1.compareToIgnoreCase(o2);
    }

    private HttpHeaderNameComparator() {
    	// singleton
    }
}
