/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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

package org.kaazing.gateway.security.connector.auth;

import java.util.Arrays;

/**
 * A challenge response contains a character array representing the response to the server,
 * and a reference to the next challenge handler to handle any further challenges for the request.
 *
 */
public class ChallengeResponse {

    private char[] credentials;
    private ChallengeHandler nextChallengeHandler;

    /**
     * Constructor from a set of credentials to send to the server in an 'Authorization:' header
     * and the next challenge handler responsible for handling any further challenges for the request.
     *
     * @param credentials a set of credentials to send to the server in an 'Authorization:' header
     * @param nextChallengeHandler the next challenge handler responsible for handling any further challenges for the request.
     */
    public ChallengeResponse(char[] credentials, ChallengeHandler nextChallengeHandler) {
        this.credentials = credentials;
        this.nextChallengeHandler = nextChallengeHandler;
    }

    /**
     * Return the next challenge handler responsible for handling any further challenges for the request.
     *
     * @return the next challenge handler responsible for handling any further challenges for the request.
     */
    public ChallengeHandler getNextChallengeHandler() {
        return nextChallengeHandler;
    }

    /**
     * Return a set of credentials to send to the server in an 'Authorization:' header.
     *
     * @return a set of credentials to send to the server in an 'Authorization:' header.
     */
    public char[] getCredentials() {
        return credentials;
    }

    /**
     * Establish the credentials for this response.
     *
     * @param credentials the credentials to be used for this challenge response.
     */
    public void setCredentials(char[] credentials) {
        this.credentials = credentials;
    }

    /**
     * Establish the next challenge handler responsible for handling any further challenges for the request.
     *
     * @param nextChallengeHandler the next challenge handler responsible for handling any further challenges for the request.
     */
    public void setNextChallengeHandler(ChallengeHandler nextChallengeHandler) {
        this.nextChallengeHandler = nextChallengeHandler;
    }

    /**
     * Clear the credentials of this response.
     * <p/>
     * Calling this method once the credentials have been communicated to the network layer
     * protects credentials in memory.
     */
    public void clearCredentials() {
        if (getCredentials() != null) {
            Arrays.fill(getCredentials(), (char) 0);
        }
    }
}
