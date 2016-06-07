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

import java.util.HashMap;
import java.util.Map;


// provides strongly-typed key-value pairs even though different entries may have different types
public interface ResourceOptions {

    <T> T setOption(ResourceOption<T> key, T value);

    <T> T getOption(ResourceOption<T> key);

    <T> boolean hasOption(ResourceOption<T> key);

    interface Factory {
        
        ResourceOptions newResourceOptions();
        
        ResourceOptions newResourceOptions(ResourceOptions defaults);
    }

    Factory FACTORY = new  ResourceOptions.Factory() {

        @Override
        public ResourceOptions newResourceOptions() {
            return new ResourceOptionsImpl();
        }
        
        @Override
        public ResourceOptions newResourceOptions(final ResourceOptions defaults) {
            return new ResourceOptionsImpl() {
                @Override
                public <T> boolean hasOption(ResourceOption<T> key) {
                    return super.hasOption(key) || defaults.hasOption(key);
                }

                @Override
                protected <T> T getOption0(ResourceOption<T> key) {
                    T option = super.getOption0(key);
                    if (option == null && !super.hasOption(key)) {
                        option = defaults.getOption(key);
                    }
                    return option;
                }
            };
        }

        class ResourceOptionsImpl implements ResourceOptions {
            private final Map<ResourceOption<?>, Object> optionsByKey = new HashMap<>();
        
            @Override
            @SuppressWarnings("unchecked")
            public final <T> T setOption(ResourceOption<T> key, T value) {
                return (T) optionsByKey.put(key, value);
            }
        
            @Override
            public final <T> T getOption(ResourceOption<T> key) {
                return key.resolveValue(getOption0(key));
            }
            
            @Override
            public <T> boolean hasOption(ResourceOption<T> key) {
                return optionsByKey.containsKey(key);
            }

            @SuppressWarnings("unchecked")
            protected <T> T getOption0(ResourceOption<T> key) {
                return (T) optionsByKey.get(key);
            }
            
            @Override
            public String toString() {
                return optionsByKey.toString();
            }
        }
        
    };

}