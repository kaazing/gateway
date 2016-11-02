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
package org.kaazing.gateway.transport.http.security.auth.token;

import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_AUTHORIZATION;

import java.io.UnsupportedEncodingException;
import java.util.Set;

import org.kaazing.gateway.resource.address.http.HttpRealmInfo;
import org.kaazing.gateway.security.auth.token.DefaultAuthenticationToken;
import org.kaazing.gateway.server.spi.security.AuthenticationToken;
import org.kaazing.gateway.transport.http.HttpCookie;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;

public class AbstractAuthenticationTokenExtractor implements AuthenticationTokenExtractor {

    @Override
    public AuthenticationToken extract(HttpRequestMessage httpRequest, HttpRealmInfo realm) throws UnsupportedEncodingException {
        DefaultAuthenticationToken result = new DefaultAuthenticationToken();

        extractAuthorizationHeader(httpRequest, result);

        final String[] httpHeaders = realm.getHeaderNames();
        final String[] httpQueryParameters = realm.getParameterNames();
        final String[] httpCookieNames = realm.getAuthenticationCookieNames();

        if ( httpHeaders != null && httpHeaders.length > 0 ) {
            extractHttpHeaders(httpRequest, httpHeaders, result);
        }

        if ( httpQueryParameters != null && httpQueryParameters.length > 0 ) {
            extractQueryParameters(httpRequest, httpQueryParameters, result);
        }

        if ( httpCookieNames != null && httpCookieNames.length > 0 ) {
            extractCookies(httpRequest, httpCookieNames, result);
        }

        return result;
    }

    protected void extractHttpHeaders(HttpRequestMessage httpRequest, String[] httpHeaders, DefaultAuthenticationToken result) {
        for ( String headerName: httpHeaders ) {
            //TODO: support multiple values for headers
            String value = httpRequest.getHeader(headerName);
            if ( value != null ) {
                if ( result.get(headerName) != null ) {
                    throw new IllegalStateException("Cannot authenticate with multiple http header values for header name \""+headerName+"\"");
                }
                result.add(headerName, value);
            }
        }
    }

    protected void extractQueryParameters(HttpRequestMessage httpRequest, String[] httpQueryParameters, DefaultAuthenticationToken result) {
        for ( String parameterName: httpQueryParameters ) {
            //TODO: support multiple values for parameters
            String value = httpRequest.getParameter(parameterName);
            if ( value != null ) {
                if ( result.get(parameterName) != null ) {
                    throw new IllegalStateException("Cannot authenticate with multiple http parameter values for parameter name \""+parameterName+"\"");
                }
                result.add(parameterName, value);
            }
        }
    }

     protected void extractCookies(HttpRequestMessage httpRequest, String[] httpCookieNames, DefaultAuthenticationToken result) {
        for ( String cookieName: httpCookieNames ) {
            Set<HttpCookie> cookies =  httpRequest.getCookies();
            if ( cookies != null ) {
                for (HttpCookie cookie: cookies) {
                    if ( cookieName != null && cookieName.equals(cookie.getName()) ) {
                        String value = cookie.getValue();
                        if ( value != null ) {
                            if ( result.get(cookieName) != null ) {
                                throw new IllegalStateException("Cannot authenticate with multiple http cookie values for cookie name \""+cookieName+"\"");
                            }
                            result.add(cookieName, value);
                        }
                    }
                }
            }
        }
    }

    private void extractAuthorizationHeader(HttpRequestMessage httpRequest, DefaultAuthenticationToken result) {
        if (httpRequest.hasHeader(HEADER_AUTHORIZATION) ) {
            String authorization = httpRequest.getHeader(HEADER_AUTHORIZATION).trim();

            // We have to be careful when handling any client-supplied data.  In
            // this particular case, we need to extract the challenge scheme from
            // the Authorization: header -- and it may not be there, or may not be
            // there in the form we expect.

            int idx = -1;

            for (int i = 0; i < authorization.length(); i++) {
                if (Character.isWhitespace(authorization.charAt(i))) {
                    idx = i;
                    break;
                }
            }

            if (idx == -1) {
                // The client sent a badly formed Authorization: header value,
                // where only the scheme was provided.  The login modules will
                // be the judge of how to handle this.
                result.setScheme(authorization);

            } else {
                String scheme = authorization.substring(0, idx);
                authorization = authorization.substring(idx+1);
 
                result.setScheme(scheme);
                if (authorization.length() > 0) {
                    result.add(authorization);
                }
            }
        }
    }
}
