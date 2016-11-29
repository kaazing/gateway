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

import static org.kaazing.gateway.service.TransportOptionNames.HTTP_KEEP_ALIVE;
import static org.kaazing.gateway.service.TransportOptionNames.HTTP_KEEP_ALIVE_TIMEOUT_KEY;
import static org.kaazing.gateway.service.TransportOptionNames.HTTP_SERVER_HEADER_ENABLED;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.kaazing.gateway.server.context.GatewayContext;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.service.AcceptOptionsContext;
import org.kaazing.gateway.service.ConnectOptionsContext;
import org.kaazing.gateway.service.ServiceContext;

public class ServiceDefaultsTest {

    @Test
    public void testDefaultConnectOptions() throws Exception {
        //@formatter:off
        final String sslCipherValue = "LOW";

        GatewayConfiguration gc = new GatewayConfigurationBuilder()
            .serviceDefaults()
                .connectOption("ssl.ciphers", sslCipherValue)
                .connectOption("ssl.protocols", "TLSv1")
                .connectOption("ssl.encryption", "disabled")
                .connectOption("udp.interface", "en0")
                .connectOption("tcp.transport", "socks://localhost:8000")
                .connectOption("http.keepalive", "disabled")
                .connectOption("http.keepalive.timeout", "5sec")
                .connectOption("http.keepalive.connections", "7")
                .done()
            .service()
                .type("echo")
                .name("test1")
                .accept("ws://localhost:8000")
            .done()
        .done();
        //@formatter:on

        Gateway gateway = new Gateway();
        GatewayContext gatewayContext = gateway.createGatewayContext(gc);
        ServiceContext service = (ServiceContext) gatewayContext.getServices().toArray()[0];
        ConnectOptionsContext connectOptionsContext = service.getConnectOptionsContext();
        // LOW Ciphers get converted to real ssl ciphers
        Map<String, Object> connectOptionsMap = connectOptionsContext.asOptionsMap();
        Assert.assertNotNull(((String[]) connectOptionsMap.get("ssl.ciphers"))[0]);

        String[] sslProtocols = (String[]) connectOptionsMap.get("ssl.protocols");
        Assert.assertTrue("TLSv1".equals(sslProtocols[0]));

        Assert.assertTrue("en0".equals(connectOptionsMap.get("udp.interface")));
        Assert.assertEquals("socks://localhost:8000", connectOptionsMap.get("tcp.transport").toString().trim());
        Assert.assertFalse((Boolean)connectOptionsMap.get("ssl.encryptionEnabled"));

        Assert.assertEquals(5, connectOptionsMap.get("http[http/1.1]."+HTTP_KEEP_ALIVE_TIMEOUT_KEY));
        Assert.assertFalse((Boolean) connectOptionsMap.get("http[http/1.1]."+HTTP_KEEP_ALIVE));
        Assert.assertEquals(7, connectOptionsMap.get("http[http/1.1].keepalive.connections"));
    }

    @Test
    public void testDefaultAcceptOptions() throws Exception {
        //@formatter:off
        GatewayConfiguration gc = new GatewayConfigurationBuilder()
                .serviceDefaults()
                    .acceptOption("http.server.header", "disabled")
                .done()
                .service()
                    .type("echo")
                    .name("test1")
                    .accept("ws://localhost:8000")
                .done()
        .done();
        //@formatter:on

        Gateway gateway = new Gateway();
        GatewayContext gatewayContext = gateway.createGatewayContext(gc);
        ServiceContext service = (ServiceContext) gatewayContext.getServices().toArray()[0];
        AcceptOptionsContext acceptOptionsContext = service.getAcceptOptionsContext();
        Map<String, Object> acceptOptions = acceptOptionsContext.asOptionsMap();
        Assert.assertFalse((Boolean) acceptOptions.get(HTTP_SERVER_HEADER_ENABLED));
    }
}
