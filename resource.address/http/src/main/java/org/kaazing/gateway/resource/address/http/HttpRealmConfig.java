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

import static java.lang.String.format;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;

import org.kaazing.gateway.security.AuthenticationContext;
import org.kaazing.gateway.security.LoginContextFactory;
import org.kaazing.gateway.security.RealmContext;

// TODO, decide on location and name of class...

public class HttpRealmConfig {

    private final String name;
    private final String authorizationMode;
    private final String challengeScheme;
    private final String description;
    private final String[] headerNames;
    private final String[] parameterNames;
    private final String[] authenticationCookieNames;
    private final LoginContextFactory loginContextFactory;
    private final Collection<Class<? extends Principal>> userPrincipleClasses;

    public HttpRealmConfig(RealmContext serviceRealmContext) {
        // TODO, change call site to this to use other constructor
        final AuthenticationContext authenticationContext = serviceRealmContext.getAuthenticationContext();
        name = serviceRealmContext.getName();
        authorizationMode = authenticationContext.getAuthorizationMode();
        challengeScheme = authenticationContext.getHttpChallengeScheme();
        description = serviceRealmContext.getDescription();
        headerNames = authenticationContext.getHttpHeaders();
        parameterNames = authenticationContext.getHttpQueryParameters();
        authenticationCookieNames = authenticationContext.getHttpCookieNames();
        loginContextFactory = serviceRealmContext.getLoginContextFactory();
        userPrincipleClasses = loadUserPrincipalClasses(serviceRealmContext.getUserPrincipalClasses());
    }

    // TODO remove Authorization Mode
    public HttpRealmConfig(String name, String authorizationMode, String challengeScheme, String description,
            String[] headerNames, String[] parameterNames, String[] authenticationCookieNames,
            LoginContextFactory loginContextFactory, Collection<Class<? extends Principal>> userPrincipleClasses) {
        this.name = name;
        this.authorizationMode = authorizationMode;
        this.challengeScheme = challengeScheme;
        this.description = description;
        this.headerNames = headerNames;
        this.parameterNames = parameterNames;
        this.authenticationCookieNames = authenticationCookieNames;
        this.loginContextFactory = loginContextFactory;
        this.userPrincipleClasses = userPrincipleClasses;

    }

    /**
     * Method converting String[] userPrincipalClasses to Class[]
     * @param userPrincipalClasses
     * @return
     */
    private Collection<Class<? extends Principal>> loadUserPrincipalClasses(String[] userPrincipalClasses) {
        Collection<Class<? extends Principal>> userPrincipals = new ArrayList<>();
        for (String className : userPrincipalClasses) {
            try {
                userPrincipals.add(Class.forName(className).asSubclass(Principal.class));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(
                        format("%s%s%s", "Class ", className,
                                " could not be loaded. Please check the gateway configuration xml and confirm that"
                                        + " user-principal-class value(s) are spelled correctly for realm " + name + "."),
                        new ClassNotFoundException(e.getMessage()));
            }
        }
        return userPrincipals;
    }

    public String getName() {
        return name;
    }

    public String getAuthorizationMode() {
        return authorizationMode;
    }

    public String getChallengeScheme() {
        return challengeScheme;
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

    public Collection<Class<? extends Principal>> getUserPrincipleClasses() {
        return userPrincipleClasses;
    }

}
