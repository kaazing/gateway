package org.kaazing.gateway.service.http.proxy;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.util.feature.EarlyAccessFeatures;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class HttpProxyConnectionDifferentPortIT {

    private final K3poRule k3po = new K3poRule();
    private final GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                            .property(EarlyAccessFeatures.HTTP_PROXY_SERVICE.getPropertyName(), "true")
                            .service()
                            .accept("http://localhost:8110")
                            .connect("http://localhost:8080")
                            .name("Proxy Service 1")
                            .type("http.proxy")
                            .connectOption("http.keepalive", "disabled")
                            .done()

                            .service()
                            .accept("http://localhost:8111")
                            .connect("http://localhost:8081")
                            .type("http.proxy")
                            .done()
                            .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, k3po);


    @Test
    @Specification("test/http.test")
    public void httppath() throws Exception {
        k3po.finish();
    }

}