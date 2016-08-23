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
package org.kaazing.gateway.service.util;

import org.kaazing.gateway.service.ServiceProperties;
import org.kaazing.gateway.util.Utils;

public class ServiceUtils {

    public static final String LIST_SEPARATOR = ",";
    
    public static String getRequiredProperty(ServiceProperties properties, String propertyName) {
        String propertyValue = properties.get(propertyName);
        if (propertyValue == null) {
            throw new IllegalStateException("Missing required property \"" + propertyName + "\"");
        }
        return propertyValue;
    }

    public static int getRequiredIntProperty(ServiceProperties properties, String propertyName) {
        return Integer.parseInt(getRequiredProperty(properties, propertyName));
    }

    public static float getRequiredFloatProperty(ServiceProperties properties, String propertyName) {
        return Float.parseFloat(getRequiredProperty(properties, propertyName));
    }
    
    public static String getOptionalProperty(ServiceProperties properties, String propertyName, String defaultValue) {
        String propertyValue = properties.get(propertyName);
        if (propertyValue == null) {
            return defaultValue;
        }
        return propertyValue;
    }

    public static int getOptionalIntProperty(ServiceProperties properties, String propertyName, int defaultValue) {
        return Integer.parseInt(getOptionalProperty(properties, propertyName, Integer.toString(defaultValue)));
    }
    
    public static long getOptionalLongProperty(ServiceProperties properties, String propertyName, long defaultValue) {
        return Long.parseLong(getOptionalProperty(properties, propertyName, Long.toString(defaultValue)));
    }
    
    public static boolean getOptionalBooleanProperty(ServiceProperties properties, String propertyName, boolean defaultValue) {
        return Boolean.parseBoolean(getOptionalProperty(properties, propertyName, Boolean.toString(defaultValue)));
    }
    
    /**
     * Get a property which is a number of bytes expressed either as an integer (number of bytes) or an integer followed by K
     * (number of kilobytes) or an integer followed by M (for megabytes). Examples: 1048, 64k, 10M
     */
    public static int getOptionalDataSizeProperty(ServiceProperties properties, String propertyName, int defaultValue) {
        String value = getOptionalProperty(properties, propertyName, Integer.toString(defaultValue));
        return Utils.parseDataSize(value);
    }

}
