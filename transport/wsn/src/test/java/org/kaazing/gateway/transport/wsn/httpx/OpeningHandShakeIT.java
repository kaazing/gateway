package org.kaazing.gateway.transport.wsn.httpx;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.rules.RuleChain.outerRule;

import java.net.URI;

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

public class OpeningHandShakeIT {

    private TestRule trace = new MethodExecutionTrace();
    private TestRule timeout = new DisableOnDebug(new Timeout(4, SECONDS));
    private final K3poRule robot = new K3poRule().setScriptRoot("org/kaazing/specification/httpx/opening");

    public GatewayRule gateway = new GatewayRule() {
        {

            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                    .service()
                        .accept(URI.create("wsn://localhost:8000/echo"))
                        .type("echo")
                        .crossOrigin()
                            .allowOrigin("*")
                        .done()
                    .done()
                .done();
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = outerRule(trace).around(robot).around(gateway).around(timeout);

    @Test
    @Specification("extended.handshake.protocol.negotiated/request")
    public void shouldPassNegotiatingProtocol() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("extended.handshake.protocol.not.negotiated/request")
    public void shouldFailNotNegotiatingProtocol() throws Exception {
        robot.finish();
    }

}
