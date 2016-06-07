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
package org.kaazing.gateway.transport.wsn.extensions.test;

import static org.kaazing.gateway.util.Utils.asByteArray;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.net.ProtocolException;
import java.nio.ByteBuffer;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.ws.WsResourceAddress;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.transport.ws.WsFilterAdapter;
import org.kaazing.gateway.transport.ws.WsTextMessage;
import org.kaazing.gateway.transport.ws.extension.ExtensionHeader;
import org.kaazing.gateway.transport.ws.extension.ExtensionHelper;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtension;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtensionFactorySpi;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.session.IoSessionEx;

public class ExtensionWithFilterIT {

    private final K3poRule robot = new K3poRule();

    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .accept("wsn://localhost:8001/echo")
                            .type("echo")
                            .crossOrigin()
                                .allowOrigin("*")
                            .done()
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, robot);

    @Specification("should.transform.messages")
    @Test
    public void shouldTransformMessages() throws Exception {
        robot.finish();
    }

    public static class ExtensionFactory extends WebSocketExtensionFactorySpi {

        @Override
        public String getExtensionName() {
            return "filter-test";
        }

        @Override
        public WebSocketExtension negotiate(ExtensionHeader header, ExtensionHelper extensionHelper, WsResourceAddress address) throws ProtocolException {
            return new Extension(header, extensionHelper);
        }
    }

    public static class Extension extends WebSocketExtension  {
        private final ExtensionHeader extension;

        public Extension(ExtensionHeader extension, ExtensionHelper extensionHelper) {
            super(extensionHelper);
            this.extension = extension;
        }

        @Override
        public ExtensionHeader getExtensionHeader() {
            return extension;
        }

        @Override
        public IoFilter getFilter() {
            return new ExtensionFilter();
        }
    }

    public static class ExtensionFilter extends WsFilterAdapter {

        @Override
        // Repeats written payload separated by ':'
        protected Object doFilterWriteWsText(NextFilter nextFilter, IoSession session, WriteRequest writeRequest, WsTextMessage message)
                throws Exception {
            byte[] payload = asByteArray(message.getBytes().buf());
            ByteBuffer newPayload = ByteBuffer.allocate(payload.length * 2 + 1);
            newPayload.put(payload).put((byte)'-').put(payload);
            newPayload.flip();
            WsTextMessage newMessage = new WsTextMessage(((IoSessionEx)session).getBufferAllocator().wrap(newPayload));
            return newMessage;
        }

        @Override
        // Repeats read payload separated by '-'
        protected void wsTextReceived(NextFilter nextFilter, IoSession session, WsTextMessage message) throws Exception {
            byte[] payload = asByteArray(message.getBytes().buf());
            ByteBuffer newPayload = ByteBuffer.allocate(payload.length * 2 + 1);
            newPayload.put(payload).put((byte)':').put(payload);
            newPayload.flip();
            WsTextMessage newMessage = new WsTextMessage(((IoSessionEx)session).getBufferAllocator().wrap(newPayload));
            super.wsTextReceived(nextFilter, session, newMessage);
        }
    }

}
