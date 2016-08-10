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

import javax.security.auth.Subject;

public interface TurnRestCredentialsGenerator {
    
    /**
     * 
     * @param ttl the time in seconds for which the credentials are valid
     */
    public void setCredentialsTTL(String ttl);
    
    /**
     * 
     * @param alias the name of the certificate containing the shared
     * secret key
     */
    public void setSharedKey(String sharedKey);
    
    /**
     * 
     * @param separator character in the generated username separating
     * the expiration timestamp from the Subject username
     */
    public void setUsernameSeparator(char separator);

    /**
     * 
     * @param subject Subject representing the currently logged in user
     * @return TurnRestCredentials object 
     */
    public TurnRestCredentials generate(Subject subject);
}
