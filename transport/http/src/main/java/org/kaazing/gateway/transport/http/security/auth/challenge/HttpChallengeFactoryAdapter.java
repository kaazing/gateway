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
import static org.kaazing.gateway.transport.http.bridge.filter.HttpSubjectSecurityFilter.WWW_AUTHENTICATE_HEADER;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;

public abstract class HttpChallengeFactoryAdapter implements HttpChallengeFactory {

    // TODO: remove "Application " prefix from the wire protocol, already handled at correct transport layer
    @Deprecated
    protected boolean isApplication(String authScheme) {
        return authScheme != null &&
            authScheme.startsWith(AUTH_SCHEME_APPLICATION_PREFIX);
    }

    @Override
    public HttpResponseMessage createChallenge(HttpRequestMessage httpRequestMessage,
                                               Object... params) {
        return createChallenge0(httpRequestMessage,  params);
    }

    private HttpResponseMessage createChallenge0(HttpRequestMessage httpRequestMessage, Object... params) {
        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(httpRequestMessage.getVersion());
        httpResponse.setStatus(HttpStatus.CLIENT_UNAUTHORIZED);
        String challenge = makeChallengeString(httpRequestMessage.getLocalAddress(), params);
        httpResponse.setHeader(WWW_AUTHENTICATE_HEADER, challenge);
        return httpResponse;
    }

    protected abstract String getAuthenticationScheme();

    protected String makeChallengeString(ResourceAddress address, Object... params) {
        StringBuilder builder = new StringBuilder();

        //-- existing clients expect 'Application Basic' in the challenge so we
        //-- cannot ye remove the block below that sometimes prepends 'Application'.
        String challengeScheme = address.getOption(HttpResourceAddress.REALM_CHALLENGE_SCHEME);
        if (isApplication(challengeScheme)) {
            // if you look like a directory service, skip adding application-prefix
            if (address.getOption(ResourceAddress.NEXT_PROTOCOL) != null || !"http/1.1".equals(address.getTransport().getOption(ResourceAddress.NEXT_PROTOCOL))) {
                builder.append(AUTH_SCHEME_APPLICATION_PREFIX);
            }
        }
        builder.append(getAuthenticationScheme()).append(" realm=\"").append(address.getOption(HttpResourceAddress.REALM_DESCRIPTION)).append("\"");

        if (params != null) {
            // Don't forget the additional parameters (KG-2191)
            for (Object obj : params) {
                builder.append(" ").append(obj);
            }
        }

        return builder.toString();
    }}

