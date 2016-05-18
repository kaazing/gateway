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
package org.kaazing.gateway.transport.http.specification.httpxe;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.kaazing.test.util.ITUtil.timeoutRule;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.service.IoHandler;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpAcceptorRule;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MethodExecutionTrace;

/**
 * Defines how httpxe will deal with http methods.
 */
public class RequestsIT {

    private static final ResourceAddress HTTP_ADDRESS = httpAddress();
    private static final ResourceAddress HTTPXE_ADDRESS = httpxeAddress();

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/httpxe/requests");

    private final HttpAcceptorRule acceptor = new HttpAcceptorRule();

    private JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private TestRule contextRule = ITUtil.toTestRule(context);
    private final TestRule trace = new MethodExecutionTrace();
    private final TestRule timeoutRule = timeoutRule(5, SECONDS);

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(acceptor).around(contextRule).
            around(k3po).around(timeoutRule);

    @Test
    @Specification("post.request.with.km.parameter.get/request")
    public void shouldProcessPostRequestAsGetRequest() throws Exception {
        testMethod(HTTP_ADDRESS, HttpMethod.GET);
    }

    @Test
    @Specification("post.request.with.path.encoded.get/request")
    public void shouldProcessPathEncodedPostRequestAsGetRequest() throws Exception {
        testMethod(HTTP_ADDRESS, HttpMethod.GET);
    }

    @Test
    @Specification("post.request.with.km.parameter.head/request")
    public void shouldProcessPostRequestAsHeadRequest() throws Exception {
        testMethod(HTTP_ADDRESS, HttpMethod.HEAD);
    }

    @Test
    @Specification("post.request.with.path.encoded.head/request")
    public void shouldProcessPathEncodedPostRequestAsHeadRequest() throws Exception {
        testMethod(HTTP_ADDRESS, HttpMethod.HEAD);
    }

    @Test
    @Specification("post.request.with.path.encoded.post/request")
    public void shouldProcessPathEncodedPostRequestAsPostRequest() throws Exception {
        testMethod(HTTP_ADDRESS, HttpMethod.POST);
    }

    @Test
    @Specification("post.request.with.km.parameter.put/request")
    public void shouldProcessPostRequestAsPutRequest() throws Exception {
        testMethod(HTTP_ADDRESS, HttpMethod.PUT);
    }

    @Test
    @Specification("post.request.with.path.encoded.put/request")
    public void shouldProcessPathEncodedPostRequestAsPutRequest() throws Exception {
        testMethod(HTTP_ADDRESS, HttpMethod.PUT);
    }

    @Test
    @Specification("post.request.with.km.parameter.delete/request")
    public void shouldProcessPostRequestAsDeleteRequest() throws Exception {
        testMethod(HTTP_ADDRESS, HttpMethod.DELETE);
    }

    @Test
    @Specification("post.request.with.path.encoded.delete/request")
    public void shouldProcessPathEncodedPostRequestAsDeleteRequest() throws Exception {
        testMethod(HTTP_ADDRESS, HttpMethod.DELETE);
    }

    @Test
    @Specification("post.request.with.km.parameter.options/request")
    public void shouldProcessPostRequestAsOptionsRequest() throws Exception {
        testMethod(HTTP_ADDRESS, HttpMethod.OPTIONS);
    }

    @Test
    @Specification("post.request.with.path.encoded.options/request")
    public void shouldProcessPathEncodedPostRequestAsOptionsRequest() throws Exception {
        testMethod(HTTP_ADDRESS, HttpMethod.OPTIONS);
    }

    @Test
    @Specification("post.request.with.km.parameter.trace/request")
    public void shouldProcessPostRequestAsTraceRequest() throws Exception {
        testMethod(HTTP_ADDRESS, HttpMethod.TRACE);
    }

    @Test
    @Specification("post.request.with.path.encoded.trace/request")
    public void shouldProcessPathEncodedPostRequestAsTraceRequest() throws Exception {
        testMethod(HTTP_ADDRESS, HttpMethod.TRACE);
    }

    @Test
    @Specification("post.request.with.km.parameter.custom/request")
    @Ignore("Gateway doesn't support custom HTTP methods")
    public void shouldProcessPostRequestAsCustomRequest() throws Exception {
    }

    @Test
    @Specification("post.request.with.path.encoded.custom/request")
    @Ignore("Gateway doesn't support custom HTTP methods")
    public void shouldProcessPathEncodedPostRequestAsCustomRequest() throws Exception {
    }

    @Test
    @Specification("client.sends.httpxe.request/request")
    public void shouldPassWithHttpxeReqestUsingHttpContentType() throws Exception {
        testHttpxeContentType(HTTPXE_ADDRESS, HttpMethod.POST);
    }

    @Test
    @Specification("client.sends.httpxe.request.using.kct.parameter/request")
    public void shouldPassWithHttpxeReqestUsingKctParamter() throws Exception {
        testHttpxeContentType(HTTPXE_ADDRESS, HttpMethod.POST);
    }

    private void testMethod(ResourceAddress address, HttpMethod method) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {
            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                assertEquals(session.getMethod(), method);
                assertEquals(session.getServicePath().getPath(), "/path");
                session.setStatus(HttpStatus.SUCCESS_OK);
                session.close(false);
            }
        };
        acceptor.bind(address, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    private void testHttpxeContentType(ResourceAddress address, HttpMethod method) throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                assertEquals(session.getMethod(), method);
                assertEquals(session.getServicePath().getPath(), "/path");
                assertEquals(session.getReadHeader("Content-Type"), "text/plain");
                assertEquals(session.getReadHeader("Content-Length"), "12");
            }

            @Override
            protected void doMessageReceived(HttpAcceptSession session, Object message) throws Exception {
                latch.countDown();
                IoBufferEx actual = (IoBufferEx)message;
                assertEquals(12, actual.remaining());

                byte[] bytes = "Hello World!".getBytes();
                ByteBuffer data = ByteBuffer.wrap(bytes);
                IoBufferEx expected = session.getBufferAllocator().wrap(data);
                assertEquals(expected, actual);

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
        String address = "http://localhost:8000/path";
        return addressFactory.newResourceAddress(address);
    }

    private static ResourceAddress httpxeAddress() {
        String address = "httpxe://localhost:8000/path";
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        return addressFactory.newResourceAddress(address);
    }

}
