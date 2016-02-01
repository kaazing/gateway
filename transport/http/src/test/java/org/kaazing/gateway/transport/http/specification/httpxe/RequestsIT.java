/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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

import org.apache.mina.core.service.IoHandler;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpAcceptorRule;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

import java.net.URI;

import static org.kaazing.test.util.ITUtil.createRuleChain;

/**
 * Defines how httpxe will deal with http methods.
 */
public class RequestsIT {

    private static final ResourceAddress HTTP_ADDRESS = httpAddress();
    private static final ResourceAddress HTTPXE_ADDRESS = httpxeAddress();

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/httpxe/requests");

    private final HttpAcceptorRule acceptor = new HttpAcceptorRule();

    @Rule
    public final TestRule chain = createRuleChain(acceptor, k3po);

    @Test
    @Specification("post.request.with.km.parameter.get/request")
    public void shouldProcessPostRequestAsGetRequest() throws Exception {
        test(HTTP_ADDRESS);
    }

    @Test
    @Specification("post.request.with.path.encoded.get/request")
    public void shouldProcessPathEncodedPostRequestAsGetRequest() throws Exception {
        test(HTTP_ADDRESS);
    }

    @Test
    @Specification("post.request.with.km.parameter.head/request")
    public void shouldProcessPostRequestAsHeadRequest() throws Exception {
        test(HTTP_ADDRESS);
    }

    @Test
    @Specification("post.request.with.path.encoded.head/request")
    public void shouldProcessPathEncodedPostRequestAsHeadRequest() throws Exception {
        test(HTTP_ADDRESS);
    }

    @Test
    @Specification("post.request.with.path.encoded.post/request")
    public void shouldProcessPathEncodedPostRequestAsPostRequest() throws Exception {
        test(HTTP_ADDRESS);
    }

    @Test
    @Specification("post.request.with.km.parameter.put/request")
    public void shouldProcessPostRequestAsPutRequest() throws Exception {
        test(HTTP_ADDRESS);
    }

    @Test
    @Specification("post.request.with.path.encoded.put/request")
    public void shouldProcessPathEncodedPostRequestAsPutRequest() throws Exception {
        test(HTTP_ADDRESS);
    }

    @Test
    @Specification("post.request.with.km.parameter.delete/request")
    public void shouldProcessPostRequestAsDeleteRequest() throws Exception {
        test(HTTP_ADDRESS);
    }

    @Test
    @Specification("post.request.with.path.encoded.delete/request")
    public void shouldProcessPathEncodedPostRequestAsDeleteRequest() throws Exception {
        test(HTTP_ADDRESS);
    }

    @Test
    @Specification("post.request.with.km.parameter.options/request")
    public void shouldProcessPostRequestAsOptionsRequest() throws Exception {
        test(HTTP_ADDRESS);
    }

    @Test
    @Specification("post.request.with.path.encoded.options/request")
    public void shouldProcessPathEncodedPostRequestAsOptionsRequest() throws Exception {
        test(HTTP_ADDRESS);
    }

    @Test
    @Specification("post.request.with.km.parameter.trace/request")
    public void shouldProcessPostRequestAsTraceRequest() throws Exception {
        test(HTTP_ADDRESS);
    }

    @Test
    @Specification("post.request.with.path.encoded.trace/request")
    public void shouldProcessPathEncodedPostRequestAsTraceRequest() throws Exception {
        test(HTTP_ADDRESS);
    }

    @Test
    @Specification("post.request.with.km.parameter.custom/request")
    public void shouldProcessPostRequestAsCustomRequest() throws Exception {
        test(HTTP_ADDRESS);
    }

    @Test
    @Specification("post.request.with.path.encoded.custom/request")
    @Ignore("Gateway doesn't support custom HTTP methods")
    public void shouldProcessPathEncodedPostRequestAsCustomRequest() throws Exception {
        test(HTTP_ADDRESS);
    }

    @Test
    @Specification("client.sends.httpxe.request/request")
    public void shouldPassWithHttpxeReqestUsingHttpContentType() throws Exception {
        test(HTTPXE_ADDRESS);
    }

    @Test
    @Specification("client.sends.httpxe.request.using.kct.parameter/request")
    public void shouldPassWithHttpxeReqestUsingKctParamter() throws Exception {
        test(HTTPXE_ADDRESS);
    }

    private void test(ResourceAddress address) throws Exception {
        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {
            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                session.setStatus(HttpStatus.SUCCESS_OK);
                session.close(false);
            }
        };
        acceptor.bind(address, acceptHandler);

        k3po.finish();
    }

    private static ResourceAddress httpAddress() {
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        String address = "http://localhost:8000/path";
        return addressFactory.newResourceAddress(URI.create(address));
    }

    private static ResourceAddress httpxeAddress() {
        String address = "httpxe://localhost:8000/path";
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        return addressFactory.newResourceAddress(URI.create(address));
    }

}
