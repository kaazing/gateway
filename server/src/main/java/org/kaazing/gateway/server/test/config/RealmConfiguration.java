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

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class RealmConfiguration implements Configuration<SuppressibleRealmConfiguration> {

    private SuppressibleRealmConfiguration _configuration;
    private Suppressible<String> _name;
    private Suppressible<String> _description;
    private Suppressible<String> _httpChallengeScheme;
    private Suppressible<String> _authorizationMode;
    private Suppressible<String> _sessionTimeout;
    private Map<String, Suppressible<String>> _extendedProperties = new HashMap<>();
    private final List<Suppressible<String>> httpHeaders = new ArrayList<>();
    private final List<Suppressible<String>> httpQueryParameters = new ArrayList<>();
    private final List<Suppressible<String>> httpCookies = new ArrayList<>();
    private final List<Suppressible<String>> userPrincipalClasses = new ArrayList<>();
    private final List<String> unsuppressibleUserPrincipalClasses = Suppressibles.unsuppressibleList(userPrincipalClasses);
    private final List<LoginModuleConfiguration> loginModules = new LinkedList<>();
    private final List<String> unsuppressibleHttpHeaders = Suppressibles.unsuppressibleList(httpHeaders);
    private final List<String> unsuppressibleHttpQueryParameters = Suppressibles
            .unsuppressibleList(httpQueryParameters);
    private final List<String> unsuppressibleHttpCookies = Suppressibles.unsuppressibleList(httpCookies);
    private static CharsetEncoder asciiEncoder =
            Charset.forName("US-ASCII").newEncoder();

    public RealmConfiguration() {
        _configuration = new SuppressibleRealmConfigurationImpl();
        _configuration.setSuppression(Suppressibles.getDefaultSuppressions());
    }

    @Override
    public void accept(ConfigurationVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public SuppressibleRealmConfiguration getSuppressibleConfiguration() {
        return _configuration;
    }

    public Collection<LoginModuleConfiguration> getLoginModules() {
        return loginModules;
    }

    public void setLoginModules(List<LoginModuleConfiguration> newLoginModules) {
        loginModules.clear();
        loginModules.addAll(newLoginModules);
    }

    // name
    public String getName() {
        if (_name == null) {
            return null;
        }
        return _name.value();
    }

    public void setName(String name) {
        this._name = new Suppressible<>(name);
    }

    // description
    public String getDescription() {
        if (_description == null) {
            return null;
        }
        return _description.value();
    }

    public void setDescription(String description) {
        if (description == null || asciiEncoder.canEncode(description)) {
            this._description = new Suppressible<>(description);
        } else {
            throw new RuntimeException(
                    "Invalid non US-ASCII character in Realm description. Realm description can only contain US-ASCII values");
        }
    }

    // http challenge scheme
    public String getHttpChallengeScheme() {
        if (_httpChallengeScheme == null) {
            return null;
        }
        return _httpChallengeScheme.value();
    }

    public void setHttpChallengeScheme(String httpChallengeScheme) {
        this._httpChallengeScheme = new Suppressible<>(httpChallengeScheme);
    }

    // authorization mode
    public String getAuthorizationMode() {
        if (_authorizationMode == null) {
            return null;
        }
        return _authorizationMode.value();
    }

    public void setAuthorizationMode(String authorizationMode) {
        this._authorizationMode = new Suppressible<>(authorizationMode);
    }

    // session timeout
    public String getSessionTimeout() {
        if (_sessionTimeout == null) {
            return null;
        }
        return _sessionTimeout.value();
    }

    public void setSessionTimeout(String sessionTimeout) {
        this._sessionTimeout = new Suppressible<>(sessionTimeout);
    }

    // http headers
    public List<String> getHttpHeaders() {
        return unsuppressibleHttpHeaders;
    }

    public void addHttpHeader(String httpHeader) {
        getHttpHeaders().add(httpHeader);
    }

    // http query parameters
    public List<String> getHttpQueryParameters() {
        return unsuppressibleHttpQueryParameters;
    }

    public void addHttpQueryParameter(String httpQueryParameter) {
        getHttpQueryParameters().add(httpQueryParameter);
    }

    // http cookies
    public List<String> getHttpCookies() {
        return unsuppressibleHttpCookies;
    }

    public void addHttpCookie(String httpCookie) {
        getHttpCookies().add(httpCookie);
    }

    public Map<String, String> getExtendedProperties() {
        Map<String, String> result = new HashMap<>();
        for (Entry<String, Suppressible<String>> entry : _extendedProperties.entrySet()) {
            result.put(entry.getKey(), entry.getValue().value());
        }
        return result;
    }

    public void setExtendedProperty(String name, String value) {
        _extendedProperties.put(name, new Suppressible<>(value));
    }

    //user principal classes
    public List<String> getUserPrincipalClasses() {
        return Collections.unmodifiableList(unsuppressibleUserPrincipalClasses);
    }

    public void addUserPrincipalClass(String userPrincipalClass) {
        unsuppressibleUserPrincipalClasses.add(userPrincipalClass);
    }

    private class SuppressibleRealmConfigurationImpl extends SuppressibleRealmConfiguration {
        private Set<Suppression> _suppressions;

        @Override
        public Set<org.kaazing.gateway.server.test.config.SuppressibleConfiguration.Suppression> getSuppressions() {
            return _suppressions;
        }

        @Override
        public void setSuppression(Set<org.kaazing.gateway.server.test.config.SuppressibleConfiguration.Suppression>
                                                   suppressions) {
            _suppressions = suppressions;
        }

        @Override
        public Suppressible<String> getName() {
            return _name;
        }

        @Override
        public void setName(Suppressible<String> name) {
            _name = name;
        }

        @Override
        public Suppressible<String> getDescription() {
            return _description;
        }

        @Override
        public void setDescription(Suppressible<String> description) {
            _description = description;
        }

        @Override
        public Suppressible<String> getHttpChallengeScheme() {
            return _httpChallengeScheme;
        }

        @Override
        public void setHttpChallengeScheme(Suppressible<String> httpChallengeScheme) {
            _httpChallengeScheme = httpChallengeScheme;
        }

        @Override
        public Suppressible<String> getAuthorizationMode() {
            return _authorizationMode;
        }

        @Override
        public void setAuthorizationMode(Suppressible<String> authorizationMode) {
            _authorizationMode = authorizationMode;
        }

        @Override
        public Suppressible<String> getSessionTimeout() {
            return _sessionTimeout;
        }

        @Override
        public void setSessionTimeout(Suppressible<String> sessionTimeout) {
            _sessionTimeout = sessionTimeout;
        }

        @Override
        public List<Suppressible<String>> getHttpHeaders() {
            return httpHeaders;
        }

        @Override
        public void addHttpHeader(Suppressible<String> httpHeader) {
            httpHeaders.add(httpHeader);
        }

        @Override
        public List<Suppressible<String>> getHttpQueryParameters() {
            return httpQueryParameters;
        }

        @Override
        public void addHttpQueryParameter(Suppressible<String> httpQueryParameter) {
            httpQueryParameters.add(httpQueryParameter);
        }

        @Override
        public List<Suppressible<String>> getHttpCookies() {
            return httpCookies;
        }

        @Override
        public void addHttpCookie(Suppressible<String> httpCookie) {
            httpCookies.add(httpCookie);
        }

        @Override
        public Map<String, Suppressible<String>> getExtendedProperties() {
            return _extendedProperties;
        }

        @Override
        public void setExtendedProperty(String name, Suppressible<String> value) {
            _extendedProperties.put(name, value);
        }

        @Override
        public List<Suppressible<String>> getUserPrincipalClasses() {
            return Collections.unmodifiableList(userPrincipalClasses);
        }

        @Override
        public void addUserPrincipalClass(Suppressible<String> userPrincipalClass) {
            userPrincipalClasses.add(userPrincipalClass);
        }
    }
}
