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
import static org.kaazing.gateway.transport.http.bridge.filter.HttpSubjectSecurityFilter.AUTH_SCHEME_NEGOTIATE;

import java.net.URI;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.http.HttpRealmInfo;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;

public class NegotiateHttpChallengeFactory extends HttpChallengeFactoryAdapter {

    @Override
    protected String getAuthenticationScheme() {
        return AUTH_SCHEME_NEGOTIATE;
    }

    @Override
    protected String makeChallengeString(ResourceAddress address, HttpRealmInfo realm, Object... params) {
        String challengeScheme = realm.getChallengeScheme();

        boolean application = isApplication(challengeScheme);

        final String authConnectValue = address.getOption(HttpResourceAddress.AUTHENTICATION_CONNECT);
        URI authConnect = authConnectValue == null ? null : URI.create(authConnectValue);
        String authIdentifier = address.getOption(HttpResourceAddress.AUTHENTICATION_IDENTIFIER);
        StringBuilder builder = new StringBuilder();
        if (application) {
            builder.append(AUTH_SCHEME_APPLICATION_PREFIX);
        }
        builder.append(getAuthenticationScheme());
        String gssData = null;
        if ( params != null && params.length == 1) {
             gssData = (String) params[0];
        }
        if (gssData != null) {
            builder.append(' ').append(gssData);
        } else if (application && authConnect != null) { // Use authentication-connect URI for Application Negotiate
            builder.append(' ').append(authConnect);
            if (authIdentifier != null) {
                builder.append(' ').append(authIdentifier); // Use authentication-identified if one is specified
            }
        }
        return builder.toString();
    }
}

