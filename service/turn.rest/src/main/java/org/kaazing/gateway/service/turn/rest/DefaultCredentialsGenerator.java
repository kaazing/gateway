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

import javax.security.auth.Subject;

import org.kaazing.gateway.util.turn.TurnUtils;
/**
 * This class uses the keystore of the gateway, as defined in the security/keystore/file tag.
 * To import passwords in this keystore, use the following keytool command:
 * keytool -importpassword -storetype JCEKS -alias password_alias -keystore keystore_filename.db
 *
 */
public class DefaultCredentialsGenerator implements TurnRestCredentialsGenerator {

    private long ttl;
    private Key sharedSecret;
    private char separator;

    private String algorithm;

    @Override
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

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
        username = unixTime + Character.toString(this.separator) + subject.getPrincipals().iterator().next().getName();


        password = TurnUtils.generatePassword(username, sharedSecret, algorithm);
        return  new TurnRestCredentials(username, password);
    }

}
