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

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.http.HttpRealmInfo;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;

public class TokenHttpChallengeFactory extends HttpChallengeFactoryAdapter {

    public static final String AUTH_SCHEME = "Token";

    @Override
    protected String getAuthenticationScheme() {
        return AUTH_SCHEME;
    }

    /* Override the HttpChallengeFactoryAdapter makeChallengeString method,
     * since that method automatically adds the "realm=" challenge parameter,
     * and for the suis generis "Application Token" authentication scheme --
     * explicitly designed to be opaque -- we want to stay out of the way
     * of the custom opaque tokens.
     */
    @Override
    protected String makeChallengeString(ResourceAddress address, HttpRealmInfo realm, Object... params) {
        StringBuilder sb = new StringBuilder();
        String challengeScheme = realm.getChallengeScheme();

        if (isApplication(challengeScheme)) {
            sb.append(AUTH_SCHEME_APPLICATION_PREFIX);
        }

        sb.append(getAuthenticationScheme());

        if (params != null) {
            // Don't forget the additional parameters (KG-2191)
            for (Object obj : params) {
                sb.append(" ").append(obj);
            }
        }

        return sb.toString();
    }
}

