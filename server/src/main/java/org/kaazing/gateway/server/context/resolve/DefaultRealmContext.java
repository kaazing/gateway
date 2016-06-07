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
package org.kaazing.gateway.server.context.resolve;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import javax.security.auth.login.Configuration;
import org.kaazing.gateway.security.AuthenticationContext;
import org.kaazing.gateway.security.LoginContextFactory;
import org.kaazing.gateway.security.RealmContext;
import org.kaazing.gateway.security.auth.context.LoginContextFactories;

public class DefaultRealmContext implements RealmContext {
    private final String name;
    private final String description;
    private final String[] userPrincipalClasses;
    private final Configuration configuration;
    private final LoginContextFactory loginContextFactory;
    private final AuthenticationContext authenticationContext;

    private static CharsetEncoder asciiEncoder =
            Charset.forName("US-ASCII").newEncoder();

    public DefaultRealmContext(String name, String description, String[] userPrincipalClasses, Configuration configuration,
                               AuthenticationContext authenticationContext) {
        this.name = name;
        if (description == null || asciiEncoder.canEncode(description)) {
            this.description = description;
        } else {
            throw new RuntimeException(
                    "Invalid non US-ASCII character in Realm description. Realm description can only contain US-ASCII values");
        }
        this.userPrincipalClasses = userPrincipalClasses;
        this.configuration = configuration;
        this.loginContextFactory = LoginContextFactories.create(name, configuration);
        this.authenticationContext = authenticationContext;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String[] getUserPrincipalClasses() {
        return userPrincipalClasses;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public LoginContextFactory getLoginContextFactory() {
        return loginContextFactory;
    }

    @Override
    public AuthenticationContext getAuthenticationContext() {
        return authenticationContext;
    }
}

