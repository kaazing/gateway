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
package org.kaazing.gateway.server.test.config;

import java.util.List;
import java.util.Map;

public abstract class SuppressibleRealmConfiguration implements SuppressibleConfiguration {

    public abstract Suppressible<String> getName();

    public abstract void setName(Suppressible<String> name);

    public abstract Suppressible<String> getDescription();

    public abstract void setDescription(Suppressible<String> description);

    public abstract Suppressible<String> getHttpChallengeScheme();

    public abstract void setHttpChallengeScheme(Suppressible<String> httpChallengeScheme);

    public abstract Suppressible<String> getAuthorizationMode();

    public abstract void setAuthorizationMode(Suppressible<String> authorizationMode);

    public abstract Suppressible<String> getSessionTimeout();

    public abstract void setSessionTimeout(Suppressible<String> sessionTimeout);

    public abstract List<Suppressible<String>> getHttpHeaders();

    public abstract void addHttpHeader(Suppressible<String> httpHeader);

    public abstract List<Suppressible<String>> getHttpQueryParameters();

    public abstract void addHttpQueryParameter(Suppressible<String> httpQueryParameter);

    public abstract List<Suppressible<String>> getHttpCookies();

    public abstract void addHttpCookie(Suppressible<String> httpCookie);

    public abstract Map<String, Suppressible<String>> getExtendedProperties();

    public abstract void setExtendedProperty(String name, Suppressible<String> value);

    public abstract List<Suppressible<String>> getUserPrincipalClasses();

    public abstract void addUserPrincipalClass(Suppressible<String> userPrincipalClass);
}
