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

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
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
import org.kaazing.gateway.transport.http.HttpHeaders;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MethodExecutionTrace;

/**
 * Test to validate behavior as specified in <a href="https://tools.ietf.org/html/rfc7231#section-4">RFC 7231 section 4:
 * Request Methods</a>.
 */
public class ConnectionManagementIT {
    private static final String ADDRESS = "http://localhost:8080/";
    private static final ResourceAddress HTTP_ADDRESS = httpAddress();

    private final HttpAcceptorRule acceptor = new HttpAcceptorRule();

    private JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private TestRule contextRule = ITUtil.toTestRule(context);
    private final TestRule trace = new MethodExecutionTrace();
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/http/rfc7230/connection.management");
    private final TestRule timeoutRule = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(acceptor).around(contextRule).around(k3po).around(timeoutRule);

    @Test
    @Specification({"client.must.close.connection.after.request.with.connection.close/request"})
    public void clientMustCloseConnectionAfterRequestWithConnectionClose() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        acceptor.getAcceptOptions().put("http.dateHeaderEnabled", Boolean.FALSE);
        acceptor.getAcceptOptions().put("http.serverHeaderEnabled", Boolean.FALSE);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();

                // write back the same message received
                session.setStatus(HttpStatus.SUCCESS_OK);
                session.setWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(0));
                session.close(false);
            }

        };
        acceptor.bind(ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"server.must.close.connection.after.response.with.connection.close/request"})
    public void serverMustCloseConnectionAfterResponseWithConnectionClose() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();

                // write back the same message received
                session.setStatus(HttpStatus.SUCCESS_OK);
                session.addWriteHeader(HttpHeaders.HEADER_CONTENT_TYPE, String.valueOf("text/plain"));
                session.addWriteHeader(HttpHeaders.HEADER_CONNECTION, String.valueOf("close"));
                session.close(true);
            }

        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"connections.should.persist.by.default/client"})
    public void connectionsShouldPersistByDefault() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        acceptor.getAcceptOptions().put("http.dateHeaderEnabled", Boolean.FALSE);
        acceptor.getAcceptOptions().put("http.serverHeaderEnabled", Boolean.FALSE);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.SUCCESS_OK);
                session.close(false);
            }

        };
        acceptor.bind(ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    private WriteFuture writeStringMessageToSession(String message, IoSession session) {
        ByteBuffer data = ByteBuffer.wrap(message.getBytes());
        IoBufferAllocatorEx<?> allocator = ((IoSessionEx) session).getBufferAllocator();
        return session.write(allocator.wrap(data));
    }

    @Test
    @Specification({"server.should.accept.http.pipelining/request"})
    public void serverShouldAcceptHttpPipelining() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        acceptor.getAcceptOptions().put("http.dateHeaderEnabled", Boolean.FALSE);
        acceptor.getAcceptOptions().put("http.serverHeaderEnabled", Boolean.FALSE);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();

                session.setStatus(HttpStatus.SUCCESS_OK);
                session.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(8));

                if (session.getRequestURI().toString().equals("/request1")) {
                    writeStringMessageToSession("request1", session);
                } else if (session.getRequestURI().toString().equals("/request2")) {
                    writeStringMessageToSession("request2", session);
                } else {
                    writeStringMessageToSession("request3", session);
                }

                session.close(true);
            }

        };
        acceptor.bind(ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"client.with.pipelining.must.not.retry.pipelining.immediately.after.failure/request"})
    public void clientWithPipeliningMustNotRetryPipeliningImmediatelyAfterFailure() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        acceptor.getAcceptOptions().put("http.dateHeaderEnabled", Boolean.FALSE);
        acceptor.getAcceptOptions().put("http.serverHeaderEnabled", Boolean.FALSE);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();

                session.setStatus(HttpStatus.SUCCESS_OK);
                session.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(8));
                if (session.getRequestURI().toString().equals("/request1")) {
                    writeStringMessageToSession("request1", session);
                } else {
                    writeStringMessageToSession("request2", session);
                }

                session.close(true);
            }

        };
        acceptor.bind(ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"server.must.close.its.half.of.connection.after.sending.response.if.it.receives.a.close/request"})
    public void serverMustCloseItsHalfOfConnectionAfterSendingResponseIfItReceivesAClose() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        acceptor.getAcceptOptions().put("http.dateHeaderEnabled", Boolean.FALSE);
        acceptor.getAcceptOptions().put("http.serverHeaderEnabled", Boolean.FALSE);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();

                session.setStatus(HttpStatus.SUCCESS_OK);
                session.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(0));

                session.close(true);
            }

        };
        acceptor.bind(ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"client.must.not.reuse.tcp.connection.when.receives.connection.close/request"})
    public void clientMustNotReuseTcpConnectionWhenReceivesConnectionClose() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        acceptor.getAcceptOptions().put("http.dateHeaderEnabled", Boolean.FALSE);
        acceptor.getAcceptOptions().put("http.serverHeaderEnabled", Boolean.FALSE);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.SUCCESS_OK);
                session.addWriteHeader(HttpHeaders.HEADER_CONNECTION, "close");
                session.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(0));
                session.close(true);
            }

        };
        acceptor.bind(ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"server.getting.upgrade.request.must.respond.with.upgrade.header/request"})
    public void serverGettingUpgradeRequestMustRespondWithUpgradeHeader() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.INFO_SWITCHING_PROTOCOLS);
                session.addWriteHeader(HttpHeaders.HEADER_UPGRADE, String.valueOf("WebSocket"));
                session.close(true);
            }

        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"server.that.sends.upgrade.required.must.include.upgrade.header/request"})
    public void serverThatSendsUpgradeRequiredMustIncludeUpgradeHeader() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.CLIENT_UPGRADE_REQUIRED);
                session.addWriteHeader(HttpHeaders.HEADER_UPGRADE, "WebSocket");

                session.close(true);
            }

        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"server.that.is.upgrading.must.send.a.101.response/request"})
    public void serverThatIsUpgradingMustSendA100Response() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.INFO_SWITCHING_PROTOCOLS);

                session.close(true);
            }

        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    private static ResourceAddress httpAddress() {
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        String address = ADDRESS;
        return addressFactory.newResourceAddress(address);
    }

}
