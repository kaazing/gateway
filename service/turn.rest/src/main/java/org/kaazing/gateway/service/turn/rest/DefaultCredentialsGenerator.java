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

import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.Subject;

/**
 * Default implementation of the TurnRestCredentialsGenerator
 * It contains a standard implementation of the short term credentials 
 * as described in <a href='https://tools.ietf.org/html/draft-uberti-behave-turn-rest-00'>draft-uberti-behave-turn-rest-00</a> 
 */
public class DefaultCredentialsGenerator implements TurnRestCredentialsGenerator {

    public static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    private long ttl;
    private String sharedKey;
    private char separator;

    /**
     * @see org.kaazing.gateway.service.turn.rest.TurnRestCredentialsGenerator#setCredentialsTTL(java.lang.String)
     */
    @Override
    public void setCredentialsTTL(String ttl) {
        this.ttl = Long.valueOf(ttl);
    }


    /**
     * @see org.kaazing.gateway.service.turn.rest.TurnRestCredentialsGenerator#setSharedKey(java.lang.String)
     */
    @Override
    public void setSharedKey(String sharedKey) {
        this.sharedKey = sharedKey;
    }

    /**
     * @see org.kaazing.gateway.service.turn.rest.TurnRestCredentialsGenerator#setUsernameSeparator(char)
     */
    @Override
    public void setUsernameSeparator(char separator) {
        this.separator = separator;
    }

    /**
     * Generate TurnCredentials using the default algorithm, for generating them.
     * The algorithm will generate the short term user name and the password using the algorithm described in 
     * <a href='https://tools.ietf.org/html/draft-uberti-behave-turn-rest-00'>draft-uberti-behave-turn-rest-00</a>
     * <p>
     * The user name is generated based on the authenticated user's name.
     * @see org.kaazing.gateway.service.turn.rest.TurnRestCredentialsGenerator#generate(javax.security.auth.Subject)
     */
    @Override
    public TurnRestCredentials generate(Subject subject) {
        String username;
        char[] password;

        long unixTime = Instant.now().getEpochSecond() + ttl;

        try {
            username = unixTime + Character.toString(this.separator) + subject.getPrincipals().iterator().next().getName();
        } catch (Exception e) {
            throw new IllegalArgumentException("Missing Subject", e);
        }

        try {
            Mac hmacSHA1 = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            SecretKeySpec signingKey = new SecretKeySpec(this.sharedKey.getBytes(), HMAC_SHA1_ALGORITHM);
            hmacSHA1.init(signingKey);
            password = Base64.getEncoder().encodeToString(hmacSHA1.doFinal(username.getBytes())).toCharArray();
        } catch (Exception e) {
            throw new TurnServiceException("Password formation failed", e);
        }

        return new TurnRestCredentials(username, password);
    }

}
