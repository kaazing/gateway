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

import java.io.File;
import java.util.Set;
import org.kaazing.gateway.server.test.config.ClusterConfiguration;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.NetworkConfiguration;
import org.kaazing.gateway.server.test.config.SecurityConfiguration;
import org.kaazing.gateway.server.test.config.ServiceConfiguration;
import org.kaazing.gateway.server.test.config.ServiceDefaultsConfiguration;
import org.kaazing.gateway.server.test.config.SuppressibleConfiguration.Suppression;

public abstract class AbstractGatewayConfigurationBuilder<R> extends
        AbstractConfigurationBuilder<GatewayConfiguration, R> {

    public AbstractGatewayConfigurationBuilder(GatewayConfiguration configuration, R result,
                                               Set<Suppression> suppressions) {
        super(configuration, result, suppressions);
    }

    public AbstractGatewayConfigurationBuilder<R> property(String name, String value) {
        configuration.getProperties().put(name, value);
        return this;
    }

    public AbstractGatewayConfigurationBuilder<R> webRootDirectory(File webRootDirectory) {
        configuration.setWebRootDirectory(webRootDirectory);
        return this;
    }

    public AbstractGatewayConfigurationBuilder<R> tempDirectory(File tempDirectory) {
        configuration.setTempDirectory(tempDirectory);
        return this;
    }

    public abstract AbstractSecurityConfigurationBuilder<? extends AbstractGatewayConfigurationBuilder<R>> security();

    public abstract AbstractServiceConfigurationBuilder<? extends AbstractGatewayConfigurationBuilder<R>> service();

    public abstract AbstractClusterConfigurationBuilder<? extends AbstractGatewayConfigurationBuilder<R>> cluster();

    public abstract AbstractServiceDefaultsConfigurationBuilder<? extends AbstractGatewayConfigurationBuilder<R>>
    serviceDefaults();

    public abstract AbstractNetworkBuilder<? extends AbstractGatewayConfigurationBuilder<R>> network();

    public static class SetSecurityBuilder<R extends AbstractGatewayConfigurationBuilder<?>> extends
            AbstractSecurityConfigurationBuilder<R> {
        public SetSecurityBuilder(R result, Set<Suppression> suppressions) {
            super(new SecurityConfiguration(), result, suppressions);
        }

        @Override
        public AddRealmBuilder<SetSecurityBuilder<R>> realm() {
            return new AddRealmBuilder<>(this, getCurrentSuppressions());
        }

        @Override
        public R done() {
            result.configuration.setSecurity(configuration);
            return super.done();
        }
    }

    public static class AddServiceBuilder<R extends AbstractGatewayConfigurationBuilder<?>> extends
            AbstractServiceConfigurationBuilder<R> {
        protected AddServiceBuilder(R result, Set<Suppression> suppressions) {
            super(new ServiceConfiguration(), result, suppressions);
        }

        @Override
        public AddCrossOriginConstraintBuilder<AddServiceBuilder<R>> crossOrigin() {
            return new AddCrossOriginConstraintBuilder<>(this, getCurrentSuppressions());
        }

        @Override
        public AddAuthorizationConstraintBuilder<AddServiceBuilder<R>> authorization() {
            return new AddAuthorizationConstraintBuilder<>(this, getCurrentSuppressions());
        }

        @Override
        public AddNestedPropertyBuilder<AddServiceBuilder<R>> nestedProperty(String propertyName) {
            return new AddNestedPropertyBuilder<>(propertyName, this, getCurrentSuppressions());
        }

        @Override
        public R done() {
            result.configuration.getServices().add(configuration);
            return super.done();
        }
    }

    public static class SetClusterBuilder<R extends AbstractGatewayConfigurationBuilder<?>> extends
            AbstractClusterConfigurationBuilder<R> {

        protected SetClusterBuilder(R result, Set<Suppression> suppressions) {
            super(new ClusterConfiguration(), result, suppressions);
        }

        @Override
        public R done() {
            result.configuration.setCluster(configuration);
            return super.done();
        }
    }

    public static class SetServiceDefaultsBuilder<R extends AbstractGatewayConfigurationBuilder<?>> extends
            AbstractServiceDefaultsConfigurationBuilder<R> {

        protected SetServiceDefaultsBuilder(R result, Set<Suppression> suppressions) {
            super(new ServiceDefaultsConfiguration(), result, suppressions);
        }

        @Override
        public R done() {
            result.configuration.setServiceDefaults(configuration);
            return super.done();
        }
    }

    public static class AddNetworkBuilder<R extends AbstractGatewayConfigurationBuilder<?>> extends
            AbstractNetworkBuilder<R> {

        protected AddNetworkBuilder(R result) {
            super(new NetworkConfiguration(), result);
        }

        @Override
        public R done() {
            result.configuration.setNetwork(configuration);
            return super.done();
        }
    }

    @Override
    public AbstractGatewayConfigurationBuilder<R> suppress(Suppression... suppressions) {
        super.addCurrentSuppressions(suppressions);
        return this;
    }
}
