package org.kaazing.gateway.service.turn.rest;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class TurnRestWithLoginModuleIT {
    private static final String ACCEPT_URL = "http://localhost:8000/";

    private final K3poRule robot = new K3poRule();

    private final GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .accept(ACCEPT_URL)
                            .type("turn.rest")
                            .realmName("turn")
                                .authorization()
                                .requireRole("mbzrxpgjys")
                            .done()
                            .property("generate.credentials", "class:" + TestCredentialGenerator.class.getName())
                            .nestedProperty("options")
                                .property("secret", "secret")
                                .property("symbol", ":")
                                .nestedProperty("uris")
                                    .property("uri", "uri1")
                                    .property("uri", "uri2")
                                    .property("uri", "uri3")
                                .done()
                            .done()
                        .done()
                        .security()
                            .realm()
                                .name("turn")
                                .description("TURN REST Login Module Test")
                                .httpChallengeScheme("Basic")
                                .loginModule()
                                    .type("class:" + TestTurnRestLoginModule.class.getName())
                                    .success("requisite")
                                    .option("roles", "mbzrxpgjys")
                                .done()
                            .done()
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, robot);
    
    @Specification("login.module.generate.valid.response")
    @Test
    public void generateValidResponse() throws Exception {
        robot.finish();
    }
}
