package org.kaazing.gateway.transport.wsn.auth;

import static org.junit.rules.RuleChain.outerRule;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.MethodExecutionTrace;

public class AuthorizationWithoutRealmIT {
    
    private TestRule trace = new MethodExecutionTrace();
    
    private TestRule timeout = new DisableOnDebug(new Timeout(7, TimeUnit.SECONDS));

    private final K3poRule robot = new K3poRule();

    public GatewayRule gateway = new GatewayRule() {
        {

            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                    .service() 
                        .accept(URI.create("ws://localhost:8000/echo")) // No authorization constraints
                        .type("echo")
                        .crossOrigin()
                            .allowOrigin("*")
                        .done()
                    .done()
                    .done();
            init(configuration); // , "log4j-diagnostic.properties");  // for debugging
        }
    };

    @Rule
    public TestRule chain = outerRule(trace).around(robot).around(gateway).around(timeout);

    @Specification("should.pass.with.no.realm.correct.user.login")
    @Test
    public void shouldPassWithNoRealmCorrectUserLogin() throws Exception  {
        robot.finish();
    }

    @Specification("should.pass.with.no.realm.incorrect.user.login")
    @Test
    public void shouldPassWithNoRealmIncorrectUserLogin() throws Exception  {
        robot.finish();
    }

}

