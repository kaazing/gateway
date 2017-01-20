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
public class TransferCodingsIT {
    private static final ResourceAddress HTTP_ADDRESS = httpAddress();

    private final HttpAcceptorRule acceptor = new HttpAcceptorRule();

    private JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private TestRule contextRule = ITUtil.toTestRule(context);
    private final TestRule trace = new MethodExecutionTrace();
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/http/rfc7230/transfer.codings");
    private final TestRule timeoutRule = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(acceptor).around(contextRule).around(k3po).around(timeoutRule);

    @Test
    @Specification({"request.transfer.encoding.chunked/request"})
    public void requestTransferEncodingChunked() throws Exception {
        standardHttpTestCase(HTTP_ADDRESS);
    }

    private WriteFuture writeStringMessageToSession(String message, IoSession session) {
        ByteBuffer data = ByteBuffer.wrap(message.getBytes());
        IoBufferAllocatorEx<?> allocator = ((IoSessionEx) session).getBufferAllocator();
        return session.write(allocator.wrap(data));
    }

    @Test
    @Specification({"response.transfer.encoding.chunked/request"})
    public void responseTransferEncodingChunked() throws Exception {
        acceptor.getAcceptOptions().put("http.dateHeaderEnabled", Boolean.FALSE);
        acceptor.getAcceptOptions().put("http.serverHeaderEnabled", Boolean.FALSE);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                session.setStatus(HttpStatus.SUCCESS_OK);
                session.addWriteHeader(HttpHeaders.HEADER_CONTENT_TYPE, "text/plain");
                session.addWriteHeader(HttpHeaders.HEADER_TRANSFER_ENCODING, "chunked");
                writeStringMessageToSession("Chunk A", session);
                writeStringMessageToSession("Chunk B", session);
                writeStringMessageToSession("Chunk C", session);
                session.close(false);
            }

        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
    }

    @Test
    @Specification({"request.transfer.encoding.chunked.with.trailer/request"})
    public void requestTransferEncodingChunkedWithTrailer() throws Exception {
        standardHttpTestCase(HTTP_ADDRESS);
    }

    @Ignore("Cannot write a trailer.")
    @Test
    @Specification({"response.transfer.encoding.chunked.with.trailer/request"})
    public void responseTransferEncodingChunkedWithTrailer() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.SUCCESS_OK);
                session.addWriteHeader(HttpHeaders.HEADER_CONTENT_TYPE, "text/plain");
                session.addWriteHeader(HttpHeaders.HEADER_TRANSFER_ENCODING, "chunked");
                session.addWriteHeader(HttpHeaders.HEADER_TRAILER, "Trailing-Header");
                writeStringMessageToSession("Chunk A", session);
                writeStringMessageToSession("Chunk B", session);
                writeStringMessageToSession("Chunk C", session);
                session.close(false);
            }

        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    private void standardHttpTestCase(ResourceAddress address) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.SUCCESS_OK);
                session.addWriteHeader(HttpHeaders.HEADER_CONTENT_TYPE, "text/plain");
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
