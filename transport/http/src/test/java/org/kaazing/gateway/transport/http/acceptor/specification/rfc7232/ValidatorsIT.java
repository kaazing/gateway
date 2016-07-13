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
package org.kaazing.gateway.transport.http.acceptor.specification.rfc7232;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;

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
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MethodExecutionTrace;

/**
 * Test to validate behavior as specified in <a href="https://tools.ietf.org/html/rfc7231#section-4">RFC 7231 section 4:
 * Request Methods</a>.
 */
public class ValidatorsIT {
    private static final ResourceAddress HTTP_ADDRESS = httpAddress();

    private final HttpAcceptorRule acceptor = new HttpAcceptorRule();

    private JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private TestRule contextRule = ITUtil.toTestRule(context);
    private final TestRule trace = new MethodExecutionTrace();
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/http/rfc7232/validators");
    private final TestRule timeoutRule = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(acceptor).around(contextRule).around(k3po).around(timeoutRule);

    @Test
    @Specification({"last.modified.in.get/request"})
    public void shouldReceiveLastModifiedInGetResponse() throws Exception {
        testHttpWithLastModifiedAndETag(HTTP_ADDRESS);
    }
    
    @Test
    @Specification({"last.modified.in.head/request"})
    public void shouldReceiveLastModifiedInHeadResponse() throws Exception {
        testHttpWithLastModifiedAndETag(HTTP_ADDRESS);
    }
    
    @Test
    @Specification({"last.modified.in.post/request"})
    public void shouldReceiveLastModifiedInPostResponse() throws Exception {
        testHttpWithLastModifiedAndETag(HTTP_ADDRESS);
    }
    
    @Test
    @Specification({"last.modified.in.put/request"})
    public void shouldReceiveLastModifiedInPutResponse() throws Exception {
        testHttpWithLastModifiedAndETag(HTTP_ADDRESS);
    }
    
    @Test
    @Specification({"last.modified.with.strong.etag.in.get/request"})
    public void shouldReceiveLastModifiedAndStrongETagInGetResponse() throws Exception {
        testHttpWithLastModifiedAndETag(HTTP_ADDRESS);
    }
    
    @Test
    @Specification({"last.modified.with.strong.etag.in.head/request"})
    public void shouldReceiveLastModifiedAndStrongETagInHeadResponse() throws Exception {
        testHttpWithLastModifiedAndETag(HTTP_ADDRESS);
    }
    
    @Test
    @Specification({"last.modified.with.strong.etag.in.post/request"})
    public void shouldReceiveLastModifiedAndStrongETagInPostResponse() throws Exception {
        testHttpWithLastModifiedAndETag(HTTP_ADDRESS);
    }
    
    @Test
    @Specification({"last.modified.with.strong.etag.in.put/request"})
    public void shouldReceiveLastModifiedAndStrongETagInPutResponse() throws Exception {
        testHttpWithLastModifiedAndETag(HTTP_ADDRESS);
    }
    
    @Test
    @Specification({"last.modified.with.weak.etag.in.get/request"})
    public void shouldReceiveLastModifiedAndWeakETagInGetResponse() throws Exception {
        testHttpWithLastModifiedAndWeakETag(HTTP_ADDRESS);
    }
    
    @Test
    @Specification({"last.modified.with.weak.etag.in.head/request"})
    public void shouldReceiveLastModifiedAndWeakETagInHeadResponse() throws Exception {
        testHttpWithLastModifiedAndWeakETag(HTTP_ADDRESS);
    }
    
    @Test
    @Specification({"last.modified.with.weak.etag.in.post/request"})
    public void shouldReceiveLastModifiedAndWeakETagInPostResponse() throws Exception {
        testHttpWithLastModifiedAndWeakETag(HTTP_ADDRESS);
    }
    
    @Test
    @Specification({"last.modified.with.weak.etag.in.put/request"})
    public void shouldReceiveLastModifiedAndWeakETagInPutResponse() throws Exception {
        testHttpWithLastModifiedAndWeakETag(HTTP_ADDRESS);
    }
    
    @Test
    @Specification({"strong.etag.in.get/request"})
    public void shouldReceiveStrongETagInGetResponse() throws Exception {
        testHttpWithLastModifiedAndETag(HTTP_ADDRESS);
    }
    
    @Test
    @Specification({"strong.etag.in.head/request"})
    public void shouldReceiveStrongETagInHeadResponse() throws Exception {
        testHttpWithLastModifiedAndETag(HTTP_ADDRESS);
    }
    
    @Test
    @Specification({"strong.etag.in.post/request"})
    public void shouldReceiveStrongETagInPostResponse() throws Exception {
        testHttpWithLastModifiedAndETag(HTTP_ADDRESS);
    }
    
    @Test
    @Specification({"weak.etag.in.get/request"})
    public void shouldReceiveWeakETagInGetResponse() throws Exception {
        testHttpWithLastModifiedAndWeakETag(HTTP_ADDRESS);
    }
    
    @Test
    @Specification({"weak.etag.in.head/request"})
    public void shouldReceiveWeakETagInHeadResponse() throws Exception {
        testHttpWithLastModifiedAndWeakETag(HTTP_ADDRESS);
    }
    
    @Test
    @Specification({"weak.etag.in.post/request"})
    public void shouldReceiveWeakETagInPostResponse() throws Exception {
        testHttpWithLastModifiedAndWeakETag(HTTP_ADDRESS);
    }
    
    @Test
    @Specification({"weak.etag.in.put/request"})
    public void shouldReceiveWeakETagInPutResponse() throws Exception {
        testHttpWithLastModifiedAndWeakETag(HTTP_ADDRESS);
    }
    
    private void testHttpWithLastModifiedAndETag(ResourceAddress address) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.addWriteHeader(HttpHeaders.HEADER_LAST_MODIFIED, String.valueOf(System.currentTimeMillis()));
                session.addWriteHeader(HttpHeaders.HEADER_E_TAG, String.valueOf("6d82cbb050ddc7fa9cbb659014546e59"));
                session.close(false);
            }
        };
        acceptor.bind(address, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }
    
    private void testHttpWithLastModifiedAndWeakETag(ResourceAddress address) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.addWriteHeader(HttpHeaders.HEADER_LAST_MODIFIED, String.valueOf(System.currentTimeMillis()));
                session.addWriteHeader(HttpHeaders.HEADER_E_TAG, String.valueOf("W/\"123456789"));
                session.close(false);
            }
        };
        acceptor.bind(address, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }
    
    private static ResourceAddress httpAddress() {
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        String address = "http://localhost:8000/index.html";
        return addressFactory.newResourceAddress(address);
    }
}
