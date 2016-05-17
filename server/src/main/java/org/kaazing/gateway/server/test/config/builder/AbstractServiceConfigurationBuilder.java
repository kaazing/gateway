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

import org.kaazing.gateway.server.test.config.AuthorizationConstraintConfiguration;
import org.kaazing.gateway.server.test.config.CrossOriginConstraintConfiguration;
import org.kaazing.gateway.server.test.config.NestedServicePropertiesConfiguration;
import org.kaazing.gateway.server.test.config.ServiceConfiguration;
import org.kaazing.gateway.server.test.config.Suppressible;
import org.kaazing.gateway.server.test.config.SuppressibleConfiguration.Suppression;

public abstract class AbstractServiceConfigurationBuilder<R> extends
        AbstractConfigurationBuilder<ServiceConfiguration, R> {

    public AbstractServiceConfigurationBuilder<R> balance(String balance) {
        configuration.getSuppressibleConfiguration().addBalance(
                new Suppressible<>(balance, getCurrentSuppressions()));
        return this;
    }

    public AbstractServiceConfigurationBuilder<R> accept(String string) {
        configuration.getSuppressibleConfiguration().addAccept(new Suppressible<>(string, getCurrentSuppressions()));
        return this;
    }

    public AbstractServiceConfigurationBuilder<R> acceptOption(String optionName, String optionValue) {
        configuration.getSuppressibleConfiguration().addAcceptOption(optionName,
                new Suppressible<>(optionValue, getCurrentSuppressions()));
        return this;
    }

    public AbstractServiceConfigurationBuilder<R> connect(String string) {
        configuration.getSuppressibleConfiguration().addConnect(
                new Suppressible<>(string, getCurrentSuppressions()));
        return this;
    }

    public AbstractServiceConfigurationBuilder<R> connectOption(String optionName, String optionValue) {
        configuration.getSuppressibleConfiguration().addConnectOption(optionName,
                new Suppressible<>(optionValue, getCurrentSuppressions()));
        return this;
    }

    public AbstractServiceConfigurationBuilder<R> realmName(String realmName) {
        configuration.getSuppressibleConfiguration().setRealmName(
                new Suppressible<>(realmName, getCurrentSuppressions()));
        return this;
    }

    public AbstractServiceConfigurationBuilder<R> type(String type) {
        configuration.getSuppressibleConfiguration().setType(new Suppressible<>(type, getCurrentSuppressions()));
        return this;
    }

    public AbstractServiceConfigurationBuilder<R> property(String propertyName, String propertyValue) {
        configuration.getSuppressibleConfiguration().addProperty(propertyName,
                new Suppressible<>(propertyValue, getCurrentSuppressions()));
        return this;
    }

    public AbstractServiceConfigurationBuilder<R> mimeMapping(String extension, String type) {
        configuration.getSuppressibleConfiguration().addMimeMapping(extension,
                new Suppressible<>(type, getCurrentSuppressions()));
        return this;
    }

    public AbstractServiceConfigurationBuilder<R> description(String description) {
        configuration.getSuppressibleConfiguration().setDescription(
                new Suppressible<>(description, getCurrentSuppressions()));
        return this;
    }

    public AbstractServiceConfigurationBuilder<R> name(String name) {
        configuration.getSuppressibleConfiguration().setName(new Suppressible<>(name, getCurrentSuppressions()));
        return this;
    }

    public abstract AbstractNestedPropertyConfigurationBuilder<? extends AbstractServiceConfigurationBuilder<R>>
        nestedProperty(String propertyName);

    public abstract AbstractAuthorizationConstraintConfigurationBuilder<? extends AbstractServiceConfigurationBuilder<R>>
    authorization();

    public abstract AbstractCrossOriginConstraintConfigurationBuilder<? extends AbstractServiceConfigurationBuilder<R>>
    crossOrigin();

    protected AbstractServiceConfigurationBuilder(ServiceConfiguration configuration, R result,
                                                  Set<Suppression> suppressions) {
        super(configuration, result, suppressions);
    }

    public static class AddCrossOriginConstraintBuilder<R extends AbstractServiceConfigurationBuilder<?>> extends
            AbstractCrossOriginConstraintConfigurationBuilder<R> {
        protected AddCrossOriginConstraintBuilder(R result, Set<Suppression> suppressions) {
            super(new CrossOriginConstraintConfiguration(), result, suppressions);
        }

        @Override
        public R done() {
            result.configuration.getCrossOriginConstraints().add(configuration);
            return super.done();
        }
    }

    public static class AddAuthorizationConstraintBuilder<R extends AbstractServiceConfigurationBuilder<?>> extends
            AbstractAuthorizationConstraintConfigurationBuilder<R> {
        protected AddAuthorizationConstraintBuilder(R result, Set<Suppression> suppressions) {
            super(new AuthorizationConstraintConfiguration(), result, suppressions);
        }

        @Override
        public R done() {
            result.configuration.getAuthorizationConstraints().add(configuration);
            return super.done();
        }
    }

    public static class AddNestedPropertyBuilder<R extends AbstractServiceConfigurationBuilder<?>> extends
            AbstractNestedPropertyConfigurationBuilder<R> {

        final String propertyName;

        protected AddNestedPropertyBuilder(String propertyName, R result, Set<Suppression> suppressions) {
            super(new NestedServicePropertiesConfiguration(propertyName), result, suppressions);
            this.propertyName = propertyName;
        }

        @Override
        public R done() {
            result.configuration.addNestedProperties(configuration);
            return super.done();
        }
    }

    @Override
    public AbstractServiceConfigurationBuilder<R> suppress(Suppression... suppressions) {
        super.addCurrentSuppressions(suppressions);
        return this;
    }

}
