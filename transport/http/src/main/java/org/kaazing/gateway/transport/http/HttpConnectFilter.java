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
package org.kaazing.gateway.transport.http;

import static java.lang.String.format;

import org.apache.mina.core.filterchain.IoFilter;
import org.kaazing.gateway.transport.http.bridge.filter.HttpCodecFilter;
import org.kaazing.gateway.transport.http.bridge.filter.HttpContentLengthAdjustmentFilter;
import org.kaazing.gateway.transport.http.bridge.filter.HttpxeProtocolFilter;

public enum HttpConnectFilter {
    
    CONTENT_LENGTH_ADJUSTMENT("http#content-length", new HttpContentLengthAdjustmentFilter()),
    CODEC("http#codec", new HttpCodecFilter(true)),
    PROTOCOL_HTTPXE("http#protocol[httpxe/1.1]", new HttpxeProtocolFilter(true));

    private final String filterName;
    private final IoFilter filter;

    HttpConnectFilter(String filterName, IoFilter filter) {
        this.filterName = filterName;
        this.filter = filter;
    }
    
    public String filterName() {
        return filterName;
    }
    
    public IoFilter filter() {
        if (filter == null) {
            throw new IllegalStateException(format("%s connector filter is not shared", filterName));
        }
        return filter;
    }
}