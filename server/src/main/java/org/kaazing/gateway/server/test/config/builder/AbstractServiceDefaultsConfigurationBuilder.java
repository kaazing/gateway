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
import org.kaazing.gateway.server.test.config.ServiceDefaultsConfiguration;
import org.kaazing.gateway.server.test.config.Suppressible;
import org.kaazing.gateway.server.test.config.SuppressibleConfiguration.Suppression;

public class AbstractServiceDefaultsConfigurationBuilder<R> extends
        AbstractConfigurationBuilder<ServiceDefaultsConfiguration, R> {

    protected AbstractServiceDefaultsConfigurationBuilder(ServiceDefaultsConfiguration configuration, R result,
                                                          Set<Suppression> suppressions) {
        super(configuration, result, suppressions);
    }

    public AbstractServiceDefaultsConfigurationBuilder<R> mimeMapping(String extension, String type) {
        configuration.getSuppressibleConfiguration().addMimeMapping(extension,
                new Suppressible<>(type, getCurrentSuppressions()));
        return this;
    }

    public AbstractServiceDefaultsConfigurationBuilder<R> acceptOption(String option, String value) {
        configuration.getSuppressibleConfiguration().addAcceptOption(option,
                new Suppressible<>(value, getCurrentSuppressions()));
        return this;
    }

    public AbstractServiceDefaultsConfigurationBuilder<R> connectOption(String option, String value) {
        configuration.getSuppressibleConfiguration().addConnectOption(option,
                new Suppressible<>(value, getCurrentSuppressions()));
        return this;
    }

    @Override
    public AbstractServiceDefaultsConfigurationBuilder<R> suppress(Suppression... suppressions) {
        super.addCurrentSuppressions(suppressions);
        return this;
    }

}
