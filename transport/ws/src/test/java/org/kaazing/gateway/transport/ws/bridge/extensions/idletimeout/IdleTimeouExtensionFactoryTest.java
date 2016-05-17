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
package org.kaazing.gateway.transport.ws.bridge.extensions.idletimeout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.session.IoSession;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ws.WsResourceAddress;
import org.kaazing.gateway.security.auth.context.ResultAwareLoginContext;
import org.kaazing.gateway.transport.ws.extension.ExtensionHeader;
import org.kaazing.gateway.transport.ws.extension.ExtensionHeaderBuilder;
import org.kaazing.gateway.transport.ws.extension.ExtensionHelper;

/**
* NOTE: this class is a "classic" unit test for the WsCloseFilter. Overall testing of websocket close
* handling for the wsn transport layer is in test class WsCloseTransportTest.
*/
public class IdleTimeouExtensionFactoryTest {
    private static final String extensionName = "x-kaazing-idle-timeout";
    private static final Long TIMEOUT = 2500L;
    private static final ExtensionHelper extensionHelper = new ExtensionHelper() {

        @Override
        public void setLoginContext(IoSession session, ResultAwareLoginContext loginContext) {
            throw new RuntimeException("Not expected to be called");
        }

        @Override
        public void closeWebSocketConnection(IoSession session) {
            throw new RuntimeException("Not expected to be called");
        }
    };

    ExtensionHeader requested = new ExtensionHeaderBuilder(extensionName).done();

    @Test
    public void negotiateShouldAddTimeoutParameter() throws Exception {
        String addressURI = "ws://localhost:2020/";
        Map<String, Object> options = new HashMap<>();
        options.put("ws.inactivityTimeout", 2500L);
        WsResourceAddress address = (WsResourceAddress) ResourceAddressFactory.newResourceAddressFactory().newResourceAddress(addressURI, options);
        IdleTimeoutExtensionFactory factory = new IdleTimeoutExtensionFactory();
        IdleTimeoutExtension extension = (IdleTimeoutExtension) factory.negotiate(requested, extensionHelper, address);
        assertEquals(extensionName, extension.getExtensionHeader().getExtensionToken());
        assertEquals(Long.toString(TIMEOUT), extension.getExtensionHeader().getParameters().get(0).getValue());
    }

    @Test
    public void shouldNotNegotiateWhenNoInactivityTimeoutIsSet() throws Exception {
        String addressURI = "ws://localhost:2020/";
        Map<String, Object> options = new HashMap<>();
        WsResourceAddress address = (WsResourceAddress) ResourceAddressFactory.newResourceAddressFactory().newResourceAddress(addressURI, options);
        IdleTimeoutExtensionFactory factory = new IdleTimeoutExtensionFactory();
        IdleTimeoutExtension extension = (IdleTimeoutExtension) factory.negotiate(requested, extensionHelper, address);
        assertNull(extension);
    }

}
