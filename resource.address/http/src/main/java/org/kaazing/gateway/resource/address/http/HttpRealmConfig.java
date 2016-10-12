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

import org.kaazing.gateway.security.AuthenticationContext;
import org.kaazing.gateway.security.LoginContextFactory;
import org.kaazing.gateway.security.RealmContext;

// TODO, decide on location and name of class...

public class HttpRealmConfig {

    private final String name;
    private final String authorizationMode;
    private final String realmChallengeScheme;
    private final String description;
    private final String[] headerNames;
    private final String[] parameterNames;
    private final String[] authenticationCookieNames;
    private final LoginContextFactory loginContextFactory;
    private final String[] userPrincipleClasses;

    public HttpRealmConfig(RealmContext serviceRealmContext) {
        AuthenticationContext authenticationContext = serviceRealmContext.getAuthenticationContext();
        name = serviceRealmContext.getName();
        authorizationMode = authenticationContext.getAuthorizationMode();
        realmChallengeScheme = authenticationContext.getHttpChallengeScheme();
        description = serviceRealmContext.getDescription();
        headerNames = authenticationContext.getHttpHeaders();
        parameterNames = authenticationContext.getHttpQueryParameters();
        authenticationCookieNames = authenticationContext.getHttpCookieNames();
        loginContextFactory = serviceRealmContext.getLoginContextFactory();
        userPrincipleClasses = serviceRealmContext.getUserPrincipalClasses();
    }

    public String getName() {
        return name;
    }

    public String getAuthorizationMode() {
        return authorizationMode;
    }

    public String getRealmChallengeScheme() {
        return realmChallengeScheme;
    }

    public String getDescription() {
        return description;
    }

    public String[] getHeaderNames() {
        return headerNames;
    }

    public String[] getParameterNames() {
        return parameterNames;
    }

    public String[] getAuthenticationCookieNames() {
        return authenticationCookieNames;
    }

    public LoginContextFactory getLoginContextFactory() {
        return loginContextFactory;
    }

    public String[] getUserPrincipleClasses() {
        return userPrincipleClasses;
    }

}
