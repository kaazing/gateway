/**
 * Copyright (c) 2007-2014, Kaazing Corporation. All rights reserved.
 */

package org.kaazing.gateway.service.proxy;

import java.net.URI;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.ITUtil;

public class ProxyServiceExtensionIT {
    private K3poRule k3po = new K3poRule();

    public GatewayRule gateway = new GatewayRule() {
        {
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                .service()
                    .accept(URI.create("tcp://localhost:8001"))
                    .connect(URI.create("tcp://localhost:8002"))
                    .type("proxy")
                    .crossOrigin()
                        .allowOrigin("*")
                    .done()
                .done()
            .done();

            init(configuration);
        }
    };

    @Rule
    public TestRule chain = ITUtil.createRuleChain(gateway, k3po);

    @Specification("shouldInjectBytesBeforeForwardingMessages")
    @Test
    public void shouldInjectBytesBeforeForwardingMessages() throws Exception {
        k3po.finish();
    }

}
