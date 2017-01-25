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
import org.kaazing.gateway.transport.http.HttpHeaders;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MethodExecutionTrace;

/**
 * Test to validate behavior as specified in <a href="https://tools.ietf.org/html/rfc7231#section-4">RFC 7231 section 4:
 * Request Methods</a>.
 */
public class MessageFormatIT {
    private static final ResourceAddress HTTP_ADDRESS = httpAddress();

    private final HttpAcceptorRule acceptor = new HttpAcceptorRule();

    private JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private TestRule contextRule = ITUtil.toTestRule(context);
    private final TestRule trace = new MethodExecutionTrace();
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/http/rfc7230/message.format");
    private final TestRule timeoutRule = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(acceptor).around(contextRule).around(k3po).around(timeoutRule);

    @Test
    @Specification({"inbound.should.accept.headers/request"})
    public void inboundShouldAcceptHeaders() throws Exception {
        standardHttpTestCase(HTTP_ADDRESS);
    }

    @Test
    @Specification({"outbound.should.accept.no.headers/request"})
    public void outboundShouldAcceptNoHeaders() throws Exception {
        standardHttpTestCase(HTTP_ADDRESS);
    }

    @Test
    @Specification({"outbound.should.accept.headers/request"})
    public void outboundShouldAcceptHeaders() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {
            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.addWriteHeader("some", "header");
                session.close(true);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"inbound.should.reject.request.with.whitespace.between.start.line.and.first.header/request"})
    public void inboundShouldRejectRequestWithWhitespaceBetweenStartLineAndFirstHeader() throws Exception {

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>();
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
    }

    @Test
    @Specification({"request.must.start.with.request.line/request"})
    public void requestMustStartWithRequestLine() throws Exception {
        standardHttpTestCase(HTTP_ADDRESS);
    }

    @Ignore("BUG: No response to lacking URI")
    @Test
    @Specification({"inbound.should.reject.invalid.request.line/request"})
    public void inboundShouldRejectInvalidRequestLine() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {
            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.SUCCESS_OK);
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"server.should.send.501.to.unimplemented.methods/request"})
    public void serverShouldSend501ToUnImplementedMethods() throws Exception {
        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>();
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
    }

    @Ignore("BUG: No response to error in URI")
    @Test
    @Specification({"server.should.send.414.to.request.with.too.long.a.request/request"})
    public void serverShouldSend414ToRequestWithTooLongARequest() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {
            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.SUCCESS_OK);
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"server.should.send.status.line.in.start.line/request"})
    public void serverShouldSendStatusLineInStartLine() throws Exception {
        standardHttpTestCase(HTTP_ADDRESS);
    }

    /*
     * No whitespace is allowed between the header field-name and colon.  In
     * the past, differences in the handling of such whitespace have led to
     * security vulnerabilities in request routing and response handling.  A
     * server MUST reject any received request message that contains
     * whitespace between a header field-name and colon with a response code
     * of 400 (Bad Request).  A proxy MUST remove any such whitespace from a
     * response message before forwarding the message downstream.
     */
    @Ignore("Not Spec Complient")
    @Test
    @Specification({"server.must.reject.header.with.space.between.header.name.and.colon/request"})
    public void serverMustRejectHeaderWithSpaceBetweenHeaderNameAndColon() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {
            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.SUCCESS_OK);
                session.close(true);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Ignore("BUG: No response to error in bad header.")
    @Test
    @Specification({"server.should.reject.obs.in.header.value/request"})
    public void serverShouldRejectOBSInHeaderValue() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {
            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.SUCCESS_OK);
                session.close(true);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Ignore("BUG: No response to error in bad header.")
    @Test
    @Specification({"proxy.or.gateway.must.reject.obs.in.header.value/request"})
    public void proxyOrGatewayMustRejectOBSInHeaderValue() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {
            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.SUCCESS_OK);
                session.close(true);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"inbound.on.receiving.field.with.length.larger.than.wanting.to.process.must.reply.with.4xx/request"})
    public void inboundOnReceivingFieldWithLengthLargerThanWantingToProcessMustReplyWith4xx() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {
            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.CLIENT_BAD_REQUEST);
                session.close(true);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"server.should.send.501.to.unknown.transfer.encoding/request"})
    public void serverShouldSend501ToUnknownTransferEncoding() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {
            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.SERVER_NOT_IMPLEMENTED);
                session.close(true);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"outbound.should.process.response.with.content.length/request"})
    public void outboundShouldProcessResponseWithContentLength() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {
            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.SUCCESS_OK);
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"inbound.should.process.request.with.content.length/request"})
    public void inboundShouldProcessRequestWithContentLength() throws Exception {
        standardHttpTestCase(HTTP_ADDRESS);
    }

    @Test
    @Specification({"client.should.send.content.length.header.in.post.even.if.no.content/request"})
    public void clientShouldSendContentLengthHeaderInPostEvenIfNoContent() throws Exception {
        standardHttpTestCase(HTTP_ADDRESS);
    }

    @Test
    @Specification({"head.response.must.not.have.content/request"})
    public void headResponseMustNotHaveContent() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {
            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.INFO_CONTINUE);
                session.close(true);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"head.response.must.not.have.content.though.may.have.content.length/request"})
    public void headResponseMustNotHaveContentThoughMayHaveContentLength() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {
            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(100));
                session.close(true);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"server.must.reject.request.with.multiple.different.content.length/request"})
    public void serverMustRejectRequestWithMultipleDifferentContentLength() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {
            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.CLIENT_BAD_REQUEST);
                session.close(true);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Ignore("BUG: Extra CRLF makes response 400.")
    @Test
    @Specification({"robust.server.should.allow.extra.CRLF.after.request.line/request"})
    public void robustServerShouldAllowExtraCRLFAfterRequestLine() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {
            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.SUCCESS_OK);
                session.close(true);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    /*
     * Bytes won't make it to the Http layer
     * 
     * @Test
     * @Specification({"non.http.request.to.http.server.should.be.responded.to.with.400/request"})
     * public void nonHttpRequestToHttpServerShouldBeRespondedToWith400() throws Exception {
     *     final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {
     *         @Override
     *         protected void doSessionOpened(HttpAcceptSession session) throws Exception {
     *             session.setStatus(HttpStatus.CLIENT_BAD_REQUEST);
     *             session.close(false);
     *         }
     *     };
     *     acceptor.bind(HTTP_ADDRESS, acceptHandler);
     *     k3po.finish();
     * }
     */

    private void standardHttpTestCase(ResourceAddress address) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {
            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.close(true);
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
