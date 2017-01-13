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
package org.kaazing.gateway.transport.http.connector.specification.rfc7231;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.transport.http.HttpConnectSession;
import org.kaazing.gateway.transport.http.HttpConnectorRule;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.session.IoSessionEx;

/**
 * Test to validate behavior as specified in <a href="https://tools.ietf.org/html/rfc7231#section-4">RFC 7231 section 4:
 * Request Methods</a>.
 */
public class RedirectionIT {
    private final HttpConnectorRule connector = new HttpConnectorRule();
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/http/rfc7231/redirection");

    @Rule
    public TestRule chain = createRuleChain(connector, k3po);

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private final IoHandler handler = context.mock(IoHandler.class);

    private static class ConnectSessionInitializer implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.GET);
        }
    }

    @Test
    @Specification({"absolute.location/response"})
    public void absoluteLocationHeader() throws Exception {
        context.checking(new Expectations() {{
            allowing(handler).sessionOpened(with(any(IoSessionEx.class)));
            allowing(handler).sessionCreated(with(any(IoSessionEx.class)));
            allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
        }});
        connector.getConnectOptions().put("http.maximum.redirects", 1);
        connector.connect("http://localhost:8000/resource#fragment", handler, new ConnectSessionInitializer());
        k3po.finish();
    }

    @Test
    @Specification({"change.fragment/response"})
    public void changeFragment() throws Exception {
        context.checking(new Expectations() {{
            allowing(handler).sessionOpened(with(any(IoSessionEx.class)));
            allowing(handler).sessionCreated(with(any(IoSessionEx.class)));
            allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
        }});
        connector.getConnectOptions().put("http.maximum.redirects", 1);
        connector.connect("http://localhost:8000/resource#fragment", handler, new ConnectSessionInitializer());
        k3po.finish();
    }

    @Test
    @Specification({"relative.location/response"})
    public void relativeLocation() throws Exception {
        context.checking(new Expectations() {{
            allowing(handler).sessionOpened(with(any(IoSessionEx.class)));
            allowing(handler).sessionCreated(with(any(IoSessionEx.class)));
            allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
        }});
        connector.getConnectOptions().put("http.maximum.redirects", 1);
        connector.connect("http://localhost:8000/resource#fragment", handler, new ConnectSessionInitializer());
        k3po.finish();
    }
}
