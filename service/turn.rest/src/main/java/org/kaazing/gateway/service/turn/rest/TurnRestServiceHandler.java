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
package org.kaazing.gateway.service.turn.rest;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Arrays;

import javax.security.auth.Subject;

import org.kaazing.gateway.service.ServiceProperties;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpHeaders;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpVersion;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;

class TurnRestServiceHandler extends IoHandlerAdapter<HttpAcceptSession> {
    
    private ServiceProperties options;
    private KeyStore keystore;
    private TurnRestCredentialsGenerator credentialGenerator;
    private String uris;

    TurnRestServiceHandler(ServiceProperties options, KeyStore keystore, TurnRestCredentialsGenerator credentialGenerator, String uris) {
        this.options = options;
        this.keystore = keystore;
        this.credentialGenerator = credentialGenerator;
        this.uris = uris;
    }

    @Override
    protected void doSessionOpened(HttpAcceptSession session) throws Exception {        
        HttpMethod method = session.getMethod();
        String service = session.getParameter("service");
        
        String ttl = options.get("credentials.ttl");
        Certificate alias = keystore.getCertificate(options.get("key.alias"));
        char separator = options.get("username.separator").charAt(0);
                
        if (method != HttpMethod.GET) {
            session.setStatus(HttpStatus.CLIENT_METHOD_NOT_ALLOWED);
            session.close(false);
            throw new IllegalArgumentException("HTTP method not allowed: " + method);        
        } else if (!service.equals("turn")) {
            session.setStatus(HttpStatus.CLIENT_BAD_REQUEST);
            session.close(false);
            throw new IllegalArgumentException("Unsupported/invalid service: " + service);
        }

        session.setVersion(HttpVersion.HTTP_1_1);
        session.setWriteHeader(HttpHeaders.HEADER_CONTENT_TYPE, "application/json");
        
        Subject subject = session.getSubject();
        
        if (ttl == null) {
            ttl = session.getParameter("Max-Age");
        }

        TurnRestCredentials credentials = null;
        if (credentialGenerator != null) {
            credentialGenerator.setCredentialsTTL(ttl);
            credentialGenerator.setKeyAlias(alias);
            credentialGenerator.setUsernameSeparator(separator);
            credentials = credentialGenerator.generate(subject);
        }
        
        String username = credentials.getUsername();
        char[] password = credentials.getPassword();
        String response = TurnRestJSONResponse.createResponse(username, password, ttl, this.uris); 
        CharSequence responseChars = response;
        Arrays.fill(password, '0');
        
        // get io buffer for file
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        IoBufferEx out = allocator.wrap(allocator.allocate(response.length())).setAutoExpander(allocator);
        out.put(response.getBytes());
        out.putString(responseChars, StandardCharsets.UTF_8.newEncoder());
       
        // add content length
        session.setWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, Integer.toString(out.remaining()));

        // write buffer and close session
        session.write(out);    
        session.close(false);
    }
    
}
