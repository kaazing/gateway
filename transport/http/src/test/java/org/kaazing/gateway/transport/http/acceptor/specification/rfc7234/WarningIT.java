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
public class WarningIT {
    private static final ResourceAddress HTTP_ADDRESS = httpAddress();

    private final HttpAcceptorRule acceptor = new HttpAcceptorRule();

    private JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private TestRule contextRule = ITUtil.toTestRule(context);
    private final TestRule trace = new MethodExecutionTrace();
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/http/rfc7234/warning");
    private final TestRule timeoutRule = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(acceptor).around(contextRule).around(k3po).around(timeoutRule);

    @Ignore("I don't know what the error is.")
    @Test
    @Specification({"199.misc.warning/request"})
    public void shouldReceiveResponseWithWarningHeader() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS, "199 - \"Miscellaneous Warning\"");
    }

    @Ignore("I don't know what the error is.")
    @Test
    @Specification({"110.response.stale.from.cache/request"})
    public void shouldReceiveResponseWithStaleHeader() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS, "110 - \"Response is Stale\"");
    }

    @Ignore("I don't know what the error is.")
    @Test
    @Specification({"111.revalidation.failed.from.cache/request"})
    public void shouldReceiveResponseWithRevalidateHeader() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS, "111 - \"Revalidation Failed\"");
    }

    @Ignore("I don't know what the error is.")
    @Test
    @Specification({"112.disconnected.operation.from.cache/request"})
    public void shouldReceiveResponseWithDisconnectedHeader() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS, "112 - \"Disconnected Operation\"");
    }

    @Ignore("I don't know what the error is.")
    @Test
    @Specification({"113.heuristic.expiration.from.cache/request"})
    public void shouldReceiveResponseWithHeuristicHeader() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS, "113 - \"Heuristic Expiration\"");
    }

    @Ignore("I don't know what the error is.")
    @Test
    @Specification({"214.transformation.applied.from.cache/request"})
    public void shouldReceiveResponseWithTransformationHeader() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS, "214 - \"Transformation Applied\"");
    }

    @Ignore("I don't know what the error is.")
    @Test
    @Specification({"299.misc.persistent.warning/request"})
    public void shouldReceiveResponseWithMiscPersistentWarning() throws Exception {
        testHttpNoResponseMessage(HTTP_ADDRESS, "299 - \"Miscellaneous Persistent Warning\"");
    }

    private void testHttpNoResponseMessage(ResourceAddress address, String warning) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {

            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                latch.countDown();
                session.setStatus(HttpStatus.CLIENT_BAD_REQUEST);
                session.addWriteHeader(HttpHeaders.HEADER_WARNING, warning);
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
