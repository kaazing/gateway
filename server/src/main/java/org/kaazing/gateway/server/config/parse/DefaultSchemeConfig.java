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
package org.kaazing.gateway.server.config.parse;

import org.kaazing.gateway.server.config.SchemeConfig;

public class DefaultSchemeConfig implements SchemeConfig {

    private String name;
    private int defaultPort = -1;
    private String transportName;

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setDefaultPort(int defaultPort) {
        this.defaultPort = defaultPort;
    }

    @Override
    public int getDefaultPort() {
        return defaultPort;
    }

    public void setTransportName(String transportName) {
        this.transportName = transportName;
    }

    @Override
    public String getTransportName() {
        return transportName;
    }

}
