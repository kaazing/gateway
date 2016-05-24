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

import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.server.context.SchemeContext;

public class DefaultSchemeContext implements SchemeContext {

    private final String name;
    private final int defaultPort;


    public DefaultSchemeContext(String name, int defaultPort, ResourceAddressFactory resourceAddressFactory) {
        this.name = name;
        this.defaultPort = defaultPort;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getDefaultPort() {
        return defaultPort;
    }

}
