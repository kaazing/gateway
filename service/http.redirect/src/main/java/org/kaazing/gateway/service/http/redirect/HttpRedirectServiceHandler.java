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
package org.kaazing.gateway.service.http.redirect;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.slf4j.Logger;

class HttpRedirectServiceHandler extends IoHandlerAdapter<HttpAcceptSession> {
    private final Logger logger;
    private String location;
    private HttpStatus statusCode;
    private String cacheControl;

    private static final DateFormat RFC822_FORMAT_PATTERN =
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
    static {
        RFC822_FORMAT_PATTERN.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    HttpRedirectServiceHandler(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void doSessionCreated(HttpAcceptSession session) throws Exception {
        // NOOP no license check needed
    }

    @Override
    public void doSessionClosed(HttpAcceptSession session) throws Exception {
        // NOOP no license check needed
    }

    @Override
    protected void doExceptionCaught(HttpAcceptSession session, Throwable cause) throws Exception {
        // trigger sessionClosed to update connection capabilities accordingly
        session.close(true);
    }

    @Override
    protected void doSessionOpened(HttpAcceptSession session) throws Exception {
        // Get the method used for this request; if not a GET, refuse the
        // request (KG-1233).
        // (KG-11211) Enabling HEAD method too.
        HttpMethod method = session.getMethod();
        if (method != HttpMethod.GET && method != HttpMethod.HEAD) {
            logger.warn("Wrong Http method used: " + method + ". Only GET or HEAD accepted");
            session.close(false);
            return;
        }

        session.setStatus(getStatusCode());
        session.setWriteHeader("Location", getLocation());
        if (getCacheControl() != null) {
            session.setWriteHeader("Cache-Control", getCacheControl());
            session.setWriteHeader("Expires", getExpiresHeader());
        }
        session.close(false);
    }

    private String getExpiresHeader() {
        return RFC822_FORMAT_PATTERN.format(new Date(0));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HttpRedirectServiceHandler [");
        sb.append("location='").append(location).append("'");
        sb.append(", cache-control='").append(cacheControl).append("'");
        sb.append(", status-code=").append(statusCode);
        sb.append("]");
        return sb.toString();
    }

    String getLocation() {
        return location;
    }

    void setLocation(String location) {
        this.location = location;
    }

    HttpStatus getStatusCode() {
        return statusCode;
    }

    void setStatusCode(HttpStatus statusCode) {
        this.statusCode = statusCode;
    }

    String getCacheControl() {
        return cacheControl;
    }

    void setCacheControl(String cacheControl) {
        this.cacheControl = cacheControl;
    }
}
