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
package org.kaazing.gateway.server.context.resolve;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.kaazing.gateway.service.ServiceProperties;

public class DefaultServiceProperties implements ServiceProperties {
    private static final Map<String, String> EMPTY_SIMPLE = Collections.emptyMap();
    private static final Map<String, List<ServiceProperties>> EMPTY_NESTED =
            Collections.emptyMap();
    private static final List<ServiceProperties> EMPTY_LIST = Collections.emptyList();

    private Map<String, String> simpleProperties = EMPTY_SIMPLE;
    private Map<String, List<ServiceProperties>> nestedProperties = EMPTY_NESTED;

    public boolean containsSimpleProperty(String name) {
        return simpleProperties.containsKey(name);
    }

    /**
     * @see org.kaazing.gateway.server.context.resolve.DefaultServiceProperties#get(java.lang.String)
     */
    @Override
    public String get(String name) {
        return simpleProperties.get(name);
    }

    @Override
    public List<ServiceProperties> getNested(String name) {
        List<ServiceProperties> result = nestedProperties.get(name);
        return result == null ? EMPTY_LIST : result;
    }

    @Override
    public List<ServiceProperties> getNested(String name, boolean create) {
        if (!create) {
            return getNested(name);
        }
        if (nestedProperties == EMPTY_NESTED) {
            nestedProperties = new HashMap<>();
        }
        List<ServiceProperties> result = nestedProperties.get(name);
        if (result == null) {
            result = new LinkedList<>();
            nestedProperties.put(name, result);
        }
        return result;
    }

    @Override
    public Iterable<String> nestedPropertyNames() {
        return nestedProperties.keySet();
    }

    @Override
    public boolean isEmpty() {
        return simpleProperties.isEmpty() && nestedProperties.isEmpty();
    }

    @Override
    public Iterable<String> simplePropertyNames() {
        return simpleProperties.keySet();
    }

    @Override
    public void put(String name, String value) {
        if (simpleProperties == EMPTY_SIMPLE) {
            simpleProperties = new HashMap<>();
        }
        simpleProperties.put(name, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultServiceProperties that = (DefaultServiceProperties) o;

        if (nestedProperties != null ? !nestedProperties.equals(that.nestedProperties) : that.nestedProperties != null) {
            return false;
        }
        if (simpleProperties != null ? !simpleProperties.equals(that.simpleProperties) : that.simpleProperties != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = simpleProperties != null ? simpleProperties.hashCode() : 0;
        result = 31 * result + (nestedProperties != null ? nestedProperties.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return simpleProperties.toString() + nestedProperties.toString();
    }

    String remove(String name) {
        return simpleProperties == null ? null : simpleProperties.get(name);
    }

}
