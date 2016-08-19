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

import java.security.Key;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.Subject;

public class TestCredentialsGenerator implements TurnRestCredentialsGenerator {

    public static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    
    private long ttl;
    private Key sharedSecret;
    private char separator;

    @Override
    public void setCredentialsTTL(String ttl) {
        this.ttl = Long.valueOf(ttl);
    }

    @Override
    public void setSharedSecret(Key sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    @Override
    public void setUsernameSeparator(char separator) {
        this.separator = separator;
    }    
    
    @Override
    public TurnRestCredentials generate(Subject subject) {
        String username;
        char[] password;

        long unixTime = Instant.now().getEpochSecond() + ttl;
        
        try {    
            username = unixTime + Character.toString(this.separator) + subject.getPrincipals().iterator().next().getName();
        } catch (Exception e) {
            throw new IllegalArgumentException("Missing Subject");
        }

        byte[] key = sharedSecret.getEncoded();

        try {
            // Only HMAC_SHA1_ALGORITHM is supported in TestCredentialsGenerator
            Mac hmacSHA1 = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            SecretKeySpec signingKey = new SecretKeySpec(key, HMAC_SHA1_ALGORITHM);
            hmacSHA1.init(signingKey);
            password = Base64.getEncoder().encodeToString(hmacSHA1.doFinal(username.getBytes())).toCharArray();
        } catch (Exception e) {
            throw new RuntimeException("Password formation failed");  
        }
  
        TurnRestCredentials credentials = new TurnRestCredentials(username, password);
        return credentials;
    }

    @Override
    public void setAlgorithm(String algorithm) {
        
    }

}

