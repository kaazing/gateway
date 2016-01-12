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

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.service.IoHandler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.http.HttpAcceptFilter;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpAcceptorRule;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

import java.net.URI;

import static org.kaazing.test.util.ITUtil.createRuleChain;

public class ResponsesIT {

    private static final ResourceAddress HTTPXE_ADDRESS = httpxeAddress();

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/httpxe/responses");

    private final HttpAcceptorRule acceptor = new HttpAcceptorRule();

    @Rule
    public final TestRule chain = createRuleChain(acceptor, k3po);

    @Test
    @Specification("wrapped.101.response.in.200/request")
    public void shouldPassWithWrapped101ResponseIn200() throws Exception {
        test(HTTPXE_ADDRESS, HttpStatus.INFO_SWITCHING_PROTOCOLS);
    }

    @Test
    @Specification("wrapped.201.response.in.200/request")
    public void shouldPassWithWrapped201ResponseIn200() throws Exception {
        test(HTTPXE_ADDRESS, HttpStatus.SUCCESS_CREATED);
    }

    @Test
    @Specification("wrapped.302.response.in.200/request")
    public void shouldPassWithWrapped302ResponseIn200() throws Exception {
        test(HTTPXE_ADDRESS, HttpStatus.REDIRECT_FOUND);
    }

    @Test
    @Specification("wrapped.400.response.in.200/request")
    public void shouldPassWithWrapped400ResponseIn200() throws Exception {
        test(HTTPXE_ADDRESS, HttpStatus.CLIENT_BAD_REQUEST);
    }

    @Test
    @Specification("wrapped.501.response.in.200/request")
    public void shouldPassWithWrapped501ResponseIn200() throws Exception {
        test(HTTPXE_ADDRESS, HttpStatus.SERVER_NOT_IMPLEMENTED);
    }

    @Test
    @Specification("connection.header.not.enveloped.in.response.body/request")
    public void shouldPassWhenConnectionHeaderInHeaderNotBody() throws Exception {
        test(HTTPXE_ADDRESS, HttpStatus.INFO_SWITCHING_PROTOCOLS);
    }

    private void test(ResourceAddress address, HttpStatus status) throws Exception {
        final IoHandler acceptHandler = new IoHandlerAdapter<HttpAcceptSession>() {
            @Override
            protected void doSessionOpened(HttpAcceptSession session) throws Exception {
                // Removing the filter that doesn't keep the wrapped responses
                IoFilterChain filterChain = session.getParent().getFilterChain();
                if (filterChain.contains(HttpAcceptFilter.CONDITIONAL_WRAPPED_RESPONSE.filterName())) {
                    filterChain.remove(HttpAcceptFilter.CONDITIONAL_WRAPPED_RESPONSE.filterName());
                }

                session.setStatus(status);
                session.setWriteHeader("Content-Type", "text/html");
                session.setWriteHeader("Connection", "keep-alive");
                session.close(false);
            }
        };
        acceptor.bind(address, acceptHandler);

        k3po.finish();
    }

    private static ResourceAddress httpxeAddress() {
        String address = "httpxe://localhost:8000/path";
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        return addressFactory.newResourceAddress(URI.create(address));
    }

}
