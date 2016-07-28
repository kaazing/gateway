package org.kaazing.gateway.service.proxy;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.rules.RuleChain.outerRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class TurnProxyIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/gateway/service/turn/proxy");

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public final TestRule chain = outerRule(k3po).around(timeout);

    @Test
    @Specification({"default.turn.protocol.test/request",
            "default.turn.protocol.test/response"} )
    public void shouldPassWithDefaultTurnProtocolTest() throws Exception {
        k3po.finish();
    }

}
