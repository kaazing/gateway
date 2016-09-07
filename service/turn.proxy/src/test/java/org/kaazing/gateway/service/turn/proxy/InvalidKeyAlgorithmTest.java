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

import java.io.FileInputStream;
import java.security.KeyStore;

import javax.crypto.spec.SecretKeySpec;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.util.feature.EarlyAccessFeatures;
import org.kaazing.test.util.MethodExecutionTrace;

public class InvalidKeyAlgorithmTest {

    @Rule
    public TestRule testExecutionTrace = new MethodExecutionTrace();

    @Ignore ("Please see issue #708: https://github.com/kaazing/tickets/issues/708")
    @Test
    public void shouldFailGatewayStartupWhenKeyAlgorithmSetToInvalid() throws Exception {
        Gateway gateway = new Gateway();
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
                            .property("key.alias", "turn.shared.secret")
                            .property("key.algorithm", "no algorithm")
                            .done()
                        .security()
                            .keyStore(keyStore)
                            .keyStorePassword(password)
                        .done()
                        .done();
        // @formatter:on
        try {
            gateway.start(configuration);
            throw new AssertionError("Gateway should fail to start because the key algorithm is invalid.");
        } catch (NumberFormatException e) {
            Assert.assertTrue("Wrong error message: " + e.getMessage(), e.getMessage().matches(
                    "Invalid key algorithm"));
        } finally {
            gateway.stop();
        }
    }
}
