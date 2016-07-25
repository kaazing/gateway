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

public class TestCredentials implements TurnRestCredentials {

    private String username;
    private String password;
    private long ttl;
    private String uris;
    private String responseString;
    
    TestCredentials(String username, String password, long ttl, String uris) {
        this.username = username;
        this.password = password;
        this.ttl = ttl;
        this.uris = uris;
        this.responseString = "{\"username\":\"" + this.username + "\",\"password\":\"" 
                + this.password + "\",\"ttl\":" + ttl + ",\"uris\":[" + this.uris + "]}";
    }
    
    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public long getTTL() {
        return this.ttl;
    }

    @Override
    public String getURIs() {
        return this.uris;
    }

    @Override
    public String getResponseString() {
        return this.responseString;
    }

}