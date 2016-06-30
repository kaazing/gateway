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
package org.kaazing.gateway.transport.wseb;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.transport.ws.AbstractWsBridgeSession.LAST_ROUND_TRIP_LATENCY;
import static org.kaazing.gateway.transport.ws.AbstractWsBridgeSession.LAST_ROUND_TRIP_LATENCY_TIMESTAMP;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.GatewayObserver;
import org.kaazing.gateway.server.Launcher;
import org.kaazing.gateway.server.context.GatewayContext;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.session.IoSessionEx;

public class WsebRoundTripLatencyIT {

    private K3poRule k3po = new K3poRule();
    private GatewayContext context;
    private final Launcher launcher = new Launcher(GatewayObserver.newInstance());

    @Before
    public void StartGateway() throws Throwable {
        // @formatter:off
        GatewayConfiguration configuration =
                new GatewayConfigurationBuilder()
                    .service()
                        .accept("wse://localhost:8123/echo")
                        .acceptOption("ws.inactivity.timeout", "2sec")
                        .type("echo")
                    .done()
                .done();
        // @formatter:on

        context = new Gateway().createGatewayContext(configuration);
        launcher.init(context);
    }

    @After
    public void StopGateway() throws Throwable {
        context = null;
        launcher.destroy();
    }

    @Rule
    public TestRule chain = createRuleChain(k3po, 15, TimeUnit.SECONDS);

    @Specification("shouldPropagateWseRoundTripLatencyAttributesToSession")
    @Test
    public void shouldPropagateRoundTripLatencyAttributesToSession() throws Exception {
        Long latency = null;
        Long latencyTimestamp = null;
        Long currentTimestamp = currentTimeMillis();

        k3po.start();

        k3po.awaitBarrier("SESSION_ESTABLISHED");

        for (ServiceContext serviceContext : context.getServices()) {
            for (IoSessionEx session : serviceContext.getActiveSessions()) {
                while (session != null) {
                    latency = LAST_ROUND_TRIP_LATENCY.get(session);
                    latencyTimestamp = LAST_ROUND_TRIP_LATENCY_TIMESTAMP.get(session);
                    if (latency == null && session instanceof BridgeSession) {
                        session = ((BridgeSession) session).getParent();
                    } else {
                        break;
                    }
                }
            }
        }

        k3po.notifyBarrier("READ_LATENCY_ATTRIBUTES");

        assertTrue("Could not retrieve Round Trip Latency from Jmx", latency != null && latency > -1);
        assertTrue("Could not retrieve Round Trip Latency Timestamp from Jmx", latencyTimestamp > currentTimestamp);

        k3po.finish();
    }

}
