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
package org.kaazing.gateway.transport.http.acceptor.specification.rfc7234;

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
public class CacheControlInRequestIT {
    private static final ResourceAddress HTTP_ADDRESS = httpAddress();

    private final HttpAcceptorRule acceptor = new HttpAcceptorRule();

    private JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private TestRule contextRule = ITUtil.toTestRule(context);
    private final TestRule trace = new MethodExecutionTrace();
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/http/rfc7234/request.cache-control");
    private final TestRule timeoutRule = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(acceptor).around(contextRule).around(k3po).around(timeoutRule);

    @Test
    @Specification({"max-age.fresh.response.from.cache/request"})
    public void shouldReceiveCachedResponseWithMaxAgeWhenCachedResponseIsFresh() throws Exception {
        testHttpCacheControlHeader(HTTP_ADDRESS);
    }

    @Test
    @Specification({"max-age.stale.response.conditional.request.304/request"})
    public void shouldReceiveNotModifiedWhenCachedResponseIsStaleForConditionalRequestWithMaxAge() throws Exception {
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
                session.addWriteHeader(HttpHeaders.HEADER_CACHE_CONTROL, String.valueOf("max-age=600"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"max-age.stale.response.unconditional.request.200/request"})
    public void shouldReceiveNotModifiedWhenCachedResponseIsStaleForUnconditionalRequestWithMaxAge() throws Exception {
        testHttpCacheControlHeader(HTTP_ADDRESS);
    }

    @Test
    @Specification({"max-stale.any.age.from.cache/request"})
    public void shouldReceiveCachedResponseForMaxStaleWithNoValue() throws Exception {
        testHttpCacheControlHeader(HTTP_ADDRESS);
    }

    @Test
    @Specification({"max-stale.stale.response.from.cache/request"})
    public void shouldReceiveCachedResponseForMaxStaleWithinLimit() throws Exception {
        testHttpCacheControlHeader(HTTP_ADDRESS);
    }

    @Test
    @Specification({"max-stale.stale.response.conditional.request.304/request"})
    public void shouldReceiveNotModifiedWithStaleCachedResponseWhenMaxStaleExceedsLimitForConditionalRequest() throws Exception {
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
                session.addWriteHeader(HttpHeaders.HEADER_CACHE_CONTROL, String.valueOf("max-age=600"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"max-stale.stale.response.unconditional.request.200/request"})
    public void shouldReceiveOKWithStaleCachedResponseWhenMaxStaleExceedsLimitForUnconditionalRequest() throws Exception {
        testHttpCacheControlHeader(HTTP_ADDRESS);
    }

    @Test
    @Specification({"min-fresh.fresh.response.from.cache/request"})
    public void shouldReceiveCachedResponseForMinFreshWithinLimit() throws Exception {
        testHttpCacheControlHeader(HTTP_ADDRESS);
    }

    @Test
    @Specification({"min-fresh.fresh.response.conditional.request.304/request"})
    public void shouldReceiveNotModifiedWithFreshCachedResponseWhenMinFreshExceedsLimitForForConditionalRequest()
            throws Exception {
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
                session.addWriteHeader(HttpHeaders.HEADER_CACHE_CONTROL, String.valueOf("max-age=600"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"min-fresh.fresh.response.unconditional.request.200/request"})
    public void shouldReceiveOKWithFreshCachedResponseWhenMinFreshExceedsLimitForForUnconditionalRequest() throws Exception {
        testHttpCacheControlHeader(HTTP_ADDRESS);
    }

    @Test
    @Specification({"min-fresh.stale.response.conditional.request.304/request"})
    public void shouldReceiveNotModifiedWithStaleCachedResponseWhenMinFreshExceedsLimitForConditionalRequest() throws Exception {
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
                session.addWriteHeader(HttpHeaders.HEADER_CACHE_CONTROL, String.valueOf("max-age=600"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"min-fresh.stale.response.unconditional.request.200/request"})
    public void shouldReceiveNotModifiedWithStaleCachedResponseWhenMinFreshExceedsLimitForUnconditionalRequest()
            throws Exception {
        testHttpCacheControlHeader(HTTP_ADDRESS);
    }

    @Test
    @Specification({"no-cache.conditional.request.304/request"})
    public void shouldReceiveNotModifiedWithNoCacheForConditionalRequest() throws Exception {
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
                session.addWriteHeader(HttpHeaders.HEADER_CACHE_CONTROL, String.valueOf("max-age=600"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"no-cache.unconditional.request.200/request"})
    public void shouldReceiveOKWithNoCacheForUnconditionalRequest() throws Exception {
        testHttpCacheControlHeader(HTTP_ADDRESS);
    }

    @Test
    @Specification({"no-transform/request"})
    public void shouldReceiveUntransformedCachedResponse() throws Exception {
        testHttpCacheControlHeader(HTTP_ADDRESS);
    }

    @Test
    @Specification({"only-if-cached.from.cache/request"})
    public void shouldReceiveCachedResponseWithOnlyIfCachedAndReachableCache() throws Exception {
        testHttpCacheControlHeader(HTTP_ADDRESS);
    }

    @Test
    @Specification({"max-stale.with.warn-code.110/request"})
    public void shouldGiveWarningCode110WithMaxStale() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.addWriteHeader(HttpHeaders.HEADER_CACHE_CONTROL, String.valueOf("max-age=600"));
                session.addWriteHeader(HttpHeaders.HEADER_WARNING, "110");
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"max-stale.with.warn-code.112/request"})
    public void shouldGiveWarningCode112WithMaxStale() throws Exception {
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
                session.addWriteHeader(HttpHeaders.HEADER_CACHE_CONTROL, String.valueOf("max-age=600"));
                session.addWriteHeader(HttpHeaders.HEADER_WARNING, "112");
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    @Test
    @Specification({"only-if-cached.504/request"})
    public void shouldRespondToOnlyIfCachedWith504() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.SERVER_GATEWAY_TIMEOUT);
                session.addWriteHeader(HttpHeaders.HEADER_CACHE_CONTROL, String.valueOf("max-age=600"));
                session.close(false);
            }
        };
        acceptor.bind(HTTP_ADDRESS, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    private void testHttpCacheControlHeader(ResourceAddress address) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.addWriteHeader(HttpHeaders.HEADER_CACHE_CONTROL, String.valueOf("max-age=600"));
                session.close(false);
            }
        };
        acceptor.bind(address, acceptHandler);

        k3po.finish();
        assertTrue(latch.await(4, SECONDS));
    }

    private void testHttpETagAndCacheControlHeader(ResourceAddress address) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.addWriteHeader(HttpHeaders.HEADER_E_TAG, String.valueOf("6d82cbb050ddc7fa9cbb659014546e59"));
                session.addWriteHeader(HttpHeaders.HEADER_CACHE_CONTROL, String.valueOf("max-age=600"));
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
