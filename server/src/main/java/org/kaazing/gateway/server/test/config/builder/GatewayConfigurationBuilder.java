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
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.SuppressibleConfiguration.Suppression;
import org.kaazing.gateway.server.test.config.Suppressibles;

public class GatewayConfigurationBuilder extends AbstractGatewayConfigurationBuilder<GatewayConfiguration> {

    public GatewayConfigurationBuilder() {
        this(new GatewayConfiguration(), Suppressibles.getDefaultSuppressions());
    }

    public GatewayConfigurationBuilder(Set<Suppression> suppressions) {
        this(new GatewayConfiguration(), suppressions);
    }

    @Override
    public SetSecurityBuilder<GatewayConfigurationBuilder> security() {
        return new SetSecurityBuilder<>(this, getCurrentSuppressions());
    }

    @Override
    public AddServiceBuilder<GatewayConfigurationBuilder> service() {
        return new AddServiceBuilder<>(this, getCurrentSuppressions());
    }

    @Override
    public AbstractClusterConfigurationBuilder<GatewayConfigurationBuilder> cluster() {
        return new SetClusterBuilder<>(this, getCurrentSuppressions());
    }

    @Override
    public AbstractServiceDefaultsConfigurationBuilder<GatewayConfigurationBuilder> serviceDefaults() {
        return new SetServiceDefaultsBuilder<>(this, getCurrentSuppressions());
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public AbstractNetworkBuilder<GatewayConfigurationBuilder> network() {
        return new AddNetworkBuilder<>(this);
    }

    private GatewayConfigurationBuilder(GatewayConfiguration configuration, Set<Suppression> suppressions) {
        super(configuration, configuration, suppressions);
    }

}
