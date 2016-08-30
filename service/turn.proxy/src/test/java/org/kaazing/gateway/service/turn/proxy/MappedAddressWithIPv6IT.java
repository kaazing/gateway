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
public class MappedAddressWithIPv6IT {

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
                            .property("mapped.address", "[0:0:0:0:0:ffff:c000:210]:8080")
                            .property("key.alias", "turn.shared.secret")
                            .property("key.algorithm", "HmacMD5")
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

    @Ignore ("Please see issue #719: https://github.com/kaazing/tickets/issues/719")
    @Test
    @Specification({
            "correct.allocation.with.ipv6.method/request",
            "correct.allocation.with.ipv6.method/response" })
    public void shouldSucceedWithCorrectAllocationOnIpv6() throws Exception {
        k3po.finish();
    }

}
