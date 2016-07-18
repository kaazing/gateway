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
public class IfMatchIT {
    private static final ResourceAddress HTTP_ADDRESS = httpAddress();

    private final HttpAcceptorRule acceptor = new HttpAcceptorRule();

    private JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private TestRule contextRule = ITUtil.toTestRule(context);
    private final TestRule trace = new MethodExecutionTrace();
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/http/rfc7232/preconditions/if.match");
    private final TestRule timeoutRule = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(acceptor).around(contextRule).around(k3po).around(timeoutRule);

    @Test
    @Specification({"multiple.etags.delete.status.200/request"})
    public void shouldSucceedWithDeleteAndMatchingETagInTheList() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"multiple.etags.get.status.200/request"})
    public void shouldIgnoreIfMatchHeaderWithGetAndMatchingETagInTheList() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"multiple.etags.head.status.200/request"})
    public void shouldIgnoreIfMatchHeaderWithHeadAndMatchingETagInTheList() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"multiple.etags.post.status.200/request"})
    public void shouldSucceedWithPostAndMatchingETagInTheList() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"multiple.etags.put.status.200/request"})
    public void shouldSucceedWithPutAndMatchingETagInTheList() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"unexpected.etags.delete.status.412/request"})
    public void shouldCausePreconditionFailedWithDeleteAndNoMatchingETagsInTheList() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.CLIENT_PRECONDITION_FAILED);
                session.addWriteHeader(HttpHeaders.HEADER_E_TAG, String.valueOf("6d82cbb050ddc7fa9cbb659014546e59"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"unexpected.etags.get.status.200/request"})
    public void shouldIgnoreIfMatchHeaderWithGetAndNoMatchingETagsInTheList() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"unexpected.etags.head.status.200/request"})
    public void shouldIgnoreIfMatchHeaderWithHeadAndNoMatchingETagsInTheList() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"unexpected.etags.post.status.412/request"})
    public void shouldCausePreconditionFailedWithPostAndNoMatchingETagsInTheList() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.CLIENT_PRECONDITION_FAILED);
                session.addWriteHeader(HttpHeaders.HEADER_E_TAG, String.valueOf("6d82cbb050ddc7fa9cbb659014546e59"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"unexpected.etags.put.status.412/request"})
    public void shouldCausePreconditionFailedWithPutAndNoMatchingETagsInTheList() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.CLIENT_PRECONDITION_FAILED);
                session.addWriteHeader(HttpHeaders.HEADER_E_TAG, String.valueOf("6d82cbb050ddc7fa9cbb659014546e59"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"strong.etag.delete.status.200/request"})
    public void shouldSucceedWithDeleteAndMatchingStrongETag() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"strong.etag.get.status.200/request"})
    public void shouldSucceedWithGetAndMatchingStrongETag() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"strong.etag.head.status.200/request"})
    public void shouldSucceedWithHeadAndMatchingStrongETag() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"strong.etag.post.status.200/request"})
    public void shouldSucceedWithPostAndMatchingStrongETag() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"strong.etag.put.status.200/request"})
    public void shouldSucceedWithPutAndMatchingStrongETag() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"strong.unmatched.etag.delete.status.412/request"})
    public void shouldCausePreconditionFailedWithDeleteAndUnmatchedStrongETag() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.CLIENT_PRECONDITION_FAILED);
                session.addWriteHeader(HttpHeaders.HEADER_E_TAG, String.valueOf("6d82cbb050ddc7fa9cbb659014546e59"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"strong.unmatched.etag.get.status.200/request"})
    public void shouldIgnoreIfMatchHeaderWithGetAndUnmatchedStrongETag() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"strong.unmatched.etag.head.status.200/request"})
    public void shouldIgnoreIfMatchHeaderWithHeadAndUnmatchedStrongETag() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"strong.unmatched.etag.post.status.412/request"})
    public void shouldCausePreconditionFailedWithPostAndUnmatchedStrongETag() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.CLIENT_PRECONDITION_FAILED);
                session.addWriteHeader(HttpHeaders.HEADER_E_TAG, String.valueOf("6d82cbb050ddc7fa9cbb659014546e59"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"strong.unmatched.etag.put.status.412/request"})
    public void shouldCausePreconditionFailedWithPutAndUnmatchedStrongETag() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.CLIENT_PRECONDITION_FAILED);
                session.addWriteHeader(HttpHeaders.HEADER_E_TAG, String.valueOf("6d82cbb050ddc7fa9cbb659014546e59"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"weak.etag.delete.status.412/request"})
    public void shouldCausePreconditionFailedWithDeleteAndWeakETag() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.CLIENT_PRECONDITION_FAILED);
                session.addWriteHeader(HttpHeaders.HEADER_E_TAG, String.valueOf("6d82cbb050ddc7fa9cbb659014546e59"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"weak.etag.get.status.200/request"})
    public void shouldIgnoreIfMatchHeaderWithGetAndWeakETag() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"weak.etag.head.status.200/request"})
    public void shouldIgnoreIfMatchHeaderWithHeadAndWeakETag() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"weak.etag.post.status.412/request"})
    public void shouldCausePreconditionFailedWithPostAndWeakETag() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.CLIENT_PRECONDITION_FAILED);
                session.addWriteHeader(HttpHeaders.HEADER_E_TAG, String.valueOf("6d82cbb050ddc7fa9cbb659014546e59"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"weak.etag.put.status.412/request"})
    public void shouldCausePreconditionFailedWithPutAndWeakETag() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.CLIENT_PRECONDITION_FAILED);
                session.addWriteHeader(HttpHeaders.HEADER_E_TAG, String.valueOf("6d82cbb050ddc7fa9cbb659014546e59"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"wildcard.etag.delete.status.200/request"})
    public void shouldSucceedWithDeleteAndWildcardForValidResource() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"wildcard.etag.get.status.200/request"})
    public void shouldSucceedWithGetAndWildcardForValidResource() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"wildcard.etag.head.status.200/request"})
    public void shouldSucceedWithHeadAndWildcardForValidResource() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"wildcard.etag.post.status.200/request"})
    public void shouldSucceedWithPostAndWildcardForValidResource() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }

    @Test
    @Specification({"wildcard.etag.put.status.200/request"})
    public void shouldSucceedWithPutAndWildcardForValidResource() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }

    /*
     * Delete Method not handled w/ 412
     * 
     * @Test
     * @Specification({"wildcard.etag.delete.status.412/request"})
     * public void shouldCausePreconditionFailedWithDeleteAndWildcardForInvalidResource() throws Exception {
     *    final CountDownLatch latch = new CountDownLatch(1);
     *    final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {
     *        @Override
     *        protected void doSessionOpened(HttpAcceptSession session) throws Exception {
     *            latch.countDown();
     *            session.setStatus(HttpStatus.CLIENT_PRECONDITION_FAILED);
     *           session.close(false);
     *        }
     *    };
     *    acceptor.bind(HTTP_ADDRESS, acceptHandler);
     *  k3po.finish();
     *  assertTrue(latch.await(4, SECONDS));
     * }
    */

    /*
     * Post Method not handled w/ 412
     * 
     * @Test
     * @Specification({"wildcard.etag.post.status.412/request"})
     * public void shouldCausePreconditionFailedWithPostAndWildcardForInvalidResource() throws Exception {
     *    final CountDownLatch latch = new CountDownLatch(1);
     *    final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {
     *        @Override
     *        protected void doSessionOpened(HttpAcceptSession session) throws Exception {
     *            latch.countDown();
     *            session.setStatus(HttpStatus.CLIENT_PRECONDITION_FAILED);
     *           session.close(false);
     *        }
     *    };
     *    acceptor.bind(HTTP_ADDRESS, acceptHandler);
     *  k3po.finish();
     *  assertTrue(latch.await(4, SECONDS));
     * }
    */

    /*
     * Put Method not handled w/ 412
     * 
     * @Test
     * @Specification({"wildcard.etag.put.status.412/request"})
     * public void shouldCausePreconditionFailedWithPutAndWildcardForInvalidResource() throws Exception {
     *    final CountDownLatch latch = new CountDownLatch(1);
     *    final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {
     *        @Override
     *        protected void doSessionOpened(HttpAcceptSession session) throws Exception {
     *            latch.countDown();
     *            session.setStatus(HttpStatus.CLIENT_PRECONDITION_FAILED);
     *           session.close(false);
     *        }
     *    };
     *    acceptor.bind(HTTP_ADDRESS, acceptHandler);
     *  k3po.finish();
     *  assertTrue(latch.await(4, SECONDS));
     * }
    */

    private void testHttpNoResponseMessage(ResourceAddress address) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.addWriteHeader(HttpHeaders.HEADER_E_TAG, String.valueOf("6d82cbb050ddc7fa9cbb659014546e59"));
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
