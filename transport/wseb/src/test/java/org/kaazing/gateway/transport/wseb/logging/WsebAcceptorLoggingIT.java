/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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

package org.kaazing.gateway.transport.wseb.logging;

import static org.kaazing.test.util.ITUtil.createRuleChain;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.spi.LoggingEvent;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

import org.kaazing.gateway.util.InternalSystemProperty;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.test.util.MemoryAppender;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;

public class WsebAcceptorLoggingIT {

    private final K3poRule k3po = new K3poRule()
            .setScriptRoot("org/kaazing/specification/wse/data/binary");

    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .property(InternalSystemProperty.WSE_SPECIFICATION.getPropertyName(), "true")
                        .service()
                            .accept(URI.create("ws://localhost:8080/path"))
                            .type("echo")
                            .crossOrigin()
                                .allowOrigin("http://localhost:8001")
                            .done()
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, k3po);

    @Test
    @Specification({
        "echo.payload.length.127/request"
        })
    public void shouldLogOpenWriteReceivedAndClose() throws Exception {
        k3po.finish();
        List<String> expectedPatterns = new ArrayList<String>(Arrays.asList(new String[] {
                "tcp#.*OPENED.*",
                "tcp#.*WRITE.*",
                "tcp#.*CLOSED.*",
                "http#.*OPENED",
                "http#.*RECEIVED",
                "http#.*CLOSED",
                "wseb#.*OPENED",
                "wseb#.*RECEIVED",
                "wseb#.*CLOSED"
            }));

            for (LoggingEvent event : MemoryAppender.getEvents()) {
                String message = event.getMessage().toString();
                if (message.matches(".*\\[.*#.*].*")) {
                    System.out.println(message);
                    Iterator<String> iterator = expectedPatterns.iterator();
                    while (iterator.hasNext()) {
                        String pattern = iterator.next();
                        if (message.matches(".*" + pattern + ".*")) {
                            iterator.remove();
                        }
                    }
                }
            }
            assertTrue("The following patterns of log messages were not logged: " + expectedPatterns,
                    expectedPatterns.isEmpty());
    }

}
