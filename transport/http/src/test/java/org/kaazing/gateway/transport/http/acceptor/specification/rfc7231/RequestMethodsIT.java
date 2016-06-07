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
package org.kaazing.gateway.transport.http.acceptor.specification.rfc7231;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.service.IoHandler;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
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
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MethodExecutionTrace;

/**
 * Test to validate behavior as specified in <a href="https://tools.ietf.org/html/rfc7231#section-4">RFC 7231 section 4:
 * Request Methods</a>.
 */
public class RequestMethodsIT {
    private static final ResourceAddress HTTP_ADDRESS = httpAddress();

    private final HttpAcceptorRule acceptor = new HttpAcceptorRule();

    private JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private TestRule contextRule = ITUtil.toTestRule(context);
    private final TestRule trace = new MethodExecutionTrace();
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/http/rfc7231/method.definitions");
    private final TestRule timeoutRule = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(acceptor).around(contextRule).around(k3po).around(timeoutRule);

    @Test
    @Specification({"501/request"})
    public void serverMustRespondToUnknownMethodWith501() throws Exception {
        // When a request method is received
        // that is unrecognized or not implemented by an origin server, the
        // origin server SHOULD respond with the 501 (Not Implemented) status
        // code.
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"get/request"})
    public void serverShouldImplementGet() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"head/request"})
    public void serverShouldImplementHead() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"post/request"})
    public void serverShouldImplementPost() throws Exception {
        testHttpWithResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"put/request"})
    public void serverShouldImplementPut() throws Exception {
        testHttpWithResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"405/request"})
    public void serverShouldRespondWith405ToUnrecognizedMethods() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }

    private void testHttpNoResponseMessage(ResourceAddress address) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                
                if(session.getMethod().equals(HttpMethod.TRACE)) {
                    session.setStatus(HttpStatus.SERVER_NOT_IMPLEMENTED);
                } else if(session.getMethod().equals(HttpMethod.OPTIONS)) {
                    session.setStatus(HttpStatus.CLIENT_METHOD_NOT_ALLOWED);
                } else {
                    session.setStatus(HttpStatus.SUCCESS_OK);
                }
                
                session.close(false);
            }
        };
        acceptor.bind(address, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    private void testHttpWithResponseMessage(ResourceAddress address) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doMessageReceived(HttpAcceptSession session, Object message) throws Exception {
                latch.countDown();
                IoBufferEx actual = (IoBufferEx) message;
                assertEquals(7, actual.remaining());

                byte[] bytes = "content".getBytes();
                ByteBuffer data = ByteBuffer.wrap(bytes);
                IoBufferEx expected = session.getBufferAllocator().wrap(data);
                assertEquals(expected, actual);

                // write back the same message received
                session.setStatus(HttpStatus.SUCCESS_OK);
                session.setWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(expected.capacity()));
                session.write(expected);
                session.close(false);
            }

        };
        acceptor.bind(address, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    private static ResourceAddress httpAddress() {
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        String address = "http://localhost:8000/resource";
        return addressFactory.newResourceAddress(address);
    }
}
