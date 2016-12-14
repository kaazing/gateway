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
package org.kaazing.gateway.service.turn.rest.internal;

import java.util.Arrays;

import javax.security.auth.Subject;

import org.kaazing.gateway.service.turn.rest.TurnRestCredentials;
import org.kaazing.gateway.service.turn.rest.TurnRestCredentialsGenerator;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpHeaders;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpVersion;
import org.kaazing.gateway.util.turn.TurnException;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TurnRestServiceHandler extends IoHandlerAdapter<HttpAcceptSession> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TurnRestServiceHandler.class);
    private TurnRestCredentialsGenerator credentialGenerator;
    private String urls;

    private String ttl;

    TurnRestServiceHandler(String ttl, TurnRestCredentialsGenerator credentialGenerator,
            String urls) {

        this.ttl = ttl;
        this.credentialGenerator = credentialGenerator;
        this.urls = urls;

    }

    @Override
    protected void doSessionOpened(HttpAcceptSession session) throws Exception {
        HttpMethod method = session.getMethod();
        String service = session.getParameter("service");

        if (method != HttpMethod.GET) {
            session.setStatus(HttpStatus.CLIENT_METHOD_NOT_ALLOWED);
            session.close(false);
            throw new IllegalArgumentException("HTTP method not allowed: " + method);
        } else if (!"turn".equals(service)) {
            session.setStatus(HttpStatus.CLIENT_BAD_REQUEST);
            session.close(false);
            throw new IllegalArgumentException("Unsupported/invalid service: " + service);
        }

        session.setVersion(HttpVersion.HTTP_1_1);
        session.setWriteHeader(HttpHeaders.HEADER_CONTENT_TYPE, "application/json");

        String username = null;
        char[] password = null;
        if (credentialGenerator != null) {
            Subject subject = session.getSubject();
            if (subject == null) {
                throw new TurnException("Subject is null");
            }
            credentialGenerator.setCredentialsTTL(ttl);
            TurnRestCredentials credentials = credentialGenerator.generate(subject);
            username = credentials.getUsername();
            password = credentials.getPassword();
            LOGGER.info(String.format("%s Generated username: %s", session, username));
        }

        String response = TurnRestJSONResponse.createResponse(username, password, ttl, this.urls);

        if (password != null) {
            Arrays.fill(password, '0');
        }

        // get io buffer for file
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        IoBufferEx out = allocator.wrap(allocator.allocate(response.length())).setAutoExpander(allocator);
        out.put(response.getBytes());
        out.flip();
        // add content length
        session.setWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, Integer.toString(out.remaining()));
        session.setWriteHeader(HttpHeaders.HEADER_MAX_AGE, ttl);

        // write buffer and close session
        session.write(out);
        session.close(false);
    }

}
