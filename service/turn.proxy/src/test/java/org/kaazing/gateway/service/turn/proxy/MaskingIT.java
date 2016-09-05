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

import static java.lang.String.format;
import static java.nio.charset.Charset.forName;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.util.feature.EarlyAccessFeatures;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Collection;


@RunWith(Parameterized.class)
public class MaskingIT {

    @Parameters
    public static Collection<String> data() {
        return Arrays.asList(new String[]{"tcp", "udp"});
    }

    private final K3poRule k3po;
    private final GatewayRule gateway;


    public MaskingIT(String scheme){
        k3po = new K3poRule()
                .setScriptRoot("org/kaazing/gateway/service/turn/proxy")
                .scriptProperty(format("acceptURI '%s://localhost:3479'", scheme))
                .scriptProperty(format("connectURI '%s://localhost:3478'", scheme));

        gateway = new GatewayRule() {
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
                        password,
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
                        .accept(scheme + "://localhost:3478")
                        .connect(scheme + "://localhost:3479")
                        .type("turn.proxy")
                        .property("mapped.address", "192.0.2.15:8080")
                        .property("masking.key", "0x1010101")
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
        this.chain = createRuleChain(gateway, k3po);
    }

    @Rule
    public TestRule chain;

    @Test
    @Specification({
            "mask.relay.peer.address/request",
            "mask.relay.peer.address/response"
    })
    public void shouldPassWithDefaultTurnProtocolTest() throws Exception {
        k3po.finish();
    }

    // TODO create also a test for IPv6

}
