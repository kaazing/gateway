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

/**
 * Short term credentials for authenticating to a TURN server
 *
 */
public class TurnRestCredentials {

    private String username;
    private char[] password;
    
    /**
     * Properties are only set via constructor
     * @param username - The user name used for authentication
     * @param password - The password used for authentication
     */
    TurnRestCredentials(String username, char[] password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Retrieves the user name used for authentication
     * @return
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Retrieves the password used for authentication
     * @return
     */
    public char[] getPassword() {
        return this.password;
    }

}
