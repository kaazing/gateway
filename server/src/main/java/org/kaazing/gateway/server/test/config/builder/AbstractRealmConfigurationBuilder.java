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
package org.kaazing.gateway.server.test.config.builder;

import java.util.Set;
import org.kaazing.gateway.server.test.config.LoginModuleConfiguration;
import org.kaazing.gateway.server.test.config.RealmConfiguration;
import org.kaazing.gateway.server.test.config.Suppressible;
import org.kaazing.gateway.server.test.config.SuppressibleConfiguration.Suppression;

public abstract class AbstractRealmConfigurationBuilder<R> extends AbstractConfigurationBuilder<RealmConfiguration, R> {

    public AbstractRealmConfigurationBuilder<R> name(String name) {
        configuration.getSuppressibleConfiguration().setName(new Suppressible<>(name, getCurrentSuppressions()));
        return this;
    }

    public AbstractRealmConfigurationBuilder<R> description(String description) {
        configuration.getSuppressibleConfiguration().setDescription(
                new Suppressible<>(description, getCurrentSuppressions()));
        return this;
    }

    public abstract AbstractLoginModuleConfigurationBuilder<? extends AbstractRealmConfigurationBuilder<R>> loginModule();

    protected AbstractRealmConfigurationBuilder(RealmConfiguration configuration, R result,
                                                Set<Suppression> suppressions) {
        super(configuration, result, suppressions);
    }

    public static class AddLoginModuleBuilder<R extends AbstractRealmConfigurationBuilder<?>> extends
            AbstractLoginModuleConfigurationBuilder<R> {
        protected AddLoginModuleBuilder(R result, Set<Suppression> suppressions) {
            super(new LoginModuleConfiguration(), result, suppressions);
        }

        @Override
        public R done() {
            result.configuration.getLoginModules().add(configuration);
            return super.done();
        }

    }

    public AbstractRealmConfigurationBuilder<R> httpChallengeScheme(String httpChallengeScheme) {
        configuration.getSuppressibleConfiguration().setHttpChallengeScheme(
                new Suppressible<>(httpChallengeScheme, getCurrentSuppressions()));
        return this;
    }

    public AbstractRealmConfigurationBuilder<R> httpHeader(String header) {
        configuration.getSuppressibleConfiguration().addHttpHeader(
                new Suppressible<>(header, getCurrentSuppressions()));
        return this;
    }

    public AbstractRealmConfigurationBuilder<R> httpQueryParameter(String queryParameter) {
        configuration.getSuppressibleConfiguration().addHttpQueryParameter(
                new Suppressible<>(queryParameter, getCurrentSuppressions()));
        return this;
    }

    public AbstractRealmConfigurationBuilder<R> httpCookie(String cookie) {
        configuration.getSuppressibleConfiguration().addHttpCookie(
                new Suppressible<>(cookie, getCurrentSuppressions()));
        return this;
    }

    public AbstractRealmConfigurationBuilder<R> authorizationMode(String chars) {
        configuration.getSuppressibleConfiguration().setAuthorizationMode(
                new Suppressible<>(chars, getCurrentSuppressions()));
        return this;
    }

    public AbstractRealmConfigurationBuilder<R> sessionTimeout(String chars) {
        configuration.getSuppressibleConfiguration().setSessionTimeout(
                new Suppressible<>(chars, getCurrentSuppressions()));
        return this;
    }

    public AbstractRealmConfigurationBuilder<R> extendedProperty(String name, String chars) {
        configuration.getSuppressibleConfiguration().setExtendedProperty(
                name, new Suppressible<>(chars, getCurrentSuppressions()));
        return this;
    }

    public AbstractRealmConfigurationBuilder<R> userPrincipalClass(String userPrincipalClass) {
        configuration.getSuppressibleConfiguration().addUserPrincipalClass(
                new Suppressible<>(userPrincipalClass, getCurrentSuppressions()));
        return this;
    }

    @Override
    public AbstractRealmConfigurationBuilder<R> suppress(Suppression... suppressions) {
        super.addCurrentSuppressions(suppressions);
        return this;
    }

}
