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

public class HttpProxyMultipleServicesIT {

    private final K3poRule k3po = new K3poRule();
    private final GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                            .property(EarlyAccessFeatures.HTTP_PROXY_SERVICE.getPropertyName(), "true")
                            .service()
                            .accept("http://localhost:8110/a")
                            .connect("http://localhost:8080/a")
                            .name("Proxy Service 1")
                            .type("http.proxy")
                            .connectOption("http.keepalive", "disabled")
                            .done()


                            .service()
                            .accept("wse://localhost:8110/d")
                            .connect("wse://localhost:8080/d")
                            .name("Proxy Service wse")
                            .type("http.proxy")
                            .done()
                    .done();
                // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, k3po);

    @Ignore
    @Test
    @Specification("http.proxy.test01")
    public void usingWseAndHttpServices() throws Exception {
        k3po.finish();
    }

}

