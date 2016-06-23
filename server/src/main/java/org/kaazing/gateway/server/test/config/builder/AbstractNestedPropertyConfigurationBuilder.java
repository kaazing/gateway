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

import org.kaazing.gateway.server.test.config.NestedServicePropertiesConfiguration;
import org.kaazing.gateway.server.test.config.SuppressibleConfiguration.Suppression;

public abstract class AbstractNestedPropertyConfigurationBuilder<R> extends
        AbstractConfigurationBuilder<NestedServicePropertiesConfiguration, R> {

    protected AbstractNestedPropertyConfigurationBuilder(NestedServicePropertiesConfiguration configuration, R result,
                                                         Set<Suppression> suppressions) {
        super(configuration, result, suppressions);
    }

    public AbstractNestedPropertyConfigurationBuilder<R> property(String propertyName, String propertyValue) {
        configuration.addSimpleProperty(propertyName, propertyValue);
        return this;
    }

    public AddNestedPropertyBuilder<AbstractNestedPropertyConfigurationBuilder<R>> nestedProperty(String propertyName) {
        return new AddNestedPropertyBuilder<>(propertyName, this, getCurrentSuppressions());
    }

    // We do not need more than one level of nesting at present
    @Override
    public AbstractNestedPropertyConfigurationBuilder<R> suppress(Suppression... suppressions) {
        super.addCurrentSuppressions(suppressions);
        return this;
    }

	public static class AddNestedPropertyBuilder<R extends AbstractConfigurationBuilder<NestedServicePropertiesConfiguration,?>>
        extends AbstractNestedPropertyConfigurationBuilder<R> {

        final String propertyName;
        final R parent;

        protected AddNestedPropertyBuilder(String propertyName, R parent, Set<Suppression> suppressions) {
			super(new NestedServicePropertiesConfiguration(propertyName), parent, suppressions);
			this.propertyName = propertyName;
			this.parent = parent;
		}

		public R done() {
			parent.configuration.addNestedProperties(configuration);
			return parent;
		}
	}
}
