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
import org.kaazing.gateway.server.test.config.CrossOriginConstraintConfiguration;
import org.kaazing.gateway.server.test.config.Suppressible;
import org.kaazing.gateway.server.test.config.SuppressibleConfiguration.Suppression;

public abstract class AbstractCrossOriginConstraintConfigurationBuilder<R> extends
        AbstractConfigurationBuilder<CrossOriginConstraintConfiguration, R> {

    public AbstractCrossOriginConstraintConfigurationBuilder<R> allowOrigin(String allowOrigin) {
        configuration.getSuppressibleConfiguration().setAllowOrigin(
                new Suppressible<>(allowOrigin, getCurrentSuppressions()));
        return this;
    }

    public AbstractCrossOriginConstraintConfigurationBuilder<R> allowMethods(String allowMethods) {
        configuration.getSuppressibleConfiguration().setAllowMethods(
                new Suppressible<>(allowMethods, getCurrentSuppressions()));
        return this;
    }

    public AbstractCrossOriginConstraintConfigurationBuilder<R> allowHeaders(String allowHeaders) {
        configuration.getSuppressibleConfiguration().setAllowHeaders(
                new Suppressible<>(allowHeaders, getCurrentSuppressions()));
        return this;
    }

    protected AbstractCrossOriginConstraintConfigurationBuilder(CrossOriginConstraintConfiguration configuration,
                                                                R result, Set<Suppression> suppressions) {
        super(configuration, result, suppressions);
    }

    @Override
    public AbstractCrossOriginConstraintConfigurationBuilder<R> suppress(Suppression... suppressions) {
        super.addCurrentSuppressions(suppressions);
        return this;
    }
}
