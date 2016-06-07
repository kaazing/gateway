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
import static org.kaazing.gateway.transport.http.HttpAcceptor.MERGE_REQUEST_LOGGER_NAME;

import org.apache.mina.core.filterchain.IoFilter;
import org.kaazing.gateway.transport.http.bridge.filter.HttpCodecFilter;
import org.kaazing.gateway.transport.http.bridge.filter.HttpContentLengthAdjustmentFilter;
import org.kaazing.gateway.transport.http.bridge.filter.HttpContentMessageInjectionFilter;
import org.kaazing.gateway.transport.http.bridge.filter.HttpHostHeaderFilter;
import org.kaazing.gateway.transport.http.bridge.filter.HttpMergeRequestFilter;
import org.kaazing.gateway.transport.http.bridge.filter.HttpNextProtocolHeaderFilter;
import org.kaazing.gateway.transport.http.bridge.filter.HttpOperationFilter;
import org.kaazing.gateway.transport.http.bridge.filter.HttpOriginHeaderFilter;
import org.kaazing.gateway.transport.http.bridge.filter.HttpOriginSecurityFilter;
import org.kaazing.gateway.transport.http.bridge.filter.HttpPathMatchingFilter;
import org.kaazing.gateway.transport.http.bridge.filter.HttpPersistenceFilter;
import org.kaazing.gateway.transport.http.bridge.filter.HttpProtocolCompatibilityFilter;
import org.kaazing.gateway.transport.http.bridge.filter.HttpProtocolFilter;
import org.kaazing.gateway.transport.http.bridge.filter.HttpSessionCleanupFilter;
import org.kaazing.gateway.transport.http.bridge.filter.HttpxeProtocolFilter;
import org.slf4j.LoggerFactory;

public enum HttpAcceptFilter {

    // dynamically added by HttpxeProtocolFilter
    CONTENT_LENGTH_ADJUSTMENT("http#content-length", new HttpContentLengthAdjustmentFilter()),

    // dynamically added by HttpProtocolCompatibilityFilter
    ELEVATE_EMULATED_REQUEST("http#elevateEmulatedRequest"),

    CODEC("http#codec", new HttpCodecFilter(false)),

    // always added per-session filter (new HttpSerializeRequestFilter(logger))
    HTTP_SERIALIZE_REQUEST_FILTER("http#serializeRequests"),

    MERGE_REQUEST("http#merge-request", new HttpMergeRequestFilter(LoggerFactory.getLogger(MERGE_REQUEST_LOGGER_NAME))),

    NEXT_PROTOCOL_HEADER("http#next-protocol", new HttpNextProtocolHeaderFilter()),

    ORIGIN_HEADER("http#origin", new HttpOriginHeaderFilter()),

    HOST_HEADER("http#host", new HttpHostHeaderFilter()),

    PROTOCOL_COMPATIBILITY("http#protocol-compatibility", new HttpProtocolCompatibilityFilter()),

    // dynamically added by HttpProtocolCompatibilityFilter
    CONDITIONAL_WRAPPED_RESPONSE("http#conditionalWrappedResponse", new HttpProtocolCompatibilityFilter.HttpConditionalWrappedResponseFilter()),

    PROTOCOL_HTTPXE("http#protocol[httpxe/1.1]", new HttpxeProtocolFilter(false)),

    CONTENT_MESSAGE_INJECTION("http#contentMessageInjection", new HttpContentMessageInjectionFilter()),

    PROTOCOL_HTTP("http#protocol[http/1.1]", new HttpProtocolFilter()),

    SESSION_CLEANUP("http#session-cleanup", new HttpSessionCleanupFilter()),

    NEXT_ADDRESS("http#next-address"),

    PATH_MATCHING("http#path-matching", new HttpPathMatchingFilter()),

    PERSISTENCE("http#persistence", new HttpPersistenceFilter()),

    OPERATION("http#operation", new HttpOperationFilter()),

    ORIGIN_SECURITY("http#origin-security", new HttpOriginSecurityFilter()),

    SUBJECT_SECURITY("http#subject-security");

    private final String filterName;
    private final IoFilter filter;
    
    HttpAcceptFilter(String filterName) {
        this(filterName, null);
    }
    
    HttpAcceptFilter(String filterName, IoFilter filter) {
        this.filterName = filterName;
        this.filter = filter;
    }
    
    public String filterName() {
        return filterName;
    }
    
    public IoFilter filter() {
        if (filter == null) {
            throw new IllegalStateException(format("%s acceptor filter is not shared", filterName));
        }
        return filter;
    }
}