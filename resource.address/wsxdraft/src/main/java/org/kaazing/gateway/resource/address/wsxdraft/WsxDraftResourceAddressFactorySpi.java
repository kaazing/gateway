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
package org.kaazing.gateway.resource.address.wsxdraft;

import static org.kaazing.gateway.resource.address.ResourceFactories.changeSchemeOnly;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.CODEC_REQUIRED;

import org.kaazing.gateway.resource.address.ResourceFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.resource.address.ws.WsResourceAddress;
import org.kaazing.gateway.resource.address.wsdraft.WsDraftResourceAddressFactorySpi;

public class WsxDraftResourceAddressFactorySpi extends WsDraftResourceAddressFactorySpi {

    public static final String SCHEME_NAME = "wsx-draft";

    private static final ResourceFactory TRANSPORT_FACTORY = changeSchemeOnly("httpx-draft");

    static final String PROTOCOL_NAME = "ws/draft-7x";

    @Override
    public String getSchemeName() {
        return SCHEME_NAME;
    }

    @Override
    protected String getProtocolName() {
        return PROTOCOL_NAME;
    }

    @Override
    protected ResourceFactory getTransportFactory() {
        return TRANSPORT_FACTORY;
    }

    @Override
    protected void setOptions(WsResourceAddress address, ResourceOptions options, Object qualifier) {
        // Extended websocket sessions do not have a codec, by design.
        options.setOption(CODEC_REQUIRED, Boolean.FALSE);
        super.setOptions(address, options, qualifier);
    }

    @Override
    protected String getRootSchemeName() {
        return "ws";
    }
}
