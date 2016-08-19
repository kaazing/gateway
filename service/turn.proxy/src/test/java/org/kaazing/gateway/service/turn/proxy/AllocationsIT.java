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
package org.kaazing.gateway.service.turn.proxy;

import static java.nio.charset.Charset.forName;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.io.FileInputStream;
import java.security.KeyStore;

import javax.crypto.spec.SecretKeySpec;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.util.feature.EarlyAccessFeatures;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

/**
 * Test to validate behavior as specified in <a href="https://tools.ietf.org/html/rfc5766">RFC 5766: TURN</a> through TCP.
 */
public class AllocationsIT {

    private final K3poRule k3po = new K3poRule()
            .setScriptRoot("org/kaazing/specification/turn/allocations")
            .scriptProperty("acceptURI 'tcp://localhost:3479'");

    private final GatewayRule gateway = new GatewayRule() {
        {
            KeyStore keyStore = null;
            char[] password = "ab987c".toCharArray();
            try {
                FileInputStream fileInStr = new FileInputStream(System.getProperty("user.dir")
                        + "/target/truststore/keystore.db");
                keyStore = KeyStore.getInstance("JCEKS");
                keyStore.load(fileInStr, "ab987c".toCharArray());
                keyStore.setKeyEntry(
                    "turn.shared.secret",
                    new SecretKeySpec("turnAuthenticationSharedSecret".getBytes(forName("UTF-8")), "PBEWithMD5AndDES"),
                    "ab987c".toCharArray(),
                    null
                );
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            // @formatter:off
            GatewayConfiguration configuration =
                new GatewayConfigurationBuilder()
                    .property(EarlyAccessFeatures.TURN_PROXY.getPropertyName(), "true")
                    .service()
                        .accept("tcp://localhost:3478")
                        .connect("tcp://localhost:3479")
                        .type("turn.proxy")
                        .property("mapped.address", "192.0.2.15:8080")
                        .property("key.alias", "turn.shared.secret")
                        .property("key.algorithm", "HmacMD5")
                            // TODO relay adress override
                            //.property("relay.address.mask", propertyValue)
                    .done()
                    .security()
                        .keyStore(keyStore)
                        .keyStorePassword(password)
                    .done()
                .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, k3po);

    /**
     * See <a href="https://tools.ietf.org/html/rfc5766#section-6">RFC 5766 section 6: Allocations</a>.
     */
    @Test
    @Specification({
        "simple.allocate.method/request",
        "simple.allocate.method/response" })
    public void shouldSucceedWithGenericSTUNHeader() throws Exception {
        k3po.finish();
    }

    /**
     * See <a href="https://tools.ietf.org/html/rfc5766#section-6">RFC 5766 section 6: Allocations</a>.
     */
    @Test
    @Specification({
        "two.allocate.methods.with.no.credentials/request",
        "two.allocate.methods.with.no.credentials/response" })
    public void shouldRespondWithTwo401sWhenGivenAllocateMethodsWithNoCred() throws Exception {
        k3po.finish();
    }

    /**
     * See <a href="https://tools.ietf.org/html/rfc5766#section-6">RFC 5766 section 6: Allocations</a>.
     */
    @Test
    @Specification({
        "allocate.method.with.requested.transport.attribute/request",
        "allocate.method.with.requested.transport.attribute/response" })
    public void shouldSucceedWithOnlyTransportAttribute() throws Exception {
        k3po.finish();
    }

    /**
     * See <a href="https://tools.ietf.org/html/rfc5766#section-6">RFC 5766 section 6: Allocations</a>.
     */
    @Test
    @Specification({
        "correct.allocation.method/request",
        "correct.allocation.method/response" })
    public void shouldSucceedWithCorrectAllocation() throws Exception {
        k3po.finish();
    }

    /**
     * See <a href="https://tools.ietf.org/html/rfc5766#section-6">RFC 5766 section 6: Allocations</a>.
     */
    @Test
    @Specification({
            "incorrect.attribute.length.with.error.message.length/request",
            "incorrect.attribute.length.with.error.message.length/no.response"})
    public void shouldGive400WithIncorrectLength() throws Exception {
        k3po.finish();
    }

    /**
     * See <a href="https://tools.ietf.org/html/rfc5766#section-6">RFC 5766 section 6: Allocations</a>.
     */
    @Test
    @Specification({
        "incorrect.length.given/request",
        "incorrect.length.given/response" })
    @Ignore("No script in specification")
    public void shouldGive401IfDirectlyGivesCredentials() throws Exception {
        k3po.finish();
    }

    /**
     * See <a href="https://tools.ietf.org/html/rfc5766#section-6">RFC 5766 section 6: Allocations</a>.
     */
    @Test
    @Specification({
        "multiple.connections.with.same.credentials.responds.437/request",
        "multiple.connections.with.same.credentials.responds.437/response" })
    public void shouldRespond437ToMultipleConnectionsWithSameCredentials() throws Exception {
        k3po.finish();
    }

    /**
     * See <a href="https://tools.ietf.org/html/rfc5766#section-6">RFC 5766 section 6: Allocations</a>.
     */
    @Test
    @Specification({
        "wrong.credentials.responds.441/request",
        "wrong.credentials.responds.441/response" })
    public void shouldRespond441ToWrongCredentials() throws Exception {
        k3po.finish();
    }

    /**
     * See <a href="https://tools.ietf.org/html/rfc5766#section-6">RFC 5766 section 6: Allocations</a>.
     */
    @Test
    @Specification({
        "unknown.attribute.responds.420/request",
        "unknown.attribute.responds.420/response" })
    public void shouldRespond420ToExtraBytes() throws Exception {
        k3po.finish();
    }

    /**
     * See <a href="https://tools.ietf.org/html/rfc5766#section-6">RFC 5766 section 6: Allocations</a>.
     */
    @Test
    @Specification({
        "no.requested.transport.attribute.responds.400/request",
        "no.requested.transport.attribute.responds.400/response" })
    public void shouldRespond400ToAllocateWithNoRequestedTransportAttribute() throws Exception {
        k3po.finish();
    }

}

