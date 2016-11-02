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
package org.kaazing.gateway.transport.http.security.auth.challenge;

import static org.kaazing.gateway.transport.http.bridge.filter.HttpSubjectSecurityFilter.AUTH_SCHEME_APPLICATION_PREFIX;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.http.HttpRealmInfo;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;

public class DispatchHttpChallengeFactory implements HttpChallengeFactory {

    // public until unit test is moved
    public static Map<String, HttpChallengeFactory> challengeFactoriesByAuthScheme =
            new ConcurrentHashMap<>(5);

    // public until unit test is moved
    public static void clear() {
        challengeFactoriesByAuthScheme =
                new ConcurrentHashMap<>(5);
    }

    public void register(String authScheme, HttpChallengeFactory factory) {
        if (authScheme == null) {
            throw new NullPointerException("authScheme");
        }
        if (factory == null) {
            throw new NullPointerException("factory");
        }
        challengeFactoriesByAuthScheme.put(authScheme, factory);
    }



    @Override
    public HttpResponseMessage createChallenge(HttpRequestMessage httpRequestMessage,
                                               HttpRealmInfo realm,
                                               Object... params) {

        if ( httpRequestMessage == null || httpRequestMessage.getLocalAddress() == null) {
            throw new NullPointerException("Cannot issue challenge; httpRequestMessage not provided.");
        }

        String authScheme = realm.getChallengeScheme();

        HttpChallengeFactory factory = lookup(authScheme);
        if (factory == null) {
            throw new IllegalStateException("Cannot issue a challenge for authentication scheme: " + authScheme);
        }

        return factory.createChallenge(httpRequestMessage, realm, params);
    }

    // public until unit test is moved
    public HttpChallengeFactory lookup(String authScheme) {
        HttpChallengeFactory result;

        if (authScheme == null) return null;

        result = challengeFactoriesByAuthScheme.get(authScheme);
        if (result == null) {

            if (authScheme.startsWith(AUTH_SCHEME_APPLICATION_PREFIX)) {
                authScheme = authScheme.replaceFirst(AUTH_SCHEME_APPLICATION_PREFIX, "");
            }

            result = challengeFactoriesByAuthScheme.get(authScheme);
        }
        return result;
    }
}

