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
package org.kaazing.gateway.transport.http.acceptor.specification.rfc7230;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.service.IoHandler;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpAcceptorRule;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpVersion;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MethodExecutionTrace;

/**
 * Test to validate behavior as specified in <a href="https://tools.ietf.org/html/rfc7231#section-4">RFC 7231 section 4:
 * Request Methods</a>.
 */
public class ArchitectureIT {
    private static final ResourceAddress HTTP_ADDRESS = httpAddress();

    private final HttpAcceptorRule acceptor = new HttpAcceptorRule();

    private JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private TestRule contextRule = ITUtil.toTestRule(context);
    private final TestRule trace = new MethodExecutionTrace();
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/http/rfc7230/architecture");
    private final TestRule timeoutRule = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(acceptor).around(contextRule).around(k3po).around(timeoutRule);

    @Test
    @Specification({"outbound.must.send.version/request"})
    public void outboundMustSendVersion() throws Exception {
        testHttpVersionNoResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"inbound.must.send.version/request"})
    public void inBoundMustSendVersion() throws Exception {
        testHttpVersionNoResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"response.must.be.400.on.invalid.version/request"})
    public void inboundMustSend400OnInvalidVersion() throws Exception {
        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>();
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
    }

    @Test
    @Specification({"inbound.must.reply.with.version.one.dot.one.when.received.higher.minor.version/request"})
    public void inboundMustReplyWithVersionOneDotOneWhenReceivedHigherMinorVersion() throws Exception {
        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>();
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
    }

    @Test
    @Specification({"origin.server.should.send.505.on.major.version.not.equal.to.one/request"})
    public void originServerShouldSend505OnMajorVersionNotEqualToOne() throws Exception {
        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>();
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
    }

    @Test
    @Specification({"client.must.send.host.identifier/request"})
    public void clientMustSendHostIdentifier() throws Exception {
        testHttpVersionNoResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"inbound.must.reject.requests.missing.host.identifier/request"})
    public void inboundMustRejectRequestsMissingHostIdentifier() throws Exception {
        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>();
        acceptor.bind(HTTP_ADDRESS, acceptHandler);
        k3po.finish();
    }

    @Ignore("BUG: 404 Not Found response to bad URI request")
    @Test
    @Specification({"inbound.must.reject.requests.with.user.info.on.uri/request"})
    public void inboundMustRejectRequestWithUserInfoOnURI() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"inbound.should.allow.requests.with.percent.chars.in.uri/request"})
    public void inboundShouldAllowRequestsWithPercentCharsInURI() throws Exception {
        testHttpVersionNoResponseMessage(HTTP_ADDRESS);
    }

    private void testHttpVersionNoResponseMessage(ResourceAddress address) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();

                session.setStatus(HttpStatus.SUCCESS_OK);

                session.close(false);
            }
        };
        acceptor.bind(address, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    private static ResourceAddress httpAddress() {
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        String address = "http://localhost:8080/";
        return addressFactory.newResourceAddress(address);
    }
}
