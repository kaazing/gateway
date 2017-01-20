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
public class IfNoneMatchIT {
    private static final ResourceAddress HTTP_ADDRESS = httpAddress();

    private final HttpAcceptorRule acceptor = new HttpAcceptorRule();

    private JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private TestRule contextRule = ITUtil.toTestRule(context);
    private final TestRule trace = new MethodExecutionTrace();
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/http/rfc7232/preconditions/if.none.match");
    private final TestRule timeoutRule = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(acceptor).around(contextRule).around(k3po).around(timeoutRule);

    @Test
    @Specification({"multiple.etags.delete.status.400/request"})
    public void shouldResultInBadRequestResponseWithDeleteAndMutipleETags() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                if(session.getReadHeaders().containsKey(HttpHeaders.HEADER_IF_NONE_MATCH)) {
                    session.setStatus(HttpStatus.CLIENT_BAD_REQUEST);
                } else {
                    session.setStatus(HttpStatus.SUCCESS_OK);
                }
                session.addWriteHeader(HttpHeaders.HEADER_E_TAG, String.valueOf("6d82cbb050ddc7fa9cbb659014546e59"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }
    
    @Test
    @Specification({"multiple.etags.get.status.200/request"})
    public void shouldResultInOKResponseWithGetAndMutipleETags() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }
    
    @Test
    @Specification({"multiple.etags.head.status.200/request"})
    public void shouldResultInOKResponseWithHeadAndMutipleETags() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }
    
    @Test
    @Specification({"multiple.etags.post.status.400/request"})
    public void shouldResultInBadRequestResponseWithPostAndMutipleETags() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                if(session.getReadHeaders().containsKey(HttpHeaders.HEADER_IF_NONE_MATCH)) {
                    session.setStatus(HttpStatus.CLIENT_BAD_REQUEST);
                } else {
                    session.setStatus(HttpStatus.SUCCESS_OK);
                }
                session.addWriteHeader(HttpHeaders.HEADER_E_TAG, String.valueOf("6d82cbb050ddc7fa9cbb659014546e59"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }
    
    @Test
    @Specification({"multiple.etags.put.status.400/request"})
    public void shouldResultInBadRequestResponseWithPutAndMutipleETags() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                if(session.getReadHeaders().containsKey(HttpHeaders.HEADER_IF_NONE_MATCH)) {
                    session.setStatus(HttpStatus.CLIENT_BAD_REQUEST);
                } else {
                    session.setStatus(HttpStatus.SUCCESS_OK);
                }
                session.addWriteHeader(HttpHeaders.HEADER_E_TAG, String.valueOf("6d82cbb050ddc7fa9cbb659014546e59"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }
    
    @Test
    @Specification({"multiple.etags.get.status.304/request"})
    public void shouldResultInNotModifiedResponseWithGetAndMutipleETags() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                if(session.getReadHeaders().containsKey(HttpHeaders.HEADER_IF_NONE_MATCH)) {
                    session.setStatus(HttpStatus.REDIRECT_NOT_MODIFIED);
                } else {
                    session.setStatus(HttpStatus.SUCCESS_OK);
                }
                session.addWriteHeader(HttpHeaders.HEADER_E_TAG, String.valueOf("6d82cbb050ddc7fa9cbb659014546e59"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }
    
    @Test
    @Specification({"multiple.etags.head.status.304/request"})
    public void shouldResultInNotModifiedResponseWithHeadAndMutipleETags() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                if(session.getReadHeaders().containsKey(HttpHeaders.HEADER_IF_NONE_MATCH)) {
                    session.setStatus(HttpStatus.REDIRECT_NOT_MODIFIED);
                } else {
                    session.setStatus(HttpStatus.SUCCESS_OK);
                }
                session.addWriteHeader(HttpHeaders.HEADER_E_TAG, String.valueOf("6d82cbb050ddc7fa9cbb659014546e59"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }
    
    @Test
    @Specification({"single.etag.delete.status.400/request"})
    public void shouldResultBadRequestResponseWithDeleteAndSingleETag() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                if(session.getReadHeaders().containsKey(HttpHeaders.HEADER_IF_NONE_MATCH)) {
                    session.setStatus(HttpStatus.CLIENT_BAD_REQUEST);
                } else {
                    session.setStatus(HttpStatus.SUCCESS_OK);
                }
                session.addWriteHeader(HttpHeaders.HEADER_E_TAG, String.valueOf("6d82cbb050ddc7fa9cbb659014546e59"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }
    
    @Test
    @Specification({"single.etag.get.status.200/request"})
    public void shouldResultInOKResponseWithGetAndSingleETag() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }
    
    @Test
    @Specification({"single.etag.get.status.304/request"})
    public void shouldResultInNotModifiedResponseWithGetAndSingleETag() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                if(session.getReadHeaders().containsKey(HttpHeaders.HEADER_IF_NONE_MATCH)) {
                    session.setStatus(HttpStatus.REDIRECT_NOT_MODIFIED);
                } else {
                    session.setStatus(HttpStatus.SUCCESS_OK);
                }
                session.addWriteHeader(HttpHeaders.HEADER_E_TAG, String.valueOf("6d82cbb050ddc7fa9cbb659014546e59"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }
    
    @Test
    @Specification({"single.etag.head.status.200/request"})
    public void shouldResultInOKResponseWithHeadAndSingleETag() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS);
    }
    
    @Test
    @Specification({"single.etag.head.status.304/request"})
    public void shouldResultInNotModifiedResponseWithHeadAndSingleETag() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                if(session.getReadHeaders().containsKey(HttpHeaders.HEADER_IF_NONE_MATCH)) {
                    session.setStatus(HttpStatus.REDIRECT_NOT_MODIFIED);
                } else {
                    session.setStatus(HttpStatus.SUCCESS_OK);
                }
                session.addWriteHeader(HttpHeaders.HEADER_E_TAG, String.valueOf("6d82cbb050ddc7fa9cbb659014546e59"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }
    
    @Test
    @Specification({"single.etag.post.status.400/request"})
    public void shouldResultBadRequestResponseWithPostAndSingleETag() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                if(session.getReadHeaders().containsKey(HttpHeaders.HEADER_IF_NONE_MATCH)) {
                    session.setStatus(HttpStatus.CLIENT_BAD_REQUEST);
                } else {
                    session.setStatus(HttpStatus.SUCCESS_OK);
                }
                session.addWriteHeader(HttpHeaders.HEADER_E_TAG, String.valueOf("6d82cbb050ddc7fa9cbb659014546e59"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }
    
    @Test
    @Specification({"single.etag.put.status.400/request"})
    public void shouldResultBadRequestResponseWithPutAndSingleETag() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                if(session.getReadHeaders().containsKey(HttpHeaders.HEADER_IF_NONE_MATCH)) {
                    session.setStatus(HttpStatus.CLIENT_BAD_REQUEST);
                } else {
                    session.setStatus(HttpStatus.SUCCESS_OK);
                }
                session.addWriteHeader(HttpHeaders.HEADER_E_TAG, String.valueOf("6d82cbb050ddc7fa9cbb659014546e59"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }
    
    @Test
    @Specification({"wildcard.delete.status.412/request"})
    public void shouldResultInPreconditionFailedResponseWithDeleteAndWildcard() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                if(session.getReadHeaders().containsKey(HttpHeaders.HEADER_IF_NONE_MATCH)) {
                    session.setStatus(HttpStatus.CLIENT_PRECONDITION_FAILED);
                } else {
                    session.setStatus(HttpStatus.SUCCESS_OK);
                }
                session.addWriteHeader(HttpHeaders.HEADER_E_TAG, String.valueOf("6d82cbb050ddc7fa9cbb659014546e59"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }
    
    @Test
    @Specification({"wildcard.get.status.304/request"})
    public void shouldResultInNotModifiedResponseWithGetAndWildcard() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                if(session.getReadHeaders().containsKey(HttpHeaders.HEADER_IF_NONE_MATCH)) {
                    session.setStatus(HttpStatus.REDIRECT_NOT_MODIFIED);
                } else {
                    session.setStatus(HttpStatus.SUCCESS_OK);
                }
                session.addWriteHeader(HttpHeaders.HEADER_E_TAG, String.valueOf("6d82cbb050ddc7fa9cbb659014546e59"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }
    
    @Test
    @Specification({"wildcard.head.status.304/request"})
    public void shouldResultInNotModifiedResponseWithHeadAndWildcard() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.REDIRECT_NOT_MODIFIED);
                session.addWriteHeader(HttpHeaders.HEADER_E_TAG, String.valueOf("6d82cbb050ddc7fa9cbb659014546e59"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }
    
    @Test
    @Specification({"wildcard.post.status.412/request"})
    public void shouldResultInPreconditionFailedResponseWithPostAndWildcard() throws Exception {
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
    @Specification({"wildcard.put.status.412/request"})
    public void shouldResultInPreconditionFailedResponseWithPutAndWildcard() throws Exception {
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
