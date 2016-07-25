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

import static org.kaazing.gateway.service.ServiceProperties.LIST_SEPARATOR;

import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.Subject;

import org.kaazing.gateway.service.ServiceProperties;

public class TestCredentialGenerator implements TurnRestCredentialGenerator {

    public static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    public static final long ttl = 86400;
    
    private String secret;
    private String symbol;
    private String uris;
    
    @Override
    public void init(ServiceProperties options) {
        this.secret = options.get("secret");
        this.symbol = options.get("symbol");
        
        StringBuilder u = new StringBuilder();
        for (String uri: options.getNested("uris").get(0).get("uri").split(LIST_SEPARATOR)) {
            u.append("\"").append(uri).append("\",");
        }
        u.setLength(u.length() - 1);
        this.uris = u.toString();
    }

    @Override
    public TurnRestCredentials generateCredentials(String parameterUsername, Subject subject) {
        String username;
        String password;

        long unixTime = Instant.now().getEpochSecond() + ttl;
        
        try {    
            if (parameterUsername != null) {
                username = unixTime + this.symbol + parameterUsername;
            } else  {
                username = subject.getPrincipals().iterator().next().getName();
            } 
        } catch (Exception e) {
            throw new IllegalArgumentException("Missing username parameter or Subject");
        }
          
        try {
            Mac hmacSHA1 = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            SecretKeySpec signingKey = new SecretKeySpec(this.secret.getBytes(), HMAC_SHA1_ALGORITHM);
            hmacSHA1.init(signingKey);
            password = Base64.getEncoder().encodeToString(hmacSHA1.doFinal(username.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Password formation failed.");  
        }
  
        TestCredentials response = new TestCredentials(username, password, ttl, uris);
        return response;
    }    
}