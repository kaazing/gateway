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

import javax.security.auth.Subject;

import org.kaazing.gateway.service.ServiceProperties;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpHeaders;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpVersion;
import org.kaazing.gateway.transport.ws.util.WsUtils;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;

class TurnRestServiceHandler extends IoHandlerAdapter<HttpAcceptSession> {
    
    private ServiceProperties options;
    private TurnRestCredentialGenerator credentialGenerator;

    TurnRestServiceHandler(TurnRestCredentialGenerator credentialGenerator, ServiceProperties options) {
        this.credentialGenerator = credentialGenerator;
        this.options = options;
        credentialGenerator.init(options);
    }

    @Override
    protected void doSessionOpened(HttpAcceptSession session) throws Exception {        
        HttpMethod method = session.getMethod();
        String service = session.getParameter("service");
        
        if (method != HttpMethod.GET) {
            session.setStatus(HttpStatus.CLIENT_METHOD_NOT_ALLOWED);
            session.close(false);
            throw new IllegalArgumentException("HTTP method not allowed: " + method);        
        } else if (!service.equals("turn")) {
            session.setStatus(HttpStatus.CLIENT_BAD_REQUEST);
            session.close(false);
            throw new IllegalArgumentException("Unsupported/invalid service: " + service);
        }

        String parameterUsername = session.getParameter("username");
        Subject subject = session.getSubject();
        
        setResponseHeaders(session, parameterUsername, subject);
        
        TurnRestCredentials response = credentialGenerator.generateCredentials(parameterUsername, subject);
        String responseString = response.getResponseString(); 
        CharSequence responseChars = responseString;
        
        // get io buffer for file
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        IoBufferEx out = allocator.wrap(allocator.allocate(responseString.length())).setAutoExpander(allocator);
        out.put(responseString.getBytes());
        out.putString(responseChars, StandardCharsets.UTF_8.newEncoder());

        // add content length
        session.setWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, Integer.toString(out.remaining()));

        // write buffer and close session
        session.write(out);    
        session.close(false);
    }
    
    /**
     * 
     * @param session username passed in as HTTP parameter if present, null otherwise
     * @param parameterUsername
     */
    private void setResponseHeaders(HttpAcceptSession session, String parameterUsername, Subject subject) {
        if (parameterUsername != null) {
            session.setStatus(HttpStatus.SUCCESS_OK);
        } else if (subject != null) {
            String wsKey = session.getReadHeader("Sec-WebSocket-Key");
            String wsAccept = WsUtils.acceptHash(wsKey);
            
            session.setStatus(HttpStatus.INFO_SWITCHING_PROTOCOLS);
            session.setWriteHeader(HttpHeaders.HEADER_CONNECTION, "Upgrade");
            session.setWriteHeader("Sec-WebSocket-Accept", wsAccept);
            session.setWriteHeader("Server", "Kaazing Gateway");
            session.setWriteHeader(HttpHeaders.HEADER_UPGRADE, "websocket");
        } else {
            session.close(false);
            throw new IllegalArgumentException("Missing username parameter or Subject");
        }
        session.setVersion(HttpVersion.HTTP_1_1);
        session.setWriteHeader(HttpHeaders.HEADER_CONTENT_TYPE, "application/json");
    }
    
}
