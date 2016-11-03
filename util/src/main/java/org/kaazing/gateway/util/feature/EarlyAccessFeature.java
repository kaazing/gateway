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
package org.kaazing.gateway.util.feature;

import static java.lang.String.format;

import java.util.Map;
import java.util.Properties;
import java.util.function.BooleanSupplier;

import org.slf4j.Logger;

/**
 * Represents an early access feature, that is, a new product feature that is intended
 * to be used for development and testing purposes only and is not yet ready for use
 * in a production environment.
 */
public class EarlyAccessFeature {

    private final boolean enabledByDefault;
    private final String name;
    private final String description;
    private final EarlyAccessFeature[] impliedBy;

    public EarlyAccessFeature(String name, String description, boolean enabledByDefault,
                              EarlyAccessFeature... impliedBy) {
        this.name = name;
        this.description = description;
        this.enabledByDefault = enabledByDefault;
        this.impliedBy = impliedBy;
    }

    @SuppressWarnings({
        "unchecked", "rawtypes" })
    public void assertEnabled(Properties configuration, Logger logger) {
        assertEnabled((Map) configuration, logger);
    }

    @SuppressWarnings({
        "unchecked", "rawtypes" })
    public boolean isEnabled(Properties configuration) {
        return isEnabled((Map) configuration);
    }

    public void assertEnabled(Map<String, ?> configuration, Logger logger) {
        if (!isEnabled(configuration)) {
            String message = "Early access feature {} is disabled. To use it you must set " +
                    "system property \"feature.{}\", or include {} in environment variable " +
                    "GATEWAY_FEATURES (a comma separated list) if you are using " +
                    "the standard Gateway startup script";
            logger.error(message, toString(), name, name);
            throw new UnsupportedOperationException(format("Feature %s not enabled", toString()));
        }
    }


    public boolean isEnabled(Map<String, ?> configuration) {
        BooleanSupplier defaulter = () -> enabledByDefault || implied(configuration);
        return isEnabled(configuration, defaulter);
    }

    private boolean isEnabled(Map<String, ?> configuration, BooleanSupplier defaulter) {
        Object value = configuration.get(getPropertyName());
        if (!(value instanceof String)) {
            return defaulter.getAsBoolean();
        }
        String propertyValue = (String)value;
        if (propertyValue.length() == 0) {
            return true;
        }
        return Boolean.parseBoolean(propertyValue);
    }

    // public to allow use by tests
    public String getPropertyName() {
        return "feature." + name;
    }

    private boolean implied(Map<String, ?> configuration) {
        for (EarlyAccessFeature feature : impliedBy) {
            if (feature.isEnabled(configuration)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return format("\"%s\" (%s)", name, description);
    }
}
