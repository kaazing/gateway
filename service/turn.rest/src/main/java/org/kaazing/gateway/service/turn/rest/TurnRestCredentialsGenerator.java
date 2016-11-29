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

import javax.security.auth.Subject;

/**
 * Describes a class which can generate short term credentials 
 *
 */
public interface TurnRestCredentialsGenerator {
    
    /**
     * Sets the amount of time in which the credentials are available
     * 
     * @param ttl the time in seconds for which the credentials are valid
     */
    public void setCredentialsTTL(String ttl);
    
    /**
     * Sets the algorithm. In this context, the algorithm is used for generating message authenticity
     * 
     * @param algorithm the name of the algorithm. e.g.: HmacSHA1
     */
    public void setAlgorithm(String algorithm);

    /**
     * Sets the shared key. In this context, the key is shared between this service and the TURN server
     * 
     * @param sharedSecret the name of the certificate containing the shared
     * secret key
     */
    public void setSharedSecret(Key sharedSecret);
    /**
     * The separator used in the user name between the time stamp when credentials are no longer active, and the actual username.
     * 
     * @param separator character in the generated username separating
     * the expiration timestamp from the Subject username
     */
    public void setUsernameSeparator(char separator);

    /**
     * Generates short term Turn credentials for the currently logged user
     * 
     * @param subject Subject representing the currently logged in user
     * @return TurnRestCredentials object 
     */
    public TurnRestCredentials generate(Subject subject);
}
