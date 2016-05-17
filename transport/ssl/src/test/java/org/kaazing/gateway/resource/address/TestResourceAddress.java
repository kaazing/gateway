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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

final class TestResourceAddress extends ResourceAddress {
    private static final long serialVersionUID = 1L;

    static final TestResourceOption<String> OPTION = new TestResourceOption<>("option");

    private String option;

    TestResourceAddress(ResourceAddressFactorySpi factory, String externalURI, URI resourceURI) {
        super(factory, externalURI, resourceURI);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <V> V getOption0(ResourceOption<V> option) {

        if (option == OPTION) {
            return (V) this.option;
        }

        return super.getOption0(option);
    }

    <T> void setOption(TestResourceOption<T> key, T value) {

        if (key == OPTION) {
            option = (String) value;
        }

    }

    static final class TestResourceOption<V> extends ResourceOption<V> {

        private static final Map<String, ResourceOption<?>> OPTION_NAMES =  new HashMap<>();

        public TestResourceOption(String name) {
            super(OPTION_NAMES, name);
        }
    }
}
