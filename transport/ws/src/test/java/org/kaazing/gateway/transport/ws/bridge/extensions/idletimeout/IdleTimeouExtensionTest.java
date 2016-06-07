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

public class IdleTimeouExtensionTest {
    private static final String extensionName = "x-kaazing-idle-timeout";
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
    
    @Rule
    public JUnitRuleMockery context = new  JUnitRuleMockery();


    @Test
    public void shouldAddTimeoutParameter() throws Exception {
        IdleTimeoutExtension extension = new IdleTimeoutExtension(requested, extensionHelper, 1234L);
        assertEquals(extensionName, extension.getExtensionHeader().getExtensionToken());
        assertEquals(Long.toString(1234L), extension.getExtensionHeader().getParameters().get(0).getValue());
    }

    @Test
    public void shouldCreateIdleTimeoutFilter() {
        final ExtensionHelper extensionHelper = context.mock(ExtensionHelper.class);
        IdleTimeoutExtension extension = new IdleTimeoutExtension(requested, extensionHelper, 1234L);
        IoFilter filter = extension.getFilter();
        assertTrue(filter instanceof IdleTimeoutFilter);
    }

}
