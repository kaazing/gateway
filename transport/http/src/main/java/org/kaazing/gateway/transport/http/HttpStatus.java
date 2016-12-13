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

import java.util.HashMap;
import java.util.Map;

public enum HttpStatus {
    // 1xx
    INFO_CONTINUE(100, "Continue"),
    INFO_SWITCHING_PROTOCOLS(101, "Switching Protocols"),

    // 2xx
    SUCCESS_OK(200, "OK"),
    SUCCESS_CREATED(201, "Created"),
    SUCCESS_ACCEPTED(202, "Accepted"),
    SUCCESS_NON_AUTHORATIVE_INFORMATION(203, "Non-Authorative Information"),
    SUCCESS_NO_CONTENT(204, "No Content"),
    SUCCESS_RESET_CONTENT(205, "Reset Content"),
    SUCCESS_PARTIAL_CONTENT(206, "Partial Content"),

    // 3xx
    REDIRECT_MULTIPLE_CHOICES(300, "Multiple Choices"),
    REDIRECT_MOVED_PERMANENTLY(301, "Moved Permanently"),
    REDIRECT_FOUND(302, "Found"),
    REDIRECT_SEE_OTHER(303, "See Other"),
    REDIRECT_NOT_MODIFIED(304, "Not Modified"),
    REDIRECT_USE_PROXY(305, "Use Proxy"),
    REDIRECT_TEMPORARY(307, "Temporary Redirect"),

    // 4xx
    CLIENT_BAD_REQUEST(400, "Bad Request"),
    CLIENT_UNAUTHORIZED(401, "Unauthorized"),
    CLIENT_PAYMENT_REQUIRED(402, "Payment Required"),
    CLIENT_FORBIDDEN(403, "Forbidden"),
    CLIENT_NOT_FOUND(404, "Not Found"),
    CLIENT_METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    CLIENT_NOT_ACCEPTABLE(406, "Not Acceptable"),
    CLIENT_PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required"),
    CLIENT_REQUEST_TIMEOUT(408, "Request Timeout"),
    CLIENT_CONFLICT(409, "Conflict"),
    CLIENT_GONE(410, "Gone"),
    CLIENT_LENGTH_REQUIRED(411, "Length Required"),
    CLIENT_PRECONDITION_FAILED(412, "Precondition Failed"),
    CLIENT_REQUEST_ENTITY_TOO_LARGE(413, "Request Entity Too Large"),
    CLIENT_REQUEST_URI_TOO_LONG(414, "Request URI Too Long"),
    CLIENT_UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
    CLIENT_REQUEST_RANGE_NOT_SATISFIABLE(416, "Request Range Not Satisfiable"),
    CLIENT_EXPECTATION_FAILED(417, "Expectation Failed"),
    CLIENT_UPGRADE_REQUIRED(426, "Upgrade Required"),

    // 5xx
    SERVER_INTERNAL_ERROR(500, "Internal Error"),
    SERVER_NOT_IMPLEMENTED(501, "Not Implemented"),
    SERVER_BAD_GATEWAY(502, "Bad Gateway"),
    SERVER_SERVICE_UNAVAILABLE(503, "Service Unavailable"),
    SERVER_GATEWAY_TIMEOUT(504, "Gateway Timeout"),
    SERVER_VERSION_NOT_SUPPORTED(505, "Version Not Supported"),
    SERVER_LOOP_DETECTED(508, "Loop detected");

    // status code string --> HttpStatus (for e.g. "200" -> SUCCESS_OK)
    private static final Map<String, HttpStatus> HTTP_STATUS_MAP = new HashMap<>();
    static {
        for(HttpStatus httpStatus : HttpStatus.values()) {
            HTTP_STATUS_MAP.put(String.valueOf(httpStatus.code), httpStatus);
        }
    }

    public static HttpStatus getHttpStatus(String code) {
        HttpStatus status = HTTP_STATUS_MAP.get(code);
        if (status != null) {
            return status;
        }
        throw new IllegalArgumentException("Unrecognized status code: " + code);
    }

    private final int code;
    private final String reason;
    private final String status;

    HttpStatus(int code, String reason) {
        this.code = code;
        this.reason = reason;
        this.status = String.valueOf(code);
    }

    public int code() {
        return code;
    }

    public String reason() {
        return reason;
    }

    @Override
    public String toString() {
        return status;
    }
}
