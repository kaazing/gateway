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
package org.kaazing.gateway.resource.address;

import static java.lang.String.format;

import java.util.Map;

public abstract class ResourceOption<V> {

    private final int identity;
    private final String name;
    private final V defaultValue;
    
    protected ResourceOption(Map<String, ResourceOption<?>> optionNames, String name) {
        this(optionNames, name, null);
    }

    protected ResourceOption(Map<String, ResourceOption<?>> optionNames, String name, V defaultValue) {
        if (optionNames.containsKey(name)) {
            throw new IllegalArgumentException(format("Duplicate option '%s'", name));
        }

        this.identity = optionNames.size();
        this.name = name;
        this.defaultValue = defaultValue;

        // detect duplicates next time
        optionNames.put(name, this);
    }

    public int identity() {
        return identity;
    }

    public String name() {
        return name;
    }

    public V defaultValue() {
        return defaultValue;
    }

    public V resolveValue(V value) {
        return (value != null) ? value : defaultValue;
    }

    @Override
    public String toString() {
        return name;
    }
}