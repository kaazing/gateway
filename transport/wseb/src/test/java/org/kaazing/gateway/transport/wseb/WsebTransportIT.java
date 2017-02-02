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
package org.kaazing.gateway.transport.wseb;

import static org.kaazing.gateway.util.InternalSystemProperty.TCP_PROCESSOR_COUNT;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class WsebTransportIT {
    private final K3poRule robot = new K3poRule();

    private final GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .webRootDirectory(new File("src/test/webapp"))
                        .tempDirectory(new File("src/test/temp"))
                        .property(TCP_PROCESSOR_COUNT.getPropertyName(), "64")
                            /*
                        .service()
                            .accept(URI.create("http://localhost:8000/"))
                            .type("directory")
                            .property("directory", "/extras")
                        .done()
                        */
                        .service()
                            .accept("wse://localhost:8000/echo")
                            .type("echo")
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, robot);

    @Specification("echo.aligned.downstream")
    @Test
    public void testEchoAlignedDownstream() throws Exception {
        robot.finish();
    }

    @Specification("echo.aligned.downstream.with.sequence.number")
    @Test
    public void testEchoAlignedDownstreamWithSequenceNumber() throws Exception {
        robot.finish();
    }

    @Specification("echo.aligned.downstream.with.sequence.number.ksn")
    @Test
    public void testEchoAlignedDownstreamWithSequenceNumberKsn() throws Exception {
        robot.finish();
    }

    @Specification("echo.no.kaazing.handshake.protocol.negotiated")
    @Test
    public void testXKaazingHandshakeMustNOTBeNegotiatedForWseHandhakeRequests() throws Exception {
        robot.finish();
    }

    @Specification("propagate.create.query.params")
    @Test
    public void shouldPropagateQueryParametersFromCreateToUpstreamAndDownstream() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("echo.text.and.binary")
    public void echoTextAndBinary() throws Exception {
        robot.finish();
    }

    @Specification("echo.javascript.downstream.with.sequence.number.ksn")
    @Test
    public void testJavascriptSequenceNumberKsn() throws Exception {
        robot.finish();
    }

}
