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
package org.kaazing.gateway.service.messaging;

import java.util.Map.Entry;

/**
 * Common interface for objects that can contain arbitrary collections of attributes.
 */
public interface AttributeStore{

    /**
     * Get attribute by name.
     * 
     * @param name attribute name
     * @return attribute value
     */
    Object getAttribute(String name);
    
    /**
     * Remove attribute by name.
     * 
     * @param name attribute name
     * @return
     */
    Object removeAttribute(String name);
    
    /**
     * Set attribute with specified name to the specified value.
     * 
     * @param name attribute name
     * @param value attribute value
     * @return previous attribute value, or null if it did not exist
     */
    Object setAttribute(String name, Object value);
    
    /**
     * Get an iterable object over all attributes.
     * 
     * @return iterable over attributes
     */
    Iterable<Entry<String, Object>> attributes();
    
}
