package org.kaazing.gateway.service.amqp.amqp091;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.net.URI;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class AmqpOpenHandshakeIT {


    private K3poRule k3po = new K3poRule().setScriptRoot("./");

    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                .service()
                    .accept(URI.create("ws://localhost:8001/amqp"))
                    .connect(URI.create("tcp://localhost:8010"))
                    .type("amqp.proxy")
                    .property("service.domain","localhost")
                    .property("encryption.key.alias", "session")
                    .realmName("demo")
                    .authorization()
                        .requireRole("AUTHORIZED")
                    .done()
                .done()
                .security()
                    .realm()
                        .name("demo")
                        .description("Kaazing WebSocket Gateway Demo")
                        .httpChallengeScheme("Application Basic")
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
    @Specification({ "org/kaazing/specification/amqp/ws/ws.connect", "ws/open/identity/request", 
                    "org/kaazing/gateway/service/amqp/amqp091/ws.accept", "ws/open/identity/response"})
    public void test() throws Exception {
            k3po.finish();
    }

}
