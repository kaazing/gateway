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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.resource.address.http.HttpOriginSecurity;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.server.context.resolve.DefaultCrossSiteConstraintContext;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpAcceptorRule;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.kaazing.test.util.ITUtil.createRuleChain;

public class OriginSecurityIT {

    private static final ResourceAddress HTTP_ADDRESS = httpAddress();

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/httpxe/origin");

    private final HttpAcceptorRule acceptor = new HttpAcceptorRule();

    @Rule
    public final TestRule chain = createRuleChain(acceptor, k3po);

    @Test
    @Specification("request.with.origin.header/request")
    public void shouldPassWithOriginRequestHeader() throws Exception {
        test(HTTP_ADDRESS);
    }

    @Test
    @Specification("request.with.origin.header.and.x.origin.header/request")
    public void shouldPassWithOriginAndXoriginRequests() throws Exception {
        test(HTTP_ADDRESS);
    }

    @Test
    @Specification("origin.request.using.ko.parameter/request")
    public void shouldPassWhenUsingKoParameter() throws Exception {
        test(HTTP_ADDRESS);
    }

    @Test
    @Specification("origin.request.using.referer/request")
    public void shouldPassWithOnlyRefererAndXoriginRequest() throws Exception {
        test(HTTP_ADDRESS);
    }

    @Test
    @Specification("x.origin.header.not.identical.to.origin.header/request")
    public void shouldPassWhenXoriginHeaderDiffersFromOriginHeader() throws Exception {
        test(HTTP_ADDRESS);
    }

    @Test
    @Specification("request.with.kac.parameter/request")
    public void shouldPassWithAccessControlWithKacParameter() throws Exception {
        test(HTTP_ADDRESS);
    }

    @Test
    @Specification("x.origin.encoded.request.header/request")
    public void shouldPassWithEncodedXoriginRequest() throws Exception {
        test(HTTP_ADDRESS);
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
        Map<String, DefaultCrossSiteConstraintContext> constraints = new HashMap<>();
        DefaultCrossSiteConstraintContext constraintContext =
                new DefaultCrossSiteConstraintContext("http://source.example.com:80", "GET,POST", null, null);
        constraints.put(constraintContext.getAllowOrigin(), constraintContext);
        HttpOriginSecurity httpOriginSecuirty = new HttpOriginSecurity(constraints);

        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        ResourceOptions options = ResourceOptions.FACTORY.newResourceOptions();
        options.setOption(HttpResourceAddress.ORIGIN_SECURITY, httpOriginSecuirty);
        return addressFactory.newResourceAddress(URI.create("http://localhost:8000/path"), options);
    }

}
