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
package org.kaazing.gateway.transport.ws.bridge.extensions.pingpong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.session.IoSession;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.kaazing.gateway.security.auth.context.ResultAwareLoginContext;
import org.kaazing.gateway.transport.ws.extension.ExtensionHeader;
import org.kaazing.gateway.transport.ws.extension.ExtensionHeaderBuilder;
import org.kaazing.gateway.transport.ws.extension.ExtensionHelper;
import org.kaazing.gateway.util.Utils;

/**
* NOTE: this class is a "classic" unit test for the WsCloseFilter. Overall testing of websocket close
* handling for the wsn transport layer is in test class WsCloseTransportTest.
*/
public class PingPongExtensionTest {
    private static final String extensionName = "x-kaazing-ping-pong";
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

    private ExtensionHeader requested = new ExtensionHeaderBuilder(extensionName).done();
    
    @Rule
    public JUnitRuleMockery context = new  JUnitRuleMockery();


    @Test
    public void shouldAddControlBytesParameter() throws Exception {
        PingPongExtension extension = new PingPongExtension(requested, extensionHelper);
        assertEquals(extensionName, extension.getExtensionHeader().getExtensionToken());
        assertEquals(Utils.toHex(PingPongExtension.CONTROL_BYTES), extension.getExtensionHeader().getParameters().get(0).getName());
    }

    @Test
    public void shouldCreateIdleTimeoutFilter() {
        final ExtensionHelper extensionHelper = context.mock(ExtensionHelper.class);
        PingPongExtension extension = new PingPongExtension(requested, extensionHelper);
        IoFilter filter = extension.getFilter();
        assertTrue(filter instanceof PingPongFilter);
    }


}
