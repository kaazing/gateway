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
package org.kaazing.gateway.resource.address.ws;

import static org.kaazing.gateway.resource.address.ResourceFactories.changeSchemeOnly;

import org.kaazing.gateway.resource.address.ResourceFactory;

public class MockWsxSsl extends WssResourceAddressFactorySpi {

    @Override
    public String getSchemeName() {
        return "wsx+ssl";
    }

    @Override
    protected ResourceFactory getTransportFactory() {
        return changeSchemeOnly("httpx+ssl");
    }

    @Override
    protected String getRootSchemeName() {
        return "wss";
    }

}
