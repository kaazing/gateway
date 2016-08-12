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
package org.kaazing.gateway.service;

import java.util.List;

/**
 * Represents a set of regular (String) properties and/or nested properties. Nested properties are ones which are
 * grouped under one or more instances of an owning parent element, whose local name is the name of the nested property.
 * Example:
 * <pre>{@code
 *   <service>
 *     ...
 *     <properties>
 *       <regularProperty>a string value</regularProperty>
 *       <nestedProperty>
 *         <name>arrivals</name>
 *         <property1>value1</property1>
 *         <property2>value1</property2>
 *       </nestedProperty>
 *       <nestedProperty>
 *         <name>departures</name>
 *         <property1>value1</property1>
 *         <property2>value1</property2>
 *       </nestedProperty>
 *     </properties>
 * </service>}
 * </pre>
 * In this case, getNested("nestedProperty") returns a list containing two ServiceProperties objects, representing the
 * two sets of properties contained inside parent elements with that name.
 */
public interface ServiceProperties {

    String get(String name);

    /**
     * Returns a nested property value (list of property sets). This list may not be mutable.
     * Use {@link #get(String, boolean)} to add or alter nested properties.
     * @param name
     * @return The nested property value, or an empty list if there is no such property
     */
    List<ServiceProperties> getNested(String name);

    /**
     * Returns a nested property value, optionally adding the property if it is not yet present.
     * @param name
     * @param create  If true, the nested property is created if not already present, and the
     *                list returned is always mutable, to allow the addition or removal of property sets
     * @return The nested property value, or an empty list if there is no such property
     */
    List<ServiceProperties> getNested(String name, boolean create);

    /**
     * @return Names of the the simple (String) properties
     */
    Iterable<String> simplePropertyNames();

    /**
     * @return Names of the nested properties
     */
    Iterable<String> nestedPropertyNames();

    boolean isEmpty();

    void put(String name, String value);

}
