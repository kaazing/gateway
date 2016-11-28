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

package org.kaazing.gateway.resource.address.http;

import java.security.Principal;
import java.util.Collection;

import org.kaazing.gateway.security.LoginContextFactory;

public class DefaultHttpRealmInfo implements HttpRealmInfo {

    private final String name;
    private final String challengeScheme;
    private final String description;
    private final String[] headerNames;
    private final String[] parameterNames;
    private final String[] authenticationCookieNames;
    private final LoginContextFactory loginContextFactory;
    private final Collection<Class<? extends Principal>> userPrincipleClasses;

    public DefaultHttpRealmInfo(String name, String challengeScheme, String description, String[] headerNames,
            String[] parameterNames, String[] authenticationCookieNames, LoginContextFactory loginContextFactory,
            Collection<Class<? extends Principal>> userPrincipleClasses) {
        this.name = name;
        this.challengeScheme = challengeScheme;
        this.description = description;
        this.headerNames = headerNames;
        this.parameterNames = parameterNames;
        this.authenticationCookieNames = authenticationCookieNames;
        this.loginContextFactory = loginContextFactory;
        this.userPrincipleClasses = userPrincipleClasses;

    }

    /* (non-Javadoc)
     * @see org.kaazing.gateway.resource.address.http.HttpRealmInfo#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see org.kaazing.gateway.resource.address.http.HttpRealmInfo#getChallengeScheme()
     */
    @Override
    public String getChallengeScheme() {
        return challengeScheme;
    }

    /* (non-Javadoc)
     * @see org.kaazing.gateway.resource.address.http.HttpRealmInfo#getDescription()
     */
    @Override
    public String getDescription() {
        return description;
    }

    /* (non-Javadoc)
     * @see org.kaazing.gateway.resource.address.http.HttpRealmInfo#getHeaderNames()
     */
    @Override
    public String[] getHeaderNames() {
        return headerNames;
    }

    /* (non-Javadoc)
     * @see org.kaazing.gateway.resource.address.http.HttpRealmInfo#getParameterNames()
     */
    @Override
    public String[] getParameterNames() {
        return parameterNames;
    }

    /* (non-Javadoc)
     * @see org.kaazing.gateway.resource.address.http.HttpRealmInfo#getAuthenticationCookieNames()
     */
    @Override
    public String[] getAuthenticationCookieNames() {
        return authenticationCookieNames;
    }

    /* (non-Javadoc)
     * @see org.kaazing.gateway.resource.address.http.HttpRealmInfo#getLoginContextFactory()
     */
    @Override
    public LoginContextFactory getLoginContextFactory() {
        return loginContextFactory;
    }

    /* (non-Javadoc)
     * @see org.kaazing.gateway.resource.address.http.HttpRealmInfo#getUserPrincipleClasses()
     */
    @Override
    public Collection<Class<? extends Principal>> getUserPrincipleClasses() {
        return userPrincipleClasses;
    }

}
